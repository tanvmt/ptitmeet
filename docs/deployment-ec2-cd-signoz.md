# Deployment EC2 + CD + SigNoz

## 1. Muc tieu

Tai lieu nay dung cho lan deploy dau tien cua `ptitmeet` len AWS EC2 voi cac dich vu ngoai da ton tai san:

- MySQL cloud
- MongoDB cloud
- LiveKit Cloud
- S3-compatible storage cho recording

Flow de xuat:

1. Developer push len `dev` hoac `main`.
2. GitHub Actions CI chay test/lint nhu hien tai.
3. Neu CI xanh, GitHub Actions CD build Docker image va day len GHCR.
4. GitHub Actions SSH vao EC2, cap nhat file `.env.prod`, `docker compose pull`, sau do `up -d`.
5. Backend gui trace metric ve SigNoz bang OpenTelemetry Java agent.

## 2. Kien truc de xuat

Tren 1 EC2 ban dau:

- `reverse-proxy`: Nginx public port `80`
- `web`: frontend static image
- `backend`: Spring Boot image
- `redis`: local container

Ngoai EC2:

- Aiven/MySQL
- MongoDB Atlas
- LiveKit Cloud
- S3/R2 bucket
- SigNoz server

Khuyen nghi:

- Thu nghiem: 1 EC2 `t3.large` tro len neu ban con chay them SigNoz cung may
- Tot hon: 1 EC2 cho app, 1 EC2 rieng cho SigNoz

## 3. Viec can lam truoc khi deploy

### 3.1 Xoay secret

Trong repo da tung co secret that trong file mau. Hay rotate ngay:

- MySQL password
- MongoDB password
- LiveKit API key/secret
- SMTP password
- S3 access key/secret
- JWT secret

Sau do chi luu secret trong:

- GitHub Secrets
- `deploy/ec2/.env.prod` tren server
- `backend/.env` khi dev local

### 3.2 Domain

Neu dung cung 1 domain:

- `meet.your-domain.com` tro ve public IP EC2

Frontend co the dung:

- `VITE_API_URL=https://meet.your-domain.com/api`

## 4. Tao EC2

Khuyen nghi Ubuntu 24.04 LTS.

Security Group mo toi thieu:

- `22/tcp` tu IP cua ban
- `80/tcp` tu Internet
- `443/tcp` tu Internet neu them SSL

Khong mo cong backend `8080` va Redis `6379` ra Internet.

## 5. Cai moi truong tren EC2

Dang nhap server va cai Docker:

```bash
sudo apt update
sudo apt install -y ca-certificates curl git
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER
newgrp docker
docker --version
docker compose version
```

Clone repo:

```bash
git clone <YOUR_REPOSITORY_URL> ptitmeet
cd ptitmeet
mkdir -p deploy/ec2/nginx
cp deploy/ec2/.env.prod.example deploy/ec2/.env.prod
```

Sua `deploy/ec2/.env.prod` bang secret that.

## 6. Chay thu deploy manual truoc

Tu may local hoac tren EC2, build image va day len GHCR 1 lan dau, hoac tam thoi build tai server:

```bash
cd ptitmeet
docker build -t ptitmeet-backend:manual ./backend
docker build --build-arg VITE_API_URL=https://meet.your-domain.com/api -t ptitmeet-web:manual ./web-client
```

Neu muon chay khong qua GHCR de test rat nhanh, sua `deploy/ec2/docker-compose.prod.yml` tam thoi tu `image:` sang `build:`. Khi flow on dinh thi quay lai `image:` de dung CD chuan.

## 7. Cau hinh GitHub Secrets cho CD

Tao cac secret sau trong repository:

- `PROD_VITE_API_URL`: `https://meet.your-domain.com/api`
- `EC2_HOST`: public IP hoac domain EC2
- `EC2_USER`: thuong la `ubuntu`
- `EC2_SSH_PRIVATE_KEY`: private key PEM de SSH
- `EC2_APP_DIR`: vi du `/home/ubuntu/ptitmeet`
- `EC2_PROD_ENV_FILE`: noi dung day du cua `deploy/ec2/.env.prod`
- `GHCR_READ_TOKEN`: PAT co quyen read:packages

Neu repo private, EC2 can dang nhap duoc vao GHCR de pull image.

## 8. CD workflow dang hoat dong ra sao

Workflow moi nam o `.github/workflows/cd.yml`.

No hoat dong nhu sau:

1. Lang nghe khi workflow `CI` hoan thanh.
2. Chi deploy khi `CI` thanh cong va branch la `dev`, `main` hoac `master`.
3. Checkout dung commit vua qua CI.
4. Build 2 image:
   - `ghcr.io/<owner>/ptitmeet-backend:<sha7>`
   - `ghcr.io/<owner>/ptitmeet-web:<sha7>`
5. SSH vao EC2, cap nhat `.env.prod`, pull image moi, va restart compose.

Luu y:

- Workflow hien tai dang `git reset --hard` tren server ve dung commit can deploy. Khong duoc sua tay trong working tree tren EC2.
- Neu ban muon an toan hon, ta co the doi sang cach chi sync file compose va env, khong reset repo tren server.

## 9. SSL voi Nginx

File `deploy/ec2/nginx/default.conf` dang mo cong `80`.

Cho moi truong that, ban nen them SSL bang 1 trong 2 cach:

1. Nhanh nhat: dat Nginx Proxy Manager/Caddy o truoc.
2. Pho bien: cai Certbot va sua Nginx de listen `443 ssl`.

Neu ban muon, buoc sau minh co the viet tiep bo file Nginx + Certbot day du cho domain that cua ban.

## 10. SigNoz

### 10.1 Cach nen dung cho giai doan dau

It xam lan nhat la dung OpenTelemetry Java agent. Repo da duoc chuan bi san trong `backend/Dockerfile`:

- Image backend tu dong tai `opentelemetry-javaagent.jar`
- Bien `JAVA_TOOL_OPTIONS` da gan `-javaagent:/otel/opentelemetry-javaagent.jar`

Ban chi can truyen env:

- `OTEL_SERVICE_NAME=ptitmeet-backend`
- `OTEL_EXPORTER_OTLP_ENDPOINT=http://<signoz-host>:4318`
- `OTEL_EXPORTER_OTLP_PROTOCOL=http/protobuf`
- `OTEL_RESOURCE_ATTRIBUTES=deployment.environment=prod,service.namespace=ptitmeet`

### 10.2 Dung SigNoz tren may rieng

Khuyen nghi hon neu ban muon do on dinh:

1. Tao 1 EC2 rieng cho SigNoz.
2. Cai theo docker compose quickstart cua SigNoz.
3. Mo cong toi thieu:
   - `3301` cho UI
   - `4317/4318` cho OTLP ingest, chi nen mo noi bo/VPN neu duoc

### 10.3 Dung SigNoz cung may voi app

Chi nen de test. Ban phai nang cau hinh may vi SigNoz kha ton RAM va disk.

Neu chay chung may, hay dat:

- App EC2: toi thieu `4 vCPU / 8 GB RAM`
- Bat volume luu du lieu monitoring

### 10.4 Nhung gi ban se nhin thay trong SigNoz

- Request traces cho Spring Boot
- Latency, throughput, error rate
- Outbound calls neu Java agent bat duoc
- JVM metrics co ban

Neu muon logs day du ve SigNoz, ta se bo sung log appender/collector o buoc tiep theo. Hien tai bo khung nay uu tien trace + metrics truoc.

## 11. Kiem tra sau deploy

Sau khi workflow deploy xong, kiem tra:

```bash
docker compose -f deploy/ec2/docker-compose.prod.yml ps
docker compose -f deploy/ec2/docker-compose.prod.yml logs backend --tail=100
curl http://localhost/actuator/health
```

Va tu may local:

```bash
curl http://<EC2_PUBLIC_IP>/actuator/health
curl http://<EC2_PUBLIC_IP>/api/auth/login
```

Request thu hai co the tra `405` neu goi sai method, nhung dieu quan trong la Nginx da route duong `/api`.

## 12. Van de hien tai trong repo can nho

- `WebConfig` truoc day hardcode CORS localhost. Da doi sang bien `CORS_ALLOWED_ORIGINS`.
- File `application.properties.example` khong nen giu secret that. Da thay bang `backend/.env.example`.
- Backend chua co health endpoint public truoc day. Da mo `actuator/health`.

## 13. Buoc tiep theo nen uu tien

1. Test deploy manual 1 lan tren EC2.
2. Cau hinh domain va SSL.
3. Bat CD workflow.
4. Noi backend voi SigNoz.
5. Neu can, bo sung rollback strategy bang cach giu lai image tag cu.
