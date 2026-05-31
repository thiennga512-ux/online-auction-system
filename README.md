# 🌟 Nền Tảng Đấu Giá Trực Tuyến Hiện Đại (Online Auction System)

[![Java CI with Maven](https://github.com/thiennga512-ux/online-auction-system/actions/workflows/maven.yml/badge.svg)](https://github.com/thiennga512-ux/online-auction-system/actions)


Nền tảng đấu giá trực tuyến được phát triển trên kiến trúc **Client-Server** sử dụng ngôn ngữ **Java**, giao diện **JavaFX** và truyền tải dữ liệu thời gian thực qua **TCP Sockets** định dạng JSON (Gson). Hệ thống được thiết kế theo mô hình đa module (multi-module) quản lý bởi Maven, tuân thủ mô hình kiến trúc MVC, kiểm thử tự động và áp dụng các cơ chế đấu giá tiên tiến như tự động thầu (Auto-Bidding) và chống bắn tỉa thầu (Anti-Sniping).

---

## 📖 1. Mô tả bài toán & Phạm vi hệ thống

### Bài toán đặt ra
Trong môi trường thương mại điện tử hiện nay, việc tổ chức đấu giá trực tuyến đòi hỏi tính minh bạch, phản hồi thời gian thực tức thì và độ bảo mật cao về mặt giao dịch/tài chính. Hệ thống giải quyết triệt để bài toán:
1. Đảm bảo luồng thầu cập nhật liên tục tới hàng ngàn client cùng lúc mà không gây nghẽn (Realtime update).
2. Cam kết tài chính bằng cơ chế tự động ký quỹ (đóng băng số dư) khi đặt thầu để tránh hiện tượng thầu ảo.
3. Ngăn chặn việc bắn tỉa thầu (Anti-Sniping) ở những giây cuối cùng nhằm tạo môi trường cạnh tranh lành mạnh.
4. Đảm bảo an toàn dữ liệu khi có hiện tượng tranh chấp đặt thầu đồng thời (Race Condition) tại cùng một mili-giây.

### Phạm vi hệ thống
* **Mô hình Client-Server**: Kết nối Socket TCP trực tiếp giữ luồng thông tin hai chiều liên tục, áp dụng mẫu thiết kế **Observer Pattern** để broadcast trạng thái giá mới cho tất cả các Client đang kết nối.
* **Ba nhóm đối tượng chính**:
    * **Bidder (Người mua)**: Duyệt xem phiên đấu giá, nạp tiền ký quỹ, đặt thầu thủ công, cấu hình thầu tự động, nhận thông báo thời gian thực.
    * **Seller (Người bán)**: Đăng ký nâng cấp từ Bidder, quản lý danh mục sản phẩm (CRUD), đăng mặt hàng đấu giá phong phú, quản lý phiên đấu giá cá nhân.
    * **Admin (Quản trị viên)**: Phê duyệt/từ chối phiên đấu giá mới, xử lý đơn nạp tiền của người dùng, quản lý trạng thái hoạt động của tài khoản (khóa/mở khóa).
* **Môi trường mạng**: Chạy trên mạng cục bộ hoặc Internet qua cổng TCP `8080`.

---

## 🛠️ 2. Công nghệ sử dụng, môi trường chạy & Yêu cầu cài đặt

### Công nghệ & Thư viện sử dụng
* **Backend**: Java SE 21 Core, Multi-threading (ThreadPoolExecutor), JDBC API.
* **Frontend**: JavaFX 21, FXML, CSS phong cách hiện đại.
* **Database**: MySQL Server (v8.x).
* **Logging Framework**: SLF4J & Logback (Ghi vết lỗi hệ thống chuyên nghiệp, thay thế cho System.out.println).
* **Định dạng truyền dẫn**: JSON (Google Gson 2.11.0).
* **Kiểm thử tự động**: JUnit 5 & Plugin đo độ bao phủ mã nguồn **JaCoCo** (Cấu hình nghiêm ngặt: build fail nếu test coverage < 80%).
* **Quản lý dự án & CI/CD**: Maven 3.8+, GitHub Actions Workflow.

### Môi trường chạy (Runtime)
* **Hệ điều hành**: Windows 10/11, macOS hoặc Linux.
* **Java Runtime**: JRE/JDK 21 trở lên.

### Yêu cầu cài đặt
1. **Cài đặt JDK 21**: Tải và cấu hình biến môi trường `JAVA_HOME`.
2. **Cài đặt Maven**: Tải và cấu hình biến môi trường `MAVEN_HOME`.
3. **Cài đặt MySQL Server**:
    * Cổng mặc định: `3306`
    * Tài khoản kết nối mặc định: User `root` / Password `Phu1234@@`
    * *(Lưu ý: Hệ thống sẽ tự động tạo cơ sở dữ liệu `auction_db` và các bảng dữ liệu liên quan ở lần khởi chạy đầu tiên).*

### Các biến môi trường tùy chọn (Environment Variables)
Bạn có thể cấu hình các biến sau để thay đổi thông tin kết nối DB (nếu khác mặc định):
* `AUCTION_DB_HOST` (Mặc định: `localhost`)
* `AUCTION_DB_PORT` (Mặc định: `3306`)
* `AUCTION_DB_NAME` (Mặc định: `auction_db`)
* `AUCTION_DB_USER` (Mặc định: `root`)
* `AUCTION_DB_PASSWORD` (Mặc định: `Phu1234@@`)

---

## 📂 3. Cấu trúc thư mục & các Module chính

Dự án được thiết kế theo cấu trúc đa module của Maven nhằm tách biệt nghiệp vụ và tối ưu hóa việc tái sử dụng mã nguồn theo mô hình kiến trúc **MVC**:

```text
online-auction-system/
├── .github/workflows/
│   └── maven.yml                  # Cấu hình CI/CD tự động chạy kiểm thử trên GitHub
├── README.md                      # Tài liệu hướng dẫn sử dụng hệ thống
└── auction-project/
    ├── pom.xml                    # Maven Parent POM quản lý dependencies chung
    ├── shared/                    # Module dùng chung (Shared Module)
    │   ├── pom.xml
    │   └── src/main/java/com/auction/
    │       ├── dto/               # Dữ liệu truyền tải giữa Client và Server (Data Transfer Objects)
    │       ├── enums/             # Các kiểu liệt kê (Role, AuctionStatus, ItemCategory,...)
    │       ├── factory/           # [Design Pattern] Factory Pattern tạo sản phẩm (Art, Vehicle, Electronics)
    │       ├── model/             # Các thực thể cốt lõi (User, Bidder, Seller, Item,...)
    │       └── network/           # Định nghĩa cấu trúc Request và Response JSON
    ├── server/                    # Module Máy chủ (Server Module - Model & DAO)
    │   ├── pom.xml
    │   └── src/main/java/com/auction/server/
    │       ├── ServerMain.java    # Entry point khởi chạy Server & nạp Demo Seed Data
    │       ├── dao/               # Lớp kết nối, truy vấn dữ liệu trực tiếp từ MySQL (Tầng DAO)
    │       ├── database/          # [Design Pattern] DatabaseManager áp dụng Singleton quản lý Connection Pool
    │       ├── network/           # SocketServer & ClientHandler quản lý kết nối thầu đồng thời
    │       ├── scheduler/         # Quản lý Timer đếm ngược thời gian kết thúc phiên tự động
    │       └── service/           # Tầng nghiệp vụ xử lý thầu (Xử lý concurrency an toàn, ký quỹ)
    └── client/                    # Module Giao diện (Client Module - View & Controller)
        ├── pom.xml
        └── src/main/
            ├── java/com/
            │   ├── Launcher.java  # Điểm kích hoạt Client (bỏ qua check Module-path JavaFX)
            │   ├── ClientMain.java # Khởi động ứng dụng JavaFX, quản lý kết nối SocketClient
            │   └── Controller/    # Trình điều khiển giao diện (Home, Detail, Dashboards,...)
            └── resources/
                ├── fxml/          # Định nghĩa giao diện người dùng bằng file FXML XML (Tầng View)
                ├── css/           # Style CSS tùy biến (style.css phong cách hiện đại)
                └── img/           # Tài nguyên hình ảnh, biểu tượng của hệ thống

```
---

## 📦 4. Vị trí các file .jar sau khi biên dịch

Khi bạn thực hiện biên dịch dự án bằng lệnh Maven, các file `.jar` sẽ được đóng gói tương ứng tại các thư mục đích của từng module:

*   **Shared Module**: 
    `[thư_mục_gốc]/auction-project/shared/target/shared-1.0-SNAPSHOT.jar`
*   **Server Module**: 
    `[thư_mục_gốc]/auction-project/server/target/server-1.0-SNAPSHOT.jar`
*   **Client Module**: 
    `[thư_mục_gốc]/auction-project/client/target/client-1.0-SNAPSHOT.jar`

---

## 🚀 5. Hướng dẫn chạy Hệ thống theo thứ tự cụ thể

Vui lòng tuân thủ chính xác thứ tự các bước sau đây để đảm bảo hệ thống vận hành trơn tru:

### Bước 1: Khởi động Server (Bắt buộc chạy trước)
Tại Terminal ở thư mục `auction-project`, chạy lệnh khởi động Server:
```bash
mvn exec:java -pl server -Dexec.mainClass="com.auction.server.ServerMain"
```
#### Quá trình tự động của Server ở lần chạy đầu tiên:
1.  Kết nối MySQL, tự động tạo database `auction_db`.
2.  Tự động sinh cấu trúc toàn bộ các bảng trong database.
3.  **Tự động Seed dữ liệu mẫu (Demo Data)** để bạn kiểm thử ngay lập tức mà không mất thời gian nhập liệu:
    *   **Admin**: Email: `admin@auction.vn` | Mật khẩu: `admin@123`
    *   **Seller**: Email: `seller@auction.vn` | Mật khẩu: `seller@123`
    *   **Bidder 1**: Email: `bidder1@auction.vn` | Mật khẩu: `bidder@123` *(Đã nạp sẵn 50,000,000 VND)*
    *   **Bidder 2**: Email: `bidder2@auction.vn` | Mật khẩu: `bidder@456` *(Đã nạp sẵn 100,000,000 VND)*
    *   **Sản phẩm đấu giá mẫu**: Đã tự động đăng và phê duyệt một phiên đấu giá chiếc **MacBook Pro M3** để bạn trải nghiệm ngay.

### Bước 2: Khởi động Client
Mở một cửa sổ Terminal mới tại thư mục `auction-project` và chạy lệnh để mở giao diện JavaFX:
```bash
mvn exec:java -pl client -Dexec.mainClass="com.Launcher"
```
*(Bạn có thể mở nhiều cửa sổ Client cùng lúc để giả lập nhiều Bidder/Seller đang tương tác thầu thời gian thực).*

---

## ✅ 6. Danh sách chức năng đã hoàn thành

Hệ thống đã triển khai đầy đủ và kiểm thử thành công các nghiệp vụ cốt lõi sau:

### 1. Hệ thống Xác thực & Phân quyền (Authentication)
*   **Đăng ký & Đăng nhập**: Hỗ trợ đăng ký tài khoản Bidder mới. Đăng nhập phân quyền chính xác các chức năng tương ứng với role (Admin, Seller, Bidder).
*   **Nâng cấp tài khoản (Upgrade to Seller)**: Bidder có thể gửi yêu cầu nâng cấp lên Seller đi kèm thông tin Tên cửa hàng và Căn cước công dân.

### 2. Quản lý Sản phẩm Đa dạng (Rich Item Management)
Hỗ trợ đăng và hiển thị thuộc tính chi tiết theo 3 danh mục sản phẩm khác nhau (sử dụng Factory Pattern):
*   **Thiết bị điện tử (Electronics)**: Thương hiệu, Model, Tình trạng (Mới/Cũ/Like-new), Thời gian bảo hành.
*   **Tác phẩm Nghệ thuật (Art)**: Tên tác giả, Năm sáng tác, Chất liệu chế tác, Trạng thái chứng nhận xác thực.
*   **Phương tiện di chuyển (Vehicle)**: Hãng sản xuất, Dòng xe, Năm sản xuất, Số ODO đã chạy, Loại nhiên liệu (Xăng/Dầu/Điện).

### 3. Quy trình Đấu giá & Tự động kết thúc phiên(Auction Flow)
*   **Lập phiên đấu giá**: Seller chọn sản phẩm có sẵn để lập phiên đấu giá mới, cấu hình thời gian bắt đầu/kết thúc và thời gian chống bắn tỉa. Trạng thái ban đầu của phiên là `PENDING`.
*   **Phê duyệt từ Admin**: Admin kiểm duyệt danh sách phiên chờ duyệt, có thể chấp nhận (`APPROVED` - tự động kích hoạt đếm ngược thời gian) hoặc từ chối kèm lý do cụ thể.
*   **Tự động kết thúc phiên & Xác định người thắng**:Tầng scheduler của Server liên tục quét thời gian. Khi phiên đấu giá hết giờ, hệ thống tự động đóng phiên, xác định người đặt giá cao nhất là người chiến thắng, tự động chuyển tiền đóng băng (ký quỹ) thành doanh thu cho Seller và gửi thông báo kết quả thời gian thực tới tất cả các client tham gia.

### 4. Động cơ Đấu giá Tiên tiến & Xử lý đồng thời (Concurrency Control)
*   **Xử lý đấu giá đồng thời an toàn (Concurrency Control)**: Hệ thống sử dụng cơ chế khóa đồng bộ ReentrantLock kết hợp với tầng Transaction cô lập của Database tại nghiệp vụ đặt thầu. Đảm bảo khi hàng trăm Bidder cùng nhấn thầu tại một mili-giây cuối cùng, hệ thống sẽ xếp hàng xử lý tuần tự, tuyệt đối không xảy ra hiện tượng Lost Update (ghi đè giá thầu lỗi) hoặc sai lệch số dư.
*   **Đấu giá thủ công (Manual Bidding)**: Người mua đặt giá thầu trực tiếp. Hệ thống tự động xác thực số dư khả dụng, bước giá tối thiểu và cập nhật vị trí dẫn đầu thời gian thực.
*   **Đấu giá tự động (Auto-Bidding)**: Người mua thiết lập ngân sách tối đa và bước giá mong muốn. Hệ thống tự động thay mặt Bidder đặt giá cao hơn đối thủ 1 bước giá mỗi khi bị vượt mặt, đảm bảo chiến thắng với mức chi phí tối ưu nhất mà không vượt quá ngân sách đã cài.
*   **Chống bắn tỉa thầu(Anti-Sniping)**: Nếu có bất cứ lượt đặt giá thầu hợp lệ nào diễn ra trong vòng 30 giây cuối cùng trước khi phiên đấu giá kết thúc, hệ thống sẽ tự động gia hạn thời gian kết thúc thêm 30 giây nữa. Cơ chế này đảm bảo mọi người tham gia đều có cơ hội phản hồi bình đẳng.

### 5. Quản lý Tài chính & Ví Ký quỹ An toàn
*   **Yêu cầu nạp tiền thời gian thực**: Người dùng gửi yêu cầu nạp tiền từ ví. Yêu cầu lập tức được đẩy qua socket và hiển thị trên màn hình Admin.
*   **Phê duyệt nạp tiền**: Admin phê duyệt đơn nạp, số dư khả dụng của người dùng lập tức được cập nhật mà không cần tải lại trang.
*   **Cơ chế Ký quỹ tự động**: Khi đặt giá cao nhất, hệ thống tự động khóa (đóng băng) số dư tương ứng của Bidder dẫn đầu để đảm bảo khả năng thanh toán. Khi có người trả giá cao hơn, số tiền đóng băng lập tức được hoàn trả vào số dư khả dụng một cách tự động và an toàn.

### 6. Quản trị viên (Admin Utilities)
*   **Quản lý người dùng**: Admin xem toàn bộ danh sách thành viên hệ thống và có quyền khóa (`active = 0`) hoặc mở khóa tài khoản người dùng ngay lập tức.

### 7. Trực quan hóa lịch sử đấu giá (Bid History Visualization) - Tính năng nâng cao
*   **Biểu đồ biến động giá**: Tích hợp đồ thị trực quan LineChart của JavaFX hiển thị dòng thời gian và các bước nhảy giá của phiên đấu giá, giúp người dùng dễ dàng phân tích xu hướng cạnh tranh của sản phẩm.

📌 **Tài nguyên dự án:**
* 📂 [Báo cáo PDF](https://drive.google.com/file/d/1HcZAC2LSc648RADioBzEgppqP9fctkZj/view?usp=sharing) 
* 🎬 [Video Demo vận hành hệ thống](https://drive.google.com/file/d/1wSy2Yu_9IujGjAcbtcFk8o6VDPlNkNU9/view?usp=sharing)