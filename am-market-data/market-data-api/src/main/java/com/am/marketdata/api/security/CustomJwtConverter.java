package com.am.marketdata.api.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class CustomJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
        UserContext userContext = extractUserContext(jwt);
        return new CustomJwtAuthenticationToken(jwt, userContext, authorities);
    }

    private UserContext extractUserContext(Jwt jwt) {
        String userId = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        String name = jwt.getClaimAsString("name");
        String picture = jwt.getClaimAsString("picture");
        String googleId = jwt.getClaimAsString("google_id");
        List<String> scopes = jwt.getClaimAsStringList("scopes");

        return new UserContext(userId, email, name, picture, googleId, scopes);
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        List<String> scopes = jwt.getClaimAsStringList("scopes");
        if (scopes == null) {
            return new ArrayList<>();
        }
        return scopes.stream()
                .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope))
                .collect(Collectors.toList());
    }
}
