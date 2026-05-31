---
service: reaction-service
base_url: http://reaction-service:8083
external_base_url: http://gateway:8080
auth: Bearer JWT (Gateway strips, X-User-Id forwarded as X-User-Id header)
http_port: 8083
grpc_port: 9093
tech_stack: Spring Boot 21, MySQL, RabbitMQ
---

# Reaction Service API

Manages likes and bookmarks for cardsets. All endpoints require authentication via the API Gateway.

---

## POST /v1/likes/{targetType}/{targetId}

**Auth:** Bearer JWT (Gateway strips, X-User-Id forwarded)
**Description:** Add a like to the specified target.

**Path Params:**
  targetType: enum(card_set) — type of the target to like
  targetId: number — ID of the target

**Response 200:**
  id: number — ID of the created like record

**Errors:**
  400 LIKE_001 — invalid targetType value (not in enum)
  409 LIKE_002 — user has already liked this target
  403 COMMON_003 — target is not viewable by the user
  502 COMMON_004 — downstream gRPC call to cardset-service failed

---

## DELETE /v1/likes/{targetType}/{targetId}

**Auth:** Bearer JWT (Gateway strips, X-User-Id forwarded)
**Description:** Remove a previously added like from the specified target.

**Path Params:**
  targetType: enum(card_set) — type of the target
  targetId: number — ID of the target

**Response 200:**
  (empty body)

**Errors:**
  400 LIKE_001 — invalid targetType value
  404 LIKE_003 — like record not found for this user and target

---

## GET /v1/likes/{targetType}

**Auth:** Bearer JWT (Gateway strips, X-User-Id forwarded)
**Description:** Retrieve a paginated list of items liked by the authenticated user for the given target type.

**Path Params:**
  targetType: enum(card_set) — type of the liked targets to list

**Query Params:**
  page: number (optional, default 1, min 1) — 1-based page number
  size: number (optional, default 10, min 1, max 30) — items per page
  sortBy: string (optional) — field name to sort by
  order: string (optional, default "desc") — sort direction, enum(asc|desc)

**Response 200:**
  content: object[] — array of LikeResult items
    content[].targetType: string — target type name (e.g. "CARD_SET")
    content[].targetId: number — ID of the liked target
    content[].likedAt: string — ISO 8601 datetime when the like was created
    content[].cardSet: object — CardSetSummary (present when targetType is CARD_SET)
      content[].cardSet.id: number — cardset ID
      content[].cardSet.name: string — cardset name
      content[].cardSet.groupId: number — owning group ID
      content[].cardSet.visibility: string — visibility setting
      content[].cardSet.category: string — category
      content[].cardSet.hashtag: string — hashtag string
      content[].cardSet.imageRefId: number|null — image reference ID (optional)
      content[].cardSet.cardCount: number — number of cards in the set
  page: number — current page index (0-based in response)
  size: number — page size
  totalElements: number — total count of liked items
  totalPages: number — total number of pages
  first: boolean — whether this is the first page
  last: boolean — whether this is the last page
  hasNext: boolean — whether a next page exists
  hasPrevious: boolean — whether a previous page exists

**Errors:**
  400 LIKE_001 — invalid targetType value
  400 COMMON_002 — invalid query param values (page/size out of range)

---

## POST /v1/bookmarks/{targetType}/{targetId}

**Auth:** Bearer JWT (Gateway strips, X-User-Id forwarded)
**Description:** Add a bookmark to the specified target.

**Path Params:**
  targetType: enum(card_set) — type of the target to bookmark
  targetId: number — ID of the target

**Response 200:**
  id: number — ID of the created bookmark record

**Errors:**
  400 BOOKMARK_001 — invalid targetType value (not in enum)
  409 BOOKMARK_002 — user has already bookmarked this target
  403 COMMON_003 — target is not viewable by the user
  502 COMMON_004 — downstream gRPC call to cardset-service failed

---

## DELETE /v1/bookmarks/{targetType}/{targetId}

**Auth:** Bearer JWT (Gateway strips, X-User-Id forwarded)
**Description:** Remove a previously added bookmark from the specified target.

**Path Params:**
  targetType: enum(card_set) — type of the target
  targetId: number — ID of the target

**Response 200:**
  (empty body)

**Errors:**
  400 BOOKMARK_001 — invalid targetType value
  404 BOOKMARK_003 — bookmark record not found for this user and target

---

## GET /v1/bookmarks/{targetType}

**Auth:** Bearer JWT (Gateway strips, X-User-Id forwarded)
**Description:** Retrieve a paginated list of items bookmarked by the authenticated user for the given target type.

**Path Params:**
  targetType: enum(card_set) — type of the bookmarked targets to list

**Query Params:**
  page: number (optional, default 1, min 1) — 1-based page number
  size: number (optional, default 10, min 1, max 30) — items per page
  sortBy: string (optional) — field name to sort by
  order: string (optional, default "desc") — sort direction, enum(asc|desc)

**Response 200:**
  content: object[] — array of BookmarkResult items
    content[].targetType: string — target type name (e.g. "CARD_SET")
    content[].targetId: number — ID of the bookmarked target
    content[].bookmarkedAt: string — ISO 8601 datetime when the bookmark was created
    content[].cardSet: object — CardSetSummary (present when targetType is CARD_SET)
      content[].cardSet.id: number — cardset ID
      content[].cardSet.name: string — cardset name
      content[].cardSet.groupId: number — owning group ID
      content[].cardSet.visibility: string — visibility setting
      content[].cardSet.category: string — category
      content[].cardSet.hashtag: string — hashtag string
      content[].cardSet.imageRefId: number|null — image reference ID (optional)
      content[].cardSet.cardCount: number — number of cards in the set
  page: number — current page index (0-based in response)
  size: number — page size
  totalElements: number — total count of bookmarked items
  totalPages: number — total number of pages
  first: boolean — whether this is the first page
  last: boolean — whether this is the last page
  hasNext: boolean — whether a next page exists
  hasPrevious: boolean — whether a previous page exists

**Errors:**
  400 BOOKMARK_001 — invalid targetType value
  400 COMMON_002 — invalid query param values (page/size out of range)

---

## gRPC: ReactionService (port 9093)

### rpc IsLiked

**Description:** Check whether a single user has liked a specific target.

**Request:**
  target_type: string — target type (e.g. "CARD_SET")
  target_id: int64 — ID of the target
  user_id: int64 — ID of the user

**Response:**
  reacted: bool — true if the user has liked the target

---

### rpc AreLiked

**Description:** Batch check whether a user has liked each of multiple targets.

**Request:**
  target_type: string — target type
  target_ids: int64[] — list of target IDs to check
  user_id: int64 — ID of the user

**Response:**
  results: map<int64, bool> — map of target_id → liked status

---

### rpc IsBookmarked

**Description:** Check whether a single user has bookmarked a specific target.

**Request:**
  target_type: string — target type
  target_id: int64 — ID of the target
  user_id: int64 — ID of the user

**Response:**
  reacted: bool — true if the user has bookmarked the target

---

### rpc AreBookmarked

**Description:** Batch check whether a user has bookmarked each of multiple targets.

**Request:**
  target_type: string — target type
  target_ids: int64[] — list of target IDs to check
  user_id: int64 — ID of the user

**Response:**
  results: map<int64, bool> — map of target_id → bookmarked status

---

## RabbitMQ Events Published

**Exchange:** `reaction.exchange` (TopicExchange)

| Routing Key | Event Type | Trigger |
|---|---|---|
| reaction.like.added | LIKE_ADDED | User adds a like |
| reaction.like.removed | LIKE_REMOVED | User removes a like |
| reaction.bookmark.added | BOOKMARK_ADDED | User adds a bookmark |
| reaction.bookmark.removed | BOOKMARK_REMOVED | User removes a bookmark |

**Message Schema:**
  eventType: string — event type identifier
  targetType: string — type of the reaction target
  targetId: number — ID of the reaction target
  userId: number — ID of the user who triggered the event
