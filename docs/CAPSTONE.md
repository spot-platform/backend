# Project Capstone Harness

이 파일은 프로젝트의 일관성을 유지하고, 협업 시 발생할 수 있는 충돌을 방지하기 위한 핵심 지침서입니다. 모든 작업자는 작업을 시작하기 전 이 내용을 반드시 숙지해야 합니다.

---

## 📢 1. AI 작업자 행동 지침 (General Rules)
1. **사용자 확인**: 작업을 시작하기 전, 반드시 현재 사용자의 이름을 확인하십시오.
2. **명세 준수**: 모든 필드명, 타입, 엔드포인트는 `docs/FRONTEND.md`와 100% 일치해야 합니다. 임의 변경을 금지합니다.
3. **히스토리 파악**: `docs/CAPSTONE.md`의 Todo 및 지침을 읽어 중복 작업을 방지하고 문맥을 파악하십시오.

---

## 🎨 2. 코드 컨벤션 및 구조 (Coding Standard)
1. **네이버 핵데이 컨벤션 준수**:
   - 들여쓰기: **Tab (1칸 = 4공백)** 사용.
   - 줄바꿈: **LF (Unix 방식)** 적용.
   - 임포트: 와일드카드(`.*`) 금지, 구체적 명시.
     - 그룹 순서: `static` → `java.` → `javax.` / `jakarta.` → `org.` / `net.` → `com.` (외부) → 그 외 (third-party `io.`, `lombok.` 등과 `backend.*` 가 동일 catch-all 그룹, **알파벳 순**)
     - 즉 `backend.*` 가 `io.*` / `lombok.*` 보다 알파벳순으로 앞이면 위에 위치한다. 이는 `config/checkstyle/checkstyle.xml` 의 ImportOrder 룰(`ordered=true`, catch-all regex)이 실제로 강제하는 규칙이며, 빈 줄로 분리되더라도 같은 그룹으로 취급된다.
2. **기능별 패키징 (Package by Feature)**:
   - `backend.[feature].entity`, `backend.[feature].service`, `backend.[feature].controller`, `backend.[feature].dto` 구조 유지.
3. **멀티 모듈 의존성**:
   - `capstone-domain`은 `capstone-api`를 참조할 수 없음. 공용 DTO는 domain 모듈에 위치 가능.

---

## 🏗️ 3. 설계 제약 조건 (Technical Principles)
1. **논리적 외래키 (Logical FK)**: DB 수준의 물리적 FK 제약은 지양한다. 무결성은 애플리케이션(Service) 레이어에서 검증하며, 필요 시 인덱스만 생성한다.
2. **풍부한 도메인 모델 (Rich Domain)**: 엔티티에 `@Setter` 사용을 금지한다. 비즈니스 로직(상태 변경 등)은 엔티티 내부 메서드(예: `.match()`)로 구현한다.
3. **소프트 딜리트 (Soft Delete)**: `is_deleted` 등의 플래그를 사용하여 물리적 삭제로 인한 참조 무결성 오류를 원천 차단한다.
4. **불변성 및 생성자 주입**: `@RequiredArgsConstructor`를 통한 생성자 주입을 필수로 하며, 가급적 객체의 불변성을 유지한다.
5. **검증의 계층화**: DTO는 형식 검증(`@Valid`), 서비스/도메인은 비즈니스 규칙 검증을 담당한다.

---

## ✅ 4. 작업 검증 절차 (Verification)
1. **빌드 확인**: `./gradlew compileJava` 실행 시 에러가 없어야 함.
2. **컨벤션 체크**: `./gradlew checkstyleMain` 실행 시 위반 사항이 0건이어야 함.
3. **테스트 보장**: 핵심 로직 변경 시 단위 테스트를 작성하고 `./gradlew test`를 통과해야 함.

---

## 🔒 5. 환경 설정 및 보안 (Configuration & Security)
1. **비밀 정보 관리**: `application-secret.yml`, `.env` 등 민감한 정보가 포함된 파일은 **절대 Git에 커밋하지 않는다.**
   - 해당 파일들은 반드시 `.gitignore`에 등록되어 있어야 하며, 서버 배포 시에는 수동으로 업로드하거나 환경 변수를 통해 관리한다.
2. **환경별 설정 분리**: 로컬 개발 환경과 실제 운영 서버 환경의 설정을 분리하여 관리하며, DB 비밀번호나 API 키와 같은 보안 정보는 별도의 Secret 파일로 격리한다.

---

## 🤝 6. PR 워크플로우 (Pull Request Workflow)
모든 PR 생성 시 아래를 **자동으로** 적용한다. AI 작업자는 PR 만든 직후 즉시 수행.

### 작업자 ↔ GitHub 매핑

| 이름 | GitHub login |
|---|---|
| 황호찬 | `hoTan35` |
| 이성찬 | `ca5tlechan` |
| 김동현 | `ThonApple` |

### 룰
1. **CodeRabbit Full Review 요청**:
   - PR 생성 직후 `@coderabbitai full review` 코멘트를 단다.
   - 한도 초과 시 사용자에게 즉시 보고하고, 풀리는 시간을 함께 알린다.
2. **리뷰어 자동 지정 (본인 제외)**:
   - 모든 PR은 **본인을 제외한 두 명**을 reviewer로 자동 등록한다.
   - 둘 중 **한 명 이상의 approve**가 머지 조건.
3. **재리뷰 트리거**:
   - fix 커밋 push 후 `@coderabbitai review`(증분) 또는 `full review`(전체)를 트리거한다.

---

## 🌐 7. API 경로 규칙
- **Base URL**: `/api/v1` (모든 컨트롤러 `@RequestMapping` prefix 통일)
- **상세 계약**: `docs/FRONTEND.md` 기준. 필드명·타입·HTTP 메서드 모두 일치시킬 것.

---

## 📋 8. PR #32 후속 작업 (Issue 트래킹)

PR #32 "FRONTEND.md 컴플라이언스" 머지 후 분리된 후속 항목. 도메인 오너 기준 분담.

| # | 항목 | 담당 | 우선순위 | 의존성 |
|---|---|---|---|---|
| [#33](https://github.com/spot-platform/backend/issues/33) | F1 인증 통합 (`dummy-user-id` 제거) | 🧔 성찬 | 🔴 High | — (F4·F3 일부의 블로커) |
| [#34](https://github.com/spot-platform/backend/issues/34) | F2 SpotSchedule 구조 재설계 | 🧑 동현 | 🔴 High | 독립 |
| [#35](https://github.com/spot-platform/backend/issues/35) | F3-a 피드 북마크 실구현 | 🧔 호찬 | 🟡 Med | F1 일부 |
| [#36](https://github.com/spot-platform/backend/issues/36) | F3-b 전체 알림 읽음 처리 | 🧔 성찬 | 🟡 Med | F1 |
| [#37](https://github.com/spot-platform/backend/issues/37) | F3-c 스팟 지도 마커 / 검색 | 🧑 동현 | 🟡 Med | 독립 |
| [#38](https://github.com/spot-platform/backend/issues/38) | F4 FeedItemResponse 신규 필드 채우기 | 🧔 호찬 | 🟡 Med | **F1** |
| [#39](https://github.com/spot-platform/backend/issues/39) | F5 SpotChecklist 담당자 컬럼 | 🧑 동현 | 🟡 Med | 독립 |
| [#40](https://github.com/spot-platform/backend/issues/40) | F6-a Spot/Chat ID Long→String | 🧑 동현 | 🔵 Low | 가장 마지막 |
| [#41](https://github.com/spot-platform/backend/issues/41) | F6-b Feed ID Long→String | 🧔 호찬 | 🔵 Low | F3-a 후 |
| [#42](https://github.com/spot-platform/backend/issues/42) | F7 OpenAPI 자동 검증 CI | 🧑 동현 | 🔵 Low | 독립 |
| [#43](https://github.com/spot-platform/backend/issues/43) | F8-a Checkstyle WARN (ChatService) | 🧑 동현 | 🔵 Low | 독립 |
| [#44](https://github.com/spot-platform/backend/issues/44) | F8-b Checkstyle WARN (OAuth2) | 🧔 성찬 | 🔵 Low | 독립 |

권장 순서: **F1 (성찬, 가장 먼저)** → F2 / F3 / F5 / F7 / F8 병렬 → F4 (F1 머지 후) → **F6 마지막** (다른 거 다 머지된 base 위에서)

---

## 🔄 9. Post→Feed 통합 작업 (hoTan35 담당)

### 배경
Post(write 모델)와 FeedItem(read/display 모델)이 중복. FeedItem이 AI 피드·시뮬레이션을 수용하면서
Post보다 더 풍부한 구조가 됨. Post를 제거하고 FeedItem으로 단일화. Offer/Request 생성 API도
`POST /posts/*` → `POST /feeds/*`로 이동.

### 확정된 결정사항

| 항목 | 결정 |
|---|---|
| 다중 카테고리·사진 저장 | **JSON 컬럼** (`categoriesJson`, `photoUrlsJson`) — ElementCollection 아님 |
| FeedItem.spotId | **AI 피드 전용 유지** — 일반 피드 Spot 전환 시엔 기록 안 함 (전환 후 softDelete) |
| UUID→Long 변경 범위 | **FeedItem, Spot, Notification만** — User, Chat은 UUID 유지 |

### 작업 목록 (번호 순, 하나씩 진행)

#### ✅ T1 — FeedItem 필드 업그레이드
> Post에만 있던 필드를 FeedItem으로 추가하고, 단수→다중 필드 업그레이드.
> 완료 기준: `./gradlew compileJava` 통과 + FeedController에 Offer/Request 생성 API 노출

**엔티티 (`FeedItem.java`) 추가 필드:**
```
spotName             String  nullable   스팟 명칭
detailDescription    TEXT    nullable   상세 설명 (description과 별개)
supporterPhotoUrl    String  nullable   OFFER 전용
serviceStylePhotoUrl String  nullable   REQUEST 전용
categoriesJson       TEXT    nullable   List<FeedCategory> JSON — 기존 category 컬럼과 별개 유지
photoUrlsJson        TEXT    nullable   List<String> JSON — 기존 imageUrl은 AI 피드용 유지
```

**FeedController 추가 엔드포인트:**
- `POST /api/v1/feeds/offer` — FeedItemService.createOfferFeed()
- `POST /api/v1/feeds/request` — FeedItemService.createRequestFeed()
- 요청 DTO: `CreateOfferFeedRequest`, `CreateRequestFeedRequest` (post 패키지 DTO 참고)
- 응답 DTO: `FeedCreateResponse` (`id`, `type`, `title`, `redirectUrl` 포함)

**응답 DTO (`FeedItemResponse`, `FeedDetailResponse`) 추가 필드:**
```
spotName, detailDescription, supporterPhotoUrl, serviceStylePhotoUrl,
categories (List<String>), photoUrls (List<String>)
```

**⚠️ 주의**: `category`(단일) 컬럼은 삭제하지 않음 — AI 피드 이니셜라이저가 사용 중.
`categoriesJson`은 신규 사용자 생성 피드 전용.

---

#### ✅ T2 — Spot.fromPost() → fromFeedItem() 교체
> 완료 기준: `./gradlew compileJava` 통과, Spot에 Post import 없음

**변경 파일: `capstone-domain/.../spot/entity/Spot.java`**
- `fromPost(Post post, ...)` 정적 팩토리 메서드 제거
- `fromFeedItem(FeedItem feedItem)` 추가
  ```java
  public static Spot fromFeedItem(FeedItem feedItem) {
      return Spot.builder()
          .type(feedItem.getType())
          .status(FeedItemStatus.MATCHED)
          .matchedAt(LocalDateTime.now())
          .title(feedItem.getTitle())
          .description(feedItem.getDescription())
          .pointCost(feedItem.getPrice())
          .authorId(feedItem.getAuthorId())
          .authorNickname(feedItem.getAuthorNickname())
          .build();
  }
  ```
- `import backend.post.entity.Post` 제거

---

#### ✅ T3 — Spot 변환 로직 재구성 + Post 패키지 완전 삭제
> 완료 기준: `./gradlew compileJava` 통과, post 패키지 파일 0개, FeedItemService에 PostService·PostRepository 의존 없음

**단계:**
1. `FeedItemService.acceptApplication()` 수정
   - `postService.convertToSpot(feedItem.getPostId())` → `spotRepository.save(Spot.fromFeedItem(feedItem))`
   - `notificationService` 주입 추가, 알림 전송 로직 이동
2. `FeedItemService.deleteFeedItem()` 수정
   - Post softDelete 연동 로직 제거
3. `FeedItemService` 필드 제거
   - `PostRepository postRepository` 제거
   - `PostService postService` 제거
4. **삭제 대상 파일 (capstone-api, capstone-domain 전체)**
   - `backend.post.controller.PostController`
   - `backend.post.service.PostService`
   - `backend.post.entity.Post`
   - `backend.post.repository.PostRepository`
   - `backend.post.dto.CreateOfferPostRequest`
   - `backend.post.dto.CreateRequestPostRequest`
   - `backend.post.dto.PostResponse`
   - `backend.post.dto.PostCompletionResponse`
   - `capstone-api/.../post/service/PostServiceTest`
5. `FeedItemServiceTest` 정리
   - `PostService`, `PostRepository` mock 제거
   - `feedItemWithPost()` 헬퍼 제거
   - `acceptApplication — SpotConversion` 테스트를 PostService 없이 재작성 (SpotRepository.save() verify로 대체)

---

#### ✅ T4 — FeedItem.spotId 처리 (문서화만, 코드 변경 없음)
> 확정: spotId는 AI 피드 전용 시뮬레이션 참조 컬럼으로 유지.
> 일반 피드 Spot 전환 시엔 spotId 기록 안 함 (전환 후 feedItem softDelete).
> **코드 변경 없음.** Spot.java·FeedItem.java에 주석 추가만.

---

#### ✅ T5 — PostType → FeedType rename
> 완료 기준: `./gradlew compileJava` 통과, `PostType` 참조 0건

**변경 파일 목록 (14개):**
```
capstone-domain/.../global/enums/PostType.java          → FeedType.java (클래스명 변경)
capstone-domain/.../feed/entity/FeedItem.java           PostType → FeedType
capstone-domain/.../feed/dto/FeedListQuery.java         PostType → FeedType
capstone-domain/.../feed/repository/FeedItemRepositoryImpl.java  PostType → FeedType
capstone-domain/.../spot/entity/Spot.java               PostType → FeedType
capstone-domain/.../spot/repository/SpotRepository.java PostType → FeedType
capstone-api/.../feed/dto/FeedItemResponse.java         PostType → FeedType (4곳)
capstone-api/.../feed/dto/FeedDetailResponse.java       PostType → FeedType (4곳)
capstone-api/.../feed/initializer/AiFeedDataInitializer FeedType.valueOf(...)
capstone-api/.../spot/dto/CreateSpotRequest.java        PostType → FeedType
capstone-api/.../spot/dto/SpotMapItemResponse.java      PostType → FeedType
capstone-api/.../spot/dto/SpotResponse.java             PostType → FeedType
capstone-api/.../spot/service/SpotService.java          PostType → FeedType
capstone-api/.../feed/service/FeedItemService.java      PostType → FeedType
```
**DB 영향 없음** — DB에 저장되는 값은 enum 상수명(`OFFER`, `REQUEST`, `RENT`)이므로 클래스명 변경과 무관.

---

#### ✅ T6 — FeedCategory 통합 (PostSpotCategory → FeedCategory 흡수)
> 완료 기준: `./gradlew compileJava` 통과, PostSpotCategory 참조 0건, Spot.category가 FeedCategory 타입

**FeedCategory.java에 추가할 값 (PostSpotCategory에서 이관):**
```java
public enum FeedCategory {
    음악("음악"), 요리("요리"), 운동("운동"), 공예("공예"), 언어("언어"), 기타("기타"),
    // ↓ PostSpotCategory에서 이관
    음식_요리("음식·요리"), BBQ_조개("BBQ·조개"), 공동구매("공동구매"), 교육("교육");

    private final String label;
    FeedCategory(String label) { this.label = label; }
    public String getLabel() { return label; }
}
```
- `PostSpotCategory.java` 삭제
- `Spot.category: String` → `Spot.category: FeedCategory` (@Enumerated(EnumType.STRING))
- `SpotRepository.findMapItems()` — `:category` 파라미터 타입 `String` → `FeedCategory`
- `SpotService.getSpotMap()` — `parseEnum(PostType, ...)` 패턴 동일하게 FeedCategory도 적용
- `SpotMapItemResponse`에 `category: FeedCategory`로 타입 변경

**DB 주의**: Spot 테이블의 기존 `category` 컬럼 값이 현재 String이므로,
기존 데이터가 FeedCategory enum 값과 일치하면 자동 매핑. 불일치 데이터는 서버 기동 시 에러.
로컬 DB라면 테이블 초기화 후 재기동으로 해결.

---

#### ✅ T7 — FeedItem.postId 컬럼 제거
> 완료 기준: `./gradlew compileJava` 통과, FeedItem에 postId 필드 없음

**변경 파일:**
- `FeedItem.java` — `postId` 필드 제거
- `FeedItemServiceTest.java` — `feedItemWithPost()` 헬퍼 및 `SpotConversion` 테스트 정리 (T3에서 이미 처리됐으면 skip)

---

#### ✅ T8 — UUID → Long (순번) ID 변경
> 완료 기준: `./gradlew compileJava` 통과, 프론트에서 숫자 ID로 정상 호출

**변경 대상 엔티티 ID:**
```
FeedItem.id    : String (UUID) → Long (@GeneratedValue IDENTITY)
Spot.id        : String (UUID) → Long (@GeneratedValue IDENTITY)
Notification.id: String (UUID) → Long (@GeneratedValue IDENTITY)
```

**연쇄로 바꿔야 할 외래키 필드들:**
```
FeedApplication.feedItemId : String → Long
Bookmark.feedItemId        : String → Long
SpotParticipant.spotId     : String → Long
SpotVote.spotId            : String → Long   (creatorId는 userId라 String 유지)
SpotNote.spotId            : String → Long
SpotFile.spotId            : String → Long
SpotChecklist.spotId       : String → Long
SpotSchedule.spotId        : String → Long
FeedItem.spotId            : String → Long   (AI 피드용 시뮬레이션 참조)
ChatRoom.spotId(?)         : 확인 후 결정 — Chat ID는 변경 대상 아님
```

**Repository 타입 파라미터 변경:**
```
FeedItemRepository     : JpaRepository<FeedItem, String>    → <FeedItem, Long>
SpotRepository         : JpaRepository<Spot, String>        → <Spot, Long>
NotificationRepository : JpaRepository<Notification, String>→ <Notification, Long>
FeedApplicationRepository, BookmarkRepository 등 관련 메서드 파라미터도 확인
```

**Controller PathVariable:**
```java
// 변경 전
@GetMapping("/{feedId}")
public ... getFeedItem(@PathVariable String feedId)

// 변경 후
@GetMapping("/{feedId}")
public ... getFeedItem(@PathVariable Long feedId)
```

**ID 생성 전략 변경:**
```java
// 변경 전
@GeneratedValue(generator = "uuid2")
@GenericGenerator(name = "uuid2", strategy = "uuid2")
@Column(columnDefinition = "VARCHAR(36)")
private String id;

// 변경 후
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

**⚠️ T8은 범위가 가장 큼. T1~T7 완료 후 마지막에 진행.**

---

### 실행 순서 요약

```
T1 → T2 → T3 → T4(no-op) → T5 → T6 → T7 → T8
```

각 단계 완료 후 `./gradlew compileJava` + `./gradlew checkstyleMain` 필수 확인.
T3 이후 `./gradlew test`도 실행해서 테스트 통과 여부 확인.
