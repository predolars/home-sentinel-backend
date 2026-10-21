package org.com.homesentinelbackend.models.auth;

public record VerisureSecurityHeader(
    String token,
    String type,
    String otpHash
) {
    public static VerisureSecurityHeader forOtp(String token, String otpHash) {
        return new VerisureSecurityHeader(token, "OTP", otpHash);
    }
}
