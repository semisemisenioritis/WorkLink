package com.example.worklink.api;

import android.content.Context;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ApiClient {
    private static final String BASE_URL = "http://192.168.1.12:8000/";
    private static Retrofit retrofit = null;

    public static ApiService getService(Context context) {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            final SessionManager sessionManager = new SessionManager(context.getApplicationContext());

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .addInterceptor(new Interceptor() {
                        @Override
                        public Response intercept(Chain chain) throws IOException {
                            Request original = chain.request();
                            String path = original.url().encodedPath().toLowerCase();
                            
                            Request.Builder requestBuilder = original.newBuilder()
                                    .header("Accept", "application/json");

                            if (original.body() != null) {
                                requestBuilder.header("Content-Type", "application/json");
                            }

                            // Public endpoints: OTP, Login, Register
                            // /work/users/ POST now requires authentication after login
                            boolean isPublic = path.contains("/auth/login") || 
                                             path.contains("/auth/register") ||
                                             path.contains("/auth/send-otp") ||
                                             path.contains("/auth/verify-otp-register");

                            if (isPublic) {
                                requestBuilder.removeHeader("Authorization");
                            } else {
                                String token = sessionManager.getAccessToken();
                                if (token != null && !token.isEmpty() && !token.equalsIgnoreCase("null")) {
                                    requestBuilder.header("Authorization", "Bearer " + token);
                                }
                            }
                            
                            return chain.proceed(requestBuilder.build());
                        }
                    })
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();
        }
        return retrofit.create(ApiService.class);
    }
}