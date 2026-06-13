# 01. Kien Truc He Thong, SFU, WebRTC, va LiveKit

## 1. Muc tieu noi trong 1-2 phut

- PTITMeet la ung dung hop truc tuyen thoi gian thuc.
- Frontend dung React de render giao dien.
- Backend dung Spring Boot de xu ly nghiep vu, xac thuc, lich su, waiting room, summary, recording metadata.
- Media room khong tu xay tu dau ma dung LiveKit, mot nen tang WebRTC theo kien truc SFU.
- Du lieu nghiep vu di qua REST API + MySQL + MongoDB, con su kien thoi gian thuc di qua WebSocket/STOMP va LiveKit room.

## 2. Kien truc tong quan cua PTITMeet

### Frontend

- Thu muc: `web-client/`
- Cong nghe chinh:
  - React
  - React Router
  - STOMP client cho WebSocket
  - LiveKit React components
- Vai tro:
  - Dang nhap, tao meeting, waiting room, vao meeting room
  - Hien thi video/audio stream
  - Chat, reactions, sidebar, host controls
  - Goi API recordings, history, summary

### Backend

- Thu muc: `backend/`
- Cong nghe chinh:
  - Spring Boot
  - Spring Security
  - JPA/Hibernate
  - WebSocket/STOMP
  - springdoc OpenAPI
- Vai tro:
  - Xac thuc va phan quyen
  - Tao/schedule/join/leave/end meeting
  - Waiting room va phe duyet participant
  - Quan ly role `host runtime` va `owner`
  - Goi LiveKit de tao join token va bat/dung recording
  - Luu meeting metadata, participant sessions, meeting history

### Luu tru

- MySQL:
  - `Meeting`
  - `Participant`
  - `ParticipantSession`
  - `MeetingRecording`
  - `MeetingFeedback`
- MongoDB:
  - chat messages trong phong hop

## 3. SFU la gi

SFU la `Selective Forwarding Unit`.

Y tuong:
- Moi nguoi tham gia chi upload 1 luong media len server SFU.
- SFU khong mix thanh 1 video duy nhat nhu MCU.
- SFU chon va forward cac stream can thiet den tung participant.

Loi ich:
- Giam tai upload tren client.
- Scale tot hon mesh P2P.
- De ho tro grid nhieu nguoi, spotlight, screen share, adaptive stream.

## 4. So sanh SFU voi WebRTC "binh thuong"

Khi noi "WebRTC binh thuong", thay thuong muon nghe ban phan biet giua:

### Cach 1: WebRTC Mesh P2P

- Moi nguoi ket noi truc tiep voi tat ca moi nguoi con lai.
- Neu co `n` nguoi, moi client phai quan ly nhieu ket noi.
- So ket noi tang nhanh theo so nguoi.
- Upload cua moi may tang rat manh khi phong dong.

Vi du:
- 2 nguoi: on
- 3-4 nguoi: van chap nhan duoc
- 8-10 nguoi: rat nang, de lag, ton CPU va bang thong

### Cach 2: WebRTC + SFU

- Moi client gui 1 stream len SFU.
- SFU phan phoi stream cho cac client khac.
- Client giam ap luc upload, he thong de mo rong hon.
- Phu hop phong hop nhieu nguoi, screen share, record, host controls.

## 5. Tai sao du an chon LiveKit

Neu tu xay WebRTC thu cong, nhom se phai tu lam:
- signaling
- ICE/STUN/TURN orchestration
- participant state
- reconnect behavior
- adaptive media handling
- recording/egress
- SDK UI integration

Dung LiveKit thi nhom tap trung vao nghiep vu:
- waiting room
- host approval
- owner/host separation
- meeting history
- recording ownership
- summary sau hop
- dashboard va lich su

## 6. LiveKit da ho tro nhung gi trong du an nay

### Media room

- Ket noi phong hop qua token
- Audio stream
- Video stream
- Screen sharing
- Participant presence
- Raise hand/reactions phia UI va realtime event
- Room audio rendering

Trong code frontend:
- `web-client/pages/MeetingPage.jsx`
- `LiveKitRoom`
- `RoomAudioRenderer`
- `ParticipantGrid`
- `ControlBar`

### Token va ket noi

Backend sinh token trong:
- `backend/src/main/java/com/ptithcm/ptitmeet/services/LiveKitService.java`
- ham `generateJoinToken(...)`

Token chua:
- room name
- participant identity
- quyen join room

### Recording / Egress

LiveKit ho tro egress:
- bat dau room composite recording
- dung recording
- callback webhook khi complete/failed

Trong code:
- `startRoomRecording(...)`
- `stopRecording(...)`
- `updateRecordingCompleted(...)`
- `updateRecordingFailed(...)`
- `LiveKitWebhookController`

### LiveKit KHONG thay backend nghiep vu

Day la diem rat quan trong khi thuyet trinh.

LiveKit giup:
- media transport
- room token
- room recording

Nhung backend cua PTITMeet van phai tu xu ly:
- ai duoc join
- ai dang cho trong waiting room
- ai la host hien tai
- ai la owner goc
- ai duoc record
- ai duoc xem history/recordings
- khi nao meeting finish

## 7. Role trong phong hop sau khi toi uu nghiep vu

Du an tach 2 role:

### `hostId`

- La host hien tai khi phong dang live
- Duoc:
  - admit/reject waiting room
  - mute participant
  - stop camera participant
  - kick participant
  - mute all
  - end meeting for all

### `ownerId`

- La chu phong goc, giu on dinh sau khi transfer host
- Duoc:
  - start/stop recording
  - xem danh sach recordings
  - uu tien so huu du lieu meeting

Day la diem em co the noi la nhom da "lam ro nghiep vu" thay vi dung 1 bien `hostId` cho ca runtime va ownership.

## 8. Luong du lieu khi hop truc tuyen

### REST API

Dung cho:
- tao meeting
- join meeting
- waiting room list
- approve/reject
- leave meeting
- summary
- recordings

### WebSocket/STOMP

Dung cho:
- chat message
- host control actions
- waiting room notifications
- host transferred event
- recording started/stopped event

### LiveKit media channel

Dung cho:
- audio/video
- screen share
- participant media rendering

## 9. Neu thay hoi "tai sao khong dung Zoom API hoac Google Meet"

Co the tra loi:
- Muc tieu cua do an la tu xay mot nen tang meeting co nghiep vu rieng.
- LiveKit giu vai tro media infrastructure, con nghiep vu va giao dien van do nhom tu thiet ke.
- Nhom can toan quyen mo rong waiting room, ownership, recording policy, meeting summary, test flow va deploy theo he thong rieng.

## 10. Y chinh de chot phan kien truc

- WebRTC la nen tang truyen media thoi gian thuc.
- LiveKit xay tren WebRTC va cung cap SFU + SDK + egress.
- PTITMeet dung LiveKit de giam do kho media, nhung backend van tu giai bai toan nghiep vu.
- Kien truc nay hop ly cho do an vi can can bang giua kha nang mo rong va toc do phat trien.

