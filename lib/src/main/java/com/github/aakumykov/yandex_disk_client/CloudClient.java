package com.github.aakumykov.yandex_disk_client;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.List;

import io.reactivex.Single;

public interface CloudClient<CloudDirType, CloudFileType, OutputItemType, SortingModeType> {

    // Операции с авторизацией (над приватными ресурсами)

    List<OutputItemType> listDir(String path) throws IOException, CloudClientException;


    void createDir(String dirNameOrPath) throws IOException, OperationFailedException;


    String getLinkForUpload(String path) throws IOException, CloudClientException;

    // Операции без авторизации (над публичными ресурсами)

    Single<List<OutputItemType>> getListAsync(@NonNull String resourceKey,
                                              @Nullable String subdirName,
                                              @NonNull SortingModeType sortingMode,
                                              int startOffset,
                                              int limit);

    List<OutputItemType> getList(@NonNull String resourceKey,
                                 @Nullable String subdirName,
                                 @NonNull SortingModeType sortingMode,
                                 int startOffset,
                                 int limit) throws CloudClientException, IOException;

    Single<String> getItemDownloadLink(@NonNull String resourceKey, @NonNull String remoteFilePath);

    Single<Boolean> checkItemExists(@NonNull String resourceKey, @NonNull String remoteFilePath);




    // Вспомогательные методы

    /**
     * Преобразует тип сортировки программы, использующей библиотеку библиотеку,
     * во внутренний тип сортировки библиотеки.
     */
    LibrarySortingMode externalToLibrarySortingMode(SortingModeType externalSortingMode);

    /**
     * Преобразует внутренний тип сортировки библиотеки в тот параметр сортировки, что
     * передаётся облачному API.
     */
    String libraryToCloudSortingMode(@NonNull LibrarySortingMode sortingMode);

    List<OutputItemType> extractCloudItemsFromCloudDir(CloudDirType cloudResource);

    OutputItemType cloudItemToLocalItem(CloudFileType cloudFileType);

    String cloudFileToString(CloudFileType cloudFileType);


    // Классы исключений

    abstract class CloudClientException extends Exception {
        public CloudClientException() {}
        public CloudClientException(String message) {
            super(message);
        }
    }
    
    class BadResponseException extends CloudClientException {
        public BadResponseException(String errorMsg) {
            super(errorMsg);
        }
    }
    
    class NullPayloadException extends CloudClientException {}

    class OperationFailedException extends CloudClientException {
        public OperationFailedException(String message) {
            super(message);
        }
    }
}
