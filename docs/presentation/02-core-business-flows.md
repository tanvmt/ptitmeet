# 02. Nghiep Vu Chinh va Cac Flow Quan Trong

## 1. Muc tieu noi trong 1-2 phut

- Nghiep vu trong PTITMeet khong chi la "vao room video".
- Phan nang nhat la `join meeting`, vi no ket hop:
  - trang thai meeting
  - waiting room
  - password/access type
  - host/owner
  - participant session
  - LiveKit token
  - WebSocket notification

## 2. Cac flow chinh cua he thong

### Flow 1: Tao meeting nhanh

Vi tri code:
- `MeetingService.createInstantMeeting(...)`

Tac dung:
- Tao ma phong
- Gan `hostId = ownerId = nguoi tao`
- Set meeting active ngay
- Gan settings mac dinh neu request thieu

### Flow 2: Schedule meeting

Vi tri code:
- `MeetingService.scheduleMeeting(...)`

Tac dung:
- Tao meeting co `startTime`, `endTime`
- Validate khoang thoi gian hop le
- Moi participant qua email
- Trang thai ban dau la `SCHEDULED`

### Flow 3: Join meeting

Vi tri code quan trong nhat:
- `backend/src/main/java/com/ptithcm/ptitmeet/services/MeetingService.java`
- ham `joinMeeting(...)`

## 3. Join meeting la flow nang nhat nhu the nao

Ban co the trinh bay theo cac buoc nay:

### Buoc 1: Tim meeting theo code

- Neu khong ton tai: bao loi `MEETING_NOT_FOUND`
- Neu meeting da `FINISHED` hoac `CANCELED`: khong cho vao

### Buoc 2: Xac dinh vai tro nguoi vao

Code phan biet:
- `isRuntimeHost`
- `isOwner`

Y nghia:
- Host hien tai duoc phep dieu phoi phien hop
- Owner goc duoc so huu data va recording

### Buoc 3: Xu ly meeting scheduled

- Neu meeting chua toi gio ma attendee vao som:
  - tra ve `PENDING`
  - thong diep cho biet hay doi host vao
- Neu host hoac owner vao:
  - co the kich hoat meeting sang `ACTIVE`

### Buoc 4: Validate chinh sach truy cap

He thong co cac che do:
- `OPEN`
- `TRUSTED`
- `PRIVATE`

Ket hop voi:
- waiting room bat/tat
- invited list
- email noi bo
- mat khau neu co

Ket qua co the la:
- `APPROVED`
- `PENDING`
- `REJECTED`

### Buoc 5: Tim hoac tao Participant

- Neu user da tung tham gia: tai su dung record participant
- Neu chua co: tao moi

Chi tiet quan trong:
- Participant phai duoc `save` truoc khi truy session
- Day la phan da duoc fix de tranh `TransientObjectException`

### Buoc 6: Xem lich su session

He thong doc `latestSessionStatus` de xu ly cac case:
- vao lai binh thuong
- tung bi `KICKED`

Rule quan trong:
- Neu user tung bi kick thi lan vao lai phai quay ve flow xin quyen

### Buoc 7: Neu trang thai la PENDING

- Tao thong bao cho host qua STOMP
- Nguoi dung o lai waiting room
- Frontend long-poll/subscribe de doi ket qua duyet

### Buoc 8: Neu APPROVED

Backend se:
- tao `ParticipantSession` moi voi `ACTIVE`
- goi `liveKitService.generateJoinToken(...)`
- tra ve `JoinMeetingResponse`

Response gom:
- `token`
- `serverUrl`
- `status`
- `role`
- `settings`
- `isOwner`
- `currentHostId`

## 4. Waiting room flow

### Muc tieu nghiep vu

- Khong phai ai cung vao thang room
- Host can kiem soat nguoi moi

### Luong xu ly

1. Attendee join
2. Neu chua duoc phe duyet thi `PENDING`
3. Backend gui thong bao cho host
4. Host mo participants/waiting queue
5. Host `APPROVED` hoac `REJECTED`
6. Backend broadcast ket qua
7. Frontend cua attendee nhan ket qua va vao room neu duoc duyet

API lien quan:
- `POST /api/meetings/{code}/join`
- `GET /api/meetings/{code}/waiting-room`
- `POST /api/meetings/{code}/approval`

## 5. Nghiep vu trong phong hop

Day la phan em co the trinh bay thanh "bo chuc nang room".

### Nhung gi LiveKit ho tro truc tiep

- Audio
- Video
- Screen share
- Participant state trong room
- Media track lifecycle
- Room connect/disconnect

### Nhung gi frontend/backend PTITMeet xay them

- Sidebar chat/participants
- Unread chat count
- Toast chat
- Raise hand/reactions
- Join request toast cho host
- Host transferred event
- Recording indicator cho tat ca moi nguoi
- Waiting room approval
- Summary page sau khi roi phong

## 6. Host controls

Host runtime co cac quyen:
- approve/reject waiting room
- mute participant
- mute all
- stop camera participant
- kick participant
- end meeting for all

Control channel:
- GUI tren `MeetingPage`, `ParticipantGrid`, `MeetingSidebar`, `ControlBar`
- STOMP message vao `/app/meeting/{code}/system`
- backend xu ly trong `MeetingSystemController` va `MeetingService.handleSystemAction(...)`

Supported actions hien tai gom:
- `MUTE_ALL`
- `STOP_CAMERA_ALL`
- `KICK_ALL`
- `MUTE_PARTICIPANT`
- `STOP_CAMERA_PARTICIPANT`
- `KICK_PARTICIPANT`
- `RECORDING_STARTED`
- `RECORDING_STOPPED`

## 7. Recording flow

Vi tri code:
- `LiveKitService`
- `RecordingController`
- `LiveKitWebhookController`

### Rule nghiep vu da chot

- `owner` moi duoc record
- `host runtime` khong tu dong co quyen record neu khong phai owner

### Start recording

1. Owner bam record
2. Frontend goi API recordings
3. Backend:
   - tim meeting
   - xac dinh `ownerId`
   - tu choi neu user hien tai khong phai owner
4. Backend goi LiveKit `startRoomCompositeEgress`
5. Luu `MeetingRecording` vao MySQL voi:
   - `meetingId`
   - `ownerId`
   - `egressId`
   - `status = RECORDING`
6. Broadcast system event `RECORDING_STARTED`
7. Tat ca thanh vien thay dau hieu dang ghi hinh

### Stop recording

1. Owner bam stop
2. Backend goi `stopEgress(egressId)`
3. Khi LiveKit xong, webhook goi ve backend
4. Backend cap nhat:
   - `status = COMPLETED` hoac `FAILED`
   - `fileUrl`

### Tai sao phai co `egressId`

- Day la id ky thuat cua phien recording trong LiveKit
- Dung de:
  - dung dung phien record
  - map webhook ve ban ghi DB
  - theo doi trang thai record

## 8. Host transfer flow

Day la phan nghiep vu rat hay de trinh bay.

### Bai toan

- Neu host roi phong ma phong van con nguoi:
  - ai se duyet waiting room
  - ai duoc mute/kick/end meeting

### Cach xu ly hien tai

- Khi `leaveMeeting(...)`:
  - session active cua nguoi roi bi set `LEFT`
  - neu phong rong thi finish meeting
  - neu van con nguoi va nguoi roi la host hien tai:
    - transfer host sang participant active tiep theo
    - broadcast event `HOST_TRANSFERRED`

### Diem hay trong nghiep vu

- `ownerId` khong doi
- `hostId` co the doi
- Nguoi tao phong quay lai van la owner, nhung khong tu dong cuop lai host

## 9. Leave meeting va finish meeting

### Leave meeting

Khi user bam leave:
- session `ACTIVE` -> `LEFT`
- redirect sang summary

### Auto finish meeting

Neu khong con ai active trong phong:
- `meeting.status = FINISHED`
- `meeting.endTime = now`

Diem nay quan trong vi no dung nghia nghiep vu hon viec chi finish khi host bam "End for all".

## 10. End meeting for all

Chi host runtime duoc lam:
- goi API ket thuc meeting
- tat ca session active thanh `ENDED_BY_HOST`
- tat ca nguoi dang trong room bi dua ve summary

## 11. History va summary

### Meeting history

API:
- `GET /api/meetings/history`

Y nghia:
- day la lich su tham gia hop
- co filter vai tro `ALL / HOST / GUEST`

### Chat history

API:
- `GET /api/meetings/{code}/chat/history`

Trang thai hien tai trong code:
- owner duoc xem full
- active participant dang hop cung co the doc lai lich su chat cua room

Neu thay hoi, ban nen noi trung thuc:
- business rule mong muon la owner uu tien so huu du lieu
- va he thong da tach owner/host de xu ly dung ownership recording

### Summary

API:
- `GET /api/meetings/{code}/summary`

Tac dung:
- hien thong ke sau hop
- tinh thoi gian tham gia
- phan biet host/guest khi tong hop du lieu

## 12. Frontend trong phong hop gom nhung khoi nao

Ban co the ke theo component:

- `MeetingHeader`
  - ma phong
  - badge host/owner
  - recording indicator
- `ParticipantGrid`
  - hien video grid
  - screen share/participant state
- `MeetingSidebar`
  - chat
  - participants
  - waiting list
- `ControlBar`
  - mic/cam/share/chat/participants/record/leave/end
- `Reactions`
  - emoji
  - raise hand

## 13. 3 diem hay de chot phan nghiep vu

- Join meeting la flow ket hop nhieu dieu kien nghiep vu nhat, khong chi la "cap token roi vao room".
- Nhom da tach `owner` va `host runtime` de dung nghia nghiep vu khi transfer host va recording ownership.
- Meeting khong phu thuoc hoan toan vao LiveKit; media do LiveKit xu ly, con waiting room, history, roles, summary, recording policy la nghiep vu do backend cua nhom dam nhan.

