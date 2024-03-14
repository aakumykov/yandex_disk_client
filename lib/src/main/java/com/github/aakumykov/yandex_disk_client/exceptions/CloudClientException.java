package com.github.aakumykov.yandex_disk_client.exceptions;

public abstract class CloudClientException extends Exception {
    public CloudClientException() {}
    public CloudClientException(String message) {
        super(message);
    }
    public CloudClientException(Exception e) {
        super(e);
    }
}
