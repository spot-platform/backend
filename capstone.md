# Project Capstone Harness

이 파일은 프로젝트의 일관성을 유지하고, 협업 시 발생할 수 있는 충돌을 방지하기 위한 핵심 지침서입니다. 모든 작업자는 작업을 시작하기 전 이 내용을 반드시 숙지해야 합니다.

---

## 📢 1. AI 작업자 행동 지침 (General Rules)
1. **사용자 확인**: 작업을 시작하기 전, 반드시 현재 사용자의 이름을 확인하십시오. (소유자: **황호찬**)
2. **명세 준수**: 모든 필드명, 타입, 엔드포인트는 `UIBackendSpec.md`와 100% 일치해야 합니다. 임의 변경을 금지합니다.
3. **히스토리 파악**: `Work Log`와 `Todo List`를 읽어 중복 작업을 방지하고 문맥을 파악하십시오.
4. **작업 로그 기록**: 작업 완료 후 반드시 아래 양식으로 기록을 남기십시오.
   - **양식**: `[이름] [YYYY-MM-DD HH:mm]에 [수행한 업무]를 작성함. [앞으로 해야 할 일]을 해야 됨.`

---

## 🎨 2. 코드 컨벤션 및 구조 (Coding Standard)
1. **네이버 핵데이 컨벤션 준수**:
   - 들여쓰기: **Tab (1칸 = 4공백)** 사용.
   - 줄바꿈: **LF (Unix 방식)** 적용.
   - 임포트: 와일드카드(`.*`) 금지, 구체적 명시. (순서: static -> java -> javax -> org -> com -> backend)
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

## 📝 작업 로그 (Work Log)
- 황호찬 2026-04-10 14:00에 `UIBackendSpec.md` 기반으로 `Post`, `FeedItem`, `Spot` 엔티티와 Enum을 작성함.
- 황호찬 2026-04-10 14:30에 `GET /feeds` 피드 목록 조회 API를 작성함.
- 황호찬 2026-04-10 15:00에 `Post` 매칭 시 `Spot` 자동 생성 로직 및 서비스 테스트 코드를 작성함.
- 황호찬 2026-04-10 15:30에 전체 코드에 네이버 핵데이 컨벤션을 적용하고 컴파일 오류를 해결함.
- 황호찬 2026-04-10 18:45에 `POST /posts/offer` 및 `POST /posts/request` 게시글 등록 API를 작성함.
- 황호찬 2026-04-10 19:15에 QueryDSL 5.0.0을 사용하여 `GET /feeds` API를 고도화함.
- 황호찬 2026-04-10 20:10에 `FeedListQuery` 위치 조정으로 빌드 오류를 해결함. 현재 전체 빌드 및 컴파일 성공 상태임.
- 김동현 2026-04-13 10:00에 Spot/Chat 도메인 엔티티 8개(`SpotParticipant`, `SpotSchedule`, `SpotVote`, `SpotVoteOption`, `SpotVoteAnswer`, `SpotChecklist`, `SpotFile`, `SpotNote`) 및 Chat 엔티티 2개(`ChatRoom`, `ChatMessage`), Repository 10개, `SpotController`(21개 엔드포인트 뼈대), `ChatController`(9개 엔드포인트 뼈대)를 작성함. `SpotService` 나머지 기능(참여자/일정/투표/체크리스트/파일/노트/리뷰) 및 `ChatService`(SSE 포함) 구현을 해야 됨.
- 김동현 2026-04-13 14:30에 `SpotService` 기본 CRUD 및 상태 변경(`match`, `cancel`, `complete`) 구현, DTO 3개(`CreateSpotRequest`, `SpotResponse`, `SpotListResponse`) 작성, 논리적 FK 원칙 적용(엔티티 `@ManyToOne` → ID 필드로 전환), 네이버 핵데이 컨벤션 및 `@Valid` 검증 적용, `ParticipantRole` 명세서 기준(`AUTHOR`/`PARTICIPANT`)으로 수정, `ChatController` 패키지 경로 수정을 완료함. `SpotService` 나머지 기능 및 `ChatService` 구현을 해야 됨.
- 김동현 2026-04-27 10:00에 `SpotService` 나머지 기능(참여자 조회, 일정 추가/조회, 투표 생성/조회/참여, 체크리스트 조회/추가/토글, 파일 업로드/조회/삭제, 노트 작성/조회) 전부 구현 완료, 관련 DTO 11개 추가, 엔티티 비즈니스 메서드(`toggleDone`, `updateContent`, `incrementCount`, `update`) 추가, `SpotController` 전 엔드포인트 실제 서비스 연결 완료함. `ChatService` 구현 및 SSE 실시간 채팅 연결을 해야 됨.
- 김동현 2026-04-27 10:30에 `ChatService`(채팅방 CRUD, 커서 기반 메시지 페이지네이션, 메시지 전송), `SseEmitterService`(roomId 기반 구독·브로드캐스트, ping·timeout·error 처리), `ChatController`(전 엔드포인트 실제 연결 + SSE `/api/chat/connect?roomId=`) 구현 완료, Chat DTO 5개(`ChatRoomResponse`, `CreateChatRoomRequest`, `ChatMessageResponse`, `SendMessageRequest`, `ChatMessageListResponse`) 추가함. 인증 도입 후 dummy-user-id 교체 및 ChatRoomMember 테이블 추가가 필요함.

---

## 📌 현재 진행 상태 (Todo)

### 공통
- [x] 핵심 엔티티 및 리포지토리 구축 (Logical FK 원칙 적용)
- [x] QueryDSL 기반 동적 피드 조회 API 구현
- [x] 게시글 등록 및 자동 매칭/스팟 생성 로직 구현
- [ ] Neon Serverless Postgres 데이터베이스 연동 (`application.yml` 수정)
- [ ] 실제 데이터베이스 환경에서의 통합 테스트

- 이성찬 2026-04-12 에 Auth/User 파트 전체(JWT 인증, OAuth2 소셜 로그인, 회원가입/탈퇴, 프로필 관리)를 작성함. Neon Serverless Postgres 데이터베이스 연동(`application.yml` 수정) 및 실제 DB 환경에서의 통합 테스트를 해야 됨.

---

## 📌 현재 진행 상태 (Todo)

### 공통
- [x] 핵심 엔티티 및 리포지토리 구축 (Logical FK 원칙 적용)
- [x] QueryDSL 기반 동적 피드 조회 API 구현
- [x] 게시글 등록 및 자동 매칭/스팟 생성 로직 구현
- [ ] Neon Serverless Postgres 데이터베이스 연동 (`application.yml` 수정)
- [ ] 실제 데이터베이스 환경에서의 통합 테스트

### 황호찬 (Feed / Post 도메인)
- [ ] 게시글 상세 조회 및 수정/삭제 API (Soft Delete 적용 필요)

### 이성찬 (Auth / User / MY 도메인)
- [x] JWT 인증 필터 및 토큰 발급/갱신
- [x] OAuth2 소셜 로그인
- [x] 회원가입/탈퇴, 프로필 관리 API

### 김동현 (Spot / Chat 도메인)
- [x] Spot/Chat 전체 엔티티 및 Repository 구축
- [x] `SpotController`, `ChatController` 엔드포인트 뼈대 작성
- [x] `SpotService` 기본 CRUD(`GET /spots`, `POST /spots`, `GET /spots/{id}`) 구현
- [x] `SpotService` 상태 변경(`/match`, `/cancel`, `/complete`) 구현
- [x] `SpotService` 나머지 기능 (참여자 조회, 일정, 투표, 체크리스트, 파일, 노트)
- [x] `ChatService` 구현 (채팅방 생성/조회, 커서 기반 메시지 조회/전송, 읽음 처리 stub)
- [x] SSE 실시간 채팅 연결 (`/api/chat/connect?roomId=`) 구현 (단일 서버 인메모리)

---
*마지막 업데이트: 2026-04-27 10:30 (김동현)*
