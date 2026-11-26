# Bus Station Management

Backend quản lý chuyến xe với đăng ký/đăng nhập, phân quyền theo vai trò, quản lý tuyến đường, xe, tài xế và CRUD chuyến xe.

## Công nghệ

- Java 21, Spring Boot 3.2.5
- Spring Data JPA / Hibernate
- MySQL 8+
- Flyway
- Springdoc OpenAPI (Swagger UI)
- Lombok

## Mã định danh dễ đọc (tự động sinh)

Để khách hàng dễ kiểm soát, hệ thống sinh mã business thay vì hiển thị UUID:

| Đối tượng | Mã | Trường |
|-----------|-----|--------|
| Khách hàng | `KH-0001` | `customerCode` |
| Tài xế | `DRV-0001` | `customerCode` |
| Tuyến đường | `RTE-0001` | `code` |
| Xe | `VEH-0001` | `vehicleCode` |
| Chuyến xe | `TRIP-0001` | `tripCode` |
| Vé | `TKT-0001` | `ticketCode` |

## Cài đặt môi trường (máy mới)

Xem chi tiết trong file `cachcaidat.txt`. Tóm tắt nhanh:

### 1. Cài Java 21 + Maven (không cần sudo)

```bash
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 21.0.5-tem
sdk install maven
```

### 2. Chạy MySQL 8 qua Docker

```bash
docker run -d --name qlnx-mysql \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=qlnhaxe_java \
  -p 3306:3306 \
  mysql:8

docker exec -i qlnx-mysql mysql -uroot -proot <<'SQL'
ALTER USER 'root'@'localhost' IDENTIFIED BY '123456789';
CREATE USER IF NOT EXISTS 'root'@'%' IDENTIFIED BY '123456789';
GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' WITH GRANT OPTION;
FLUSH PRIVILEGES;
SQL
```

### 3. Chạy ứng dụng

```bash
cd /home/hoan.nguyen@rikai.technology/java/QLNX_springboot
source "$HOME/.sdkman/bin/sdkman-init.sh"
mvn spring-boot:run
```

Hoặc build jar rồi chạy:

```bash
mvn clean package -DskipTests
java -jar target/bus-station-management-1.0.0.jar
```

Lưu ý: Mỗi lần mở terminal mới, hãy chạy `source "$HOME/.sdkman/bin/sdkman-init.sh"` trước khi dùng `java`/`mvn`.

## Cấu hình kết nối DB

File `src/main/resources/application.properties`:

```
spring.datasource.url=jdbc:mysql://localhost:3306/qlnhaxe_java?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=123456789
```

Flyway tự tạo bảng và chèn dữ liệu mẫu khi chạy lần đầu (yêu cầu database trống).

## Truy cập ứng dụng

- API gốc: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- API docs JSON: `http://localhost:8080/v3/api-docs`

## Tài khoản ban đầu

- Email: `admin@busstation.com`
- Mật khẩu: `admin123`
- Vai trò: `ADMIN`

Đăng ký công khai tạo tài khoản `CUSTOMER`; chỉ tài khoản `ADMIN` mới có thể tạo tài xế và thực hiện ghi/xóa dữ liệu tuyến, xe, chuyến.

## API chính

| API | Quyền |
|-----|-------|
| `POST /api/auth/register` | Public |
| `POST /api/auth/login` | Public |
| `POST /api/auth/logout` | Đã đăng nhập |
| `GET /api/auth/me` | Đã đăng nhập |
| `GET /api/routes` | Public |
| `GET /api/routes/{id}` | Public |
| `GET /api/routes/code/{code}` | Public |
| `POST/PUT/DELETE /api/routes/**` | ADMIN |
| `GET /api/vehicles` | Public |
| `GET /api/vehicles/{id}` | Public |
| `GET /api/vehicles/code/{vehicleCode}` | Public |
| `POST/PUT/DELETE /api/vehicles/**` | ADMIN |
| `GET /api/trips` | Public |
| `GET /api/trips/{id}` | Public |
| `GET /api/trips/code/{tripCode}` | Public |
| `GET /api/trips/search` | Public |
| `POST/PUT/DELETE /api/trips/**` | ADMIN |
| `GET /api/drivers/**` | ADMIN |
| `POST /api/tickets` | Đã đăng nhập |
| `POST /api/tickets/{id}/pay` | Đã đăng nhập (chủ sở hữu vé) |
| `GET /api/tickets/me` | Đã đăng nhập |
| `GET /api/tickets/search` | Đã đăng nhập |

Các API yêu cầu session đăng nhập, ngoại trừ đăng ký và đăng nhập. Các thao tác tạo/sửa/xóa tuyến, xe, chuyến và quản lý tài xế chỉ dành cho ADMIN.

## Giao diện React

Giao diện tiếng Việt nằm trong thư mục `frontend`. Chạy MySQL và Spring Boot
trên cổng `8080`, rồi mở một terminal khác tại thư mục dự án:

```bash
cd frontend
npm ci
npm run dev
```

Mở [http://localhost:5173](http://localhost:5173). Vite chuyển tiếp
`/api` sang Spring Boot và giữ cookie đăng nhập theo cùng nguồn. Nếu backend
dùng cổng khác, chạy ví dụ `BUS_API_TARGET=http://localhost:8081 npm run dev`.

- Khách có thể tìm chuyến, đăng nhập hoặc tạo tài khoản, chọn ghế và xem vé.
- Quản trị viên có màn hình tổng quan, quản lý chuyến, tuyến, xe, tài xế và
  tra cứu vé. Nhân viên có thể tra cứu vé.
- Trong ô chọn, bấm để mở toàn bộ danh sách hoặc gõ tên, mã, biển số hay
  số điện thoại tùy trường. Có thể gõ tiếng Việt không dấu và dùng phím
  lên, xuống, Enter để chọn.
- Sơ đồ ghế lấy danh sách số ghế đã đặt từ
  `GET /api/trips/{id}/occupied-seats`. Spring Boot kiểm tra lại tình trạng
  ghế khi khách gửi yêu cầu đặt.

Chạy `npm run build` trong `frontend` để tạo `frontend/dist`. Khi triển khai
bản build, phục vụ thư mục này và chuyển tiếp đường dẫn `/api` đến Spring Boot
trên cùng tên miền.
