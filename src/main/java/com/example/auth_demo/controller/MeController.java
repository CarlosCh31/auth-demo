package com.example.auth_demo.controller;

import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeController {

    @GetMapping("/me")
    public Map<String, Object> me(Authentication auth) {
        OAuth2AuthenticationToken oat = (OAuth2AuthenticationToken) auth;
        OidcUser user = (OidcUser) oat.getPrincipal();
        return Map.of(
                "name", user.getFullName(),
                "email", user.getEmail(),
                "claims", user.getClaims());
    }
}
