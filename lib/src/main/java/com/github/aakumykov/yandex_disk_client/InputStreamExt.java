package com.github.aakumykov.yandex_disk_client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class InputStreamExt {

    public static long copyTo(InputStream in, OutputStream out, int bufferSize) throws IOException {

        long bytesCopied = 0;
        byte[] buffer = new byte[bufferSize];
        int bytesReaded;

        while ((bytesReaded = in.read(buffer)) >= 0) {
            out.write(buffer, 0, bytesReaded);
            bytesCopied += bytesReaded;
        }

        return bytesCopied;
    }
}