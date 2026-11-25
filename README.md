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
