package com.github.aakumykov.yandex_disk_client;

import com.yandex.disk.rest.json.Link;
import com.yandex.disk.rest.json.Resource;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PUT;
import retrofit2.http.Query;

interface YandexDiskApi {

    // Приватныя ресурсы

    @GET("resources")
    Call<Resource> getResourceByPath(@Header("Authorization") String authToken,
                                     @Query("path") String path);

    @GET("resources")
    Call<Resource> getResourceByPath(@Header("Authorization") String authToken,
                                     @Query("path") String path,
                                     @Query("sort") String sort);

    @GET("resources")
    Call<Resource> getResourceByPath(@Header("Authorization") String authToken,
                                     @Query("path") String path,
                                     @Query("sort") String sort,
                                     @Query("offset") int offset,
                                     @Query("limit") int limit);

    @PUT("resources")
    Call<Resource> createDirectory(@Header("Authorization") String authToken,
                                   @Query("path") String dirNameOrPath);

    @GET("resources/upload")
    Call<Link> getLinkForUpload(@Header("Authorization") String authToken,
                                @Query("path") String path);

    // Публичныя ресурсы

    @GET("public/resources")
    Call<Resource> getPublicResource(@Query("public_key") String publicKey,
                                     @Query("path") String path);

    @GET("public/resources")
    Call<Resource> getPublicResourceWithContentList(@Query("public_key") String publicKey,
                                                    @Query("path") String path,
                                                    @Query("sort") String sortingKey,
                                                    @Query("offset") int offsetFromListStart,
                                                    @Query("limit") int limit);

    @GET("public/resources/download")
    Call<Link> getPublicFileDownloadLink(@Query("public_key") String publicKey,
                                         @Query("path") String filePath);
}
