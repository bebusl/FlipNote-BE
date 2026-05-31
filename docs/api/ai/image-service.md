---
service: image-service
base_url_external: http://api-gateway:8080
base_url_internal: http://image-service:8082
grpc_port: 9092
auth_model: Gateway strips Bearer JWT, forwards X-User-Id header
version: 1.0
---

# Image Service API

## Overview

Handles image upload via S3 Presigned URLs and provides image URL resolution for other services via gRPC.
Upload flow: (1) client calls POST /v1/images/upload to receive a presigned URL, (2) client PUT-uploads the file directly to S3 using that URL, (3) calling service activates the imageRef via gRPC ActivateImage.

Hash-based deduplication: if a file with the same MD5 hash already exists in S3, the presigned URL step is skipped and the existing public URL is returned immediately.

---

## HTTP Endpoints

## POST /v1/images/upload

**Auth:** Public
**Description:** Issue an S3 Presigned URL for direct client-to-S3 upload. If the file already exists (by MD5 hash), returns the existing public URL instead of a presigned URL.

**Request Body:**
  fileName: string (required) — file name in the format `{32-char MD5 hex hash}.{ext}`. Allowed extensions: jpg, jpeg, png, gif. Example: `d41d8cd98f00b204e9800998ecf8427e.jpg`
  type: enum(GROUP|USER|CARD_SET) (required) — the entity type this image will be associated with

**Response 200:**
  url: string — S3 Presigned URL for PUT upload (expires in 5 minutes), OR existing public S3 URL if the image already exists
  imageRefId: number — image reference ID; must be passed to ActivateImage gRPC call after upload

**Errors:**
  400 — fileName does not match required format (32-char MD5 hex + valid extension)
  400 — fileName is null or blank

---

## gRPC Service

**Package:** `image.v1`
**Service:** `ImageCommandService`
**Port:** 9092

### GetUrlByReference

**Description:** Fetch the public image URL for a given reference (user, group, or cardset).

**Request:**
  reference_type: enum(USER=1|GROUP=2|CARD_SET=3) — entity type
  reference_id: int64 — entity ID

**Response:**
  image_url: string — public S3 URL of the active image

---

### GetUrlsByIds

**Description:** Batch-fetch public image URLs by imageRef IDs. Returns a map of id → url.

**Request:**
  ids: repeated int64 — list of imageRef IDs

**Response:**
  image_urls: map<int64, string> — map of imageRefId to public S3 URL

---

### ActivateImage

**Description:** Activate an imageRef by linking it to a concrete entity. Called after the client completes the S3 upload. Must be called to finalize the upload flow.

**Request:**
  image_ref_id: int64 — ID returned from POST /v1/images/upload
  reference_type: enum(USER=1|GROUP=2|CARD_SET=3) — entity type to attach to
  reference_id: int64 — entity ID to attach to

**Response:**
  url: string — public S3 URL of the activated image

---

### ChangeImage

**Description:** Replace the image for an existing reference with a new imageRef. Previous imageRef is detached.

**Request:**
  reference_type: enum(USER=1|GROUP=2|CARD_SET=3) — entity type
  reference_id: int64 — entity ID
  image_ref_id: int64 — new imageRef ID (obtained from POST /v1/images/upload)

**Response:**
  image_ref_id: int64 — updated imageRef ID
  url: string — public S3 URL of the new image

---

### DeleteById

**Description:** Permanently deactivate and delete an imageRef. Called when the owning entity (user, group, cardset) is deleted.

**Request:**
  image_ref_id: int64 — imageRef ID to delete

**Response:** (empty)

---

## Enum Reference

| Value | Proto int | Description |
|---|---|---|
| USER | 1 | User profile image |
| GROUP | 2 | Group thumbnail |
| CARD_SET | 3 | Card set cover image |

---

## gRPC Callers

| Caller Service | Methods Used |
|---|---|
| User (9091) | ActivateImage, ChangeImage, DeleteById, GetUrlByReference |
| Group (9094) | ActivateImage, ChangeImage, DeleteById, GetUrlByReference |
| Cardset (9095) | ActivateImage, ChangeImage, DeleteById, GetUrlsByIds |
