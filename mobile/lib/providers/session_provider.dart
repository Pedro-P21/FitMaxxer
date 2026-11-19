import 'dart:async';
import 'dart:convert';

import 'package:flutter/foundation.dart';

import '../models/profile.dart';
import '../services/api_client.dart';
import '../services/auth_service.dart';
import '../services/profile_service.dart';
import '../services/token_store.dart';

enum SessionStage {
  starting,
  signedOut,
  checkingProfile,
  onboarding,
  ready,
  expired,
  retry,
}

class SessionProvider extends ChangeNotifier {
  SessionProvider({required this.api, required this.store})
    : auth = AuthService(api),
      profiles = ProfileService(api) {
    api.onUnauthorized = expire;
  }
  final ApiClient api;
  final TokenStore store;
  final AuthService auth;
  final ProfileService profiles;
  SessionStage stage = SessionStage.starting;
  bool busy = false;
  bool registered = false;
  String? error;
  UserProfile? profile;
  ProfileDraft draft = ProfileDraft();
  Timer? _expiryTimer;

  static DateTime? expiryOf(String token) {
    try {
      final parts = token.split('.');
      if (parts.length != 3) return null;
      final claims = jsonDecode(
        utf8.decode(base64Url.decode(base64Url.normalize(parts[1]))),
      ) as Map;
      final seconds = claims['exp'] as num;
      return DateTime.fromMillisecondsSinceEpoch(
        seconds.toInt() * 1000,
        isUtc: true,
      );
    } catch (_) {
      return null;
    }
  }

  Future<void> restore() async {
    stage = SessionStage.starting;
    error = null;
    notifyListeners();
    try {
      final token = await store.read();
      if (token == null) {
        stage = SessionStage.signedOut;
      } else if (!_valid(token)) {
        await expire();
        return;
      } else {
        api.token = token;
        _scheduleExpiry(token);
        await loadProfile();
        return;
      }
    } catch (_) {
      stage = SessionStage.retry;
      error = 'Your session could not be restored. Please try again.';
    }
    notifyListeners();
  }

  bool _valid(String token) =>
      expiryOf(token)?.isAfter(DateTime.now()) ?? false;
  void _scheduleExpiry(String token) {
    _expiryTimer?.cancel();
    _expiryTimer = Timer(expiryOf(token)!.difference(DateTime.now()), expire);
  }

  void checkExpiry() {
    if (api.token != null && !_valid(api.token!)) {
      unawaited(expire());
    }
  }

  Future<void> loadProfile() async {
    stage = SessionStage.checkingProfile;
    error = null;
    notifyListeners();
    try {
      profile = await profiles.getProfile();
      if (stage == SessionStage.expired) return;
      draft = profile?.toDraft() ?? ProfileDraft();
      stage = profile == null ? SessionStage.onboarding : SessionStage.ready;
    } on ApiException catch (e) {
      if (stage != SessionStage.expired) {
        stage = SessionStage.retry;
        error = e.message;
      }
    } catch (_) {
      if (stage != SessionStage.expired) {
        stage = SessionStage.retry;
        error = 'Your profile could not be loaded. Please try again.';
      }
    }
    notifyListeners();
  }

  Future<void> authenticate(
    String identifier,
    String password, {
    String? email,
  }) async {
    if (busy) return;
    busy = true;
    registered = false;
    error = null;
    notifyListeners();
    try {
      if (email != null) {
        await auth.register(email, identifier, password);
        registered = true;
      }
      final token = await auth.login(identifier, password);
      if (!_valid(token)) throw const ApiException('Please log in again.');
      await store.write(token);
      api.token = token;
      _scheduleExpiry(token);
      await loadProfile();
    } on ApiException catch (e) {
      error = registered
          ? 'Your account was created, but login did not complete. Please log in.'
          : e.status == 401
          ? 'Email, username or password is incorrect'
          : e.message;
    } catch (_) {
      error = registered
          ? 'Your account was created. Please log in to continue.'
          : 'Login could not be completed. Please try again.';
    } finally {
      busy = false;
      notifyListeners();
    }
  }

  Future<bool> saveProfile() async {
    if (busy) return false;
    busy = true;
    error = null;
    notifyListeners();
    try {
      profile = await profiles.save(draft);
      return stage != SessionStage.expired;
    } on ApiException catch (e) {
      if (stage != SessionStage.expired) error = e.message;
      return false;
    } catch (_) {
      error = 'Your profile could not be saved. Please try again.';
      return false;
    } finally {
      busy = false;
      notifyListeners();
    }
  }

  void finishOnboarding() {
    stage = SessionStage.ready;
    notifyListeners();
  }

  void clearError() {
    if (error != null) {
      error = null;
      notifyListeners();
    }
  }

  void loginAgain() {
    stage = SessionStage.signedOut;
    error = null;
    notifyListeners();
  }

  Future<void> expire() async {
    _expiryTimer?.cancel();
    api.token = null;
    profile = null;
    draft = ProfileDraft();
    stage = SessionStage.expired;
    error = null;
    notifyListeners();
    try {
      await store.clear();
    } catch (_) {
      /* Invalid tokens are rejected on restore too. */
    }
  }

  Future<void> logout() async {
    // Clear secure storage first: a failed clear must not silently restore a session.
    await store.clear();
    _expiryTimer?.cancel();
    api.token = null;
    profile = null;
    draft = ProfileDraft();
    registered = false;
    error = null;
    stage = SessionStage.signedOut;
    notifyListeners();
  }

  Future<void> retry() => api.token == null ? restore() : loadProfile();
  @override
  void dispose() {
    _expiryTimer?.cancel();
    api.onUnauthorized = null;
    super.dispose();
  }
}
