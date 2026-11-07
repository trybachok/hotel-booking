# api-gateway

Единая точка входа. Порт `8080`.

## За что отвечает

Маршруты к Booking и Hotel через Eureka. Проверка подписи и срока JWT до передачи запроса. Создание и прокидывание `traceId`. Скрытие `confirm-availability` и `release`.

Открыты регистрация, вход, health и Swagger.

## Запуск

После Eureka, Booking и Hotel. Секрет подписи — тот же `APP_JWT_SECRET`.

```bash
mvn -pl api-gateway spring-boot:run
```

Проверка: `mvn -pl api-gateway -am test`
