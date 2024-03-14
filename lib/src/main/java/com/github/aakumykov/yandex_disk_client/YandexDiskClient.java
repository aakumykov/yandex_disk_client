package com.github.aakumykov.yandex_disk_client;

import static com.github.aakumykov.argument_utils.ArgumentUtils.checkNotNull;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.aakumykov.yandex_disk_client.exceptions.BadResponseException;
import com.github.aakumykov.yandex_disk_client.exceptions.CloudClientException;
import com.github.aakumykov.yandex_disk_client.exceptions.NullPayloadException;
import com.github.aakumykov.yandex_disk_client.exceptions.OperationFailedException;
import com.yandex.disk.rest.json.Link;
import com.yandex.disk.rest.json.Resource;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import io.reactivex.Single;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public abstract class YandexDiskClient<OutputItemType,SortingModeType> implements CloudClient<Resource, Resource, OutputItemType, SortingModeType> {

    private static final String TAG = YandexDiskClient.class.getSimpleName();
    private static final String SLASH = "/";
    private final YandexDiskApi mYandexDiskApi;
    @Nullable private String mAuthToken = null;


    public YandexDiskClient() {
        mYandexDiskApi = getWebApi();
    }


    public YandexDiskClient(@NonNull final String authToken) {
        this();
        mAuthToken = authToken;
    }


    public void setAuthToken(@Nullable String authToken) {
        mAuthToken = authToken;
    }


    /**
     * Запрашивает список файлов в приватном каталоге. Для работы метода требуется передать клиенту
     * токен авторизации методом {@link #setAuthToken(String authToken)}
     * @param path Путь к каталогу.
     * @return Список файлов (объектов типа OutputItemType) в виде коллекции List.
     */
    @Override
    public List<OutputItemType> listDir(String path) throws IOException, CloudClientException {
        Response<Resource> response = mYandexDiskApi.getResourceByPath(mAuthToken, path).execute();
        Resource resource = response2resource(response);
        return extractCloudItemsFromCloudDir(resource);
    }



    @Override
    public void createDir(String dirNameOrPath) throws IOException, OperationFailedException {
        final Response<Resource> response = mYandexDiskApi.createDirectory(mAuthToken, dirNameOrPath).execute();
        if (!response.isSuccessful()) {

            /*// Проверил JSON-сообщение с подробностями ошибки.
            try (ResponseBody responseBody = response.errorBody()) {
                String s = (null != responseBody) ? responseBody.string() : "";
                Log.d(TAG, "createDir: "+s);
            }*/

            throw new OperationFailedException(codeAndMessage(response));
        }
    }

    @Override
    public String getLinkForUpload(String path) throws IOException, CloudClientException {
        final Response<Link> response = mYandexDiskApi.getLinkForUpload(mAuthToken, path).execute();

        if (!response.isSuccessful())
            throw new BadResponseException(codeAndMessage(response));

        final Link link = response.body();
        if (null == link)
            throw new NullPayloadException();

        return link.getHref();
    }

    /**
     * Запрашивает список элементов в каталоге, на который указывает ссылка на публичный ресурс,
     * или его подкаталоге remoteDirName, если он не равен null.
     */
    @Override
    public Single<List<OutputItemType>> getListAsync(@NonNull String resourceKey,
                                        @Nullable String subdirName,
                                        @NonNull SortingModeType sortingMode,
                                        @IntRange(from = 0) int startOffset,
                                        int limit) {

        return Single.fromCallable(() -> getList(resourceKey, subdirName, sortingMode, startOffset, limit));
    }

    /**
     * Запрашивает список элементов в каталоге, на который указывает ссылка на публичный ресурс,
     * или его подкаталоге remoteDirName, если он не равен null.
     */
    @Override
    public List<OutputItemType> getList(@NonNull String resourceKey,
                           @Nullable String subdirName,
                           @NonNull SortingModeType sortingMode,
                           @IntRange(from = 0) int startOffset,
                           int limit) throws CloudClientException, IOException {

        // Проверка аргументов
        checkNotNull(resourceKey);
        checkNotNull(sortingMode);
        if (startOffset < 0)
            throw new IllegalArgumentException("Start offset must cannot be lesser than zero");

        // Определение эффективного имени подкаталога
        final String dirName = (null != subdirName) ? subdirName : "";

        // Запрос к API
        final Call<Resource> call = mYandexDiskApi.getPublicResourceWithContentList(
                resourceKey,
                dirName,
                libraryToCloudSortingMode(appToDiskSortingMode(sortingMode)),
                startOffset,
                limit
        );

        return extractCloudItemsFromCloudDir(
                response2resource(
                        call.execute()
                )
        );
    }

    @Override
    public Single<String> getItemDownloadLink(@NonNull String resourceKey,
                                              @NonNull String remoteFileOrDirPath) {

        checkNotNull(resourceKey);
        checkNotNull(remoteFileOrDirPath);

        return Single.create(emitter -> {

            final Call<Link> call = mYandexDiskApi.getPublicFileDownloadLink(resourceKey, fixFilePathStartingSlash(remoteFileOrDirPath));

            call.enqueue(new Callback<Link>() {
                @Override
                public void onResponse(@NonNull Call<Link> call, @NonNull Response<Link> response) {
                    final Link link = response.body();
                    if (null != link)
                        emitter.onSuccess(link.getHref());
                    else
                        emitter.onError(new DownloadLinkIsNullException());
                }

                @Override
                public void onFailure(@NonNull Call<Link> call, @NonNull Throwable t) {
                    emitter.onError(t);
                }
            });
        });
    }

    @Override
    public Single<Boolean> checkItemExists(@NonNull String resourceKey, @NonNull String remoteFileOfDirPath) {

        checkNotNull(resourceKey);
        checkNotNull(remoteFileOfDirPath);

        final Call<Resource> call = mYandexDiskApi.getPublicResource(resourceKey, fixFilePathStartingSlash(remoteFileOfDirPath));

        return Single.create(emitter -> call.enqueue(new Callback<Resource>() {
            @Override
            public void onResponse(@NonNull Call<Resource> call1, @NonNull Response<Resource> response) {
                final Resource resource = response.body();
                emitter.onSuccess(null != resource);
            }

            @Override
            public void onFailure(@NonNull Call<Resource> call1, @NonNull Throwable t) {
                emitter.onError(t);
            }
        }));
    }

    @Override
    public String libraryToCloudSortingMode(@NonNull YandexDiskSortingMode sortingMode) {
        switch (sortingMode) {
            case NAME_DIRECT:
                return "name";
            case NAME_REVERSE:
                return "-name";
            case C_TIME_FROM_NEW_TO_OLD:
                return "-ctime";
            case C_TIME_FROM_OLD_TO_NEW:
                return "ctime";
            default:
                throw new IllegalArgumentException("Неизвестное значение аргумента: "+sortingMode);
        }
    }

    @Override
    public List<OutputItemType> extractCloudItemsFromCloudDir(Resource resource) {

        if (!resource.isDir())
            throw new IllegalArgumentException("Resource is not a dir: "+cloudFileToString(resource));

        return resource.getResourceList().getItems()
                .stream()
                .map(this::cloudItemToLocalItem)
                .collect(Collectors.toList());
    }

    @Override
    public abstract OutputItemType cloudItemToLocalItem(Resource resource);

    @Override
    public abstract String cloudFileToString(Resource resource);


    public static class YandexDiskClientException extends Throwable {}

    public static class ResourceIsNullException extends YandexDiskClientException {
    }

    public static class DownloadLinkIsNullException extends YandexDiskClientException {}


    private static YandexDiskApi getWebApi() {

        OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request request = chain.request().newBuilder()
                            .addHeader("Content-Type", "application/json")
                            .build();
                    return chain.proceed(request);
                });

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://cloud-api.yandex.net/v1/disk/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClientBuilder.build())
                .build();

        return retrofit.create(YandexDiskApi.class);
    }


    private String fixFilePathStartingSlash(String filePath) {
        return filePath.startsWith(SLASH) ? filePath : SLASH + filePath;
    }


    private Resource response2resource(Response<Resource> response) throws BadResponseException, NullPayloadException {
        if (!response.isSuccessful())
            throw new BadResponseException(response.code()+": "+response.message());

        final Resource resource = response.body();
        if (null == resource)
            throw new NullPayloadException();

        return resource;
    }

    /*private OperationFailedException operationFailedException(Response<Resource> response) {
        return new OperationFailedException(response.code() + ": " + response.message())
    }*/

    private String codeAndMessage(Response<?> response) {
        return response.code() + ": " + response.message();
    }
}
