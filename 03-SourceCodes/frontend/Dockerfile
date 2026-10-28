# Image chạy frontend: bước 1 build React bằng Node; bước 2 dùng nginx phục vụ file tĩnh và chuyển tiếp /api tới
# backend. Trình duyệt chỉ gọi một địa chỉ (cùng origin) nên cookie phiên và token chống CSRF hoạt động như khi dùng
# proxy của Vite lúc phát triển, backend không cần cấu hình CORS.

FROM node:24-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.29-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
