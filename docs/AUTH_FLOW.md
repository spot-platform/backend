# AUTH FLOW

이 문서는 SPOT FE 연동을 위한 인증 흐름을 정리한다. 핵심 원칙은 Access Token(AT)은 FE가 API 호출에 사용하고, Refresh Token(RT)은 BE가 HttpOnly 쿠키로 관리한다는 점이다.

## 1. 자체 로그인

### Request

| Method | URL | Body |
| --- | --- | --- |
| POST | `/api/auth/login` | `email`, `password` |

### Response

| Field | Description |
| --- | --- |
| `accessToken` | API 호출에 사용할 Access Token |
| `userId` | 로그인한 사용자 ID |
| `redirectTo` | 로그인 후 이동할 경로. `next` 쿼리가 없으면 `/feed` |

```json
{
  "accessToken": "eyJhbGci...",
  "userId": "uuid-string",
  "redirectTo": "/feed"
}
```

### Side effects

| Side effect | Description |
| --- | --- |
| RefreshEntity 저장 | 기존 사용자 RT를 삭제한 뒤 새 RT를 저장한다. |
| `Set-Cookie: refresh=...` | RT는 HttpOnly 쿠키로만 전달된다. JS에서 직접 접근할 수 없다. |

### Token usage

| Token | FE 사용 방식 |
| --- | --- |
| `accessToken` | API 호출 시 `Authorization: Bearer <accessToken>` 헤더에 넣는다. |
| `refresh` cookie | JS에서 접근하지 않는다. AT 만료 시 body 없이 `POST /api/auth/refresh`를 호출하면 브라우저가 쿠키를 자동 전송한다. 소셜 로그인 토큰 교환 흐름에서는 body 없이 `POST /api/jwt/exchange`를 호출한다. |

## 2. 소셜 로그인

| Step | Description |
| --- | --- |
| 1 | FE가 `/api/auth/oauth/{provider}/start`로 이동한다. provider는 `naver`, `google`을 사용한다. |
| 2 | Spring Security OAuth2가 provider 인증을 처리한다. |
| 3 | 성공 시 BE가 RT를 HttpOnly `refresh` 쿠키로 저장하고 FE 콜백 페이지로 리다이렉트한다. |
| 4 | FE는 `POST /api/jwt/exchange`를 body 없이 호출해 쿠키 기반으로 토큰을 교환한다. |

## 3. 토큰 재발급

### Request

| Method | URL | Body | Credential |
| --- | --- | --- | --- |
| POST | `/api/auth/refresh` | 없음 | HttpOnly `refresh` 쿠키 자동 전송 |

### Response

| Field | Description |
| --- | --- |
| `accessToken` | 새 Access Token |

### Side effects

| Side effect | Description |
| --- | --- |
| Refresh Rotation | 기존 RT를 원자적으로 삭제하고 새 RT를 저장한다. 이미 사용된 RT를 다시 쓰면 401 처리한다. |
| `Set-Cookie: refresh=...` | 새 RT를 HttpOnly 쿠키로 다시 내려준다. FE는 RT 값을 읽거나 저장하지 않는다. |

## 4. 로그아웃

| Method | URL | Header |
| --- | --- | --- |
| POST | `/api/auth/logout` | `Authorization: Bearer <accessToken>` |

BE는 Access Token에서 식별한 사용자의 RefreshEntity를 삭제하고, 응답에 `Set-Cookie: refresh=; Max-Age=0; Path=/; HttpOnly; Secure; SameSite=Strict`를 포함해 refresh 쿠키를 clear한다.

FE는 로컬에 보관 중인 AT만 폐기하면 된다.

## 5. API 호출 규칙

| 상황 | FE 처리 |
| --- | --- |
| 일반 API 호출 | `Authorization` 헤더에 AT를 넣는다. |
| AT 만료 감지 | body 없이 `POST /api/auth/refresh` 호출 후 새 AT로 원 요청을 재시도한다. |
| refresh 실패 | 로그인 화면으로 이동하고 로컬 AT를 폐기한다. |
| 로그아웃 | `POST /api/auth/logout` 호출 후 로컬 AT를 폐기한다. |

## 6. FE 측 권장 구현 흐름

| Step | Description |
| --- | --- |
| 로그인 성공 | 응답 body의 `accessToken`, `userId`, `redirectTo`만 처리한다. RT는 BE가 쿠키로 관리하므로 FE는 신경 쓸 필요가 없다. |
| AT 저장 | AT만 메모리 또는 FE 정책에 맞는 storage에 둔다. |
| API 요청 | AT를 `Authorization` 헤더에 붙인다. |
| 401 또는 AT 만료 | `POST /api/auth/refresh`를 body 없이 호출한다. 쿠키는 브라우저가 자동 전송한다. |
| refresh 성공 | 새 AT를 저장하고 실패했던 요청을 한 번 재시도한다. |
| refresh 실패 | 로컬 AT를 폐기하고 로그인 화면으로 이동한다. |
| 로그아웃 | BE가 RefreshEntity 삭제와 `Set-Cookie: refresh=; Max-Age=0` 쿠키 clear까지 책임진다. FE는 로컬 AT만 폐기하면 된다. |

## 7. 알려진 갭 / 후속 검토

| Item | Status |
| --- | --- |
| 옵션 B 적용 | FE 권장 흐름은 RT를 JS에 노출하지 않는 쿠키 기반 방식으로 재작성됐다. |
| `/api/jwt/exchange` 응답 RT 제거 | 이번 PR 범위에서는 기존 동작을 유지한다. 추후 옵션 D로 후속 PR 검토 가능. |
| CSRF 토큰 | 이번 PR 범위에서는 도입하지 않는다. 현재 쿠키는 `SameSite=Strict`를 사용한다. |
| 모바일/non-browser 클라이언트 | 이번 PR은 SPA 브라우저 클라이언트를 가정한다. |
