package org.com.homesentinelbackend.models.auth;

public record VerisureAuthHeader(
    long loginTimestamp,
    String user,
    String id,
    String country,
    String lang,
    String callby,
    String hash
) {
    public static VerisureAuthHeader of(long loginTimestamp, String user, String id, String hash) {
        return new VerisureAuthHeader(loginTimestamp, user, id, "ES", "es", "OWP_10", hash);
    }
}
