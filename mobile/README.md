# Mobile notes

Project `mobile/` hien tai la Android native Java. Muc tieu cua cau truc moi la tach ro:

- `activities/`: man hinh UI
- `api/config/`: config endpoint
- `api/interceptor/`: tu dong gan token
- `api/services/`: Retrofit interface va client
- `api/dto/auth|common|meeting|recording|user`: model request/response theo backend

## Cau hinh backend URL

Mac dinh app dung:

```text
http://10.0.2.2:8080/
```

Dia chi nay dung cho Android Emulator de tro vao backend chay local tren may tinh.

Neu ban muon doi URL, them dong nay vao file `mobile/local.properties`:

```properties
PTITMEET_API_BASE_URL=http://192.168.1.10:8080/
```

Goi y:

- Android Emulator: dung `http://10.0.2.2:8080/`
- May that cung wifi voi may chay backend: dung IP LAN cua may tinh, vi du `http://192.168.1.10:8080/`
- Khong dung `http://localhost:8080/` tren mobile, vi `localhost` luc do la chinh thiet bi/emulator

## Backend API hien co

### Auth

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/google`
- `POST /api/auth/refresh-token`
- `POST /api/auth/logout`
- `POST /api/auth/forgot-password`
- `POST /api/auth/reset-password`

### Users

- `GET /api/users/me`
- `GET /api/users/profile`
- `PUT /api/users/profile`
- `POST /api/users/avatar`

### Meetings

- `POST /api/meetings/instant`
- `POST /api/meetings/schedule`
- `GET /api/meetings/history`
- `GET /api/meetings/up-next`
- `GET /api/meetings/my-meetings`
- `GET /api/meetings/{code}/info`
- `DELETE /api/meetings/{code}`
- `POST /api/meetings/{code}/join`
- `GET /api/meetings/{code}/waiting-room`
- `POST /api/meetings/{code}/approval`
- `GET /api/meetings/{code}/chat/history`
- `POST /api/meetings/{code}/leave`
- `POST /api/meetings/{code}/end`
- `GET /api/meetings/{code}/summary`
- `POST /api/meetings/{code}/feedback`
- `GET /api/meetings/{code}/settings`
- `PUT /api/meetings/{code}/settings`

### Recording

- `POST /api/livekit/recordings/start`
- `POST /api/livekit/recordings/stop`
- `GET /api/livekit/recordings/status`
- `GET /api/livekit/recordings/my`

## Cach goi API trong Activity

```java
ApiService apiService = RetrofitClient.getApiService(this);
```

Sau khi dang nhap thanh cong, token duoc luu trong `SessionManager` va tu dong duoc gan vao header `Authorization: Bearer ...` cho cac request can dang nhap.
