SPOT 백엔드 프로젝트 PR #32 (**refactor/frontend-md-compliance**)에 대한 리뷰 결과입니다.

`docs/FRONTEND.md` 명세(v1.5, v1.6)를 기준으로 코드를 대조한 결과, 다수의 명세 불일치 및 구현 누락이 발견되었습니다.

---

### 🔍 PR 리뷰 결과 리포트

#### 1. FRONTEND.md 섹션 1.5, 1.6 명세 불일치 (DTO)

*   **ID 타입 불일치 (String vs Long)**
    *   **파일:** `capstone-api/src/main/java/backend/spot/dto/` 내 `SpotVoteResponse.java`, `SpotVoteOptionResponse.java`, `SpotChecklistResponse.java`, `SpotFileResponse.java`, `SpotNoteResponse.java`
    *   **설명:** `FRONTEND.md`에서는 모든 ID를 `string`으로 정의하고 있으나, 위 DTO들에서는 `Long` 타입을 사용하고 있습니다. 프론트엔드 라이브러리(ex. Zod, TypeScript Interface)와의 호환성을 위해 타입을 맞추거나 백엔드에서 String으로 변환하여 응답해야 합니다.

*   **Spot Collaboration (1.5) 필드 누락 및 오류**
    *   **파일:** `capstone-api/src/main/java/backend/spot/dto/SpotChecklistResponse.java`
        *   `assigneeId`, `assigneeNickname` 필드가 누락되었습니다.
    *   **파일:** `capstone-api/src/main/java/backend/spot/dto/SpotFileResponse.java` (L44, L56)
        *   `sizeBytes` 필드가 `null`로 하드코딩되어 있습니다. 명세상 필수 값이므로 실제 파일 크기를 반영해야 합니다.

*   **Feed (1.6) 필드 누락 및 명명 규칙 위반**
    *   **파일:** `capstone-api/src/main/java/backend/feed/dto/FeedAuthorProfile.java` (L29)
        *   `@JsonProperty("avatar_url")` 사용으로 인해 JSON 응답이 `avatar_url`(snake_case)로 나갑니다. 명세는 `avatarUrl`(camelCase)입니다.
    *   **파일:** `capstone-api/src/main/java/backend/feed/dto/FeedItemResponse.java`
        *   `isRentable`, `myApplicationRole`, `myApplicationDeposit` 필드가 누락되었습니다.
    *   **파일:** `capstone-api/src/main/java/backend/feed/dto/FeedApplicationResponse.java` (L30-36)
        *   `appliedRole`, `deposit` 필드는 선언되어 있으나, `from()` 팩토리 메서드에서 빌더에 포함되지 않아 항상 `null` 또는 기본값으로 응답됩니다.

#### 2. 응답 Envelope 규격 불일치

*   **파일:** `capstone-api/src/main/java/backend/feed/controller/FeedController.java` (L74)
    *   `cancelApplication` 메서드가 `ApiResponse.success()`를 반환하여 `data: null`이 응답됩니다.
    *   **FRONTEND.md 명세:** `{ data: { feedId, status: "CANCELLED" } }` 형태의 응답을 기대하고 있습니다.

#### 3. 엔티티-DTO 간 불일치 및 마이그레이션 누락

*   **파일:** `capstone-domain/src/main/java/backend/feed/entity/FeedApplication.java`
    *   DTO(`FeedApplicationResponse`)에는 `appliedRole`, `deposit`이 추가되었으나, 엔티티에는 해당 필드가 없고 DB 마이그레이션 파일도 존재하지 않습니다.
    *   **의견:** 현재 DTO에서만 필드를 추가하고 매핑을 누락한 상태라 API가 불완전합니다. 후속 작업으로 분리하기보다는, **프론트엔드 명세 준수(Compliance)**가 목적인 PR인 만큼 엔티티 수정 및 마이그레이션을 이번 PR에 포함시키는 것이 아키텍처 일관성 측면에서 바람직합니다.

#### 4. 스타일 및 Import 순서 위반

*   **Import 순서:** `static→java→javax→org→com→backend` 순서를 준수해야 하나, `FeedController.java`, `SpotVoteResponse.java` 등에서 `backend.*` 패키지가 `io.swagger.*`나 `lombok.*`보다 앞에 위치하고 있습니다.
*   **Checkstyle:** 실행 결과 약 6,700건의 경고(대부분 CRLF vs LF Newline 문제)가 발생합니다. 프로젝트 표준인 LF로 파일 엔딩을 일괄 조정할 필요가 있습니다.

#### 5. SpotVoteOptionResponse.from() 위험성 확인

*   **확인 결과:** `SpotVoteOptionResponse.from()`은 현재 프로젝트 내에서 직접 호출되고 있지 않습니다. 다만, `SpotService.java` (L314)에서 유사하게 `voterIds`를 `List.of()`로 초기화하여 반환하는 로직이 있습니다. 이는 투표 생성 직후의 응답이므로 현재로서는 정상 동작으로 판단되나, 추후 `from()` 메서드를 무분별하게 사용할 경우 기존 투표 데이터를 조회할 때 투표자 목록이 누락될 위험이 있으므로 주의가 필요합니다.

---

### 💬 종합 의견
**Request Changes (❌ 수정 필요)**

프론트엔드 명세 준수를 위한 리팩토링 PR임에도 불구하고, **(1) 주요 필드 누락, (2) snake_case 혼용, (3) 응답 Envelope 불일치** 등 명세와 상충하는 지점이 많습니다. 특히 `appliedRole`과 `deposit`은 도메인 모델까지 영향이 가는 변경사항이므로, 이를 완전히 구현하거나 혹은 명세에서 제외하는 방향으로 동기화가 필요합니다. 위 이슈들을 수정한 후 다시 리뷰 요청 부탁드립니다.