package com.monkey.ktplus.lib.download;

import com.monkey.ktplus.util.net.KtOkHttp;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public final class HttpArtifactDownloader implements LibraryDownloader {
    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 10_000;
    private static final int MAX_REDIRECTS = 5;

    private final OkHttpClient httpClient = KtOkHttp.client(CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS);

    @Override
    public void download(URI source, Path destination, String userAgent) throws IOException {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(destination, "destination");
        Objects.requireNonNull(userAgent, "userAgent");
        Request request = new Request.Builder()
                .url(source.toString())
                .get()
                .header("Accept", "application/java-archive, application/octet-stream")
                .header("User-Agent", userAgent)
                .build();
        try (Response response = KtOkHttp.getFollowingHttpsRedirects(httpClient, request, MAX_REDIRECTS)) {
            if (!response.isSuccessful()) {
                throw new IOException("download returned HTTP " + response.code() + " from " + source);
            }
            try (InputStream input = response.body().byteStream();
                    OutputStream output = Files.newOutputStream(destination)) {
                byte[] buffer = new byte[16_384];
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    if (read > 0) {
                        output.write(buffer, 0, read);
                    }
                }
            }
        }
    }
}
