INSERT INTO users (username, password, role)
SELECT 'demo', '$2a$10$boLeejqdPrwUMgJ5Z5.O0e08sEsAJCSRNMW9viqF629ZQc280FGtO', 'USER'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'demo');

INSERT INTO bookings (user_id, room_id, start_date, end_date, status, created_at, request_id)
SELECT u.id, 1, DATE '2026-11-01', DATE '2026-11-03', 'CONFIRMED', CURRENT_TIMESTAMP, 'seed-astoria-101'
FROM users u
WHERE u.username = 'demo'
  AND NOT EXISTS (SELECT 1 FROM bookings WHERE request_id = 'seed-astoria-101');
