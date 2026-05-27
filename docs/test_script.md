# TÀI LIỆU TEST SCRIPT
## Dự án: PTITMeet

---

## 1. Thông tin tài liệu

| Thuộc tính | Nội dung |
| :--- | :--- |
| Tên tài liệu | Test Script |
| Dự án | PTITMeet |
| Phiên bản | 1.0 |
| Mục đích | Hướng dẫn thực thi kiểm thử theo luồng nghiệp vụ |
| Đối tượng sử dụng | QA, Tester, Nhóm phát triển, Nhóm demo nghiệm thu |
| Tài liệu liên quan | `docs/test_cases.md`, `docs/development-workflow.md` |

---

## 2. Mục đích

Tài liệu này được xây dựng nhằm:

- Hướng dẫn tester thực hiện kiểm thử theo đúng trình tự nghiệp vụ.
- Giảm trùng lặp thao tác khi chạy test thủ công.
- Hỗ trợ phân chia công việc trong nhóm.
- Hỗ trợ chạy smoke test, regression test, và UAT/demo.
- Làm cầu nối giữa `test_cases.md` và quá trình test thực tế.

Lưu ý:

- `test_cases.md` mô tả chi tiết từng trường hợp kiểm thử.
- `test_script.md` mô tả cách chạy test theo từng luồng lớn.

---

## 3. Phạm vi áp dụng

### 3.1 Trong phạm vi

- Web Client
- Backend API
- Waiting Room
- Meeting Room
- Realtime Chat/WebSocket
- Recording
- Summary/History
- Android Mobile App

### 3.2 Ngoài phạm vi

- Kiểm thử hiệu năng số lượng lớn
- Kiểm thử bảo mật chuyên sâu
- Kiểm thử DR/backup/restore
- Kiểm thử iOS

---

## 4. Môi trường kiểm thử

### 4.1 Môi trường phần mềm

- Frontend PTITMeet chạy ổn định
- Backend PTITMeet chạy ổn định
- Kết nối được tới MySQL
- Kết nối được tới MongoDB
- Kết nối được tới LiveKit
- WebSocket hoạt động
- Mail service hoạt động
- Recording storage hoạt động

### 4.2 Môi trường phần cứng

- Laptop/PC có camera, microphone, loa
- Kết nối mạng ổn định
- Android device hoặc Android emulator

### 4.3 Trình duyệt/thiết bị đề xuất

- Google Chrome bản mới nhất
- Microsoft Edge hoặc Firefox để đối chiếu
- Android 10+ nếu test trên thiết bị thật

---

## 5. Điều kiện bắt đầu và kết thúc kiểm thử

### 5.1 Entry Criteria

- Bản build mới nhất đã pass CI
- Các service phụ trợ hoạt động bình thường
- Có sẵn dữ liệu test cơ bản
- Tài khoản test đã được chuẩn bị
- Có ít nhất 2 người hoặc 2 thiết bị để test luồng nhiều người

### 5.2 Exit Criteria

- Hoàn thành thực thi các script ưu tiên
- Không còn lỗi mức Blocker/Critical đang mở
- Có biên bản tổng hợp kết quả
- Có danh sách bug và trạng thái xử lý

---

## 6. Dữ liệu kiểm thử

### 6.1 Tài khoản kiểm thử đề xuất

| Vai trò | Tài khoản | Mục đích |
| :--- | :--- | :--- |
| Host | `host1.ptitmeet@test.com` | Tạo phòng, duyệt phòng chờ, điều khiển cuộc họp |
| Guest 1 | `guest1.ptitmeet@test.com` | Tham gia phòng họp thông thường |
| Guest 2 | `guest2.ptitmeet@test.com` | Test chat, reaction, mute, kick |
| User mới | `newuser.ptitmeet@test.com` | Đăng ký tài khoản |
| Email lỗi | `invalid-email` | Test validation |

### 6.2 Dữ liệu nghiệp vụ mẫu

- Tiêu đề cuộc họp: `Hop nhom Sprint 1`
- Tin nhắn chat mẫu: `Chao moi nguoi`
- Mã cuộc họp hợp lệ
- Link cuộc họp hợp lệ
- 1 cuộc họp đang hoạt động
- 1 cuộc họp đã lên lịch
- 1 cuộc họp đã kết thúc

---

## 7. Phân công thực hiện

| Thành viên | Phạm vi ưu tiên |
| :--- | :--- |
| Đạt | Authentication, Profile |
| Tài | Dashboard, Schedule Meeting |
| Tấn | Waiting Room, Summary, History, Settings |
| Sang | Meeting Room, Recording, Host Controls |

Ghi chú:

- Các script liên quan meeting nên có nhiều hơn 1 người phối hợp.
- Script recording và host control nên ưu tiên chạy ở môi trường ổn định.

---

## 8. Chiến lược thực thi

Nên thực hiện kiểm thử theo 3 giai đoạn:

### 8.1 Smoke Test

Mục tiêu:

- Xác nhận hệ thống còn hoạt động sau khi build/deploy.

Phạm vi:

- Đăng ký/Đăng nhập
- Tạo meeting
- Join meeting
- Leave meeting
- Xem summary/history

### 8.2 Functional Test

Mục tiêu:

- Chạy đầy đủ các module theo `test_cases.md`.

Phạm vi:

1. Authentication & Profile
2. Dashboard & Meetings
3. Schedule Meeting
4. Waiting Room
5. Meeting Room
6. Summary & History
7. Settings
8. Mobile App

### 8.3 Regression Test

Mục tiêu:

- Đảm bảo các lỗi đã sửa không làm ảnh hưởng luồng cốt lõi.

Phạm vi tối thiểu:

- Login
- Create/Join meeting
- Waiting room
- Chat
- Host control
- Leave/End meeting
- Summary/History
- Mobile basic flow

---

## 9. Danh sách Test Script

## TS_01 - Đăng ký và đăng nhập tài khoản mới

### Mục tiêu

Xác nhận người dùng mới có thể đăng ký tài khoản và đăng nhập thành công.

### Tiền điều kiện

- Email `newuser.ptitmeet@test.com` chưa tồn tại trong hệ thống.

### Các bước thực hiện

1. Truy cập trang `/signup`.
2. Nhập họ tên hợp lệ.
3. Nhập email `newuser.ptitmeet@test.com`.
4. Nhập mật khẩu hợp lệ.
5. Nhấn `Sign Up`.
6. Kiểm tra điều hướng về `/login`.
7. Đăng nhập lại bằng tài khoản vừa tạo.

### Kết quả mong đợi

- Hệ thống tạo tài khoản thành công.
- Điều hướng đúng về trang đăng nhập.
- Người dùng đăng nhập thành công và vào dashboard.

### Test case liên quan

- `TC_AUTH_01`
- `TC_AUTH_06`

---

## TS_02 - Kiểm thử lỗi đăng nhập/đăng ký và validation

### Mục tiêu

Xác nhận hệ thống xử lý đúng các dữ liệu đầu vào không hợp lệ.

### Các bước thực hiện

1. Truy cập `/login`.
2. Để trống form và bấm `Sign In`.
3. Nhập email chưa đăng ký.
4. Nhập sai mật khẩu.
5. Truy cập `/signup`.
6. Nhập email sai định dạng.
7. Nhập mật khẩu ngắn hơn quy định.

### Kết quả mong đợi

- Hiển thị cảnh báo validate phù hợp.
- Không tạo session/token không hợp lệ.
- Hệ thống thông báo lỗi rõ ràng.

### Test case liên quan

- `TC_AUTH_02`
- `TC_AUTH_03`
- `TC_AUTH_04`
- `TC_AUTH_07`
- `TC_AUTH_08`
- `TC_AUTH_09`

---

## TS_03 - Quên mật khẩu và đặt lại mật khẩu

### Mục tiêu

Kiểm tra luồng reset password từ đầu đến cuối.

### Tiền điều kiện

- Mail service đang hoạt động.
- Tài khoản test đã tồn tại.

### Các bước thực hiện

1. Truy cập `/forgot-password`.
2. Nhập email hợp lệ.
3. Gửi yêu cầu reset.
4. Kiểm tra email nhận được.
5. Mở link reset password.
6. Nhập mật khẩu mới hợp lệ.
7. Đăng nhập bằng mật khẩu mới.

### Kết quả mong đợi

- Gửi email reset thành công.
- Link/token hợp lệ.
- Đặt lại mật khẩu thành công.
- Có thể đăng nhập lại bằng mật khẩu mới.

### Test case liên quan

- `TC_AUTH_11`
- `TC_AUTH_13`

---

## TS_04 - Xem và cập nhật hồ sơ người dùng

### Mục tiêu

Xác nhận trang hồ sơ hiển thị đúng và cập nhật được dữ liệu.

### Các bước thực hiện

1. Đăng nhập hệ thống.
2. Vào `/settings`.
3. Kiểm tra thông tin hồ sơ hiện tại.
4. Đổi họ tên.
5. Đổi avatar URL.
6. Nhấn lưu thay đổi.

### Kết quả mong đợi

- Thông tin hồ sơ hiển thị đúng.
- Dữ liệu được cập nhật thành công.
- Giao diện phản ánh ngay thông tin mới.

### Test case liên quan

- `TC_PROF_01`
- `TC_PROF_02`
- `TC_PROF_03`

---

## TS_05 - Tạo cuộc họp tức thì và vào phòng họp

### Mục tiêu

Kiểm tra luồng từ dashboard tới waiting room và meeting room của Host.

### Tiền điều kiện

- Host đã đăng nhập.

### Các bước thực hiện

1. Vào dashboard.
2. Nhấn `New Meeting`.
3. Kiểm tra trang waiting room.
4. Thử bật/tắt mic và camera.
5. Nhấn `Start meeting`.
6. Chờ kết nối vào meeting room.

### Kết quả mong đợi

- Meeting được tạo thành công.
- Waiting room hiển thị đúng.
- Host vào meeting room thành công.

### Test case liên quan

- `TC_DASH_02`
- `TC_WAIT_01`
- `TC_WAIT_02`
- `TC_WAIT_08`
- `TC_ROOM_01`

---

## TS_06 - Guest tham gia cuộc họp qua waiting room

### Mục tiêu

Kiểm tra luồng tham gia phòng họp của Guest.

### Tiền điều kiện

- Host đang có mặt trong meeting.
- Có mã phòng hoặc link phòng hợp lệ.

### Các bước thực hiện

1. Guest đăng nhập.
2. Chọn `Join Meeting`.
3. Nhập meeting code hoặc link.
4. Vào waiting room.
5. Nhấn `Join now`.
6. Host duyệt yêu cầu.
7. Guest vào meeting room.

### Kết quả mong đợi

- Guest được đưa vào hàng chờ.
- Host nhìn thấy yêu cầu duyệt.
- Sau khi approve, guest tham gia cuộc họp thành công.

### Test case liên quan

- `TC_DASH_03`
- `TC_DASH_04`
- `TC_WAIT_06`
- `TC_WAIT_07`
- `TC_ROOM_12`

---

## TS_07 - Chat, reaction và sidebar trong phòng họp

### Mục tiêu

Xác nhận các tương tác realtime hoạt động bình thường.

### Tiền điều kiện

- Có ít nhất 2 người trong cùng cuộc họp.

### Các bước thực hiện

1. Mở sidebar chat.
2. Guest gửi tin nhắn.
3. Host kiểm tra tin nhắn nhận được.
4. Đóng sidebar.
5. Guest gửi thêm tin nhắn.
6. Kiểm tra unread badge và toast.
7. Gửi reaction.

### Kết quả mong đợi

- Tin nhắn hiển thị realtime.
- Badge và toast hoạt động đúng.
- Reaction hiển thị cho thành viên khác.

### Test case liên quan

- `TC_ROOM_06`
- `TC_ROOM_07`
- `TC_ROOM_08`
- `TC_ROOM_11`

---

## TS_08 - Điều khiển cuộc họp bởi Host

### Mục tiêu

Kiểm tra các hành động quản trị của Host trong phòng họp.

### Tiền điều kiện

- Có 1 Host và ít nhất 2 Guest trong meeting.

### Các bước thực hiện

1. Host mute 1 guest.
2. Host stop video 1 guest.
3. Host chọn `Mute All`.
4. Host kick 1 guest.
5. Host chọn `End meeting for all`.

### Kết quả mong đợi

- Các lệnh quản trị tác động đúng đối tượng.
- Guest bị kick sẽ ra khỏi cuộc họp.
- Khi host end meeting, tất cả đều bị ngắt kết nối.

### Test case liên quan

- `TC_ROOM_13`
- `TC_ROOM_14`
- `TC_ROOM_15`
- `TC_ROOM_16`
- `TC_ROOM_18`

---

## TS_09 - Ghi hình cuộc họp

### Mục tiêu

Kiểm tra luồng start/stop recording.

### Tiền điều kiện

- LiveKit recording/egress hoạt động.
- Storage bucket cấu hình đúng.

### Các bước thực hiện

1. Host vào meeting room.
2. Nhấn `Record`.
3. Kiểm tra trạng thái recording.
4. Chờ một khoảng thời gian ngắn.
5. Nhấn `Stop Recording`.
6. Kiểm tra file recording đã được lưu.

### Kết quả mong đợi

- Recording bắt đầu thành công.
- Hiển thị trạng thái REC trên giao diện.
- Recording dừng thành công.
- File ghi hình được lưu trên storage.

### Test case liên quan

- `TC_ROOM_09`
- `TC_ROOM_10`

---

## TS_10 - Summary và history sau cuộc họp

### Mục tiêu

Xác nhận dữ liệu cuộc họp được tổng hợp chính xác sau khi rời phòng.

### Các bước thực hiện

1. Leave hoặc end meeting.
2. Mở trang summary.
3. Kiểm tra meeting id, thời lượng, số người tham gia, tin nhắn.
4. Gửi feedback.
5. Quay về dashboard.
6. Truy cập history.
7. Kiểm tra meeting vừa diễn ra xuất hiện trong danh sách.

### Kết quả mong đợi

- Summary hiển thị đúng dữ liệu.
- Gửi feedback thành công.
- History cập nhật đúng trạng thái cuộc họp.

### Test case liên quan

- `TC_SUMM_01`
- `TC_SUMM_02`
- `TC_SUMM_04`
- `TC_HIST_01`

---

## TS_11 - Lên lịch cuộc họp

### Mục tiêu

Xác nhận người dùng có thể tạo cuộc họp theo lịch và xem lại trong dashboard.

### Các bước thực hiện

1. Truy cập `/schedule`.
2. Nhập tiêu đề, ngày, giờ, thời lượng hợp lệ.
3. Thêm email người tham gia.
4. Cấu hình meeting options.
5. Nhấn `Schedule Meeting`.
6. Quay về dashboard.
7. Kiểm tra phần `Up Next`.

### Kết quả mong đợi

- Meeting được lên lịch thành công.
- Hiển thị đúng trong `Up Next`.
- Danh sách người mời được lưu.

### Test case liên quan

- `TC_SCHED_01`
- `TC_SCHED_03`
- `TC_SCHED_06`
- `TC_DASH_06`
- `TC_DASH_07`

---

## TS_12 - Luồng mobile cơ bản

### Mục tiêu

Kiểm tra các chức năng cốt lõi của ứng dụng Android.

### Các bước thực hiện

1. Mở ứng dụng.
2. Đăng nhập.
3. Tạo meeting hoặc join bằng mã.
4. Cấp quyền camera/microphone.
5. Vào meeting activity.
6. Bật/tắt mic.
7. Bật/tắt camera.
8. Rời cuộc họp.

### Kết quả mong đợi

- Ứng dụng không bị crash.
- Đăng nhập thành công.
- Join meeting thành công.
- Mic/camera hoạt động bình thường.

### Test case liên quan

- `TC_MOB_01`
- `TC_MOB_03`
- `TC_MOB_05`
- `TC_MOB_06`
- `TC_MOB_08`
- `TC_MOB_09`
- `TC_MOB_10`
- `TC_MOB_11`

---

## 10. Danh sách Smoke Test tối thiểu trước demo

Nếu thời gian ngắn, bắt buộc ưu tiên chạy:

1. `TS_01` - Đăng ký/Đăng nhập
2. `TS_05` - Tạo instant meeting
3. `TS_06` - Join waiting room
4. `TS_07` - Chat/Reaction
5. `TS_08` - Host control
6. `TS_10` - Summary/History
7. `TS_12` - Mobile basic flow

---

## 11. Quy tắc ghi nhận lỗi

Khi phát hiện lỗi, tester cần ghi:

- Script ID
- Test case ID liên quan
- Môi trường test
- Tài khoản test
- Các bước tái hiện
- Kết quả thực tế
- Kết quả mong đợi
- Ảnh/video minh chứng
- Log nếu có

Mẫu mô tả bug:

```text
[Module] [Script ID] Tieu de loi

Environment:
- Build:
- Browser/Device:
- Account:

Steps:
1. ...
2. ...
3. ...

Actual:
...

Expected:
...
```

---

## 12. Kết luận

Tài liệu này giúp nhóm kiểm thử thực hiện test theo luồng nghiệp vụ hoàn chỉnh, thay vì chạy rời rạc từng test case.

Giá trị sử dụng chính:

- Dễ phân công trong nhóm
- Dễ chạy smoke test trước demo
- Dễ regression sau khi fix bug
- Dễ dùng cho báo cáo kiểm thử và nghiệm thu

