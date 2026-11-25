# Bus Station Management

Backend quản lý chuyến xe với đăng ký/đăng nhập, phân quyền theo vai trò, quản lý tuyến đường, xe, tài xế và CRUD chuyến xe.

## Công nghệ

- Java 21, Spring Boot 3.2.5
- Spring Data JPA / Hibernate
- MySQL 8+
- Flyway

## Chạy với MySQL local

1. Tạo database rỗng:

   ```sql
   CREATE DATABASE bus_station CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

2. Cấu hình `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` theo thông tin MySQL trên máy. Mặc định kết nối `localhost:3306`, database `bus_station`, user `root`, mật khẩu trống. Nếu MySQL có mật khẩu, cần đặt `DB_PASSWORD`; lỗi `Access denied for user 'root'` nghĩa là thông tin đăng nhập không khớp.
3. Chạy ứng dụng:

   ```bash
   mvn spring-boot:run
   ```

   Trên PowerShell, có thể thiết lập thông tin kết nối cho phiên hiện tại trước khi chạy:

   ```powershell
   $env:DB_USERNAME = "root"
   $env:DB_PASSWORD = "<mat-khau-MySQL>"
   mvn spring-boot:run
   ```

Flyway tạo các bảng và dữ liệu mẫu ở lần chạy đầu. Migration yêu cầu database trống; không dùng lại schema PostgreSQL hoặc database đã có dữ liệu.

## Tài khoản ban đầu

- Email: `admin@busstation.com`
- Mật khẩu: `admin123`

Đăng ký công khai tạo tài khoản `CUSTOMER`; chỉ tài khoản `ADMIN` mới có thể tạo tài xế và thực hiện các thao tác ghi/xóa dữ liệu tuyến, xe, chuyến.

## API chính

- `POST /api/auth/register`, `POST /api/auth/login`, `POST /api/auth/logout`, `GET /api/auth/me`
- `/api/routes`: CRUD tuyến
- `/api/vehicles`: CRUD xe
- `/api/drivers`: CRUD tài xế (ADMIN)
- `/api/trips`: CRUD chuyến

Các API yêu cầu session đăng nhập, ngoại trừ đăng ký và đăng nhập. Các thao tác tạo/sửa/xóa tuyến, xe, chuyến và quản lý tài xế chỉ dành cho ADMIN.
