# Backend Project - Spring Boot

이 프로젝트는 **Java 17**과 **Spring Boot 3.4.3** 기반의 백엔드 프로젝트입니다.
**네이버 핵데이 자바 컨벤션**을 채택하여 코드 품질과 일관성을 유지합니다.

## 🛠 Tech Stack & Versions
- **Language:** Java 17
- **Framework:** Spring Boot 3.4.3
- **Build Tool:** Gradle 8.7
- **Database:** PostgreSQL (Driver: `postgresql`)
- **API Documentation:** SpringDoc OpenAPI (Swagger UI) v2.3.0
- **Lombok:** Boilerplate 코드 자동 생성
- **Validation:** 데이터 유효성 검사 적용

## 📂 Project Structure (Domain-Driven)
```text
src/main/java/com/example/backend
├── domain/               # 비즈니스 도메인 로직 (회원, 주문 등)
└── global/               # 프로젝트 전역 설정
    ├── common/response/  # 공통 응답 클래스 (ApiResponse)
    ├── config/           # 전역 설정 (Swagger, DB 등)
    └── error/            # 에러 핸들링 (GlobalExceptionHandler, ErrorCode)
```

## 📝 Convention & Style Guide
이 프로젝트는 **네이버 핵데이 자바 컨벤션**을 따릅니다.

### 🔍 Checkstyle 실행 (터미널)
빌드 시 자동으로 검사하며, 수동으로 확인하려면 아래 명령어를 입력하세요.
```bash
./gradlew checkstyleMain
```

### ⌨️ IDE 필수 단축키 (IntelliJ 기준)
컨벤션을 유지하기 위해 코딩 중 수시로 아래 단축키를 사용해 주세요.
- **코드 자동 정렬:** `Ctrl + Alt + L` (Windows) / `Cmd + Opt + L` (Mac)
- **임포트 최적화:** `Ctrl + Alt + O` (Windows) / `Cmd + Opt + O` (Mac)
- **파일 전체 저장:** `Ctrl + S`

### ⚙️ IntelliJ 필수 설정 (중요!)
1. **Tabs vs Spaces:** 네이버 컨벤션은 **Tabs**를 사용합니다.
   - `Settings > Editor > Code Style > Java` 에서 `Use tab character` 체크
2. **Line Separator:** **LF** (Unix/macOS) 방식을 권장합니다.
   - `Settings > Editor > Code Style` 에서 `Line separator`를 `Unix (\n)`로 설정
3. **Checkstyle Plugin:** `Checkstyle-IDEA` 플러그인 설치 권장
   - 설치 후 `config/checkstyle/checkstyle.xml`을 규칙 파일로 등록하면 실시간으로 감지됩니다.

## 🚀 Infrastructure Features
- **Swagger UI:** `http://localhost:8080/swagger-ui/index.html` (서버 실행 시 자동 생성)
- **공통 응답:** `ApiResponse.success(data)`를 사용하여 JSON 포맷 통일
- **에러 처리:** `GlobalExceptionHandler`를 통해 모든 예외를 `ErrorCode` 기반으로 깔끔하게 반환

## 🗄 Database Setup
`src/main/resources/application.yml` 파일에서 본인의 PostgreSQL 계정 정보를 수정하여 사용하세요.
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/backend_db
    username: (본인계정)
    password: (본인패스워드)
```
