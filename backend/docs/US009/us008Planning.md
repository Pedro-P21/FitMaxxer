# US008 - User Account Registration and Secure Login

## 1. Requirements Engineering

##### 1.1. User Story Description
As a User, I want to create an account and log in securely, so that my personal data, custom recipes, and history are safely synchronized and saved.

##### 1.2. Use Case Specifications
**From the user story:**
- User accounts must be uniquely identified by an email address.
- Passwords must be stored securely using strong cryptographic hashing (e.g., BCrypt).
- Authentication is a prerequisite for accessing personalized features and managing personal data, meals, and progress.

**From the glossary and domain analysis:**
- **User:** The entity root representing a system user, managing authentication, profile data, and nutritional objectives within the `User Authentication Aggregate`.
- **UserId:** A unique identifier for the user.
- **Email:** A unique valid email address used for user identification, authentication, and communication within the application.
- **Password:** The confidential secret credential used to authenticate a user securely, stored in its hashed form.

##### 1.3. Acceptance Criteria
- **AC1:** The email address must be unique in the system; duplicate registrations with the same email must be rejected.
- **AC2:** The email format must be valid.
- **AC3:** The password must meet minimum security standards (e.g., length and complexity rules).
- **AC4:** Successful registration stores the user credentials securely with a hashed password.
- **AC5:** A registered user must be able to log in successfully using correct credentials, while incorrect credentials result in authentication failure.

##### 1.4. Found out Dependencies
- **None:** This is a foundational user story establishing the core authentication and user identity required by all other functional modules (such as profile creation, daily logs, and custom recipes).

##### 1.5 Input and Output Data
**Input Data:**
- *Typed data (Registration):* Email, Password, UserID.
- *Typed data (Login):* Email/UserId, Password.

**Output Data:**
- Confirmation of successful account creation / authentication token or session indicator.
- Error messages in case of validation failures (e.g., "Email already in use", "Invalid email format", "Invalid credentials").

##### 1.6. System Sequence Diagram (SSD)
![US009-SSD.svg](US008_SSD.svg)

##### 1.7. Assumptions and Decisions
- Account creation and authentication will use stateless or token-based session handling suitable for REST APIs interacting with a Flutter mobile client.

---

## 2. Analysis

### 2.1. Relevant Domain Model Excerpt
The following domain entities and value objects compose the `User Authentication Aggregate`:
- `User` (Entity Root)
- `UserId` (Value Object)
- `Email` (Value Object)
- `Password` (Value Object)

---

## 3. Design

### 3.1. Rationale

### 3.2. Design Decisions & Architecture

1. **Separation of Authentication and Profile Aggregates**
   The `User` entity only contains core credentials (`Email`, `Password`). Profile data (`UserProfile`) and nutritional goals are maintained in a separate aggregate to allow onboarding to happen independently after registration.

2. **Secure Password Handling**
   Raw passwords are never stored in memory or the database. They are validated via the `Password` value object and immediately hashed using Spring Security's `PasswordEncoder` (BCrypt).

3. **Unique Email Constraint**
   Enforced at both the domain service level (via repository check) and database level (unique constraint on the email column) to prevent race conditions during concurrent sign-ups.

### 3.3. Sequence Diagram (SD)

---

## 4. Tests

Following Test-Driven Development (TDD), testing proceeds in a bottom-up approach from Value Objects up to Controllers.

### 4.1. Domain Validations (Bottom-Up Approach)



### 4.2. Application Layer & Integration Tests


---

## 5. Implementation

### 5.1. Overall Flow
1. **Request Intake:** The REST controller receives registration/login payloads.
2. **Value Object Validation:** `Email` and `Password` VOs validate format and constraints upon instantiation.
3. **Business Verification:** The service checks email uniqueness against the repository.
4. **Credential Security:** Passwords are securely hashed.
5. **Persistence:** The `User` aggregate is saved within a transactional boundary.

### 5.2. Key Methods and Classes

| Class                       | Method                                        | Responsibility                              |
|-----------------------------|-----------------------------------------------|---------------------------------------------|
| `UserAuthController`        | `register(RegisterRequestDTO)`                | Exposes registration endpoint.              |
| `UserAuthController`        | `login(LoginRequestDTO)`                      | Exposes authentication endpoint.            |
| `UserAuthService`           | `registerUser(String email, String password)` | Orchestrates user creation and persistence. |
| `UserRepository`            | `findByEmail(Email email)`                    | Queries storage for existing email.         |
| `PasswordEncryptionService` | `encode(String rawPassword)`                  | Cryptographically hashes passwords.         |