package com.example.qr_code_scanner;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface ApiService {
    @GET("urlData/{url}")
    Call<UrlDataResponse> getUrlData(@Path("url") String url);
}

class UrlDataResponse {
    float registrationDuration; // 등록 기간
    float ttl; // TTL 값
}
