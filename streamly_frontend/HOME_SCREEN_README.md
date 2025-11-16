# Native Android TV Home Screen Implementation

## Overview
This implementation creates a pixel-accurate native Android TV home screen based on the 'AAF_inicio Copy 2' Figma design (screen 2001:3396). The screen is built entirely with native Android TV components (no WebView) and includes proper D-pad navigation, focus management, and TV-optimized UI.

## Architecture

### Components

1. **HomeFragment** (`app/claro/tv/fragments/HomeFragment.kt`)
   - Main container for the home screen
   - Orchestrates all sections: navigation, hero banner, content rails
   - Implements proper focus management for TV navigation

2. **ContinueWatchingCard** (`app/claro/tv/views/ContinueWatchingCard.kt`)
   - Custom CardView for "Seguí viendo" (Continue Watching) rail
   - Features: thumbnail placeholder, progress bar, title area
   - Dimensions: 412dp x 312dp (regular), 474dp x 329dp (first card)
   - Focus effects: scales to 1.05x when focused

3. **TvChannelCard** (`app/claro/tv/views/TvChannelCard.kt`)
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
   - Cards: 5 content items with progress bars
   - Spacing: 10dp between cards

4. **TV Channels Section ("Canales de TV")**
   - Position: 72dp below Continue Watching
   - Title: 24sp, white, bold
   - Layout: Horizontal scrolling rail
   - Cards: 3 TV channel items with live badges
   - Spacing: 10dp between cards

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

### Key Handling
- **Arrow Keys**: Navigate between focusable elements
- **Enter/Select**: Activate focused element
- **Back**: Return to previous screen or exit app

## Current State

### Implemented ✓
- Native Android TV UI with CardView-based components
- Top navigation bar with menu items and avatar
- Hero banner placeholder
- Continue Watching rail with 5 cards
- TV Channels rail with 3 cards
- Progress bars on all content cards
- Live badges on TV channel cards
- D-pad navigation with focus management
- Proper overscan-safe margins
- Pixel-accurate dimensions matching Figma design

### Placeholder Data
Currently using hardcoded dummy data:
- Continue Watching: Rogue One, Ex Machina, Sing Street, 2012, Ad Astra
- TV Channels: Marca Claro Radio, E.T., with channel numbers and time slots

## Future Enhancements

### High Priority
1. **Image Loading**: Integrate Glide for thumbnail images
2. **Hero Carousel**: Implement auto-rotating hero banner with multiple highlights
3. **Dynamic Data**: Connect to backend API for real content
4. **Click Handlers**: Add navigation to detail screens on card click
5. **Search Functionality**: Implement search overlay

### Medium Priority
1. **Delete Functionality**: Add delete button with confirmation for Continue Watching items
2. **Yellow Navigation Indicator**: Add position indicator to Continue Watching rail
3. **Smooth Scrolling**: Enhance horizontal scroll with focus-driven auto-scroll
4. **Animations**: Add fade-in animations on screen load
5. **Avatar Menu**: Implement user profile dropdown

### Low Priority
1. **Error States**: Add error handling for missing images
2. **Empty States**: Handle empty rails gracefully
3. **Loading States**: Add loading indicators while fetching data
4. **Accessibility**: Enhanced TalkBack support

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
- Glide: Image loading (already included in dependencies)

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

## File Structure
```
app/src/main/java/app/claro/tv/
├── MainActivity.kt                      # Main activity hosting HomeFragment
├── SplashActivity.kt                    # Splash screen (3-second delay)
├── fragments/
│   └── HomeFragment.kt                  # Main home screen implementation
├── views/
│   ├── ContinueWatchingCard.kt         # Custom card for Continue Watching
│   └── TvChannelCard.kt                # Custom card for TV Channels
└── models/
    ├── ContentItem.kt                   # Data model for content items
    └── TvChannel.kt                     # Data model for TV channels
```

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
