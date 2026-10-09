# TransactionMate 💳

TransactionMate is a modern, full-featured Android personal finance and transaction management application built with **Jetpack Compose**, **Material 3**, and **MVVM architecture**. It connects directly to the TransactionMate REST API server.

## Build

Run `./build.sh` from any directory to build the debug APK. The script uses the project Gradle wrapper and creates a local debug keystore if one is not present. The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

---

## 🌟 Features

### 1. 📊 Financial Dashboard
- **Live Summary**: Real-time Total Balance, Income, and Expenses cards.
- **Current Month Spending**: Real-time debit consumption against monthly budgets.
- **Spending Breakdown**: Visual debit-vs-credit proportion chart and daily trend visualizations.
- **Quick Action Triggers**: Instant buttons for adding income, expenses, budgets, and viewing payment QR codes.
- **Recent Transactions**: Snapshot of recent activity with one-tap deep navigation.

### 2. 💸 Transactions Management
- **Record Transactions**: Add debit (- expense) and credit (+ income) records with amount, bank name, category, description, and date (`POST /create/payment`).
- **History View**: Transactions sorted by newest first (`POST /get/txn`).
- **Interactive Filtering**: Filter by transaction type (All, Debit, Credit) and categories (Food, Shopping, Salary, Bills, Health, Travel, etc.).
- **Live Search**: Instant keyword search matching descriptions, banks, and categories.
- **Form Validation**: Strict client-side validation ensuring positive numbers, dates, and account details before submission.

### 3. 🎯 Budget Management
- **Budget Tracking**: Create monthly budget targets (`POST /add/budget`).
- **Utilization Visualizer**: Progress bars indicating budget status with color alerts (Green < 75%, Amber warning 75–99%, Red alert ≥ 100%).
- **Budget Updates**: Live modification of existing budget targets (`POST /update/budget`).
- **Monthly Spending Overview**: Comparison of total monthly debit against total active budget limits.

### 4. 👤 Profile & Multi-Bank Accounts
- **User Profile**: Display user details (Name, Username, Mobile number, Email address) from `POST /get/account`.
- **Multiple Bank Accounts**: Support for multiple linked accounts with account type (Savings/Current), bank name, masked account number (`•••• 5678`), balance, and UPI VPA.
- **User Registration**: On-device registration (`POST /create`) to register new accounts and link bank accounts on the backend.
- **User Switching**: Switch active user profiles to load their respective transactions, budgets, and QR codes.
- **Administrative Action**: Dedicated confirmation-gated action to trigger `GET /send/report/` to dispatch financial report emails.

### 5. 📲 Payment QR Codes
- **QR Generation**: Request and display payment QR codes from the backend (`POST /get/qr`).
- **Multiple Accounts Support**: Switch between linked bank accounts and UPI VPAs.
- **Custom Payment Amount**: Request QR codes with pre-set payment amounts.
- **Share & Copy**: Native Android sharing via `Intent.ACTION_SEND` and one-tap copy of UPI VPAs.

### 6. 🌐 Dynamic Backend URL & Health Monitor
- **Default Server URL**: Automatically connects to **`http://147.224.251.137:8001`**.
- **Configurable Base URL**: Change server address on the fly without recompiling.
- **Pre-set Options**: Quick-select presets for the main remote server (`http://147.224.251.137:8001`), Android Emulator (`http://10.0.2.2:5000`, `http://10.0.2.2:8000`), or custom host addresses.
- **Live Ping**: Health check tool running `GET /` to verify backend reachability and latency.
- **Persistent Storage**: Retained across launches using Jetpack DataStore Preferences.

### 7. 🔐 Biometric Authentication & Secure App-Unlock
- **Hardware & Enrolled Biometric Detection**: Seamlessly checks whether Fingerprint or Face recognition is supported and enrolled on the device using AndroidX `BiometricManager`.
- **System Priority**: Prioritizes Fingerprint, then Face, with automatic fallback to Device PIN / pattern / password credentials (`BIOMETRIC_STRONG or DEVICE_CREDENTIAL`).
- **Strict Privacy Barrier**: Keeps all financial dashboards, balances, and records completely locked until authenticated.
- **Enrollment Guidance**: Automatically guides users to Android Security Settings (`Settings.ACTION_BIOMETRIC_ENROLL` or `Settings.ACTION_SECURITY_SETTINGS`) if no lock method is configured.
- **Lifecycle Background Timeout**: Automatically re-locks the application when returned from the background after 15 seconds.
- **Manual Lock Controls**: On-demand lock button and biometric toggle in the Profile screen for instant verification and testing.

### 8. 🎨 Theme Settings & Appearance Customization
- **Theme Modes**: Choose between **Light**, **Dark**, and **System Default** (automatically tracking Android OS system theme changes).
- **8 Dynamic Accent Palettes**:
  - Green (Default Brand Forest Green)
  - Blue
  - Purple
  - Teal
  - Orange
  - Red
  - Pink
  - Dynamic Color (Material You wallpaper-based theming on Android 12+)
- **Material 3 Color Harmony**: Fully coordinated light and dark `ColorScheme` definitions for each accent, ensuring accessible contrast ratios.
- **Immutable Financial Semantics**: Transaction status colors remain strictly guarded (green for credits/income, red for debits/expenses) across all themes.
- **Real-Time Live Preview**: Interactive preview card showcasing themed cards, buttons, progress indicators, and chips before and after applying changes.
- **DataStore Persistence**: All theme settings persist across app relaunches with instant zero-restart reactive updates.

---

## 📐 UX & UI Design Principles Adherence

The UI and UX architecture strictly incorporates foundational interaction design laws:

1. **Hick's Law (Minimizing Decision Complexity)**:
   - Form inputs provide sensible defaults (pre-filling today's date, defaulting to user's primary bank account, default categories).
   - Quick-select category and account chips reduce the number of choices and eliminate manual typing friction.
   - Quick action triggers on the dashboard present only the 4 most critical financial tasks.

2. **Fitts's Law (Touch Target Size & Proximity)**:
   - All interactive elements strictly enforce a minimum touch target size of 48dp x 48dp (`minimumInteractiveComponentSize()`).
   - Primary creation actions (FABs, primary submit buttons, and Bottom Navigation) are anchored in the ergonomic thumb zone at the bottom of the screen.

3. **Jakob's Law (Familiar Mental Models)**:
   - Follows standard Android financial and banking patterns: bottom navigation bar with conventional icons, card-based overview, standard account masked formatting (`•••• 5678`), and standard pull-to-refresh paradigms.
   - Preserves universally accepted green for income/credit (`+`) and red for expense/debit (`-`).

4. **Miller's Law (Cognitive Chunking)**:
   - Complex data is broken into digestible chunks (dashboard shows top 5 recent transactions rather than overwhelming infinite lists).
   - Theme color swatches are chunked into 4-item rows (well within the 7 ± 2 working memory rule).
   - Profile settings are organized into discrete thematic cards (Profile, Appearance, Security, Server Configuration).

5. **Gestalt Law of Proximity (Visual Relatedness)**:
   - Closely related elements are clustered together inside dedicated surface cards with consistent 8dp/16dp spacing.
   - Labels and input fields are explicitly coupled; Income and Expense metrics are paired in symmetrical sub-cards beneath Total Balance.
   - In budget cards, Spent, Utilization %, and Limit are visually bound directly underneath the progress bar.

6. **Aesthetic-Usability Effect (Visual Appeal Enhancing Usability)**:
   - Polished Material 3 design system featuring rounded cards (16dp–20dp), cohesive tone palettes, subtle depth elevations, and smooth touch ripples.
   - Aesthetically harmonious interfaces instill user trust in financial data precision and reliability.

---

## 📡 API Endpoints (`api.doc` Contracts)

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/` | API health check & server status |
| `POST` | `/create` | Register/create a new user profile with bank accounts |
| `POST` | `/create/payment` | Record a debit or credit transaction |
| `POST` | `/add/budget` | Create a new budget |
| `POST` | `/get/account` | Retrieve user account and bank details |
| `POST` | `/get/txn` | Retrieve transaction history |
| `POST` | `/get/budget` | Retrieve budget targets |
| `POST` | `/update/budget` | Update existing budget amount |
| `POST` | `/get/txn/statistics` | Retrieve aggregate debit/credit counts, totals, and daily stats |
| `POST` | `/get/monthly/txn` | Retrieve current month debit total |
| `GET` | `/send/report/` | Send financial report email (Admin workflow, confirmation-gated) |
| `POST` | `/get/qr` | Generate payment QR code for accounts and UPI VPAs |

---

## ⚙️ Setup and Network Configuration

### 1. Default API Server
The app defaults to:
```
http://147.224.251.137:8001
```
Android's `network_security_config.xml` explicitly permits cleartext HTTP traffic for `147.224.251.137`, `10.0.2.2`, and `localhost` during development.

### 2. Changing the Server URL
- Tap the **Server Settings icon (Dns)** in the top app bar on the Dashboard or Profile screen.
- Select from the presets or enter any custom IP/URL.
- Tap **Test Connection (Ping GET /)** to verify connectivity before saving.

---

## 🏗️ Architecture & Tech Stack

- **UI Framework**: Jetpack Compose (Material 3)
- **Architecture**: MVVM (Model-View-ViewModel) + Repository Pattern
- **Networking**: Retrofit 2 + OkHttp 4 + Moshi
- **Network Security**: Android Network Security Config (`res/xml/network_security_config.xml`)
- **Local Storage**: Jetpack DataStore Preferences (Base URL, active user preferences)
- **State Handling**: StateFlow & SharedFlow with lifecycle-aware collection
- **Image Loading**: Coil Compose

---

## 🔒 Security Notice

- **Authentication Limitation**: The backend currently operates without authentication/authorization middleware. Do not transmit production-grade banking secrets across unencrypted public networks until JWT/OAuth2 authentication and HTTPS with valid TLS certificates are deployed on the server.
- The app handles inputs safely and does not log plain-text personal or financial information.
