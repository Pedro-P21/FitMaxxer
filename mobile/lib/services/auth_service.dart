import 'api_client.dart';

class AuthService {
  AuthService(this.api);
  final ApiClient api;
  Future<void> register(String email, String username, String password) async {
    await api.request(
      'POST',
      '/api/auth/register',
      authenticated: false,
      body: {
        'email': email.trim(),
        'userId': username.trim(),
        'password': password,
      },
    );
  }

  Future<String> login(String identifier, String password) async {
    final data = await api.request(
      'POST',
      '/api/auth/login',
      authenticated: false,
      body: {'identifier': identifier.trim(), 'password': password},
    );
    return data['token'] as String;
  }
}
