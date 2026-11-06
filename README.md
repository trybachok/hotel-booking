# hotel-booking

Распределённый REST API бронирования отелей: Eureka, API Gateway, Booking Service и Hotel Service.

Сейчас собран каркас. Бизнес-операции бронирования, JWT и сага появятся следующими этапами. Каждый сервис — отдельное приложение Spring Boot 3.5 на Java 17. Booking и Hotel поднимают свою in-memory H2.

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

Публичные префиксы шлюза уже заданы: `/user/**`, `/booking/**`, `/bookings/**`, `/api/bookings/**` идут в Booking; `/api/hotels/**` и `/api/rooms/**` идут в Hotel. Сами операции ещё не реализованы.
