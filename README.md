# Point Assignment 2603

Spring Boot 기반 포인트 관리 시스템.  
포인트 적립·사용·취소·만료 처리와 어드민 관리 화면을 포함합니다.

---

## 기술 스택

| 항목 | 내용 |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5.11 |
| Persistence | Spring Data JPA |
| Database | H2 (파일 모드, MySQL 호환) |
| Build | Gradle |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| 기타 | Lombok, DevTools |

---

## 실행 방법

### 1. 최초 실행 / 스키마 변경 후

H2는 파일 기반 DB이므로 스키마가 변경된 경우 기존 DB 파일을 삭제해야 합니다.

```bash
# 프로젝트 루트의 data 폴더 삭제
rm -rf data/
```

### 2. 애플리케이션 실행

```bash
./gradlew bootRun
```

> Windows 터미널에서 한글 로그를 올바르게 출력하려면 먼저 `chcp 65001`을 실행하세요.

### 3. 접속 주소

| 경로 | 내용 |
|---|---|
| `http://localhost:8080/admin-policy.html` | 어드민 관리 화면 |
| `http://localhost:8080/swagger-ui.html` | Swagger API 문서 |
| `http://localhost:8080/h2-console` | H2 콘솔 |

---

## 프로젝트 구조

```
src/main/java/.../pointassignment2603/
├── api/
│   ├── point/
│   │   ├── controller/   PointApiController        포인트 API 엔드포인트
│   │   ├── service/      PointApiService            API 레이어 서비스
│   │   └── dto/          요청/응답 DTO
│   ├── admin/
│   │   └── controller/   AdminWalletController      어드민 지갑·이력 API
│   └── policy/
│       └── controller/   PointPolicyController      정책 관리 API
├── domain/
│   ├── point/
│   │   ├── entity/       JPA 엔티티 (지갑, 적립, 사용, 매핑)
│   │   ├── repository/   Spring Data JPA 레포지토리
│   │   └── service/
│   │       ├── PointDomainService   핵심 비즈니스 로직
│   │       └── AdminPointService    어드민 조회 로직
│   └── policy/
│       ├── entity/       PointPolicy, PolicyKey
│       ├── repository/   PointPolicyRepository
│       └── service/      PointPolicyService (캐싱 포함)
├── common/
│   ├── component/        IdGenerator
│   ├── config/           CacheConfig
│   └── error/            ErrorCode, PointApiException, GlobalExceptionHandler
└── resources/
    ├── schema.sql        DDL (테이블 + DB 시퀀스)
    ├── data.sql          초기 데이터
    ├── application.yml
    └── static/
        └── admin-policy.html  어드민 화면
```

---

## 도메인 설계

### 테이블 구조

```
user_point_wallet          지갑 (잔액, 보유한도, 만료 예정 정보)
  │
  ├── point_earn_record    적립 원장 (FIFO 차감 기준)
  │
  ├── point_usage_record   사용/취소 이력
  │
  └── point_usage_earn_mapping   사용-적립 간 상세 매핑
  
point_policy               정책 테이블 (KEY-VALUE)
```

### 공통 코드

| 구분 | 값 | 설명 |
|---|---|---|
| **EARN_TYPE** | `ORDER` | 주문 적립 |
| | `MANUAL` | 수기 적립 |
| | `EVENT` | 이벤트 적립 |
| | `RE_EARN` | 만료 후 취소 재적립 |
| **EARN_STATUS** | `ACTIVE` | 사용 가능 |
| | `EXPIRED` | 만료됨 |
| | `CANCELLED` | 적립 취소됨 |
| **USAGE_TYPE** | `USE` | 포인트 사용 |
| | `PARTIAL_CANCEL` | 부분 취소 |
| | `FULL_CANCEL` | 전체 취소 |
| | `EXPIRE` | 만료 차감 (내부 처리용) |
| **USAGE_STATUS** | `COMPLETED` | 정상 완료 |
| | `PARTIAL_CANCEL` | 부분 취소됨 |
| | `FULL_CANCEL` | 전체 취소됨 |
| **WALLET_TYPE** | `FREE` | 무료 포인트 |
| | `CASH` | 충전 포인트 |

### ID 형식

DB 시퀀스(0000~9999 CYCLE) 기반으로 생성합니다.

```
{PREFIX}-{yyMMddHHmmss}{4자리 시퀀스}
예) ERN-2603151432010042
```

| 접두사 | 대상 |
|---|---|
| `WLT` | 지갑 |
| `ERN` | 적립 |
| `USG` | 사용 |
| `CNC` | 전체 취소 |
| `PCN` | 부분 취소 |

---

## API 목록

### 포인트 API (`/api/point`)

| Method | Path | 설명 |
|---|---|---|
| `GET` | `/api/point` | 지갑 조회 (만료 갱신 포함) |
| `POST` | `/api/point/earn` | 포인트 적립 |
| `POST` | `/api/point/earn/cancel` | 적립 취소 (미사용 건만 가능) |
| `POST` | `/api/point/use` | 포인트 사용 |
| `POST` | `/api/point/use/cancel` | 사용 취소 (부분/전체) |

#### GET /api/point
```
Query: userId (필수), walletType (선택: FREE | CASH)
```

#### POST /api/point/earn
```json
{
  "walletId": "WLT-...",
  "orderNo": "ORD-001",
  "earnType": "ORDER",
  "amount": 10000,
  "expiryDays": 365
}
```
> `expiryDays` 생략 시 기본값 365일 적용

#### POST /api/point/earn/cancel
```json
{
  "walletId": "WLT-...",
  "earnId": "ERN-..."
}
```
> 미사용(remaining = original) 상태인 ACTIVE 적립 건만 취소 가능

#### POST /api/point/use
```json
{
  "walletId": "WLT-...",
  "orderNo": "ORD-001",
  "amount": 5000
}
```
> FIFO 순서로 차감: `MANUAL` 먼저 → 만료일 짧은 순 → 적립일 오래된 순

#### POST /api/point/use/cancel
```json
{
  "walletId": "WLT-...",
  "originalUsageId": "USG-...",
  "cancelAmount": 3000,
  "cancelType": "PARTIAL_CANCEL"
}
```
> - `cancelType`: `PARTIAL_CANCEL` | `FULL_CANCEL`  
> - `FULL_CANCEL` 시 `cancelAmount` 생략 가능 (잔여 전액 자동 계산)  
> - 부분 취소 이력이 있는 건은 전체 취소 불가  
> - `PARTIAL_CANCEL` 시 `cancelAmount` 1원 이상 필수

---

### 어드민 API

#### 지갑 관리 (`/api/admin/wallet`)

| Method | Path | 설명 |
|---|---|---|
| `GET` | `/api/admin/wallet` | 전체 지갑 목록 조회 |
| `GET` | `/api/admin/wallet/search?userId=` | userId로 지갑 목록 조회 |
| `GET` | `/api/admin/wallet/{walletId}/earn-history` | 적립 이력 조회 (`?earnStatus=`) |
| `GET` | `/api/admin/wallet/{walletId}/usage-history` | 사용/취소 이력 조회 (`?usageType=`) |
| `PUT` | `/api/admin/wallet/{walletId}/max-balance` | 보유 한도 변경 (FREE 타입 전용) |

#### 정책 관리 (`/api/admin/policy`)

| Method | Path | 설명 |
|---|---|---|
| `GET` | `/api/admin/policy` | 전체 정책 조회 |
| `PUT` | `/api/admin/policy/{key}` | 정책 값 변경 (캐시 자동 무효화) |

---

## 핵심 비즈니스 로직

### 포인트 적립

1. 지갑 조회 → 만료 처리 선행
2. 1회 최대 적립 금액 체크 (정책: `MAX_EARN_AMOUNT_PER_TX`)
3. 보유 한도 체크 (지갑별 `max_balance_limit`)
4. 적립 원장 INSERT (중복 `orderNo` 허용, 동일 `earnId` 중복 시 409)
5. 지갑 잔액 증가

### 포인트 사용 - FIFO 차감

1. 지갑 조회 → 만료 처리 선행
2. 잔액 확인
3. 지갑 잔액 차감 (원자적 UPDATE)
4. 적립 원장에서 FIFO 순으로 차감 대상 선택  
   우선순위: `MANUAL` 먼저 → 만료일 짧은 순 → 적립일 오래된 순
5. 적립 건별 잔액 차감 + 사용-적립 매핑 테이블 INSERT
6. 사용 이력 INSERT

### 사용 취소 - 부분/전체

- **전체 취소(`FULL_CANCEL`)**: 부분 취소 이력이 없어야 함. 취소 금액 자동 계산(원거래 금액 - 기취소 금액).
- **부분 취소(`PARTIAL_CANCEL`)**: 취소 금액 필수(1원 이상). 이미 취소된 금액 + 이번 취소 금액 ≤ 원거래 금액
- 적립 원장 환급 시:
  - `ACTIVE` 건: 잔액 직접 복원
  - `EXPIRED` 건: 신규 `RE_EARN` 적립 생성 (원 `expiryDays` 기준으로 만료일 재계산)

### 만료 처리 (Lazy Refresh)

지갑 조회·적립·사용·적립취소 등 지갑에 접근하는 모든 연산 수행 전 자동 실행:

1. `expiration_date <= today` 인 ACTIVE 적립 건을 `EXPIRED` 처리, 잔액 0으로 일괄 변경
2. 지갑 잔액에서 만료 금액 차감
3. 다음 만료 예정 정보(날짜, 금액) 갱신
4. `expiration_updated_at` 이 오늘 날짜 이후이면 갱신 생략 (당일 중복 처리 방지)

### 멱등성

| 연산 | 멱등성 여부 | 비고 |
|---|---|---|
| 포인트 적립 | **조건부 멱등** | `earnId`(PK) 중복 시 `409 DUPLICATE_EARN_RECORD` 반환. 동일 `earnId`로 재호출해도 DB 변경 없음 |
| 포인트 사용 | **조건부 멱등** | `usageId`(PK) 중복 시 `409 DUPLICATE_USAGE_RECORD` 반환. 동일 `usageId`로 재호출해도 DB 변경 없음 |
| 사용 취소 | **비멱등** | 취소 ID는 매 호출마다 채번됨. 동일 원거래에 대해 중복 취소 방지는 취소 가능 잔액 체크로 제한 |
| 적립 취소 | **조건부 멱등** | 이미 취소된 `earnId`에 재요청 시 `400 EARN_ALREADY_CANCELLED` 반환 |

### 정책 캐싱

`PointPolicyService`에서 `@Cacheable` / `@CacheEvict` 사용.  
`ConcurrentMapCacheManager` 기반 인메모리 캐시로, 정책 수정 시 해당 키의 캐시 자동 무효화.

---

## 정책 테이블

| 키 | 기본값 | 설명 |
|---|---|---|
| `MAX_EARN_AMOUNT_PER_TX` | 100,000 | 1회 최대 적립 가능 금액 (원) |

> `http://localhost:8080/admin-policy.html` 에서 변경 가능

---

## 에러 코드

| 코드 | HTTP | 내용 |
|---|---|---|
| C001 | 400 | 올바르지 않은 입력값 |
| C004 | 500 | 서버 내부 오류 |
| P001 | 404 | 지갑 없음 |
| P002 | 400 | 잔액 부족 |
| P003 | 400 | 보유 한도 초과 |
| P004 | 409 | 적립 ID 중복 (멱등 처리) |
| P005 | 409 | 사용 ID 중복 (멱등 처리) |
| P006 | 404 | 적립 이력 없음 |
| P007 | 404 | 사용 이력 없음 |
| P008 | 400 | 이미 전체 취소된 거래 |
| P009 | 400 | 부분 취소 이력 있어 전체 취소 불가 |
| P010 | 400 | 취소 금액이 취소 가능 잔액 초과 |
| P011 | 500 | 적립 잔액 환급 처리 실패 (동시성 충돌) |
| P012 | 400 | 이미 취소된 적립 건 |
| P013 | 400 | 일부 사용된 적립 건은 취소 불가 |
| P014 | 404 | 정책 정보 없음 |
| P015 | 400 | 1회 최대 적립 금액 초과 |
| P016 | 400 | FREE 타입 지갑만 보유 한도 변경 가능 |
| P017 | 400 | 부분 취소 시 취소 금액 필수 (1원 이상) |

---

## 어드민 화면

`http://localhost:8080/admin-policy.html`

| 섹션 | 기능 |
|---|---|
| 정책 관리 | 현재 정책 조회 및 인라인 수정 |
| 지갑 조회 | 진입 시 전체 지갑 자동 로드. userId 검색 → 지갑 테이블 → 클릭 선택 |
| 적립 이력 탭 | 상태 필터(전체/ACTIVE/EXPIRED/CANCELLED) + 이력 테이블 + 합계 행. 정렬: MANUAL 우선 → 만료일 오름차순 |
| 사용 이력 탭 | USE 이력 + 차감 적립건 서브테이블 (원금·잔액·차감액·합계) |
| 취소 이력 탭 | 부분/전체 취소 이력 + 원거래 정보 서브테이블 + 환급 적립건 서브테이블 |
| 거래 입력 | 적립 / 적립취소 / 사용 / 사용취소 폼. 지갑 선택 시 ID 자동 입력. 응답 레이어 팝업 표시 |
| 보유한도 변경 | 지갑별 max_balance_limit 수정 (FREE 타입 전용) |
