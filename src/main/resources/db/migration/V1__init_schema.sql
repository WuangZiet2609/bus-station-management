CREATE TABLE users (
    id CHAR(36) NOT NULL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(20) UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    identity_card VARCHAR(20) UNIQUE,
    address TEXT,
    role VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    phone_verified BOOLEAN NOT NULL DEFAULT FALSE,
    last_login_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
);

CREATE TABLE routes (
    id CHAR(36) NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    departure_province VARCHAR(100) NOT NULL,
    arrival_province VARCHAR(100) NOT NULL,
    distance_km DOUBLE,
    estimated_duration_minutes INT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
);

CREATE TABLE vehicles (
    id CHAR(36) NOT NULL PRIMARY KEY,
    license_plate VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(255),
    brand VARCHAR(100),
    model VARCHAR(100),
    seat_count INT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
);

CREATE TABLE trips (
    id CHAR(36) NOT NULL PRIMARY KEY,
    trip_code VARCHAR(50) NOT NULL UNIQUE,
    route_id CHAR(36) NOT NULL,
    vehicle_id CHAR(36) NOT NULL,
    driver_id CHAR(36),
    co_driver_id CHAR(36),
    departure_time DATETIME(6) NOT NULL,
    arrival_time DATETIME(6) NOT NULL,
    base_price DECIMAL(12,2) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'SCHEDULED',
    available_seats INT NOT NULL,
    total_seats INT NOT NULL,
    notes TEXT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_trips_route FOREIGN KEY (route_id) REFERENCES routes(id),
    CONSTRAINT fk_trips_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id),
    CONSTRAINT fk_trips_driver FOREIGN KEY (driver_id) REFERENCES users(id),
    CONSTRAINT fk_trips_co_driver FOREIGN KEY (co_driver_id) REFERENCES users(id)
);

CREATE INDEX idx_trips_departure_time ON trips(departure_time);
CREATE INDEX idx_trips_route_id ON trips(route_id);

INSERT INTO users (
    id, email, phone, password_hash, full_name, role, status,
    email_verified, phone_verified, created_at, updated_at
) VALUES (
    '00000000-0000-0000-0000-000000000001',
    'admin@busstation.com',
    '0900000000',
    '$2a$10$mKRZ4ra1.mAwGEsz1WpTkugjHzi2A0lsbzt.2Bo0UFDy86X8qFmdW',
    'System Administrator',
    'ADMIN',
    'ACTIVE',
    TRUE,
    TRUE,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
);

INSERT INTO routes (
    id, name, code, departure_province, arrival_province,
    distance_km, estimated_duration_minutes, is_active, created_at, updated_at
) VALUES
('00000000-0000-0000-0000-000000000002', 'Hà Nội - TP.HCM', 'HN-SGN', 'Hà Nội', 'TP.HCM', 1700, 1800, TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
('00000000-0000-0000-0000-000000000003', 'Hà Nội - Đà Nẵng', 'HN-DAD', 'Hà Nội', 'Đà Nẵng', 800, 900, TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6));

INSERT INTO vehicles (
    id, license_plate, name, brand, model, seat_count, status, created_at, updated_at
) VALUES
('00000000-0000-0000-0000-000000000004', '30A-12345', 'Xe khách VIP 1', 'Hyundai', 'Universe', 34, 'ACTIVE', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
('00000000-0000-0000-0000-000000000005', '30B-67890', 'Xe khách thường 1', 'Samco', 'Samco 350', 45, 'ACTIVE', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6));
