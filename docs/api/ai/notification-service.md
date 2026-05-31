---
service: notification
port: 8086
grpc_port: null
base_path: /v1/notifications
auth_note: All endpoints require Bearer JWT. Gateway validates token, strips Authorization header, and forwards X-User-Id.
tech_stack: Spring Boot 21, MySQL, Firebase FCM, RabbitMQ
---

# Notification Service API

## Common

**Request Header (injected by Gateway):**
  X-User-Id: number — authenticated user ID (Long), forwarded from Gateway after JWT validation

**Response Wrapper:**
  All responses are wrapped by `ApiResponseAdvice`:
  status: number — HTTP status code
  code: string | null — error code (null on success)
  message: string | null — error message (null on success)
  data: any | null — response body (null on error)

**Error Codes:**
  NOTIFICATION_001 — 500 FCM_INTERNAL_ERROR — FCM internal error occurred
  NOTIFICATION_002 — 503 FCM_SERVER_UNAVAILABLE — FCM server is unavailable
  NOTIFICATION_003 — 404 NOTIFICATION_NOT_FOUND — notification not found or not owned by user
  NOTIFICATION_004 — 409 ALREADY_READ_NOTIFICATION — notification is already read
  COMMON_001 — 500 — unexpected server error
  COMMON_002 — 400 — invalid input value

---

## GET /v1/notifications

**Auth:** Bearer JWT (Gateway strips, X-User-Id forwarded)
**Description:** Retrieve the authenticated user's notifications with cursor-based pagination. Supports filtering by group and read status.

**Query Params:**
  cursor: string (optional) — cursor value from previous response's `nextCursor`. Omit for first page.
  size: number (optional, default: 10, min: 1, max: 30) — page size
  sortBy: string (optional) — field name to sort by. Omits DB default order when not provided.
  order: enum(asc|desc) (optional, default: desc) — sort direction
  groupId: number (optional, min: 1) — filter by group ID
  read: boolean (optional) — true: read only / false: unread only / omit: all

**Response 200:**
  content: NotificationResult[] — list of notifications
  hasNext: boolean — whether more pages exist
  nextCursor: string | null — cursor for next page request; null on last page
  size: number — count of items in current page

**NotificationResult object:**
  notificationId: number — notification ID
  groupId: number | null — related group ID
  message: string — fully-rendered notification message string
  metadata: object — type-specific additional data (see below)
  isRead: boolean — whether the notification has been read
  readAt: string | null — ISO 8601 datetime when read; null if unread
  createdAt: string — ISO 8601 datetime when created

**NotificationType and metadata:**
  GROUP_INVITE — message example: "스터디 그룹에 초대되셨습니다." — metadata: {}
  GROUP_JOIN_REQUEST — message example: "홍길동님이 그룹 가입을 신청했습니다." — metadata: { "requesterId": number }

---

## POST /v1/notifications/token

**Auth:** Bearer JWT (Gateway strips, X-User-Id forwarded)
**Description:** Register a Firebase FCM device token for the authenticated user. Safe to call with duplicate tokens.

**Request Body:**
  token: string (required, non-empty) — FCM device token issued by Firebase

**Response 201:**
  (no body)

---

## POST /v1/notifications/read-all

**Auth:** Bearer JWT (Gateway strips, X-User-Id forwarded)
**Description:** Mark all unread notifications for the authenticated user as read in bulk.

**Response 200:**
  (no body)

---

## POST /v1/notifications/{notificationId}/read

**Auth:** Bearer JWT (Gateway strips, X-User-Id forwarded)
**Description:** Mark a single notification as read. Fails if the notification does not belong to the user or is already read.

**Path Params:**
  notificationId: number — ID of the notification to mark as read

**Response 200:**
  (no body)

**Errors:**
  404 NOTIFICATION_003 — notification not found or not owned by authenticated user
  409 NOTIFICATION_004 — notification is already read

---

## RabbitMQ — Consumed Events

Exchange: `flipnote.notification` (TopicExchange, durable)
DLX Exchange: `flipnote.notification.dlx` (DirectExchange)
DLQ: `flipnote.notification.dlq`

### GROUP_INVITE

Queue: `notification.group-invite.queue`
Routing Key: `notification.group.invite`
Publisher: Group Service

Message fields:
  groupId: number — target group ID
  inviteeId: number — user ID being invited
  groupName: string — group name for message rendering

Effect: Creates a GROUP_INVITE notification for `inviteeId`, sends FCM push to registered devices.

### GROUP_JOIN_REQUEST

Queue: `notification.group-join-request.queue`
Routing Key: `notification.group.join-request`
Publisher: Group Service

Message fields:
  groupId: number — target group ID
  receiverIds: number[] — list of user IDs who receive the notification (e.g., group admins)
  requesterId: number — user ID who submitted the join request
  requesterNickname: string — nickname of the requester for message rendering

Effect: Creates a GROUP_JOIN_REQUEST notification for each receiver in `receiverIds`, sends FCM push to their registered devices.
