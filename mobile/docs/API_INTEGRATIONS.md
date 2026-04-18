# Внешние API и сеть (Solvare)

Описание интеграций с **Solana JSON-RPC (devnet)** и **CoinGecko**, политики ошибок, кэширования и сетевого стека.

---

## Solana JSON-RPC (devnet)

### Базовый URL

```
https://api.devnet.solana.com
```

В коде задаётся константой `SolanaRepository.DEVNET_URL`; запросы идут методом **POST** на корень (`/`) с телом JSON.

### Общий формат запроса

Все вызовы используют обёртку JSON-RPC 2.0:

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "имя_метода",
  "params": [ ... ]
}
```

### Общий формат ответа (успех)

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": { ... }
}
```

### Ответ с ошибкой RPC

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "error": {
    "code": -32002,
    "message": "Текст ошибки от ноды"
  }
}
```

В `SolanaRepository` при непустом `error` выбрасывается исключение с сообщением вида `RPC error: …`.

---

### Метод: `getBalance`

**Назначение:** баланс аккаунта в лампортах.

**Пример запроса** (как в приложении — один параметр, публичный ключ base58):

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "getBalance",
  "params": ["9xQeWvG816bUx9EPjHmaT23yvVM2ZWbrrpZb9PusVFin"]
}
```

**Пример успешного ответа:**

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "context": { "slot": 123456789 },
    "value": 1500000000
  }
}
```

`value` — целое число лампортов. В репозитории оно делится на `10^9` для отображения в SOL.

---

### Метод: `getLatestBlockhash`

**Назначение:** актуальный blockhash и высота для построения транзакции.

**Пример запроса** (с конфигом `commitment: finalized`, как в коде):

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "getLatestBlockhash",
  "params": [
    { "commitment": "finalized" }
  ]
}
```

**Пример успешного ответа:**

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "context": { "slot": 123456790 },
    "value": {
      "blockhash": "EkSnNWid3dxJkZNfv7BxahHWrTj3aFR5dY9dCfoiFhEv",
      "lastValidBlockHeight": 987654321
    }
  }
}
```

---

### Метод: `sendTransaction`

**Назначение:** отправка подписанной транзакции в сеть.

**Пример запроса** (подписанная транзакция в base64 + опции, как в коде):

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "sendTransaction",
  "params": [
    "AQABAgMEBQYHCAkKCwwNDg8QERITFBUWFxgZGhscHR4fICEiIyQlJic...",
    {
      "encoding": "base64",
      "preflightCommitment": "confirmed"
    }
  ]
}
```

**Пример успешного ответа** (подпись транзакции — строка):

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": "5VERv8NMvzbJMEkV8xnrLkEaWRtN9ERL8YWJjkvnzUTV5BcuY3k5NBhXMzMNKyjkYQVW6WVa9tZrUmn2o1pWrj"
}
```

---

### Метод: `getSignatureStatuses`

**Назначение:** проверка статуса подтверждения по сигнатуре.

**Пример запроса:**

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "getSignatureStatuses",
  "params": [
    ["5VERv8NMvzbJMEkV8xnrLkEaWRtN9ERL8YWJjkvnzUTV5BcuY3k5NBhXMzMNKyjkYQVW6WVa9tZrUmn2o1pWrj"]
  ]
}
```

**Пример успешного ответа** (фрагмент; поля могут различаться в зависимости от стадии):

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "context": { "slot": 123456800 },
    "value": [
      {
        "slot": 123456799,
        "confirmations": 32,
        "err": null,
        "confirmationStatus": "finalized"
      }
    ]
  }
}
```

Если статус ещё недоступен, элемент массива `value` может быть `null` — репозиторий обрабатывает это и возвращает `null` как статус.

---

### Ошибки и таймауты

| Тип | Поведение |
|-----|-----------|
| **Ошибка RPC** | Поле `error` в JSON → исключение после парсинга в `SolanaRepository`. |
| **Сетевые ошибки** | Сбой TCP/TLS, DNS и т.д. → пробрасывается через Retrofit/OkHttp в `Result` или обработчик ошибок ViewModel. |
| **Таймауты** | В `NetworkModule` для OkHttp заданы **connect** и **read** таймауты **30 секунд** каждый. |

### Лимиты запросов (rate limits)

Публичные RPC-эндпоинты Solana часто имеют **ограничения по числу запросов** и могут отвечать ошибками или замедлением при перегрузке. Для production обычно используют выделенные провайдеры или собственную ноду. В MVP заложена осознанная экономия запросов (обновления по действию пользователя, без агрессивного polling).

---

## CoinGecko API

### URL запроса цены SOL в USD

Полный пример (как у конечной точки в браузере):

```
https://api.coingecko.com/api/v3/simple/price?ids=solana&vs_currencies=usd
```

В приложении базовый URL — `https://api.coingecko.com/`, путь и параметры задаются интерфейсом Retrofit (`GET api/v3/simple/price`).

### Формат ответа

Тело — JSON-объект с вложенным объектом для id монеты и полем валюты:

```json
{
  "solana": {
    "usd": 142.51
  }
}
```

`PriceRepository` извлекает `solana.usd` как число с плавающей точкой.

### Кэширование

- **TTL кэша:** **60 секунд** (`CACHE_TTL_MS` в `PriceRepository`).
- Повторный запрос в пределах минуты возвращает **закэшированное** значение без сети.
- При **ошибке** запроса, если в памяти есть **предыдущее успешное** значение, оно **возвращается как запасной вариант**; если кэша нет — ошибка пробрасывается наверх.

### Лимиты (бесплатный tier)

У CoinGecko на бесплатном плане типичный ориентир — порядка **~30 запросов в минуту** (точные цифры зависят от плана и документации на момент интеграции). Рекомендуется не опрашивать API чаще, чем позволяет кэш в 60 секунд, и избегать параллельных дублей одного и того же запроса без необходимости.

---

## Политика повторных попыток (retry)

Для **MVP** **автоматических повторов** запросов при сбоях **нет**: пользователь инициирует обновление (например, повторное открытие экрана или явное действие «обновить», если оно предусмотрено в UI). Это упрощает поведение и снижает риск шторма запросов к публичным API.

---

## Конфигурация сети (OkHttp)

В `NetworkModule` для обоих Retrofit-клиентов используется один `OkHttpClient` с:

- таймаутами **30 с** на соединение и чтение;
- **`HttpLoggingInterceptor`** с уровнем **`BODY`** (полезно при отладке; для release-сборок обычно понижают уровень или отключают логирование, чтобы не светить данные в логах).

---

*Документ согласован с реализацией в `NetworkModule`, `SolanaRepository`, `PriceRepository` и интерфейсами `SolanaRpcApi` / `PriceApi`.*
