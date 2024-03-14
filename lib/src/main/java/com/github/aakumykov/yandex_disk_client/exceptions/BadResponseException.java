package com.github.aakumykov.yandex_disk_client.exceptions;

public class BadResponseException extends CloudClientException {
    public BadResponseException(String errorMsg) {
        super(errorMsg);
    }
    public BadResponseException(Exception e) { super(e); }
}
