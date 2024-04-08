package com.example.biosec.network;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface YourApiService {
    @POST("register")
    Call<Void> registerDevice(@Body RequestBody requestBody);

    @POST("unlock")
    Call<Void> unlockFolders(RequestBody requestBody);
}
