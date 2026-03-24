# Redis Template

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-brightgreen)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)
![Redis](https://img.shields.io/badge/Redis-8-red)
![Liquibase](https://img.shields.io/badge/Liquibase-migrations-blueviolet)
![Tests](https://img.shields.io/badge/tests-Testcontainers-informational)

Небольшой пет-проект на Spring Boot про кэширование данных через Redis. Это короткий законченный backend-кейс: с базой данных, кэшем, миграциями, обработкой ошибок и интеграционными тестами.

## О проекте

В основе проекта простая задача: хранить текстовый контент в PostgreSQL и ускорять чтение по `id` через Redis.

Сценарий работы такой:
- `GET /posts/{id}` сначала ищет запись в Redis
- если в кэше ничего нет, приложение читает данные из PostgreSQL
- после чтения из базы сохраняет результат в Redis
- при `PUT` и `DELETE` кэш по этому `id` удаляется

Стратегия `cache-aside` в прямой и понятной реализации, без `@Cacheable` и без скрытой логики.

## Что здесь показано

Проект демонстрирует несколько важных базовых вещей:
- как вручную реализовать `cache-aside` поверх Redis
- как держать PostgreSQL источником истины
- как инвалидировать кэш после изменения данных
- как вести схему базы через Liquibase
- как проверять поведение приложения интеграционными тестами с Testcontainers

## Стек

- Java 21
- Spring Boot 4
- Spring Web
- Spring Data JPA
- Spring Data Redis
- PostgreSQL
- Liquibase
- Docker Compose
- Testcontainers
- JUnit 5

## Что реализовано

- CRUD API для `Post`
- чтение `GET /posts/{id}` через `cache-aside`
- TTL для записей в Redis
- ручная инвалидация кэша при `PUT` и `DELETE`
- миграции схемы базы
- корректные HTTP-ответы для основных ошибок
- интеграционные тесты с автоматическим поднятием PostgreSQL и Redis в контейнерах

## Как устроен поток запроса

```text
Client
  -> REST Controller
  -> Service
  -> Redis
      -> hit  -> вернуть ответ
      -> miss -> PostgreSQL
                -> сохранить в Redis
                -> вернуть ответ
```

Ключ кэша:

```text
post:id:{id}
```

## Структура проекта

```text
src/main/java/ru/luterel/template
├── api
│   ├── GlobalExceptionHandler.java
│   ├── PostController.java
│   └── dto
├── application
│   └── PostService.java
├── domain
│   └── PostEntity.java
└── infrastructure
    ├── cache
    │   ├── PostCacheService.java
    │   └── RedisPostCacheService.java
    └── persistence
        └── PostRepository.java
```

## API

### Создание поста

`POST /posts`

Пример тела запроса:

```json
{
  "title": "First post",
  "body": "Hello",
  "status": "DRAFT",
  "tags": "java,spring,redis"
}
```

### Получение поста по id

`GET /posts/{id}`

### Обновление поста

`PUT /posts/{id}`

Пример тела запроса:

```json
{
  "title": "Updated post",
  "body": "Updated body",
  "status": "PUBLISHED",
  "tags": "updated,redis"
}
```

### Удаление поста

`DELETE /posts/{id}`

## Локальный запуск

Сначала нужно поднять PostgreSQL и Redis:

```powershell
docker compose up -d
```

После этого можно запустить приложение:

```powershell
.\mvnw.cmd spring-boot:run
```

Используемые переменные окружения:

```env
APP_PORT=8080

DB_HOST=localhost
DB_PORT=5432
DB_NAME=redis_template
DB_USER=postgres
DB_PASSWORD=postgres

REDIS_HOST=localhost
REDIS_PORT=6379
```

## Тесты

В проекте есть три интеграционных теста. Они запускают приложение вместе с временными контейнерами PostgreSQL и Redis через Testcontainers и проверяют поведение системы целиком.

Запуск тестов:

```powershell
.\mvnw.cmd test
```

Сценарии проверки следующие.

### 1. Кэш заполняется после первого чтения

Тест `getCachedPostInRedis` делает следующее:
- создает пост через `POST /posts`
- проверяет, что ключа `post:id:{id}` в Redis еще нет
- вызывает `GET /posts/{id}`
- проверяет, что после первого чтения ключ появился в Redis
- делает второй `GET /posts/{id}` и убеждается, что ответ корректный

Это базовая проверка того, что кэш действительно работает.

### 2. Удаление очищает кэш и делает запись недоступной

Тест `deleteRemovesCacheAndPostBecomesUnavailable` работает так:
- создает пост через `POST /posts`
- прогревает кэш через `GET /posts/{id}`
- убеждается, что ключ в Redis существует
- выполняет `DELETE /posts/{id}`
- проверяет, что ключ из Redis удален
- после этого делает `GET /posts/{id}`
- убеждается, что API возвращает `404`

Этот тест нужен, чтобы убедиться, что после удаления приложение не хранит и не отдает устаревшие данные.


### 3. Обновление инвалидирует кэш и собирает его заново

Тест `updateInvalidatesCacheAndNextGetRebuildsIt` проверяет такой путь:
- создает пост через `POST /posts`
- вызывает `GET /posts/{id}`, чтобы прогреть кэш
- убеждается, что ключ в Redis существует
- выполняет `PUT /posts/{id}` с новыми данными
- проверяет, что после обновления ключ удален
- снова вызывает `GET /posts/{id}`
- убеждается, что пришли уже обновленные данные
- и что кэш после этого был заполнен заново

То есть здесь проверяется и инвалидация, и повторное построение кэша.

## Зачем этот проект

Этот проект нужен для того, чтобы понять, как приложение ведет себя вокруг кэша. И здесь собран минимальный сценарий:
- есть база данных как основной их источник
- есть внешний кэш
- есть понятное правило, когда кэш читается
- есть понятное правило, когда кэш должен быть удален
- есть автоматическая проверка этих сценариев

Это шаблонный проект для первого этапа более крупной архитектуры. Сейчас он покрывает базовый сценарий кэширования и ручной инвалидации.

## Что дальше

Текущая версия закрывает базовый этап и служит основой для следующего шага:
- мульти-модульная архитектура
- разделение cached / non-cached приложений
- versioned keys для page/search сценариев
- warming и метрики
