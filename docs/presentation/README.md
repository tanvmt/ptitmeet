# PTITMeet Presentation Docs

Bo tai lieu nay duoc viet de ban on lai nhanh truoc buoi thuyet trinh. Noi dung bam theo code va docs hien co trong repo, uu tien cac phan nghiep vu chinh, phan cong test, CI/CD, va chat luong ma nguon.

## Doc theo thu tu nay

1. [01-system-architecture-and-livekit.md](./01-system-architecture-and-livekit.md)
2. [02-core-business-flows.md](./02-core-business-flows.md)
3. [03-testing-assignment-and-quality.md](./03-testing-assignment-and-quality.md)
4. [04-deployment-cicd-github-and-swagger.md](./04-deployment-cicd-github-and-swagger.md)

## Ban co the noi ngan gon nhu sau

- PTITMeet la nen tang hop truc tuyen, backend dung Spring Boot, frontend dung React, realtime dung WebSocket/STOMP, media room dung LiveKit theo kien truc SFU.
- Phan em phu trach manh nhat la module phong hop truc tuyen, gom join meeting, waiting room, host controls, recording, chat/reactions, va cac test case lien quan.
- Em cung lam phan on dinh hoa quy trinh chat luong: unit test, controller test, frontend test, pre-commit hook, OpenAPI sync, CI/CD len GHCR va deploy EC2.

## Neu thay hoi "phai hoc ky nhat file nao"

- Hoc ky nhat:
  - `02-core-business-flows.md`
  - `03-testing-assignment-and-quality.md`
  - `04-deployment-cicd-github-and-swagger.md`

- File `01-system-architecture-and-livekit.md` la de tra loi cac cau hoi nen tang nhu:
  - tai sao dung SFU
  - LiveKit ho tro gi
  - khac nhau gi voi WebRTC thong thuong

