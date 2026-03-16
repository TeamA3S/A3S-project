# ID를 number 타입으로 변경했을 때 상점 +/- 버튼이 안 되는 문제

## 상황 설명

`client-api-config.yml`에서 상품 ID나 주문 ID의 타입을 `string` → `number`로 변경하고
백엔드도 숫자로 반환하면, 상점 페이지에서 **+ / - 버튼이 아무 반응이 없는** 현상이 발생합니다.

---

## 원인: JavaScript `===` (Strict Equality) 타입 불일치

### 문제 흐름

```
1. 백엔드 응답: { id: 1, name: "스프링 부트", price: 35000 }
                     ↑ number 타입

2. HTML 렌더링:
   onclick="updateQuantity('1', 1)"
                           ↑ 따옴표로 감싸져서 string '1'이 됨

3. 함수 내부:
   products.find(p => p.id === productId)
                      1    === '1'        → false ❌
                      ↑number  ↑string
```

`===`는 **값과 타입이 모두 같아야** `true`입니다.
숫자 `1`과 문자열 `'1'`은 타입이 다르므로 항상 `false`가 되어 상품을 찾지 못합니다.

---

## 수정해야 할 파일과 위치

### 파일 1: `src/main/resources/templates/shop.html`

#### 수정 1) onclick 핸들러에서 따옴표 제거

```javascript
// ❌ 수정 전: 따옴표가 ID를 문자열로 만듦
onclick="updateQuantity('${product.id}', -1)"
onclick="updateQuantity('${product.id}', 1)"

// ✅ 수정 후: 따옴표 제거하여 숫자 그대로 전달
onclick="updateQuantity(${product.id}, -1)"
onclick="updateQuantity(${product.id}, 1)"
```

#### 수정 2) `updateQuantity()` 함수의 `===` → `==`

```javascript
// ❌ 수정 전: strict equality
const product = products.find(p => p.id === productId);

// ✅ 수정 후: loose equality (타입 자동 변환)
const product = products.find(p => p.id == productId);
```

> `==`는 비교 시 타입을 자동 변환하므로 `1 == '1'` → `true`가 됩니다.
> 이 경우 `Object.keys(cart)`가 항상 문자열을 반환하기 때문에 `==`가 더 안전합니다.

#### 수정 3) `updateCartSummary()` 함수의 `===` → `==`

```javascript
// ❌ 수정 전
const product = products.find(p => p.id === productId);

// ✅ 수정 후
const product = products.find(p => p.id == productId);
```

> `Object.keys(cart)`는 JavaScript 특성상 **항상 문자열 키**를 반환합니다.
> 따라서 cart에서 꺼낸 productId는 `'1'` (string)이고, products 배열의 id는 `1` (number)이므로
> `==`를 써야 매칭됩니다.

---

### 파일 2: `src/main/resources/templates/orders.html`

#### 수정 1) 결제하기 버튼 onclick에서 따옴표 제거

```javascript
// ❌ 수정 전
onclick="payOrder('${order.orderId}', ${finalAmount})"

// ✅ 수정 후
onclick="payOrder(${order.orderId}, ${finalAmount})"
```

#### 수정 2) `payOrder()` 함수의 `===` → `==`

```javascript
// ❌ 수정 전
const order = orders.find(o => o.orderId === orderId);

// ✅ 수정 후
const order = orders.find(o => o.orderId == orderId);
```

---

## 왜 이런 문제가 생기는가?

HTML의 `onclick` 속성은 **문자열**입니다. 템플릿 리터럴 안에서 값을 따옴표로 감싸면:

```javascript
// 템플릿 리터럴
`onclick="updateQuantity('${product.id}', 1)"`

// product.id가 1(number)일 때 렌더링 결과:
onclick="updateQuantity('1', 1)"
//                       ↑ string
```

따옴표를 제거하면:

```javascript
`onclick="updateQuantity(${product.id}, 1)"`

// 렌더링 결과:
onclick="updateQuantity(1, 1)"
//                      ↑ number
```

---

## 추가: JavaScript Object 키는 항상 문자열

```javascript
const cart = {};
cart[1] = 3;           // 숫자 1로 넣어도
Object.keys(cart);     // → ['1']  (문자열!)
```

이 때문에 `updateCartSummary()`에서 `Object.keys(cart)`로 꺼낸 키는 항상 `string`입니다.
`products.find(p => p.id === productId)`에서 `===`를 쓰면 매칭이 안 됩니다.

---

## 수정 요약 체크리스트

| 파일 | 위치 | 수정 내용 |
|------|------|-----------|
| `shop.html` | `onclick="updateQuantity(...)"` (2곳) | `'${product.id}'` → `${product.id}` |
| `shop.html` | `updateQuantity()` 함수 | `===` → `==` |
| `shop.html` | `updateCartSummary()` 함수 | `===` → `==` |
| `orders.html` | `onclick="payOrder(...)"` | `'${order.orderId}'` → `${order.orderId}` |
| `orders.html` | `payOrder()` 함수 | `===` → `==` |
