package com.github.aakumykov.yandex_disk_client;

import static com.github.aakumykov.argument_utils.ArgumentUtils.checkNotNull;

import androidx.annotation.NonNull;

import com.yandex.disk.rest.json.Link;
import com.yandex.disk.rest.json.Resource;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import io.reactivex.Single;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class YandexDiskCloudClient implements CloudClient<Resource, Resource, String> {

    private static final String SLASH = "/";
    private final YandexDiskApi mYandexDiskApi;
    @NonNull private final String mPublicResourceKey;

    public YandexDiskCloudClient(@NonNull YandexDiskApi yandexDiskApi,
                                 @NonNull String publicResourceKey) {
        mPublicResourceKey = checkNotNull(publicResourceKey);
        mYandexDiskApi = yandexDiskApi;
    }


    @Override
    public Single<List<String>> getItemsListAsync(@NonNull String remoteDirName,
                                                  @NonNull SortingMode sortingMode,
                                                  int startOffset,
                                                  int limit) {
        

        return Single.fromCallable(() -> getItemsList(remoteDirName, sortingMode, startOffset, limit));
    }

    @Override
    public List<String> getItemsList(@NonNull String remoteDirName, 
                                     @NonNull SortingMode sortingMode, 
                                     int startOffset, 
                                     int limit) throws CloudClientException, IOException {
        checkNotNull(remoteDirName);
        checkNotNull(remoteDirName);

        final Call<Resource> call = mYandexDiskApi.getPublicResourceWithContentList(
                mPublicResourceKey,
                "",
                sortingModeToSortingKey(sortingMode),
                startOffset,
                limit
        );

        final Response<Resource> response = call.execute();
        
        if (!response.isSuccessful())
            throw new BadResponseException(response.code()+": "+response.message());

        final Resource resource = response.body();
        if (null == resource)
            throw new NullPayloadException();

        return extractCloudItemsFromCloudDir(resource);
    }

    @Override
    public Single<String> getItemDownloadLink(@NonNull String remoteFileOrDirPath) {

        checkNotNull(remoteFileOrDirPath);

        return Single.create(emitter -> {

            final Call<Link> call = mYandexDiskApi.getPublicFileDownloadLink(mPublicResourceKey, fixStartingSlash(remoteFileOrDirPath));

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
    public Single<Boolean> checkItemExists(@NonNull String remoteFileOfDirPath) {

        checkNotNull(remoteFileOfDirPath);

        final Call<Resource> call = mYandexDiskApi.getPublicResource(mPublicResourceKey, fixStartingSlash(remoteFileOfDirPath));

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
    public String sortingModeToSortingKey(@NonNull SortingMode sortingMode) {
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
    public List<String> extractCloudItemsFromCloudDir(Resource cloudDirResource) {

        return cloudDirResource.getResourceList().getItems()
                .stream()
                .map(this::cloudItemToLocalItem)
                .collect(Collectors.toList());
    }

    @Override
    public String cloudItemToLocalItem(Resource resource) {
        return resource.getName();
    }


    public static class YandexDiskClientException extends Throwable {}

    public static class ResourceIsNullException extends YandexDiskClientException {
    }

    public static class DownloadLinkIsNullException extends YandexDiskClientException {}


    private String fixStartingSlash(String filePath) {
        return filePath.startsWith(SLASH) ? filePath : SLASH + filePath;
    }
}
