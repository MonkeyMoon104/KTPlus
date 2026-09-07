package com.monkey.ktplus.review;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.monkey.ktplus.util.net.KtOkHttp;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentHashMap;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jspecify.annotations.Nullable;

public final class ReviewApiClient {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 8000;
    private static final int MAX_REDIRECTS = 5;
    private static final int MAX_STARRED_PAGES = 8;
    private static final int MAX_REVIEW_PAGES = 5;

    private final String userAgent;
    private final OkHttpClient httpClient;
    private final ConcurrentHashMap<Integer, String> authorNameCache = new ConcurrentHashMap<Integer, String>();

    public ReviewApiClient(String pluginVersion) {
        this.userAgent = "KTPlus/" + Objects.requireNonNull(pluginVersion, "pluginVersion");
        this.httpClient = KtOkHttp.client(CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS);
    }

    public ReviewStats fetchGitHubStats(String owner, String repo) throws Exception {
        JsonNode root = getJson("https://api.github.com/repos/" + enc(owner) + "/" + enc(repo));
        long stars = root.path("stargazers_count").asLong(0L);
        return new ReviewStats(stars, 0.0D);
    }

    public ReviewStats fetchSpigotStats(int resourceId) throws Exception {
        JsonNode root = getJson("https://api.spiget.org/v2/resources/" + resourceId);
        JsonNode rating = root.path("rating");
        return new ReviewStats(rating.path("count").asLong(0L), rating.path("average").asDouble(0.0D));
    }

    public boolean hasGitHubStar(String owner, String repo, String username) throws Exception {
        Objects.requireNonNull(username, "username");
        String target = owner + "/" + repo;
        for (int page = 1; page <= MAX_STARRED_PAGES; page++) {
            String url = "https://api.github.com/users/"
                    + enc(username)
                    + "/starred?per_page=100&page="
                    + page;
            JsonNode array = getJson(url);
            if (!array.isArray() || array.isEmpty()) {
                return false;
            }
            for (JsonNode node : array) {
                if (target.equalsIgnoreCase(node.path("full_name").asText(""))) {
                    return true;
                }
            }
            if (array.size() < 100) {
                return false;
            }
        }
        return false;
    }

    public boolean hasSpigotReview(int resourceId, String username) throws Exception {
        return findSpigotReviewStars(resourceId, username).isPresent();
    }

    public OptionalInt findSpigotReviewStars(int resourceId, String username) throws Exception {
        Objects.requireNonNull(username, "username");
        Optional<Integer> authorId = resolveSpigotAuthorId(username);
        if (authorId.isEmpty()) {
            return OptionalInt.empty();
        }
        int targetId = authorId.get().intValue();
        for (int page = 1; page <= MAX_REVIEW_PAGES; page++) {
            String url = "https://api.spiget.org/v2/resources/"
                    + resourceId
                    + "/reviews?size=100&page="
                    + page
                    + "&sort=-date";
            JsonNode array = getJson(url);
            if (!array.isArray() || array.isEmpty()) {
                return OptionalInt.empty();
            }
            for (JsonNode node : array) {
                int id = node.path("author").path("id").asInt(-1);
                boolean match = id == targetId;
                if (!match) {
                    String cached = authorNameCache.get(Integer.valueOf(id));
                    match = cached != null && cached.equalsIgnoreCase(username);
                }
                if (match) {
                    int stars = (int) Math.round(node.path("rating").path("average").asDouble(0.0D));
                    return OptionalInt.of(Math.max(1, Math.min(5, stars)));
                }
            }
            if (array.size() < 100) {
                return OptionalInt.empty();
            }
        }
        return OptionalInt.empty();
    }

    private Optional<Integer> resolveSpigotAuthorId(String username) throws Exception {
        JsonNode array = getJson("https://api.spiget.org/v2/search/authors/" + enc(username) + "?size=10");
        if (!array.isArray()) {
            return Optional.empty();
        }
        for (JsonNode node : array) {
            String name = node.path("name").asText("");
            int id = node.path("id").asInt(-1);
            if (id > 0 && !name.isEmpty()) {
                authorNameCache.put(Integer.valueOf(id), name.toLowerCase(Locale.ROOT));
            }
            if (id > 0 && username.equalsIgnoreCase(name)) {
                return Optional.of(Integer.valueOf(id));
            }
        }
        return Optional.empty();
    }

    private JsonNode getJson(String url) throws Exception {
        String body = getBody(url);
        if (body == null || body.isEmpty()) {
            return MAPPER.createObjectNode();
        }
        return MAPPER.readTree(body);
    }

    private @Nullable String getBody(String endpoint) throws Exception {
        Request.Builder builder = new Request.Builder().url(endpoint).get().header("User-Agent", userAgent);
        String host = hostOf(endpoint);
        if (host != null && host.endsWith("github.com")) {
            builder.header("Accept", "application/vnd.github+json");
            builder.header("X-GitHub-Api-Version", "2022-11-28");
        } else {
            builder.header("Accept", "application/json");
        }
        try (Response response =
                KtOkHttp.getFollowingHttpsRedirects(httpClient, builder.build(), MAX_REDIRECTS)) {
            if (!response.isSuccessful()) {
                return null;
            }
            return KtOkHttp.readBodyUtf8(response);
        }
    }

    private static @Nullable String hostOf(String endpoint) {
        try {
            return java.net.URI.create(endpoint).getHost();
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
