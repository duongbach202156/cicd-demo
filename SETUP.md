# Setup demo CI/CD với Jenkins

Các bước dưới đây cần thao tác tay qua UI/tài khoản (Jenkins, GitHub, Docker Hub, SonarQube) — không thể tự động hoá bằng code vì liên quan đăng nhập/secrets.

## 0. Chuẩn bị tài khoản

1. Tạo repo trên GitHub (public hoặc private đều được), push code trong thư mục này lên (`git init`, `git remote add origin ...`, `git push`).
2. Tạo Docker Hub Access Token: Docker Hub → Account Settings → Security → New Access Token. Lưu lại `username` + `token` (dùng token thay vì password).

## 1. Chạy Jenkins + SonarQube

```bash
cd infra
docker compose up -d --build
```

- Jenkins UI: http://localhost:8081
- SonarQube UI: http://localhost:9000 (mặc định `admin` / `admin`, sẽ bắt đổi mật khẩu lần đầu)

Lấy mật khẩu Jenkins admin lần đầu:

```bash
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

Plugin cần thiết (Git, Pipeline, GitHub Branch Source, SonarQube Scanner, Credentials Binding, JUnit, Blue Ocean) đã được cài sẵn qua `infra/plugins.txt` khi build image Jenkins — không cần cài tay qua UI nữa. Nếu muốn cài thêm, vào **Manage Jenkins → Plugins**.

## 2. Cấu hình SonarQube

1. Đăng nhập SonarQube → **My Account → Security → Generate Token** (lưu lại token).
2. Tạo project mới (Manually), project key ví dụ `demo` (phải khớp `sonar.projectKey` trong `Jenkinsfile`).
3. Vào **Administration → Configuration → Webhooks** → thêm webhook:
   - Name: `jenkins`
   - URL: `http://jenkins:8080/sonarqube-webhook/` (dùng tên service `jenkins` trong docker network, **không** dùng `localhost`)

Nếu bỏ qua bước webhook này, stage `Quality Gate` trong Jenkinsfile sẽ bị timeout vì không nhận được callback.

## 3. Cấu hình Jenkins

### 3.1. System Config
**Manage Jenkins → System** → mục "SonarQube servers":
- Name: `sonarqube` (phải khớp tên trong `withSonarQubeEnv('sonarqube')` ở Jenkinsfile)
- Server URL: `http://sonarqube:9000`
- Server authentication token: tạo Credential kiểu "Secret text" chứa token SonarQube ở bước 2.1, rồi chọn credential đó.

### 3.2. Global Tool Configuration
**Manage Jenkins → Tools**:
- Maven: thêm 1 bản tên `M3` (khớp `tools { maven 'M3' }` trong Jenkinsfile) — có thể chọn "Install automatically" hoặc dùng Maven đã cài sẵn trong image Jenkins (`/usr/bin/mvn`, cấu hình MAVEN_HOME tương ứng).

### 3.3. Credentials
**Manage Jenkins → Credentials → System → Global credentials**, thêm:
- `dockerhub-cred`: kiểu "Username with password" — username/token Docker Hub ở bước 0.2.
- (Nếu repo GitHub private) thêm Personal Access Token GitHub kiểu "Username with password" hoặc "Secret text" tuỳ plugin dùng.

### 3.4. Sửa Jenkinsfile
Mở `Jenkinsfile`, thay `<dockerhub-user>` trong biến `IMAGE_NAME` bằng username Docker Hub thật của bạn, commit lại.

## 4. Tạo Multibranch Pipeline Job

1. Jenkins Dashboard → **New Item** → chọn **Multibranch Pipeline**, đặt tên (vd `demo`).
2. **Branch Sources → Add → GitHub**: nhập URL repo, chọn Credentials (nếu private).
3. **Scan Multibranch Pipeline Triggers**: tick "Periodically if not otherwise run", chọn interval (vd 1 minute) — đây là cơ chế polling thay cho webhook thật (vì Jenkins chạy local, GitHub không gọi vào được).
4. Save. Jenkins sẽ tự quét branch/PR và tạo job con tương ứng.
5. Lần build đầu tiên của 1 PR/branch mới có thể hiện thông báo "requires approval" — vào job đó bấm **approve** thủ công (Jenkins mặc định không tin code lạ).

## 5. (Khuyến nghị) Branch Protection Rule trên GitHub

GitHub repo → **Settings → Branches → Add branch protection rule** cho `main`:
- Tick "Require status checks to pass before merging", chọn check tương ứng do Jenkins report.

Nhờ vậy PR sẽ thực sự bị chặn merge nếu Jenkins build/test/SonarQube fail — đúng tinh thần CI/CD thật, không chỉ chạy cho có.

## 6. Test luồng end-to-end

1. Tạo branch mới, sửa code nhỏ, push, mở Pull Request vào `main`.
2. Trong vòng ~1 phút (chu kỳ poll), Jenkins tự tạo job build cho PR đó. Xem tiến trình qua **Blue Ocean** (icon ở sidebar Jenkins).
3. Kỳ vọng: chạy Checkout → Build → Unit Test (JUnit report xuất hiện) → SonarQube Analysis → Quality Gate. Dừng ở đây, **không** chạy Build & Push Image / Deploy (do `when { branch 'main' }`).
4. Merge PR vào `main`. Jenkins tự build lại trên `main`, lần này chạy full pipeline: build & push image lên Docker Hub, sau đó deploy (`docker run`).
5. Kiểm tra:
   ```bash
   docker images | grep demo
   docker ps | grep demo-app
   curl http://localhost:8080/api/hello?name=Jenkins
   ```
   Và kiểm tra trên Docker Hub thấy tag mới xuất hiện trong repo.

## Mở rộng optional: webhook thật thay vì polling

```bash
ngrok http 8081
```

Lấy URL ngrok (vd `https://xxxx.ngrok-free.app`), vào GitHub repo → Settings → Webhooks → Add webhook → Payload URL: `https://xxxx.ngrok-free.app/github-webhook/`, Content type: `application/json`, event: "Just the push event" + "Pull requests". Khi đó Jenkins sẽ được trigger ngay lập tức thay vì đợi chu kỳ poll.
