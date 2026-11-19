# Redis Chat API

A Spring Boot REST API for creating chat rooms, joining participants, sending messages, retrieving recent history, and receiving live room events over Server-Sent Events (SSE). Redis stores room metadata in hashes, participants in sets, and messages in lists; Redis Pub/Sub distributes new messages to application instances.

## Live Link
- Base URL: https://chat-app-4vmj.onrender.com/
- Swagger-UI URL: https://chat-app-4vmj.onrender.com/swagger-ui/index.html

## Requirements

- Java 17 or newer
- Redis 6 or newer

### Start Redis locally with Docker

Redis runs separately from the application. Install and start [Docker Desktop](https://www.docker.com/products/docker-desktop/), then run this once in PowerShell:

```powershell
docker run -d --name chatapp-redis -p 127.0.0.1:6379:6379 redis:7
```

This downloads the Redis image if needed and starts a Redis container on `localhost:6379`. Check that it is ready:

```powershell
docker exec chatapp-redis redis-cli ping
```

The response should be `PONG`. If port 6379 is already in use, use the existing Redis server instead, or choose another host port, for example `-p 127.0.0.1:6380:6379`, and set `REDIS_PORT` to `6380` when starting the app.

On subsequent runs, start the existing container with `docker start chatapp-redis`; stop it with `docker stop chatapp-redis`. Data is not guaranteed to survive container removal with this setup.

Alternatively, if Redis is already installed locally, start it using `redis-server` and verify it with `redis-cli ping`. If a Redis server is already running on the configured host and port, you do not need to start another one.

### Start the application

With Redis running, launch the application:

```powershell
.\mvnw.cmd spring-boot:run
```

By default the app connects to `localhost:6379`. To use another Redis instance, set `REDIS_HOST` and `REDIS_PORT` in the same PowerShell window before starting the app:

```powershell
$env:REDIS_HOST = "localhost"
$env:REDIS_PORT = "6380"
.\mvnw.cmd spring-boot:run
```

The `localhost:6379` values in `application.properties` are fallback defaults for local testing, not production credentials. For deployment, configure `SPRING_DATA_REDIS_URL` in the hosting service's environment with the provider's connection URL. Spring Boot reads this variable automatically and uses it instead of the host and port defaults. If the provider requires TLS (`rediss://`), also set `SPRING_DATA_REDIS_SSL_ENABLED=true`. Keep connection URLs and credentials out of source control.

### IntelliJ IDEA Java version

The Maven build targets Java 17 and also works with a newer JDK, such as JDK 23. If IntelliJ reports `release version 5 not supported`:

1. Open `pom.xml` as a Maven project, then use **Reload All Maven Projects** in the Maven tool window.
2. Under **File > Project Structure**, set the project SDK to JDK 17 or newer. Under **Modules**, set the language level to 17 or inherit it from the project.
3. Under **Settings > Build, Execution, Deployment > Compiler > Java Compiler**, set the project and any `chatapp` module bytecode targets to 17 (or remove a stale module override).
4. Rebuild the project. To check the Maven build independently, run `.\mvnw.cmd test`.

## API

All endpoints are under `/api/chatapp/chatrooms`.

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/` | Create a room: `{"roomName":"general"}` |
| `POST` | `/{roomId}/join` | Join a room: `{"participant":"guest_user"}` |
| `POST` | `/{roomId}/messages` | Send a message: `{"participant":"guest_user","message":"Hello!"}` |
| `GET` | `/{roomId}/messages?limit=10` | Get the most recent messages (default 50, maximum 100) |
| `GET` | `/{roomId}/events` | Subscribe to live messages using SSE |

Join a room before sending messages. Room names and participant names may contain letters, numbers, underscores, and hyphens; participant names may also contain dots. Message text is limited to 2,000 characters.

The create and join endpoints return a `status` and human-readable `message`; room creation also returns `roomId`. Errors use an HTTP status code and a JSON object with `timestamp`, `status`, `error`, `message`, and `path`. Duplicate rooms return `409`, missing rooms `404`, non-members `403`, and invalid requests `400`.

### Real-time events

The events endpoint keeps an HTTP connection open and streams new messages for one room. To try it locally, start Redis and the application, then use two PowerShell windows:

1. In the first window, create a room and join it:

   ```powershell
   $base = "BASE_URL/api/chatapp/chatrooms"
   Invoke-RestMethod -Method Post -Uri $base -ContentType "application/json" `
     -Body (@{ roomName = "general" } | ConvertTo-Json)
   Invoke-RestMethod -Method Post -Uri "$base/general/join" -ContentType "application/json" `
     -Body (@{ participant = "guest_user" } | ConvertTo-Json)
   ```

   If `general` already exists, skip creation or use another room name throughout the example.

2. In the second window, subscribe and leave the command running:

   ```powershell
   curl.exe -N BASE_URL/api/chatapp/chatrooms/general/events
   ```

   The stream can remain blank until a message arrives. Multiple clients can subscribe to the same room.

3. Back in the first window, send a message:

   ```powershell
   Invoke-RestMethod -Method Post -Uri "$base/general/messages" -ContentType "application/json" `
     -Body (@{ participant = "guest_user"; message = "Hello!" } | ConvertTo-Json)
   ```

   The subscriber should receive an event like:

   ```text
   event:message
   data:{"participant":"guest_user","message":"Hello!","timestamp":"2024-01-01T10:00:00Z"}
   ```

   The timestamp is generated when the message is sent. Press Ctrl+C in the subscriber window to disconnect.

Events are transient: messages sent before subscription or while disconnected are not replayed. Retrieve recent history separately:

```powershell
Invoke-RestMethod -Uri "$base/general/messages?limit=10"
```

History returns the most recent messages in chronological order. SSE connections can be interrupted by clients or proxies; reconnect and check history after a disconnection. The stream has no authentication or replay cursor.

### Redis storage

Each room uses a Redis hash for metadata, a set for participants, a list for JSON messages, and a Pub/Sub channel for live events. Room creation and message append/publish use Redis scripts to keep each operation atomic. Messages are not automatically expired or trimmed, and the API does not currently support deleting rooms.

Messages remain in Redis while its data is retained; survival across restarts depends on the Redis provider's persistence settings. Pub/Sub itself does not persist events.

## Security and future improvements

This assignment demo focuses on the chat and Redis requirements, so API authentication (such as API keys) was not included. Participant names are supplied by clients and are not authenticated; the membership check is not a security boundary, and room history and live events are publicly accessible. Do not use the application for private or sensitive conversations in its current form.

Future improvements include authenticating users or clients, authorizing room access and actions, and adding rate limiting. API keys alone would not establish individual participant identities.

## Test

```powershell
.\mvnw.cmd test
```

The tests do not require a running Redis instance; the application context test mocks the Redis listener. Start Redis to run and manually exercise the application.

OpenAPI documentation is available at `/swagger-ui.html`.

## Code organization

The chat feature is split by responsibility under `com.example.chatapp.chat`: `controller` exposes the REST API, `dto` defines request and response types, `model` contains chat data, `service` owns business rules, `repository` handles Redis persistence, `exception` maps domain and request errors, `realtime` handles Pub/Sub and SSE, and `config` wires Redis listeners.
