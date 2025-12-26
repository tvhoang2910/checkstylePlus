package com.checkstyleplus.utils;

import java.nio.file.*;
import java.security.MessageDigest;
import java.util.Optional;

/**
 * Utility class for caching LLM responses on disk.
 * Cache files are stored under ~/.llm-checks-cache/
 */
public class CacheUtils {

    private static final String CACHE_DIR_NAME = ".llm-checks-cache";

    /**
     * Compute SHA-256 hash of the input string. Used as cache key.
     */
    public static String sha256(String input) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Read the cached response for the given key (if it exists).
     */
    public static Optional<String> readCache(String key) {
        try {
            Path f = getCacheDir().resolve(key + ".json");
            if (Files.exists(f)) {
                return Optional.of(new String(Files.readAllBytes(f)));
            }
        } catch (Exception e) {
            System.err.println("CacheUtils: Failed to read cache - " + e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Write the cached response for the given key.
     */
    public static void writeCache(String key, String resp) {
        try {
            Path f = getCacheDir().resolve(key + ".json");
            Files.write(f, resp.getBytes());
        } catch (Exception e) {
            System.err.println("CacheUtils: Failed to write cache - " + e.getMessage());
        }
    }

    private static Path getCacheDir() throws Exception {
        Path p = Paths.get(System.getProperty("user.home"), CACHE_DIR_NAME);
        if (!Files.exists(p)) Files.createDirectories(p);
        return p;
    }
}
