package com.github.aakumykov.yandex_disk_client_demo;

public interface ItemClickListener {
    void onItemClicked(DiskItem diskItem);
    default boolean onItemLongClicked(DiskItem diskItem) { return false; };
}
