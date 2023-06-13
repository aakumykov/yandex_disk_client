package com.github.aakumykov.yandex_disk_client_demo;

import static com.github.aakumykov.argument_utils.ArgumentUtils.checkNotNull;

import androidx.annotation.NonNull;

public class DiskItem {
    @NonNull public final String name;
    public final long cTime;

    public DiskItem(@NonNull String name, long cTime) {
        this.name = checkNotNull(name);
        this.cTime = cTime;
    }
}
