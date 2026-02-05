# Movie Recap App - Features and Functionalities

## Overview
The Movie Recap App is an Android application designed for browsing and watching movie recaps. It uses a modern tech stack centered around Jetpack Compose and Firebase.

## Key Functionalities

### 1. Movie Discovery
- **Categorized Lists**: Movies are organized by categories (e.g., "Top Picks", "Action", "Romance"). 
- **Top Picks**: A highlighted section displaying movies in portrait format for better visibility.
- **Infinite Scrolling**: Category rows support infinite scrolling for a seamless browsing experience.
- **All Movies Grid**: A dedicated screen to view the entire catalog in a grid layout.

### 2. Video Playback
- **Media3 ExoPlayer**: Utilizes the latest Media3 library for robust video playback.
- **Subtitle Support**: 
    - Supports WebVTT and SRT formats.
    - Automatic timestamp shifting to handle subtitle delays specified by the backend.
- **Playback Controls**: Features a custom UI with Play/Pause and Fullscreen toggle.
- **Background Playback Handling**: The video automatically pauses when the user minimizes the app or switches to another application.

### 3. Movie Details
- **Rich Metadata**: Displays movie title, release year, duration, and a full description.
- **Smart Hero Section**: Shows the movie thumbnail initially and seamlessly transitions to the video player when "Watch Now" is clicked.
- **Gradient Overlays**: Uses aesthetic gradient overlays for text readability and visual appeal.

### 4. Monetization (Ads)
- **Rewarded Ads**: Integrated with Google AdMob. Users are shown a rewarded ad before they can start watching a video, ensuring monetization while providing free content.

### 5. Backend Integration
- **Dynamic Content**: All movie data is fetched in real-time from a Firebase Realtime Database.
- **Remote Configuration**: Subtitle URLs, video links, and movie metadata are all managed remotely.

## Technical Highlights
- **Architecture**: Single Activity with Jetpack Compose Navigation.
- **Data Layer**: Repository pattern fetching from Firebase REST endpoint.
- **Concurrency**: Kotlin Coroutines for off-main-thread operations (network calls, subtitle parsing).
- **UI System**: Material3 Design with custom theming (`GomoPrimaryPink`, `GomoPurpleMid`).
