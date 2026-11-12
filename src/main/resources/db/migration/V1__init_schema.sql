CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TYPE user_role AS ENUM ('ADMIN', 'CUSTOMER', 'EMPLOYEE', 'DRIVER');
CREATE TYPE user_status AS ENUM ('ACTIVE', 'INACTIVE', 'BLOCKED', 'PENDING_VERIFICATION');
CREATE TYPE trip_status AS ENUM ('SCHEDULED', 'BOARDING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'DELAYED');
CREATE TYPE booking_status AS ENUM ('PENDING', 'CONFIRMED', 'PAID', 'CANCELLED', 'REFUNDED', 'EXPIRED');
CREATE TYPE payment_status AS ENUM ('PENDING', 'SUCCESS', 'FAILED', 'REFUNDED', 'CANCELLED');
CREATE TYPE payment_method AS ENUM ('CASH', 'VNPAY', 'MOMO', 'BANK_TRANSFER');
CREATE TYPE seat_status AS ENUM ('AVAILABLE', 'BOOKED', 'BLOCKED', 'MAINTENANCE');
CREATE TYPE vehicle_status AS ENUM ('ACTIVE', 'MAINTENANCE', 'RETIRED', 'INACTIVE');
CREATE TYPE shift_status AS ENUM ('SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED');
CREATE TYPE notification_type AS ENUM ('BOOKING_CONFIRMATION', 'BOOKING_REMINDER', 'TRIP_CANCELLED', 'TRIP_DELAYED', 'PAYMENT_SUCCESS', 'PAYMENT_FAILED', 'GENERAL');
CREATE TYPE notification_channel AS ENUM ('EMAIL', 'SMS', 'IN_APP', 'PUSH');

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(20) UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    avatar_url VARCHAR(500),
    date_of_birth DATE,
    gender VARCHAR(10),
    identity_card VARCHAR(20) UNIQUE,
    address TEXT,
    role user_role NOT NULL DEFAULT 'CUSTOMER',
    status user_status NOT NULL DEFAULT 'PENDING_VERIFICATION',
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    phone_verified BOOLEAN NOT NULL DEFAULT FALSE,
    last_login_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_phone ON users(phone);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_users_deleted_at ON users(deleted_at) WHERE deleted_at IS NULL;

CREATE TABLE routes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    departure_province VARCHAR(100) NOT NULL,
    departure_district VARCHAR(100),
    departure_ward VARCHAR(100),
    departure_address TEXT,
    arrival_province VARCHAR(100) NOT NULL,
    arrival_district VARCHAR(100),
    arrival_ward VARCHAR(100),
    arrival_address TEXT,
    distance_km DECIMAL(10,2),
    estimated_duration_minutes INTEGER,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_routes_code ON routes(code);
CREATE INDEX idx_routes_departure_arrival ON routes(departure_province, arrival_province);
CREATE INDEX idx_routes_active ON routes(is_active);

CREATE TABLE vehicles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    license_plate VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(255),
    brand VARCHAR(100),
    model VARCHAR(100),
    year_of_manufacture INTEGER,
    seat_count INTEGER NOT NULL,
    seat_layout JSONB,
    vehicle_type VARCHAR(50),
    fuel_type VARCHAR(50),
    status vehicle_status NOT NULL DEFAULT 'ACTIVE',
    registration_date DATE,
    insurance_expiry_date DATE,
    inspection_expiry_date DATE,
    image_urls JSONB,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_vehicles_license_plate ON vehicles(license_plate);
CREATE INDEX idx_vehicles_status ON vehicles(status);

CREATE TABLE trips (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    trip_code VARCHAR(50) NOT NULL UNIQUE,
    route_id UUID NOT NULL REFERENCES routes(id),
    vehicle_id UUID NOT NULL REFERENCES vehicles(id),
    driver_id UUID REFERENCES users(id),
    co_driver_id UUID REFERENCES users(id),
    departure_time TIMESTAMP WITH TIME ZONE NOT NULL,
    arrival_time TIMESTAMP WITH TIME ZONE NOT NULL,
    actual_departure_time TIMESTAMP WITH TIME ZONE,
    actual_arrival_time TIMESTAMP WITH TIME ZONE,
    base_price DECIMAL(12,2) NOT NULL,
    status trip_status NOT NULL DEFAULT 'SCHEDULED',
    available_seats INTEGER NOT NULL,
    total_seats INTEGER NOT NULL,
    notes TEXT,
    cancel_reason TEXT,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    cancelled_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_trips_trip_code ON trips(trip_code);
CREATE INDEX idx_trips_route_id ON trips(route_id);
CREATE INDEX idx_trips_vehicle_id ON trips(vehicle_id);
CREATE INDEX idx_trips_driver_id ON trips(driver_id);
CREATE INDEX idx_trips_departure_time ON trips(departure_time);
CREATE INDEX idx_trips_status ON trips(status);
CREATE INDEX idx_trips_departure_status ON trips(departure_time, status);

CREATE TABLE seats (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    trip_id UUID NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    seat_number VARCHAR(10) NOT NULL,
    seat_row INTEGER,
    seat_col INTEGER,
    floor INTEGER DEFAULT 1,
    seat_type VARCHAR(50) DEFAULT 'STANDARD',
    price_multiplier DECIMAL(3,2) DEFAULT 1.0,
    status seat_status NOT NULL DEFAULT 'AVAILABLE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(trip_id, seat_number)
);

CREATE INDEX idx_seats_trip_id ON seats(trip_id);
CREATE INDEX idx_seats_status ON seats(status);

CREATE TABLE bookings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    booking_code VARCHAR(50) NOT NULL UNIQUE,
    user_id UUID NOT NULL REFERENCES users(id),
    trip_id UUID NOT NULL REFERENCES trips(id),
    seat_ids UUID[] NOT NULL,
    passenger_name VARCHAR(255) NOT NULL,
    passenger_phone VARCHAR(20) NOT NULL,
    passenger_email VARCHAR(255),
    passenger_identity_card VARCHAR(20),
    total_amount DECIMAL(12,2) NOT NULL,
    discount_amount DECIMAL(12,2) DEFAULT 0,
    final_amount DECIMAL(12,2) NOT NULL,
    status booking_status NOT NULL DEFAULT 'PENDING',
    payment_method payment_method DEFAULT 'CASH',
    payment_status payment_status DEFAULT 'PENDING',
    payment_transaction_id VARCHAR(255),
    paid_at TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    cancel_reason TEXT,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_bookings_booking_code ON bookings(booking_code);
CREATE INDEX idx_bookings_user_id ON bookings(user_id);
CREATE INDEX idx_bookings_trip_id ON bookings(trip_id);
CREATE INDEX idx_bookings_status ON bookings(status);
CREATE INDEX idx_bookings_payment_status ON bookings(payment_status);
CREATE INDEX idx_bookings_created_at ON bookings(created_at);

CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    booking_id UUID NOT NULL REFERENCES bookings(id),
    transaction_id VARCHAR(255) NOT NULL UNIQUE,
    payment_method payment_method NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    status payment_status NOT NULL DEFAULT 'PENDING',
    gateway_response JSONB,
    gateway_transaction_id VARCHAR(255),
    paid_at TIMESTAMP WITH TIME ZONE,
    refunded_at TIMESTAMP WITH TIME ZONE,
    refund_amount DECIMAL(12,2),
    refund_reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_payments_booking_id ON payments(booking_id);
CREATE INDEX idx_payments_transaction_id ON payments(transaction_id);
CREATE INDEX idx_payments_status ON payments(status);

CREATE TABLE shifts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    employee_id UUID NOT NULL REFERENCES users(id),
    trip_id UUID REFERENCES trips(id),
    shift_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    shift_type VARCHAR(50) DEFAULT 'DRIVING',
    status shift_status NOT NULL DEFAULT 'SCHEDULED',
    check_in_time TIMESTAMP WITH TIME ZONE,
    check_out_time TIMESTAMP WITH TIME ZONE,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(employee_id, shift_date, start_time)
);

CREATE INDEX idx_shifts_employee_id ON shifts(employee_id);
CREATE INDEX idx_shifts_trip_id ON shifts(trip_id);
CREATE INDEX idx_shifts_shift_date ON shifts(shift_date);
CREATE INDEX idx_shifts_status ON shifts(status);

CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id),
    type notification_type NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    channel notification_channel NOT NULL DEFAULT 'IN_APP',
    reference_id UUID,
    reference_type VARCHAR(50),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMP WITH TIME ZONE,
    sent_at TIMESTAMP WITH TIME ZONE,
    failed_at TIMESTAMP WITH TIME ZONE,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_is_read ON notifications(is_read);
CREATE INDEX idx_notifications_created_at ON notifications(created_at);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(500) NOT NULL UNIQUE,
    expiry_date TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_expiry ON refresh_tokens(expiry_date);

CREATE TABLE system_configs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    config_key VARCHAR(100) NOT NULL UNIQUE,
    config_value TEXT,
    description TEXT,
    is_public BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Insert default admin user (password: admin123 - BCrypt encoded)
INSERT INTO users (email, phone, password_hash, full_name, role, status, email_verified, phone_verified)
VALUES ('admin@busstation.com', '0900000000', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', 'System Administrator', 'ADMIN', 'ACTIVE', true, true);

-- Insert sample routes
INSERT INTO routes (name, code, departure_province, arrival_province, distance_km, estimated_duration_minutes) VALUES
('Hà Nội - TP.HCM', 'HN-SGN', 'Hà Nội', 'TP.HCM', 1700, 1800),
('Hà Nội - Đà Nẵng', 'HN-DAD', 'Hà Nội', 'Đà Nẵng', 800, 900),
('TP.HCM - Cần Thơ', 'SGN-CT', 'TP.HCM', 'Cần Thơ', 170, 240),
('Hà Nội - Hải Phòng', 'HN-HPH', 'Hà Nội', 'Hải Phòng', 100, 120);

-- Insert sample vehicles
INSERT INTO vehicles (license_plate, name, brand, model, year_of_manufacture, seat_count, vehicle_type, fuel_type, status, seat_layout)
VALUES
('30A-12345', 'Xe khách VIP 1', 'Hyundai', 'Universe', 2022, 34, 'VIP', 'DIESEL', 'ACTIVE', '{"rows": 17, "cols": 2, "seatMap": [[1,2],[3,4],[5,6],[7,8],[9,10],[11,12],[13,14],[15,16],[17,18],[19,20],[21,22],[23,24],[25,26],[27,28],[29,30],[31,32],[33,34]]}'),
('30B-67890', 'Xe khách thường 1', 'Samco', 'Samco 350', 2021, 45, 'NORMAL', 'DIESEL', 'ACTIVE', '{"rows": 15, "cols": 3, "seatMap": [[1,2,3],[4,5,6],[7,8,9],[10,11,12],[13,14,15],[16,17,18],[19,20,21],[22,23,24],[25,26,27],[28,29,30],[31,32,33],[34,35,36],[37,38,39],[40,41,42],[43,44,45]]}');

-- Insert system configs
INSERT INTO system_configs (config_key, config_value, description, is_public) VALUES
('booking.expiry.minutes', '15', 'Thời gian hết hạn đặt vé chưa thanh toán (phút)', true),
('booking.max.seats.per.user', '6', 'Số ghế tối đa một người đặt được', true),
('notification.reminder.before.departure.minutes', '30', 'Nhắc nhở trước giờ khởi hành (phút)', true),
('payment.vnpay.enabled', 'true', 'Bật thanh toán VNPAY', true),
('payment.momo.enabled', 'true', 'Bật thanh toán MoMo', true);