# Security protocols

## Authentication and access tokens

The authentication endpoints are `POST /api/auth/register` and `POST /api/auth/login`. They are publicly accessible so a user can create an account or obtain credentials. During registration, the application hashes the submitted password with BCrypt before persisting the user. During login, it looks up the account by email or user ID and verifies the submitted password against the stored hash. A successful login returns a signed JSON Web Token (JWT).

The application uses signed JWT access tokens with the HS256 algorithm. The token contains:

- `iss` (issuer): `fitmaxxer-api`;
- `sub` (subject): the authenticated user's ID;
- `aud` (audience): `fitmaxxer-api`;
- `iat` and `exp`: issue and expiry times;
- `token_use`: `access`.

The configured access-token lifetime defaults to 15 minutes and can be changed with `app.jwt.access-token-lifetime`. Tokens are signed with the HMAC-SHA-256 key supplied by `app.jwt.secret`, which resolves from the `JWT_SECRET` environment variable. The key must contain at least 32 UTF-8 bytes. A single key is used to sign and verify tokens; the application refuses to start when the key is missing or too short.

For each protected request, the client sends the token in the HTTP `Authorization` header using the `Bearer` scheme. Spring Security verifies the signature, expiry, issuer, audience, token type, and subject before allowing the request through. The subject is exposed as the authenticated principal and is used by the profile controller to identify the user whose data is being accessed. A missing, malformed, expired, incorrectly signed, or invalid-claims token is rejected with HTTP 401.

The security filter chain is stateless: the server does not create an HTTP session to remember a login. The client presents its access token with each protected request. Registration and login are permitted without authentication; `/api/profiles/**` and other unmatched routes require a valid authenticated request.

## Registration

1. The client sends `POST /api/auth/register` with an email, password, and user ID.
2. The security configuration allows this route without a token.
3. `UserAuthService` hashes the password with BCrypt, asks `UserFactory` to construct and validate the user, checks that the email and user ID are unique, and saves the user.
4. A successful registration returns `201 Created`. Duplicate or invalid values are translated to `400 Bad Request`.

Registration does not authenticate the client. The client uses the login endpoint after registration to obtain an access token.

## Login and access-token issuance

1. The client sends `POST /api/auth/login` with its identifier and password. Login is public so a client without a token can authenticate.
2. `UserAuthService` finds the account and compares the supplied password against the stored BCrypt hash. The raw password is never put into the token.
3. On success, `JwtTokenService` asks Spring Security's `JwtEncoder` to sign a JWT with HS256. The token contains:
    - `sub`: the user's stable ID, used as the profile owner;
    - `iss` and `aud`: the expected issuer and API audience;
    - `iat` and `exp`: issue and expiry times, with a 15-minute lifetime;
    - `token_use`: `access`.
4. The login mapper returns the token as a bearer token in the response. Invalid credentials return `401 Unauthorized`.

The JWT is signed, not encrypted. A holder can read its claims, so it must not contain a password, signing secret, or sensitive profile details.

### Token exchange, step by step

1. **Client:** The Flutter app sends the identifier and password to `POST /api/auth/login` over HTTPS.
2. **Server:** `UserAuthController` passes them to `UserAuthService`.
3. **Server:** The service loads the account and asks BCrypt to compare the submitted password with the stored password hash.
4. **Server:** After a match, the service asks `JwtTokenService` to issue an access token whose `sub` is that account's user ID. `JwtEncoder` signs the claims using the server's `JWT_SECRET`.
5. **Server → Client:** The server returns the signed token in the JSON login response, with type `Bearer`. The signing secret is never sent to the client.
6. **Client:** The app stores the token in platform secure storage, such as iOS Keychain or Android Keystore, and keeps using that same token until it expires. It does not generate or edit the token.
7. **Client → Server:** For each protected request, the app sends the exact token it received in the `Authorization: Bearer ...` header, over HTTPS.
8. **Server:** Spring Security verifies the signature and claims with `JwtDecoder`. It creates an authenticated principal from `sub`, then the controller passes that subject to the service as the profile owner.
9. **Server → Client:** The server processes the owner's data and returns the normal endpoint response. A missing, altered, or expired token is rejected with `401 Unauthorized`.

The client and server do not negotiate or share the signing key. The server creates the token; the client receives and returns it unchanged. The server recognizes it because the signature verifies under the same server-side key. HTTPS protects the password and token while they travel across the network and lets the client verify it is talking to the real server. JWT signing protects the token's integrity after issuance; it does not replace HTTPS.


## Where the implementation lives
 Let us assume US001:

| Area                                                              | Implementation                                    |
|-------------------------------------------------------------------|---------------------------------------------------|
| Public registration and login routes                              | `UserAuthController` and `UserAuthService`        |
| Stateless route authorization and bearer-token filter             | `SecurityConfig`                                  |
| HMAC key, JWT encoder, and validation rules                       | `JwtConfiguration`                                |
| 15-minute token claims and issuance                               | `JwtTokenService`                                 |
| Owner ID from the verified token subject                          | `PersonalDataController`                          |
| Existing-profile update/create and unique-ID persistence          | `PersonalDataService` and `UserProfileRepository` |
| HTTP error-status mapping for invalid data and database conflicts | `GlobalExceptionHandler`                          |
| Full flow diagrams                                                | `us008-SD.puml` and `us001-SD.puml`               |

The build adds Spring Security's OAuth2 Resource Server and JOSE modules. The former validates bearer tokens on requests; JOSE supplies the standards-based JWT encoder/decoder implementation. Tests cover successful authentication, missing/modified/expired tokens, profile updates, and existing domain/service constraints.

## BCrypt password hashing

Passwords are never stored as plaintext. The application uses Spring Security's `BCryptPasswordEncoder` to create a one-way password hash at registration. BCrypt generates a random salt for each encoded password and stores the salt and work-factor information as part of the resulting hash. Consequently, encoding the same password more than once produces different hash strings.

At login, the application does not decrypt the stored value. `PasswordEncoder.matches` uses the salt and parameters embedded in the stored hash to hash the submitted password for comparison. The password is accepted only when the computed result matches the stored BCrypt hash. BCrypt's configurable work factor makes each verification deliberately computationally expensive, which raises the cost of offline guessing if password hashes are exposed.

## Secret handling

`JWT_SECRET` must be supplied through the process environment or a deployment secret manager. For local development, it can be loaded from an ignored `.env` file before starting the application, or set in the IDE run configuration. Spring Boot does not load a plain `.env` file automatically. Do not commit the secret, include it in source files, or log it. Every application instance that verifies tokens from the same issuer must use the same key. Replacing the key invalidates all tokens signed with the previous key.

## User stories requiring authentication

All User Stories that require the user to be logged in use this authentication and authorization mechanism. Their protected requests must include a valid bearer access token.

## Current limits

Access tokens last 15 minutes and there is no refresh-token or per-token revocation endpoint. Logging out on the client discards its copy, but a stolen token remains usable until expiry. The short lifetime limits that window; a future refresh-token design should use revocable, server-tracked tokens or a trusted identity provider.

