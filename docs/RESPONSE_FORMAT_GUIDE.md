# 공통 응답 DTO로 감쌌을 때 클라이언트가 데이터를 못 읽는 문제

## 상황 설명

백엔드에서 공통 응답 래퍼 DTO를 만들어 사용하면 클라이언트가 데이터를 인식하지 못합니다.

```java
// 수강생이 만든 공통 응답 DTO
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;        // ← 실제 데이터가 여기에 들어감
}
```

```java
// Controller에서 이렇게 감싸서 반환
@GetMapping
public ApiResponse<List<Product>> listProducts() {
    List<Product> products = ...;
    return ApiResponse.success(products);
}
```

이렇게 하면 실제 HTTP 응답이 이렇게 됩니다:

```
클라이언트가 기대하는 응답:
[
  { "id": 1, "name": "스프링 부트", "price": 35000, "stock": 50 },
  { "id": 2, "name": "JPA 마스터", "price": 42000, "stock": 30 }
]

실제 응답 (래퍼로 감싸진):
{
  "success": true,
  "message": "OK",
  "data": [
    { "id": 1, "name": "스프링 부트", "price": 35000, "stock": 50 },
    { "id": 2, "name": "JPA 마스터", "price": 42000, "stock": 30 }
  ]
}
```

---

## 어디서 어떻게 깨지는가?

### 1단계: `api-handler.js` → 래퍼 객체를 그대로 반환

```
파일: src/main/resources/static/js/api-handler.js (97행)
```

```javascript
const text = await response.text();
const data = text ? JSON.parse(text) : { ... };
// ...
return data;  // ← { success: true, data: [...] } 래퍼 객체 그대로 반환
```

`makeApiRequest()`는 HTTP 응답 본문을 JSON 파싱해서 그대로 돌려줍니다.
래퍼를 벗기는 로직이 없으므로 `{ success, message, data }` 객체가 그대로 넘어갑니다.

### 2단계: `api-validator.js` → "배열이어야 하지만 object 타입" 오류

```
파일: src/main/resources/static/js/api-validator.js (31~33행)
```

```javascript
// YML에 type: array로 정의되어 있으면
if (bodySchema.type === 'array') {
    if (!Array.isArray(response)) {
        // response = { success: true, data: [...] }
        // Array.isArray({ success: true, data: [...] }) → false ❌
        errors.push(`응답이 배열이어야 하지만 ${typeof response} 타입입니다.`);
    }
}
```

YML 스펙에 `type: array`라고 정의했는데, 실제 받은 건 래퍼 **객체**이므로 검증 실패합니다.

### 3단계: 각 페이지에서 데이터 사용 실패

**상점 페이지 (`shop.html` 132~138행):**

```javascript
const result = await makeApiRequest('list-products', {});
// result = { success: true, data: [...] }

if (result && Array.isArray(result)) {  // ← false! 래퍼 객체는 배열이 아님
    products = result;
} else {
    showNotification('상품 목록이 배열 형식이 아닙니다', 'error');  // ← 이 에러 발생
}
```

**주문 페이지 (`orders.html` 244~247행):**

```javascript
const result = await makeApiRequest('list-orders', {});
// result = { success: true, data: [...] }

orders = result;              // orders = 래퍼 객체
if (orders.length === 0) {    // undefined === 0 → false (객체에 length 없음)
    // ...
}
// renderOrderList()에서 orders.forEach() 호출 시 에러 발생
```

**주문 생성 (`shop.html` 321행):**

```javascript
const result = await makeApiRequest('create-order', { body: orderData });
// result = { success: true, data: { orderId: 1003, totalAmount: 89000 } }

if (result.orderId) {   // ← undefined! orderId는 result.data.orderId에 있음
    // 주문 페이지로 이동하는 코드가 실행되지 않음
}
```

---

## 해결 방법: 3가지 중 택 1

### 방법 A: 래퍼 DTO를 사용하지 않기 (가장 쉬움)

Controller에서 데이터를 직접 반환합니다.

```java
// ✅ 래퍼 없이 직접 반환 → 클라이언트 수정 불필요
@GetMapping
public List<Product> listProducts() {
    return productService.findAll();
}

@PostMapping
public Map<String, Object> createOrder(...) {
    Map<String, Object> response = new HashMap<>();
    response.put("orderId", order.getId());
    response.put("totalAmount", order.getTotalAmount());
    return response;
}
```

### 방법 B: `api-handler.js`에서 래퍼를 벗기기 (권장 — 한 곳만 수정)

`makeApiRequest()` 함수에서 응답을 반환하기 전에 `data` 필드를 꺼내도록 수정합니다.
**이 한 곳만 수정하면 모든 페이지가 정상 동작합니다.**

```
파일: src/main/resources/static/js/api-handler.js
수정 위치: return data; (97행 부근)
```

```javascript
// ❌ 수정 전
return data;

// ✅ 수정 후: 래퍼 객체면 data 필드를 꺼내고, success도 함께 넘겨주기
if (data && data.data !== undefined) {
    const unwrapped = data.data;

    // 객체 응답이면 래퍼의 success를 병합 (배열이면 건너뜀)
    if (unwrapped && typeof unwrapped === 'object' && !Array.isArray(unwrapped)) {
        if (unwrapped.success === undefined && data.success !== undefined) {
            unwrapped.success = data.success;
        }
    }

    return unwrapped;
}
return data;
```

이 코드가 하는 일:

```
1) 배열 응답 (list-products, list-orders 등):
   { success: true, data: [{...}, {...}] }
   → [{...}, {...}]                          배열이므로 success 병합 안 함 (필요 없음)

2) 객체 응답 + 비즈니스 DTO에 success 없음 (create-order 등):
   { success: true, data: { orderId: 1003, totalAmount: 89000 } }
   → { success: true, orderId: 1003, totalAmount: 89000 }
     ↑ 래퍼의 success가 자동 병합됨 (있어도 해가 없음)

3) 객체 응답 + 비즈니스 DTO에 success 있음 (create-payment 등):
   { success: true, data: { success: true, paymentId: "PAY-123" } }
   → { success: true, paymentId: "PAY-123" }
     ↑ 이미 있으므로 덮어쓰지 않음 (비즈니스 값 유지)
```

**이 방식의 장점**: 백엔드에서 비즈니스 DTO에 `success`를 넣든 안 넣든 상관없이 동작합니다.

> **`success`가 YML 스펙에 정의된 API 목록** (총 7개):
> `login`, `register`, `create-payment`, `confirm-payment`, `cancel-payment`,
> `update-subscription`, `create-billing`
>
> 위 API들은 래퍼를 쓸 때 비즈니스 DTO에 `success`를 안 넣어도
> 언래핑 시 래퍼의 `success`가 자동으로 채워지므로 검증을 통과합니다.
> 비즈니스 DTO에 `success`를 넣어도 상관없습니다 — 이미 있으면 덮어쓰지 않습니다.

---

### 방법 C: 각 페이지에서 개별 언래핑 (비추천 — 수정 범위 넓음)

각 HTML 페이지에서 `result.data`로 접근하도록 수정합니다.
수정해야 할 곳이 많아 비추천하지만, 참고로 나열합니다.

**shop.html (`loadProductsFromApi` 함수):**

```javascript
// 수정 전
if (result && Array.isArray(result)) {
    products = result;

// 수정 후
const productData = result.data || result;  // 래퍼면 .data, 아니면 그대로
if (productData && Array.isArray(productData)) {
    products = productData;
```

**orders.html (`loadOrders` 함수):**

```javascript
// 수정 전
orders = result;

// 수정 후
orders = result.data || result;  // 래퍼면 .data, 아니면 그대로
```

**shop.html (`createOrder` 함수):**

```javascript
// 수정 전
if (result.orderId) {

// 수정 후
const orderResult = result.data || result;
if (orderResult.orderId) {
```

> 방법 C로 하면 모든 `makeApiRequest()` 호출 지점을 찾아서 수정해야 하므로
> **방법 B (api-handler.js 한 곳 수정)** 를 추천합니다.

---

## 주의: `api-validator.js` 검증도 함께 고려

방법 B를 적용하면 `api-validator.js`에 넘어가는 `response`도 언래핑된 상태가 되므로
검증은 정상 동작합니다. 하지만 방법 C를 택한 경우, `validateApiResponse()` 호출 전에
언래핑을 해야 합니다:

```javascript
// 방법 C 사용 시, 검증도 언래핑 후 수행
const rawResult = await makeApiRequest('list-products', {});
const result = rawResult.data || rawResult;

if (!validateApiResponse('list-products', result)) {  // ← 언래핑된 데이터로 검증
    return false;
}
```

---

## 수정 파일 요약

| 방법 | 수정 파일 | 수정 범위 |
|------|-----------|-----------|
| **A** 래퍼 제거 | Controller Java 파일들 | 백엔드만 수정 |
| **B** api-handler.js 수정 (권장) | `static/js/api-handler.js` 1곳 | 프론트 1곳만 수정 |
| **C** 각 페이지 수정 | `shop.html`, `orders.html` 등 모든 페이지 | 프론트 여러 곳 수정 |

---

## 요약

1. 공통 응답 DTO(`ApiResponse<T>`)를 쓰면 `{ success, data: [...] }` 형태가 됨
2. 클라이언트는 **배열이나 객체를 직접** 받을 것을 기대하므로 래퍼를 인식 못함
3. 가장 깔끔한 해결: **`api-handler.js`의 `return data` 부분에서 `data.data`를 꺼내면서 `success`도 병합**
4. 이렇게 하면 비즈니스 DTO에 `success`를 넣든 안 넣든 클라이언트가 정상 동작
5. F12 Console에서 "응답이 배열이어야 하지만 object 타입입니다" 메시지가 보이면 이 문제임
