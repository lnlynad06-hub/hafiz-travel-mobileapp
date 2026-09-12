# Project Architecture & Developer Guide

## System Overview

The system consists of two major sub-systems:
1. **Android Mobile Application** (`hafiz-travel-mobileapp`)
2. **Laravel Web System & Backend API** (`HAFIZ TRAVEL & TOUR`)

---

## 1. Android Mobile Application (`hafiz-travel-mobileapp`)

Built with native Java/Android, Retrofit, and OkHttp.

```
hafiz-travel-mobileapp/
├── app/
│   └── src/main/
│       ├── AndroidManifest.xml          # App declarations, permissions & activities
│       ├── java/com/hafiztraveltours/app/
│       │   ├── ui/                      # Screen Activities (Main, PackageDetail, Umrah, Tour, Auth, etc.)
│       │   ├── models/                  # Domain Data Models (UmrahPackage, PackageDetail, Podcast, FilterCriteria)
│       │   ├── adapters/                # RecyclerView Adapters (PackagePopularAdapter, UmrahPackageAdapter, PodcastAdapter)
│       │   ├── network/                 # Retrofit API Client, Service, DTOs & Responses
│       │   ├── services/                # Background Services & Broadcast Receivers (AzanService, PrayerTimeScheduler, etc.)
│       │   ├── utils/                   # Shared Utilities (SessionManager, FavoritesManager, LocaleHelper, BottomNavHelper)
│       │   └── views/                   # Custom Views & Dialogs (PrayerArcView, PackageFilterBottomSheet, RemoveFavoriteDialog)
│       └── res/                         # Layouts, Drawables, Values & Localized Strings (ms, en, ar, ja, ko, zh)
```

### Communication with Laravel Backend
- Network calls are managed by `ApiClient` and `ApiService` via Retrofit.
- API Endpoints:
  - `GET /api/v1/home` -> Curated homepage featured packages, popular Umrah & Tour packages.
  - `GET /api/v1/packages` -> Filtered package listings (category, search, pagination).
  - `GET /api/v1/packages/{id}` -> Full package details (rooms, flight, itinerary, gallery images).
  - `POST /api/v1/auth/login`, `POST /api/v1/auth/register`, `POST /api/v1/auth/google`, `POST /api/v1/auth/forgot-password` -> Authentication.

---

## 2. Laravel Web & Backend System (`HAFIZ TRAVEL & TOUR`)

Built with Laravel 11, Blade, TailwindCSS, MySQL, and REST APIs.

```
HAFIZ TRAVEL & TOUR/
├── app/
│   ├── Http/
│   │   ├── Controllers/
│   │   │   ├── Api/                     # Mobile REST API Controllers (PackageApiController, AuthApiController)
│   │   │   ├── Company/                 # Staff ERP & CRM Controllers (Packages, Departures, Bookings, Payments, etc.)
│   │   │   ├── Customer/                # Customer Portal & Public Website Controllers
│   │   │   ├── Corporate/               # Corporate Client Portal Controllers
│   │   │   └── Common/                  # Shared Auth & Locale Controllers
│   │   ├── Requests/                    # Form Validation & Input Sanitization
│   │   └── Middleware/                  # Auth, RBAC, Locale & Account Status Middleware
│   ├── Models/                          # Eloquent Models (Package, Departure, Booking, User, Customer, etc.)
│   ├── Services/                        # Business Logic Services (BookingService, InvoiceService, PaymentService, etc.)
│   └── Support/                         # View helpers, navigation, and role redirectors
├── config/                              # App, Database, Payments & Package Configuration
├── database/                            # Migrations, Seeders & Factories
├── resources/
│   ├── views/                           # Blade Templates (company, customer, corporate, layouts, components)
│   └── lang/                            # Multi-language translations (ms, en)
└── routes/
    ├── api.php                          # Mobile REST API routes
    ├── customer.php                     # Public website & customer portal routes
    ├── company.php                      # Staff CRM & ERP routes
    ├── corporate.php                    # Corporate B2B portal routes
    └── auth.php                         # Shared authentication routes
```
