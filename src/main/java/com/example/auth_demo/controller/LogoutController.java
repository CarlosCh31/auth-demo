package com.example.auth_demo.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
public class LogoutController {

    @Value("${spring.security.oauth2.client.provider.auth0.issuer-uri}")
    private String issuer;

    @Value("${spring.security.oauth2.client.registration.auth0.client-id}")
    private String clientId;

    // Logout local ONLY
    @GetMapping("/logout-local")
    public void logoutLocal(HttpServletRequest request, HttpServletResponse response, Authentication auth)
            throws Exception {
        new SecurityContextLogoutHandler().logout(request, response, auth);
        response.sendRedirect("/home.html?loggedOut=1");
    }

    // Logout completo (local + Auth0 SSO) para cambiar usuario
    @GetMapping("/logout-auth0")
    public void logoutAuth0(HttpServletRequest request,
            HttpServletResponse response,
            Authentication auth) throws Exception {

        new SecurityContextLogoutHandler().logout(request, response, auth);

        String baseUrl = getBaseUrl(request);
        String returnTo = baseUrl + "/home.html?loggedOut=1";

        String logoutUrl = issuer + "v2/logout" +
                "?client_id=" + url(clientId) +
                "&returnTo=" + url(returnTo);

        response.sendRedirect(logoutUrl);
    }

    private String getBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int port = request.getServerPort();

        boolean isDefaultPort = ("http".equals(scheme) && port == 80) ||
                ("https".equals(scheme) && port == 443);

        return isDefaultPort
                ? scheme + "://" + serverName
                : scheme + "://" + serverName + ":" + port;
    }

    private String url(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
