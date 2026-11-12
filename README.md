# Hệ thống Quản Lý Nhà Xe Khách - Bus Station Management

Hệ thống quản lý nhà xe khách được xây dựng với Spring Boot 3.x, PostgreSQL, và kiến trúc REST API cho frontend React/Vue.

## Tính năng chính

### Quản trị viên (Admin)
- Quản lý người dùng (khách hàng, nhân viên, tài xế)
- Quản lý tuyến đường, xe, chuyến xe
- Quản lý ca làm việc
- Báo cáo doanh thu, tỷ lệ lấp đầy
- Cấu hình hệ thống

### Nhân viên (Employee)
- Tạo/sửa/xóa chuyến xe
- Quản lý đặt vé, check-in khách
- Phân công tài xế, ca làm việc
- Xem báo cáo doanh thu

### Tài xế (Driver)
- Xem lịch chạy xe được phân công
- Check-in/check-out ca làm việc
- Cập nhật trạng thái chuyến xe
- Nhận thông báo nhắc nhở

### Khách hàng (Customer)
- Tìm kiếm chuyến xe theo tuyến, ngày
- Chọn ghế, đặt vé online
- Thanh toán VNPAY/MoMo
- Nhận vé điện tử, nhắc nhở qua email
- Xem lịch sử đặt vé

## Công nghệ sử dụng

- **Backend**: Spring Boot 3.2.5, Java 21
- **Database**: PostgreSQL 16
- **Migration**: Flyway
- **Security**: Spring Security (Session-based)
- **ORM**: Spring Data JPA, Hibernate
- **Mapping**: MapStruct
- **Build**: Maven
- **Payment**: VNPAY, MoMo (sandbox)
- **Real-time**: WebSocket (STOMP/SockJS)
- **Email**: Spring Mail (SMTP)
- **Scheduling**: Spring Scheduler
- **Caching**: Caffeine

## Cấu trúc dự án

```
src/main/java/com/busstation/
├── config/              # Cấu hình ứng dụng
├── controller/          # REST Controllers
├── dto/
│   ├── request/         # Request DTOs
│   └── response/        # Response DTOs
├── entity/              # JPA Entities
├── exception/           # Exception handling
├── payment/
│   ├── vnpay/           # VNPAY integration
│   └── momo/            # MoMo integration
├── repository/          # Spring Data Repositories
├── schedule/            # Scheduled tasks
├── security/            # Security config
├── service/             # Business logic
├── util/                # Utilities
└── websocket/           # WebSocket config
```

## Yêu cầu hệ thống

- Java 21+
- Maven 3.9+
- PostgreSQL 16+ (hoặc Docker)
- Node.js 18+ (cho frontend)

## Cài đặt và chạy

### 1. Khởi động Database (Docker)

```bash
docker-compose up -d
```

### 2. Cấu hình ứng dụng

Sửa file `src/main/resources/application.yml`:
- Database connection
- Mail settings (Gmail SMTP)
- VNPAY/MoMo credentials (sandbox)

### 3. Build và chạy

```bash
# Build
mvn clean install

# Chạy
mvn spring-boot:run
```

Ứng dụng sẽ chạy tại: `http://localhost:8080/api`

### 4. Kiểm tra API

- Swagger UI: `http://localhost:8080/api/swagger-ui.html` (cần thêm dependency)
- Health check: `http://localhost:8080/api/actuator/health`

## Tài khoản mặc định

Sau khi chạy migration (Flyway), tài khoản admin mặc định:
- Email: `admin@busstation.com`
- Mật khẩu: `admin123`

## API Endpoints chính

### Auth
- `POST /api/auth/register` - Đăng ký
- `POST /api/auth/login` - Đăng nhập
- `POST /api/auth/logout` - Đăng xuất
- `GET /api/auth/me` - Thông tin user hiện tại

### Public (Không cần đăng nhập)
- `GET /api/public/routes` - Danh sách tuyến
- `GET /api/public/routes/search?departure=...&arrival=...` - Tìm tuyến
- `GET /api/public/trips/search?routeId=...&departureDate=...` - Tìm chuyến
- `GET /api/public/trips/{id}/seats` - Sơ đồ ghế

### Admin
- `GET/POST/PUT/DELETE /api/admin/users` - Quản lý user
- `GET/POST/PUT/DELETE /api/routes` - Quản lý tuyến
- `GET/POST/PUT/DELETE /api/vehicles` - Quản lý xe
- `GET /api/reports/*` - Báo cáo

### Employee
- `GET/POST/PUT /api/trips` - Quản lý chuyến xe
- `GET/PUT /api/bookings` - Quản lý đặt vé
- `GET/POST/PUT /api/shifts` - Quản lý ca làm việc

### Customer
- `POST /api/bookings` - Đặt vé
- `GET /api/bookings` - Lịch sử đặt vé
- `POST /api/payments/init` - Khởi tạo thanh toán

### Driver
- `GET /api/shifts/my` - Lịch làm việc của mình
- `PATCH /api/shifts/{id}/checkin` - Check-in ca
- `PATCH /api/shifts/{id}/checkout` - Check-out ca

## Thanh toán

### VNPAY (Sandbox)
- TMN Code: cấu hình trong `application.yml`
- Hash Secret: cấu hình trong `application.yml`
- Test card: 9704198526191432198 / 07/25 / 123 / OTP: 123456

### MoMo (Sandbox)
- Partner Code: cấu hình trong `application.yml`
- Access Key: cấu hình trong `application.yml`
- Secret Key: cấu hình trong `application.yml`

## WebSocket

Kết nối: `ws://localhost:8080/api/ws`

Subscribe notifications:
```
STOMP -> /user/queue/notifications
```

## Scheduled Tasks

- Mỗi phút: Kiểm tra chuyến sắp khởi hành, gửi nhắc nhở
- Mỗi 5 phút: Hủy vé hết hạn chưa thanh toán
- Hàng ngày 2h sáng: Dọn dẹp dữ liệu cũ

## Triển khai Production

1. Build JAR: `mvn clean package -DskipTests`
2. Cấu hình `application-prod.yml`
3. Chạy: `java -jar -Dspring.profiles.active=prod app.jar`

Hoặc dùng Docker:
```dockerfile
FROM eclipse-temurin:21-jre
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

## Phát triển thêm

- Thêm Swagger/OpenAPI documentation
- Unit/Integration tests với Testcontainers
- CI/CD pipeline (GitHub Actions/GitLab CI)
- Monitoring với Prometheus/Grafana
- Log aggregation (ELK/Loki)
- Rate limiting, API Gateway
- Multi-language support (i18n)

## License

MIT License