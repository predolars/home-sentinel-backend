package org.com.homesentinelbackend.models.graphql.variables;

public record SendOtpVariables(
    int recordId,
    String otpHash
) {}
