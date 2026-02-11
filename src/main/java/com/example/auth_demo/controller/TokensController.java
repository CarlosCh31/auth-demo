package com.example.auth_demo.controller;

import java.util.Base64;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
public class TokensController {

    private final OAuth2AuthorizedClientService clientService;
    private final ObjectMapper mapper = new ObjectMapper();

    public TokensController(OAuth2AuthorizedClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping("/tokens")
    public Map<String, Object> tokens(Authentication auth) throws Exception {

        OAuth2AuthenticationToken oat = (OAuth2AuthenticationToken) auth;
        OidcUser user = (OidcUser) oat.getPrincipal();

        OAuth2AuthorizedClient client = clientService.loadAuthorizedClient(
                oat.getAuthorizedClientRegistrationId(),
                oat.getName());

        String accessToken = (client != null && client.getAccessToken() != null)
                ? client.getAccessToken().getTokenValue()
                : null;

        Object accessTokenBody = decodeJwtBody(accessToken);

        return Map.of(
                "id_token_claims", user.getClaims(),
                "access_token_raw", accessToken,
                "access_token_body", accessTokenBody);
    }

    private Object decodeJwtBody(String jwt) {
        try {
            if (jwt == null)
                return null;
            String[] parts = jwt.split("\\.");
            if (parts.length != 3)
                return "(access token is not a JWT)";
            String bodyJson = new String(Base64.getUrlDecoder().decode(parts[1]));
            return mapper.readValue(bodyJson, Object.class);
        } catch (Exception e) {
            return "(failed to decode access token body: " + e.getMessage() + ")";
        }
    }
}
