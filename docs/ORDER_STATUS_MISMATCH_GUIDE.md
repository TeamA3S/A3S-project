# 주문 상태명을 변경하면 결제하기 버튼이 안 보이는 문제

## 상황 설명

주문 목록에서 상태가 `PENDING`인 주문에만 "결제하기" 버튼이 표시됩니다.
백엔드에서 주문 상태를 다른 이름(예: `WAITING`, `대기중`, `READY`)으로 반환하면
프론트엔드의 하드코딩된 상태 비교가 매칭되지 않아 버튼이 나타나지 않습니다.

```
백엔드 응답: { status: "WAITING" }
프론트엔드:  order.status === 'PENDING'  → false ❌  → 버튼 안 보임
```

---

## 영향받는 코드 위치

### `src/main/resources/templates/orders.html`

이 파일에 상태 문자열이 하드코딩된 곳이 **3군데** 있습니다.

---

### 1) 결제하기 버튼 표시 조건 (가장 중요, 이 부분만 수정해도 테스트하는데 큰 문제 없습니다.)

```javascript
// renderOrderList() 함수 내부
${order.status === 'PENDING' ?
    `<button class="btn btn-primary" onclick="payOrder(${order.orderId}, ${finalAmount})">
        💳 결제하기
    </button>` :
    `<span>-</span>`
}
```

`'PENDING'`을 백엔드에서 사용하는 상태명으로 바꿔야 합니다.

```javascript
// 예: 상태명을 'WAITING'으로 바꾼 경우
${order.status === 'WAITING' ?
```

---

### 2) 상태 스타일 매핑

```javascript
// getStatusStyle() 함수
function getStatusStyle(status) {
    const styles = {
        'PENDING': 'background: #FEF3C7; color: #92400E;',
        'PAID': 'background: #D1FAE5; color: #065F46;',
        'CANCELLED': 'background: #FEE2E2; color: #991B1B;',
        'SHIPPED': 'background: #DBEAFE; color: #1E40AF;',
        'DELIVERED': 'background: #E0E7FF; color: #3730A3;'
    };
    return styles[status] || 'background: #F3F4F6; color: #374151;';
}
```

매칭되지 않는 상태명은 기본 회색 스타일(`#F3F4F6`)이 적용됩니다.
새 상태명을 추가하거나 기존 키를 변경해야 합니다.

```javascript
// 예: 상태명을 한글로 바꾼 경우
const styles = {
    '대기중': 'background: #FEF3C7; color: #92400E;',
    '주문완료': 'background: #D1FAE5; color: #065F46;',
    '취소됨': 'background: #FEE2E2; color: #991B1B;',
    // ...
};
```

---

### 3) 상태 텍스트 매핑

```javascript
// getStatusText() 함수
function getStatusText(status) {
    const texts = {
        'PENDING': '결제 대기',
        'PAID': '주문 완료',
        'CANCELLED': '취소됨',
        'SHIPPED': '배송중',
        'DELIVERED': '배송 완료'
    };
    return texts[status] || status;
}
```

매칭되지 않으면 상태값 원문이 그대로 표시됩니다 (예: `WAITING`).
새 상태명에 대한 한글 텍스트를 추가해야 합니다.

```javascript
// 예: 새로운 상태명 추가
const texts = {
    'WAITING': '결제 대기',      // 변경된 상태명
    'COMPLETED': '주문 완료',    // 변경된 상태명
    // ...
};
```

---

## 수정 방법

### 방법 A: 백엔드에서 기존 상태명 사용 (가장 쉬움)

프론트엔드와 약속된 상태명을 그대로 사용합니다.

```java
// OrderController.java
order.put("status", "PENDING");    // ✅ 프론트엔드와 일치
order.put("status", "PAID");       // ✅ 프론트엔드와 일치
```

### 방법 B: orders.html에서 상태명 수정

백엔드 상태명을 커스텀하고 싶다면 orders.html의 3곳을 모두 수정합니다.

**예시: `PENDING` → `READY_TO_PAY`로 변경하는 경우**

```javascript
// 1) 결제 버튼 조건
${order.status === 'READY_TO_PAY' ?

// 2) 스타일 매핑
'READY_TO_PAY': 'background: #FEF3C7; color: #92400E;',

// 3) 텍스트 매핑
'READY_TO_PAY': '결제 대기',
```

### 방법 C: enum 연동으로 근본적 해결 (직접 구현 필요, 권장 X)

상태값을 `client-api-config.yml`에서 관리하고, 프론트엔드가 이를 동적으로 읽도록 개선할 수 있습니다.

```yaml
# client-api-config.yml에 상태 정의 추가 (커스텀 확장)
order-statuses:
  payable: "READY_TO_PAY"    # 결제 가능 상태
  paid: "COMPLETED"           # 주문 완료
  cancelled: "CANCELLED"      # 취소
```

```javascript
// orders.html에서 동적으로 읽기
const config = await getConfig();
const payableStatus = config.api['order-statuses']?.payable || 'PENDING';

${order.status === payableStatus ?
    `<button>💳 결제하기</button>` : `<span>-</span>`
}
```

---

## 수정 체크리스트

백엔드에서 상태명을 변경했을 때 확인할 곳:

| 순서 | 파일 | 함수/위치 | 확인 사항 |
|------|------|-----------|-----------|
| 1 | `orders.html` | `renderOrderList()` | `order.status === '???'` 버튼 표시 조건 |
| 2 | `orders.html` | `getStatusStyle()` | 스타일 매핑 객체의 키 |
| 3 | `orders.html` | `getStatusText()` | 표시 텍스트 매핑 객체의 키 |
| 4 | `client-api-config.yml` | `list-orders > status` | description에 적힌 상태값 목록 업데이트 |

---

## 주의: 다른 페이지의 상태 비교도 확인

상태 문자열 비교는 주문 외에도 다른 곳에서 사용될 수 있습니다.
상태명을 변경할 때는 프로젝트 전체에서 해당 문자열을 검색하세요.

```
검색할 키워드 예시: 'PENDING', 'PAID', 'CANCELLED'
검색 대상: src/main/resources/templates/*.html
```
