![java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring](https://img.shields.io/badge/Spring-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![mySQL](https://img.shields.io/badge/MySQL-00000F?style=for-the-badge&logo=mysql&logoColor=white)
![gradle](https://img.shields.io/badge/Gradle-02303A.svg?style=for-the-badge&logo=Gradle&logoColor=white)
![aws](https://img.shields.io/badge/Amazon_AWS-FF9900?style=for-the-badge&logo=amazonaws&logoColor=white)
![DOCKER](https://img.shields.io/badge/docker-%230db7ed.svg?style=for-the-badge&logo=docker&logoColor=white)


# <img width="55" height="25" alt="image" src="src/main/resources/static/images/a3s-logo-v2.png" /> A3S Hub - 결제/구독 플랫폼 

> Spring Boot 기반 커머스 결제 & 구독 백엔드 프로젝트  
> PortOne(포트원) 연동 | JWT 인증 | 정기결제 구독 관리

---

## 📌 프로젝트 소개

CommerceHub는 실제 커머스 서비스에서 필요한 **결제, 구독, 포인트, 멤버십** 기능을 구현한 학습용 Spring Boot 프로젝트입니다.  
PortOne V2 SDK를 활용한 일반결제·빌링키 정기결제, JWT 인증, 포인트 FIFO 소멸 정책 등 실무 수준의 기능을 다룹니다.

---

## 🛠️ 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 4.0.3 |
| ORM | Spring Data JPA (Hibernate 7) |
| Security | Spring Security + JWT (jjwt 0.12) |
| DB (운영) | MySQL |
| DB (테스트) | H2 In-Memory |
| Build | Gradle 9.3.1 |
| Template | Thymeleaf |
| 결제 | PortOne V2 (포트원) |
| 인프라 | Docker, AWS EC2 (ARM64), SSM Parameter Store |
| CI/CD | GitHub Actions |

---

## 🏗️ 아키텍처 개요

```
Client (Thymeleaf + JS)
    │
    ▼
Spring Security (JWT Filter)
    │
    ▼
Controller Layer
    │
    ▼
Service Layer ──────► PortOne API (외부 결제)
    │
    ▼
Repository Layer (JPA)
    │
    ▼
MySQL (운영) / H2 (테스트)
```

---

## 📂 패키지 구조

```
src/main/java/com/example/a3sproject/
├── A3SProjectApplication.java
├── config/
│   ├── SecurityConfig.java          # Spring Security 설정
│   ├── PortOneConfig.java           # PortOne RestClient Bean
│   ├── DotenvInitializer.java       # .env 파일 로드
│   └── initializer/
│       └── DataInitializer.java     # 초기 더미 데이터 삽입
├── domain/
│   ├── user/                        # 사용자 도메인
│   ├── order/                       # 주문 도메인
│   ├── payment/                     # 결제 도메인
│   ├── refund/                      # 환불 도메인
│   ├── point/                       # 포인트 도메인
│   ├── membership/                  # 멤버십/등급 도메인
│   ├── product/                     # 상품 도메인
│   ├── plan/                        # 구독 플랜 도메인
│   ├── subscription/                # 구독 도메인
│   ├── paymentMethod/               # 결제수단(빌링키) 도메인
│   └── portone/                     # PortOne 연동 (Client, Webhook)
└── global/
    ├── controller/                  # 인증, 설정, 홈 컨트롤러
    ├── dto/                         # 공통 응답 DTO
    ├── exception/                   # 공통 예외 처리
    ├── security/                    # JWT 필터, UserDetails
    └── aop/                         # 로깅 AOP
```

---

## ⚙️ 환경 설정

### 1. `.env` 파일 생성 (로컬 개발)

프로젝트 루트에 `.env` 파일을 생성합니다.

```env
JWT_SECRET=your-secret-key-minimum-64-chars-here
JWT_VALIDITY=3600
JWT_REFRESH_VALIDITY=604800

PORTONE_API_SECRET=your_portone_api_secret
PORTONE_STORE_ID=your_portone_store_id
PORTONE_CHANNEL_TOSS=your_toss_channel_key
PORTONE_CHANNEL_TOSS_BILLING=your_toss_billing_channel_key
```

### 2. `application.yml` 주요 설정

```yaml
api:
  base-url: http://localhost:8080  # 로컬 개발 시
  # base-url: https://your-domain.com  # 운영 시

portone:
  api:
    base-url: https://api.portone.io
```

### 3. 테스트 계정

애플리케이션 시작 시 자동으로 더미 데이터가 생성됩니다.

| 이메일 | 비밀번호 | 설명 |
|--------|---------|------|
| `admin@test.com` | `admin123` | 관리자 계정 |
| `abc@abc.com` | `12345678` | VVIP 테스트 계정 (포인트 100,000P) |

---

## 🚀 실행 방법

```bash
# 1. 프로젝트 클론
git clone https://github.com/TeamA3S/A3S-project.git
cd A3S-project

# 2. .env 파일 생성 (위 환경 설정 참고)

# 3. 빌드 및 실행
./gradlew bootRun

# 또는 JAR 빌드 후 실행
./gradlew build -x test
java -jar build/libs/A3S-project-*.jar

# 4. 브라우저에서 접속
# http://localhost:8080
```

### Docker 실행

```bash
# 이미지 빌드
docker build -t a3s-project .

# 컨테이너 실행
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e JWT_SECRET=your_secret \
  -e PORTONE_API_SECRET=your_portone_secret \
  -e PORTONE_STORE_ID=your_store_id \
  a3s-project
```

---

## 🔑 주요 API

### 인증

| 메서드 | URL | 설명 |
|--------|-----|------|
| POST | `/api/auth/login` | 로그인 (Access + Refresh Token 발급) |
| POST | `/api/auth/reissue` | 토큰 재발급 (RTR 방식) |
| POST | `/api/auth/logout` | 로그아웃 |
| POST | `/api/users/signUp` | 회원가입 |
| GET | `/api/users/me` | 내 프로필 조회 |

### 상품 & 주문

| 메서드 | URL | 설명 |
|--------|-----|------|
| GET | `/api/products` | 상품 목록 조회 (판매중만) |
| GET | `/api/products/{productId}` | 상품 단건 조회 |
| POST | `/api/orders` | 주문 생성 |
| GET | `/api/orders` | 내 주문 목록 조회 |
| GET | `/api/orders/{orderId}` | 주문 상세 조회 |

### 결제

| 메서드 | URL | 설명 |
|--------|-----|------|
| POST | `/api/payments/attempt` | 결제 시작 (PENDING 등록) |
| POST | `/api/payments/{paymentId}/confirm` | 결제 확정 (PortOne 검증) |
| POST | `/api/refunds/{portOneId}` | 환불 처리 |
| POST | `/api/payments/webhook` | PortOne 웹훅 수신 |

### 포인트 & 멤버십

| 메서드 | URL | 설명 |
|--------|-----|------|
| GET | `/api/points/me` | 포인트 거래 내역 조회 |
| GET | `/api/memberships/me` | 멤버십 등급 조회 |

### 구독

| 메서드 | URL | 설명 |
|--------|-----|------|
| GET | `/api/plans` | 플랜 목록 조회 |
| POST | `/api/subscriptions` | 구독 생성 (빌링키 + 구독 트랜잭션) |
| GET | `/api/subscriptions/{subscriptionId}` | 구독 조회 |
| PATCH | `/api/subscriptions/{subscriptionId}` | 구독 해지 |
| POST | `/api/subscriptions/{subscriptionId}/billings` | 즉시 청구 |
| GET | `/api/subscriptions/{subscriptionId}/billings` | 청구 내역 조회 |

---

## 💳 결제 플로우

### 일반 결제 (PortOne SDK)

```
1. POST /api/payments/attempt  ──► DB에 PENDING 상태로 결제 기록
2. PortOne SDK (클라이언트)     ──► 결제창 열기 (서버에서 받은 paymentId 사용)
3. POST /api/payments/{id}/confirm ──► PortOne 서버 검증 후 주문 완료
```

### 포인트 포함 복합 결제

```
1. POST /api/payments/attempt (pointsToUse 포함)
   ├── 포인트 전액 결제: 즉시 완료 (PortOne SDK 호출 생략)
   └── 일부 포인트: 포인트 차감 후 잔여 금액으로 PortOne SDK 호출
2. POST /api/payments/{id}/confirm
```

### 구독 결제 (빌링키)

```
1. PortOne SDK (클라이언트) ──► 빌링키 발급
2. POST /api/subscriptions ──► 빌링키 저장 + 구독 생성 + 최초 결제
3. 매일 자정 스케줄러       ──► 만료된 구독 자동 정기결제
```

---

## 🏅 멤버십 등급 정책

| 등급 | 조건 (총 결제금액) | 포인트 적립률 |
|------|-----------------|------------|
| NORMAL | 기본 | 1% |
| VIP | 300,000원 이상 | 5% |
| VVIP | 500,000원 이상 | 10% |

- 결제 확정 시 자동 등급 갱신
- 환불 시 결제금액 차감 후 등급 재계산

---

## 💰 포인트 정책

- **적립**: 결제 완료 시 결제 금액 × 적립률, 만료일 1년
- **사용**: FIFO 방식 (만료일 빠른 것부터 차감)
- **소멸**: 매일 자정 스케줄러로 만료 포인트 자동 소멸
- **복구**: 결제 실패 시 사용 포인트 자동 복구
- **취소**: 환불 시 적립 포인트 회수

---

## 🔐 보안

- **JWT 인증**: Access Token (헤더) + Refresh Token (바디) 이중 발급
- **RTR (Refresh Token Rotation)**: 재발급 시 기존 토큰 즉시 무효화
- **IDOR 방지**: 모든 데이터 조회 시 userId 소유권 검증
- **비관적 락**: 포인트 차감 시 동시성 보장
- **낙관적 락**: 결제 시점 재고 조회 시 충돌 방지
- **웹훅 멱등성**: webhookUuid 기반 중복 처리 방지

---

## 🧪 테스트 실행

```bash
# 전체 테스트 실행
./gradlew test

# 특정 테스트 클래스 실행
./gradlew test --tests "*.PaymentServiceTest"
```

### 테스트 구성

- **단위 테스트**: Mockito 기반 (Service, Domain 로직)
- **통합 테스트**: MockMvc + H2 기반 (Controller, Repository)
- 총 테스트 커버리지: 주요 도메인 100+ 테스트 케이스

---

## 🚢 배포 (CI/CD)

### GitHub Actions 워크플로우

```
Push to dev 브랜치
    │
    ▼
CI: Gradle 빌드 + JAR 저장
    │
    ▼
CD: Docker 이미지 빌드 (ARM64) & DockerHub Push
    │
    ▼
EC2 배포 (AWS SSM Parameter Store 환경변수 주입)
```

### 환경 변수 (GitHub Secrets)

```
JWT_SECRET, PORTONE_API_SECRET, PORTONE_STORE_ID
PORTONE_CHANNEL_TOSS, PORTONE_CHANNEL_TOSS_BILLING
DB_URL, DB_USERNAME, DB_PASSWORD
DOCKERHUB_USERNAME, DOCKERHUB_TOKEN
AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY
```

---

## 🖥️ 프론트엔드 페이지

| URL | 설명 |
|-----|------|
| `/` | 홈 (기능 소개) |
| `/pages/login` | 로그인 |
| `/pages/register` | 회원가입 |
| `/pages/shop` | 상점 (상품 목록, 주문 생성) |
| `/pages/orders` | 주문 관리 (결제 진행, 확정, 환불) |
| `/pages/points` | 포인트 포함 결제 테스트 |
| `/pages/plans` | 구독 플랜 목록 |
| `/pages/subscribe` | 구독 신청 (빌링키 발급) |
| `/pages/subscriptions` | 구독 관리 (청구, 해지) |
| `/pages/mypage` | 내 정보 조회 (포인트, 주문내역, 멤버십 등) |

---

## 📋 공통 응답 형식(예시)

```json
// 성공
{
  "success": true,
  "status": 200,
  "data": { ... }
}

// 실패
{
  "code": "USER_NOT_FOUND",
  "message": "유저를 찾을 수 없습니다.",
  "data": null
}
```

---

## 📝 개선 사항 (해결 완료)

피드백 리포트에서 확인된 개선 필요 항목입니다.

- **재고 동시성**: 주문 시 비관적 락 미적용 → 초과 판매 위험
- **포인트 소멸 스케줄러**: 대량 데이터 처리 시 Batch 처리 필요
- **트랜잭션 내 외부 API 호출**: PortOne API와 DB 트랜잭션 분리 필요
- **환불 포인트 회수**: 포인트 소진 시 환불 실패 가능 (비즈니스 정책 검토 필요)
- **회원/멤버십 조회 효율화**: 2-step 쿼리 → Join Fetch 최적화 여지
