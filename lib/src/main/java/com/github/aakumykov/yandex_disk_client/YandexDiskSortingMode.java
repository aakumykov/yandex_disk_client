package com.github.aakumykov.yandex_disk_client;

public enum YandexDiskSortingMode {
    NAME_DIRECT,
    NAME_REVERSE,

    C_TIME_FROM_OLD_TO_NEW,
    C_TIME_FROM_NEW_TO_OLD,

    M_TIME_FROM_OLD_TO_NEW,
    M_TIME_FROM_NEW_TO_OLD,

    SIZE_FROM_SMALL_TO_BIG,
    SIZE_FROM_BIG_TO_SMALL
}
