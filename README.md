# 🏆 Kế Hoạch Phát Triển: Online Bidding System (Java)

Hệ thống đấu giá trực tuyến đầy đủ tính năng, xây dựng bằng **Java + JavaFX + Socket**, áp dụng chặt chẽ OOP, Design Patterns và kiến trúc Client-Server.

---

## 🗺️ Tổng Quan Lộ Trình (5 Giai đoạn)

| Giai đoạn | Tên | Nội dung chính | Trạng thái |
|-----------|-----|----------------|------------|
| **1** | Core Entities & OOP | Class hierarchy: User, Item, Auction + OOP 4 tính chất | 🔲 Chờ |
| **2** | Server & Database Logic | Socket Server, DAO layer, quản lý phiên đấu giá | 🔲 Chờ |
| **3** | Realtime Bidding | Thread-safe bidding, Observer Pattern, broadcast | 🔲 Chờ |
| **4** | Advanced Features | Auto-Bidding, Anti-sniping, Price Curve Chart | 🔲 Chờ |
| **5** | Polish & Testing | JUnit, Exception Handling, Google Java Style | 🔲 Chờ |

---

## 📐 Kiến Trúc Tổng Thể

```
online-auction/
├── auction-common/          # Shared models & DTOs (dùng chung Client + Server)
│   └── src/main/java/com/auction/common/
│       ├── model/           # Entity classes
│       ├── dto/             # Data Transfer Objects (JSON)
│       └── util/            # JsonUtil, Constants
│
├── auction-server/          # Backend Server
│   └── src/main/java/com/auction/server/
│       ├── ServerMain.java
│       ├── network/         # ClientHandler (Socket threads)
│       ├── controller/      # Request routing logic
│       ├── service/         # Business logic (BiddingService, AuctionService...)
│       ├── dao/             # Data Access Object (in-memory / file)
│       └── observer/        # AuctionEventBroadcaster
│
└── auction-client/          # Frontend JavaFX
    └── src/main/java/com/auction/client/
        ├── ClientMain.java
        ├── network/         # ServerConnection (Socket)
        ├── controller/      # JavaFX Controllers (MVC)
        ├── view/            # FXML files
        └── model/           # Client-side state
```

> **Multi-module Maven project** — mỗi module là một dự án độc lập nhưng chia sẻ chung `auction-common`.

---

## 🎯 Giai Đoạn 1 — Chi Tiết: Core Entities & OOP

### Mục tiêu
Xây dựng **bộ xương (skeleton)** của toàn bộ hệ thống. Tất cả class ở đây đều nằm trong module `auction-common` vì Server lẫn Client đều cần chúng.

---

### 1.1 Cây Kế Thừa: `User`

```
               ┌──────────────┐
               │  User (ABC)  │   ← Abstract Class
               │ - id, name   │   ← Encapsulation (private fields)
               │ - email, pwd │
               │ + getRole()  │   ← Abstract Method → Abstraction
               └──────┬───────┘
          ┌───────────┼───────────┐
    ┌─────▼────┐ ┌────▼─────┐ ┌──▼──────┐
    │  Admin   │ │  Seller  │ │ Bidder  │
    │+manageAll│ │+listItem │ │+placeBid│
    └──────────┘ └──────────┘ └─────────┘
```

**4 tính chất OOP áp dụng:**
- **Đóng gói (Encapsulation):** `id`, `password` là `private`, truy cập qua `getter/setter`
- **Kế thừa (Inheritance):** `Admin`, `Seller`, `Bidder` kế thừa `User`
- **Đa hình (Polymorphism):** `getRole()` trả về kết quả khác nhau tùy subclass
- **Trừu tượng (Abstraction):** `User` là `abstract class`, không thể tạo `new User()`

**Design Pattern áp dụng:** `UserFactory` — tạo đúng loại User từ String role.

---

### 1.2 Cây Kế Thừa: `Item`

```
               ┌──────────────────┐
               │  Item (Abstract) │
               │ - id, name       │
               │ - basePrice      │
               │ + getCategoryFee()│ ← Abstract → Mỗi loại tính phí khác nhau
               └────────┬─────────┘
          ┌─────────────┼──────────────┐
   ┌──────▼──────┐ ┌────▼───┐ ┌───────▼──────┐
   │ Electronics │ │  Art   │ │   Vehicle    │
   │ + warranty  │ │+artist │ │ + mileage    │
   └─────────────┘ └────────┘ └──────────────┘
```

---

### 1.3 Class: `AuctionSession`

```
┌─────────────────────────────────────┐
│           AuctionSession            │
│ - id, item, seller                  │
│ - startTime, endTime                │
│ - currentPrice, currentWinner       │
│ - status: OPEN | RUNNING | FINISHED │
│ - List<Bid> bidHistory              │
└─────────────────────────────────────┘
```

---

### 1.4 Class: `Bid` (Một lượt đặt giá)

```
┌──────────────────────────┐
│           Bid            │
│ - bidId                  │
│ - bidder (Bidder ref)    │
│ - amount (double)        │
│ - timestamp              │
└──────────────────────────┘
```

---

### 1.5 Design Pattern: `UserFactory`

```java
// Thay vì:
User u = new Bidder(...); // Client biết quá nhiều chi tiết

// Dùng Factory:
User u = UserFactory.create("BIDDER", id, name, email, password);
```
Factory Pattern che giấu logic khởi tạo, giúp code dễ mở rộng khi thêm loại User mới.

---

### 1.6 Cấu Trúc File Sẽ Tạo (Giai đoạn 1)

#### Module: `auction-common`

| File | Mô tả |
|------|-------|
| `model/user/User.java` | Abstract base class |
| `model/user/Admin.java` | Subclass Admin |
| `model/user/Seller.java` | Subclass Seller |
| `model/user/Bidder.java` | Subclass Bidder |
| `model/user/UserRole.java` | Enum: ADMIN, SELLER, BIDDER |
| `model/item/Item.java` | Abstract base class |
| `model/item/Electronics.java` | Subclass |
| `model/item/Art.java` | Subclass |
| `model/item/Vehicle.java` | Subclass |
| `model/item/ItemCategory.java` | Enum: ELECTRONICS, ART, VEHICLE |
| `model/auction/AuctionSession.java` | Phiên đấu giá |
| `model/auction/AuctionStatus.java` | Enum: OPEN, RUNNING, FINISHED |
| `model/auction/Bid.java` | Một lượt đặt giá |
| `factory/UserFactory.java` | Factory Pattern tạo User |
| `factory/ItemFactory.java` | Factory Pattern tạo Item |
| `pom.xml` | Maven config |

**Tổng cộng: ~14 files Java + Maven setup**

---

## ✅ Tiêu Chí Hoàn Thành Giai Đoạn 1

- [ ] Maven multi-module project khởi tạo thành công (`mvn compile` không lỗi)
- [ ] Tất cả class có Javadoc đầy đủ
- [ ] Abstract method `getRole()` và `getCategoryFee()` hoạt động đúng với polymorphism
- [ ] `UserFactory` và `ItemFactory` test được bằng main()
- [ ] Code tuân thủ Google Java Style Guide (camelCase, indentation 2 spaces)

---

## 📅 Kế Hoạch Thực Thi

Sau khi bạn approve, tôi sẽ:
1. Tạo cấu trúc Maven multi-module
2. Viết từng class có giải thích chi tiết bằng tiếng Việt
3. Demo Factory Pattern với main() test
4. Sau Giai đoạn 1 xong → chuyển sang Giai đoạn 2
