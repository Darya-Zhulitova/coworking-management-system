INSERT INTO app_users (id, email, name, description, password_hash) VALUES
    (1, 'darya.zhulitova@example.com', 'Дарья Жулитова', 'Разработчик.', '$2a$10$75hBiEWenAGSqQiqWvTcLuS5HnK1Y05Zf3qLCjOJzDPPniBHlHYHq'),
    (2, 'alexey.orlov@example.com', 'Алексей Орлов', 'Дизайнер.', '$2a$10$75hBiEWenAGSqQiqWvTcLuS5HnK1Y05Zf3qLCjOJzDPPniBHlHYHq'),
    (3, 'ekaterina.smirnova@example.com', 'Екатерина Смирнова', 'Маркетолог.', '$2a$10$75hBiEWenAGSqQiqWvTcLuS5HnK1Y05Zf3qLCjOJzDPPniBHlHYHq'),
    (4, 'oleg.morozov@example.com', 'Олег Морозов', 'Фрилансер.', '$2a$10$75hBiEWenAGSqQiqWvTcLuS5HnK1Y05Zf3qLCjOJzDPPniBHlHYHq');

INSERT INTO user_memberships (id, user_id, coworking_id, status, created_at, approved_at, blocked_at) VALUES
    (1, 1, 1, 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '46 days', CURRENT_TIMESTAMP - INTERVAL '45 days', NULL),
    (2, 2, 1, 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '31 days', CURRENT_TIMESTAMP - INTERVAL '30 days', NULL),
    (3, 3, 1, 'PENDING', CURRENT_TIMESTAMP - INTERVAL '2 days', NULL, NULL),
    (4, 4, 1, 'BLOCKED', CURRENT_TIMESTAMP - INTERVAL '58 days', CURRENT_TIMESTAMP - INTERVAL '57 days', CURRENT_TIMESTAMP - INTERVAL '5 days'),
    (5, 1, 2, 'PENDING', CURRENT_TIMESTAMP - INTERVAL '8 days', NULL, NULL),
    (6, 1, 3, 'BLOCKED', CURRENT_TIMESTAMP - INTERVAL '70 days', CURRENT_TIMESTAMP - INTERVAL '69 days', CURRENT_TIMESTAMP - INTERVAL '20 days');

INSERT INTO pay_requests (id, membership_id, amount, status, user_comment, admin_comment, created_at) VALUES
    (1, 1, 1200000, 'APPROVED', 'Пополнение перед серией бронирований на май. Перевод по СБП.', 'Поступление сверено с банковской выпиской.', CURRENT_TIMESTAMP - INTERVAL '18 days'),
    (2, 2, 1800000, 'APPROVED', 'Пополнение для командных встреч на текущий месяц.', 'Оплата подтверждена администратором.', CURRENT_TIMESTAMP - INTERVAL '14 days'),
    (3, 3, 350000, 'PENDING', 'СБП.', NULL, CURRENT_TIMESTAMP - INTERVAL '9 hours'),
    (4, 4, 400000, 'REJECTED', 'Пополнение наличными через администратора.', 'Платеж не найден в кассовом журнале. Запрос отклонен до уточнения.', CURRENT_TIMESTAMP - INTERVAL '11 days');

INSERT INTO ledger_entries (id, membership_id, type, reference_id, amount, name, comment, timestamp) VALUES
    (1, 1, 'BALANCE_TOP_UP', 1, 1200000, 'Пополнение баланса по подтвержденной платежной заявке №1', NULL, CURRENT_TIMESTAMP - INTERVAL '18 days' + INTERVAL '17 minutes'),
    (2, 2, 'BALANCE_TOP_UP', 2, 1800000, 'Пополнение баланса по подтвержденной платежной заявке №2', NULL, CURRENT_TIMESTAMP - INTERVAL '14 days' + INTERVAL '9 minutes');

INSERT INTO bookings (
    id, version, place_id, membership_id, date, cost, active, status, booking_number, request_id,
    tariff_id, price_per_day, full_refund_hours_before, late_cancellation_refund_percent,
    cancellation_compensation_coefficient, day_closure_compensation_coefficient,
    membership_block_compensation_coefficient
) VALUES
    (1, 0, 1, 1, CURRENT_DATE + 1, 100000, TRUE, 'ACTUAL', 'BK-DZ-20260518', 'demo-request-1', 1, 100000, 24, 50, 1.0000, 1.0000, 1.0000),
    (2, 0, 2, 1, CURRENT_DATE + 2, 100000, TRUE, 'ACTUAL', 'BK-DZ-20260519', 'demo-request-2', 1, 100000, 24, 50, 1.0000, 1.0000, 1.0000),
    (3, 0, 4, 2, CURRENT_DATE + 3, 280000, TRUE, 'ACTUAL', 'BK-AO-20260520', 'demo-request-3', 2, 280000, 24, 50, 1.0000, 1.0000, 1.0000);

INSERT INTO ledger_entries (id, membership_id, type, reference_id, amount, name, comment, timestamp) VALUES
    (3, 1, 'BOOKING_CHARGE', 1, -100000, 'Списание за бронирование BK-DZ-20260518', NULL, CURRENT_TIMESTAMP - INTERVAL '2 days'),
    (4, 1, 'BOOKING_CHARGE', 2, -100000, 'Списание за бронирование BK-DZ-20260519', NULL, CURRENT_TIMESTAMP - INTERVAL '2 days'),
    (5, 2, 'BOOKING_CHARGE', 3, -280000, 'Списание за бронирование BK-AO-20260520', NULL, CURRENT_TIMESTAMP - INTERVAL '2 days');

INSERT INTO user_service_requests (id, membership_id, type_id, type_name, name, cost, status, created_at, resolved_at) VALUES
    (1, 1, 1, 'Бытовая заявка', 'Замена воды в кулере', 0, 'IN_PROGRESS', CURRENT_TIMESTAMP - INTERVAL '1 day', NULL),
    (2, 2, 2, 'Платная услуга', 'Подготовка переговорной', 50000, 'RESOLVED', CURRENT_TIMESTAMP - INTERVAL '3 days', CURRENT_TIMESTAMP - INTERVAL '2 days');

INSERT INTO request_messages (id, service_request_id, author_type, author_id, text, timestamp, read_at) VALUES
    (1, 1, 'USER', 1, 'Добрый день, закончилась вода в кулере в открытой зоне.', CURRENT_TIMESTAMP - INTERVAL '1 day', NULL),
    (2, 1, 'ADMIN', NULL, 'Приняли в работу.', CURRENT_TIMESTAMP - INTERVAL '20 hours', NULL),
    (3, 2, 'USER', 2, 'Нужно подготовить переговорную к встрече.', CURRENT_TIMESTAMP - INTERVAL '3 days', CURRENT_TIMESTAMP - INTERVAL '2 days'),
    (4, 2, 'SYSTEM', NULL, 'Сервисная заявка выполнена.', CURRENT_TIMESTAMP - INTERVAL '2 days', NULL);

SELECT setval(pg_get_serial_sequence('app_users', 'id'), (SELECT MAX(id) FROM app_users));
SELECT setval(pg_get_serial_sequence('user_memberships', 'id'), (SELECT MAX(id) FROM user_memberships));
SELECT setval(pg_get_serial_sequence('pay_requests', 'id'), (SELECT MAX(id) FROM pay_requests));
SELECT setval(pg_get_serial_sequence('ledger_entries', 'id'), (SELECT MAX(id) FROM ledger_entries));
SELECT setval(pg_get_serial_sequence('bookings', 'id'), (SELECT MAX(id) FROM bookings));
SELECT setval(pg_get_serial_sequence('user_service_requests', 'id'), (SELECT MAX(id) FROM user_service_requests));
SELECT setval(pg_get_serial_sequence('request_messages', 'id'), (SELECT MAX(id) FROM request_messages));
SELECT setval('booking_number_seq', 1000, TRUE);
SELECT setval('manual_adjustment_reference_seq', 1000, TRUE);
