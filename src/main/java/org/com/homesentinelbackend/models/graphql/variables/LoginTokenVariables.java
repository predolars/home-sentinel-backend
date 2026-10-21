package org.com.homesentinelbackend.models.graphql.variables;

public record LoginTokenVariables(
    String id,
    String country,
    String callby,
    String lang,
    String user,
    String password
) {
    public static LoginTokenVariables of(String id, String user, String password) {
        return new LoginTokenVariables(id, "ES", "OWP_10", "es", user, password);
    }
}
