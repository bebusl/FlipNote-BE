---
service: api-gateway
port: 8080
stack: Spring Boot 3.4.1, Java 21, Spring Cloud Gateway, WebFlux
role: Single entry point for all external HTTP/WebSocket requests. Handles JWT authentication, routing, and CORS. Microservices are not exposed externally.
base_url: http://gateway:8080
auth_mechanism: Cookie `accessToken` (JWT). On success, strips the cookie and injects X-User-Id, X-User-Email, X-User-Role headers into the downstream request.
---

# API Gateway — Routing Reference

The Gateway does not expose its own business API. It proxies all external requests to downstream microservices.
This document describes routing rules, authentication behavior, and the internal token validation call.

---

## Authentication Behavior

**Global (applies to every request, before auth filter):**
- `HeaderCleanupGlobalFilter` removes `X-User-Id`, `X-User-Email`, `X-User-Role` from all incoming requests to prevent header spoofing.

**Routes with `AuthenticationFilter`:**
1. Extract JWT from cookie named `accessToken`.
2. If cookie is missing → `401 Unauthorized` (no body).
3. Call `POST /v1/auth/token/validate` on User Service.
4. If validation fails → `401 Unauthorized` (no body).
5. On success, inject into downstream request headers:
   - `X-User-Id: {userId}` (long)
   - `X-User-Email: {email}` (string)
   - `X-User-Role: {role}` (string, e.g. `ROLE_USER`)

**Public routes:** no filter applied; requests are proxied as-is.

---

## Internal Call: Token Validation

The Gateway calls the User Service internally to validate tokens.

**POST** `http://user-service:8081/v1/auth/token/validate`

**Request Body:**
  token: string (required) — JWT access token from cookie

**Response 200:**
  userId: number — authenticated user's ID
  email: string — authenticated user's email
  role: string — authenticated user's role (e.g. ROLE_USER)

---

## Routing Table

### User Service (→ user-service:8081)

**Route: auth-public** — Public
  /v1/auth/login
  /v1/auth/register
  /v1/auth/token/refresh
  /v1/auth/email-verification/request
  /v1/auth/email-verification
  /v1/auth/password-reset/request
  /v1/auth/password-reset
  /oauth2/callback/*
  /oauth2/authorization/*

**Route: auth-private** — Bearer JWT (Cookie)
  /v1/auth/logout
  /v1/auth/password
  /v1/auth/social-links
  /v1/auth/social-links/*
  /v1/oauth2/links/*

**Route: user-private** — Bearer JWT (Cookie)
  /v1/users/me
  /v1/users/{userId}
  /v1/users

**Route: user-swagger** — Public
  /users/swagger-ui.html
  /users/swagger-ui/**
  /users/v3/api-docs
  /users/v3/api-docs/**

---

### Image Service (→ image-service:8082)

**Route: image-public** — Public
  /v1/images/upload

**Route: image-private** — Bearer JWT (Cookie)
  /v1/images/**

---

### Reaction Service (→ reaction-service:8083)

**Route: reaction-private** — Bearer JWT (Cookie)
  /v1/likes/{targetType}/{targetId}
  /v1/likes/{targetType}
  /v1/bookmarks/{targetType}/{targetId}
  /v1/bookmarks/{targetType}

---

### Group Service (→ group-service:8084)

**Route: group** — Bearer JWT (Cookie)
  /v1/groups/**
  /v1/joins/**
  /v1/group-invitations

**Route: group-swagger** — Public
  /groups/swagger-ui.html
  /groups/swagger-ui/**
  /groups/v3/api-docs
  /groups/v3/api-docs/**

**Route: group-health** — Public
  /groups/actuator/health

---

### Cardset Service (→ cardset-service:8085)

**Route: group-cardsets** — Bearer JWT (Cookie) — protocol: HTTP
  /v1/groups/*/card-sets
  NOTE: This route is defined before `group` route, so card-set listing is handled by cardset-service, not group-service.

**Route: cardset-private** — Bearer JWT (Cookie) — protocol: HTTP
  /v1/card-sets
  /v1/card-sets/**
  /v1/cards
  /v1/cards/**

**Route: cardset-websocket** — Bearer JWT (Cookie) — protocol: WebSocket (ws://)
  /v1/card-sets/ws/**

**Route: cardset-swagger** — Public
  /card-sets/swagger-ui.html
  /card-sets/swagger-ui/**
  /card-sets/v3/api-docs
  /card-sets/v3/api-docs/**

**Route: cardset-health** — Public
  /card-sets/health

---

### Notification Service (→ notification-service:8086)

**Route: notification-private** — Bearer JWT (Cookie)
  /v1/notifications/**

**Route: notification-swagger** — Public
  /notifications/swagger-ui.html
  /notifications/swagger-ui/**
  /notifications/v3/api-docs
  /notifications/v3/api-docs/**

---

## CORS Configuration

  allowedOriginPatterns: * (all origins)
  allowedMethods: GET, POST, PUT, DELETE, PATCH, OPTIONS
  allowedHeaders: * (all headers)
  exposedHeaders: X-User-Id, X-User-Email, X-User-Role
  allowCredentials: true

---

## Route Priority Note

Spring Cloud Gateway matches routes in order of declaration. The `group-cardsets` route (`/v1/groups/*/card-sets`) is declared before the `group` route (`/v1/groups/**`), ensuring cardset list requests are routed to the cardset-service.

## Actuator

  GET /actuator/health           — overall health
  GET /actuator/health/liveness  — Kubernetes liveness probe
  GET /actuator/health/readiness — Kubernetes readiness probe
