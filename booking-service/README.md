# booking-service

Сервис пользователей и бронирований. Порт `8081`. Своя in-memory H2 `booking`.

## За что отвечает

Регистрация, вход, JWT на один час, роли `USER` и `ADMIN`. Создание брони в статусе `PENDING`, подтверждение номера в Hotel и перевод в `CONFIRMED` или `CANCELLED`.

## Запуск

Из корня репозитория, после Eureka и Hotel:

```bash
mvn -pl booking-service spring-boot:run
```

Проверка: `mvn -pl booking-service -am test`

## Данные

`admin` / `admin12345`, `demo` / `password1`. У `demo` есть прошедшая бронь `seed-astoria-101` на номер `1`.

Таблицы: `users`, `bookings`. Схема — `src/main/resources/schema.sql`.
