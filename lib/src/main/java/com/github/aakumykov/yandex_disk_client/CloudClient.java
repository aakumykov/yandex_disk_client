package com.github.aakumykov.yandex_disk_client;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.List;

import io.reactivex.Single;

public interface CloudClient<CloudDirType, CloudFileType, OutputItemType> {

    enum SortingMode {
        NAME_DIRECT,
        NAME_REVERSE,
        C_TIME_FROM_OLD_TO_NEW,
        C_TIME_FROM_NEW_TO_OLD,
    }

    // Главные методы
    Single<List<OutputItemType>> getListAsync(@NonNull String resourceKey,
                                              @Nullable String subdirName,
                                              @NonNull SortingMode sortingMode,
                                              int startOffset,
                                              int limit);

    List<OutputItemType> getList(@NonNull String resourceKey,
                                 @Nullable String subdirName,
                                 @NonNull SortingMode sortingMode,
                                 int startOffset,
                                 int limit) throws CloudClientException, IOException;

    Single<String> getItemDownloadLink(@NonNull String resourceKey, @NonNull String remoteFilePath);

    Single<Boolean> checkItemExists(@NonNull String resourceKey, @NonNull String remoteFilePath);




    // Вспомогательные методы
    String sortingModeToSortingKey(@NonNull SortingMode sortingMode);

    List<OutputItemType> extractCloudItemsFromCloudDir(CloudDirType cloudResource);

    OutputItemType cloudItemToLocalItem(CloudFileType cloudFileType);

    String cloudFileToString(CloudFileType cloudFileType);

    
    abstract class CloudClientException extends Exception {
        public CloudClientException() {
        }

        public CloudClientException(String message) {
            super(message);
        }
    }
    
    class BadResponseException extends CloudClientException {
        public BadResponseException(String errorMsg) {
            super(errorMsg);
        }
    }
    
    class NullPayloadException extends CloudClientException {
    }
}
