package org.com.homesentinelbackend.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.com.homesentinelbackend.models.auth.VerisureAuthHeader;
import org.com.homesentinelbackend.models.auth.VerisurePhone;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class LoginSession {
    private final long loginTimestamp;
    private final String id;
    private String hash;
    private String refreshToken;
    private String otpHash;
    private String capabilities;
    private List<VerisurePhone> availablePhones = new ArrayList<>();

    public LoginSession(String user) {
        this.loginTimestamp = System.currentTimeMillis();
        this.id = "OWP_______________" + user + "_______________" + this.loginTimestamp;
    }

    public VerisureAuthHeader buildAuthHeader(String user) {
        return VerisureAuthHeader.of(System.currentTimeMillis(), user, this.id, this.hash);
    }

    public boolean isAuthenticated() {
        return this.hash != null && !this.hash.isBlank();
    }
}