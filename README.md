# Smart Waste Management App

A modern Android application built with Kotlin and Jetpack Compose for efficient waste collection management, featuring three user roles: Admin, TPS Officer, and Driver with real-time tracking and photo proof collection.

## 🎯 Features

### 👤 Admin
- ✅ Firebase Auth Login with automatic approval
- ✅ User management (approve/reject TPS officers and drivers)
- ✅ TPS location management with Google Maps integration
- ✅ Assign officers to TPS locations
- ✅ Schedule pickup assignments with date picker
- ✅ Route optimization via genetic algorithm API
- ✅ Real-time driver tracking
- ✅ Send notifications to TPS officers
- ✅ Analytics dashboard with comprehensive statistics
- ✅ TPS status monitoring and alerts

### 🗑️ TPS Officer
- ✅ View assigned TPS locations
- ✅ Update TPS status (Full/Not Full) in real-time
- ✅ Receive notifications for status updates
- ✅ Status update history tracking
- ✅ Automated reminders at 8 PM daily (configured)

### 🚛 Driver
- ✅ View pickup schedules with route details
- ✅ Route visualization with optimized paths via Google Maps
- ✅ Photo proof uploads at each TPS location
- ✅ Real-time location tracking with GPS
- ✅ Complete pickup reports with notes
- ✅ Navigation assistance with turn-by-turn directions
- ✅ Progress tracking for collection routes

## 🏗️ Architecture

The app follows Clean Architecture principles with MVVM pattern:

```
app/
├── data/
│   ├── model/          # Data models (User, TPS, Schedule, Proof, etc.)
│   ├── repository/     # Data repositories with Firestore integration
│   ├── api/           # Retrofit API interfaces (Route optimization)
│   └── seeder/        # Database seeding utilities
├── di/                # Hilt dependency injection modules
├── presentation/      # UI layer
│   ├── auth/         # Authentication screens
│   ├── admin/        # Admin dashboard & management
│   ├── tps/          # TPS officer screens
│   ├── driver/       # Driver screens & navigation
│   ├── navigation/   # Navigation setup
│   └── splash/       # Splash screen
├── service/          # Firebase messaging service
├── ui/
│   ├── components/   # Reusable UI components
│   └── theme/        # Material Design 3 theme
└── util/             # Utility classes and helpers
```

## 🛠️ Technologies Used

- **Language**: Kotlin
- **UI**: Jetpack Compose with Material Design 3
- **Architecture**: MVVM + Clean Architecture + Hilt DI
- **Navigation**: Navigation Compose
- **Database**: Firebase Firestore with real-time listeners
- **Authentication**: Firebase Auth
- **Storage**: Firebase Storage (for proof photos)
- **Notifications**: Firebase Cloud Messaging
- **Maps**: Google Maps API with Compose integration
- **Location**: FusedLocationProvider with permissions handling
- **Networking**: Retrofit + OkHttp + Gson
- **Image Loading**: Coil Compose
- **Work Scheduling**: WorkManager (configured)
- **Permissions**: Accompanist Permissions

## 🔧 Setup Instructions

### 1. Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- Kotlin plugin enabled
- Google Maps API key
- Firebase project setup
- JDK 11 or higher

### 2. Firebase Configuration
1. Create a new Firebase project
2. Enable Authentication, Firestore, Storage, and Cloud Messaging
3. Download `google-services.json` and place it in the `app/` directory
4. Configure authentication methods (Email/Password)
5. Set up Firestore security rules (provided in `firestore.rules`)

### 3. Google Maps Setup
1. Enable Google Maps SDK and Places API in Google Cloud Console
2. Create an API key and restrict it appropriately
3. Add the API key to `local.properties`:
   ```
   GOOGLE_MAPS_API_KEY=your_api_key_here
   ```

### 4. Route Optimization API
The app integrates with a genetic algorithm API for route optimization:
- **Endpoint**: `https://rute-980275249842.asia-southeast2.run.app/optimize-route`
- **Method**: POST
- **Body**: TPS status data with coordinates
- **Response**: Optimized route coordinates and sequence

## 📱 Screen Flow

```
Splash Screen
     ↓
Authentication Screen (Login/Register)
     ↓
Role-based Dashboard:
├── Admin Dashboard → User Management → TPS Management → Schedule Management → Reports
├── TPS Officer Dashboard → Status Updates → History → Notifications
└── Driver Dashboard → Active Routes → Navigation → Photo Capture → Completion
```

## 🔐 User Roles & Permissions

### Admin
- Auto-approved upon registration
- Full access to all management features
- Can approve/reject other users
- Access to analytics and reports
- TPS location management
- Schedule creation and assignment

### TPS Officer
- Requires admin approval
- Limited to assigned TPS management
- Real-time status updates
- Receives automated notifications
- Access to status history

### Driver
- Requires admin approval
- Access to assigned schedules and routes
- GPS navigation with turn-by-turn directions
- Photo proof capture and upload
- Route completion reporting

## 🎨 UI Design

The app features a modern design with Material Design 3:
- **Primary Colors**: Professional blue theme (#1976D2 - #42A5F5)
- **Status Colors**: Green (#4CAF50) for available, Red (#E53935) for full
- **Typography**: Material Design 3 typography scale
- **Components**: Modern cards, chips, progress indicators
- **Design System**: Consistent 20dp padding, 16dp corner radius
- **Elevation**: Material Design 3 shadows and elevation

## 📦 Key Dependencies

```kotlin
// Core Android & Compose
implementation("androidx.core:core-ktx:1.16.0")
implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.1")
implementation("androidx.activity:activity-compose:1.10.1")
implementation(platform("androidx.compose:compose-bom:2024.09.00"))
implementation("androidx.compose.material3:material3")
implementation("androidx.navigation:navigation-compose:2.8.3")

// Firebase
implementation("com.google.firebase:firebase-auth:23.1.0")
implementation("com.google.firebase:firebase-firestore:25.1.4")
implementation("com.google.firebase:firebase-storage:21.0.1")
implementation("com.google.firebase:firebase-messaging:24.1.0")

// Google Play Services & Maps
implementation("com.google.android.gms:play-services-maps:19.2.0")
implementation("com.google.android.gms:play-services-location:21.3.0")
implementation("com.google.maps.android:maps-compose:4.4.1")

// Dependency Injection
implementation("com.google.dagger:hilt-android:2.52")
implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

// Networking
implementation("com.squareup.retrofit2:retrofit:2.11.0")
implementation("com.squareup.retrofit2:converter-gson:2.11.0")

// Image Loading & Permissions
implementation("io.coil-kt:coil-compose:2.7.0")
implementation("com.google.accompanist:accompanist-permissions:0.32.0")
```

## 🚀 Getting Started

1. Clone the repository
2. Open in Android Studio
3. Add your `google-services.json` file to the `app/` directory
4. Configure Google Maps API key in `local.properties`
5. Sync project with Gradle files
6. Run the app

## 📋 Firestore Structure

```
users/
  {uid}/
    name: String
    email: String
    role: 'ADMIN' | 'TPS' | 'DRIVER'
    approved: Boolean

tps/
  {tpsId}/
    name: String
    location: GeoPoint
    status: 'PENUH' | 'TIDAK_PENUH'
    assignedOfficerId: String
    address: String
    lastUpdated: Long

schedules/
  {scheduleId}/
    date: Timestamp
    driverId: String
    tpsRoute: Array[String]
    status: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED'
    createdAt: Long
    completedAt: Long (optional)

proofs/
  {proofId}/
    driverId: String
    tpsId: String
    scheduleId: String
    photoUrl: String
    timestamp: Timestamp
    verified: Boolean
```

## 🔄 Current Status

**Production Ready - Core Features Complete**
- ✅ Authentication system with role-based access
- ✅ User role management with approval workflow
- ✅ Firebase integration with real-time updates
- ✅ Modern UI design with Material Design 3
- ✅ Navigation structure with deep linking
- ✅ Complete dashboard screens for all user roles
- ✅ TPS status management with real-time updates
- ✅ Google Maps integration for location services
- ✅ Photo capture and proof upload system
- ✅ Route optimization API integration
- ✅ Real-time location tracking
- ✅ Push notifications infrastructure
- ✅ Database seeding utilities
- ✅ Comprehensive security rules

## 🛣️ Roadmap

### Phase 1 - Enhanced Features
- [ ] Advanced analytics and reporting dashboard
- [ ] Offline support with data synchronization
- [ ] Enhanced WorkManager integration for automated tasks
- [ ] Push notification automation
- [ ] Advanced route optimization algorithms

### Phase 2 - Extended Functionality
- [ ] Machine learning for waste prediction
- [ ] IoT sensor integration for smart bins
- [ ] Public API for third-party integrations
- [ ] Multi-language support
- [ ] Dark theme support

### Phase 3 - Scale & Performance
- [ ] Performance optimizations
- [ ] Automated testing suite
- [ ] CI/CD pipeline
- [ ] Monitoring and crash reporting
- [ ] Load testing and optimization

## 🧪 Testing

The app includes comprehensive testing setup:
- Unit tests with JUnit
- Instrumented tests with Espresso
- Compose UI testing
- Firebase emulator support

## 🔒 Security

- Firebase Authentication with secure rules
- Firestore security rules for data protection
- Role-based access control
- Photo upload validation
- Input sanitization and validation

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Create a Pull Request


## 🙏 Acknowledgments

- Material Design 3 for the beautiful design system
- Firebase for the robust backend infrastructure
- Google Maps for location services
- The Android development community

---

**Built with ❤️ for efficient waste management and environmental sustainability** 
