# Jackson 애너테이션을 활용한 필드 매핑 가이드

`client-api-config.yml`의 API 계약을 유지하면서 백엔드에서 자유롭게 필드명을 사용하는 방법

---

## 기본 개념

YAML(외부 API 계약)과 Java 필드명 (내부 구현)을 분리하여 관리할 수 있습니다.

```yaml
# client-api-config.yml (변경 안 함)
response:
  fields:
    - name: user_id
      type: string
```

```java
// Java (자유롭게 필드명 선택)
@JsonProperty("user_id")  // API 계약 준수
private String userId;     // Java 컨벤션 준수
```

---

## 주요 Jackson 애너테이션

### 1. @JsonProperty - 필드명 매핑

가장 기본적이고 많이 사용하는 애너테이션입니다.

```java
@Data
public class UserInfoResponse {
    // API: customerId → Java: customerUid
    @JsonProperty("customerId")
    private String customerUid;

    // API: phone_number → Java: phoneNumber
    @JsonProperty("phone_number")
    private String phoneNumber;

    // API: email → Java: email (동일하면 생략 가능)
    private String email;
}
```

**JSON 출력:**
```json
{
  "customerId": "CUST-123",
  "phone_number": "010-1234-5678",
  "email": "user@test.com"
}
```

---

### 2. @JsonAlias - 여러 이름 허용 (요청)

요청을 받을 때 여러 필드명을 허용합니다.

```java
@Data
public class CreateOrderRequest {
    // "product_id", "id", "productId" 모두 허용
    @JsonAlias({"product_id", "id"})
    private String productId;

    // "qty", "count", "quantity" 모두 허용
    @JsonAlias({"qty", "count"})
    private Integer quantity;
}
```

**허용되는 요청:**
``` text
{ "productId": "1", "quantity": 2 }
{ "product_id": "1", "qty": 2 }
{ "id": "1", "count": 2 }
```

---

### 3. @JsonInclude - null/빈 값 제외

응답에서 null이나 빈 값을 제외합니다.

```java
@Data
public class OrderResponse {
    private String orderId;

    // null이면 JSON에서 제외
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String cancelReason;

    // 빈 문자열도 제외
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String description;
}
```

**JSON 출력 (cancelReason이 null일 때):**
``` text
{
  "orderId": "ORD-123"
  // cancelReason 필드 자체가 제외됨
}
```

---

### 4. @JsonIgnore - 특정 필드 제외

JSON 직렬화/역직렬화에서 완전히 제외합니다.

```java
@Data
public class UserResponse {
    private String email;
    private String name;

    // 내부용 필드 (JSON에 포함 안 함)
    @JsonIgnore
    private String passwordHash;

    @JsonIgnore
    private String internalNote;
}
```

---

### 5. @JsonFormat - 날짜/숫자 포맷

날짜, 시간, 숫자의 포맷을 지정합니다.

```java
@Data
public class OrderResponse {
    // 날짜 포맷 지정
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime createdAt;

    // ISO 8601 형식
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    // 숫자를 문자열로
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;
}
```

**JSON 출력:**
```json
{
  "createdAt": "2026-02-06 14:30:00",
  "updatedAt": "2026-02-06T14:30:00",
  "id": "123"
}
```

---

### 6. @JsonNaming - 클래스 전체 네이밍 전략

클래스의 모든 필드에 네이밍 전략을 적용합니다.

```java
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ProductResponse {
    // 자동으로 product_id로 변환
    private String productId;

    // 자동으로 unit_price로 변환
    private BigDecimal unitPrice;

    // 자동으로 stock_quantity로 변환
    private Integer stockQuantity;
}
```

**JSON 출력:**
```json
{
  "product_id": "PROD-1",
  "unit_price": 10000,
  "stock_quantity": 50
}
```

**사용 가능한 전략:**
- `SnakeCaseStrategy` - `snake_case`
- `KebabCaseStrategy` - `kebab-case`
- `LowerCaseStrategy` - `lowercase`
- `UpperCamelCaseStrategy` - `PascalCase`

---

### 7. @JsonUnwrapped - 중첩 객체 평탄화

중첩된 객체를 상위 레벨로 펼칩니다.

```java
@Data
public class Address {
    private String city;
    private String street;
}

@Data
public class UserResponse {
    private String name;

    // address 객체를 풀어서 같은 레벨에 배치
    @JsonUnwrapped
    private Address address;
}
```

**Java 객체:**
``` text
UserResponse {
  name = "홍길동";
  // address = { city = "서울", street = "강남대로" }
}
```

**JSON 출력:**
```json
{
  "name": "홍길동",
  "city": "서울",
  "street": "강남대로"
}
```

---

### 8. @JsonProperty(access) - 읽기/쓰기 제어

필드를 읽기 전용 또는 쓰기 전용으로 만듭니다.

```java
@Data
public class RegisterRequest {
    private String email;

    // 요청에서만 받고 응답에는 포함 안 함
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
}

@Data
public class UserResponse {
    // 응답에만 포함하고 요청에서는 무시
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String userId;

    private String name;
}
```

---

## 실전 예시

### 예시 1: snake_case API + camelCase Java

**YAML:**
```yaml
get-current-user:
  response:
    fields:
      - name: customer_uid
      - name: point_balance
      - name: phone_number
```

**Java:**
```java
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class UserInfoResponse {
    private String customerUid;     // → customer_uid
    private Long pointBalance;      // → point_balance
    private String phoneNumber;     // → phone_number
}
```

---

### 예시 2: 레거시 API 지원

**YAML:**
```yaml
create-order:
  request:
    fields:
      - name: prod_id  # 레거시 이름
```

**Java:**
```java
@Data
public class CreateOrderRequest {
    @JsonAlias({"prod_id", "product_id"})  // 둘 다 허용
    @JsonProperty("prod_id")               // 응답은 prod_id로
    private String productId;              // 내부는 명확한 이름
}
```

---

### 예시 3: 날짜 포맷 통일

**YAML:**
```yaml
list-orders:
  response:
    items:
      - name: created_at
        type: string
```

**Java:**
```java
@Data
public class OrderResponse {
    @JsonProperty("created_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}
```

**JSON:**
```json
{
  "created_at": "2026-02-06T14:30:00"
}
```

---

### 예시 4: 선택 필드 처리

**YAML:**
```yaml
payment-response:
  response:
    fields:
      - name: paymentId
        required: true
      - name: failureReason
        required: false
```

**Java:**
```java
@Data
public class PaymentResponse {
    private String paymentId;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String failureReason;  // 성공 시 null → JSON에서 제외
}
```

---

## 전역 설정 (application.yml)

Jackson의 기본 동작을 프로젝트 전체에 적용할 수 있습니다.

```yaml
spring:
  jackson:
    # snake_case 전역 적용
    property-naming-strategy: SNAKE_CASE

    # null 값 제외
    default-property-inclusion: non_null

    # 날짜를 ISO-8601 형식으로
    serialization:
      write-dates-as-timestamps: false

    # 알 수 없는 필드 무시
    deserialization:
      fail-on-unknown-properties: false
```

---

## 주의사항

### 1. @JsonProperty vs YAML 불일치

```yaml
# YAML
- name: user_id
```

```java
// ❌ 잘못된 매핑
@JsonProperty("userId")  // YAML과 다름!
private String userId;
```

**결과**: 프론트엔드가 `user_id`를 찾지 못함

### 2. @JsonNaming과 @JsonProperty 충돌

```java
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Response {
    @JsonProperty("customName")  // 이게 우선순위 높음
    private String fieldName;    // snake_case 무시됨
}
```

### 3. Request와 Response 다른 이름

```java
@Data
public class UserDto {
    // 요청: user_id, 응답: userId로 하고 싶다면
    @JsonAlias("user_id")        // 요청 허용
    @JsonProperty("userId")      // 응답 이름
    private String userId;
}
```

---

## 참고 자료

- [Jackson Annotations Guide](https://github.com/FasterXML/jackson-annotations)
- [Spring Boot Jackson Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto.spring-mvc.customize-jackson-objectmapper)