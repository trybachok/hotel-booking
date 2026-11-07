# eureka-server

Реестр сервисов. Порт `8761`.

## За что отвечает

Booking, Hotel и Gateway регистрируются здесь и находят друг друга по имени. Сам реестр в список клиентов не входит.

## Запуск

Первым из четырёх процессов:

```bash
mvn -pl eureka-server spring-boot:run
```

Проверка списка: `curl http://localhost:8761/eureka/apps`
