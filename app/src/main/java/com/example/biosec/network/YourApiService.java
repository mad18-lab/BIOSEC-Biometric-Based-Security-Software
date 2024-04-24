package com.example.biosec.network;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface YourApiService {
    @POST("/connect")
    Call<ResponseBody> establishConnection(@Body RequestBody requestData);

    @POST("unlock")
    Call<Void> unlockFolders(RequestBody requestBody);
}
