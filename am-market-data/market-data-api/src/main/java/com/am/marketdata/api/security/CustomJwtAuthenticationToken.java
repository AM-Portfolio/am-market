package com.am.marketdata.api.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;

public class CustomJwtAuthenticationToken extends AbstractAuthenticationToken {
    private final Jwt jwt;
    private final UserContext userContext;

    public CustomJwtAuthenticationToken(Jwt jwt, UserContext userContext,
            Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.jwt = jwt;
        this.userContext = userContext;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return jwt;
    }

    @Override
    public Object getPrincipal() {
        return userContext;
    }

    public Jwt getJwt() {
        return jwt;
    }

    public UserContext getUserContext() {
        return userContext;
    }
}
