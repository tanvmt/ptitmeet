# 03. Kiem Thu, Test Case Phu Trach, va Chat Luong Ma Nguon

## 1. Phan cong cua ban trong tai lieu test case

Theo `docs/test_cases.md`, phan cong cua ban la:

- **Sang**
- **Module 5: Phong hop truc tuyen (Core Room)**
- **18 test cases**

Mo ta trong tai lieu:
- kiem thu luong goi video LiveKit
- bat/tat Mic/Cam
- screen share
- chat
- reactions
- recording
- host controls: mute all, kick, end for all

## 2. 18 test case Module 5 gom nhung gi

### Nhom ket noi media

- `TC_ROOM_01`: vao phong thanh cong va hien stream
- `TC_ROOM_02`: bat/tat microphone
- `TC_ROOM_03`: bat/tat camera
- `TC_ROOM_04`: bat dau screen share
- `TC_ROOM_05`: dung screen share

### Nhom chat va sidebar

- `TC_ROOM_06`: mo/dong sidebar
- `TC_ROOM_07`: gui tin nhan chat
- `TC_ROOM_08`: thong bao chat moi khi sidebar dang dong

### Nhom recording va reactions

- `TC_ROOM_09`: start recording
- `TC_ROOM_10`: stop recording
- `TC_ROOM_11`: gui reactions

### Nhom host controls

- `TC_ROOM_12`: host duyet participant
- `TC_ROOM_13`: mute 1 participant
- `TC_ROOM_14`: mute all
- `TC_ROOM_15`: stop camera 1 participant
- `TC_ROOM_16`: kick participant
- `TC_ROOM_18`: end meeting for all

### Nhom roi phong

- `TC_ROOM_17`: participant tu leave meeting

## 3. Cach trinh bay phan test case voi thay

Khong nen doc tung dong bang test case. Nen nhom theo chuc nang:

### Layer 1: Room media hoat dong dung

- co vao duoc room
- mic/cam hoat dong
- screen share hoat dong

### Layer 2: Room collaboration hoat dong dung

- chat realtime
- thong bao chat moi
- reactions

### Layer 3: Room governance hoat dong dung

- waiting room approval
- mute/kick/end
- recording

Nhu vay thay se thay em nam duoc "pham vi kiem thu" chu khong chi hoc thuoc ma test case.

## 4. Cac loai test dang co trong repo

## Backend tests

Framework:
- JUnit 5
- Mockito

Dang test hien co:

### Service unit tests

Vi du:
- `AuthServiceTest`
- `MeetingServiceTest`
- `UserServiceTest`

Muc tieu:
- test nghiep vu
- mock repository/service ben ngoai
- assert output va side effects

### Controller tests

Vi du:
- `AuthControllerTest`
- `MeetingControllerTest`
- `RecordingControllerTest`
- `MeetingSystemControllerTest`
- `ChatControllerTest`
- `LiveKitWebhookControllerTest`
- `UserControllerTest`

Muc tieu:
- test mapping request/response
- test controller goi dung service
- test ma loi va ApiResponse

### Security/config tests

Vi du:
- `JwtAuthenticationFilterTest`

Muc tieu:
- xac minh JWT duoc doc dung tu cookie/token
- SecurityContext duoc set dung

### Exception mapping tests

Vi du:
- `GlobalExceptionHandlerTest`

Muc tieu:
- dam bao exception duoc map dung sang HTTP status, message, error code

## Frontend tests

Framework:
- Vitest
- React Testing Library

Dang test hien co:

### Routing / app shell

- `App.test.jsx`

### Auth context

- `AuthContext.test.jsx`

### Page-level UI

- `LoginPage.test.jsx`
- `SignUpPage.test.jsx`
- `ResetPassword.test.jsx`
- `SchedulePage.test.jsx`
- `WaitingRoomPage.test.jsx`

### Service tests

- `meetingService.test.js`

Muc tieu:
- xac minh route protection
- login/register/reset flow
- waiting room navigation
- join meeting payload
- history API query string

## 5. Nhung test backend quan trong de ban nho

### `MeetingServiceTest`

Day la test quan trong nhat neu thuyet trinh ve meeting flow.

No dang cover cac tinh huong tieu bieu:
- tao meeting nhanh va ap default settings
- schedule meeting reject time range sai
- `joinMeeting` tra `PENDING` khi host chua vao cho scheduled meeting
- `processParticipantApproval` tao session va gui response phe duyet

Ban co the noi:
- day la lop test nghiep vu cao nhat cua module room
- vi no dung mock de test logic ma khong phu thuoc LiveKit that

### `RecordingControllerTest`

Cover:
- start recording tra `ApiResponse` dung
- loi runtime duoc translate sang `AppException`
- stop recording tra ve `400` neu service loi

### `MeetingSystemControllerTest`

Cover:
- chi broadcast supported actions
- payload json/plain deu duoc kiem tra
- payload khong hop le thi bi bo qua

## 6. Nhung test frontend quan trong de ban nho

### `WaitingRoomPage.test.jsx`

Day la test sat nghiep vu room.

Cover:
- neu join duoc approve thi navigate sang `/meeting/{code}`
- neu join `PENDING` thi hien waiting state va mo websocket

### `meetingService.test.js`

Cover:
- join meeting gui dung payload password
- history API gui dung query string `page/size/role/status`

### `App.test.jsx`

Cover:
- guest thay landing page
- user da login bi redirect vao dashboard
- guest vao protected route bi day sang login

## 7. Quy trinh dam bao chat luong trong repo

He thong dang co 3 tang bao ve:

### Tang 1: Test case va test script tai lieu

Tai lieu:
- `docs/test_cases.md`
- `docs/test_script.md`
- `docs/test_report_template.md`

Y nghia:
- co mo ta truoc dieu kien
- co buoc test
- co expected result
- co mau report ket qua

### Tang 2: Local pre-commit hook

Theo `docs/development-workflow.md`, hook se chay:

- `cd backend && ./mvnw -Popenapi verify`
- `cd web-client && npm run lint`
- `cd web-client && npm run test:run`
- diff check de buoc commit `docs/openapi.json` va `docs/openapi.yaml` neu API doi

Y nghia:
- chan loi truoc khi day len GitHub
- khong de frontend test fail hoac swagger out-of-sync len branch

### Tang 3: GitHub Actions

CI rerun:
- backend lint
- backend tests
- frontend lint
- frontend tests
- frontend build

Y nghia:
- xac nhan lai tren moi truong doc lap
- chi khi xanh moi duoc build image va deploy

## 8. Lint, docs sync, va OpenAPI sync quan trong o diem nao

Day la diem thuyet trinh rat "co quy trinh".

- Backend co checkstyle trong `mvn validate`
- Frontend co lint
- OpenAPI docs phai dong bo voi code backend
- pre-commit se block neu swagger docs chua sync

Noi cach khac:
- repo nay khong chi viet code xong la xong
- ma co quy trinh bat buoc giu tai lieu API va chat luong code dong bo

## 9. Cach chay test trong repo

### Backend

- `cd backend && ./mvnw validate`
- `cd backend && ./mvnw test`
- `cd backend && ./mvnw -Popenapi verify`

Them:
- JaCoCo report tao tai `backend/target/site/jacoco/index.html`

### Frontend

- `cd web-client && npm install`
- `npm run lint`
- `npm run test:run`
- `npm run test`
- `npm run build`

## 10. Noi voi thay the nao ve "em da dam bao chat luong ra sao"

Ban co the noi theo mau nay:

1. Em xac dinh pham vi nghiep vu can test bang test case chi tiet.
2. Em co test script va expected result ro rang.
3. Em co unit test/backend controller test/frontend component test.
4. Em dung pre-commit hook de chan loi truoc khi commit.
5. Em dung GitHub Actions de xac nhan lai test/lint/build truoc khi deploy.
6. Em buoc Swagger/OpenAPI dong bo voi code de tranh tai lieu lech voi implementation.

## 11. 3 diem hay de chot phan kiem thu

- Phan ban duoc giao la module room, day la module kho nhat vi ket hop media, realtime va host controls.
- Repo co ca kiem thu thu cong bang test case/test script va kiem thu tu dong bang unit/component tests.
- Chat luong duoc giu boi pre-commit hook + CI + OpenAPI sync, khong phai chi test bang tay.

