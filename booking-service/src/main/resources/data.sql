INSERT INTO users (username, password, role)
SELECT 'admin', '$2a$10$zogokw7XcrLyEe.w.GJ2.OU5TFluE1VCitaWdqLmYcq/0m2h2Idt2', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin');
