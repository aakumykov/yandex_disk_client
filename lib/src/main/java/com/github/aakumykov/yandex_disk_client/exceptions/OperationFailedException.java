package com.github.aakumykov.yandex_disk_client.exceptions;

public class OperationFailedException extends CloudClientException {
    public OperationFailedException(String message) {
        super(message);
    }
    public OperationFailedException(Exception e) { super(e); }
}
