INSERT INTO admins (id, email, name, password_hash, created_at, updated_at) VALUES
    (1, 'admin@test.test', 'Артём', '$2a$10$RahuMXezTSTvHwgMouDiPuZWeGeGLW5eGuHwRImJ7UHWJPprV5DOq', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'manager@test.test', 'Игорь', '$2a$10$RahuMXezTSTvHwgMouDiPuZWeGeGLW5eGuHwRImJ7UHWJPprV5DOq', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, 'staff@test.test', 'Мария', '$2a$10$RahuMXezTSTvHwgMouDiPuZWeGeGLW5eGuHwRImJ7UHWJPprV5DOq', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (4, 'support@test.test', 'Дмитрий', '$2a$10$RahuMXezTSTvHwgMouDiPuZWeGeGLW5eGuHwRImJ7UHWJPprV5DOq', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO coworkings (
    id, name, description, address, working_hours_label, hero_title, hero_text,
    image_urls_json, image_file_ids_json, schedule, owner_id, auto_approve_membership,
    floor_map_enabled, join_token, is_active, archived, configuration_version,
    archived_at, created_at, updated_at
) VALUES
    (1, 'Волга Хаб', 'Городской коворкинг в деловом квартале Нижнего Новгорода.', 'Нижний Новгород, улица Новая, 2к1', 'Пн–Вс, 08:00–22:00', 'Рабочие места, переговорные и тихие кабинеты у метро Горьковская', 'Дневные рабочие места, переговорные, сервисные заявки и внутренний баланс.', '["https://images.unsplash.com/photo-1497366754035-f200968a6e72?auto=format&fit=crop&w=1600&q=80"]', '[]', 127, 1, TRUE, TRUE, '11111111-1111-1111-1111-111111111111', TRUE, FALSE, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'Северный квартал', 'Современный коворкинг в центре Санкт-Петербурга.', 'Санкт-Петербург, Лиговский проспект, 74', 'Пн–Сб, 09:00–21:00', 'Пространство для спокойной работы и небольших команд', 'Переговорные комнаты, стабильный интернет и зоны для сосредоточенной работы.', '["https://images.unsplash.com/photo-1497366811353-6870744d04b2?auto=format&fit=crop&w=1600&q=80"]', '[]', 63, 2, FALSE, TRUE, '22222222-2222-2222-2222-222222222222', TRUE, FALSE, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, 'Урал Точка', 'Деловое пространство в Екатеринбурге для командных встреч.', 'Екатеринбург, улица Малышева, 51', 'Пн–Пт, 10:00–20:00', 'Переговорные и командные комнаты в центре Екатеринбурга', 'Формат для встреч, презентаций и совместной работы небольших команд.', '["https://images.unsplash.com/photo-1497366412874-3415097a27e7?auto=format&fit=crop&w=1600&q=80"]', '[]', 31, 1, FALSE, TRUE, '33333333-3333-3333-3333-333333333333', TRUE, FALSE, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO tariffs (
    id, coworking_id, name, price_per_day, full_refund_hours_before,
    late_cancellation_refund_percent, cancellation_compensation_coefficient,
    day_closure_compensation_coefficient, membership_block_compensation_coefficient,
    tariff_version, active, archived, archived_at, created_at, updated_at
) VALUES
    (1, 1, 'Рабочий день', 100000, 24, 50, 1.0000, 1.0000, 1.0000, 1, TRUE, FALSE, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 1, 'Переговорная день', 280000, 24, 50, 1.0000, 1.0000, 1.0000, 1, TRUE, FALSE, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, 2, 'Рабочий день', 120000, 24, 50, 1.0000, 1.0000, 1.0000, 1, TRUE, FALSE, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (4, 3, 'Командная комната', 350000, 24, 50, 1.0000, 1.0000, 1.0000, 1, TRUE, FALSE, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO place_types (id, coworking_id, tariff_id, name, active, archived, archived_at, created_at, updated_at) VALUES
    (1, 1, 1, 'Рабочее место', TRUE, FALSE, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 1, 2, 'Переговорная', TRUE, FALSE, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, 2, 3, 'Рабочее место', TRUE, FALSE, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (4, 3, 4, 'Командная комната', TRUE, FALSE, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO floors (id, coworking_id, name, floor_index, image_file_id, active, archived, archived_at, created_at, updated_at) VALUES
    (1, 1, 'Открытая зона', 1, NULL, TRUE, FALSE, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 1, 'Тихая зона', 2, NULL, TRUE, FALSE, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, 2, 'Основной этаж', 1, NULL, TRUE, FALSE, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (4, 3, 'Переговорный этаж', 1, NULL, TRUE, FALSE, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO places (id, coworking_id, floor_id, place_type_id, name, loc_x, loc_y, image_file_id, amenities_raw, active, archived, archived_at, created_at, updated_at) VALUES
    (1, 1, 1, 1, 'A-01', 20.0000, 30.0000, NULL, 'Монитор, розетка', TRUE, FALSE, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 1, 1, 1, 'A-02', 35.0000, 30.0000, NULL, 'Розетка', TRUE, FALSE, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, 1, 2, 1, 'B-01', 25.0000, 40.0000, NULL, 'Тихая зона', TRUE, FALSE, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (4, 1, 2, 2, 'Переговорная Нева', 60.0000, 45.0000, NULL, 'Доска, проектор', TRUE, FALSE, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (5, 2, 3, 3, 'S-01', 30.0000, 35.0000, NULL, 'Розетка', TRUE, FALSE, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (6, 3, 4, 4, 'U-Meeting', 45.0000, 45.0000, NULL, 'Проектор', TRUE, FALSE, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO roles (id, coworking_id, name, grants_raw, is_active, created_at, updated_at) VALUES
    (1, 1, 'Финансовый менеджер', 'ACCESS_EDIT,ACCESS_READ,BOOKING_READ,COWORKING_EDIT,COWORKING_READ,FLOOR_EDIT,FLOOR_READ,PLACE_EDIT,PLACE_READ,PLACE_TYPE_EDIT,PLACE_TYPE_READ,ROLE_EDIT,ROLE_READ,SCHEDULE_EDIT,SCHEDULE_READ,SERVICE_REQUEST_TYPE_EDIT,SERVICE_REQUEST_TYPE_READ,TARIFF_EDIT,TARIFF_READ,USER_READ', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 1, 'Офис-менеджер', 'ACCESS_READ,BOOKING_READ,COWORKING_READ,FLOOR_READ,PLACE_READ,PLACE_TYPE_READ,ROLE_READ,SCHEDULE_READ,SERVICE_REQUEST_TYPE_READ,TARIFF_READ,USER_READ', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO access (id, admin_id, coworking_id, role_id, is_active, created_at, updated_at) VALUES
    (1, 3, 1, 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 4, 1, 2, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO service_request_types (id, coworking_id, name, cost, type_version, active, archived, archived_at, created_at, updated_at) VALUES
    (1, 1, 'Замена воды в кулере', 0, 1, TRUE, FALSE, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 1, 'Подготовка переговорной', 50000, 1, TRUE, FALSE, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, 1, 'Печать документов', 15000, 1, TRUE, FALSE, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

SELECT setval(pg_get_serial_sequence('admins', 'id'), (SELECT MAX(id) FROM admins));
SELECT setval(pg_get_serial_sequence('coworkings', 'id'), (SELECT MAX(id) FROM coworkings));
SELECT setval(pg_get_serial_sequence('tariffs', 'id'), (SELECT MAX(id) FROM tariffs));
SELECT setval(pg_get_serial_sequence('place_types', 'id'), (SELECT MAX(id) FROM place_types));
SELECT setval(pg_get_serial_sequence('floors', 'id'), (SELECT MAX(id) FROM floors));
SELECT setval(pg_get_serial_sequence('places', 'id'), (SELECT MAX(id) FROM places));
SELECT setval(pg_get_serial_sequence('roles', 'id'), (SELECT MAX(id) FROM roles));
SELECT setval(pg_get_serial_sequence('access', 'id'), (SELECT MAX(id) FROM access));
SELECT setval(pg_get_serial_sequence('service_request_types', 'id'), (SELECT MAX(id) FROM service_request_types));
