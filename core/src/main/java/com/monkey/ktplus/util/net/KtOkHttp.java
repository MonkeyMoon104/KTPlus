package com.monkey.ktplus.util.net;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jspecify.annotations.Nullable;

public final class KtOkHttp {
    private static final OkHttpClient SHARED = new OkHttpClient.Builder()
            .followRedirects(false)
            .followSslRedirects(false)
            .retryOnConnectionFailure(true)
            .connectTimeout(Duration.ofSeconds(5))
            .readTimeout(Duration.ofSeconds(10))
            .writeTimeout(Duration.ofSeconds(10))
            .build();

    private KtOkHttp() {}

    public static OkHttpClient shared() {
        return SHARED;
    }

    public static OkHttpClient client(long connectTimeoutMs, long readTimeoutMs) {
        return SHARED.newBuilder()
                .connectTimeout(Duration.ofMillis(Math.max(1L, connectTimeoutMs)))
                .readTimeout(Duration.ofMillis(Math.max(1L, readTimeoutMs)))
                .build();
    }

    public static Response getFollowingHttpsRedirects(
            OkHttpClient client, Request request, int maxRedirects) throws IOException {
        Objects.requireNonNull(client, "client");
        Objects.requireNonNull(request, "request");
        Request currentRequest = request;
        URI currentUri = currentRequest.url().uri();
        for (int redirect = 0; redirect <= maxRedirects; redirect++) {
            Response response = client.newCall(currentRequest).execute();
            if (!response.isRedirect()) {
                return response;
            }
            String location = response.header("Location");
            response.close();
            if (location == null || location.isBlank()) {
                throw new IOException("redirect missing Location from " + currentUri);
            }
            currentUri = HttpRedirectSupport.resolveRedirect(currentUri, location);
            currentRequest = currentRequest.newBuilder().url(currentUri.toString()).build();
        }
        throw new IOException("exceeded redirect limit from " + request.url());
    }

    public static @Nullable String readBodyUtf8(Response response) throws IOException {
        String body = response.body().string();
        return body.isEmpty() ? null : body;
    }
}
