package com.example.auth_demo.config;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Value("${auth.audience}")
    private String audience;

    @Value("${auth.issuer}")
    private String issuer;

    @Bean
    SecurityFilterChain webSecurity(HttpSecurity http, ClientRegistrationRepository clients) throws Exception {

        OAuth2AuthorizationRequestResolver resolver = newAudienceResolver(clients);

        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // publics
                        .requestMatchers("/", "/home.html", "/css/**", "/js/**", "/images/**", "/public/**",
                                "/logout-auth0")
                        .permitAll()

                        // tokens page
                        .requestMatchers("/tokens").authenticated()

                        // protected resources
                        .requestMatchers("/api/r1/**").hasAuthority("SCOPE_read:r1")
                        .requestMatchers("/api/r2/**").hasAuthority("SCOPE_read:r2")

                        .anyRequest().authenticated())
                // Login
                .oauth2Login(oauth -> oauth
                        .authorizationEndpoint(a -> a.authorizationRequestResolver(resolver)))
                // Resource Server
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter())))
                .logout(logout -> logout.disable())
                .build();
    }

    private OAuth2AuthorizationRequestResolver newAudienceResolver(ClientRegistrationRepository clients) {
        DefaultOAuth2AuthorizationRequestResolver defaultResolver = new DefaultOAuth2AuthorizationRequestResolver(
                clients, "/oauth2/authorization");

        return new OAuth2AuthorizationRequestResolver() {
            @Override
            public OAuth2AuthorizationRequest resolve(jakarta.servlet.http.HttpServletRequest request) {
                OAuth2AuthorizationRequest req = defaultResolver.resolve(request);
                return customize(req);
            }

            @Override
            public OAuth2AuthorizationRequest resolve(jakarta.servlet.http.HttpServletRequest request,
                    String clientRegistrationId) {
                OAuth2AuthorizationRequest req = defaultResolver.resolve(request, clientRegistrationId);
                return customize(req);
            }

            private OAuth2AuthorizationRequest customize(OAuth2AuthorizationRequest req) {
                if (req == null)
                    return null;

                Map<String, Object> extraParams = new HashMap<>(req.getAdditionalParameters());
                extraParams.put("audience", audience);

                return OAuth2AuthorizationRequest.from(req)
                        .additionalParameters(extraParams)
                        .build();
            }
        };
    }

    private Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Set<String> perms = new HashSet<>();

            Object permissionsClaim = jwt.getClaims().get("permissions");
            if (permissionsClaim instanceof Collection<?> c) {
                for (Object x : c) {
                    if (x != null)
                        perms.add(x.toString());
                }
            }

            String scope = jwt.getClaimAsString("scope");
            if (scope != null && !scope.isBlank()) {
                perms.addAll(Arrays.asList(scope.split("\\s+")));
            }

            return perms.stream()
                    .filter(s -> s.contains(":"))
                    .map(p -> new SimpleGrantedAuthority("SCOPE_" + p))
                    .collect(Collectors.toSet());
        });
        return converter;
    }

    @Bean
    JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = JwtDecoders.fromIssuerLocation(issuer);

        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuer);
        OAuth2TokenValidator<Jwt> withAudience = jwt -> {
            List<String> aud = jwt.getAudience();
            if (aud != null && aud.contains(audience)) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "Missing/invalid audience", null));
        };

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withIssuer, withAudience));
        return decoder;
    }
}
