FROM node:18 AS build
WORKDIR /app

COPY package*.json ./
RUN npm install

COPY . .

# 👇 khai báo nhiều biến
ARG VITE_GOOGLE_CLIENT_ID

# 👇 gán vào ENV để Vite dùng
ENV VITE_GOOGLE_CLIENT_ID=$VITE_GOOGLE_CLIENT_ID

RUN npm run build

FROM nginx:stable-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]