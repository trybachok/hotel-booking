# hotel-booking

Распределённый REST API бронирования отелей: Eureka, API Gateway, Booking Service и Hotel Service.

Сейчас работают реестр, шлюз с проверкой JWT и traceId, учётные записи, каталог отелей и сага бронирования. Каждый сервис — отдельное приложение Spring Boot 3.5 на Java 17. Booking и Hotel поднимают свою in-memory H2 и сами проверяют JWT.

## Модули

| Модуль | Порт | Назначение |
| --- | --- | --- |
| `eureka-server` | 8761 | Реестр сервисов |
| `api-gateway` | 8080 | Маршрутизация. Внутренние `confirm-availability` и `release` наружу не отдаются |
| `booking-service` | 8081 | Пользователи и брони. Своя база `booking` |
| `hotel-service` | 8082 | Отели и номера. Своя база `hotel` |
| `common` | — | Общий `ErrorDTO` |
| `common-webmvc` | — | Единый обработчик ошибок для servlet-сервисов |

## Требования к окружению

- JDK 17 или новее
- Maven 3.8 или новее

Проверка версий: `java -version` и `mvn -version`.

## Сборка и тесты

Из корня репозитория:

```bash
mvn -B -ntp verify
```

Команда компилирует модули, запускает тесты и проверяет формат Google Java Format.

## Запуск

Четыре процесса, сначала реестр:

```bash
mvn -pl eureka-server spring-boot:run
mvn -pl booking-service spring-boot:run
mvn -pl hotel-service spring-boot:run
mvn -pl api-gateway spring-boot:run
```

Проверка регистрации и здоровья:

```bash
curl http://localhost:8761/eureka/apps
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8080/actuator/health
```

Неизвестный путь возвращает общий `ErrorDTO`:

```bash
curl http://localhost:8081/missing
```

OpenAPI:

- Booking: http://localhost:8081/swagger-ui.html
- Hotel: http://localhost:8082/swagger-ui.html
- Gateway: http://localhost:8080/swagger-ui.html

Публичные префиксы шлюза: `/user/**`, `/booking/**`, `/bookings/**`, `/api/bookings/**` идут в Booking; `/api/hotels/**` и `/api/rooms/**` идут в Hotel.

## Учётные записи

Токен HS256 живёт один час. В нём есть `sub`, `role` и `uid`. Booking выпускает токен, Booking и Hotel проверяют подпись и срок сами. Роли: `USER` и `ADMIN`.

В базе заранее есть администратор `admin` / `admin12345`. Это локальный пароль для разработки.

Регистрация создаёт пользователя с ролью `USER` и сразу возвращает токен:

```bash
curl -s -X POST http://localhost:8080/user/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"ada","password":"password1"}'
```

Вход:

```bash
curl -s -X POST http://localhost:8080/user/auth \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin12345"}'
```

Дальше заголовок `Authorization: Bearer <token>`.

| Операция | Кто |
| --- | --- |
| `POST /user/register` | без токена |
| `POST /user/auth` | без токена |
| `POST /user` | ADMIN, тело `username`, `password`, `role` |
| `PATCH /user` | ADMIN, тело `id`, `username`, `role`, необязательный `password` |
| `DELETE /user?id=` | ADMIN |

Запрос без токена получает `401`. Токен есть, но роль не подходит — `403`. Оба ответа в формате `ErrorDTO`.

## Бронирование

`POST /booking` создаёт бронь в статусе `PENDING`, затем вызывает Hotel. Успех переводит её в `CONFIRMED`. Отказ Hotel оставляет `CANCELLED` без снятия слота, потому что слот не был занят. Таймаут или ошибка сервиса после повторов снимает слот через `release` и тоже ставит `CANCELLED`. Повтор того же `requestId` возвращает уже созданную бронь.

`autoSelect: true` игнорирует `roomId` и берёт номера из рекомендации. `startDate` раньше `endDate` и не в прошлом. `GET /bookings?page&size` — только свои брони. `DELETE /booking/{id}` отменяет свою бронь.

## Отели и номера

`available` означает, что номер не в ремонте. Занятость по датам — это временная блокировка. День выезда в интервал не входит. Список свободных номеров сортируется по id. Рекомендация — те же номера по возрастанию `times_booked`, при равенстве по id.

| Операция | Кто |
| --- | --- |
| `POST /api/hotels`, `PATCH /api/hotels/{id}`, `DELETE /api/hotels/{id}` | ADMIN |
| `GET /api/hotels` | USER, фильтр `name` |
| `POST /api/rooms`, `PATCH /api/rooms/{id}`, `DELETE /api/rooms/{id}` | ADMIN |
| `GET /api/rooms?startDate&endDate` | USER, необязательный `hotelId` |
| `GET /api/rooms/recommend?startDate&endDate` | USER |
| `GET /api/rooms/statistics` | ADMIN |
| `POST /api/rooms/{id}/confirm-availability` | только прямой вызов сервиса, не через шлюз |
| `POST /api/rooms/{id}/release` | только прямой вызов сервиса, не через шлюз |

Повтор того же `requestId` не увеличивает `times_booked` второй раз. Повторный `release` ничего не меняет. В базе заранее есть отели «Астория» и «Нева».

## Шлюз

Защищённые пути требуют `Authorization: Bearer`. Шлюз проверяет подпись и срок токена до передачи запроса. Регистрация, вход, health и Swagger остаются открытыми. Если заголовок `traceId` не передан, шлюз создаёт его и прокидывает в сервис и в ответ. `confirm-availability` и `release` по-прежнему отвечают 404.
