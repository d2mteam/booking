# Booking vé "chuẩn lõi" (có thể chuyển thẳng thành app)

Tài liệu này mô tả **nghiệp vụ booking vé** ở mức có thể chuyển thẳng thành code, DB, DDD, state machine. Không màu mè, không phụ thuộc công nghệ.

---

## I. Phạm vi & Giả định

Áp dụng cho:

- Vé xem phim / sự kiện.
- Có **session/showtime**.
- Có **seat (ghế)** hoặc **slot (suất)**.
  - Nếu không có ghế thì **seat = slot** (logic như nhau).

Không bàn:

- Marketing, CMS.
- Recommendation.
- Affiliate.

---

## II. Use Case (chuẩn thực tế)

### UC-01: Browse Event
**Actor:** User

**Mục tiêu:** Xem danh sách sự kiện.

- Xem Event.
- Xem Session (ngày/giờ).
- Xem giá cơ bản.

**Ghi chú:**
- ⛔ Không lock, không giữ gì ở đây.

---

### UC-02: View Seat Map / Availability
**Actor:** User

**Mục tiêu:** Xem ghế trống.

**Trạng thái ghế:**
- AVAILABLE
- HELD
- SOLD

**Ghi chú:**
- ⚠️ Chỉ là read, không đảm bảo lúc click vẫn còn.

---

### UC-03: Reserve Seat (HOLD)
**Actor:** User

**Mục tiêu:** Giữ ghế tạm thời.

**Input:**
- userId
- sessionId
- seatIds[]

**Preconditions:**
- seat.status == AVAILABLE
- session.isOpen == true

**Process:**
- Chuyển seat → HELD
- Gán holdId
- Gán holdExpiresAt (VD: now + 5 phút)

**Postconditions:**
- Ghế bị lock cho user
- Người khác không chọn được

**Failure cases:**
- Ghế đã HELD
- Ghế đã SOLD
- Session closed

🔥 Đây là use case quan trọng nhất.

---

### UC-04: Release Hold (Expire / Cancel)
**Actor:** System / User

**Trigger:**
- Hết TTL
- User hủy

**Rule:**
- HELD → AVAILABLE
- Xóa holdId

**Ghi chú:**
- ⚠️ Phải chạy tự động, không phụ thuộc user.

---

### UC-05: Checkout / Create Payment
**Actor:** User

**Mục tiêu:** Thanh toán.

**Preconditions:**
- User đang giữ ghế (HELD)
- holdExpiresAt > now

**Process:**
- Tạo PaymentIntent
- Gắn paymentId với holdId

**Ghi chú:**
- ⚠️ Không confirm vé ở bước này.

---

### UC-06: Confirm Payment
**Actor:** Payment Gateway / System

**Success:**
- HELD → SOLD
- Phát hành Ticket
- Ticket.status = VALID

**Failure / Timeout:**
- Không đổi trạng thái ghế
- Chờ expire

🔥 Idempotent bắt buộc.

---

### UC-07: View Ticket
**Actor:** User

- Xem QR
- Xem trạng thái vé

---

### UC-08: Check-in / Validate Ticket
**Actor:** Staff / Scanner

**Preconditions:**
- Ticket.status == VALID
- Session chưa kết thúc

**Process:**
- VALID → USED

---

### UC-09: Cancel / Refund (Optional)
**Actor:** User / Admin

**Rule phụ thuộc business:**
- Trước session X giờ
- Vé chưa USED

---

## III. Business Rules (bắt buộc)

### Rule 1 – Anti Oversell
- 1 seat + 1 session = chỉ 1 trạng thái active tại 1 thời điểm.
- Không có ngoại lệ.

### Rule 2 – Hold phải có TTL
- Không TTL → chết hệ thống.
- TTL phổ biến: 3–5 phút.

### Rule 3 – Payment không được tạo vé trực tiếp
- Payment chỉ confirm, không sinh ticket nếu:
  - Không có hold.
  - Hold đã expire.

### Rule 4 – Idempotency
- Payment callback có thể đến n lần.
- Mỗi paymentId → chỉ xử lý 1 lần.

### Rule 5 – Check-in là bất biến
- Ticket USED → không quay lại VALID.

---

## IV. State Machine (trung tâm hệ thống)

### 1) Seat State Machine
```
AVAILABLE
   |
   | reserve()
   v
HELD
   | payment_success
   v
SOLD

HELD
   | hold_expired / cancel
   v
AVAILABLE
```

⛔ Không có đường SOLD → AVAILABLE.

---

### 2) Reservation / Hold
```
CREATED
   |
   v
ACTIVE
   |
   +-- expired --> EXPIRED
   |
   +-- payment_success --> CONSUMED
```

---

### 3) Ticket
```
CREATED
   |
   v
VALID
   |
   v
USED
```

---

### 4) Payment
```
INIT
  |
  v
PENDING
  |
  +-- SUCCESS
  |
  +-- FAILED
  |
  +-- TIMEOUT
```

---

## V. Domain Model (DDD có thể code ngay)

### Aggregate
- Event
- Session
- Seat
- Reservation (Hold)
- Ticket
- Payment

### Commands
- ReserveSeat
- ReleaseHold
- CreatePayment
- ConfirmPayment
- CheckInTicket

### Domain Events
- SeatHeld
- HoldExpired
- PaymentSucceeded
- TicketIssued
- TicketUsed

---

## VI. Minimum DB Schema (monolith)

### Tables

#### events
- id (PK)
- title
- status
- created_at

#### sessions
- id (PK)
- event_id (FK → events.id)
- starts_at
- ends_at
- is_open
- base_price

#### seats
- id (PK)
- session_id (FK → sessions.id)
- seat_no
- status (AVAILABLE / HELD / SOLD)
- hold_id (nullable)
- hold_expires_at (nullable)

**Index/Constraint:**
- UNIQUE (session_id, seat_no)
- INDEX (session_id, status)

#### reservations (holds)
- id (PK, hold_id)
- user_id
- session_id
- status (CREATED / ACTIVE / EXPIRED / CONSUMED)
- expires_at
- created_at

#### tickets
- id (PK)
- reservation_id (FK)
- session_id (FK)
- user_id
- status (CREATED / VALID / USED)
- qr_code
- issued_at

#### payments
- id (PK, payment_id)
- reservation_id (FK)
- status (INIT / PENDING / SUCCESS / FAILED / TIMEOUT)
- amount
- currency
- provider
- created_at

**DB Rule (Anti-oversell):**
- UNIQUE (session_id, seat_no) + row lock khi cập nhật.
- SELECT ... FOR UPDATE theo seat để ngăn race.

---

## VII. Core Workflows (Command Handling)

### ReserveSeat
1. Load Session + Seats FOR UPDATE.
2. Validate session.is_open.
3. Validate tất cả seats == AVAILABLE.
4. Create Reservation (ACTIVE, expires_at = now + TTL).
5. Update seats → HELD + hold_id + hold_expires_at.
6. Emit SeatHeld.

### ReleaseHold (Expire/Cancel)
1. Load Reservation (ACTIVE).
2. Set reservation → EXPIRED.
3. Update seats by hold_id → AVAILABLE, clear hold fields.
4. Emit HoldExpired.

### CreatePayment
1. Validate Reservation ACTIVE & not expired.
2. Create Payment (PENDING) & link reservation_id.

### ConfirmPayment (Idempotent)
1. If payment_id already SUCCESS → return (idempotent).
2. Validate Reservation ACTIVE & not expired.
3. Update seats → SOLD.
4. Update Reservation → CONSUMED.
5. Create Ticket (VALID).
6. Update Payment → SUCCESS.
7. Emit PaymentSucceeded + TicketIssued.

### CheckInTicket
1. Load ticket.
2. Validate status VALID & session not ended.
3. Update ticket → USED.
4. Emit TicketUsed.

---

## VIII. Hold TTL (không dùng Redis)

**Concept:** dùng DelayQueue hoặc cron/worker để expire hold.

Pseudo:
```java
class Hold implements Delayed {
    long expiresAt;
    // ...
}

// Worker thread
while (true) {
    Hold h = delayQueue.take();
    releaseHold(h.holdId);
}
```

✅ Chính xác, hiểu rõ timeout thật.
❌ Không survive restart (cần quét DB khi boot).

---

## IX. Gợi ý kỹ thuật triển khai (không bắt buộc)

- Runtime: Java 21+.
- Virtual threads cho IO blocking (JDBC, payment API).
- DB: PostgreSQL, READ COMMITTED.
- Lock: SELECT ... FOR UPDATE theo seat rows.

---

## X. Checklist bắt buộc khi code

- [ ] ReserveSeat atomic trong 1 transaction.
- [ ] Hold có TTL và auto-expire.
- [ ] ConfirmPayment idempotent.
- [ ] Ticket USED không rollback.
- [ ] Không có đường SOLD → AVAILABLE.
- [ ] Không tạo ticket nếu hold expired/không tồn tại.
