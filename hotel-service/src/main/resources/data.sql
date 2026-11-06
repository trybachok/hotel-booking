INSERT INTO hotels (name, address)
SELECT 'Астория', 'Невский проспект, 39'
WHERE NOT EXISTS (SELECT 1 FROM hotels WHERE name = 'Астория');

INSERT INTO hotels (name, address)
SELECT 'Нева', 'набережная реки Мойки, 12'
WHERE NOT EXISTS (SELECT 1 FROM hotels WHERE name = 'Нева');

INSERT INTO rooms (hotel_id, number, available, times_booked)
SELECT h.id, '101', TRUE, 5 FROM hotels h
WHERE h.name = 'Астория'
  AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.hotel_id = h.id AND r.number = '101');

INSERT INTO rooms (hotel_id, number, available, times_booked)
SELECT h.id, '102', TRUE, 0 FROM hotels h
WHERE h.name = 'Астория'
  AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.hotel_id = h.id AND r.number = '102');

INSERT INTO rooms (hotel_id, number, available, times_booked)
SELECT h.id, '201', FALSE, 2 FROM hotels h
WHERE h.name = 'Астория'
  AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.hotel_id = h.id AND r.number = '201');

INSERT INTO rooms (hotel_id, number, available, times_booked)
SELECT h.id, '1', TRUE, 1 FROM hotels h
WHERE h.name = 'Нева'
  AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.hotel_id = h.id AND r.number = '1');
