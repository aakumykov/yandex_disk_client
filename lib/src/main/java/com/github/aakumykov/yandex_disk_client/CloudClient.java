package com.github.aakumykov.yandex_disk_client;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.aakumykov.okhttp_file_downloader.exceptions.EmptyBodyException;

import java.io.File;
import java.io.IOException;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Single;

public interface CloudClient<CloudDirType, CloudFileType, OutputItemType, SortingModeType> {

    // ====== Методы получения облачных объектов ======

    /**
     * Асинхронно запрашивает список элементов в каталоге, на который указывает ссылка на публичный ресурс,
     * или его подкаталоге remoteDirName, если он не равен null.
     */
    Single<List<OutputItemType>> getListAsync(@NonNull String resourceKey,
                                              @Nullable String subdirName,
                                              @NonNull SortingModeType sortingMode,
                                              int startOffset,
                                              int limit);

    /**
     * Синхронно запрашивает список элементов в каталоге, на который указывает ссылка на публичный ресурс,
     * или его подкаталоге remoteDirName, если он не равен null.
     */
    List<OutputItemType> getList(@NonNull String resourceKey,
                                 @Nullable String subdirName,
                                 @NonNull SortingModeType sortingMode,
                                 int startOffset,
                                 int limit) throws CloudClientException, IOException;

    Single<String> getItemDownloadLink(@NonNull String resourceKey, @NonNull String remoteFilePath);

    Single<Boolean> checkItemExists(@NonNull String resourceKey, @NonNull String remoteFilePath);


    // ====== Методы скачивания файлов ======

    void downloadFileTo(@NonNull String url, @NonNull File targetFile) throws EmptyBodyException, com.github.aakumykov.okhttp_file_downloader.exceptions.BadResponseException, IOException, BadResponseException;
//    File downloadFile(@NonNull String url);
    Completable downloadFileToAsync(@NonNull String url, @NonNull File targetFile);
//    Single<File> downloadFileAsync(@NonNull String url);


    // ====== Вспомогательные методы ======

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


    // Исключения

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
