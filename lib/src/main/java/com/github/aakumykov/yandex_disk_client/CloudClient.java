package com.github.aakumykov.yandex_disk_client;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.aakumykov.yandex_disk_client.exceptions.CloudClientException;
import com.github.aakumykov.yandex_disk_client.exceptions.OperationFailedException;

import java.io.IOException;
import java.util.List;

import io.reactivex.Single;

public interface CloudClient<CloudDirType, CloudFileType, OutputItemType, AppSortingMode> {

    // Операции с авторизацией (над приватными ресурсами)

    List<OutputItemType> listDir(String path) throws IOException, CloudClientException;

    List<OutputItemType> listDir(String path, AppSortingMode sortingMode) throws IOException, CloudClientException;

    List<OutputItemType> listDir(String path,
                                 AppSortingMode sortingMode,
                                 int startOffset,
                                 int limit) throws IOException, CloudClientException;


    void createDir(String dirNameOrPath) throws IOException, OperationFailedException;


    String getLinkForUpload(String path) throws IOException, CloudClientException;

    // Операции без авторизации (над публичными ресурсами)

    Single<List<OutputItemType>> getListAsync(@NonNull String resourceKey,
                                              @Nullable String subdirName,
                                              @NonNull AppSortingMode sortingMode,
                                              int startOffset,
                                              int limit);

    List<OutputItemType> getList(@NonNull String resourceKey,
                                 @Nullable String subdirName,
                                 @NonNull AppSortingMode sortingMode,
                                 int startOffset,
                                 int limit) throws CloudClientException, IOException;

    Single<String> getItemDownloadLink(@NonNull String resourceKey, @NonNull String remoteFilePath);

    Single<Boolean> checkItemExists(@NonNull String resourceKey, @NonNull String remoteFilePath);




    // Вспомогательные методы

    /**
     * Преобразует тип сортировки программы, использующей библиотеку библиотеку,
     * во внутренний тип сортировки библиотеки.
     */
    YandexDiskSortingMode appToDiskSortingMode(AppSortingMode appSortingMode);


    /**
     * Преобразует внутренний тип сортировки библиотеки в тот параметр сортировки облачного API.
     */
    String libraryToCloudSortingMode(@NonNull YandexDiskSortingMode yandexDiskSortingMode);

    List<OutputItemType> extractCloudItemsFromCloudDir(CloudDirType cloudResource);

    OutputItemType cloudItemToLocalItem(CloudFileType cloudFileType);

    String cloudFileToString(CloudFileType cloudFileType);
}
