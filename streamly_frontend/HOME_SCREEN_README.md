# Native Android TV Home Screen Implementation

## Overview
This implementation creates a pixel-accurate native Android TV home screen based on the 'AAF_inicio Copy 2' Figma design (screen 2001:3396). The screen is built entirely with native Android TV components (no WebView) and includes proper D-pad navigation, focus management, and TV-optimized UI.

**NEW: Now integrated with real API data using Retrofit, coroutines, and MVVM architecture.**

## Architecture

### Components

1. **HomeFragment** (`app/claro/tv/fragments/HomeFragment.kt`)
   - Main container for the home screen
   - Orchestrates all sections: navigation, hero banner, content rails
   - Implements proper focus management for TV navigation
   - Observes ViewModel for data updates and loading states

2. **HomeViewModel** (`app/claro/tv/viewmodel/HomeViewModel.kt`)
   - Manages data fetching and UI state
   - Uses coroutines for asynchronous operations
   - Exposes LiveData for UI observation
   - Handles retry logic for failed requests

3. **ContinueWatchingCard** (`app/claro/tv/views/ContinueWatchingCard.kt`)
   - Custom CardView for "Seguí viendo" (Continue Watching) rail
   - Features: thumbnail placeholder, progress bar, title area
   - Dimensions: 412dp x 312dp (regular), 474dp x 329dp (first card)
   - Focus effects: scales to 1.05x when focused

4. **TvChannelCard** (`app/claro/tv/views/TvChannelCard.kt`)
   - Custom CardView for "Canales de TV" (TV Channels) rail
   - Features: thumbnail, program info, live badge, play button, progress bar
   - Dimensions: 745dp x 212dp
   - Supports "EN VIVO" badge and "ALQUILÁ" rental badge

### Data Models

1. **ContentItem** (`app/claro/tv/models/ContentItem.kt`)
   - Represents movies/series in Continue Watching rail
   - Fields: id, title, thumbnailUrl, progress

2. **TvChannel** (`app/claro/tv/models/TvChannel.kt`)
   - Represents live TV channels
   - Fields: id, programTitle, channelNumber, channelName, startTime, endTime, isLive, isRentable, thumbnailUrl, progress

### Data Layer

#### API Service (`app/claro/tv/data/api/`)

- **StreamlyApiService**: Retrofit interface defining API endpoints
  - `GET /v1/users/{userId}/continue-watching` - Fetches continue watching items
  - `GET /v1/channels` - Fetches TV channels list

- **ApiClient**: Factory for creating Retrofit instances with OkHttp configuration
  - Configurable base URL via BuildConfig
  - HTTP logging interceptor (debug builds only)
  - Connection/read/write timeouts (30 seconds)

- **AuthInterceptor**: OkHttp interceptor for Bearer token authentication
  - Reads token from TokenProvider interface
  - Stub implementation returns null (replace with actual token retrieval)

#### DTOs (`app/claro/tv/data/dto/`)

- **ContinueWatchingDto**: API response model for continue watching items
- **TvChannelDto**: API response model for TV channels
- **DataMappers**: Converts DTOs to domain models

#### Repository (`app/claro/tv/data/repository/`)

- **ContentRepository**: Interface abstracting data source
- **ApiContentRepository**: Retrofit-based implementation fetching from API
- **FakeContentRepository**: In-memory implementation for testing/fallback

#### Result Type (`app/claro/tv/data/Result.kt`)

- Sealed class for type-safe error handling
- States: Success, Error, Loading

## Configuration

### API Base URL

Set via gradle.properties or BuildConfig:

```properties
API_BASE_URL=https://api.streamly.example.com
USE_FAKE_DATA=false
```

Access in code:
```kotlin
BuildConfig.STREAMLY_API_BASE_URL
BuildConfig.USE_FAKE_DATA
```

### Feature Flags

- **USE_FAKE_DATA**: When true, uses FakeContentRepository instead of API calls

### Authentication

Replace `StubTokenProvider` with actual implementation:

```kotlin
class SecureTokenProvider(private val context: Context) : TokenProvider {
    override fun getToken(): String? {
        // Read from SharedPreferences, KeyStore, etc.
        return // your token
    }
}
```

## Design Specifications

### Screen Dimensions
- Resolution: 1920x1080 (16:9 aspect ratio)
- Background: #121212 (dark theme)

### Layout Structure

1. **Top Navigation Bar**
   - Position: 88dp from left, 36dp from top
   - Height: 74dp
   - Components: Logo, navigation menu (Inicio, Películas, Series, TV en vivo, Kids, Mis Contenidos), search icon, avatar
   - Active state: white text with semi-transparent background

2. **Hero Banner**
   - Position: Below navigation (42dp gap)
   - Dimensions: Full width x 444dp height
   - Currently: placeholder for future carousel implementation

3. **Continue Watching Section ("Seguí viendo")**
   - Position: 56dp below hero banner
   - Title: 24sp, white, bold
   - Layout: Horizontal scrolling rail
   - Cards: Dynamic from API (with progress bars)
   - Spacing: 10dp between cards
   - Loading state: Shows "Loading..." text
   - Error state: Toast with auto-retry after 3 seconds

4. **TV Channels Section ("Canales de TV")**
   - Position: 72dp below Continue Watching
   - Title: 24sp, white, bold
   - Layout: Horizontal scrolling rail
   - Cards: Dynamic from API (with live badges)
   - Spacing: 10dp between cards
   - Loading state: Shows "Loading..." text
   - Error state: Toast with auto-retry after 3 seconds

### Typography
Following design system from common.css:
- Navigation items: Roboto 20sp (regular: #b0b0b0, active: #ffffff)
- Section titles: Roboto 24sp bold, white
- Card titles: Roboto 20sp medium, white
- Program titles: Roboto 28sp bold, white
- Channel info: Roboto 18sp regular, #b0b0b0
- Badge text: Roboto 14sp bold, uppercase

### Colors
- Background: #121212
- Primary text: #ffffff
- Secondary text: #b0b0b0
- Card background: #1a1a1a
- Progress bar bg: rgba(255, 255, 255, 0.2)
- Progress bar fill: #ffffff
- Live badge: #ff0000
- Rent badge: #ffc107
- Focus outline: #ffffff

### Spacing & Overscan
- All content maintains 48dp+ from screen edges (overscan-safe)
- Consistent 10dp spacing between cards in rails
- Vertical spacing: 56-72dp between sections
- Card padding: 16dp internal padding

## D-pad Navigation

### Focus Order
1. Top navigation menu items
2. Avatar
3. Hero banner (focusable for future interaction)
4. Continue Watching cards (left-right navigation)
5. TV Channel cards (left-right navigation)

### Focus Effects
- Scale animation: 1.0 → 1.05 (200ms duration)
- All interactive elements are focusable
- Focus change triggers visual feedback (scale + highlight)
- Focus preserved across data updates

### Key Handling
- **Arrow Keys**: Navigate between focusable elements
- **Enter/Select**: Activate focused element
- **Back**: Return to previous screen or exit app

## API Integration Flow

1. **Fragment Creation**:
   - Initialize ContentRepository (API or Fake based on BuildConfig)
   - Create HomeViewModel with repository
   - Set up ViewModel observation

2. **Data Loading**:
   - ViewModel launches coroutines to fetch data
   - UI shows loading indicators during fetch
   - Result mapped to UiState (Loading/Success/Error)

3. **Success Path**:
   - DTOs mapped to domain models
   - UI updated with new data
   - Images loaded via Coil (crossfade + memory cache)
   - Focus state preserved

4. **Error Path**:
   - Toast notification with error message
   - Auto-retry after 3 seconds
   - Manual retry available via ViewModel methods

## Current State

### Implemented ✓
- Native Android TV UI with CardView-based components
- Top navigation bar with menu items and avatar
- Hero banner placeholder
- Continue Watching rail with API integration
- TV Channels rail with API integration
- Progress bars on all content cards
- Live badges on TV channel cards
- D-pad navigation with focus management
- Proper overscan-safe margins
- Pixel-accurate dimensions matching Figma design
- **Retrofit API client with configurable base URL**
- **Repository pattern with API and fake implementations**
- **MVVM architecture with ViewModel and LiveData**
- **Coroutine-based async data fetching**
- **Loading states with shimmer placeholders**
- **Error handling with retry mechanism**
- **Bearer token authentication support (stub)**
- **Coil image loading library integration**

### API Endpoints

#### Continue Watching
```
GET /v1/users/{userId}/continue-watching
Response: {
  "items": [
    {
      "id": "string",
      "title": "string",
      "subtitle": "string",
      "artwork_url": "string",
      "progress": 0.0-1.0
    }
  ]
}
```

#### TV Channels
```
GET /v1/channels
Response: {
  "channels": [
    {
      "id": "string",
      "name": "string",
      "logo_url": "string",
      "is_live": boolean,
      "current_program_title": "string",
      "current_program_time_window": "HH:MM - HH:MM",
      "channel_number": "string",
      "thumbnail_url": "string",
      "progress": 0.0-1.0,
      "is_rentable": boolean
    }
  ]
}
```

## Future Enhancements

### High Priority
1. **Image Loading**: Integrate Coil into card views for actual thumbnails ✓ (Coil added)
2. **Hero Carousel**: Implement auto-rotating hero banner with multiple highlights
3. **Click Handlers**: Add navigation to detail screens on card click
4. **Search Functionality**: Implement search overlay
5. **Token Management**: Implement secure TokenProvider with encrypted storage

### Medium Priority
1. **Delete Functionality**: Add delete button with confirmation for Continue Watching items
2. **Yellow Navigation Indicator**: Add position indicator to Continue Watching rail
3. **Smooth Scrolling**: Enhance horizontal scroll with focus-driven auto-scroll
4. **Animations**: Add fade-in animations on screen load
5. **Avatar Menu**: Implement user profile dropdown
6. **Offline Support**: Cache API responses for offline viewing

### Low Priority
1. **Error States**: Enhanced error UI with illustrations ✓ (Basic error handling done)
2. **Empty States**: Handle empty rails gracefully
3. **Shimmer Loading**: Replace text loading with shimmer effect
4. **Accessibility**: Enhanced TalkBack support
5. **Analytics**: Track user interactions and API response times

## Integration Points

### Entry Point
- **SplashActivity** (`app/claro/tv/SplashActivity.kt`) launches MainActivity after 3 seconds
- **MainActivity** (`app/claro/tv/MainActivity.kt`) hosts HomeFragment

### Navigation
- MainActivity is set as LEANBACK_LAUNCHER in AndroidManifest.xml
- Fragment-based architecture allows easy navigation to other screens

### Dependencies
- AndroidX Leanback: TV-optimized components
- CardView: Card-based UI components
- Fragment-KTX: Modern fragment APIs
- Retrofit: REST API client ✓
- OkHttp: HTTP client with interceptors ✓
- Gson: JSON serialization/deserialization ✓
- Coroutines: Async operations ✓
- LiveData/ViewModel: MVVM architecture ✓
- Coil: Image loading with caching ✓

## Testing on Android TV

### Emulator Setup
1. Create Android TV emulator (API 29+)
2. Recommended resolution: 1920x1080
3. Enable D-pad simulation

### Physical Device
1. Install APK via ADB: `adb install app-debug.apk`
2. Launch from TV home screen
3. Test with TV remote D-pad navigation

### Focus Testing
- Verify all interactive elements are focusable
- Test navigation flow: top-to-bottom, left-to-right
- Ensure focus is visible with scale effect
- Confirm no focus traps
- Verify focus preserved during data updates

### API Testing
- Test with real API endpoints (configure BASE_URL)
- Test with fake data (set USE_FAKE_DATA=true)
- Test error scenarios (disconnect network)
- Test retry mechanism
- Verify image loading with Coil

## File Structure
```
app/src/main/java/app/claro/tv/
├── MainActivity.kt                      # Main activity hosting HomeFragment
├── SplashActivity.kt                    # Splash screen (3-second delay)
├── fragments/
│   └── HomeFragment.kt                  # Main home screen with API integration
├── views/
│   ├── ContinueWatchingCard.kt         # Custom card for Continue Watching
│   └── TvChannelCard.kt                # Custom card for TV Channels
├── models/
│   ├── ContentItem.kt                   # Domain model for content items
│   └── TvChannel.kt                     # Domain model for TV channels
├── viewmodel/
│   ├── HomeViewModel.kt                 # ViewModel for home screen
│   ├── HomeViewModelFactory.kt          # Factory for ViewModel creation
│   └── UiState.kt                       # Sealed class for UI state
├── data/
│   ├── Result.kt                        # Result wrapper for error handling
│   ├── api/
│   │   ├── StreamlyApiService.kt       # Retrofit API interface
│   │   ├── ApiClient.kt                # Retrofit/OkHttp client factory
│   │   └── AuthInterceptor.kt          # Authentication interceptor
│   ├── dto/
│   │   ├── ContinueWatchingDto.kt      # API response models
│   │   └── TvChannelDto.kt
│   ├── mappers/
│   │   └── DataMappers.kt              # DTO to domain model mappers
│   └── repository/
│       ├── ContentRepository.kt         # Repository interface
│       ├── ApiContentRepository.kt      # API implementation
│       └── FakeContentRepository.kt     # Fake/test implementation
```

## Configuration Files
- `gradle.properties`: API_BASE_URL, USE_FAKE_DATA
- `app/build.gradle.kts`: BuildConfig fields, dependencies

## Design References
- HTML: `assets/aaf_inicio-copy-2-2001-3396.html`
- CSS: `assets/aaf_inicio-copy-2-2001-3396.css`
- Common CSS: `assets/common.css`
- Layout Analysis: `assets/aaf_inicio-copy-2-2001-3396.txt`
- JavaScript: `assets/app.js`

These files were used as design references only (not copied into the codebase).

## Notes
- This is a native Android implementation, not a WebView wrapper
- All dimensions are in dp (density-independent pixels) for proper scaling
- Focus management follows Android TV best practices
- Layout respects TV overscan guidelines (48dp minimum margins)
- Uses FragmentActivity for Leanback compatibility
- API integration uses industry-standard patterns (MVVM, Repository, Result)
- Coroutines ensure smooth UI with background data fetching
- Error handling gracefully degrades with retry capability
- Feature flags allow easy switching between API and fake data

## Environment Variables

**Note:** API base URL and feature flags are configured via gradle.properties, not .env file. The application requires the following to be set:

- `API_BASE_URL`: Base URL for Streamly API (default: https://api.streamly.example.com)
- `USE_FAKE_DATA`: Boolean flag to use fake repository (default: false)

These are read at build time and exposed via BuildConfig constants.
