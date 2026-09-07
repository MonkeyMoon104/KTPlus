package com.monkey.ktplus.update;

import com.monkey.ktplus.scheduler.PlatformScheduler;
import com.monkey.ktplus.util.OnceLogger;
import com.monkey.ktplus.util.net.KtOkHttp;
import java.net.URI;
import java.util.Locale;
import java.util.Objects;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.Nullable;

public final class UpdateChecker {
    public static final int SPIGOT_RESOURCE_ID = 125998;
    public static final String SPIGOT_LEGACY_UPDATE_URL =
            "https://api.spigotmc.org/legacy/update.php?resource=" + SPIGOT_RESOURCE_ID;

    private static final int CONNECT_TIMEOUT_MS = 4000;
    private static final int READ_TIMEOUT_MS = 4000;
    private static final int MAX_REDIRECTS = 5;

    private final JavaPlugin plugin;
    private final PlatformScheduler scheduler;
    private final String currentVersion;
    private final URI endpoint;
    private final OnceLogger onceLogger;
    private final OkHttpClient httpClient;

    public UpdateChecker(
            JavaPlugin plugin,
            PlatformScheduler scheduler,
            String currentVersion,
            String endpoint,
            OnceLogger onceLogger) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.currentVersion = Objects.requireNonNull(currentVersion, "currentVersion");
        this.endpoint = URI.create(requireHttpsEndpoint(endpoint));
        this.onceLogger = Objects.requireNonNull(onceLogger, "onceLogger");
        this.httpClient = KtOkHttp.client(CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS);
    }

    public static String requireHttpsEndpoint(String endpoint) {
        Objects.requireNonNull(endpoint, "endpoint");
        String trimmed = endpoint.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("endpoint");
        }
        URI uri = URI.create(trimmed);
        String scheme = uri.getScheme();
        if (scheme == null || !"https".equals(scheme.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("update-check.url must be https");
        }
        if (uri.getHost() == null || uri.getHost().isEmpty()) {
            throw new IllegalArgumentException("update-check.url host required");
        }
        return trimmed;
    }

    public void schedule() {
        scheduler.runAsync(() -> {
            String remote = fetchRemoteVersion();
            if (remote == null || remote.isEmpty()) {
                return;
            }
            if (PluginVersion.isNewer(remote, currentVersion)) {
                String message = "[Update] A newer KTPlus build may be available: "
                        + remote
                        + " (running "
                        + currentVersion
                        + ")";
                scheduler.runGlobal(() -> plugin.getLogger().info(message));
            }
        });
    }

    private @Nullable String fetchRemoteVersion() {
        try {
            Request request = new Request.Builder()
                    .url(endpoint.toString())
                    .get()
                    .header("User-Agent", "KTPlus/" + currentVersion)
                    .build();
            try (Response response =
                    KtOkHttp.getFollowingHttpsRedirects(httpClient, request, MAX_REDIRECTS)) {
                if (!response.isSuccessful()) {
                    onceLogger.warnOnce(
                            "update-check-http", "Update check HTTP status " + response.code() + " from " + endpoint);
                    return null;
                }
                String body = KtOkHttp.readBodyUtf8(response);
                if (body == null) {
                    return null;
                }
                int newline = body.indexOf('\n');
                String line = newline >= 0 ? body.substring(0, newline) : body;
                return line.trim().isEmpty() ? null : line.trim();
            }
        } catch (IllegalArgumentException ex) {
            onceLogger.warnOnce("update-check-redirect-scheme", "Update check refused non-https redirect");
            return null;
        } catch (Exception ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("redirect missing Location")) {
                onceLogger.warnOnce("update-check-redirect", "Update check redirect missing Location");
                return null;
            }
            if (ex.getMessage() != null && ex.getMessage().contains("exceeded redirect limit")) {
                onceLogger.warnOnce("update-check-redirect-limit", "Update check exceeded redirect limit");
                return null;
            }
            onceLogger.warnOnce(
                    "update-check-fail", "Update check failed (" + ex.getClass().getSimpleName() + ")");
            return null;
        }
    }
}
