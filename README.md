# Coworking Management System

## Структура проекта

Дарья Жулитова:
* user-frontend/ - Пользовательский frontend на Next.js
* user-service/ - Пользовательский бэкенд на Spring Boot

Артём Федоренко:
* admin_frontend/ - Административный frontend на Next.js
* admin_service/ - Административный backend на Spring Boot
* deploy/ - Файлы развертывания, конфигурация контейнеров и инфраструктурные настройки

## Стек
- Frontend: Next.js, TypeScript, Bootstrap 5.
- Backend: Spring Boot, Java 21.
- База данных: PostgreSQL.
- Развертывание: Docker Compose, nginx.

## Запуск проекта

Основной запуск выполняется через папку deploy/.

```bash
cd deploy
docker compose up --build
```

## Переменные окружения

Перед запуском нужно создать .env файл для docker-compose.yml.

Переменная "SPRING_PROFILES_ACTIVE" определяет режим работы сервисов. Значение "demo" включает демонстрационный режим: при запуске база данных сбрасывается и заполняется демонстрационными данными. Значение "prod" включает обычный режим работы: при запуске выполняются только необходимые новые миграции.

### Пример .env файла
```text
SPRING_PROFILES_ACTIVE=demo

USER_FRONTEND_PORT=3000
ADMIN_FRONTEND_PORT=3001

USER_DB_NAME=userservice
USER_DB_USERNAME=userservice
USER_DB_PASSWORD=password
USER_DB_PORT=5433
ADMIN_DB_NAME=adminservice
ADMIN_DB_USERNAME=adminservice
ADMIN_DB_PASSWORD=password
ADMIN_DB_PORT=5434

USER_SERVICE_PORT=8080
ADMIN_SERVICE_PORT=8081

USER_SERVICE_BASE_URL=http://user-service:8080/api
ADMIN_SERVICE_BASE_URL=http://admin-service:8081/api

USER_BACKEND_BASE_URL=http://user-service:8080
ADMIN_BACKEND_BASE_URL=http://admin-service:8081/api

USER_FRONTEND_JOIN_BASE_URL=https://myspacebooking.ru/join

INTERNAL_API_KEY=u6wABlIy4fLuRvLa8utE1Ki9whpCvVNIVeETo8MVk7v

USER_JWT_SECRET=56tW8wpxUYIu2N4PLENIvQLXe3XJPPjtiUpKGtYHiMM
ADMIN_JWT_SECRET=wrtLxK1irX3EoOcBlpyjJ5lnmeUHtQbpq3NeQLqjGxn
USER_JWT_EXPIRATION_MS=86400000
ADMIN_JWT_EXPIRATION_MS=86400000

S3_ENDPOINT=http://127.0.0.1:9000
S3_ACCESS_KEY=username
S3_SECRET_KEY=password
S3_BUCKET=coworking
S3_REGION=us-1
S3_PRESIGNED_URL_TTL_MINUTES=60
```
