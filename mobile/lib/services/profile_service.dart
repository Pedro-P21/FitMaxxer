import '../models/profile.dart';
import 'api_client.dart';

class ProfileService {
  ProfileService(this.api);
  final ApiClient api;
  Future<UserProfile?> getProfile() async {
    try {
      return UserProfile.fromJson(await api.request('GET', '/api/profiles'));
    } on ApiException catch (error) {
      if (error.status == 404) return null;
      rethrow;
    }
  }

  Future<UserProfile> save(ProfileDraft draft) async {
    final data = await api.request(
      'POST',
      '/api/profiles/personal-data',
      body: draft.toJson(),
    );
    return UserProfile.fromJson({
      ...data,
      'age': int.parse(draft.age),
      'weightKg': double.parse(draft.weightKg),
      'heightCm': double.parse(draft.heightCm),
      'gender': draft.gender,
      'activityLevel': draft.activity,
      'bodyFatPercentage': draft.bodyFat.isEmpty
          ? null
          : double.parse(draft.bodyFat),
    });
  }
}
