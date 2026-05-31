---
service: user-service
base_url_internal: http://user-service:8081
base_url_external: https://api3.flipnote.site (via Gateway :8080)
grpc_port: 9091
auth_mechanism: HttpOnly cookie (accessToken / refreshToken); Gateway validates token and forwards X-User-Id header
stack: Spring Boot 3, MySQL, Redis, gRPC
last_updated: 2026-05-31
---

# User Service API

## Overview

Handles user registration, authentication (email + OAuth2/Google), profile management, JWT lifecycle, and exposes a gRPC interface for inter-service user queries.

All authenticated endpoints require the `accessToken` HttpOnly cookie. The Gateway strips client-supplied `X-User-Id`, `X-User-Email`, `X-User-Role` headers before forwarding, then re-injects them after validating the token.

---

## Auth Endpoints — `/v1/auth`

---

### POST /v1/auth/register

**Auth:** Public
**Description:** Register a new user. Email must be verified before calling this endpoint.

**Request Body:**
  email: string (required) — user email address
  password: string (required, 8–20 chars) — plain-text password
  name: string (required) — full name
  nickname: string (required, 2–50 chars) — display name
  phone: string (required) — phone number (format: 01x-xxxx-xxxx or 01xxxxxxxxx)
  smsAgree: boolean (required) — SMS marketing consent

**Response 200:**
  data.userId: number — created user ID

**Errors:**
  400 — validation failure (field-level errors in response body)
  409 UNVERIFIED_EMAIL — email not verified before registration
  409 EMAIL_ALREADY_EXISTS — email already registered

---

### POST /v1/auth/login

**Auth:** Public
**Description:** Authenticate with email and password. Sets `accessToken` and `refreshToken` as HttpOnly cookies.

**Request Body:**
  email: string (required) — user email
  password: string (required) — user password

**Response 200:**
  Sets-Cookie: accessToken (HttpOnly, Secure, SameSite=Lax, TTL=15min)
  Sets-Cookie: refreshToken (HttpOnly, Secure, SameSite=Lax, TTL=7days)
  data: null

**Errors:**
  401 INVALID_CREDENTIALS — wrong password
  404 USER_NOT_FOUND — email not registered or user is withdrawn

---

### POST /v1/auth/logout

**Auth:** Bearer JWT (cookie)
**Description:** Invalidate the current session. Blacklists the refresh token and clears auth cookies.

**Cookies Required:**
  refreshToken: string — current refresh token cookie

**Response 200:**
  Clears accessToken and refreshToken cookies
  data: null

---

### POST /v1/auth/token/refresh

**Auth:** Public (refresh token cookie)
**Description:** Issue a new token pair using the refresh token. The old refresh token is blacklisted.

**Cookies Required:**
  refreshToken: string — valid refresh token

**Response 200:**
  Sets-Cookie: accessToken (new)
  Sets-Cookie: refreshToken (new)
  data: null

**Errors:**
  400 MISSING_COOKIE — refreshToken cookie absent
  401 INVALID_TOKEN — token signature invalid or expired
  401 BLACKLISTED_TOKEN — token already used or logged out
  401 INVALIDATED_SESSION — session invalidated (password change or withdrawal)

---

### POST /v1/auth/token/validate

**Auth:** Public
**Description:** Validate an access token and return its claims. Used internally by the Gateway.

**Request Body:**
  token: string (required) — JWT access token string

**Response 200:**
  data.userId: number — user ID from token claims
  data.email: string — user email from token claims
  data.role: enum(USER|ADMIN) — user role from token claims

**Errors:**
  401 INVALID_TOKEN — invalid or expired token
  401 BLACKLISTED_TOKEN — token is blacklisted
  401 INVALIDATED_SESSION — session has been invalidated

---

### PATCH /v1/auth/password

**Auth:** Bearer JWT (cookie); `X-User-Id` forwarded by Gateway
**Description:** Change the authenticated user's password. Invalidates all active sessions.

**Request Body:**
  currentPassword: string (required) — existing password for verification
  newPassword: string (required, 8–20 chars) — new password

**Response 200:**
  Clears accessToken and refreshToken cookies
  data: null

**Errors:**
  401 PASSWORD_MISMATCH — currentPassword does not match stored password

---

### POST /v1/auth/password-reset/request

**Auth:** Public
**Description:** Send a password-reset email. Silent no-op if email is not registered (prevents enumeration).

**Request Body:**
  email: string (required) — registered email address

**Response 200:**
  data: null

**Errors:**
  409 ALREADY_SENT_PASSWORD_RESET_LINK — link already sent (30-min cooldown)

---

### POST /v1/auth/password-reset

**Auth:** Public
**Description:** Reset password using the token received in the reset email. Invalidates all sessions.

**Request Body:**
  token: string (required) — UUID token from password-reset email
  password: string (required, 8–20 chars) — new password

**Response 200:**
  data: null

**Errors:**
  400 INVALID_PASSWORD_RESET_TOKEN — token expired (30 min) or not found

---

### POST /v1/auth/email-verification/request

**Auth:** Public
**Description:** Send a 6-digit verification code to the given email address.

**Request Body:**
  email: string (required) — email to verify

**Response 200:**
  data: null

**Errors:**
  409 ALREADY_ISSUED_VERIFICATION_CODE — code already sent (5-min cooldown)

---

### POST /v1/auth/email-verification

**Auth:** Public
**Description:** Verify the email address with the received 6-digit code. Marks email as verified (valid for 10 min).

**Request Body:**
  email: string (required) — email address
  code: string (required, exactly 6 digits) — verification code

**Response 200:**
  data: null

**Errors:**
  400 NOT_ISSUED_VERIFICATION_CODE — no code found for this email
  400 INVALID_VERIFICATION_CODE — code mismatch

---

### GET /v1/auth/social-links

**Auth:** Bearer JWT (cookie); `X-User-Id` forwarded by Gateway
**Description:** List all OAuth providers linked to the authenticated user's account.

**Response 200:**
  data.socialLinks: array of:
    socialLinkId: number — link record ID
    provider: string — OAuth provider name (e.g., "google")
    linkedAt: string (ISO 8601) — when the link was created

---

### DELETE /v1/auth/social-links/{socialLinkId}

**Auth:** Bearer JWT (cookie); `X-User-Id` forwarded by Gateway
**Description:** Unlink a social account from the authenticated user.

**Path Params:**
  socialLinkId: number — ID of the social link to remove

**Response 200:**
  data: null

**Errors:**
  403 — link does not belong to the authenticated user
  404 — link not found

---

## User Endpoints — `/v1/users`

---

### GET /v1/users/me

**Auth:** Bearer JWT (cookie); `X-User-Id` forwarded by Gateway
**Description:** Get the authenticated user's full profile.

**Response 200:**
  data.userId: number
  data.email: string
  data.nickname: string
  data.name: string
  data.phone: string
  data.smsAgree: boolean
  data.profileImageUrl: string — S3 URL (default image if not set)
  data.imageRefId: number — image reference ID (nullable)
  data.createdAt: string (ISO 8601)
  data.modifiedAt: string (ISO 8601)

**Errors:**
  404 USER_NOT_FOUND — user not found or withdrawn

---

### GET /v1/users/{userId}

**Auth:** Bearer JWT (cookie); `X-User-Id` forwarded by Gateway
**Description:** Get public profile of any active user by ID.

**Path Params:**
  userId: number — target user ID

**Response 200:**
  data.userId: number
  data.nickname: string
  data.profileImageUrl: string
  data.imageRefId: number (nullable)

**Errors:**
  404 USER_NOT_FOUND — user not found or withdrawn

---

### PUT /v1/users

**Auth:** Bearer JWT (cookie); `X-User-Id` forwarded by Gateway
**Description:** Update the authenticated user's profile. Calls Image service via gRPC to activate or swap profile image.

**Request Body:**
  nickname: string (optional, 2–50 chars) — new display name
  phone: string (optional) — new phone number
  smsAgree: boolean (optional) — SMS consent
  imageRefId: number (optional) — image reference ID from Image service

**Response 200:**
  data.userId: number
  data.nickname: string
  data.phone: string
  data.smsAgree: boolean
  data.profileImageUrl: string — updated S3 URL
  data.imageRefId: number

**Errors:**
  503 IMAGE_SERVICE_ERROR — gRPC call to Image service failed

---

### DELETE /v1/users

**Auth:** Bearer JWT (cookie); `X-User-Id` forwarded by Gateway
**Description:** Withdraw (soft-delete) the authenticated user. Sets status to WITHDRAWN, records deletedAt, invalidates all sessions, and clears auth cookies.

**Response 200:**
  Clears accessToken and refreshToken cookies
  data: null

---

## OAuth Endpoints

---

### GET /oauth2/authorization/{provider}

**Auth:** Public (optional X-User-Id header for account linking)
**Description:** Initiate OAuth2 PKCE flow. Generates a code verifier, stores it as a cookie, and redirects to the provider's authorization URI.

**Path Params:**
  provider: string — OAuth provider name (currently: `google`)

**Headers (optional):**
  X-User-Id: number — if present, flow is treated as account-linking (not login)

**Response 302:**
  Sets-Cookie: oauth2_auth_request (codeVerifier, HttpOnly, TTL=180s)
  Location: {provider authorization URI with PKCE code_challenge and state}

---

### GET /oauth2/callback/{provider}

**Auth:** Public
**Description:** OAuth2 callback endpoint. Handles both social login and social account linking based on the `state` query parameter.

**Path Params:**
  provider: string — OAuth provider (e.g., `google`)

**Query Params:**
  code: string (required) — authorization code from provider
  state: string (optional) — present for account linking, absent for login

**Cookies Required:**
  oauth2_auth_request: string — codeVerifier stored during authorization step

**Response 302 (social login, state absent):**
  Sets-Cookie: accessToken, refreshToken
  Location: {client}/social-login/success OR {client}/social-login/failure

**Response 302 (account linking, state present):**
  Location: {client}/social-link/success OR {client}/social-link/failure OR {client}/social-link/conflict

**Errors:**
  302 to failure URL — INVALID_SOCIAL_LINK_TOKEN, ALREADY_LINKED_SOCIAL_ACCOUNT, OAUTH_COMMUNICATION_ERROR

---

## gRPC Service — UserQueryService (port 9091)

**Proto:** `user_query.proto`

```
service UserQueryService {
  rpc GetUser(GetUserRequest) returns (GetUserResponse);
  rpc GetUsers(GetUsersRequest) returns (GetUsersResponse);
  rpc GetUserByEmail(GetUserByEmailRequest) returns (GetUserByEmailResponse);
  rpc GetUserByToken(GetUserByTokenRequest) returns (GetUserByTokenResponse);
}
```

### GetUser

**Request:**
  user_id: int64 (required) — user ID to look up

**Response:**
  id: int64
  email: string
  nickname: string
  profile_image_url: string

**Errors:**
  NOT_FOUND — user does not exist or is withdrawn

---

### GetUsers

**Request:**
  user_ids: repeated int64 — list of user IDs

**Response:**
  users: repeated GetUserResponse — matching users only (missing IDs silently excluded)

---

### GetUserByEmail

**Request:**
  email: string — email address to look up

**Response:**
  exists: bool — whether an active user with this email exists
  user: GetUserResponse (populated if exists=true)

---

### GetUserByToken

**Request:**
  access_token: string — JWT access token string

**Response:**
  user_id: int64
  nickname: string

**Errors:**
  UNAUTHENTICATED — token invalid, expired, blacklisted, or session invalidated

---

## Error Codes Reference

| Code | HTTP | Description |
|------|------|-------------|
| INVALID_CREDENTIALS | 401 | Wrong email or password |
| UNVERIFIED_EMAIL | 409 | Email not verified before registration |
| EMAIL_ALREADY_EXISTS | 409 | Email already registered |
| INVALID_TOKEN | 401 | JWT invalid or expired |
| BLACKLISTED_TOKEN | 401 | JWT has been revoked |
| INVALIDATED_SESSION | 401 | Session invalidated (password change / withdrawal) |
| PASSWORD_MISMATCH | 401 | Current password verification failed |
| ALREADY_LINKED_SOCIAL_ACCOUNT | 409 | Social account already linked |
| ALREADY_ISSUED_VERIFICATION_CODE | 409 | Verification code already sent (5-min cooldown) |
| NOT_ISSUED_VERIFICATION_CODE | 400 | No verification code found for this email |
| INVALID_VERIFICATION_CODE | 400 | Verification code mismatch |
| ALREADY_SENT_PASSWORD_RESET_LINK | 409 | Reset link already sent (30-min cooldown) |
| INVALID_PASSWORD_RESET_TOKEN | 400 | Reset token expired or not found |
| INVALID_SOCIAL_LINK_TOKEN | 400 | OAuth state token expired or not found |
| INVALID_OAUTH_PROVIDER | 400 | Unknown OAuth provider name |
| OAUTH_COMMUNICATION_ERROR | 502 | Failed to communicate with OAuth provider |
| USER_NOT_FOUND | 404 | User does not exist or is withdrawn |
| IMAGE_SERVICE_ERROR | 503 | gRPC call to Image service failed |

---

## Token Lifecycle Notes

- **Access token TTL:** 15 minutes (configurable via `JWT_ACCESS_EXPIRATION`)
- **Refresh token TTL:** 7 days (configurable via `JWT_REFRESH_EXPIRATION`)
- **Session invalidation:** On password change or withdrawal, a `sessionInvalidatedAt` timestamp is stored in Redis. Tokens issued before that timestamp are rejected even if not blacklisted.
- **Token blacklist:** Stored in Redis with TTL matching remaining token lifetime.
- **Storage:** Tokens are stored as HttpOnly, Secure, SameSite=Lax cookies — not in Authorization headers.
