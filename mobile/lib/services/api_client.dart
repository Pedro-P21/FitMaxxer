import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:http/http.dart' as http;

class ApiException implements Exception {
  const ApiException(this.message, {this.status});
  final String message;
  final int? status;
  @override
  String toString() => message;
}

class ApiClient {
  ApiClient({http.Client? client, String? baseUrl})
    : _client = client ?? http.Client(),
      baseUrl =
          baseUrl ??
          const String.fromEnvironment('API_BASE_URL', defaultValue: '') {
    if (this.baseUrl.isEmpty) {
      this.baseUrl = Platform.isAndroid
          ? 'http://10.0.2.2:8080'
          : 'http://127.0.0.1:8080';
    }
  }
  final http.Client _client;
  String baseUrl;
  String? token;
  Future<void> Function()? onUnauthorized;

  Future<Map<String, dynamic>> request(
    String method,
    String path, {
    Map<String, dynamic>? body,
    bool authenticated = true,
  }) async {
    final sentToken = authenticated ? token : null;
    if (authenticated && sentToken == null) {
      throw const ApiException('Please log in again.', status: 401);
    }
    try {
      final request = http.Request(method, Uri.parse('$baseUrl$path'));
      request.headers.addAll({
        'Accept': 'application/json, application/problem+json',
        if (body != null) 'Content-Type': 'application/json',
        if (sentToken != null) 'Authorization': 'Bearer $sentToken',
      });
      if (body != null) request.body = jsonEncode(body);
      final response = await (() async => http.Response.fromStream(
        await _client.send(request),
      ))().timeout(const Duration(seconds: 20));
      if (response.statusCode == 401 &&
          sentToken != null &&
          token == sentToken) {
        await onUnauthorized?.call();
      }
      Map<String, dynamic> data = {};
      if (response.body.isNotEmpty) {
        try {
          data = jsonDecode(response.body) as Map<String, dynamic>;
        } on FormatException {
          throw const ApiException(
            'The server returned an unexpected response.',
          );
        } on TypeError {
          throw const ApiException(
            'The server returned an unexpected response.',
          );
        }
      }
      if (response.statusCode >= 400) {
        throw ApiException(
          response.statusCode >= 500
              ? 'The server is unavailable. Please try again.'
              : data['detail'] as String? ??
                    'The request could not be completed.',
          status: response.statusCode,
        );
      }
      return data;
    } on TimeoutException {
      throw const ApiException('The connection timed out. Please try again.');
    } on SocketException {
      throw const ApiException(
        'Cannot connect. Check your connection and try again.',
      );
    } on http.ClientException {
      throw const ApiException(
        'Cannot connect. Check your connection and try again.',
      );
    }
  }

  void close() => _client.close();
}
