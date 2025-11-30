# TimeMaster - Hệ Thống Điểm Danh Thông Minh

![Version](https://img.shields.io/badge/version-1.0.0-blue.svg)
![Android](https://img.shields.io/badge/Android-26%2B-green.svg)
![Firebase](https://img.shields.io/badge/Firebase-Enabled-orange.svg)
![ML Kit](https://img.shields.io/badge/ML%20Kit-Face%20Detection-red.svg)

---

## 📋 Mục Lục

1. [Tổng Quan](#-tổng-quan)
2. [Tính Năng Chính](#-tính-năng-chính)
3. [Kiến Trúc Hệ Thống](#-kiến-trúc-hệ-thống)
4. [Công Nghệ Sử Dụng](#-công-nghệ-sử-dụng)
5. [Cấu Trúc Dự Án](#-cấu-trúc-dự-án)
6. [Hướng Dẫn Cài Đặt](#️-hướng-dẫn-cài-đặt)
7. [Hướng Dẫn Sử Dụng](#-hướng-dẫn-sử-dụng)
8. [Cấu Trúc Database](#-cấu-trúc-database)
9. [API & Services](#-api--services)
10. [Bảo Mật](#-bảo-mật)
11. [Testing](#-testing)
12. [Troubleshooting](#-troubleshooting)

---

## 🎯 Tổng Quan

**TimeMaster** là ứng dụng Android quản lý điểm danh thông minh sử dụng công nghệ **nhận diện khuôn mặt AI** (Face Recognition). Ứng dụng hỗ trợ hai vai trò chính: **User** (nhân viên) và **Admin** (quản lý), giúp tự động hóa quy trình điểm danh và theo dõi giờ làm việc.

### 🎓 Mục Đích Dự Án

- Tự động hóa quy trình điểm danh bằng AI
- Giảm thiểu gian lận và điểm danh thay
- Theo dõi thời gian làm việc chính xác
- Quản lý nhân viên hiệu quả
- Tạo báo cáo thống kê trực quan

### 👥 Vai Trò Người Dùng

#### 1. **User (Nhân viên)**
- Điểm danh bằng khuôn mặt
- Xem trạng thái điểm danh hôm nay
- Theo dõi lịch sử làm việc theo tuần/tháng
- Xem thống kê giờ làm việc
- Quản lý thông tin cá nhân

#### 2. **Admin (Quản lý)**
- Quản lý danh sách nhân viên
- Xem báo cáo điểm danh tổng thể
- Phê duyệt yêu cầu đăng ký
- Thống kê theo nhân viên/phòng ban
- Xuất báo cáo chi tiết

---

## ✨ Tính Năng Chính

### 🔐 1. Xác Thực & Bảo Mật

#### A. Đăng Ký Tài Khoản
- **Email/Password**: Đăng ký truyền thống với Firebase Authentication
- **Đăng ký khuôn mặt**: Quét và lưu embedding khuôn mặt vào Firestore
- **Phê duyệt**: Tài khoản mới cần admin phê duyệt trước khi kích hoạt

#### B. Đăng Nhập
- **Email/Password**: Đăng nhập thông thường
- **Google Sign-In**: Đăng nhập nhanh bằng tài khoản Google
- **Face Recognition**: Đăng nhập bằng nhận diện khuôn mặt (dành cho user đã đăng ký)

#### C. Quên Mật Khẩu
- Gửi email reset password qua Firebase Auth
- Link reset có thời hạn

### 👤 2. Quản Lý Người Dùng

#### A. Thông Tin Cá Nhân
- Hiển thị: `fullName`, `email`, `role`, `avatar`
- Chỉnh sửa profile: cập nhật tên, ảnh đại diện
- Đồng bộ với Firebase Firestore

#### B. Quản Lý Nhân Viên (Admin)
- Danh sách nhân viên với tìm kiếm, lọc
- Xem chi tiết thông tin nhân viên
- Chỉnh sửa/Xóa nhân viên
- Phê duyệt yêu cầu đăng ký mới

### 📸 3. Nhận Diện Khuôn Mặt (Face Recognition)

#### A. Công Nghệ
- **ML Kit Face Detection**: Phát hiện khuôn mặt trong ảnh/video
- **MobileFaceNet (TensorFlow Lite)**: Trích xuất embedding vector (512 chiều)
- **L2 Distance**: So sánh độ tương đồng giữa các embedding
- **Threshold**: 1.0 (điều chỉnh được)

#### B. Quy Trình Nhận Diện

```
1. Camera Capture (CameraX)
   ↓
2. ML Kit Face Detection (phát hiện khuôn mặt)
   ↓
3. Crop Face Region (cắt vùng khuôn mặt)
   ↓
4. Resize to 160x160 (chuẩn hóa)
   ↓
5. MobileFaceNet Model (trích xuất embedding)
   ↓
6. Compare with Firestore Embeddings (L2 distance)
   ↓
7. Match Found? → Điểm danh thành công
```

#### C. Tính Năng Nâng Cao
- **Real-time Detection**: Phát hiện liên tục từ camera
- **Face Tracking**: Theo dõi khuôn mặt di chuyển
- **Mirror Correction**: Tự động xử lý ảnh gương
- **Multi-face Detection**: Phát hiện nhiều khuôn mặt (chỉ xử lý face lớn nhất)

### ⏰ 4. Điểm Danh (Attendance)

#### A. Quy Trình Điểm Danh

##### Check-In/Check-Out Logic
```java
// Logic điểm danh chẵn/lẻ
Lần 1 (lẻ)  → CHECK_IN
Lần 2 (chẵn) → CHECK_OUT
Lần 3 (lẻ)  → CHECK_IN (lại)
Lần 4 (chẵn) → CHECK_OUT
...
```

##### Trạng Thái Điểm Danh
| Trạng thái | Điều kiện | Màu sắc | Icon |
|-----------|-----------|---------|------|
| **Đúng giờ** | Check-in trước 8:00 AM | 🟢 Xanh lá (#059669) | ✓ |
| **Đi muộn** | Check-in từ 8:01 - 16:59 | 🟡 Vàng cam (#D97706) | ✓ |
| **Vắng mặt** | Check-in sau 17:00 hoặc không check-in | 🔴 Đỏ (#DC2626) | ✗ |
| **Chưa điểm danh** | Chưa check-in (trước 17:00) | ⚪ Xám (#6B7280) | ⏰ |

#### B. Dữ Liệu Lưu Trữ

**Collection**: `attendanceRecords`

```javascript
{
  "uid": "userId",                    // ID người dùng
  "userEmail": "user@example.com",    // Email
  "timestamp": 1701234567890,         // Milliseconds
  "date": "2025-11-30",              // yyyy-MM-dd
  "time": "08:30:00",                // HH:mm:ss
  "checkType": "CHECK_IN",           // CHECK_IN | CHECK_OUT
  "method": "FACE_RECOGNITION",      // Phương thức điểm danh
  "confidence": 0.85,                // Độ tin cậy (L2 distance)
  "status": "PRESENT",               // PRESENT | ABSENT
  "deviceInfo": "Pixel 6"            // Thông tin thiết bị
}
```

#### C. Tính Năng Điểm Danh

1. **Real-time Status**: Hiển thị trạng thái điểm danh hôm nay
2. **Check-in/Check-out Time**: Hiển thị giờ vào/ra mới nhất
3. **Total Hours**: Tự động tính tổng giờ làm (check-out - check-in)
4. **Progress Bar**: Hiển thị tiến độ làm việc (8 giờ = 100%)
5. **Auto Update**: Cập nhật real-time qua Firestore listener

### 📊 5. Thống Kê & Báo Cáo

#### A. User Statistics (Nhân viên)

##### Dashboard Tổng Quan
- **Trạng thái hôm nay**: Đúng giờ/Muộn/Vắng
- **Thời gian làm việc**: Giờ vào, giờ ra, tổng giờ
- **Lời chào**: Theo thời gian trong ngày
- **Ngày tháng**: Định dạng tiếng Việt

##### Thống Kê Tuần/Tháng
- **Biểu đồ cột**: Số giờ làm việc mỗi ngày
- **Lịch sử điểm danh**: Danh sách chi tiết theo ngày
- **Tổng kết**: Tổng giờ làm, số ngày đi làm, tỷ lệ đúng giờ

#### B. Admin Statistics (Quản lý)

##### Dashboard Tổng Quan
- **Tổng số nhân viên**: Tổng số user trong hệ thống
- **Điểm danh hôm nay**: Số người đã/chưa điểm danh
- **Tỷ lệ đúng giờ**: Phần trăm nhân viên đến đúng giờ
- **Top nhân viên**: Xếp hạng theo giờ làm

##### Báo Cáo Chi Tiết
- **Theo nhân viên**: Lịch sử từng người
- **Theo ngày**: Danh sách điểm danh trong ngày
- **Biểu đồ**: Xu hướng theo thời gian
- **Export**: Xuất báo cáo Excel/PDF (tính năng mở rộng)

### ⚙️ 6. Cài Đặt (Settings)

#### A. Thông Tin Cá Nhân
- Xem/Chỉnh sửa profile
- Đổi avatar
- Cập nhật email, tên

#### B. Tùy Chọn Ứng Dụng
- Ngôn ngữ: Tiếng Việt/English
- Thông báo: Bật/tắt
- Chế độ tối: Dark/Light mode (future)

#### C. Quản Lý Khuôn Mặt
- Xem danh sách khuôn mặt đã đăng ký
- Thêm/Xóa khuôn mặt
- Cập nhật embedding mới

#### D. Bảo Mật
- Đổi mật khẩu
- Xem lịch sử đăng nhập
- Đăng xuất khỏi tất cả thiết bị

---

## 🏗️ Kiến Trúc Hệ Thống

### 📐 Kiến Trúc Tổng Thể

```
┌─────────────────────────────────────────────────────────┐
│                   PRESENTATION LAYER                    │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │   Activity   │  │   Fragment   │  │    Adapter   │  │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  │
└─────────┼──────────────────┼──────────────────┼─────────┘
          │                  │                  │
┌─────────┼──────────────────┼──────────────────┼─────────┐
│         ▼        DOMAIN LAYER        ▼        ▼         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │  ViewModel   │  │  Repository  │  │  Use Cases   │  │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  │
└─────────┼──────────────────┼──────────────────┼─────────┘
          │                  │                  │
┌─────────┼──────────────────┼──────────────────┼─────────┐
│         ▼         DATA LAYER         ▼        ▼         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │   Firebase   │  │  TensorFlow  │  │  Local DB    │  │
│  │  Firestore   │  │     Lite     │  │  (Future)    │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────┘
```

### 🎨 Design Pattern

#### 1. **MVVM (Model-View-ViewModel)**
```
View (Activity/Fragment)
  ↓ observe
ViewModel (LiveData)
  ↓ call
Repository (Data Source)
  ↓ fetch
Firebase/Local DB
```

#### 2. **Repository Pattern**
- **Interface**: Định nghĩa contract
- **Implementation**: Firebase/Local implementation
- **Dependency Injection**: Constructor injection

#### 3. **Observer Pattern**
- **LiveData**: Quan sát dữ liệu thay đổi
- **Firestore Listeners**: Real-time updates
- **Callbacks**: Xử lý async operations

### 📦 Layers

#### Presentation Layer
- **Activities**: Màn hình chính
- **Fragments**: Module UI nhỏ
- **Adapters**: RecyclerView adapters
- **ViewModels**: Quản lý UI state

#### Domain Layer
- **Models**: Data classes
- **Repositories**: Data access
- **Use Cases**: Business logic

#### Data Layer
- **Firebase**: Cloud storage
- **TensorFlow Lite**: ML model
- **SharedPreferences**: Local cache

---

## 🛠️ Công Nghệ Sử Dụng

### 🔧 Core Technologies

| Công nghệ | Phiên bản | Mục đích |
|-----------|-----------|----------|
| **Android SDK** | 26+ (Oreo) | Platform |
| **Java** | 8 | Ngôn ngữ lập trình |
| **Gradle** | 8.13 | Build tool |
| **Material Design** | 1.11.0 | UI Components |

### ☁️ Firebase Services

| Service | Mục đích |
|---------|----------|
| **Firebase Authentication** | Xác thực người dùng |
| **Cloud Firestore** | NoSQL database |
| **Firebase Storage** | Lưu trữ file/ảnh |
| **Firebase Functions** | Serverless backend (future) |
| **Firebase Analytics** | Phân tích hành vi |

### 🤖 Machine Learning

| Library | Phiên bản | Mục đích |
|---------|-----------|----------|
| **ML Kit Face Detection** | 16.1.6 | Phát hiện khuôn mặt |
| **TensorFlow Lite** | 2.13.0 | Runtime cho ML model |
| **TensorFlow Lite GPU** | 2.13.0 | GPU acceleration |
| **MobileFaceNet** | Custom | Face recognition model |

### 📸 Camera & Image

| Library | Phiên bản | Mục đích |
|---------|-----------|----------|
| **CameraX** | 1.3.1 | Camera API modern |
| **Glide** | 4.16.0 | Image loading/caching |
| **CircleImageView** | 3.1.0 | Avatar tròn |

### 📊 Charts & Visualization

| Library | Phiên bản | Mục đích |
|---------|-----------|----------|
| **MPAndroidChart** | 3.1.0 | Biểu đồ thống kê |

### 🧪 Testing

| Library | Phiên bản | Mục đích |
|---------|-----------|----------|
| **JUnit** | 4.13.2 | Unit testing |
| **Espresso** | 3.5.1 | UI testing |

---

## 📁 Cấu Trúc Dự Án

```
TimeMaster/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/timemaster/
│   │   │   │   ├── ui/                          # Presentation Layer
│   │   │   │   │   ├── auth/                    # Xác thực
│   │   │   │   │   │   ├── login/
│   │   │   │   │   │   │   ├── LoginActivity.java
│   │   │   │   │   │   │   └── FaceRecognitionActivity.java
│   │   │   │   │   │   ├── register/
│   │   │   │   │   │   │   └── RegisterActivity.java
│   │   │   │   │   │   └── forgotpassword/
│   │   │   │   │   │       └── ForgotPassword.java
│   │   │   │   │   │
│   │   │   │   │   ├── dashboard/               # Dashboard chính
│   │   │   │   │   │   ├── DashboardHostActivity.java
│   │   │   │   │   │   ├── DashboardFragment.java
│   │   │   │   │   │   │
│   │   │   │   │   │   ├── user/                # User features
│   │   │   │   │   │   │   └── UserStatusFragment.java
│   │   │   │   │   │   │
│   │   │   │   │   │   ├── admin/               # Admin features
│   │   │   │   │   │   │   ├── ManagementFragment.java
│   │   │   │   │   │   │   ├── EmployeeAdapter.java
│   │   │   │   │   │   │   └── EmployeeDetailActivity.java
│   │   │   │   │   │   │
│   │   │   │   │   │   ├── stats/               # Statistics
│   │   │   │   │   │   │   ├── UserStatsFragment.java
│   │   │   │   │   │   │   ├── AdminStatsFragment.java
│   │   │   │   │   │   │   └── AdminStatsViewModel.java
│   │   │   │   │   │   │
│   │   │   │   │   │   └── settings/            # Settings
│   │   │   │   │   │       └── SettingsFragment.java
│   │   │   │   │   │
│   │   │   │   │   ├── checkin/                 # Check-in feature
│   │   │   │   │   │   ├── CheckInActivity.java
│   │   │   │   │   │   ├── TimeViewModel.java
│   │   │   │   │   │   └── TimeViewModelFactory.java
│   │   │   │   │   │
│   │   │   │   │   └── widget/                  # Custom widgets
│   │   │   │   │       └── SyncedHorizontalScrollView.java
│   │   │   │   │
│   │   │   │   ├── data/                        # Data Layer
│   │   │   │   │   ├── model/                   # Data models
│   │   │   │   │   │   ├── Employee.java
│   │   │   │   │   │   ├── CheckIn.java
│   │   │   │   │   │   ├── UserAttendance.java
│   │   │   │   │   │   ├── DayAttendance.java
│   │   │   │   │   │   ├── WeekAttendance.java
│   │   │   │   │   │   ├── FaceEmbedding.java
│   │   │   │   │   │   ├── FaceHistory.java
│   │   │   │   │   │   └── StatusType.java
│   │   │   │   │   │
│   │   │   │   │   └── repository/              # Data access
│   │   │   │   │       ├── AttendanceRepository.java
│   │   │   │   │       ├── AttendanceRepositoryFirestore.java
│   │   │   │   │       ├── UserAttendanceRepository.java
│   │   │   │   │       ├── FaceRecognitionRepository.java
│   │   │   │   │       ├── TimeRepository.java
│   │   │   │   │       └── TimeRepositoryImpl.java
│   │   │   │   │
│   │   │   │   ├── ml/                          # Machine Learning
│   │   │   │   │   ├── FaceNetModel.java        # TFLite model wrapper
│   │   │   │   │   ├── FaceRecognitionProcessor.java
│   │   │   │   │   ├── FaceRecognitionConstants.java
│   │   │   │   │   ├── SimilarityClassifier.java
│   │   │   │   │   └── EmbeddingsUtils.java
│   │   │   │   │
│   │   │   │   └── utils/                       # Utilities
│   │   │   │       └── camerax/                 # CameraX helpers
│   │   │   │           ├── FaceAnalyzer.java
│   │   │   │           ├── FaceGraphic.java
│   │   │   │           └── GraphicOverlay.java
│   │   │   │
│   │   │   ├── res/                             # Resources
│   │   │   │   ├── layout/                      # XML layouts
│   │   │   │   ├── drawable/                    # Icons, shapes
│   │   │   │   ├── values/                      # Strings, colors, styles
│   │   │   │   ├── menu/                        # Navigation menus
│   │   │   │   └── xml/                         # Network config, backup
│   │   │   │
│   │   │   ├── assets/                          # ML models
│   │   │   │   └── mobile_face_net.tflite      # FaceNet model
│   │   │   │
│   │   │   └── AndroidManifest.xml              # App manifest
│   │   │
│   │   ├── test/                                # Unit tests
│   │   └── androidTest/                         # Instrumented tests
│   │
│   ├── build.gradle.kts                         # App build config
│   └── google-services.json                     # Firebase config
│
├── gradle/                                      # Gradle wrapper
├── build.gradle.kts                            # Project build config
├── settings.gradle.kts                         # Project settings
├── firestore.rules                             # Firestore security rules
├── firebase.json                               # Firebase config
└── DOCUMENTATION.md                            # This file
```

---

## ⚙️ Hướng Dẫn Cài Đặt

### 📋 Yêu Cầu Hệ Thống

#### Development Environment
- **Android Studio**: Arctic Fox trở lên (khuyến nghị: Hedgehog 2023.1.1+)
- **JDK**: Java 8 trở lên
- **Gradle**: 8.0+
- **Android SDK**: API 26+ (Android 8.0 Oreo)

#### Device Requirements
- **Android**: 8.0 (API 26) trở lên
- **RAM**: Tối thiểu 2GB
- **Camera**: Front camera (nhận diện khuôn mặt)
- **Internet**: Kết nối ổn định

### 🔧 Các Bước Cài Đặt

#### 1. Clone Repository

```bash
git clone https://github.com/your-repo/TimeMaster.git
cd TimeMaster
```

#### 2. Cấu Hình Firebase

##### A. Tạo Firebase Project
1. Truy cập [Firebase Console](https://console.firebase.google.com/)
2. Tạo project mới: "TimeMaster"
3. Thêm Android app với package name: `com.example.timemaster`

##### B. Download Configuration File
1. Download `google-services.json`
2. Copy vào `app/` folder

##### C. Enable Firebase Services
Trong Firebase Console, kích hoạt:
- ✅ **Authentication**: Email/Password, Google Sign-In
- ✅ **Cloud Firestore**: Database
- ✅ **Storage**: File storage (optional)
- ✅ **Analytics**: Tracking

##### D. Firestore Security Rules
Copy nội dung từ `firestore.rules` và deploy:

```bash
firebase deploy --only firestore:rules
```

#### 3. Cấu Hình Model AI

##### Download MobileFaceNet Model
1. Download model từ: [MobileFaceNet TFLite](https://github.com/sirius-ai/MobileFaceNet_TF)
2. Đặt file `mobile_face_net.tflite` vào `app/src/main/assets/`

##### Hoặc sử dụng model có sẵn (nếu đã bao gồm trong repo)

#### 4. Build & Run

##### Từ Android Studio
1. Open project trong Android Studio
2. Sync Gradle: `File > Sync Project with Gradle Files`
3. Build APK: `Build > Build Bundle(s) / APK(s) > Build APK(s)`
4. Run: Click nút ▶️ Run hoặc `Shift + F10`

##### Từ Command Line

```bash
# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Build & Install
./gradlew installDebug
```

#### 5. Khởi Tạo Database

##### Tạo Admin Account
1. Run app lần đầu
2. Register tài khoản admin thủ công
3. Vào Firebase Console > Firestore
4. Tìm document trong `users/{userId}`
5. Sửa field `role` thành `"admin"`

##### Import Sample Data (Optional)
```javascript
// Firebase Console > Firestore > Import
// Hoặc sử dụng Firebase CLI
firebase firestore:import ./sample-data
```

---

## 📖 Hướng Dẫn Sử Dụng

### 👤 Dành Cho Người Dùng (User)

#### 1. Đăng Ký Tài Khoản

**Bước 1**: Mở ứng dụng → Chọn "Đăng ký"

**Bước 2**: Điền thông tin
- Họ tên đầy đủ
- Email công ty
- Mật khẩu (tối thiểu 6 ký tự)
- Xác nhận mật khẩu

**Bước 3**: Đăng ký khuôn mặt
- Cho phép quyền camera
- Đối diện camera, giữ khuôn mặt trong khung
- Đợi hệ thống quét và lưu embedding
- Nhận thông báo "Đăng ký thành công"

**Bước 4**: Chờ phê duyệt
- Admin sẽ xét duyệt tài khoản
- Nhận email thông báo khi được duyệt

#### 2. Đăng Nhập

**Phương thức 1**: Email/Password
- Nhập email và mật khẩu
- Click "Đăng nhập"

**Phương thức 2**: Google Sign-In
- Click biểu tượng Google
- Chọn tài khoản Google

**Phương thức 3**: Face Recognition (Khuyến nghị)
- Click "Đăng nhập bằng khuôn mặt"
- Đối diện camera
- Hệ thống tự động nhận diện và đăng nhập

#### 3. Điểm Danh

**Check-In (Vào làm)**

1. Mở app khi đến công ty
2. Vào màn hình "Trạng thái"
3. Click nút "Điểm danh"
4. Đối diện camera
5. Nhận thông báo "Check-in thành công"

**Check-Out (Tan làm)**

1. Mở app khi về
2. Làm tương tự check-in
3. Hệ thống tự động nhận biết là check-out (lần chẵn)

**Lưu ý**:
- ✅ Check-in trước 8:00 AM → Đúng giờ
- ⚠️ Check-in từ 8:01 - 16:59 → Muộn
- ❌ Check-in sau 17:00 → Vắng

#### 4. Xem Thống Kê

**Trạng Thái Hôm Nay**
- Tab "Trạng thái" hiển thị:
  - Thời gian check-in/check-out
  - Tổng giờ làm việc
  - Trạng thái (Đúng giờ/Muộn/Vắng)
  - Progress bar giờ làm

**Lịch Sử Làm Việc**
- Tab "Thống kê"
- Chọn "Tuần này" hoặc "Tháng này"
- Xem biểu đồ số giờ làm mỗi ngày
- Chi tiết từng ngày: giờ vào/ra, tổng giờ

#### 5. Quản Lý Cá Nhân

**Chỉnh Sửa Profile**
- Tab "Cài đặt"
- Click "Chỉnh sửa hồ sơ"
- Cập nhật: Tên, ảnh đại diện
- Click "Lưu"

**Đổi Mật Khẩu**
- Tab "Cài đặt"
- "Đổi mật khẩu"
- Nhập mật khẩu cũ và mới
- Xác nhận

**Đăng Xuất**
- Tab "Cài đặt"
- Click "Đăng xuất"
- Xác nhận

### 🔐 Dành Cho Quản Lý (Admin)

#### 1. Quản Lý Nhân Viên

**Xem Danh Sách**
- Tab "Quản lý"
- Danh sách tất cả nhân viên
- Tìm kiếm theo tên, email
- Lọc theo trạng thái: Đang làm/Nghỉ

**Thêm Nhân Viên**
- Click nút "+" (hoặc "Thêm")
- Điền thông tin nhân viên
- Gửi email mời tham gia

**Xem Chi Tiết**
- Click vào nhân viên
- Xem: Thông tin cá nhân, lịch sử điểm danh
- Chỉnh sửa thông tin
- Xóa nhân viên (nếu cần)

**Phê Duyệt Đăng Ký**
- Vào "Yêu cầu chờ duyệt"
- Xem danh sách pending users
- Click "Phê duyệt" hoặc "Từ chối"

#### 2. Xem Báo Cáo

**Dashboard Tổng Quan**
- Tab "Thống kê"
- Hiển thị:
  - Tổng số nhân viên
  - Số người đã điểm danh hôm nay
  - Tỷ lệ đúng giờ
  - Biểu đồ xu hướng

**Báo Cáo Chi Tiết**
- Chọn "Báo cáo chi tiết"
- Lọc theo:
  - Ngày/Tuần/Tháng
  - Nhân viên cụ thể
  - Phòng ban (future)
- Xuất báo cáo (Excel/PDF)

#### 3. Cài Đặt Hệ Thống

**Cấu Hình Giờ Làm**
- Giờ bắt đầu: 8:00 AM (mặc định)
- Giờ kết thúc: 17:00 PM
- Giờ nghỉ trưa (future)

**Quản Lý Phòng Ban**
- Tạo/Sửa/Xóa phòng ban
- Gán nhân viên vào phòng ban

**Backup & Restore**
- Sao lưu dữ liệu định kỳ
- Khôi phục từ backup

---

## 🗄️ Cấu Trúc Database

### Firebase Firestore Collections

#### 1. **users** Collection

```javascript
users/{userId}
{
  "uid": "firebase_auth_uid",           // String (Document ID)
  "email": "user@example.com",          // String
  "fullName": "Nguyễn Văn A",          // String
  "role": "user",                       // String: "admin" | "user"
  "status": "approved",                 // String: "pending" | "approved" | "rejected"
  "photoUrl": "https://...",            // String (optional)
  "department": "IT",                   // String (optional)
  "position": "Developer",              // String (optional)
  "phone": "+84912345678",              // String (optional)
  "createdAt": Timestamp,               // Timestamp
  "updatedAt": Timestamp                // Timestamp
}
```

**Indexes**:
- `email` (ASC)
- `role` (ASC)
- `status` (ASC)

#### 2. **face_embeddings** Collection

```javascript
face_embeddings/{userId}
{
  "userId": "firebase_auth_uid",        // String (matches Document ID)
  "userEmail": "user@example.com",      // String
  "faceEmbedding": [0.123, -0.456, ...], // Array<Double> (512 elements)
  "timestamp": 1701234567890,           // Long (milliseconds)
  "deviceInfo": "Pixel 6",              // String
  "registrationMethod": "CAMERA",       // String
  "confidence": 0.95                    // Double (optional)
}
```

**Indexes**:
- `userEmail` (ASC)
- `timestamp` (DESC)

**Security**:
- Read: Public (cần cho attendance)
- Write: Chỉ user owner

#### 3. **attendanceRecords** Collection

```javascript
attendanceRecords/{recordId}
{
  "uid": "user_firebase_uid",           // String
  "userEmail": "user@example.com",      // String
  "timestamp": 1701234567890,           // Long (milliseconds)
  "date": "2025-11-30",                // String (yyyy-MM-dd)
  "time": "08:30:00",                  // String (HH:mm:ss)
  "checkType": "CHECK_IN",             // String: "CHECK_IN" | "CHECK_OUT"
  "method": "FACE_RECOGNITION",        // String
  "confidence": 0.85,                  // Double (L2 distance)
  "status": "PRESENT",                 // String: "PRESENT" | "ABSENT"
  "deviceInfo": "Pixel 6",             // String
  "location": {                        // Object (optional, future)
    "lat": 10.762622,
    "lng": 106.660172
  }
}
```

**Indexes** (Composite):
- `uid` (ASC) + `date` (ASC) + `timestamp` (DESC)
- `date` (ASC) + `checkType` (ASC)
- `uid` (ASC) + `timestamp` (DESC)

**Query Examples**:

```java
// Lấy điểm danh hôm nay của user
db.collection("attendanceRecords")
  .whereEqualTo("uid", currentUser.getUid())
  .whereEqualTo("date", "2025-11-30")
  .orderBy("timestamp", Query.Direction.DESCENDING)
  .get();

// Lấy tất cả check-in trong ngày
db.collection("attendanceRecords")
  .whereEqualTo("date", "2025-11-30")
  .whereEqualTo("checkType", "CHECK_IN")
  .get();
```

#### 4. **notifications** Collection (Future)

```javascript
notifications/{notificationId}
{
  "userId": "user_id",
  "title": "Nhắc nhở",
  "message": "Bạn chưa điểm danh hôm nay",
  "type": "REMINDER",
  "read": false,
  "timestamp": Timestamp
}
```

---

## 🔌 API & Services

### Firebase Authentication API

#### Register User
```java
FirebaseAuth.getInstance()
    .createUserWithEmailAndPassword(email, password)
    .addOnSuccessListener(result -> {
        FirebaseUser user = result.getUser();
        // Save to Firestore
    });
```

#### Login
```java
FirebaseAuth.getInstance()
    .signInWithEmailAndPassword(email, password)
    .addOnSuccessListener(result -> {
        // Navigate to dashboard
    });
```

#### Google Sign-In
```java
GoogleSignInOptions gso = new GoogleSignInOptions.Builder()
    .requestIdToken(getString(R.string.default_web_client_id))
    .requestEmail()
    .build();

GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, gso);
// Launch sign-in intent...
```

### Firestore API

#### Create Document
```java
Map<String, Object> data = new HashMap<>();
data.put("field", "value");

db.collection("collectionName")
    .document(documentId)
    .set(data)
    .addOnSuccessListener(aVoid -> {
        // Success
    });
```

#### Read Document
```java
db.collection("users")
    .document(userId)
    .get()
    .addOnSuccessListener(doc -> {
        String name = doc.getString("fullName");
    });
```

#### Real-time Listener
```java
ListenerRegistration listener = db.collection("attendanceRecords")
    .whereEqualTo("uid", userId)
    .addSnapshotListener((snapshots, error) -> {
        // Data changed
    });

// Don't forget to remove listener
listener.remove();
```

#### Query with Filters
```java
db.collection("attendanceRecords")
    .whereEqualTo("uid", userId)
    .whereGreaterThanOrEqualTo("timestamp", startTime)
    .whereLessThan("timestamp", endTime)
    .orderBy("timestamp", Query.Direction.DESCENDING)
    .limit(10)
    .get();
```

### TensorFlow Lite API

#### Load Model
```java
FaceNetModel faceNetModel = new FaceNetModel(context);
// Model loaded from assets/mobile_face_net.tflite
```

#### Get Face Embedding
```java
float[] embedding = faceNetModel.getFaceEmbedding(bitmap, faceRect);
// Returns 512-dimensional float array
```

#### Compare Embeddings
```java
float distance = EmbeddingsUtils.calculateL2Distance(embedding1, embedding2);
boolean isMatch = distance < THRESHOLD; // THRESHOLD = 1.0f
```

### ML Kit API

#### Face Detection
```java
FaceDetectorOptions options = new FaceDetectorOptions.Builder()
    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
    .setMinFaceSize(0.15f)
    .build();

FaceDetector detector = FaceDetection.getClient(options);

detector.process(inputImage)
    .addOnSuccessListener(faces -> {
        for (Face face : faces) {
            RectF boundingBox = face.getBoundingBox();
            // Process face
        }
    });
```

---

## 🔒 Bảo Mật

### 1. Firebase Security Rules

#### Firestore Rules
```javascript
// users collection
match /users/{userId} {
  allow read: if isSignedIn() && 
              (userId == currentUid() || isAdmin());
  allow create: if isSignedIn() && userId == currentUid();
  allow update: if isSignedIn() && 
                (userId == currentUid() || isAdmin());
  allow delete: if isAdmin();
}

// face_embeddings collection
match /face_embeddings/{userId} {
  allow read: if true;  // Public read for attendance
  allow write: if isSignedIn() && userId == currentUid();
}

// attendanceRecords collection
match /attendanceRecords/{recordId} {
  allow read: if isSignedIn();
  allow create: if true;  // Allow face recognition without login
  allow update, delete: if isAdmin();
}
```

#### Storage Rules
```javascript
service firebase.storage {
  match /b/{bucket}/o {
    match /avatars/{userId}/{fileName} {
      allow read: if true;
      allow write: if request.auth.uid == userId &&
                   request.resource.size < 5 * 1024 * 1024 &&
                   request.resource.contentType.matches('image/.*');
    }
  }
}
```

### 2. Data Encryption

#### Face Embeddings
- **Storage**: Float array (512 chiều)
- **Transport**: HTTPS (TLS 1.3)
- **At Rest**: Firestore encryption

#### Passwords
- **Hash**: Firebase Auth (bcrypt)
- **Salt**: Tự động bởi Firebase
- **Minimum Length**: 6 ký tự

### 3. Permissions

#### Android Manifest Permissions
```xml
<!-- Required -->
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android:permission.INTERNET" />

<!-- Optional -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

#### Runtime Permissions
```java
// Camera permission
if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
    != PackageManager.PERMISSION_GRANTED) {
    ActivityCompat.requestPermissions(this,
        new String[]{Manifest.permission.CAMERA},
        REQUEST_CODE_CAMERA);
}
```

### 4. Best Practices

#### Authentication
- ✅ Email verification (optional)
- ✅ Strong password policy
- ✅ Session timeout
- ✅ Refresh tokens

#### Face Recognition
- ✅ Liveness detection (future)
- ✅ Anti-spoofing (future)
- ✅ Multiple face registration
- ✅ Embedding versioning

#### Data Privacy
- ✅ GDPR compliance
- ✅ User consent
- ✅ Data retention policy
- ✅ Right to delete

---

## 🧪 Testing

### Unit Tests

#### Example: StatusType Test
```java
@Test
public void statusType_toText_returnsCorrectString() {
    assertEquals("Đúng giờ", StatusType.toText(StatusType.PRESENT));
    assertEquals("Đi trễ", StatusType.toText(StatusType.LATE));
    assertEquals("Vắng", StatusType.toText(StatusType.ABSENT));
}
```

#### Run Unit Tests
```bash
./gradlew test
```

### Instrumented Tests

#### Example: Firebase Test
```java
@Test
public void firestore_saveAndRetrieveUser_success() {
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    
    Map<String, Object> user = new HashMap<>();
    user.put("fullName", "Test User");
    
    db.collection("users").document("testId")
        .set(user)
        .addOnSuccessListener(aVoid -> {
            // Verify save success
        });
}
```

#### Run Instrumented Tests
```bash
./gradlew connectedAndroidTest
```

### UI Tests (Espresso)

#### Example: Login Test
```java
@Test
public void loginActivity_validCredentials_success() {
    onView(withId(R.id.editTextEmail))
        .perform(typeText("test@example.com"));
    onView(withId(R.id.editTextPassword))
        .perform(typeText("password123"));
    onView(withId(R.id.buttonLogin))
        .perform(click());
    
    onView(withId(R.id.dashboard_layout))
        .check(matches(isDisplayed()));
}
```

---

## 🐛 Troubleshooting

### Common Issues

#### 1. Camera Not Working

**Problem**: Camera không mở hoặc crash

**Solutions**:
```java
// Check permission
if (checkSelfPermission(Manifest.permission.CAMERA) 
    != PackageManager.PERMISSION_GRANTED) {
    requestPermissions(new String[]{Manifest.permission.CAMERA}, 1);
}

// Check camera availability
PackageManager pm = getPackageManager();
if (!pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
    Toast.makeText(this, "No camera available", Toast.LENGTH_SHORT).show();
}
```

#### 2. Face Recognition Fails

**Problem**: Không nhận diện được khuôn mặt

**Solutions**:
- ✅ Đảm bảo ánh sáng đủ
- ✅ Khuôn mặt trong khung
- ✅ Không đeo khẩu trang/kính đen
- ✅ Giảm threshold nếu quá strict

```java
// Adjust threshold
private static final float RECOGNITION_THRESHOLD = 1.2f; // Increase if too strict
```

#### 3. Firestore Permission Denied

**Problem**: Lỗi "Permission denied" khi query

**Solutions**:
```javascript
// Check Firestore rules
match /attendanceRecords/{recordId} {
  allow read: if request.auth != null;  // Require authentication
  allow write: if request.auth != null;
}
```

#### 4. Gradle Build Fails

**Problem**: Build error hoặc dependency conflict

**Solutions**:
```bash
# Clean build
./gradlew clean

# Invalidate cache
# Android Studio: File > Invalidate Caches / Restart

# Update Gradle wrapper
./gradlew wrapper --gradle-version=8.2
```

#### 5. Model Loading Error

**Problem**: TFLite model không load được

**Solutions**:
```java
// Check file exists
try {
    AssetFileDescriptor fileDescriptor = 
        getAssets().openFd("mobile_face_net.tflite");
    Log.d(TAG, "Model file size: " + fileDescriptor.getLength());
} catch (IOException e) {
    Log.e(TAG, "Model file not found", e);
}

// Ensure correct path in build.gradle
android {
    aaptOptions {
        noCompress "tflite"
    }
}
```

### Debug Tips

#### Enable Logging
```java
// Enable Firebase debug logging
adb shell setprop log.tag.FirebaseFirestore DEBUG

// Enable ML Kit logging
adb shell setprop log.tag.MLKit DEBUG
```

#### Monitor Network
```bash
# Use Charles Proxy or Fiddler
# Configure Android device proxy settings
```

#### Profile Performance
```bash
# Android Studio Profiler
# Run > Profile 'app'
```

---

## 📊 Performance Optimization

### 1. Face Recognition

**Optimize Model Inference**:
```java
// Use GPU delegate
GpuDelegate delegate = new GpuDelegate();
Interpreter.Options options = new Interpreter.Options();
options.addDelegate(delegate);
interpreter = new Interpreter(modelFile, options);
```

**Reduce Image Size**:
```java
// Resize before processing
Bitmap scaled = Bitmap.createScaledBitmap(
    original, 640, 480, false);
```

### 2. Firestore Queries

**Use Indexes**:
```javascript
// Create composite index for common queries
uid ASC, date ASC, timestamp DESC
```

**Limit Results**:
```java
db.collection("attendanceRecords")
    .limit(50)  // Don't fetch all records
    .get();
```

**Cache Data**:
```java
FirebaseFirestoreSettings settings = 
    new FirebaseFirestoreSettings.Builder()
        .setPersistenceEnabled(true)
        .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
        .build();
db.setFirestoreSettings(settings);
```

### 3. Image Loading

**Use Glide Caching**:
```java
Glide.with(context)
    .load(imageUrl)
    .diskCacheStrategy(DiskCacheStrategy.ALL)
    .into(imageView);
```

---

## 🚀 Roadmap

### Version 1.1 (Q1 2026)
- [ ] Liveness detection (chống giả mạo)
- [ ] Export báo cáo Excel/PDF
- [ ] Push notifications
- [ ] Dark mode

### Version 1.2 (Q2 2026)
- [ ] Geo-fencing (điểm danh theo vị trí)
- [ ] QR code check-in (backup method)
- [ ] Multi-language support
- [ ] Offline mode

### Version 2.0 (Q3 2026)
- [ ] Admin web dashboard
- [ ] Advanced analytics
- [ ] Integration with payroll systems
- [ ] Mobile app for iOS

---

## 👥 Contributors

- **Tung** - Lead Developer
- **Team TimeMaster** - Development Team

---

## 📄 License

```
Copyright (c) 2025 TimeMaster Team

Licensed under the MIT License
```

---

## 📞 Support

- **Email**: support@timemaster.com
- **GitHub**: https://github.com/timemaster/issues
- **Documentation**: https://docs.timemaster.com

---

## 🙏 Acknowledgments

- [Firebase](https://firebase.google.com/) - Backend infrastructure
- [TensorFlow](https://www.tensorflow.org/) - Machine learning framework
- [ML Kit](https://developers.google.com/ml-kit) - Face detection
- [MobileFaceNet](https://github.com/sirius-ai/MobileFaceNet_TF) - Face recognition model
- [MPAndroidChart](https://github.com/PhilJay/MPAndroidChart) - Chart library

---

**Last Updated**: November 30, 2025  
**Version**: 1.0.0  
**Document Version**: 1.0

