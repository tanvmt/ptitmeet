# 04. Deploy, CI/CD, GitHub Workflow, va Swagger/OpenAPI

## 1. Muc tieu noi trong 1-2 phut

- Du an khong dung theo kieu "code xong chay local".
- Nhóm da co quy trinh tu code -> test -> build image -> push registry -> deploy EC2.
- Ho so API duoc quan ly bang Swagger/OpenAPI va buoc dong bo trong hook.

## 2. Kien truc deploy

Theo `docs/deployment-ec2-cd-signoz.md`, kien truc de xuat gom:

### Tren EC2

- `reverse-proxy`: Nginx cong public
- `backend`: Spring Boot container
- `web`: frontend React da build, phuc vu boi Nginx
- `redis`: local container

### Ben ngoai EC2

- Aiven/MySQL
- MongoDB
- LiveKit Cloud
- SigNoz cho observability

## 3. Hanh trinh deploy em co the trinh bay

### Buoc 1: Chay local va xac nhan API/UI

- backend compile/test
- frontend lint/test/build
- xac nhan swagger docs

### Buoc 2: Container hoa

- backend build thanh Docker image
- web-client build thanh static assets va phuc vu boi Nginx

### Buoc 3: Push image len GHCR

Image duoc push len:
- `ghcr.io/<owner>/ptitmeet-backend:<sha7>`
- `ghcr.io/<owner>/ptitmeet-web:<sha7>`

### Buoc 4: Deploy len EC2

Workflow SSH vao EC2:
- cap nhat `.env.prod`
- `docker compose pull`
- `docker compose up -d --remove-orphans`

### Buoc 5: Kiem tra sau deploy

Theo docs:
- `docker compose -f deploy/ec2/docker-compose.prod.yml ps`
- `docker compose -f deploy/ec2/docker-compose.prod.yml logs backend --tail=100`
- `curl http://localhost/actuator/health`

## 4. CI/CD workflow trong repo

File:
- `.github/workflows/ci.yml`

### Trigger

Workflow chay khi push len:
- `dev`
- `main`
- `master`

### Cac job chinh

#### Job 1: `backend`

Chay tren `ubuntu-latest`

Thuc hien:
- checkout code
- setup Java 17
- backend lint: `./mvnw validate`
- backend tests: `./mvnw -B test`

#### Job 2: `frontend`

Thuc hien:
- checkout code
- setup Node.js 20
- `npm install`
- `npm run lint`
- `npm run test:run`
- `npm run build`

#### Job 3: `build-and-push`

Chi chay khi backend va frontend deu xanh.

Thuc hien:
- login GHCR
- build/push backend image
- build/push web image

#### Job 4: `deploy`

Chi chay sau `build-and-push`.

Thuc hien:
- dung `appleboy/ssh-action`
- SSH vao EC2
- login GHCR tren server
- pull image moi
- restart docker compose

## 5. Quy trinh GitHub ma em co the noi

Mau noi rat gon:

1. Dev code tren branch.
2. Commit bi pre-commit hook kiem tra lint/test/openapi sync.
3. Push len branch `dev` hoac `main`.
4. GitHub Actions chay CI.
5. Neu xanh, workflow build image va deploy.
6. Sau deploy, kiem tra health va logs.

Neu thay hoi ve diem manh:
- quy trinh nay giam nguy co deploy code chua test
- tao tinh lap lai duoc
- ro nguon image theo SHA tag

## 6. Swagger/OpenAPI trong du an

### File config

- `backend/src/main/java/com/ptithcm/ptitmeet/config/OpenApiConfig.java`

No cau hinh:
- title: `PTITMeet API`
- description: `API documentation for the PTITMeet backend services.`
- version: `v1`
- contact: `PTITMeet Team`
- server URL lay tu bien `backendUrl`

### Truy cap Swagger

Theo `SecurityConfig.java`, cac duong dan duoc `permitAll`:
- `/v3/api-docs`
- `/v3/api-docs.yaml`
- `/v3/api-docs/**`
- `/swagger-ui.html`
- `/swagger-ui/**`

Y nghia:
- co the xem docs ma khong can dang nhap
- tien cho frontend, tester, va giang vien xem API

### Tai lieu generate ra dau

Theo `docs/README.md` va `TESTING.md`:
- `docs/openapi.json`
- `docs/openapi.yaml`

Lenh generate:
- `cd backend && ./mvnw -Popenapi verify`

## 7. Tai sao OpenAPI sync la mot diem manh de thuyet trinh

Vi nhieu du an sinh vien co API nhung khong giu docs song song voi code.

Trong repo nay:
- neu API backend doi
- OpenAPI docs phai cap nhat
- pre-commit se chan commit neu docs bi out-of-sync

Day la mot diem em co the nhan manh ve quy trinh ky thuat.

## 8. Development workflow va git hook

File docs:
- `docs/development-workflow.md`

Hook duoc cai bang:
- `./scripts/install-git-hooks.ps1`
- hoac `./scripts/install-git-hooks.sh`

Hook chay:
- backend `./mvnw -Popenapi verify`
- frontend `npm run lint`
- frontend `npm run test:run`
- diff check `docs/openapi.json` va `docs/openapi.yaml`

## 9. Testing guide trong repo

File:
- `TESTING.md`

No mo ta:
- lenh run backend lint/test
- lenh run frontend lint/test/build
- JaCoCo report
- Swagger UI va OpenAPI endpoint
- pre-commit flow

Day la tai lieu giup team lam viec thong nhat, va cung la bang chung nhom co quy trinh kiem thu ro rang.

## 10. Bai hoc deploy thuc te co the ke voi thay

Neu muon trinh bay thuc te hon, ban co the noi them:

- Khi frontend dung `BrowserRouter`, server Nginx can cau hinh SPA fallback.
- Neu khong, reload tai route nhu `/dashboard` hoac `/meeting/...` se 404.
- Cach fix la cau hinh:
  - `try_files $uri $uri/ /index.html;`

Day la mot vi du rat thuc te cua viec deploy frontend SPA len production.

## 11. Nhung secret va bien moi truong quan trong

Theo docs deploy, can quan ly:
- LiveKit API key/secret
- MySQL cloud
- MongoDB
- JWT / auth secrets
- GitHub secrets:
  - `EC2_HOST`
  - `EC2_USER`
  - `EC2_SSH_PRIVATE_KEY`
  - `EC2_APP_DIR`
  - `EC2_PROD_ENV_FILE`
  - `GHCR_READ_TOKEN`
  - `PROD_VITE_API_URL`

Neu thay hoi "vi sao khong hard-code":
- de bao mat
- de tach dev/prod
- de CI/CD co the deploy tu dong

## 12. Observability va SigNoz

Theo docs deploy:
- backend gui trace/metric qua OpenTelemetry Java agent
- SigNoz dung de giam sat

Y nghia khi thuyet trinh:
- nhom khong chi deploy chay duoc
- ma con co huong theo doi he thong sau deploy

## 13. 3 diem hay de chot phan deploy/CI/CD

- CI/CD cua du an da tu dong hoa tu test den deploy EC2.
- OpenAPI/Swagger duoc dong bo voi code, co security whitelist ro rang.
- Quy trinh GitHub co pre-commit hook + GitHub Actions + GHCR + Docker Compose, the hien tu duy lam san pham nghiem tuc hon muc do do an thong thuong.

