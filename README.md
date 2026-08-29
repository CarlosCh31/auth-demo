# Auth Demo

A Spring Boot application demonstrating **OAuth 2.0 / OpenID Connect authentication and permission-based authorization** using **Spring Security and Auth0**.

The project shows how a web application can authenticate users through an external Identity Provider, obtain ID and access tokens, validate JWTs as an OAuth2 Resource Server, and protect backend resources using permissions such as `read:r1` and `read:r2`.

## Features

* OAuth 2.0 Authorization Code login
* OpenID Connect authentication
* Auth0 integration
* ID Token and Access Token handling
* JWT signature and issuer validation
* JWT audience validation
* Permission-based authorization
* OAuth scopes and Auth0 permissions
* Protected REST endpoints
* Resource Server configuration
* Local logout
* Auth0 SSO logout
* Token inspection dashboard
* Public health-check endpoint
* Spring Boot Actuator support

---

## Tech Stack

* **Java 17**
* **Spring Boot 4**
* **Spring Security**
* **Spring Security OAuth2 Client**
* **Spring Security OAuth2 Resource Server**
* **OpenID Connect**
* **OAuth 2.0**
* **JWT**
* **Auth0**
* **Maven**
* HTML / CSS / JavaScript

---

# Architecture

The application works both as an:

1. **OAuth2 Client** — redirects users to Auth0, completes the Authorization Code flow, and creates an authenticated web session.
2. **OAuth2 Resource Server** — validates JWT access tokens and protects API resources according to their permissions.

```text
┌──────────────┐
│    Browser   │
└──────┬───────┘
       │
       │ Open protected page
       ▼
┌──────────────────────┐
│  Spring Boot App     │
│                      │
│  Spring Security     │
│  OAuth2 Client       │
└──────────┬───────────┘
           │
           │ Authorization Code Flow
           ▼
┌──────────────────────┐
│        Auth0         │
│  Identity Provider   │
└──────────┬───────────┘
           │
           │ ID Token
           │ Access Token
           ▼
┌──────────────────────┐
│  Spring Boot App     │
│                      │
│ JWT Resource Server  │
│                      │
│ issuer validation    │
│ audience validation  │
│ permission mapping   │
└──────────┬───────────┘
           │
           ▼
     Protected APIs
       /api/r1
       /api/r2
```

---

# Authorization Model

The demo uses two permissions:

```text
read:r1
read:r2
```

They protect two independent backend resources.

| Endpoint  | Required permission |
| --------- | ------------------- |
| `/api/r1` | `read:r1`           |
| `/api/r2` | `read:r2`           |

Spring Security maps permissions to authorities using the `SCOPE_` prefix.

For example:

```text
read:r1
```

becomes:

```text
SCOPE_read:r1
```

The resource can therefore be protected with:

```java
.requestMatchers("/api/r1/**")
.hasAuthority("SCOPE_read:r1")
```

and:

```java
.requestMatchers("/api/r2/**")
.hasAuthority("SCOPE_read:r2")
```

---

# Authentication Flow

When an unauthenticated user attempts to access a protected page:

```text
/dashboard.html
```

Spring Security starts the OAuth2 login process.

The browser is redirected to Auth0, where the user authenticates.

After authentication, Auth0 redirects the browser back to:

```text
/login/oauth2/code/auth0
```

Spring Security exchanges the authorization code for tokens and creates the authenticated application session.

The application requests the following scopes:

```text
openid
profile
email
read:r1
read:r2
```

It also explicitly sends the configured API `audience` in the authorization request.

---

# JWT Validation

Access tokens are validated before they are accepted by the Resource Server.

The application validates:

### Issuer

The token must have been issued by the configured Identity Provider.

```yaml
auth:
  issuer: ${AUTH_ISSUER}
```

### Audience

The token must contain the configured API audience:

```yaml
auth:
  audience: ${AUTH_AUDIENCE}
```

A token with an incorrect or missing audience is rejected.

Conceptually:

```text
JWT
 │
 ├── signature valid?
 │
 ├── issuer valid?
 │
 ├── audience valid?
 │
 └── permissions valid?
        │
        ▼
   Access granted
```

---

# Permission Mapping

Auth0 can expose authorization information through a `permissions` claim.

The application also checks the standard OAuth `scope` claim.

For example:

```json
{
  "permissions": [
    "read:r1",
    "read:r2"
  ]
}
```

or:

```json
{
  "scope": "openid profile email read:r1 read:r2"
}
```

Both can be mapped into Spring Security authorities.

Only values representing application permissions are converted to `SCOPE_*` authorities.

---

# Application Pages

## Home

```text
/
```

redirects to:

```text
/home.html
```

The home page provides the entry point into the authentication demo.

---

## Dashboard

```text
/dashboard.html
```

The dashboard is available to authenticated users.

It displays:

* User identity information
* Email
* Token timestamps
* Permissions
* Decoded access-token claims
* Links to authorized resources
* Local logout
* Full Auth0 logout

Resource links are displayed according to the permissions available in the access token.

For example:

```text
read:r1
```

enables access to:

```text
Resource 1
```

while:

```text
read:r2
```

enables:

```text
Resource 2
```

---

# Protected Resources

## Resource 1

Frontend:

```text
/r1.html
```

Backend:

```text
GET /api/r1
```

Requires:

```text
read:r1
```

Successful response:

```json
{
  "resource": "Resource 1",
  "message": "Access granted to Resource 1",
  "required_permission": "read:r1"
}
```

---

## Resource 2

Frontend:

```text
/r2.html
```

Backend:

```text
GET /api/r2
```

Requires:

```text
read:r2
```

Successful response:

```json
{
  "resource": "Resource 2",
  "message": "Access granted to Resource 2",
  "required_permission": "read:r2"
}
```

---

# Token Inspector

Authenticated users can open:

```text
/tokens
```

The endpoint exposes information about the current OAuth/OIDC session, including:

```text
ID Token
ID Token claims
Access Token
Decoded Access Token payload
```

This endpoint is intentionally included to make the authentication flow easier to understand while developing and testing the demo.

> **Important:** Exposing raw access tokens or ID tokens to application users is useful for educational debugging but should generally not be done in a production application.

---

# Logout

The project demonstrates two different logout behaviors.

## Local Logout

```text
/logout-local
```

Clears the local Spring Security session.

The Auth0 SSO session remains active.

This means the Identity Provider may automatically authenticate the user again during the next login.

---

## Full Auth0 Logout

```text
/logout-auth0
```

Performs:

```text
Application session logout
        +
Auth0 SSO logout
```

The user is redirected to Auth0's:

```text
/v2/logout
```

endpoint and then returned to the application's home page.

This is useful when testing multiple Auth0 users.

---

# Public Health Endpoint

A basic health endpoint is publicly available:

```text
GET /public/health
```

Response:

```text
OK
```

Example:

```bash
curl http://localhost:8080/public/health
```

---

# Configuration

The application expects its OAuth configuration through environment variables.

| Variable              | Description                            |
| --------------------- | -------------------------------------- |
| `AUTH_ISSUER`         | Auth0 OIDC issuer URL                  |
| `AUTH_AUDIENCE`       | Identifier of the protected API        |
| `AUTH0_CLIENT_ID`     | Auth0 application Client ID            |
| `AUTH0_CLIENT_SECRET` | Auth0 application Client Secret        |
| `PORT`                | Optional HTTP port; defaults to `8080` |

Example:

```bash
export AUTH_ISSUER="https://YOUR_TENANT.auth0.com/"
export AUTH_AUDIENCE="https://auth-demo-api"
export AUTH0_CLIENT_ID="YOUR_CLIENT_ID"
export AUTH0_CLIENT_SECRET="YOUR_CLIENT_SECRET"
```

Never commit real credentials to the repository.

---

# Auth0 Setup

To run the application with Auth0, create both an **Application** and an **API** in the Auth0 dashboard.

## 1. Create an Auth0 Application

Create a regular web application.

Configure the callback URL:

```text
http://localhost:8080/login/oauth2/code/auth0
```

Configure the logout URL:

```text
http://localhost:8080/home.html
```

If using the logout query parameter generated by this demo, also allow the corresponding localhost return URL in Auth0.

---

## 2. Create an Auth0 API

Create an API and assign an identifier such as:

```text
https://auth-demo-api
```

Use that same identifier as:

```text
AUTH_AUDIENCE
```

---

## 3. Create API Permissions

Create the following permissions:

```text
read:r1
read:r2
```

---

## 4. Assign Permissions

Assign one or both permissions to users or roles.

This makes it easy to demonstrate different authorization scenarios.

For example:

### User A

```text
read:r1
```

Can access Resource 1 but not Resource 2.

### User B

```text
read:r1
read:r2
```

Can access both resources.

### User C

```text
No resource permissions
```

Can authenticate successfully but cannot access either protected API.

This demonstrates the difference between:

```text
Authentication
```

and:

```text
Authorization
```

---

# Running the Application

## Clone the repository

```bash
git clone https://github.com/CarlosCh31/auth-demo.git
cd auth-demo
```

---

## Configure environment variables

Linux/macOS:

```bash
export AUTH_ISSUER="https://YOUR_TENANT.auth0.com/"
export AUTH_AUDIENCE="https://auth-demo-api"
export AUTH0_CLIENT_ID="YOUR_CLIENT_ID"
export AUTH0_CLIENT_SECRET="YOUR_CLIENT_SECRET"
```

PowerShell:

```powershell
$env:AUTH_ISSUER="https://YOUR_TENANT.auth0.com/"
$env:AUTH_AUDIENCE="https://auth-demo-api"
$env:AUTH0_CLIENT_ID="YOUR_CLIENT_ID"
$env:AUTH0_CLIENT_SECRET="YOUR_CLIENT_SECRET"
```

---

## Start the application

Linux/macOS:

```bash
./mvnw spring-boot:run
```

Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

The application starts by default on:

```text
http://localhost:8080
```

Open:

```text
http://localhost:8080
```

---

# Build

Create the application package with:

Linux/macOS:

```bash
./mvnw clean package
```

Windows:

```powershell
.\mvnw.cmd clean package
```

The generated JAR will be available under:

```text
target/
```

---

# Tests

Run the test suite with:

Linux/macOS:

```bash
./mvnw test
```

Windows:

```powershell
.\mvnw.cmd test
```

---

# Project Structure

```text
auth-demo/
│
├── src/
│   ├── main/
│   │   ├── java/com/example/auth_demo/
│   │   │   │
│   │   │   ├── AuthDemoApplication.java
│   │   │   │
│   │   │   ├── config/
│   │   │   │   └── SecurityConfig.java
│   │   │   │
│   │   │   └── controller/
│   │   │       ├── HealthController.java
│   │   │       ├── LogoutController.java
│   │   │       ├── ResourcesController.java
│   │   │       ├── RootController.java
│   │   │       └── TokensController.java
│   │   │
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application.yml
│   │       │
│   │       └── static/
│   │           ├── home.html
│   │           ├── dashboard.html
│   │           ├── r1.html
│   │           └── r2.html
│   │
│   └── test/
│
├── .mvn/
├── docker-compose.yml
├── mvnw
├── mvnw.cmd
├── pom.xml
└── README.md
```

---

# Security Configuration

The main security behavior is implemented in:

```text
SecurityConfig.java
```

Public routes include:

```text
/
/home.html
/public/**
/logout-auth0
```

Authenticated users can access:

```text
/tokens
/dashboard.html
```

Permission-protected APIs include:

```text
/api/r1/** → SCOPE_read:r1
/api/r2/** → SCOPE_read:r2
```

The application acts as both an OAuth2 Client and JWT Resource Server within the same Spring Security filter chain.

---

# Docker Compose Note

The repository currently contains a `docker-compose.yml` that starts a local **Keycloak 26** instance:

```bash
docker compose up
```

on:

```text
http://localhost:8081
```

However, the current authentication implementation is specifically configured around Auth0, including Auth0's `/v2/logout` endpoint.

Therefore, the Keycloak container should currently be considered an experimental/local Identity Provider setup rather than a drop-in replacement for the configured Auth0 flow.

Additional configuration changes would be required for full Keycloak compatibility.

---

# Security Notes

This repository is intended as an authentication and authorization **demo**.

Some design decisions prioritize visibility and learning over production hardening.

In particular:

* `/tokens` exposes raw OAuth/OIDC tokens for demonstration purposes.
* The dashboard displays decoded access-token claims.
* CSRF protection is currently disabled.
* Auth0 logout behavior is implemented specifically for the demo provider.
* Client secrets must be supplied through environment variables and must never be committed.

For a production application, raw tokens should not normally be exposed through browser-accessible diagnostic endpoints.

---

# Concepts Demonstrated

This project provides a practical example of several identity and access-management concepts:

```text
Authentication
Authorization
OAuth 2.0
OpenID Connect
Authorization Code Flow
Identity Provider
ID Token
Access Token
JWT
Issuer
Audience
Scopes
Permissions
RBAC
Resource Server
SSO
Session Logout
```

A particularly important distinction demonstrated by the application is:

```text
Authentication
     │
     └── Who is the user?

Authorization
     │
     └── What is the user allowed to access?
```

A user can successfully authenticate while still receiving:

```text
403 Forbidden
```

when attempting to access a resource for which they do not have the required permission.

---

# Possible Improvements

Potential next steps for the project include:

* Add automated authorization tests for `401` and `403` scenarios.
* Add Testcontainers integration.
* Add dedicated Keycloak configuration.
* Make the Identity Provider configurable.
* Add Docker support for the Spring Boot application.
* Add GitHub Actions CI.
* Add structured error responses.
* Re-enable and configure CSRF protection appropriately.
* Restrict or remove the `/tokens` endpoint outside development environments.
* Add profile-based configuration such as `dev` and `prod`.
* Add a proper frontend-to-API Bearer Token demonstration.
* Add refresh-token handling.
* Add Docker health checks.
* Add deployment examples.

---

## Author

**CarlosCh31**

Repository:

`CarlosCh31/auth-demo`
