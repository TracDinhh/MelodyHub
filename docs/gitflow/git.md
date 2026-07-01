# 🌿 Git Workflow (Team)

Tài liệu này quy định cách sử dụng Git trong team để đảm bảo source code được quản lý thống nhất.

---

# 1. Luôn cập nhật code mới nhất

Trước khi bắt đầu làm chức năng mới:

```bash
git checkout main
git pull origin main
```

---

# 2. Tạo branch mới

## Quy tắc đặt tên

```
<type>/<member-name>/<feature-name>
```

Trong đó:

- **type**: Loại công việc
- **member-name**: Tên hoặc username của thành viên
- **feature-name**: Tên chức năng đang thực hiện

Ví dụ:

```bash
git checkout -b feat/tracdinh/login
```

```bash
git checkout -b feat/tracdinh/song-management
```

```bash
git checkout -b feat/tracdinh/upload-music
```

```bash
git checkout -b fix/tracdinh/login-bug
```

```bash
git checkout -b refactor/tracdinh/song-service
```

---

# 3. Các loại Branch

| Prefix | Ý nghĩa |
|---------|----------|
| feat | Thêm tính năng mới |
| fix | Sửa lỗi |
| refactor | Cải thiện code |
| docs | Cập nhật tài liệu |
| style | Format code |
| chore | Cập nhật cấu hình, dependency... |

---

# 4. Sau khi hoàn thành chức năng

Kiểm tra trạng thái:

```bash
git status
```

---

# 5. Thêm file

```bash
git add .
```

---

# 6. Commit

## Quy tắc

```
<type>: <mô tả ngắn>
```

Ví dụ:

```bash
git commit -m "feat: implement login"
```

```bash
git commit -m "feat: create song api"
```

```bash
git commit -m "fix: validate login"
```

```bash
git commit -m "refactor: optimize song service"
```

---

# 7. Push lên GitHub

Lần đầu:

```bash
git push -u origin feat/tracdinh/login
```

Các lần sau:

```bash
git push
```

---

# 8. Tạo Pull Request

Sau khi Push thành công:

- Tạo Pull Request từ branch của mình vào `main`.
- Chờ các thành viên khác review (nếu có).
- Chỉ Merge khi Pull Request được chấp nhận.

---

# 9. Sau khi Merge

```bash
git checkout main
git pull origin main
```

Xóa branch cũ:

```bash
git branch -d feat/tracdinh/login
```

---

# Quy trình làm việc

```text
main
   │
   ▼
git pull

   │
   ▼
git checkout -b feat/<member>/<feature>

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
git push -u origin feat/<member>/<feature>

   │
   ▼
Create Pull Request

   │
   ▼
Review

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
git branch -d <branch>
```

# Lưu ý

- Không code trực tiếp trên `main`.
- Mỗi branch chỉ thực hiện **một chức năng**.
- Luôn `git pull` trước khi tạo branch mới.
- Commit thường xuyên với nội dung rõ ràng.
- Chỉ tạo Pull Request khi chức năng đã hoàn thành và đã tự kiểm tra.