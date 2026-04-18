# Архитектура Solvare

**Solvare** — Android-приложение «холодного» кошелька Solana с обменом данными по NFC. Ниже описана техническая архитектура текущей кодовой базы.

## Обзор

Приложение построено по паттерну **MVVM** (Model–View–ViewModel): экраны на **Jetpack Compose** наблюдают состояние из **ViewModel**, бизнес-логика и доступ к сети сосредоточены в **репозиториях** и NFC-слое. Один экземпляр `WalletViewModel` обслуживает навигацию между экранами и единый поток состояния кошелька.

## Слои и поток данных

```
┌─────────────────────────────────────────────────────────────┐
│  UI: Compose Screens (HomeScreen, SendScreen)               │
└─────────────────────────────┬───────────────────────────────┘
                              │ collectAsState / callbacks
┌─────────────────────────────▼───────────────────────────────┐
│  ViewModel: WalletViewModel (единый VM, всё состояние)      │
└─────────────────────────────┬───────────────────────────────┘
                              │ вызовы suspend / Flow
┌─────────────────────────────▼───────────────────────────────┐
│  Repository: SolanaRepository, PriceRepository              │
└─────────────────────────────┬───────────────────────────────┘
                              │
              ┌───────────────┴───────────────┐
              ▼                               ▼
┌─────────────────────────┐     ┌─────────────────────────────┐
│  API: Retrofit          │     │  NFC: NfcSessionManager,    │
│  SolanaRpcApi, PriceApi │     │  NdefProtocol               │
└─────────────────────────┘     └─────────────────────────────┘
```

**Кратко:** UI → ViewModel → Repository → (HTTP API или NFC-стек Android).

## Описание модулей (пакетов)

### `ui/theme/`

Тема **Material 3** для Compose: цвета в духе бренда Solana, типографика (`Color.kt`, `Type.kt`, `Theme.kt`). Обёртка `SolvareTheme` задаёт `MaterialTheme` для всего приложения.

### `ui/screens/`

- **HomeScreen** — главный экран: баланс, курс, действия с кошельком и NFC.
- **SendScreen** — сценарий отправки SOL с участием NFC-подписи.

### `ui/components/`

Переиспользуемые композиции:

- **BalanceCard** — отображение баланса и связанной информации.
- **NfcBottomSheet** — нижняя панель для сессий NFC (ожидание тега, статус).
- **TransactionStatusDialog** — диалог статуса транзакции (успех / ошибка / ожидание).

### `viewmodel/`

- **WalletViewModel** — **единственная** ViewModel приложения: баланс, цена SOL, состояние отправки, интеграция с `NfcSessionManager` и репозиториями. Вся пользовательская логика экранов проходит через неё.

### `data/solana/`

- **SolanaRpcApi** — интерфейс Retrofit: `POST /` с телом `RpcRequest`, ответ `RpcResponse`.
- **SolanaRepository** — высокоуровневые операции: `getBalance`, `getLatestBlockhash`, `sendTransaction`, `getSignatureStatus`; разбор JSON-RPC и перевод лампортов в SOL где нужно.
- **SolanaModels** — `RpcRequest`, `RpcResponse`, `RpcError`, типы для баланса, blockhash, статусов подписи.

### `data/price/`

- **PriceApi** — Retrofit-запрос к CoinGecko `simple/price` (по умолчанию `ids=solana`, `vs_currencies=usd`).
- **PriceRepository** — получение цены с **кэшем 60 секунд**; при ошибке сети возвращается последнее удачное значение, если оно есть.

### `data/`

- **NetworkModule** — объект-синглтон: общий **OkHttpClient** (таймауты, логирование), два **Retrofit**-клиента (Solana devnet, CoinGecko), ленивая инициализация `SolanaRpcApi`, `PriceApi`, `SolanaRepository`, `PriceRepository`.

### `nfc/`

- **NfcSessionManager** — **foreground dispatch** в `Activity` (включение/выключение в жизненном цикле), **конечный автомат состояний** (`NfcState`: IDLE, WAITING_FOR_TAG, READING, WRITING, SUCCESS, ERROR), режимы `NfcMode` (чтение кошелька, запись запроса на подпись, чтение ответа).
- **NdefProtocol** — кодирование и декодирование полезной нагрузки в NDEF-сообщениях для протокола обмена с «холодным» устройством.

### `util/`

- **Base58** — кодирование и декодирование Base58 (адреса и бинарные данные в UI/логике по необходимости).
- **UiState** — запечатанный интерфейс/типы для унификации состояний загрузки/успеха/ошибки в UI.

## Зависимости (ключевые версии)

| Компонент | Версия |
|-----------|--------|
| Retrofit | 2.11 |
| OkHttp | 4.12 |
| kotlinx-serialization (JSON) | 1.7.3 |
| Compose BOM | 2024.12.01 |
| Navigation Compose | 2.8.5 |

Дополнительно: конвертер Retrofit для kotlinx-serialization, корутины Android, Material3 и иконки Compose, Lifecycle ViewModel Compose.

## Навигация

В `MainActivity` объявлен **NavHost** с двумя маршрутами:

| Маршрут | Эран |
|---------|------|
| `home` | `HomeScreen` (стартовый) |
| `send` | `SendScreen` |

Переход на отправку: `navController.navigate("send")`; возврат: `popBackStack()`.

## Потоки выполнения

- Асинхронные вызовы выполняются в **корутинах** в области `viewModelScope` (и связанных `launch`/`suspend` из ViewModel).
- Retrofit-методы объявлены как **`suspend`** — сетевой ввод-вывод не блокирует UI-поток при правильном использовании из корутин.
- NFC-колбэки и обновления состояния согласуются с менеджером сессий и StateFlow/Compose.

---

*Документ описывает состояние проекта в репозитории `com.example.solvare`.*
