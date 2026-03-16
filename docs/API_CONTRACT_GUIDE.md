## YAML + 백엔드 수정 시 프론트엔드 영향

### ✅ 프론트엔드 코드 수정 **불필요**

#### 1. **필드 타입 변경**
```yaml
# YAML 수정
- name: id
  type: string  → type: number
```
```java
// 백엔드 수정
private String id;  → private Long id;
```
```javascript
// 프론트엔드: 수정 불필요!
const id = product.id;  // 자동으로 number로 처리
```

#### 2. **HTTP 메서드 변경**
```yaml
# YAML 수정
method: GET  → method: POST
```
```java
// 백엔드 수정
@GetMapping  → @PostMapping
```
```javascript
// 프론트엔드: 수정 불필요!
makeApiRequest('list-products');  // YAML에서 자동으로 메서드 읽음
```

#### 3. **URL 변경**
```yaml
# YAML 수정
url: /api/products  → url: /api/v2/products
```
```java
// 백엔드 수정
@RequestMapping("/api/products")  → @RequestMapping("/api/v2/products")
```
```javascript
// 프론트엔드: 수정 불필요!
makeApiRequest('list-products');  // YAML에서 URL 읽음
```

#### 4. **경로 파라미터 이름 변경**
```yaml
# YAML 수정
url: /api/payments/{paymentId}/confirm
  → url: /api/payments/{id}/confirm
```
```java
// 백엔드 수정
@PathVariable String paymentId  → @PathVariable String id
```
```javascript
// 프론트엔드: 수정 불필요!
makeApiRequest('confirm-payment', {
    pathParams: paymentId  // 단일값 방식이면 이름 무관
});
```

#### 5. **선택 필드 추가**
```yaml
# YAML에 새 필드 추가
- name: description
  type: string
  required: false  # 선택 필드
```
```java
// 백엔드에 필드 추가
private String description;
```
```javascript
// 프론트엔드: 수정 불필요!
// 사용하려면 추가, 안 쓰면 무시
```

---

### ❌ 프론트엔드 코드 수정 **필수**

#### 1. **필드 이름 변경**
```yaml
# YAML 수정
- name: id  → name: productId
```
```java
// 백엔드 수정
private Long id;  → private Long productId;
```
```javascript
// 프론트엔드: 수정 필수!
const id = product.id;  // undefined!
  ↓
const id = product.productId;  // 수정 필요
```

#### 2. **응답 구조 변경 (flat ↔ nested)**
```yaml
# YAML 수정
fields:
  - name: amount
    type: number
  ↓
fields:
  - name: amount
    type: object
    fields:
      - name: total
      - name: currency
```
```javascript
// 프론트엔드: 수정 필수!
const amount = payment.amount;
  ↓
const amount = payment.amount.total;  // 수정 필요
```

#### 3. **배열 ↔ 객체 변경**
```yaml
# YAML 수정
type: array
  ↓
type: object
```
```javascript
// 프론트엔드: 수정 필수!
items.forEach(item => ...)
  ↓
Object.values(items).forEach(item => ...)  // 수정 필요
```

#### 4. **필수 필드 제거 (프론트엔드가 사용 중인 경우)**
```yaml
# YAML에서 필드 제거
- name: orderNumber  # 삭제!
```
```javascript
// 프론트엔드: 수정 필수!
const orderNumber = order.orderNumber;  // undefined!
// 이 코드를 제거하거나 대체 로직 필요
```

#### 5. **응답 키 변경 (body → data 등)**
```yaml
# YAML 수정
response:
  body: {...}
  ↓
response:
  data: {...}
```
```javascript
// 프론트엔드: 수정 필수!
// api-handler.js나 응답 처리 로직 수정 필요
```

---

## 요약표

| 변경 사항 | YAML 수정 | 백엔드 수정 | 프론트엔드 코드 수정 |
|----------|----------|-----------|------------------|
| **타입 변경** (string↔number) | ✅ | ✅ | ❌ 불필요 |
| **메서드 변경** (GET↔POST) | ✅ | ✅ | ❌ 불필요 |
| **URL 변경** | ✅ | ✅ | ❌ 불필요 |
| **경로 파라미터 이름** | ✅ | ✅ | ❌ 불필요 (단일값 방식 시) |
| **선택 필드 추가** | ✅ | ✅ | ❌ 불필요 |
| **필드 이름 변경** | ✅ | ✅ | ✅ **필수** |
| **응답 구조 변경** | ✅ | ✅ | ✅ **필수** |
| **배열↔객체 변경** | ✅ | ✅ | ✅ **필수** |
| **사용 중 필드 제거** | ✅ | ✅ | ✅ **필수** |

---

## 핵심 원칙

**자동 적응 (메타데이터):**
- 타입, 메서드, URL, 검증 규칙 등 → YAML에서 읽어서 자동 처리

**수동 수정 필요 (데이터 구조):**
- 필드 이름, 객체 구조, 배열/객체 변환 등 → 실제 데이터 접근 코드 수정

**기억할 것:**
> "프론트엔드가 `response.fieldName` 같은 코드로 **직접 접근하는 부분**만 수정 필요!"
