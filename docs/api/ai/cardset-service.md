---
service: cardset
version: 1.0
base_url_internal: http://cardset-service:8085
base_url_external: via Gateway http://gateway:8080
stack: NestJS / TypeScript
grpc_port: 9095
auth: Bearer JWT (Gateway strips, X-User-Id forwarded as X-USER-ID header)
---

# Cardset Service API

All HTTP endpoints require `X-USER-ID` header (integer string), forwarded by the Gateway after JWT validation.

---

## POST /v1/card-sets

**Auth:** Bearer JWT (Gateway strips, X-USER-ID forwarded)
**Description:** Create a new cardset inside a group. Caller must be a member of the target group.

**Request Body:**
  name: string (required) — cardset display name
  groupId: number (required) — group the cardset belongs to
  visibility: enum(PUBLIC|PRIVATE) (required) — access scope
  category: string (required) — cardset category label
  hashtag: string (optional) — hashtag string, e.g. "#영어#단어"
  imageRefId: number (optional) — image reference ID from Image service
  managerIds: number[] (optional) — additional manager user IDs

**Response 201:**
  status: number — 201
  code: string — "CREATED"
  message: string
  data.cardsetId: number — ID of the created cardset

**Errors:**
  403 — caller is not a member of the specified group

---

## GET /v1/card-sets

**Auth:** Bearer JWT (Gateway strips, X-USER-ID forwarded)
**Description:** Paginated list of cardsets accessible to the caller.

**Query Params:**
  page: number (optional, default 1) — 1-indexed page number
  size: number (optional, default 10, max 30) — items per page
  sortBy: enum(createdAt|name|cardCount) (optional) — sort field
  order: enum(asc|desc) (optional, default desc) — sort direction
  keyword: string (optional) — keyword search on cardset name
  category: string (optional) — filter by category

**Response 200:**
  status: number — 200
  code: string — "SUCCESS"
  message: string
  data.items: CardsetListItemResponse[] — page items
  data.total: number — total matching count
  data.page: number — current page
  data.size: number — page size

**CardsetListItemResponse fields:**
  cardSetId: number
  groupId: number
  name: string
  category: string
  hashtag: string | null
  imageUrl: string
  imageRefId: number | null
  likeCount: number
  bookmarkCount: number
  liked: boolean — whether the caller has liked this cardset
  bookmarked: boolean — whether the caller has bookmarked this cardset
  managers: ManagerInfoResponse[]

**ManagerInfoResponse fields:**
  id: number
  email: string
  nickname: string
  profileImageUrl: string

---

## GET /v1/card-sets/:cardsetId

**Auth:** Bearer JWT (Gateway strips, X-USER-ID forwarded)
**Description:** Retrieve a single cardset with full detail including like/bookmark state.

**Path Params:**
  cardsetId: number — target cardset ID

**Response 200:**
  status: number — 200
  code: string — "SUCCESS"
  message: string
  data: CardsetResponse | null

**CardsetResponse fields:**
  id: number
  name: string
  groupId: number
  visibility: enum(PUBLIC|PRIVATE)
  category: string
  hashtag: string | null
  imageRefId: number | null
  imageUrl: string
  cardCount: number
  likeCount: number
  bookmarkCount: number
  liked: boolean
  bookmarked: boolean
  managers: ManagerInfoResponse[]
  createdAt: string (ISO 8601)
  updatedAt: string (ISO 8601)

**Errors:**
  403 — caller does not have access to the cardset

---

## PUT /v1/card-sets/:cardsetId

**Auth:** Bearer JWT (Gateway strips, X-USER-ID forwarded)
**Description:** Update cardset metadata. Caller must be a manager of the cardset.

**Path Params:**
  cardsetId: number — target cardset ID

**Request Body:**
  name: string (optional) — new cardset name
  visibility: enum(PUBLIC|PRIVATE) (optional)
  category: string (optional)
  hashtag: string | null (optional)
  imageRefId: number (optional)
  managerIds: number[] (optional) — replaces the manager list

**Response 200:**
  status: number — 200
  code: string — "SUCCESS"
  message: string
  data: CardsetResponse | null

**Errors:**
  403 — caller is not a manager of the cardset

---

## DELETE /v1/card-sets/:cardsetId

**Auth:** Bearer JWT (Gateway strips, X-USER-ID forwarded)
**Description:** Delete a cardset. Caller must be a manager of the cardset.

**Path Params:**
  cardsetId: number — target cardset ID

**Response 200:**
  status: number — 200
  code: string — "SUCCESS"
  message: string — "삭제되었습니다."
  data: null

**Errors:**
  403 — caller is not a manager of the cardset

---

## GET /v1/card-sets/:cardsetId/cards

**Auth:** Bearer JWT (Gateway strips, X-USER-ID forwarded)
**Description:** Retrieve all cards in a cardset as Yjs-based card objects. Returns the current snapshot from the Yjs CRDT document.

**Path Params:**
  cardsetId: number — target cardset ID

**Response 200:**
  status: number — 200
  code: string — "SUCCESS"
  message: string
  data: YjsCardResponse[]

**YjsCardResponse fields:**
  id: string — card identifier within the Yjs document
  question: string — question content
  answer: string — answer content

---

## POST /v1/card-sets/:cardsetId

**Auth:** Bearer JWT (Gateway strips, X-USER-ID forwarded)
**Description:** Persist the current Yjs document state for a cardset to the database. Caller must be a manager.

**Path Params:**
  cardsetId: number — target cardset ID

**Response 200:**
  status: number — 200
  code: string — "SUCCESS"
  message: string
  data: null

**Errors:**
  403 — caller is not a manager of the cardset

---

## GET /v1/groups/:groupId/card-sets

**Auth:** Bearer JWT (Gateway strips, X-USER-ID forwarded)
**Description:** Paginated list of cardsets belonging to a specific group. Caller must be a member of the group.

**Path Params:**
  groupId: number — target group ID

**Query Params:**
  page: number (optional, default 1)
  size: number (optional, default 10, max 30)
  sortBy: enum(createdAt|name|cardCount) (optional)
  order: enum(asc|desc) (optional, default desc)
  keyword: string (optional)
  category: string (optional)

**Response 200:**
  status: number — 200
  code: string — "SUCCESS"
  message: string
  data.items: CardsetListItemResponse[]
  data.total: number
  data.page: number
  data.size: number

**Errors:**
  403 — caller is not a member of the group

---

## WebSocket /v1/card-sets/ws

**Auth:** Bearer JWT passed via handshake auth or Authorization header
**Description:** Real-time collaborative editing using Socket.IO + Yjs CRDT. Caller must be a manager of the cardset to send updates.

**Connection:**
  path: /v1/card-sets/ws
  transport: Socket.IO (WebSocket)
  pingTimeout: 10000ms
  pingInterval: 25000ms

**Client → Server Events:**

  join-cardset
    payload: { cardsetId: string }
    description: Join a cardset editing room. Server responds with sync event containing the full Yjs document state.

  leave-cardset
    payload: { cardsetId: string }
    description: Leave a cardset editing room. If the room becomes empty, the Yjs document is flushed to DB after 5s.

  update
    payload: { cardsetId: string, update: number[] }
    description: Send a Yjs incremental update (encoded as byte array). Server merges and broadcasts the new state to all room members.

  awareness
    payload: { cardsetId: string, awareness: number[] }
    description: Broadcast cursor/presence awareness state to other clients in the room.

**Server → Client Events:**

  sync
    payload: { cardsetId: string, update: number[] }
    description: Full or incremental Yjs document state as a byte array. Sent after join-cardset and after each update.

  awareness
    payload: { data: { cardsetId: string, awareness: Uint8Array } }
    description: Forwarded awareness state from another client.

  error
    payload: { message: string, details?: string }
    description: Error notification (e.g. permission denied, sync failure).

---

## gRPC Service: CardsetService (port 9095)

Defined in `src/proto/cardset.proto`.

### IsCardSetViewable

**Description:** Check whether a user can view a specific cardset (used by other services for access control).

**Request:**
  card_set_id: int64 (required)
  user_id: int64 (required)

**Response:**
  viewable: bool

---

### GetCardSetsByIds

**Description:** Batch fetch cardset summaries by IDs.

**Request:**
  card_set_ids: int64[] (required)
  user_id: int64 (required)

**Response:**
  card_sets: CardSetSummary[]

**CardSetSummary fields:**
  id: int64
  name: string
  group_id: int64
  visibility: string — "PUBLIC" or "PRIVATE"
  category: string
  hashtag: string
  image_ref_id: int64 (optional)
  card_count: int64
