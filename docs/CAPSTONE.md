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
   - 임포트: 와일드카드(`.*`) 금지, 구체적 명시. (순서: static → java → javax → org → com → backend)
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
