# SPOT Frontend API Contract — Fresh Start

> 목적: 기존 handoff 문서의 누적 수정 이력을 버리고, 현재 프론트엔드를 구현/연동하기 위해 필요한 **엔티티**, **스키마**, **Swagger 스타일 API 명세**를 백지 기준으로 다시 정의한다.
>
> 기준: 프론트엔드 화면/상태/액션에서 필요한 계약을 우선 정의한다. 백엔드 구현 세부사항은 이 문서에 맞춰 조정해도 되고, 불가피한 변경은 프론트와 동기화한다.

---

## 0. 공통 규칙

### Base URL


```txt
/api/v1
```



### Auth

- 기본 인증: `Authorization: Bearer <accessToken>`
- 웹 쿠키 병행 가능: `spot-auth-token` httpOnly cookie
- refresh token은 httpOnly cookie 또는 body 방식 중 하나로 통일 필요

### 날짜/시간

- 모든 timestamp: ISO 8601 string
- 기본 timezone: `Asia/Seoul`
- 날짜만 필요한 필드: `YYYY-MM-DD`

### 좌표


```ts
type GeoCoord = {
    lat: number;
    lng: number;
};
```



### 응답 envelope


```ts
type ApiResponse<T> = {
    data: T;
    meta?: PageMeta;
};

type PageMeta = {
    page: number;
    size: number;
    total: number;
    hasNext: boolean;
};

type ErrorResponse = {
    message: string;
    code?: string;
    details?: Record<string, unknown>;
};
```



### 페이지네이션

- query: `page`, `size`
- `page`는 1-base 권장
- 정렬이 필요한 API는 `sort=createdAt,desc` 형태 권장

---

## 1. 핵심 엔티티

### 1.1 User


```ts
type UserProfile = {
    id: string;
    nickname: string;
    email: string;
    phone?: string;
    avatarUrl?: string;
    pointBalance: number;
    joinedAt: string;
};

type ProfileType = 'SUPPORTER' | 'PARTNER';

type SupporterRegistrationStatus =
    | 'NOT_SUBMITTED'
    | 'PENDING'
    | 'VERIFIED'
    | 'REJECTED';

type SupporterRegistration = {
    field: string;
    mediaUrls: string[];
    career: string;
    bio: string;
    verificationStatus: SupporterRegistrationStatus;
    verificationNotes: string;
    extraNotes: string;
    updatedAt?: string;
};

type ProfileReview = {
    id: string;
    reviewerNickname: string;
    rating: 1 | 2 | 3 | 4 | 5;
    comment?: string;
    spotTitle: string;
    createdAt: string;
};

type SupporterProfile = {
    id: string;
    profileType: 'SUPPORTER';
    nickname: string;
    avatarUrl?: string;
    field: string;
    mediaUrls: string[];
    career: string;
    bio: string;
    avgRating: number;
    reviewCount: number;
    reviews: ProfileReview[];
    history: ProfileHistory[];
};

type PartnerProfile = {
    id: string;
    profileType: 'PARTNER';
    nickname: string;
    avatarUrl?: string;
    interestCategories: string[];
    isFriend: boolean;
};
```



### 1.2 Auth


```ts
type OAuthProvider = 'kakao' | 'google';

type LoginRequest = {
    email: string;
    password: string;
    next?: string;
};

type LoginResult = {
    accessToken: string;
    refreshToken: string;
    userId: string;
    redirectTo: string;
};

type RefreshTokenRequest = {
    refreshToken: string;
};

type TokenRefreshResult = {
    accessToken: string;
};
```



### 1.3 Persona / Onboarding


```ts
type UserPersonaRole = 'SUPPORTER' | 'PARTNER';
type PersonaArchetype =
    | 'explorer'
    | 'helper'
    | 'creator'
    | 'connector'
    | 'learner';

type OnboardingSelection = {
    role: UserPersonaRole;
    archetype: PersonaArchetype;
    interests: string[];
};

type UserPersona = OnboardingSelection & {
    userId: string;
    createdAt: string;
};
```



### 1.4 Spot


```ts
type SpotType = 'OFFER' | 'REQUEST';
type SpotStatus = 'OPEN' | 'MATCHED' | 'CLOSED' | 'CANCELLED';

type Spot = {
    id: string;
    type: SpotType;
    status: SpotStatus;
    title: string;
    description: string;
    pointCost: number;
    authorId: string;
    authorNickname: string;
    createdAt: string;
    updatedAt: string;
    isOwner: boolean; // 현재 인증 사용자가 권한자(작성자 또는 참여자)인지. 비인증 시 false
    forfeitPool?: {
        toPool: number;
        toPlatformFee: number;
    };
};

type SpotMapItem = {
    id: string;
    type: SpotType;
    status: SpotStatus;
    title: string;
    coord: GeoCoord;
    category: string;
    provenance?: 'virtual' | 'real' | 'mixed';
    personFitnessScore?: number;
    attractivenessScore?: number;
    authorId?: string;
    participantCount?: number;
    location?: string;
};

type TimelineEventKind =
    | 'CREATED'
    | 'MATCHED'
    | 'COMPLETED'
    | 'CANCELLED'
    | 'COMMENT'
    | 'SETTLEMENT_REQUESTED'
    | 'SETTLEMENT_APPROVED';

type TimelineEvent = {
    id: string;
    kind: TimelineEventKind;
    actorId: string;
    actorNickname: string;
    content?: string;
    createdAt: string;
};

type SpotDetail = Spot & {
    timeline: TimelineEvent[];
};
```



### 1.5 Spot Collaboration


```ts
type SpotParticipant = {
    userId: string;
    nickname: string;
    role: 'AUTHOR' | 'PARTICIPANT';
    joinedAt: string;
};

type ScheduleSlot = {
    date: string; // YYYY-MM-DD
    hour: number; // 0-23
    availableUserIds: string[];
};

type SpotSchedule = {
    spotId: string;
    proposedSlots: ScheduleSlot[];
    confirmedSlot?: ScheduleSlot;
};

type VoteOption = {
    id: string;
    label: string;
    voterIds: string[];
};

type SpotVote = {
    id: string;
    spotId: string;
    question: string;
    options: VoteOption[];
    multiSelect: boolean;
    closedAt?: string;
};

type ChecklistItem = {
    id: string;
    text: string;
    completed: boolean;
    assigneeId?: string;
    assigneeNickname?: string;
};

type SpotChecklist = {
    spotId: string;
    items: ChecklistItem[];
};

type SharedFile = {
    id: string;
    spotId: string;
    uploaderNickname: string;
    name: string;
    url: string;
    sizeBytes: number;
    uploadedAt: string;
};

type ProgressNote = {
    id: string;
    spotId: string;
    authorNickname: string;
    content: string;
    createdAt: string;
};

type SpotReview = {
    id: string;
    spotId: string;
    reviewerNickname: string;
    targetNickname: string;
    rating: 1 | 2 | 3 | 4 | 5;
    comment?: string;
    createdAt: string;
};
```



### 1.6 Feed


```ts
type FeedItemType = 'OFFER' | 'REQUEST' | 'RENT';
type FeedItemStatus = 'OPEN' | 'MATCHED' | 'CLOSED';
type FeedCategory = '음악' | '요리' | '운동' | '공예' | '언어' | '기타';
type FeedAuthorRole = 'SUPPORTER' | 'PARTNER';
type FeedApplicationStatus = 'APPLIED' | 'ACCEPTED' | 'REJECTED' | 'CANCELLED';
type FeedApplicationRole = 'SUPPORTER' | 'PARTNER';

type FeedAuthorProfile = {
    id: string;
    nickname: string;
    avatarUrl?: string;
    role: FeedAuthorRole;
    rating?: number;
    field?: string;
};

type FeedItem = {
    id: string;
    title: string;
    description?: string;
    location: string;
    authorNickname: string;
    authorProfile?: FeedAuthorProfile;
    price: number;
    type: FeedItemType;
    status: FeedItemStatus;
    progressPercent?: number;
    imageUrl?: string;
    views: number;
    likes: number;
    partnerCount?: number;
    applicantCount?: number;
    category?: FeedCategory;
    deadline?: string;
    maxParticipants?: number;
    isRentable?: boolean;
    isBookmarked?: boolean;
    myApplicationStatus?: FeedApplicationStatus;
    myApplicationRole?: FeedApplicationRole;
    myApplicationDeposit?: number;
    isOwner?: boolean; // 현재 인증 사용자가 권한자(작성자 또는 수락된 신청자)인지. 비인증 시 false
    spotId?: string;
    isAi?: boolean;
};

type FeedApplication = {
    id: string;
    feedId: string;
    userId: string;
    proposal: string;
    status: FeedApplicationStatus;
    appliedRole: FeedApplicationRole;
    deposit: number;
    createdAt: string;
};
```



### 1.7 Chat


```ts
type ChatRoomCategory = 'personal' | 'spot';
type ChatActionKind = 'vote' | 'schedule' | 'file' | 'reverse-offer';

type ChatMessage =
    | { id: string; kind: 'system'; content: string; createdAt: string }
    | {
          id: string;
          kind: 'message';
          authorId: string;
          authorName: string;
          content: string;
          createdAt: string;
      }
    | {
          id: string;
          kind: 'vote';
          authorId: string;
          authorName: string;
          vote: SpotVote;
          createdAt: string;
      }
    | {
          id: string;
          kind: 'schedule';
          authorId: string;
          authorName: string;
          schedule: ChatScheduleDraft;
          createdAt: string;
      }
    | {
          id: string;
          kind: 'file';
          authorId: string;
          authorName: string;
          file: SharedFile;
          createdAt: string;
      }
    | {
          id: string;
          kind: 'reverse-offer';
          authorId: string;
          authorName: string;
          reverseOffer: ChatReverseOfferSummary;
          createdAt: string;
      }
    | {
          id: string;
          kind: 'proposal';
          authorId: string;
          authorName: string;
          proposal: ProposalPayload;
          status: 'PENDING' | 'ACCEPTED' | 'NEGOTIATING' | 'DECLINED';
          createdAt: string;
      };

type PersonalChatRoom = {
    id: string;
    category: 'personal';
    currentUserId: string;
    currentUserName: string;
    title: string;
    subtitle: string;
    description: string;
    metaLabel: string;
    updatedAt: string;
    partnerId: string;
    partnerName: string;
    presenceLabel: string;
    unreadCount: number;
    counterpartRole: 'SUPPORTER' | 'PARTNER';
    messages: ChatMessage[];
};

type SpotChatRoom = {
    id: string;
    category: 'spot';
    currentUserId: string;
    currentUserName: string;
    title: string;
    subtitle: string;
    description: string;
    metaLabel: string;
    updatedAt: string;
    spot: SpotDetailFull;
    sourceFeedId?: string;
    participationRole?: 'SUPPORTER' | 'PARTNER';
    messages: ChatMessage[];
};

type ChatRoom = PersonalChatRoom | SpotChatRoom;
```



### 1.8 Pay / Point


```ts
type PointTransaction = {
    id: string;
    type: 'CHARGE' | 'USE' | 'REFUND' | 'WITHDRAW';
    amount: number;
    balanceAfter: number;
    description: string;
    createdAt: string;
};

type PointBalance = {
    balance: number;
    updatedAt: string;
};

type LinkedBankAccount = {
    bankName: string;
    accountNumber: string;
    accountHolder: string;
    updatedAt: string;
};

type PointWithdrawal = {
    id: string;
    amount: number;
    status: 'PENDING' | 'COMPLETED' | 'REJECTED';
    requestedAt: string;
};
```



### 1.9 Simulation / Map Data

프론트의 맵/시뮬레이션 화면은 아래 계약이 필요하다.


```ts
type SpotProvenance = 'virtual' | 'real' | 'mixed';
type SpotTeachMode = '1:1' | 'small_group' | 'workshop';
type SpotVenueType = 'cafe' | 'home' | 'studio' | 'park' | 'gym';

type ResolvedPlace = {
    place_id: number;
    name: string;
    primary_category: string;
    role: 'meetup' | 'main' | 'secondary' | 'wrapup';
    lat: number;
    lng: number;
    address: string;
    road_address?: string;
    confidence: number;
};

type PlanV3 = {
    steps: Array<{
        time: string;
        activity: string;
        place_id: number | null;
        intent: string | null;
    }>;
    total_duration_minutes: number;
};

type PriceBreakdown = {
    base_fee: number;
    included_items: Array<{ name: string; value: string }>;
    optional_addons: Array<{
        name: string;
        price: number;
        mechanism: 'fixed' | 'funding' | 'realcost';
        explanation: string | null;
    }>;
    refund_policy: {
        cutoff_hours: number;
        full_refund_until: string | null;
        note: string | null;
    } | null;
    summary_line: string | null;
};

type Preparation = {
    host_provides: string[];
    partner_brings: string[];
    weather_contingency: string | null;
    safety_notes: string[];
    host_tip: string | null;
};
```



---

## 2. DB 스키마 초안

백엔드가 RDB 기준으로 구현한다면 아래 테이블을 1차 기준으로 잡는다. 문서 목적상 상세 index/constraint는 최소만 적는다.

### users

| column        |             type | note            |
| ------------- | ---------------: | --------------- |
| id            |   uuid/string pk | user id         |
| email         |   varchar unique | login id        |
| password_hash | varchar nullable | OAuth only 가능 |
| nickname      |          varchar | 표시명          |
| phone         | varchar nullable |                 |
| avatar_url    |    text nullable |                 |
| point_balance |          integer | 캐시 가능       |
| created_at    |      timestamptz |                 |
| updated_at    |      timestamptz |                 |

### user_oauth_accounts

| column           |                   type | note               |
| ---------------- | ---------------------: | ------------------ |
| id               |         uuid/string pk |                    |
| user_id          |            fk users.id |                    |
| provider         | enum('kakao','google') |                    |
| provider_user_id |                varchar | provider 내 unique |
| created_at       |            timestamptz |                    |

### user_personas

| column     |                        type | note                                      |
| ---------- | --------------------------: | ----------------------------------------- |
| user_id    |              fk users.id pk |                                           |
| role       | enum('SUPPORTER','PARTNER') |                                           |
| archetype  |                        enum | explorer/helper/creator/connector/learner |
| interests  |                 json/text[] | string[]                                  |
| created_at |                 timestamptz |                                           |
| updated_at |                 timestamptz |                                           |

### supporter_profiles

| column              |           type | note                                    |
| ------------------- | -------------: | --------------------------------------- |
| user_id             | fk users.id pk |                                         |
| field               |        varchar | 분야                                    |
| media_urls          |    json/text[] |                                         |
| career              |           text |                                         |
| bio                 |           text |                                         |
| verification_status |           enum | NOT_SUBMITTED/PENDING/VERIFIED/REJECTED |
| verification_notes  |           text | 운영자 메모/거절 사유                   |
| extra_notes         |           text | 사용자 추가 메모                        |
| updated_at          |    timestamptz |                                         |

### spots

| column               |                                        type | note           |
| -------------------- | ------------------------------------------: | -------------- |
| id                   |                              uuid/string pk |                |
| type                 |                     enum('OFFER','REQUEST') |                |
| status               | enum('OPEN','MATCHED','CLOSED','CANCELLED') |                |
| title                |                                     varchar |                |
| description          |                                        text |                |
| point_cost           |                                     integer |                |
| author_id            |                                 fk users.id |                |
| category             |                            varchar nullable |                |
| location_label       |                            varchar nullable |                |
| lat                  |                            decimal nullable | map marker     |
| lng                  |                            decimal nullable | map marker     |
| plan_json            |                              jsonb nullable | PlanV3         |
| price_breakdown_json |                              jsonb nullable | PriceBreakdown |
| preparation_json     |                              jsonb nullable | Preparation    |
| created_at           |                                 timestamptz |                |
| updated_at           |                                 timestamptz |                |

### spot_participants

| column    |                         type | note                          |
| --------- | ---------------------------: | ----------------------------- |
| spot_id   |                  fk spots.id | composite unique with user_id |
| user_id   |                  fk users.id |                               |
| role      | enum('AUTHOR','PARTICIPANT') |                               |
| joined_at |                  timestamptz |                               |

### spot_timeline_events

| column     |           type | note                |
| ---------- | -------------: | ------------------- |
| id         | uuid/string pk |                     |
| spot_id    |    fk spots.id |                     |
| kind       |           enum | CREATED/MATCHED/... |
| actor_id   |    fk users.id |                     |
| content    |  text nullable |                     |
| created_at |    timestamptz |                     |

### feed_items

Feed는 `spots`의 projection으로 만들어도 되고, 별도 curated/admin 글과 합쳐지는 read model로 둘 수 있다.

| column           |                            type | note                     |
| ---------------- | ------------------------------: | ------------------------ |
| id               |                  uuid/string pk |                          |
| spot_id          |            fk spots.id nullable | spot 연결                |
| type             |  enum('OFFER','REQUEST','RENT') |                          |
| status           | enum('OPEN','MATCHED','CLOSED') |                          |
| title            |                         varchar |                          |
| description      |                   text nullable |                          |
| location         |                         varchar |                          |
| price            |                         integer |                          |
| author_id        |                     fk users.id |                          |
| category         |                varchar nullable |                          |
| image_url        |                   text nullable |                          |
| views            |                         integer |                          |
| likes            |                         integer |                          |
| deadline         |                   date nullable |                          |
| max_participants |                integer nullable |                          |
| is_ai            |                         boolean | simulation-origin marker |
| created_at       |                     timestamptz |                          |
| updated_at       |                     timestamptz |                          |

### feed_applications

| column       |                                              type | note |
| ------------ | ------------------------------------------------: | ---- |
| id           |                                    uuid/string pk |      |
| feed_id      |                                  fk feed_items.id |      |
| user_id      |                                       fk users.id |      |
| proposal     |                                              text |      |
| applied_role |                       enum('SUPPORTER','PARTNER') |      |
| deposit      |                                           integer |      |
| status       | enum('APPLIED','ACCEPTED','REJECTED','CANCELLED') |      |
| created_at   |                                       timestamptz |      |
| updated_at   |                                       timestamptz |      |

### chats / chat_messages

| table             | key fields                                              |
| ----------------- | ------------------------------------------------------- |
| chat_rooms        | id, category, title, spot_id?, created_at, updated_at   |
| chat_room_members | room_id, user_id, role_label?, last_read_at             |
| chat_messages     | id, room_id, kind, author_id?, payload_json, created_at |

`kind`별 payload는 ChatMessage union을 따른다.

### point_transactions / withdrawals / bank_accounts

| table                | key fields                                                            |
| -------------------- | --------------------------------------------------------------------- |
| point_transactions   | id, user_id, type, amount, balance_after, description, created_at     |
| linked_bank_accounts | user_id, bank_name, account_number_masked, account_holder, updated_at |
| point_withdrawals    | id, user_id, amount, status, requested_at, processed_at?              |

### bookmarks / recent_views / notifications

| table                 | key fields                                                                                 |
| --------------------- | ------------------------------------------------------------------------------------------ |
| bookmarks             | id, user_id, target_type, target_id, created_at                                            |
| recent_views          | id, user_id, target_type, target_id, viewed_at                                             |
| notifications         | id, user_id, type, title, body, read_at?, created_at                                       |
| notification_settings | user_id, service_notice_enabled, activity_enabled, push_enabled, email_enabled, updated_at |

---

## 3. Swagger 스타일 API 명세

아래는 OpenAPI 느낌의 프론트 필요 API 목록이다.

### 3.1 Auth

#### POST `/auth/login`

- **사용 위치:** 로그인 페이지.
- **하는 일:** 이메일/비밀번호를 검증하고 access token, refresh token, 로그인 후 이동 경로를 반환한다.
- **프론트 액션:** 로그인 폼 제출 시 호출하고, 성공하면 토큰 저장 후 `redirectTo`로 이동한다.


```yaml
summary: 이메일 로그인
requestBody:
    application/json:
        email: string
        password: string
        next?: string
responses:
    200:
        application/json: LoginResult
    400:
        application/json: ErrorResponse
```



#### POST `/auth/logout`

- **사용 위치:** 마이페이지/설정/헤더의 로그아웃 버튼.
- **하는 일:** 서버 세션 또는 인증 쿠키를 만료시킨다.
- **프론트 액션:** 로그아웃 버튼 클릭 시 호출하고, 성공하면 로컬 인증 상태를 비운 뒤 로그인 화면으로 보낸다.


```yaml
summary: 로그아웃
responses:
    200:
        application/json:
            ok: true
```



#### POST `/auth/refresh`

- **사용 위치:** API 클라이언트 공통 인증 레이어.
- **하는 일:** 만료된 access token을 refresh token으로 재발급한다.
- **프론트 액션:** 401 응답을 받았을 때 자동 호출하고, 새 토큰으로 원래 요청을 재시도한다.


```yaml
summary: access token 갱신
requestBody:
    application/json:
        refreshToken?: string
responses:
    200:
        application/json: ApiResponse<TokenRefreshResult>
    401:
        application/json: ErrorResponse
```



#### GET `/auth/oauth/{provider}/start`

- **사용 위치:** 로그인 페이지의 카카오/구글 로그인 버튼.
- **하는 일:** 선택한 OAuth provider 인증 플로우를 시작한다.
- **프론트 액션:** 버튼 클릭 시 이 URL로 이동시킨다. API 응답 JSON을 읽는 게 아니라 redirect 흐름이다.


```yaml
summary: OAuth 로그인 시작
parameters:
    path:
        provider: kakao | google
    query:
        next?: string
responses:
    302:
        description: provider 인증 페이지 또는 post-login path로 redirect
```



---

### 3.2 Me / User

#### GET `/me`

- **사용 위치:** 앱 초기 진입, 마이페이지, 헤더/프로필 영역.
- **하는 일:** 현재 로그인한 사용자의 기본 프로필과 포인트 잔액을 가져온다.
- **프론트 액션:** 인증 상태 확인 및 내 정보 화면 렌더링에 사용한다.


```yaml
summary: 내 프로필 조회
security: bearerAuth
responses:
    200:
        application/json: ApiResponse<UserProfile>
```



#### PATCH `/me`

- **사용 위치:** 마이페이지 프로필 수정 화면.
- **하는 일:** 닉네임, 이메일, 전화번호, 프로필 이미지를 수정한다.
- **프론트 액션:** 저장 버튼 클릭 시 변경된 필드만 보내고, 응답값으로 화면/캐시를 갱신한다.


```yaml
summary: 내 프로필 수정
requestBody:
    application/json:
        nickname?: string
        email?: string
        phone?: string
        avatarUrl?: string
responses:
    200:
        application/json: ApiResponse<UserProfile>
```



#### PATCH `/me/password`

- **사용 위치:** 마이페이지 보안/비밀번호 변경 폼.
- **하는 일:** 현재 비밀번호 확인 후 새 비밀번호로 변경한다.
- **프론트 액션:** 변경 성공 시 입력 폼을 초기화하고 완료 메시지를 보여준다.


```yaml
summary: 비밀번호 변경
requestBody:
    application/json:
        currentPassword: string
        newPassword: string
        confirmPassword: string
responses:
    204:
        description: no content
```



#### GET `/me/notification-settings`

- **사용 위치:** 알림 설정 화면.
- **하는 일:** 사용자의 서비스/활동/push/email 알림 on/off 상태를 가져온다.
- **프론트 액션:** 설정 토글들의 초기값으로 사용한다.


```yaml
responses:
    200:
        application/json: ApiResponse<NotificationSettings>
```



#### PUT `/me/notification-settings`

- **사용 위치:** 알림 설정 화면.
- **하는 일:** 알림 토글 설정값 전체를 저장한다.
- **프론트 액션:** 토글 변경 후 저장하거나 즉시 저장할 때 호출한다.


```yaml
requestBody:
    application/json:
        serviceNoticeEnabled: boolean
        activityEnabled: boolean
        pushEnabled: boolean
        emailEnabled: boolean
responses:
    200:
        application/json: ApiResponse<NotificationSettings>
```



#### GET `/me/supporter-registration`

- **사용 위치:** 서포터 등록/검증 신청 화면.
- **하는 일:** 내가 제출한 서포터 프로필 신청 상태와 내용을 가져온다.
- **프론트 액션:** 신청 폼 초기값, 심사 상태 배지, 거절 사유 표시에 사용한다.


```yaml
responses:
    200:
        application/json: ApiResponse<SupporterRegistration>
```



#### PUT `/me/supporter-registration`

- **사용 위치:** 서포터 등록/검증 신청 화면.
- **하는 일:** 분야, 경력, 소개, 인증 자료를 제출하거나 수정한다.
- **프론트 액션:** 제출 버튼 클릭 시 호출하고, 응답의 `verificationStatus`로 상태 UI를 갱신한다.


```yaml
requestBody:
    application/json:
        field: string
        mediaUrls: string[]
        career: string
        bio: string
        verificationStatus?: SupporterRegistrationStatus # BE가 무시/관리 권장
        verificationNotes?: string # BE가 무시/관리 권장
        extraNotes: string
responses:
    200:
        application/json: ApiResponse<SupporterRegistration>
```



#### GET `/users/{userId}/supporter-profile`

- **사용 위치:** 서포터 프로필 상세, 피드 작성자/지원자 상세.
- **하는 일:** 특정 사용자의 서포터 공개 프로필, 후기, 활동 이력을 가져온다.
- **프론트 액션:** 서포터 카드 클릭 시 상세 페이지/모달 렌더링에 사용한다.


```yaml
responses:
    200:
        application/json: ApiResponse<SupporterProfile>
```



#### GET `/users/{userId}/partner-profile`

- **사용 위치:** 파트너 프로필 상세, 채팅 상대/참여자 상세.
- **하는 일:** 특정 사용자의 파트너 공개 프로필과 관심 카테고리를 가져온다.
- **프론트 액션:** 파트너 카드 클릭 시 상세 정보 표시에 사용한다.


```yaml
responses:
    200:
        application/json: ApiResponse<PartnerProfile>
```



---

### 3.3 Onboarding / Persona

#### GET `/me/persona`

- **사용 위치:** 온보딩 진입 판단, 앱 초기화.
- **하는 일:** 현재 사용자가 온보딩에서 선택한 역할/성향/관심사를 가져온다.
- **프론트 액션:** 값이 없으면 온보딩으로 보내고, 있으면 개인화 추천/필터 기본값에 사용한다.


```yaml
responses:
    200:
        application/json: ApiResponse<UserPersona | null>
```



#### PUT `/me/persona`

- **사용 위치:** 온보딩 마지막 단계.
- **하는 일:** 사용자의 역할, 페르소나 타입, 관심사를 저장한다.
- **프론트 액션:** 온보딩 완료 버튼 클릭 시 호출하고, 성공하면 메인 맵/피드로 이동한다.


```yaml
requestBody:
    application/json:
        role: SUPPORTER | PARTNER
        archetype: explorer | helper | creator | connector | learner
        interests: string[]
responses:
    200:
        application/json: ApiResponse<UserPersona>
```



---

### 3.4 Feed

#### GET `/feeds`

- **사용 위치:** 홈 피드, 탐색 피드, 맵 바텀시트 피드 리스트.
- **하는 일:** 조건에 맞는 OFFER/REQUEST/RENT 카드 목록을 가져온다.
- **프론트 액션:** 탭/카테고리/검색어/위치 필터 변경 시 재호출한다.


```yaml
summary: 홈/탐색 피드 목록
parameters:
    query:
        type?: OFFER | REQUEST | RENT
        category?: string
        status?: OPEN | MATCHED | CLOSED
        q?: string
        nearLat?: number  # nearLng 와 함께 제공 시 반경 5km 필터 적용 (선택)
        nearLng?: number  # nearLat 와 함께 제공 시 반경 5km 필터 적용 (선택)
        page?: number
        size?: number
responses:
    200:
        application/json: ApiResponse<FeedItem[]>
```



#### GET `/feeds/{feedId}`

- **사용 위치:** 피드 상세 페이지.
- **하는 일:** 피드 카드 하나의 상세 정보와 현재 내 신청/북마크 상태를 가져온다.
- **프론트 액션:** 카드 클릭 후 상세 화면 렌더링에 사용한다.


```yaml
responses:
    200:
        application/json: ApiResponse<FeedItem>
    404:
        application/json: ErrorResponse
```



#### POST `/feeds/{feedId}/applications`

- **사용 위치:** 피드 상세의 신청/참여 버튼.
- **하는 일:** 해당 피드에 내 지원서/참여 신청을 생성한다.
- **프론트 액션:** 제안 메시지와 보증금 정보를 보내고, 성공하면 버튼 상태를 신청 완료로 바꾼다.


```yaml
summary: 피드 신청
requestBody:
    application/json:
        proposal: string
        role: SUPPORTER | PARTNER
        deposit: number
responses:
    201:
        application/json: ApiResponse<FeedApplication>
```



#### DELETE `/feeds/{feedId}/applications/me`

- **사용 위치:** 피드 상세의 신청 취소 버튼.
- **하는 일:** 내가 넣은 신청을 취소 상태로 변경한다.
- **프론트 액션:** 취소 확인 후 호출하고, 신청 상태/보증금 UI를 갱신한다.


```yaml
summary: 내 신청 취소
responses:
    200:
        application/json:
            data:
                feedId: string
                status: CANCELLED
```



#### GET `/feeds/{feedId}/applications`

- **사용 위치:** 피드 관리 화면 (작성자 전용).
- **하는 일:** 해당 피드에 들어온 신청 목록을 반환한다. 작성자만 호출 가능.
- **프론트 액션:** 신청자 목록을 렌더링하고, 각 항목의 `id`로 수락/거절 API를 호출한다.


```yaml
summary: 피드 신청 목록 조회 (작성자 전용)
security: bearerAuth
parameters:
    path:
        feedId: number
responses:
    200:
        application/json: ApiResponse<FeedApplication[]>
    403:
        application/json: ErrorResponse # 작성자 아닌 경우
    404:
        application/json: ErrorResponse
```



#### POST `/feeds/{feedId}/bookmark`

- **사용 위치:** 피드 카드/상세의 북마크 버튼.
- **하는 일:** 해당 피드를 내 관심 목록에 추가한다.
- **프론트 액션:** 북마크 아이콘을 활성화하고 마이페이지 관심 목록 캐시를 갱신한다.


```yaml
summary: 북마크 추가
responses:
    204: {}
```



#### DELETE `/feeds/{feedId}/bookmark`

- **사용 위치:** 피드 카드/상세/마이페이지 관심 목록.
- **하는 일:** 해당 피드를 내 관심 목록에서 제거한다.
- **프론트 액션:** 북마크 아이콘을 비활성화하거나 목록에서 제거한다.


```yaml
summary: 북마크 제거
responses:
    204: {}
```



---

### 3.5 Spots

#### GET `/spots`

- **사용 위치:** 내 스팟 목록, 스팟 관리 화면, 검색/필터 기반 목록.
- **하는 일:** 조건에 맞는 스팟 원본 목록을 가져온다. Feed보다 운영/워크플로우 중심 데이터다.
- **프론트 액션:** 참여 중/작성한/상태별 스팟 목록 렌더링에 사용한다.


```yaml
summary: 스팟 목록
parameters:
    query:
        type?: OFFER | REQUEST
        status?: OPEN | MATCHED | CLOSED | CANCELLED
        participating?: boolean
        page?: number
        size?: number
responses:
    200:
        application/json: ApiResponse<Spot[]>
```



#### GET `/spots/map`

- **사용 위치:** 메인 지도 화면.
- **하는 일:** 현재 지도 bounds 안에 표시할 마커용 경량 스팟 데이터를 가져온다.
- **프론트 액션:** 지도 이동/줌/필터 변경 시 호출해서 마커와 바텀시트를 갱신한다.


```yaml
summary: 지도 마커용 스팟 목록
parameters:
    query:
        swLat?: number
        swLng?: number
        neLat?: number
        neLng?: number
        category?: string
        type?: OFFER | REQUEST
        status?: OPEN | MATCHED | CLOSED | CANCELLED
responses:
    200:
        application/json: ApiResponse<SpotMapItem[]>
```



#### GET `/spots/search`

- **사용 위치:** 지도/상단 검색창.
- **하는 일:** 검색어와 일치하는 스팟을 찾는다. `scope`로 검색 범위(제목/설명/둘다)를 지정한다.
- **프론트 액션:** 검색어 입력 후 결과 리스트/자동완성/지도 이동에 사용한다. 디시인사이드식 "제목만 / 내용만 / 제목+내용" 토글에 `scope` 매핑.


```yaml
summary: 스팟 검색
parameters:
    query:
        q: string
        scope?: ALL | TITLE | CONTENT   # 기본 ALL(제목+설명), TITLE(제목만), CONTENT(설명만)
        page?: number
        size?: number
responses:
    200:
        application/json: ApiResponse<Spot[]>
```



#### POST `/spots`

- **사용 위치:** OFFER 작성, REQUEST 작성 페이지.
- **하는 일:** 새 스팟을 생성한다. 필요한 경우 계획/가격/준비물 정보도 함께 저장한다.
- **프론트 액션:** 작성 폼 제출 시 호출하고, 성공하면 생성된 스팟 상세 또는 완료 화면으로 이동한다.


```yaml
summary: OFFER/REQUEST 작성
requestBody:
    application/json:
        type: OFFER | REQUEST
        title: string
        description: string
        pointCost: number
        category?: string
        location?: string
        coord?: GeoCoord
        deadline?: string
        plan?: PlanV3
        priceBreakdown?: PriceBreakdown
        preparation?: Preparation
responses:
    201:
        application/json: ApiResponse<Spot>
```



#### GET `/spots/{spotId}`

- **사용 위치:** 스팟 상세, 채팅방 내 스팟 컨텍스트, 워크플로우 패널.
- **하는 일:** 스팟 기본 정보와 타임라인을 가져온다.
- **프론트 액션:** 상세 상단 정보, 상태 배지, 활동 로그를 렌더링한다.


```yaml
responses:
    200:
        application/json: ApiResponse<SpotDetail>
```



#### POST `/spots/{spotId}/match`

- **사용 위치:** 스팟 관리/지원자 선택 화면.
- **하는 일:** 스팟을 매칭 완료 상태로 전환한다.
- **프론트 액션:** 매칭 확정 버튼 클릭 시 호출하고 상태를 MATCHED로 갱신한다.


```yaml
summary: 스팟 매칭 처리
responses:
    200:
        application/json: ApiResponse<Spot>
```



#### POST `/spots/{spotId}/cancel`

- **사용 위치:** 스팟 상세/관리 화면의 취소 버튼.
- **하는 일:** 진행 중인 스팟을 취소 처리한다.
- **프론트 액션:** 취소 확인 모달 이후 호출하고 상태를 CANCELLED로 갱신한다.


```yaml
responses:
    200:
        application/json: ApiResponse<Spot>
```



#### POST `/spots/{spotId}/complete`

- **사용 위치:** 스팟 진행 완료 화면.
- **하는 일:** 스팟을 완료/종료 상태로 전환한다.
- **프론트 액션:** 완료 버튼 클릭 후 리뷰/정산 플로우로 이어진다.


```yaml
responses:
    200:
        application/json: ApiResponse<Spot>
```



#### GET `/spots/{spotId}/participants`

- **사용 위치:** 스팟 상세 참여자 섹션, 채팅 멤버 목록.
- **하는 일:** 작성자와 참여자 목록을 가져온다.
- **프론트 액션:** 참여자 아바타, 역할 표시, 멤버 수 계산에 사용한다.


```yaml
responses:
    200:
        application/json: ApiResponse<SpotParticipant[]>
```



#### GET `/spots/{spotId}/schedule`

- **사용 위치:** 스팟 일정 조율 섹션, 채팅 액션 패널.
- **하는 일:** 제안된 일정 후보와 확정 일정을 가져온다.
- **프론트 액션:** 캘린더/시간표 UI 초기 렌더링에 사용한다.


```yaml
responses:
    200:
        application/json: ApiResponse<SpotSchedule | null>
```



#### PUT `/spots/{spotId}/schedule`

- **사용 위치:** 일정 조율 폼.
- **하는 일:** 일정 후보 또는 확정 일정을 저장한다.
- **프론트 액션:** 사용자가 가능한 시간을 선택/수정한 뒤 저장할 때 호출한다.


```yaml
requestBody:
    application/json:
        proposedSlots: ScheduleSlot[]
        confirmedSlot?: ScheduleSlot
responses:
    200:
        application/json: ApiResponse<SpotSchedule>
```



#### GET `/spots/{spotId}/votes`

- **사용 위치:** 스팟 투표 섹션, 채팅 투표 탭.
- **하는 일:** 해당 스팟의 투표 목록과 현재 득표 상태를 가져온다.
- **프론트 액션:** 투표 카드들을 렌더링하고 내가 선택 가능한 옵션을 보여준다.


```yaml
responses:
    200:
        application/json: ApiResponse<SpotVote[]>
```



#### POST `/spots/{spotId}/votes`

- **사용 위치:** 새 투표 만들기 폼.
- **하는 일:** 스팟 안에 새 투표를 생성한다.
- **프론트 액션:** 질문/옵션 입력 후 생성 버튼 클릭 시 호출한다.


```yaml
requestBody:
    application/json:
        question: string
        options: string[]
        multiSelect?: boolean
responses:
    201:
        application/json: ApiResponse<SpotVote>
```



#### POST `/spots/{spotId}/votes/{voteId}/cast`

- **사용 위치:** 투표 카드.
- **하는 일:** 내가 선택한 옵션에 투표한다.
- **프론트 액션:** 옵션 선택 후 제출하면 결과 count와 winner 표시를 갱신한다.


```yaml
requestBody:
    application/json:
        optionIds: string[]
responses:
    200:
        application/json: ApiResponse<SpotVote>
```



#### GET `/spots/{spotId}/checklist`

- **사용 위치:** 스팟 준비 체크리스트 섹션.
- **하는 일:** 준비/진행 체크리스트 항목을 가져온다.
- **프론트 액션:** 체크박스 목록 초기 렌더링에 사용한다.


```yaml
responses:
    200:
        application/json: ApiResponse<SpotChecklist | null>
```



#### PUT `/spots/{spotId}/checklist`

- **사용 위치:** 체크리스트 편집/체크 액션.
- **하는 일:** 체크리스트 항목 전체 상태를 저장한다.
- **프론트 액션:** 항목 추가/수정/완료 체크 후 서버와 동기화한다.


```yaml
requestBody:
    application/json:
        items: ChecklistItem[]
responses:
    200:
        application/json: ApiResponse<SpotChecklist>
```



#### GET `/spots/{spotId}/files`

- **사용 위치:** 스팟 파일 공유 섹션, 채팅 파일 탭.
- **하는 일:** 공유된 파일 목록을 가져온다.
- **프론트 액션:** 첨부파일 리스트와 다운로드 링크를 렌더링한다.


```yaml
responses:
    200:
        application/json: ApiResponse<SharedFile[]>
```



#### POST `/spots/{spotId}/files`

- **사용 위치:** 파일 업로드 버튼.
- **하는 일:** 스팟에 새 공유 파일을 업로드한다.
- **프론트 액션:** 파일 선택 후 업로드하고, 성공하면 파일 목록에 추가한다.


```yaml
requestBody:
    multipart/form-data:
        file: binary
responses:
    201:
        application/json: ApiResponse<SharedFile>
```



#### DELETE `/spots/{spotId}/files/{fileId}`

- **사용 위치:** 파일 공유 섹션의 삭제 버튼.
- **하는 일:** 공유 파일을 삭제한다.
- **프론트 액션:** 삭제 확인 후 호출하고 파일 목록에서 제거한다.


```yaml
responses:
    204: {}
```



#### GET `/spots/{spotId}/notes`

- **사용 위치:** 진행 노트/활동 기록 섹션.
- **하는 일:** 스팟 진행 중 남긴 노트 목록을 가져온다.
- **프론트 액션:** 진행 기록 타임라인 또는 노트 리스트를 렌더링한다.


```yaml
responses:
    200:
        application/json: ApiResponse<ProgressNote[]>
```



#### POST `/spots/{spotId}/notes`

- **사용 위치:** 진행 노트 입력 폼.
- **하는 일:** 새 진행 노트를 추가한다.
- **프론트 액션:** 노트 작성 후 전송하면 목록 최상단/타임라인에 반영한다.


```yaml
requestBody:
    application/json:
        content: string
responses:
    201:
        application/json: ApiResponse<ProgressNote>
```



#### GET `/spots/{spotId}/reviews`

- **사용 위치:** 스팟 후기 섹션, 프로필 후기 연결.
- **하는 일:** 해당 스팟에 작성된 후기를 가져온다.
- **프론트 액션:** 별점/코멘트 리스트를 렌더링한다.


```yaml
responses:
    200:
        application/json: ApiResponse<SpotReview[]>
```



#### POST `/spots/{spotId}/reviews`

- **사용 위치:** 스팟 완료 후 후기 작성 화면.
- **하는 일:** 상대방에 대한 별점과 후기를 등록한다.
- **프론트 액션:** 제출 후 후기 목록과 프로필 평점 캐시를 갱신한다.


```yaml
requestBody:
    application/json:
        targetNickname: string
        rating: 1 | 2 | 3 | 4 | 5
        comment?: string
responses:
    201:
        application/json: ApiResponse<SpotReview>
```



#### POST `/spots/{spotId}/settlement`

- **사용 위치:** 스팟 정산 요청 화면.
- **하는 일:** 정산 항목과 요약을 제출해 승인 대기 상태를 만든다.
- **프론트 액션:** 정산 요청 버튼 클릭 시 호출하고 승인 상태 UI를 보여준다.


```yaml
requestBody:
    application/json:
        lineItems:
            - label: string
              amount: number
        summary: string
responses:
    200:
        application/json: ApiResponse<SpotSettlementApproval>
```



#### POST `/spots/{spotId}/settlement/approve`

- **사용 위치:** 정산 승인 화면/워크플로우 인박스.
- **하는 일:** 제출된 정산 요청을 승인한다.
- **프론트 액션:** 승인 버튼 클릭 시 호출하고 정산 완료 상태로 갱신한다.


```yaml
responses:
    200:
        application/json: ApiResponse<SpotSettlementApproval>
```



---

### 3.6 Chat

#### GET `/chat/rooms`

- **사용 위치:** 채팅 메인 목록.
- **하는 일:** 개인 채팅방과 스팟 팀 채팅방 목록을 가져온다.
- **프론트 액션:** 탭/필터별 채팅 리스트, unread count, 최신 메시지 표시를 위해 사용한다.


```yaml
parameters:
    query:
        category?: personal | spot
        tab?: personal | team
        filter?: string
        page?: number
        size?: number
responses:
    200:
        application/json: ApiResponse<ChatRoom[]>
```



#### GET `/chat/rooms/{roomId}`

- **사용 위치:** 채팅방 상세.
- **하는 일:** 특정 채팅방 정보와 최근 메시지, 연결된 스팟 컨텍스트를 가져온다.
- **프론트 액션:** 채팅방 진입 시 헤더/메시지 영역/스팟 액션 패널을 렌더링한다.


```yaml
responses:
    200:
        application/json: ApiResponse<ChatRoom>
```



#### POST `/chat/rooms`

- **사용 위치:** 사용자 프로필의 1:1 채팅 시작, 스팟 참여 후 팀 채팅 진입.
- **하는 일:** 조건에 맞는 채팅방을 만들거나 이미 있으면 기존 방을 반환한다.
- **프론트 액션:** 채팅 시작 버튼 클릭 후 반환된 roomId로 이동한다.


```yaml
summary: 개인/스팟 채팅방 생성 또는 기존 방 반환
requestBody:
    application/json:
        category: personal | spot
        partnerId?: string
        spotId?: string
responses:
    200:
        application/json: ApiResponse<ChatRoom>
```



#### GET `/chat/rooms/{roomId}/messages`

- **사용 위치:** 채팅방 메시지 리스트.
- **하는 일:** 메시지를 cursor 기반으로 페이지네이션 조회한다.
- **프론트 액션:** 첫 진입/위로 스크롤 시 이전 메시지를 불러온다.


```yaml
parameters:
    query:
        cursor?: string
        size?: number
responses:
    200:
        application/json:
            data: ChatMessage[]
            meta:
                nextCursor?: string
                hasNext: boolean
```



#### POST `/chat/rooms/{roomId}/messages`

- **사용 위치:** 채팅 입력창.
- **하는 일:** 일반 텍스트 메시지를 전송한다.
- **프론트 액션:** 전송 버튼/Enter 입력 시 호출하고 optimistic UI 또는 응답값으로 메시지를 추가한다.


```yaml
requestBody:
    application/json:
        kind: message
        content: string
responses:
    201:
        application/json: ApiResponse<ChatMessage>
```



#### GET `/chat/rooms/{roomId}/stream`

- **사용 위치:** 열린 채팅방의 실시간 이벤트 연결.
- **하는 일:** 새 메시지, 읽음, 입력 중 이벤트를 SSE로 전달한다.
- **프론트 액션:** 채팅방 mount 시 연결하고 unmount 시 닫는다.


```yaml
summary: SSE 채팅 이벤트 스트림
responses:
    200:
        text/event-stream:
            events:
                - type: message
                - type: read
                - type: typing
```



#### POST `/chat/rooms/{roomId}/read`

- **사용 위치:** 채팅방 진입/메시지 확인 시점.
- **하는 일:** 현재 사용자의 last read 상태를 갱신한다.
- **프론트 액션:** unread count를 0으로 만들고 목록 배지를 갱신한다.


```yaml
responses:
    204: {}
```



#### GET `/chat/action-items`

- **사용 위치:** 채팅 메인 팀 탭의 투표/일정/파일/역제안 필터.
- **하는 일:** 채팅 메시지 안의 액션성 항목만 모아서 보여준다.
- **프론트 액션:** 탭 아이템 클릭 시 관련 채팅방과 actionId로 이동한다.


```yaml
summary: 채팅 탭에 노출할 투표/일정/파일/역제안 액션 아이템
parameters:
    query:
        kind?: vote | schedule | file | reverse-offer
responses:
    200:
        application/json: ApiResponse<SpotActionItem[]>
```



---

### 3.7 Pay / Points

#### GET `/pay/balance`

- **사용 위치:** 포인트/결제 화면, 마이페이지 포인트 배지.
- **하는 일:** 현재 포인트 잔액을 가져온다.
- **프론트 액션:** 결제/신청 전 잔액 검증과 화면 표시용으로 사용한다.


```yaml
responses:
    200:
        application/json: ApiResponse<PointBalance>
```



#### GET `/pay/history`

- **사용 위치:** 포인트 사용 내역 화면.
- **하는 일:** 충전/사용/환불/출금 거래 내역을 페이지로 가져온다.
- **프론트 액션:** 내역 리스트와 무한스크롤/페이지네이션에 사용한다.


```yaml
parameters:
    query:
        page?: number
        size?: number
responses:
    200:
        application/json: ApiResponse<PointTransaction[]>
```



#### POST `/pay/charge`

- **사용 위치:** 포인트 충전 화면.
- **하는 일:** 지정 금액만큼 포인트 충전을 요청한다.
- **프론트 액션:** 충전 버튼 클릭 시 호출하고, 실제 PG가 붙으면 결제 승인 플로우와 연결한다.


```yaml
requestBody:
    application/json:
        amount: number
responses:
    200:
        application/json: ApiResponse<PointBalance>
```



#### GET `/pay/bank-account`

- **사용 위치:** 출금 계좌 설정/출금 화면.
- **하는 일:** 연결된 계좌 정보를 가져온다.
- **프론트 액션:** 계좌 입력 폼 초기값 또는 등록 여부 표시에 사용한다.


```yaml
responses:
    200:
        application/json: ApiResponse<LinkedBankAccount | null>
```



#### PUT `/pay/bank-account`

- **사용 위치:** 출금 계좌 등록/수정 폼.
- **하는 일:** 출금 받을 은행 계좌를 등록하거나 수정한다.
- **프론트 액션:** 계좌 저장 버튼 클릭 시 호출한다.


```yaml
requestBody:
    application/json:
        bankName: string
        accountNumber: string
        accountHolder: string
responses:
    200:
        application/json: ApiResponse<LinkedBankAccount>
```



#### GET `/pay/withdrawals`

- **사용 위치:** 출금 신청 내역 화면.
- **하는 일:** 과거 출금 요청과 처리 상태를 가져온다.
- **프론트 액션:** PENDING/COMPLETED/REJECTED 상태를 리스트로 보여준다.


```yaml
parameters:
    query:
        page?: number
        size?: number
responses:
    200:
        application/json: ApiResponse<PointWithdrawal[]>
```



#### POST `/pay/withdrawals`

- **사용 위치:** 포인트 출금 신청 화면.
- **하는 일:** 지정 금액의 출금 요청을 생성하고 잔액을 갱신한다.
- **프론트 액션:** 출금 신청 버튼 클릭 시 호출하고 잔액/내역을 갱신한다.


```yaml
requestBody:
    application/json:
        amount: number
responses:
    200:
        application/json: ApiResponse<PointBalance>
```



---

### 3.8 My Activity

#### GET `/me/involved-feeds`

- **사용 위치:** 지도 화면에서 내가 관여한 피드 마커 표시, 마이페이지 활동 피드 목록.
- **하는 일:** 내가 작성자인 피드 + 신청한 피드(APPLIED/ACCEPTED/REJECTED/CANCELLED) + Spot 전환 후 softDelete된 피드를 한 번에 반환한다.
- **프론트 액션:** 각 피드의 `myApplicationStatus`로 신청 상태를 구분하고, 지도 마커에 "내 피드" 표시를 적용한다.


```yaml
summary: 내가 관여한 피드 목록
security: bearerAuth
responses:
    200:
        application/json: ApiResponse<FeedItem[]>
        # myApplicationStatus: null → 내가 작성한 피드
        # myApplicationStatus: APPLIED | ACCEPTED | REJECTED | CANCELLED → 내가 신청한 피드
```



#### GET `/me/participations`

- **사용 위치:** 마이페이지 참여 내역.
- **하는 일:** 내가 작성자 또는 참여자로 들어간 스팟 목록을 가져온다.
- **프론트 액션:** 진행 중/완료된 활동 목록 렌더링에 사용한다.


```yaml
parameters:
    query:
        page?: number
        size?: number
responses:
    200:
        application/json: ApiResponse<Participation[]>
```



#### GET `/me/favorites`

- **사용 위치:** 마이페이지 관심/북마크 목록.
- **하는 일:** 내가 저장한 피드/스팟 목록을 가져온다.
- **프론트 액션:** 관심 목록 카드 렌더링에 사용한다.


```yaml
responses:
    200:
        application/json: ApiResponse<MyFavoriteItem[]>
```



#### DELETE `/me/favorites/{favoriteId}`

- **사용 위치:** 마이페이지 관심 목록의 삭제 버튼.
- **하는 일:** 저장된 관심 항목을 제거한다.
- **프론트 액션:** 삭제 후 리스트에서 항목을 제거한다.


```yaml
responses:
    204: {}
```



#### GET `/me/recent-views`

- **사용 위치:** 마이페이지 최근 본 항목.
- **하는 일:** 내가 최근 열람한 피드/스팟 목록을 가져온다.
- **프론트 액션:** 최근 본 목록 렌더링과 빠른 재진입에 사용한다.


```yaml
responses:
    200:
        application/json: ApiResponse<MyRecentViewItem[]>
```



#### DELETE `/me/recent-views/{recentViewId}`

- **사용 위치:** 최근 본 항목 개별 삭제 버튼.
- **하는 일:** 특정 최근 본 기록을 제거한다.
- **프론트 액션:** 삭제 후 해당 항목을 화면에서 제거한다.


```yaml
responses:
    204: {}
```



#### DELETE `/me/recent-views`

- **사용 위치:** 최근 본 항목 전체 삭제 버튼.
- **하는 일:** 내 최근 본 기록을 모두 삭제한다.
- **프론트 액션:** 전체 삭제 확인 후 호출하고 리스트를 비운다.


```yaml
summary: 최근 본 항목 전체 삭제
responses:
    204: {}
```



#### GET `/me/support-activity-summary`

- **사용 위치:** 마이페이지 서포터 활동 요약 카드.
- **하는 일:** 평균 평점, 후기 수, 완료 수, 최신 후기를 가져온다.
- **프론트 액션:** 활동 성과 요약 카드 렌더링에 사용한다.


```yaml
responses:
    200:
        application/json: ApiResponse<MySupportActivitySummary>
```



---

### 3.9 Notifications

#### GET `/notifications`

- **사용 위치:** 알림 페이지/알림 드롭다운.
- **하는 일:** 내 알림 목록을 가져온다.
- **프론트 액션:** 읽음/안읽음 필터와 알림 리스트 렌더링에 사용한다.


```yaml
parameters:
    query:
        unreadOnly?: boolean
        page?: number
        size?: number
responses:
    200:
        application/json: ApiResponse<NotificationItem[]>
```



#### POST `/notifications/{notificationId}/read`

- **사용 위치:** 알림 클릭 또는 개별 읽음 처리.
- **하는 일:** 특정 알림을 읽음 상태로 바꾼다.
- **프론트 액션:** 알림 클릭 후 배지와 읽음 스타일을 갱신한다.


```yaml
responses:
    204: {}
```



#### POST `/notifications/read-all`

- **사용 위치:** 알림 페이지의 전체 읽음 버튼.
- **하는 일:** 내 모든 알림을 읽음 처리한다.
- **프론트 액션:** 전체 읽음 버튼 클릭 시 unread 배지를 0으로 만든다.


```yaml
responses:
    204: {}
```



---

### 3.10 Admin Posts / Notices

#### GET `/admin-posts`

- **사용 위치:** 공지/큐레이션/리포트 목록 화면.
- **하는 일:** 운영자가 작성한 공지성 게시글 목록을 가져온다.
- **프론트 액션:** 공지 카드 리스트와 페이지네이션에 사용한다.


```yaml
parameters:
    query:
        type?: curation | notice | report
        page?: number
        size?: number
responses:
    200:
        application/json: ApiResponse<AdminPost[]>
```



#### GET `/admin-posts/{postId}`

- **사용 위치:** 공지/큐레이션/리포트 상세 화면.
- **하는 일:** 운영 게시글 하나의 상세 본문과 관련 피드를 가져온다.
- **프론트 액션:** 상세 본문과 연관 피드 추천 섹션을 렌더링한다.


```yaml
responses:
    200:
        application/json: ApiResponse<AdminPost>
```



---

### 3.11 Simulation / Locality

#### GET `/sim/runs/{runId}/manifest`

- **사용 위치:** 시뮬레이션 맵 초기 로딩.
- **하는 일:** run의 전체 메타, 에이전트, 장소, tick 설정을 가져온다.
- **프론트 액션:** 재생바 길이, 에이전트 초기 상태, 장소 맵을 구성한다.


```yaml
responses:
    200:
        application/json: SimManifest
```



#### GET `/sim/runs/{runId}/movements`

- **사용 위치:** 시뮬레이션 재생 중 청크 prefetch.
- **하는 일:** 특정 tick 구간의 에이전트 이동 정보를 가져온다.
- **프론트 액션:** 지도 위 페르소나 이동 애니메이션 계산에 사용한다.


```yaml
parameters:
    query:
        fromTick: number
        toTick: number
responses:
    200:
        application/json: MovementChunk
```



#### GET `/sim/runs/{runId}/lifecycle`

- **사용 위치:** 시뮬레이션 재생 중 이벤트 타임라인.
- **하는 일:** 특정 tick 구간의 스팟 생성/매칭/완료 이벤트를 가져온다.
- **프론트 액션:** 하이라이트, 마커 상태 변화, 이벤트 피드에 사용한다.


```yaml
parameters:
    query:
        fromTick: number
        toTick: number
responses:
    200:
        application/json: LifecycleChunk
```



#### GET `/sim/spots/{spotId}`

- **사용 위치:** 시뮬레이션 스팟 상세/AI 피드 상세.
- **하는 일:** 합성 스팟의 장소 앵커, 계획, 가격, 준비물 상세를 가져온다.
- **프론트 액션:** AI 스팟 카드 클릭 시 상세 패널을 렌더링한다.


```yaml
responses:
    200:
        application/json: ApiResponse<SimulationSpotDetail | null>
```



#### GET `/locality/regions`

- **사용 위치:** 지도 줌아웃 지역 특성 모드.
- **하는 일:** 행정동별 POI 밀도와 적합도 점수를 가져온다.
- **프론트 액션:** 지역 히트맵/폴리곤 색상/카테고리 레이어를 그린다.


```yaml
parameters:
    query:
        targetCity: string # current: suwon
responses:
    200:
        application/json: LocalityFeatureSet
```



---

## 4. 백엔드 구현용 상세 규칙

이 섹션은 백엔드 팀원이 별도 설명 없이 구현할 수 있도록 enum, validation, 상태 전이, 권한 규칙을 한곳에 모은다. 위의 API 명세와 충돌하면 **이 섹션을 우선 기준**으로 삼는다.

### 4.1 Enum 사전

#### Auth / User

| enum                          | values                                                  | 설명                          |
| ----------------------------- | ------------------------------------------------------- | ----------------------------- |
| `OAuthProvider`               | `kakao`, `google`                                       | 소셜 로그인 제공자            |
| `UserPersonaRole`             | `SUPPORTER`, `PARTNER`                                  | 사용자가 주로 수행하려는 역할 |
| `ProfileType`                 | `SUPPORTER`, `PARTNER`                                  | 공개 프로필 타입              |
| `SupporterRegistrationStatus` | `NOT_SUBMITTED`, `PENDING`, `VERIFIED`, `REJECTED`      | 서포터 검증 상태              |
| `PersonaArchetype`            | `explorer`, `helper`, `creator`, `connector`, `learner` | 온보딩 성향 타입              |

#### Spot / Feed

| enum                    | values                                                                                                   | 설명                                                          |
| ----------------------- | -------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------- |
| `SpotType`              | `OFFER`, `REQUEST`                                                                                       | OFFER=내가 무언가를 제공/개설, REQUEST=내가 도움/모임을 요청  |
| `SpotStatus`            | `OPEN`, `MATCHED`, `CLOSED`, `CANCELLED`                                                                 | 스팟 생명주기 상태                                            |
| `FeedItemType`          | `OFFER`, `REQUEST`, `RENT`                                                                               | 피드 카드 타입. `RENT`는 MVP에서 feed-only 타입으로 취급 가능 |
| `FeedItemStatus`        | `OPEN`, `MATCHED`, `CLOSED`                                                                              | 피드 노출용 상태. 취소된 스팟은 기본 목록에서 제외 권장       |
| `FeedCategory`          | `음악`, `요리`, `운동`, `공예`, `언어`, `기타`                                                           | 피드 필터 카테고리                                            |
| `SpotCategory`          | `요리`, `운동`, `음악`, `공예`, `코딩`, `등산`, `요가`, `미술`, `볼더링`                                 | 지도/시뮬레이션/스팟 도메인 카테고리                          |
| `FeedAuthorRole`        | `SUPPORTER`, `PARTNER`                                                                                   | 피드 작성자 역할                                              |
| `FeedApplicationStatus` | `APPLIED`, `ACCEPTED`, `REJECTED`, `CANCELLED`                                                           | 피드 신청 상태                                                |
| `FeedApplicationRole`   | `SUPPORTER`, `PARTNER`                                                                                   | 신청자가 어떤 역할로 신청했는지                               |
| `TimelineEventKind`     | `CREATED`, `MATCHED`, `COMPLETED`, `CANCELLED`, `COMMENT`, `SETTLEMENT_REQUESTED`, `SETTLEMENT_APPROVED` | 스팟 타임라인 이벤트 타입                                     |

#### Collaboration

| enum                     | values                  | 설명                    |
| ------------------------ | ----------------------- | ----------------------- |
| `SpotParticipant.role`   | `AUTHOR`, `PARTICIPANT` | 스팟 작성자/참여자 구분 |
| `WorkflowApprovalStatus` | `PENDING`, `APPROVED`   | 정산/최종 승인 상태     |
| `SpotReview.rating`      | `1`, `2`, `3`, `4`, `5` | 별점. 정수만 허용       |

#### Chat

| enum                      | values                                                                                   | 설명                                 |
| ------------------------- | ---------------------------------------------------------------------------------------- | ------------------------------------ |
| `ChatRoomCategory`        | `personal`, `spot`                                                                       | 1:1 채팅 / 스팟 팀 채팅              |
| `MainChatTopTab`          | `personal`, `team`                                                                       | 프론트 상단 탭                       |
| `PersonalCounterpartRole` | `SUPPORTER`, `PARTNER`                                                                   | 1:1 채팅 상대 역할                   |
| `MainChatPersonalFilter`  | `all`, `unread`, `SUPPORTER`, `PARTNER`                                                  | 개인 채팅 필터                       |
| `MainChatTeamFilter`      | `all`, `vote`, `schedule`, `file`                                                        | 팀 채팅 필터                         |
| `ChatActionKind`          | `vote`, `schedule`, `file`, `reverse-offer`                                              | 채팅에서 독립 액션으로 노출되는 항목 |
| `ChatMessage.kind`        | `system`, `message`, `vote`, `schedule`, `file`, `shortcut`, `reverse-offer`, `proposal` | 메시지 payload discriminator         |
| `ProposalStatus`          | `PENDING`, `ACCEPTED`, `NEGOTIATING`, `DECLINED`                                         | 채팅 제안 카드 상태                  |
| `ChatSSEEventType`        | `message`, `read`, `typing`                                                              | SSE 이벤트 타입                      |
| `ChatReverseOfferStatus`  | `PARTNER_REVIEW`, `ADMIN_APPROVAL_PENDING`                                               | 역제안 진행 상태                     |

#### Pay / Notification / Admin

| enum                      | values                                                | 설명                   |
| ------------------------- | ----------------------------------------------------- | ---------------------- |
| `PointTransaction.type`   | `CHARGE`, `USE`, `REFUND`, `WITHDRAW`                 | 포인트 거래 타입       |
| `PointWithdrawal.status`  | `PENDING`, `COMPLETED`, `REJECTED`                    | 출금 요청 상태         |
| `AdminPostType`           | `curation`, `notice`, `report`                        | 운영 게시글 타입       |
| `Notification.targetType` | `spot`, `feed`, `chat`, `pay`, `system`, `admin-post` | 알림 클릭 시 이동 대상 |

#### Simulation / Locality

| enum                    | values                                                                                        | 설명                       |
| ----------------------- | --------------------------------------------------------------------------------------------- | -------------------------- |
| `SpotProvenance`        | `virtual`, `real`, `mixed`                                                                    | 실제/가상/혼합 데이터 출처 |
| `SpotTeachMode`         | `1:1`, `small_group`, `workshop`                                                              | 진행 방식                  |
| `SpotVenueType`         | `cafe`, `home`, `studio`, `park`, `gym`                                                       | 장소 타입                  |
| `PlaceRole`             | `meetup`, `main`, `secondary`, `wrapup`                                                       | POI가 플랜에서 맡는 역할   |
| `AddOnMechanism`        | `fixed`, `funding`, `realcost`                                                                | 추가 비용 산정 방식        |
| `AttractivenessVerdict` | `too_cheap`, `competitive`, `slightly_high`, `too_high`                                       | 가격 매력도 판정           |
| `SimAgentRole`          | `protagonist`, `background`                                                                   | 시뮬레이션 에이전트 역할   |
| `MovementReason`        | `create_spot`, `join_spot`, `go_home`, `wander`                                               | 이동 사유                  |
| `LifecycleEventType`    | `SPOT_CREATED`, `SPOT_MATCHED`, `SPOT_CONFIRMED`, `SPOT_STARTED`, `SPOT_COMPLETED`, `NO_SHOW` | 시뮬레이션 생명주기 이벤트 |
| `LocalityCategory`      | `food`, `cafe`, `activity`, `park`, `culture`, `nightlife`, `lesson`                          | 지역 특성 카테고리         |

### 4.2 요청/응답 validation 규칙

#### 공통

- `id`는 string으로 내려준다. UUID 권장이나 프론트는 string이면 동작한다.
- `createdAt`, `updatedAt`, `joinedAt`, `uploadedAt`, `requestedAt` 등은 ISO 8601 timestamp로 내려준다.
- 금액/포인트 필드는 모두 정수 KRW/point 단위로 내려준다. 음수 금지. 단, 거래 내역의 `amount`는 타입에 따라 양수로 두고 `type`으로 방향을 구분한다.
- 빈 문자열은 저장하지 않는다. optional string은 값이 없으면 `undefined`/필드 누락 또는 `null` 중 하나로 통일한다. 프론트 타입은 주로 optional을 기대한다.
- 목록 API는 빈 결과일 때 `data: []`를 반환한다. `null` 반환 금지.
- 단건 조회에서 대상이 없으면 `404 ErrorResponse`를 반환한다. 단, 명세에서 명시한 `SimulationSpotDetail | null`, `SpotSchedule | null`, `SpotChecklist | null`, `LinkedBankAccount | null`은 `200` + `data: null` 허용.

#### Auth

| field          | rule                                                             |
| -------------- | ---------------------------------------------------------------- |
| `email`        | 필수, email format, trim 후 저장/비교                            |
| `password`     | 필수, 최소 8자 권장                                              |
| `next`         | 내부 경로만 허용. `http://`, `https://`, `//` 외부 redirect 금지 |
| `accessToken`  | API 인증용. 만료 시간 짧게 권장                                  |
| `refreshToken` | 재발급용. 탈취 위험 고려해 httpOnly cookie 권장                  |

#### Profile / Onboarding

| field           | rule                            |
| --------------- | ------------------------------- |
| `nickname`      | 1~20자 권장, trim 필수          |
| `phone`         | optional, 숫자/하이픈 허용      |
| `avatarUrl`     | optional, URL string            |
| `interests`     | 1개 이상 권장, 중복 제거        |
| `mediaUrls`     | URL string 배열, 최대 10개 권장 |
| `career`, `bio` | 0~2000자 권장                   |

#### Feed / Spot

| field                                              | rule                                                               |
| -------------------------------------------------- | ------------------------------------------------------------------ |
| `title`                                            | 필수, 1~80자 권장                                                  |
| `description`                                      | 필수 또는 optional 도메인별 결정, 최대 5000자 권장                 |
| `pointCost`, `price`, `deposit`                    | 0 이상 정수                                                        |
| `deadline`                                         | `YYYY-MM-DD`, 과거 날짜는 생성 시 거부 권장                        |
| `coord.lat`                                        | -90~90                                                             |
| `coord.lng`                                        | -180~180                                                           |
| `category`                                         | 위 enum 중 하나 권장. 기존 데이터 호환을 위해 string fallback 가능 |
| `maxParticipants`                                  | 1 이상 정수                                                        |
| `progressPercent`                                  | 0~100                                                              |
| `views`, `likes`, `partnerCount`, `applicantCount` | 0 이상 정수                                                        |

#### Schedule / Vote / Checklist

| field                       | rule                                  |
| --------------------------- | ------------------------------------- |
| `ScheduleSlot.date`         | `YYYY-MM-DD`                          |
| `ScheduleSlot.hour`         | 0~23 정수                             |
| `availableUserIds`          | 참여자 user id만 허용 권장, 중복 제거 |
| `VoteOption.label`          | 1~60자                                |
| `CreateVotePayload.options` | 최소 2개, 최대 10개 권장              |
| `multiSelect`               | 기본값 false                          |
| `ChecklistItem.text`        | 1~120자                               |
| `ChecklistItem.completed`   | boolean                               |

#### Chat

| field              | rule                                                                                                   |
| ------------------ | ------------------------------------------------------------------------------------------------------ |
| `content`          | 1~2000자, trim 후 빈 문자열 거부                                                                       |
| `roomId`           | 사용자가 멤버인 방만 조회/전송 가능                                                                    |
| `cursor`           | 서버가 발급한 opaque string 권장                                                                       |
| `messages`         | `createdAt` 오름차순 또는 내림차순을 API별로 고정. 이 문서는 채팅방 화면에서 오래된→최신 오름차순 권장 |
| `ChatMessage.kind` | kind별 payload 필수. 예: `kind='vote'`면 `vote` 필수                                                   |

#### Pay

| field             | rule                                                          |
| ----------------- | ------------------------------------------------------------- |
| `amount`          | 1 이상 정수                                                   |
| `charge.amount`   | 최소 충전 금액 정책 필요. 예: 1000 이상                       |
| `withdraw.amount` | 잔액 이하, 최소 출금 금액 정책 필요                           |
| `accountNumber`   | 저장 시 암호화/마스킹. 응답에는 가능하면 마스킹된 번호만 반환 |

### 4.3 상태 전이 규칙

#### SpotStatus


```txt
OPEN -> MATCHED -> CLOSED
OPEN -> CANCELLED
MATCHED -> CANCELLED
```



- `OPEN`: 작성 직후, 참여/신청 가능.
- `MATCHED`: 참여자가 확정되어 진행 중.
- `CLOSED`: 활동 완료, 후기/정산 가능.
- `CANCELLED`: 취소됨. 기본 목록/지도에서는 숨김 권장.
- 금지: `CLOSED -> OPEN`, `CANCELLED -> OPEN`, `CANCELLED -> MATCHED`.

#### FeedApplicationStatus


```txt
APPLIED -> ACCEPTED
APPLIED -> REJECTED
APPLIED -> CANCELLED
ACCEPTED -> CANCELLED  # 운영 정책상 허용할지 결정 필요
```



- `APPLIED`: 사용자가 신청 완료.
- `ACCEPTED`: 작성자/운영자가 수락.
- `REJECTED`: 거절.
- `CANCELLED`: 신청자가 취소.
- `REJECTED`, `CANCELLED` 이후 재신청 허용 여부는 백엔드 정책으로 결정하되, 허용 시 새 application row 생성을 권장.

#### SupporterRegistrationStatus


```txt
NOT_SUBMITTED -> PENDING -> VERIFIED
NOT_SUBMITTED -> PENDING -> REJECTED -> PENDING
VERIFIED -> PENDING  # 프로필 핵심 정보 재수정 시 재심사 정책이면 허용
```



- 사용자는 `verificationStatus`를 직접 확정할 수 없다.
- 사용자 제출/수정 시 보통 `PENDING`으로 전환한다.
- `VERIFIED`, `REJECTED` 처리는 관리자 권한 API에서 수행한다.

#### PointWithdrawal.status


```txt
PENDING -> COMPLETED
PENDING -> REJECTED
```



- 출금 요청 생성 시 `PENDING`.
- 완료/거절은 운영자 또는 결제 시스템 callback에서 처리한다.

### 4.4 권한 규칙

| 리소스                                      | 조회                                     | 수정/삭제                                  |
| ------------------------------------------- | ---------------------------------------- | ------------------------------------------ |
| `/me/**`                                    | 로그인 사용자 본인                       | 본인만 가능                                |
| `SupporterProfile`                          | 공개 프로필은 로그인 사용자 조회 가능    | 본인 또는 관리자                           |
| `Spot`                                      | 공개 목록/상세는 로그인 사용자 조회 가능 | 작성자 또는 관리자. 참여자 액션은 참여자만 |
| `SpotSchedule`, `SpotVote`, `SpotChecklist` | 스팟 참여자만 조회 권장                  | 스팟 참여자만 수정 가능                    |
| `SharedFile`                                | 스팟 참여자만 조회                       | 업로더/작성자/관리자 삭제 가능             |
| `ProgressNote`                              | 스팟 참여자만 조회                       | 스팟 참여자 작성 가능                      |
| `SpotReview`                                | 관련 사용자/공개 정책에 따름             | 활동 완료 후 참여자만 작성                 |
| `ChatRoom`                                  | 방 멤버만 조회                           | 방 멤버만 메시지 전송 가능                 |
| `Pay`                                       | 본인만 조회                              | 본인만 요청 가능                           |
| `AdminPost`                                 | 공개 조회                                | 관리자만 작성/수정/삭제                    |
| `Simulation`, `Locality`                    | read-only 공개/로그인 조회               | MVP에서는 수정 API 없음                    |

### 4.5 API별 백엔드 구현 메모

| API                                        | 백엔드에서 해야 할 핵심 처리                                                           |
| ------------------------------------------ | -------------------------------------------------------------------------------------- |
| `POST /auth/login`                         | credential 검증, token 발급, cookie 설정 옵션 처리                                     |
| `POST /auth/logout`                        | refresh token/session 무효화, auth cookie 제거                                         |
| `POST /auth/refresh`                       | refresh token 검증, access token 재발급                                                |
| `GET /me`                                  | token subject 기준 user 조회, point balance 포함                                       |
| `PATCH /me`                                | 본인 프로필 부분 수정, unique email 충돌 검사                                          |
| `PUT /me/persona`                          | 기존 persona upsert, interests 중복 제거                                               |
| `GET /feeds`                               | feed read model 조회, 내 신청/북마크 상태 조인                                         |
| `POST /feeds/{feedId}/applications`        | 중복 신청 방지, 포인트/보증금 정책 검증, application 생성                              |
| `GET /feeds/{feedId}/applications`         | 작성자 권한 확인, 해당 피드 전체 신청 최신순 반환                                      |
| `DELETE /feeds/{feedId}/applications/me`   | 내 active application 조회 후 CANCELLED 전환                                           |
| `POST /feeds/{feedId}/bookmark`            | `(user_id, feed_id)` unique upsert                                                     |
| `GET /spots/map`                           | bounds 필터, 경량 필드만 반환, 취소/비공개 제외                                        |
| `POST /spots`                              | spot 생성, author participant 생성, timeline CREATED 생성, feed projection 생성/갱신   |
| `POST /spots/{spotId}/match`               | 권한 확인, 상태 전이 검증, timeline MATCHED 생성                                       |
| `POST /spots/{spotId}/cancel`              | 권한 확인, 상태 전이 검증, 환불/패널티 정책 hook                                       |
| `POST /spots/{spotId}/complete`            | 권한 확인, 상태 CLOSED 전환, 리뷰/정산 가능 상태 오픈                                  |
| `PUT /spots/{spotId}/schedule`             | 참여자 권한 확인, slot validation, confirmedSlot이 proposedSlots 중 하나인지 검증 권장 |
| `POST /spots/{spotId}/votes`               | 참여자 권한 확인, option 최소 2개 검증                                                 |
| `POST /spots/{spotId}/votes/{voteId}/cast` | 참여자 권한 확인, 중복 투표 처리, multiSelect 정책 적용                                |
| `PUT /spots/{spotId}/checklist`            | 참여자 권한 확인, 전체 replace 또는 patch 정책 고정                                    |
| `POST /spots/{spotId}/files`               | 파일 저장/presigned 처리, SharedFile row 생성                                          |
| `POST /spots/{spotId}/notes`               | 참여자 권한 확인, note 생성, timeline COMMENT 생성 가능                                |
| `POST /spots/{spotId}/reviews`             | CLOSED 상태인지 확인, 중복 리뷰 방지                                                   |
| `POST /spots/{spotId}/settlement`          | 작성자/권한 확인, line item 합계 검증, 승인 대기 상태 생성                             |
| `POST /spots/{spotId}/settlement/approve`  | 승인 권한 확인, 포인트 정산 transaction 생성                                           |
| `GET /chat/rooms`                          | 내가 속한 방만 조회, latest message/unread count 계산                                  |
| `POST /chat/rooms`                         | personal은 `(userA,userB)` 중복 방지, spot은 `(spotId)` 기준 팀방 반환                 |
| `POST /chat/rooms/{roomId}/messages`       | 멤버 권한 확인, message 저장, SSE broadcast                                            |
| `GET /chat/rooms/{roomId}/stream`          | 멤버 권한 확인, SSE 연결 유지, heartbeat 전송 권장                                     |
| `GET /pay/history`                         | 내 point transaction만 조회, 최신순 권장                                               |
| `POST /pay/charge`                         | 결제 승인 전 mock이면 즉시 balance 증가, 실제 PG면 payment intent 생성                 |
| `POST /pay/withdrawals`                    | 잔액 확인, 출금 요청 생성, balance hold 또는 즉시 차감 정책 결정                       |
| `GET /me/involved-feeds`                   | 작성 피드 + 신청 피드 합산, softDelete 피드 포함, 중복 제거 후 반환                   |
| `GET /notifications`                       | 내 알림만 조회, unreadOnly 필터 적용                                                   |
| `GET /sim/**`                              | read-only fixture/published dataset 반환, snake_case 유지                              |
| `GET /locality/regions`                    | targetCity별 published locality dataset 반환                                           |

### 4.6 프론트가 기대하는 캐시 무효화 단위

백엔드는 몰라도 되지만, 응답 일관성을 위해 아래 변경이 발생하면 관련 read model을 같이 갱신해야 한다.

| 변경 API                                                         | 같이 갱신되어야 하는 데이터                                                   |
| ---------------------------------------------------------------- | ----------------------------------------------------------------------------- |
| `POST /spots`                                                    | `/spots`, `/spots/map`, `/feeds`, `/me/participations`                        |
| `POST /spots/{id}/match/cancel/complete`                         | `/spots`, `/spots/{id}`, `/feeds`, `/spots/map`, `/chat/rooms`                |
| `POST /feeds/{id}/applications`                                  | `/feeds`, `/feeds/{id}`, `/me/participations`, `/pay/balance` if deposit used |
| `POST/DELETE /feeds/{id}/bookmark`                               | `/feeds`, `/feeds/{id}`, `/me/favorites`                                      |
| `PUT /spots/{id}/schedule`                                       | `/spots/{id}/schedule`, `/chat/action-items`                                  |
| `POST /spots/{id}/votes`, `POST /spots/{id}/votes/{voteId}/cast` | `/spots/{id}/votes`, `/chat/action-items`                                     |
| `POST /spots/{id}/files`, `DELETE /spots/{id}/files/{fileId}`    | `/spots/{id}/files`, `/chat/action-items`                                     |
| `POST /chat/rooms/{id}/messages`                                 | `/chat/rooms`, `/chat/rooms/{id}/messages`, SSE subscribers                   |
| `POST /pay/charge`, `POST /pay/withdrawals`                      | `/pay/balance`, `/pay/history`, `/pay/withdrawals`                            |
| `PUT /me/persona`                                                | `/me/persona`, personalized `/feeds`                                          |

### 4.7 누락되면 프론트가 막히는 최소 필드

MVP에서 백엔드가 모든 필드를 완벽히 채우기 어렵다면, 최소한 아래 필드는 반드시 내려줘야 한다.

#### FeedItem minimum


```ts
type FeedItemMinimum = {
    id: string;
    title: string;
    location: string;
    authorNickname: string;
    price: number;
    type: 'OFFER' | 'REQUEST' | 'RENT';
    status: 'OPEN' | 'MATCHED' | 'CLOSED';
    views: number;
    likes: number;
};
```



#### Spot minimum


```ts
type SpotMinimum = {
    id: string;
    type: 'OFFER' | 'REQUEST';
    status: 'OPEN' | 'MATCHED' | 'CLOSED' | 'CANCELLED';
    title: string;
    description: string;
    pointCost: number;
    authorId: string;
    authorNickname: string;
    createdAt: string;
    updatedAt: string;
};
```



#### SpotMapItem minimum


```ts
type SpotMapItemMinimum = {
    id: string;
    type: 'OFFER' | 'REQUEST';
    status: 'OPEN' | 'MATCHED' | 'CLOSED' | 'CANCELLED';
    title: string;
    coord: { lat: number; lng: number };
    category: string;
};
```



#### ChatRoom minimum


```ts
type ChatRoomMinimum = {
    id: string;
    category: 'personal' | 'spot';
    currentUserId: string;
    currentUserName: string;
    title: string;
    subtitle: string;
    description: string;
    metaLabel: string;
    updatedAt: string;
    messages: ChatMessage[];
};
```



#### UserProfile minimum


```ts
type UserProfileMinimum = {
    id: string;
    nickname: string;
    email: string;
    pointBalance: number;
    joinedAt: string;
};
```



---

## 5. 프론트 구현 우선순위

### Phase 1 — MVP 연결 필수

1. Auth: login/logout/refresh/oauth
2. Feed: list/detail/apply/cancel/bookmark
3. Spot: list/map/search/detail/create + schedule/vote/checklist/files/notes/reviews
4. Chat: room list/detail/messages/SSE
5. Me: profile/persona/participations/favorites/recent views
6. Pay: balance/history/charge/bank/withdraw

### Phase 2 — 운영/고도화

1. Notification 목록/읽음 처리
2. Admin posts/notice/curation
3. Supporter verification workflow
4. Settlement approval workflow
5. Simulation/locality real backend 연결

---

## 6. 백엔드와 확정해야 할 결정사항

- `page`를 0-base로 할지 1-base로 할지: 이 문서는 1-base 권장
- token 저장 방식: Authorization header + refresh cookie 조합 권장
- Feed와 Spot의 관계: `feed_items` read model vs `spots` projection 중 선택
- `RENT` 타입을 Spot에도 포함할지, Feed-only 타입으로 둘지 결정 필요
- 파일 업로드: presigned URL 방식 vs multipart 직접 업로드 방식 결정 필요
- 실시간 채팅: SSE 우선, 추후 WebSocket 전환 가능
- 시뮬레이션/지역 데이터는 MVP에서는 read-only API로 시작 권장

---

## 7. 최소 OpenAPI 골격


```yaml
openapi: 3.0.3
info:
    title: SPOT API
    version: 0.1.0
servers:
    - url: /api/v1
security:
    - bearerAuth: []
components:
    securitySchemes:
        bearerAuth:
            type: http
            scheme: bearer
            bearerFormat: JWT
    schemas:
        ErrorResponse:
            type: object
            required: [message]
            properties:
                message: { type: string }
                code: { type: string }
                details: { type: object }
paths:
    /auth/login:
        post:
            summary: 이메일 로그인
    /me:
        get:
            summary: 내 프로필 조회
        patch:
            summary: 내 프로필 수정
    /feeds:
        get:
            summary: 피드 목록
    /spots:
        get:
            summary: 스팟 목록
        post:
            summary: 스팟 작성
    /chat/rooms:
        get:
            summary: 채팅방 목록
        post:
            summary: 채팅방 생성 또는 반환
    /pay/balance:
        get:
            summary: 포인트 잔액
```