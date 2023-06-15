package com.github.aakumykov.yandex_disk_client;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.List;

import io.reactivex.Single;

public interface CloudClient<CloudDirType, CloudFileType, OutputItemType, SortingModeType> {

    // Главные методы

    /**
     * Асинхронно запрашивает список элементов в каталоге, на который указывает ссылка на публичный ресурс,
     * или его подкаталоге remoteDirName, если он не равен null.
     */
    Single<List<OutputItemType>> getListAsync(@NonNull String resourceKey,
                                              @Nullable String subdirName,
                                              @NonNull CloudItemsSortingMode sortingMode,
                                              int startOffset,
                                              int limit);

    /**
     * Синхронно запрашивает список элементов в каталоге, на который указывает ссылка на публичный ресурс,
     * или его подкаталоге remoteDirName, если он не равен null.
     */
    List<OutputItemType> getList(@NonNull String resourceKey,
                                 @Nullable String subdirName,
                                 @NonNull CloudItemsSortingMode sortingMode,
                                 int startOffset,
                                 int limit) throws CloudClientException, IOException;

    Single<String> getItemDownloadLink(@NonNull String resourceKey, @NonNull String remoteFilePath);

    Single<Boolean> checkItemExists(@NonNull String resourceKey, @NonNull String remoteFilePath);




    // Вспомогательные методы

    /**
     * Преобразует тип сортировки списка программы-пользователя во внутренний тип сортировки библиотеки.
     */
    CloudItemsSortingMode convertSortingMode(SortingModeType externalSortingMode);

    String sortingModeToSortingKey(@NonNull CloudItemsSortingMode sortingMode);

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
