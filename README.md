# NIT CRM: Enterprise Field Sales & Service Suite

**NIT CRM** is a production-ready enterprise solution designed for on-site computer, network, and CCTV service operations. It provides a robust, offline-first experience with a premium, sleek dark-mode visual design.

---

## 🚀 Features

- **Offline-First Synchronization**: Seamless operation in low-connectivity environments with background sync via WorkManager.
- **Enterprise Invoicing**: Professional invoice generation with support for upfront payments, dues, and draft modes.
- **Customer Management**: Detailed customer tracking including service history, interaction timestamps, and pipeline status.
- **Inventory & Scanner**: Integrated barcode scanning for quick item search and inventory management.
- **Field Service Suite**: Tools for service tickets, quotations, and digital signature capture.
- **Premium UI/UX**: "Midnight Obsidian" dark-mode design system with micro-animations and smooth transitions.

## 🛠️ Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose (Material 3)
- **Architecture**: Clean Architecture (Multi-Module) with MVVM/MVI
- **Persistence**: Room Database with SQLCipher encryption
- **Networking**: Retrofit & OkHttp
- **Cloud/Sync**: Firebase Firestore & Cloud Storage
- **DI**: Dagger Hilt
- **Background Tasks**: WorkManager
- **AI Integration**: Gemini AI (via Google AI Studio/Firebase AI SDK)
- **Scanning**: ML Kit Barcode Scanning & CameraX

## 🏁 Getting Started

### Prerequisites

- [Android Studio Ladybug](https://developer.android.com/studio) or newer
- JDK 17+
- Android Device or Emulator (API 24+)

### Installation

1.  **Clone the Repository**
    ```bash
    git clone https://github.com/your-username/nitcrm.git
    cd nitcrm
    ```

2.  **Configure Environment Variables**
    Create a `.env` file in the project root (see `.env.example`):
    ```properties
    GEMINI_API_KEY=your_api_key_here
    ```

3.  **Build the Project**
    Open the project in Android Studio and allow Gradle to sync.

4.  **Run the App**
    Select your device and click **Run**.

## 🏗️ Architecture

The project follows a modular Clean Architecture pattern:

- `:app`: Entry point and dependency injection.
- `:feature:*`: Feature-specific modules (Dashboard, Billing, Customers, etc.).
- `:core:data`: Repositories and data synchronization logic.
- `:core:database`: Room database and local persistence.
- `:core:network`: Remote API clients.
- `:core:model`: Pure domain entities.
- `:core:designsystem`: Reusable UI components and theme.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

Developed by **Nazrul IT Solutions**.
