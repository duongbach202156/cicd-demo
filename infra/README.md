# Hạ tầng CI/CD cho demo

Chạy Jenkins + SonarQube bằng Docker Compose để phục vụ demo pipeline.

```bash
docker compose up -d --build
```

- Jenkins: http://localhost:8081
- SonarQube: http://localhost:9000

Xem hướng dẫn cấu hình chi tiết ở [`../SETUP.md`](../SETUP.md).

Dừng và xoá toàn bộ (kể cả data Jenkins/SonarQube đã cấu hình):

```bash
docker compose down -v
```
