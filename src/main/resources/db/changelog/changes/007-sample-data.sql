-- liquibase formatted sql

-- ============================================================
-- changeset system:007-sample-data-stations
-- comment: Sample railway stations (catalog data)
-- ============================================================
INSERT INTO railway_stations (station_id, station_code, station_name, city_code, timezone_name, display_order, status,
                              created_date, last_modified_date, created_by, last_modified_by, version)
VALUES ('11111111-0001-0000-0000-000000000001', 'HAN', 'Ga Hà Nội',        'HAN', 'Asia/Ho_Chi_Minh', 1, 'ACT', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
       ('11111111-0001-0000-0000-000000000002', 'VIN', 'Ga Vinh',           'VIN', 'Asia/Ho_Chi_Minh', 2, 'ACT', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
       ('11111111-0001-0000-0000-000000000003', 'HUE', 'Ga Huế',            'HUE', 'Asia/Ho_Chi_Minh', 3, 'ACT', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
       ('11111111-0001-0000-0000-000000000004', 'DAN', 'Ga Đà Nẵng',        'DAN', 'Asia/Ho_Chi_Minh', 4, 'ACT', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
       ('11111111-0001-0000-0000-000000000005', 'QNH', 'Ga Diêu Trì',       'QNH', 'Asia/Ho_Chi_Minh', 5, 'ACT', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
       ('11111111-0001-0000-0000-000000000006', 'NHA', 'Ga Nha Trang',      'NHA', 'Asia/Ho_Chi_Minh', 6, 'ACT', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
       ('11111111-0001-0000-0000-000000000007', 'SGN', 'Ga Sài Gòn',        'SGN', 'Asia/Ho_Chi_Minh', 7, 'ACT', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0);

-- rollback DELETE FROM railway_stations WHERE station_id IN ('11111111-0001-0000-0000-000000000001','11111111-0001-0000-0000-000000000002','11111111-0001-0000-0000-000000000003','11111111-0001-0000-0000-000000000004','11111111-0001-0000-0000-000000000005','11111111-0001-0000-0000-000000000006','11111111-0001-0000-0000-000000000007');

-- ============================================================
-- changeset system:007-sample-data-trains
-- comment: Sample trains (catalog data)
-- ============================================================
INSERT INTO trains (train_id, train_code, train_name, seat_layout_version, operator_code, status,
                    created_date, last_modified_date, created_by, last_modified_by, version)
VALUES ('44444444-0004-0000-0000-000000000001', 'SE1', 'Tàu SE1', 'v2024-se', 'VNR', 'ACT', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
       ('44444444-0004-0000-0000-000000000002', 'SE2', 'Tàu SE2', 'v2024-se', 'VNR', 'ACT', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
       ('44444444-0004-0000-0000-000000000003', 'SE3', 'Tàu SE3', 'v2024-se', 'VNR', 'ACT', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
       ('44444444-0004-0000-0000-000000000004', 'SE4', 'Tàu SE4', 'v2024-se', 'VNR', 'ACT', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0);

-- rollback DELETE FROM trains WHERE train_id IN ('44444444-0004-0000-0000-000000000001','44444444-0004-0000-0000-000000000002','44444444-0004-0000-0000-000000000003','44444444-0004-0000-0000-000000000004');

-- ============================================================
-- changeset system:007-sample-data-routes
-- comment: Sample train routes (catalog data)
-- ============================================================
-- Khoảng cách tính từ ga gốc đến ga cuối (km)
INSERT INTO train_routes (route_id, route_code, route_name, origin_station_id, destination_station_id, distance_km, status,
                          created_date, last_modified_date, created_by, last_modified_by, version)
VALUES ('22222222-0002-0000-0000-000000000001', 'SE-HAN-SGN', 'Hà Nội – Sài Gòn',  '11111111-0001-0000-0000-000000000001', '11111111-0001-0000-0000-000000000007', 1726.00, 'ACT', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
       ('22222222-0002-0000-0000-000000000002', 'SE-HAN-DAN', 'Hà Nội – Đà Nẵng',  '11111111-0001-0000-0000-000000000001', '11111111-0001-0000-0000-000000000004',  791.00, 'ACT', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
       ('22222222-0002-0000-0000-000000000003', 'SE-DAN-SGN', 'Đà Nẵng – Sài Gòn', '11111111-0001-0000-0000-000000000004', '11111111-0001-0000-0000-000000000007',  935.00, 'ACT', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0);

-- rollback DELETE FROM train_routes WHERE route_id IN ('22222222-0002-0000-0000-000000000001','22222222-0002-0000-0000-000000000002','22222222-0002-0000-0000-000000000003');

-- ============================================================
-- changeset system:007-sample-data-route-stops
-- comment: Sample route stops (catalog data)
-- planned_arrival/departure_offset_minutes: phút tính từ giờ khởi hành
-- ============================================================

-- Tuyến SE-HAN-SGN: HAN → VIN → HUE → DAN → QNH → NHA → SGN
INSERT INTO train_route_stops (route_stop_id, route_id, station_id, stop_sequence,
                               distance_from_origin_km, planned_arrival_offset_minutes, planned_departure_offset_minutes,
                               created_date, last_modified_date, created_by, last_modified_by, version)
VALUES
    -- HAN (origin)
    ('33333333-0003-0000-0000-100000000001', '22222222-0002-0000-0000-000000000001', '11111111-0001-0000-0000-000000000001', 1,    0.00, NULL, 0,    NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    -- VIN
    ('33333333-0003-0000-0000-100000000002', '22222222-0002-0000-0000-000000000001', '11111111-0001-0000-0000-000000000002', 2,  319.00,  330, 345,  NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    -- HUE
    ('33333333-0003-0000-0000-100000000003', '22222222-0002-0000-0000-000000000001', '11111111-0001-0000-0000-000000000003', 3,  688.00,  720, 740,  NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    -- DAN
    ('33333333-0003-0000-0000-100000000004', '22222222-0002-0000-0000-000000000001', '11111111-0001-0000-0000-000000000004', 4,  791.00,  820, 840,  NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    -- QNH
    ('33333333-0003-0000-0000-100000000005', '22222222-0002-0000-0000-000000000001', '11111111-0001-0000-0000-000000000005', 5, 1065.00, 1080, 1100, NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    -- NHA
    ('33333333-0003-0000-0000-100000000006', '22222222-0002-0000-0000-000000000001', '11111111-0001-0000-0000-000000000006', 6, 1315.00, 1320, 1340, NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    -- SGN (destination)
    ('33333333-0003-0000-0000-100000000007', '22222222-0002-0000-0000-000000000001', '11111111-0001-0000-0000-000000000007', 7, 1726.00, 1980, NULL, NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0);

-- Tuyến SE-HAN-DAN: HAN → VIN → HUE → DAN
INSERT INTO train_route_stops (route_stop_id, route_id, station_id, stop_sequence,
                               distance_from_origin_km, planned_arrival_offset_minutes, planned_departure_offset_minutes,
                               created_date, last_modified_date, created_by, last_modified_by, version)
VALUES
    ('33333333-0003-0000-0000-200000000001', '22222222-0002-0000-0000-000000000002', '11111111-0001-0000-0000-000000000001', 1,   0.00, NULL, 0,   NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('33333333-0003-0000-0000-200000000002', '22222222-0002-0000-0000-000000000002', '11111111-0001-0000-0000-000000000002', 2, 319.00,  330, 345, NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('33333333-0003-0000-0000-200000000003', '22222222-0002-0000-0000-000000000002', '11111111-0001-0000-0000-000000000003', 3, 688.00,  720, 740, NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('33333333-0003-0000-0000-200000000004', '22222222-0002-0000-0000-000000000002', '11111111-0001-0000-0000-000000000004', 4, 791.00,  820, NULL, NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0);

-- Tuyến SE-DAN-SGN: DAN → QNH → NHA → SGN
INSERT INTO train_route_stops (route_stop_id, route_id, station_id, stop_sequence,
                               distance_from_origin_km, planned_arrival_offset_minutes, planned_departure_offset_minutes,
                               created_date, last_modified_date, created_by, last_modified_by, version)
VALUES
    ('33333333-0003-0000-0000-300000000001', '22222222-0002-0000-0000-000000000003', '11111111-0001-0000-0000-000000000004', 1,   0.00, NULL, 0,    NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('33333333-0003-0000-0000-300000000002', '22222222-0002-0000-0000-000000000003', '11111111-0001-0000-0000-000000000005', 2, 274.00,  250, 270,  NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('33333333-0003-0000-0000-300000000003', '22222222-0002-0000-0000-000000000003', '11111111-0001-0000-0000-000000000006', 3, 524.00,  500, 520,  NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('33333333-0003-0000-0000-300000000004', '22222222-0002-0000-0000-000000000003', '11111111-0001-0000-0000-000000000007', 4, 935.00, 1140, NULL, NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0);

-- rollback DELETE FROM train_route_stops WHERE route_id IN ('22222222-0002-0000-0000-000000000001','22222222-0002-0000-0000-000000000002','22222222-0002-0000-0000-000000000003');

-- ============================================================
-- changeset system:007-sample-data-departures
-- comment: Sample train departures (scheduled service data)
-- Tất cả thời gian theo UTC; ICT = UTC+7
-- SE1 khởi hành 19:00 ICT = 12:00 UTC, hành trình ~33h
-- SE3 khởi hành 06:00 ICT = 23:00 UTC (ngày trước), hành trình ~14h
-- SE4 khởi hành 13:00 ICT = 06:00 UTC, hành trình ~19h
-- ============================================================
INSERT INTO train_departures (departure_id, route_id, train_id, departure_code, business_date,
                              origin_station_id, destination_station_id,
                              planned_departure_at, planned_arrival_at,
                              sale_opens_at, sale_closes_at,
                              status, created_date, last_modified_date, created_by, last_modified_by, version)
VALUES
    -- SE1: HAN → SGN, 01/06/2026
    ('55555555-0005-0000-0000-000000000001',
     '22222222-0002-0000-0000-000000000001', '44444444-0004-0000-0000-000000000001',
     'SE1-20260601', '2026-06-01',
     '11111111-0001-0000-0000-000000000001', '11111111-0001-0000-0000-000000000007',
     '2026-06-01 12:00:00+00', '2026-06-03 01:00:00+00',
     '2026-03-01 00:00:00+00', '2026-06-01 10:00:00+00',
     'OPN', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),

    -- SE1: HAN → SGN, 02/06/2026
    ('55555555-0005-0000-0000-000000000002',
     '22222222-0002-0000-0000-000000000001', '44444444-0004-0000-0000-000000000001',
     'SE1-20260602', '2026-06-02',
     '11111111-0001-0000-0000-000000000001', '11111111-0001-0000-0000-000000000007',
     '2026-06-02 12:00:00+00', '2026-06-04 01:00:00+00',
     '2026-03-02 00:00:00+00', '2026-06-02 10:00:00+00',
     'OPN', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),

    -- SE3: HAN → DAN, 01/06/2026
    ('55555555-0005-0000-0000-000000000003',
     '22222222-0002-0000-0000-000000000002', '44444444-0004-0000-0000-000000000003',
     'SE3-20260601', '2026-06-01',
     '11111111-0001-0000-0000-000000000001', '11111111-0001-0000-0000-000000000004',
     '2026-05-31 23:00:00+00', '2026-06-01 13:00:00+00',
     '2026-03-01 00:00:00+00', '2026-05-31 21:00:00+00',
     'OPN', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),

    -- SE4: DAN → SGN, 01/06/2026
    ('55555555-0005-0000-0000-000000000004',
     '22222222-0002-0000-0000-000000000003', '44444444-0004-0000-0000-000000000004',
     'SE4-20260601', '2026-06-01',
     '11111111-0001-0000-0000-000000000004', '11111111-0001-0000-0000-000000000007',
     '2026-06-01 06:00:00+00', '2026-06-02 01:00:00+00',
     '2026-03-01 00:00:00+00', '2026-06-01 04:00:00+00',
     'OPN', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0);

-- rollback DELETE FROM train_departures WHERE departure_id IN ('55555555-0005-0000-0000-000000000001','55555555-0005-0000-0000-000000000002','55555555-0005-0000-0000-000000000003','55555555-0005-0000-0000-000000000004');

-- ============================================================
-- changeset system:007-sample-data-inventory-buckets
-- comment: Departure inventory buckets (4 buckets / hạng ghế / chuyến)
-- Hạng ghế: SE=Giường mềm ĐH, NG=Giường cứng ĐH, GH=Ghế mềm ĐH
-- quota: REG (thường lệ)
-- Đơn vị giá: VND
-- ============================================================

-- ── SE1-20260601 ────────────────────────────────────────────
-- Hạng SE (Giường mềm): 20 chỗ/bucket × 4 bucket = 80 chỗ
INSERT INTO departure_inventory_buckets
(inventory_bucket_id, departure_id, seat_class_code, quota_code, bucket_no,
 total_quantity, available_quantity, reserved_quantity, sold_quantity, oversell_limit,
 fare_amount, currency_code, sale_status,
 created_date, last_modified_date, created_by, last_modified_by, version)
VALUES
    ('66660001-0001-0000-0000-000000000001', '55555555-0005-0000-0000-000000000001', 'SE', 'REG', 1, 20, 20, 0, 0, 0, 1200000.00, 'VND', 'OPN', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('66660001-0001-0000-0000-000000000002', '55555555-0005-0000-0000-000000000001', 'SE', 'REG', 2, 20, 20, 0, 0, 0, 1200000.00, 'VND', 'OPN', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('66660001-0001-0000-0000-000000000003', '55555555-0005-0000-0000-000000000001', 'SE', 'REG', 3, 20, 20, 0, 0, 0, 1200000.00, 'VND', 'OPN', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('66660001-0001-0000-0000-000000000004', '55555555-0005-0000-0000-000000000001', 'SE', 'REG', 4, 20, 20, 0, 0, 0, 1200000.00, 'VND', 'OPN', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0);

-- Hạng NG (Giường cứng): 28 chỗ/bucket × 4 bucket = 112 chỗ
INSERT INTO departure_inventory_buckets
(inventory_bucket_id, departure_id, seat_class_code, quota_code, bucket_no,
 total_quantity, available_quantity, reserved_quantity, sold_quantity, oversell_limit,
 fare_amount, currency_code, sale_status,
 created_date, last_modified_date, created_by, last_modified_by, version)
VALUES
    ('66660001-0002-0000-0000-000000000001', '55555555-0005-0000-0000-000000000001', 'NG', 'REG', 1, 28, 28, 0, 0, 0, 800000.00, 'VND', 'OPN', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('66660001-0002-0000-0000-000000000002', '55555555-0005-0000-0000-000000000001', 'NG', 'REG', 2, 28, 28, 0, 0, 0, 800000.00, 'VND', 'OPN', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('66660001-0002-0000-0000-000000000003', '55555555-0005-0000-0000-000000000001', 'NG', 'REG', 3, 28, 28, 0, 0, 0, 800000.00, 'VND', 'OPN', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('66660001-0002-0000-0000-000000000004', '55555555-0005-0000-0000-000000000001', 'NG', 'REG', 4, 28, 28, 0, 0, 0, 800000.00, 'VND', 'OPN', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0);

-- Hạng GH (Ghế mềm): 40 chỗ/bucket × 4 bucket = 160 chỗ
INSERT INTO departure_inventory_buckets
(inventory_bucket_id, departure_id, seat_class_code, quota_code, bucket_no,
 total_quantity, available_quantity, reserved_quantity, sold_quantity, oversell_limit,
 fare_amount, currency_code, sale_status,
 created_date, last_modified_date, created_by, last_modified_by, version)
VALUES
    ('66660001-0003-0000-0000-000000000001', '55555555-0005-0000-0000-000000000001', 'GH', 'REG', 1, 40, 40, 0, 0, 0, 500000.00, 'VND', 'OPN', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('66660001-0003-0000-0000-000000000002', '55555555-0005-0000-0000-000000000001', 'GH', 'REG', 2, 40, 40, 0, 0, 0, 500000.00, 'VND', 'OPN', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('66660001-0003-0000-0000-000000000003', '55555555-0005-0000-0000-000000000001', 'GH', 'REG', 3, 40, 40, 0, 0, 0, 500000.00, 'VND', 'OPN', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('66660001-0003-0000-0000-000000000004', '55555555-0005-0000-0000-000000000001', 'GH', 'REG', 4, 40, 40, 0, 0, 0, 500000.00, 'VND', 'OPN', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0);

-- ── SE1-20260602 ────────────────────────────────────────────
-- Hạng SE
INSERT INTO departure_inventory_buckets
(inventory_bucket_id, departure_id, seat_class_code, quota_code, bucket_no,
 total_quantity, available_quantity, reserved_quantity, sold_quantity, oversell_limit,
 fare_amount, currency_code, sale_status,
 created_date, last_modified_date, created_by, last_modified_by, version)
VALUES
    ('66660002-0001-0000-0000-000000000001', '55555555-0005-0000-0000-000000000002', 'SE', 'REG', 1, 20, 20, 0, 0, 0, 1200000.00, 'VND', 'OPN', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('66660002-0001-0000-0000-000000000002', '55555555-0005-0000-0000-000000000002', 'SE', 'REG', 2, 20, 20, 0, 0, 0, 1200000.00, 'VND', 'OPN', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('66660002-0001-0000-0000-000000000003', '55555555-0005-0000-0000-000000000002', 'SE', 'REG', 3, 20, 20, 0, 0, 0, 1200000.00, 'VND', 'OPN', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('66660002-0001-0000-0000-000000000004', '55555555-0005-0000-0000-000000000002', 'SE', 'REG', 4, 20, 20, 0, 0, 0, 1200000.00, 'VND', 'OPN', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0);

-- Hạng NG
INSERT INTO departure_inventory_buckets
(inventory_bucket_id, departure_id, seat_class_code, quota_code, bucket_no,
 total_quantity, available_quantity, reserved_quantity, sold_quantity, oversell_limit,
 fare_amount, currency_code, sale_status,
 created_date, last_modified_date, created_by, last_modified_by, version)
VALUES
    ('66660002-0002-0000-0000-000000000001', '55555555-0005-0000-0000-000000000002', 'NG', 'REG', 1, 28, 28, 0, 0, 0, 800000.00, 'VND', 'OPN', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('66660002-0002-0000-0000-000000000002', '55555555-0005-0000-0000-000000000002', 'NG', 'REG', 2, 28, 28, 0, 0, 0, 800000.00, 'VND', 'OPN', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('66660002-0002-0000-0000-000000000003', '55555555-0005-0000-0000-000000000002', 'NG', 'REG', 3, 28, 28, 0, 0, 0, 800000.00, 'VND', 'OPN', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('66660002-0002-0000-0000-000000000004', '55555555-0005-0000-0000-000000000002', 'NG', 'REG', 4, 28, 28, 0, 0, 0, 800000.00, 'VND', 'OPN', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0);

-- Hạng GH
INSERT INTO departure_inventory_buckets
(inventory_bucket_id, departure_id, seat_class_code, quota_code, bucket_no,
 total_quantity, available_quantity, reserved_quantity, sold_quantity, oversell_limit,
 fare_amount, currency_code, sale_status,
 created_date, last_modified_date, created_by, last_modified_by, version)
VALUES
    ('66660002-0003-0000-0000-000000000001', '55555555-0005-0000-0000-000000000002', 'GH', 'REG', 1, 40, 40, 0, 0, 0, 500000.00, 'VND', 'OPN', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('66660002-0003-0000-0000-000000000002', '55555555-0005-0000-0000-000000000002', 'GH', 'REG', 2, 40, 40, 0, 0, 0, 500000.00, 'VND', 'OPN', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('66660002-0003-0000-0000-000000000003', '55555555-0005-0000-0000-000000000002', 'GH', 'REG', 3, 40, 40, 0, 0, 0, 500000.00, 'VND', 'OPN', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('66660002-0003-0000-0000-000000000004', '55555555-0005-0000-0000-000000000002', 'GH', 'REG', 4, 40, 40, 0, 0, 0, 500000.00, 'VND', 'OPN', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0);

-- ── SE3-20260601 (HAN → DAN) ────────────────────────────────
-- Hạng SE
INSERT INTO departure_inventory_buckets
(inventory_bucket_id, departure_id, seat_class_code, quota_code, bucket_no,
 total_quantity, available_quantity, reserved_quantity, sold_quantity, oversell_limit,
 fare_amount, currency_code, sale_status,
 created_date, last_modified_date, created_by, last_modified_by, version)
VALUES
    ('66660003-0001-0000-0000-000000000001', '55555555-0005-0000-0000-000000000003', 'SE', 'REG', 1, 20, 20, 0, 0, 0, 650000.00, 'VND', 'OPN', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('66660003-0001-0000-0000-000000000002', '55555555-0005-0000-0000-000000000003', 'SE', 'REG', 2, 20, 20, 0, 0, 0, 650000.00, 'VND', 'OPN', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('66660003-0001-0000-0000-000000000003', '55555555-0005-0000-0000-000000000003', 'SE', 'REG', 3, 20, 20, 0, 0, 0, 650000.00, 'VND', 'OPN', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('66660003-0001-0000-0000-000000000004', '55555555-0005-0000-0000-000000000003', 'SE', 'REG', 4, 20, 20, 0, 0, 0, 650000.00, 'VND', 'OPN', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0);

-- Hạng NG
INSERT INTO departure_inventory_buckets
(inventory_bucket_id, departure_id, seat_class_code, quota_code, bucket_no,
 total_quantity, available_quantity, reserved_quantity, sold_quantity, oversell_limit,
 fare_amount, currency_code, sale_status,
 created_date, last_modified_date, created_by, last_modified_by, version)
VALUES
    ('66660003-0002-0000-0000-000000000001', '55555555-0005-0000-0000-000000000003', 'NG', 'REG', 1, 28, 28, 0, 0, 0, 420000.00, 'VND', 'OPN', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('66660003-0002-0000-0000-000000000002', '55555555-0005-0000-0000-000000000003', 'NG', 'REG', 2, 28, 28, 0, 0, 0, 420000.00, 'VND', 'OPN', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('66660003-0002-0000-0000-000000000003', '55555555-0005-0000-0000-000000000003', 'NG', 'REG', 3, 28, 28, 0, 0, 0, 420000.00, 'VND', 'OPN', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('66660003-0002-0000-0000-000000000004', '55555555-0005-0000-0000-000000000003', 'NG', 'REG', 4, 28, 28, 0, 0, 0, 420000.00, 'VND', 'OPN', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0);

-- Hạng GH
INSERT INTO departure_inventory_buckets
(inventory_bucket_id, departure_id, seat_class_code, quota_code, bucket_no,
 total_quantity, available_quantity, reserved_quantity, sold_quantity, oversell_limit,
 fare_amount, currency_code, sale_status,
 created_date, last_modified_date, created_by, last_modified_by, version)
VALUES
    ('66660003-0003-0000-0000-000000000001', '55555555-0005-0000-0000-000000000003', 'GH', 'REG', 1, 40, 40, 0, 0, 0, 260000.00, 'VND', 'OPN', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('66660003-0003-0000-0000-000000000002', '55555555-0005-0000-0000-000000000003', 'GH', 'REG', 2, 40, 40, 0, 0, 0, 260000.00, 'VND', 'OPN', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('66660003-0003-0000-0000-000000000003', '55555555-0005-0000-0000-000000000003', 'GH', 'REG', 3, 40, 40, 0, 0, 0, 260000.00, 'VND', 'OPN', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('66660003-0003-0000-0000-000000000004', '55555555-0005-0000-0000-000000000003', 'GH', 'REG', 4, 40, 40, 0, 0, 0, 260000.00, 'VND', 'OPN', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0);

-- ── SE4-20260601 (DAN → SGN) ────────────────────────────────
-- Hạng SE
INSERT INTO departure_inventory_buckets
(inventory_bucket_id, departure_id, seat_class_code, quota_code, bucket_no,
 total_quantity, available_quantity, reserved_quantity, sold_quantity, oversell_limit,
 fare_amount, currency_code, sale_status,
 created_date, last_modified_date, created_by, last_modified_by, version)
VALUES
    ('66660004-0001-0000-0000-000000000001', '55555555-0005-0000-0000-000000000004', 'SE', 'REG', 1, 20, 20, 0, 0, 0, 700000.00, 'VND', 'OPN', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('66660004-0001-0000-0000-000000000002', '55555555-0005-0000-0000-000000000004', 'SE', 'REG', 2, 20, 20, 0, 0, 0, 700000.00, 'VND', 'OPN', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('66660004-0001-0000-0000-000000000003', '55555555-0005-0000-0000-000000000004', 'SE', 'REG', 3, 20, 20, 0, 0, 0, 700000.00, 'VND', 'OPN', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('66660004-0001-0000-0000-000000000004', '55555555-0005-0000-0000-000000000004', 'SE', 'REG', 4, 20, 20, 0, 0, 0, 700000.00, 'VND', 'OPN', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0);

-- Hạng NG
INSERT INTO departure_inventory_buckets
(inventory_bucket_id, departure_id, seat_class_code, quota_code, bucket_no,
 total_quantity, available_quantity, reserved_quantity, sold_quantity, oversell_limit,
 fare_amount, currency_code, sale_status,
 created_date, last_modified_date, created_by, last_modified_by, version)
VALUES
    ('66660004-0002-0000-0000-000000000001', '55555555-0005-0000-0000-000000000004', 'NG', 'REG', 1, 28, 28, 0, 0, 0, 450000.00, 'VND', 'OPN', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('66660004-0002-0000-0000-000000000002', '55555555-0005-0000-0000-000000000004', 'NG', 'REG', 2, 28, 28, 0, 0, 0, 450000.00, 'VND', 'OPN', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('66660004-0002-0000-0000-000000000003', '55555555-0005-0000-0000-000000000004', 'NG', 'REG', 3, 28, 28, 0, 0, 0, 450000.00, 'VND', 'OPN', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('66660004-0002-0000-0000-000000000004', '55555555-0005-0000-0000-000000000004', 'NG', 'REG', 4, 28, 28, 0, 0, 0, 450000.00, 'VND', 'OPN', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0);

-- Hạng GH
INSERT INTO departure_inventory_buckets
(inventory_bucket_id, departure_id, seat_class_code, quota_code, bucket_no,
 total_quantity, available_quantity, reserved_quantity, sold_quantity, oversell_limit,
 fare_amount, currency_code, sale_status,
 created_date, last_modified_date, created_by, last_modified_by, version)
VALUES
    ('66660004-0003-0000-0000-000000000001', '55555555-0005-0000-0000-000000000004', 'GH', 'REG', 1, 40, 40, 0, 0, 0, 280000.00, 'VND', 'OPN', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('66660004-0003-0000-0000-000000000002', '55555555-0005-0000-0000-000000000004', 'GH', 'REG', 2, 40, 40, 0, 0, 0, 280000.00, 'VND', 'OPN', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('66660004-0003-0000-0000-000000000003', '55555555-0005-0000-0000-000000000004', 'GH', 'REG', 3, 40, 40, 0, 0, 0, 280000.00, 'VND', 'OPN', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('66660004-0003-0000-0000-000000000004', '55555555-0005-0000-0000-000000000004', 'GH', 'REG', 4, 40, 40, 0, 0, 0, 280000.00, 'VND', 'OPN', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0);

-- rollback DELETE FROM departure_inventory_buckets WHERE departure_id IN ('55555555-0005-0000-0000-000000000001','55555555-0005-0000-0000-000000000002','55555555-0005-0000-0000-000000000003','55555555-0005-0000-0000-000000000004');
-- rollback DELETE FROM train_departures WHERE departure_id IN ('55555555-0005-0000-0000-000000000001','55555555-0005-0000-0000-000000000002','55555555-0005-0000-0000-000000000003','55555555-0005-0000-0000-000000000004');
