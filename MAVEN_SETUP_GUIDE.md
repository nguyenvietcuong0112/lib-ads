# Hướng Dẫn Tích Hợp và Phân Phối Thư Viện Qua GitHub Maven Repo
*(Không Cần Tài Khoản, Mật Khẩu Hay Personal Access Token)*

---

## 1. Dành Cho Dự Án Con (Client Apps Sử Dụng Thư Viện)

Bất kỳ lập trình viên nào trong team khi tạo hoặc tích hợp thư viện vào app mới **hoàn toàn không cần tạo Personal Access Token**, không cần khai báo `username` hay `password`.

### Bước 1: Khai báo kho Maven trong `settings.gradle` (hoặc `settings.gradle.kts`)

```groovy
// settings.gradle (Groovy)
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        
        // Maven Repository trực tiếp từ GitHub (KHÔNG CẦN CREDENTIALS / TOKEN)
        maven { 
            url = uri("https://raw.githubusercontent.com/nguyenvietcuong0112/lib-ads/maven-repo/") 
        }
    }
}
```

*Nếu dùng Kotlin DSL (`settings.gradle.kts`):*
```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://raw.githubusercontent.com/nguyenvietcuong0112/lib-ads/maven-repo/")
        }
    }
}
```

### Bước 2: Thêm Dependency vào `app/build.gradle` (hoặc `app/build.gradle.kts`)

```groovy
dependencies {
    // Thư viện Ads Studio (tự động kéo toàn bộ AdMob, Firebase, Install Referrer, Shimmer...)
    implementation("com.app:library-ads:0.0.1")
}
```

Nhấn **Sync Now** trong Android Studio $\rightarrow$ Dự án sẽ tự động tải thư viện về trong vài giây!

---

## 2. Dành Cho Người Duy Trì Thư Viện (Phát Hành Phiên Bản Mới)

### Cách 1: Tự động 100% bằng GitHub Actions (Khuyên dùng)

1. Mở file [mobilibraryads/build.gradle](file:///E:/vc-project/ads_lib/mobilibraryads/build.gradle), cập nhật biến phiên bản mới:
   ```groovy
   def LIB_VERSION = "0.0.2" // Tăng phiên bản
   ```
2. Commit và gắn Git Tag rồi push lên GitHub:
   ```bash
   git commit -am "Release v0.0.2"
   git tag v0.0.2
   git push origin main --tags
   ```
3. **GitHub Actions** sẽ tự động kích hoạt:
   - Biên dịch thư viện Release AAR.
   - Tạo file POM chuẩn với đầy đủ dependencies.
   - Tự động đẩy toàn bộ artifact sang nhánh `maven-repo`.
   - Tạo GitHub Release và đính kèm file `.aar` để backup.

---

### Cách 2: Đóng gói thủ công từ máy tính cá nhân

Nếu muốn kiểm tra hoặc xuất file AAR/POM trực tiếp tại máy:

```powershell
# Chạy lệnh xuất bản vào thư mục maven-repo/
.\gradlew.bat :mobilibraryads:publishReleasePublicationToLocalMavenRepoRepository
```

Sau khi chạy xong, thư mục `maven-repo/` sẽ chứa đầy đủ cấu trúc:
```
maven-repo/
└── com/
    └── app/
        └── library-ads/
            ├── maven-metadata.xml
            └── 0.0.1/
                ├── library-ads-0.0.1.aar
                ├── library-ads-0.0.1.pom
                └── library-ads-0.0.1.module
```

---

## 3. Kiến Trúc Bảo Mật & Lưu Ý

1. **Bảo mật mã nguồn**:
   - File phân phối (`.aar`) chỉ chứa bytecode đã được biên dịch, **không chứa mã nguồn gốc Kotlin/Java**.
   - Toàn bộ dependencies trung gian (Google Ads, Firebase, Shimmer...) được liệt kê trong file `.pom`, giúp client app tự động giải quyết mà không phải khai báo lại bằng tay.
2. **Nếu Repository nguồn là Private**:
   - Để người ngoài tải được không cần mật khẩu qua link `raw.githubusercontent.com`, anh có thể tạo 1 repo phân phối riêng ở chế độ **Public** (ví dụ `appstudio/ads_mvn`) chỉ chứa thư mục `maven-repo/`, còn repo chứa code gốc `ads_lib` vẫn giữ chế độ **Private 100%**.
