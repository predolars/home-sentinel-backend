package org.com.homesentinelbackend.models.graphql;

public record GraphQLRequest<T>(
    String operationName,
    T variables,
    String query
) {}
