---
service: group-service
base_url: http://group-service:8084
external_base_url: http://gateway:8080
http_port: 8084
grpc_port: 9094
auth_note: >
  All endpoints require Bearer JWT via API Gateway.
  Gateway strips the Authorization header and forwards the user ID as X-USER-ID header.
  The user email is forwarded as X-User-Email where required.
---

# Group Service API

## Enums

**Category:** `IT | ENGLISH | MATH | SCIENCE | HISTORY | GEOGRAPHY | KOREAN`

**JoinPolicy:** `OPEN | APPROVAL`
- `OPEN` — anyone can join immediately
- `APPROVAL` — requires admin approval

**Visibility:** `PUBLIC | PRIVATE`

**GroupMemberRole:** `OWNER | HEAD_MANAGER | MANAGER | MEMBER`
- Priority: OWNER(4) > HEAD_MANAGER(3) > MANAGER(2) > MEMBER(1)

**GroupPermission:** `MEMBER_MANAGE | JOIN_REQUEST_MANAGE | INVITE`

**InviteStatus:** `PENDING | ACCEPTED | REJECTED | EXPIRED`

**JoinStatus:** `ACCEPT | PENDING | REJECT | CANCEL`

---

## Group Endpoints

### POST /v1/groups

**Auth:** Bearer JWT (Gateway strips, X-USER-ID forwarded)
**Description:** Create a new group. The requesting user becomes OWNER.

**Request Body:**
  name: string (required, max 50 chars) — group name
  category: enum(IT|ENGLISH|MATH|SCIENCE|HISTORY|GEOGRAPHY|KOREAN) (required) — group category
  description: string (required) — group description
  joinPolicy: enum(OPEN|APPROVAL) (required) — join policy
  visibility: enum(PUBLIC|PRIVATE) (required) — group visibility
  maxMember: number (required, min 1, max 100) — maximum number of members
  imageRefId: number (optional) — image reference ID from image service

**Response 200:**
  groupId: number — created group ID

**Errors:**
  400 GROUP_001 — invalid name (blank)
  400 GROUP_005 — group name invalid
  400 GROUP_006 — group name too long (>50)
  400 GROUP_007 — maxMember out of range
  400 GROUP_008 — invalid category
  400 GROUP_009 — invalid joinPolicy
  400 GROUP_010 — invalid visibility
  400 GROUP_011 — invalid description (blank)
  400 COMMON_001 — invalid input

---

### PUT /v1/groups/{groupId}

**Auth:** Bearer JWT (Gateway strips, X-USER-ID forwarded)
**Description:** Update group info. Requires OWNER or sufficient permission.

**Path Params:**
  groupId: number — group ID

**Request Body:**
  name: string (required) — group name
  category: enum(IT|ENGLISH|MATH|SCIENCE|HISTORY|GEOGRAPHY|KOREAN) (required) — group category
  description: string (required) — group description
  joinPolicy: enum(OPEN|APPROVAL) (required) — join policy
  visibility: enum(PUBLIC|PRIVATE) (required) — group visibility
  maxMember: number (required, min 1, max 100) — maximum number of members
  imageRefId: number (optional) — image reference ID

**Response 200:**
  groupId: number — group ID
  name: string — updated group name
  category: enum — updated category
  description: string — updated description
  joinPolicy: enum — updated join policy
  visibility: enum — updated visibility
  maxMember: number — updated max member count
  imageRefId: number — image reference ID
  createdAt: string (ISO datetime) — creation timestamp
  modifiedAt: string (ISO datetime) — last modification timestamp

**Errors:**
  403 PERM_001 — permission denied
  404 GROUP_001 — group not found

---

### GET /v1/groups/{groupId}

**Auth:** Bearer JWT (Gateway strips, X-USER-ID forwarded)
**Description:** Get group detail by ID.

**Path Params:**
  groupId: number — group ID

**Response 200:**
  groupId: number — group ID
  name: string — group name
  category: enum — category
  description: string — description
  joinPolicy: enum — join policy
  visibility: enum — visibility
  maxMember: number — max member count
  imageRefId: number — image reference ID
  imageUrl: string — resolved image URL (from image service or default)
  createdAt: string (ISO datetime) — creation timestamp
  modifiedAt: string (ISO datetime) — last modification timestamp

**Errors:**
  403 GROUP_004 — group is private and user is not a member
  404 GROUP_001 — group not found

---

### DELETE /v1/groups/{groupId}

**Auth:** Bearer JWT (Gateway strips, X-USER-ID forwarded)
**Description:** Delete a group. Only OWNER can delete.

**Path Params:**
  groupId: number — group ID

**Response 204:** (no body)

**Errors:**
  403 PERM_001 — permission denied
  403 NOT_OWNER (PERM_006) — requester is not the group owner
  404 GROUP_001 — group not found

---

### GET /v1/groups

**Auth:** Bearer JWT (Gateway strips, X-USER-ID forwarded)
**Description:** Get a paginated list of all groups (cursor-based).

**Query Params:**
  cursor: string (optional) — cursor ID for pagination
  size: number (optional, default 10, min 1, max 30) — page size
  category: enum(IT|ENGLISH|MATH|SCIENCE|HISTORY|GEOGRAPHY|KOREAN) (optional) — filter by category
  groupName: string (optional) — filter by group name (partial match)

**Response 200:**
  content: GroupInfo[] — list of groups
  hasNext: boolean — whether more pages exist
  nextCursor: string|null — cursor for next page (null if no next page)
  size: number — number of items in current page

**GroupInfo fields:**
  groupId: number
  name: string
  description: string
  category: enum
  imageRefId: number
  imageUrl: string

---

### GET /v1/groups/me

**Auth:** Bearer JWT (Gateway strips, X-USER-ID forwarded)
**Description:** Get groups the current user has joined (cursor-based).

**Query Params:**
  cursor: string (optional) — cursor ID
  size: number (optional, default 10, min 1, max 30) — page size
  category: enum (optional) — filter by category
  groupName: string (optional) — filter by name

**Response 200:** Same structure as GET /v1/groups

---

### GET /v1/groups/created

**Auth:** Bearer JWT (Gateway strips, X-USER-ID forwarded)
**Description:** Get groups created by the current user (cursor-based).

**Query Params:**
  cursor: string (optional) — cursor ID
  size: number (optional, default 10, min 1, max 30) — page size
  category: enum (optional) — filter by category
  groupName: string (optional) — filter by name

**Response 200:** Same structure as GET /v1/groups

---

### GET /v1/groups/{groupId}/managers

**Auth:** Bearer JWT (Gateway strips, X-USER-ID forwarded)
**Description:** Check if the current user is the OWNER of the group.

**Path Params:**
  groupId: number — group ID

**Response 200:**
  isOwner: boolean — true if the user is OWNER

**Errors:**
  404 GROUP_001 — group not found
  404 MEMBER_001 — member not found

---

## Member Endpoints

### GET /v1/groups/{groupId}/members

**Auth:** Bearer JWT (Gateway strips, X-USER-ID forwarded)
**Description:** Get all members of a group.

**Path Params:**
  groupId: number — group ID

**Response 200:**
  memberInfoList: MemberInfo[] — list of members

**MemberInfo fields:**
  memberId: number — group membership ID
  userId: number — user ID
  role: enum(OWNER|HEAD_MANAGER|MANAGER|MEMBER) — member role
  nickname: string — user nickname (fetched via gRPC)
  profileImage: string — user profile image URL (fetched via gRPC)

**Errors:**
  403 PERM_001 — permission denied
  404 GROUP_001 — group not found

---

### DELETE /v1/groups/{groupId}/members/{memberId}

**Auth:** Bearer JWT (Gateway strips, X-USER-ID forwarded)
**Description:** Kick a member from the group. Requester must have MEMBER_MANAGE permission or be OWNER/HEAD_MANAGER with higher role than the target.

**Path Params:**
  groupId: number — group ID
  memberId: number — membership ID to remove

**Response 204:** (no body)

**Errors:**
  403 PERM_001 — permission denied
  403 PERM_005 — requester's role is lower than target's
  404 MEMBER_001 — member not found

---

### PUT /v1/groups/{groupId}/members/{memberId}

**Auth:** Bearer JWT (Gateway strips, X-USER-ID forwarded)
**Description:** Change a member's role. Requester must have higher role than the target.

**Path Params:**
  groupId: number — group ID
  memberId: number — membership ID

**Request Body:**
  role: enum(OWNER|HEAD_MANAGER|MANAGER|MEMBER) (required) — new role to assign

**Response 200:**
  memberId: number — membership ID
  role: enum — newly assigned role

**Errors:**
  403 PERM_001 — permission denied
  403 PERM_005 — requester's role is too low
  404 MEMBER_001 — member not found

---

## Invitation Endpoints

### POST /v1/groups/{groupId}/invitations

**Auth:** Bearer JWT (Gateway strips, X-USER-ID forwarded, X-User-Email forwarded)
**Description:** Invite a user to a group by email. Requester must have INVITE permission.

**Path Params:**
  groupId: number — group ID

**Request Body:**
  email: string (required, valid email format) — email of the user to invite

**Response 201:**
  invitationId: number — created invitation ID

**Errors:**
  400 INVITE_004 — cannot invite yourself
  403 INVITE_002 — no invite permission
  404 GROUP_001 — group not found
  409 INVITE_001 — user already invited or already a member
  409 MEMBER_002 — target is already a member

---

### DELETE /v1/groups/{groupId}/invitations/{invitationId}

**Auth:** Bearer JWT (Gateway strips, X-USER-ID forwarded)
**Description:** Cancel a sent invitation. Only the original inviter can cancel.

**Path Params:**
  groupId: number — group ID
  invitationId: number — invitation ID

**Response 204:** (no body)

**Errors:**
  403 PERM_001 — permission denied
  404 INVITE_003 — invitation not found

---

### PATCH /v1/groups/{groupId}/invitations/{invitationId}

**Auth:** Bearer JWT (Gateway strips, X-USER-ID forwarded)
**Description:** Respond to a received invitation (accept or reject).

**Path Params:**
  groupId: number — group ID
  invitationId: number — invitation ID

**Request Body:**
  status: enum(ACCEPTED|REJECTED) (required) — response to the invitation

**Response 200:** (no body)

**Errors:**
  403 PERM_001 — not the invitation recipient
  404 INVITE_003 — invitation not found
  409 MEMBER_002 — already a member (when accepting)
  400 GROUP_003 — group member limit exceeded (when accepting)

---

### GET /v1/groups/{groupId}/invitations

**Auth:** Bearer JWT (Gateway strips, X-USER-ID forwarded)
**Description:** Get outgoing invitations for a group (sent by the group). Requester must have INVITE permission.

**Path Params:**
  groupId: number — group ID

**Query Params:**
  page: number (optional, default 1, min 1) — page number
  size: number (optional, default 10, min 1, max 30) — page size

**Response 200:**
  content: InviteInfo[] — list of invitations
  page: number — current page number
  size: number — items per page
  totalElements: number — total invitation count
  totalPages: number — total page count
  first: boolean — whether this is the first page
  last: boolean — whether this is the last page
  hasNext: boolean
  hasPrevious: boolean

**InviteInfo fields:**
  invitationId: number
  inviterUserId: number — ID of user who sent invite
  inviteeUserId: number — ID of invited user (null if not registered)
  inviteeEmail: string — email of invited user
  inviteeNickname: string — nickname of invited user
  status: enum(PENDING|ACCEPTED|REJECTED|EXPIRED)
  createdAt: string (ISO datetime)

---

### GET /v1/group-invitations

**Auth:** Bearer JWT (Gateway strips, X-USER-ID forwarded)
**Description:** Get invitations received by the current user (incoming invitations).

**Query Params:**
  page: number (optional, default 1) — page number
  size: number (optional, default 10, max 30) — page size

**Response 200:**
  content: InviteMyInfo[] — list of incoming invitations
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
  hasNext: boolean
  hasPrevious: boolean

**InviteMyInfo fields:**
  invitationId: number
  groupId: number
  status: enum(PENDING|ACCEPTED|REJECTED|EXPIRED)
  createdAt: string (ISO datetime)

---

## Join Request Endpoints

### POST /v1/groups/{groupId}/joins

**Auth:** Bearer JWT (Gateway strips, X-USER-ID forwarded)
**Description:** Submit a join request to a group. Only applicable when group joinPolicy is APPROVAL.

**Path Params:**
  groupId: number — group ID

**Request Body:**
  joinIntro: string (optional) — introduction message for the join request

**Response 201:**
  groupJoinId: number — join request ID
  status: enum(PENDING|ACCEPT|REJECT|CANCEL) — initial status (PENDING)

**Errors:**
  400 JOIN_004 — group is OPEN policy (direct join, no request needed) or otherwise not joinable
  403 GROUP_004 — private group
  404 GROUP_001 — group not found
  409 JOIN_002 — duplicate join request exists
  409 MEMBER_002 — already a member

---

### GET /v1/groups/{groupId}/joins

**Auth:** Bearer JWT (Gateway strips, X-USER-ID forwarded)
**Description:** Get all pending join requests for a group. Requires JOIN_REQUEST_MANAGE permission.

**Path Params:**
  groupId: number — group ID

**Response 200:**
  joinList: JoinInfo[] — list of join requests

**JoinInfo fields:**
  groupJoinId: number — join request ID
  userId: number — applicant user ID
  nickname: string — applicant nickname (fetched via gRPC)
  joinIntro: string — introduction message
  status: enum(ACCEPT|PENDING|REJECT|CANCEL)

**Errors:**
  403 PERM_001 — permission denied

---

### PATCH /v1/groups/{groupId}/joins/{joinId}

**Auth:** Bearer JWT (Gateway strips, X-USER-ID forwarded)
**Description:** Accept or reject a join request. Requires JOIN_REQUEST_MANAGE permission.

**Path Params:**
  groupId: number — group ID
  joinId: number — join request ID

**Request Body:**
  status: enum(ACCEPT|REJECT) (required) — decision on the join request

**Response 200:**
  joinId: number — join request ID
  status: enum — updated status

**Errors:**
  400 GROUP_003 — member limit exceeded (when accepting)
  403 PERM_001 — permission denied
  404 JOIN_001 — join request not found
  409 JOIN_003 — already accepted

---

### DELETE /v1/groups/{groupId}/joins/{joinId}

**Auth:** Bearer JWT (Gateway strips, X-USER-ID forwarded)
**Description:** Cancel a join request. Only the applicant can cancel their own request.

**Path Params:**
  groupId: number — group ID
  joinId: number — join request ID

**Response 200:** (no body)

**Errors:**
  403 PERM_001 — not the request owner
  404 JOIN_001 — join request not found

---

### GET /v1/groups/joins/me

**Auth:** Bearer JWT (Gateway strips, X-USER-ID forwarded)
**Description:** Get all join requests submitted by the current user.

**Response 200:**
  joinList: JoinMyInfo[] — list of own join requests

**JoinMyInfo fields:**
  groupJoinId: number — join request ID
  joinIntro: string — introduction message
  status: enum(ACCEPT|PENDING|REJECT|CANCEL)
  groupId: number — target group ID
  groupName: string — target group name

---

## Permission Endpoints

### POST /v1/groups/{groupId}/permissions

**Auth:** Bearer JWT (Gateway strips, X-USER-ID forwarded)
**Description:** Grant a permission to a role. Only OWNER can manage permissions.

**Path Params:**
  groupId: number — group ID

**Request Body:**
  changeRole: enum(OWNER|HEAD_MANAGER|MANAGER|MEMBER) (required) — role to grant permission to
  permission: enum(MEMBER_MANAGE|JOIN_REQUEST_MANAGE|INVITE) (required) — permission to grant

**Response 200:**
  role: enum — the role that was updated
  permissions: GroupPermission[] — updated permission list for that role

**Errors:**
  403 NOT_OWNER (PERM_006) — requester is not OWNER
  404 GROUP_001 — group not found
  409 PERM_002 — permission already exists for this role

---

### DELETE /v1/groups/{groupId}/permissions

**Auth:** Bearer JWT (Gateway strips, X-USER-ID forwarded)
**Description:** Revoke a permission from a role. Only OWNER can manage permissions.

**Path Params:**
  groupId: number — group ID

**Request Body:**
  changeRole: enum(OWNER|HEAD_MANAGER|MANAGER|MEMBER) (required) — role to revoke permission from
  permission: enum(MEMBER_MANAGE|JOIN_REQUEST_MANAGE|INVITE) (required) — permission to revoke

**Response 200:**
  role: enum — the role that was updated
  permissions: GroupPermission[] — updated permission list for that role

**Errors:**
  403 NOT_OWNER (PERM_006) — requester is not OWNER
  404 PERM_003 — permission not found

---

### GET /v1/groups/{groupId}/permissions

**Auth:** Bearer JWT (Gateway strips, X-USER-ID forwarded)
**Description:** Get the current user's role and permissions within the group.

**Path Params:**
  groupId: number — group ID

**Response 200:**
  role: enum(OWNER|HEAD_MANAGER|MANAGER|MEMBER) — current user's role
  permissions: GroupPermission[] — list of permissions assigned to the role

**Errors:**
  404 MEMBER_003 — user is not a member of this group

---

## gRPC Service

**Service:** `GroupCommandService` (port 9094)
**Proto package:** `group.v1`

### GetGroupName
  Request: group_id (int64)
  Response: group_name (string)
  Description: Get the name of a group by ID.

### CheckUserInGroup
  Request: group_id (int64), user_id (int64)
  Response: exists (bool)
  Description: Check if a user is a member of a group.

### GetMyGroup
  Request: user_id (int64)
  Response: group_id[] (repeated int64)
  Description: Get all group IDs the user belongs to.
