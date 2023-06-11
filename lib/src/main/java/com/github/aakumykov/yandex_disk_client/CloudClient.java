package com.github.aakumykov.yandex_disk_client;

import androidx.annotation.NonNull;

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
    Single<List<OutputItemType>> getItemsListAsync(@NonNull String remoteDirName,
                                                   @NonNull SortingMode sortingMode,
                                                   int startOffset,
                                                   int limit);

    List<OutputItemType> getItemsList(@NonNull String remoteDirName,
                                      @NonNull SortingMode sortingMode,
                                      int startOffset,
                                      int limit) throws CloudClientException, IOException;
    
    // TODO: сюда просится метод получения единичного элемента "getItem()", но он пока не нужен.

    Single<String> getItemDownloadLink(@NonNull String remoteFilePath);

    Single<Boolean> checkItemExists(@NonNull String remoteFilePath);




    // Вспомогательные методы
    String sortingModeToSortingKey(@NonNull SortingMode sortingMode);

    List<OutputItemType> extractCloudItemsFromCloudDir(CloudDirType cloudDirResource);

    OutputItemType cloudItemToLocalItem(CloudFileType cloudFileType);

    
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
