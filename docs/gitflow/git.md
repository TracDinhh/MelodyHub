# 🌿 Git Workflow

## 1. Luôn chuyển về main

```bash
git checkout main
```

---

## 2. Lấy code mới nhất

```bash
git pull origin main
```

---

## 3. Tạo branch mới cho chức năng

Quy tắc đặt tên:

```
feature/<ten-chuc-nang>
```

Ví dụ:

```bash
git checkout -b feature/login
```

```bash
git checkout -b feature/song
```

```bash
git checkout -b feature/artist
```

```bash
git checkout -b feature/upload-song
```

---

## 4. Code chức năng

Sau khi hoàn thành chức năng, kiểm tra trạng thái:

```bash
git status
```

---

## 5. Thêm các file đã thay đổi

```bash
git add .
```

Hoặc thêm từng file:

```bash
git add src/main/java/com/echobeat/controller/SongController.java
```

---

## 6. Commit

Quy tắc commit:

```
<type>: <mô tả ngắn>
```

### Các loại commit

| Type | Ý nghĩa |
|------|----------|
| feat | Thêm tính năng mới |
| fix | Sửa lỗi |
| refactor | Cải thiện code, không thay đổi chức năng |
| docs | Cập nhật tài liệu |
| style | Format code |
| chore | Công việc khác (config, dependency...) |

Ví dụ:

```bash
git commit -m "feat: create song api"
```

```bash
git commit -m "feat: implement login"
```

```bash
git commit -m "fix: validate upload file"
```

```bash
git commit -m "refactor: optimize song service"
```

```bash
git commit -m "docs: update readme"
```

---

## 7. Push lên GitHub

Lần đầu tiên của branch:

```bash
git push -u origin feature/song
```

Những lần sau:

```bash
git push
```

---

## 8. Tạo Pull Request

```
feature/song
        │
        ▼
       main
```

Sau khi Pull Request được Merge.

---

## 9. Quay về main

```bash
git checkout main
```

---

## 10. Lấy code mới

```bash
git pull origin main
```

---

## 11. Xóa branch cũ

```bash
git branch -d feature/song
```

---

# Quy trình ngắn gọn

```text
main
   │
   ▼
git pull

   │
   ▼
git checkout -b feature/...

   │
   ▼
Code

   │
   ▼
git add .

   │
   ▼
git commit -m "feat: ..."

   │
   ▼
git push -u origin feature/...

   │
   ▼
Pull Request

   │
   ▼
Merge

   │
   ▼
git checkout main

   │
   ▼
git pull

   │
   ▼
git branch -d feature/...
```

# Lưu ý

- Luôn tạo **1 branch cho 1 chức năng**.
- Không code trực tiếp trên `main`.
- Commit rõ ràng, ngắn gọn bằng tiếng Anh.
- Sau khi Merge, xóa branch cũ để giữ repository gọn gàng.