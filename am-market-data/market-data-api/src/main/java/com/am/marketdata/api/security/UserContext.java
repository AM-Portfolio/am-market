package com.am.marketdata.api.security;

import java.util.Collections;
import java.util.List;

public class UserContext {
    private final String userId;
    private final String email;
    private final String name;
    private final String picture;
    private final String googleId;
    private final List<String> scopes;

    public UserContext(String userId, String email, String name, String picture, String googleId, List<String> scopes) {
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.picture = picture;
        this.googleId = googleId;
        this.scopes = scopes != null ? scopes : Collections.emptyList();
    }

    public String getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public String getPicture() {
        return picture;
    }

    public String getGoogleId() {
        return googleId;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public boolean isAdmin() {
        return scopes.contains("admin");
    }
}
