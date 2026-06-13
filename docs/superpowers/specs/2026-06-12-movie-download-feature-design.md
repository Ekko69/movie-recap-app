# Movie Download Feature — Design Spec

**Date:** 2026-06-12  
**Status:** Approved for implementation

---

## Context

The app currently streams all movie content from Firebase-hosted URLs — there is no offline capability. Users have requested the ability to save movies to their device for watching without an internet connection. This feature adds per-movie downloads with a free tier (3 downloads) and a ₱200 lifetime IAP to unlock unlimited downloads. It is implemented alongside the existing ₱300 "Remove Ads" IAP as a fully separate, independent purchase.

---

## Decisions Made

| Question | Decision |
|---|---|
| Download limit enforcement | Device-local via SharedPreferences (consistent with existing Favorites/WatchHistory pattern) |
| Downloads section location | "MY DOWNLOADS" row on HomeScreen (same pattern as Top Picks, Recently Watched) |
| Limit-hit behavior | Hard paywall dialog — upgrade to ₱200 IAP only, no delete escape hatch |
| Download button placement | Both movie cards (icon overlay) AND detail screen (button next to Play) |
| IAP relationship | Two separate independent IAPs: ₱300 Remove Ads, ₱200 Unlimited Downloads |
| Implementation approach | Direct file download via coroutines + `HttpURLConnection` into scoped external storage |

---

## Architecture

### New Files

#### `util/DownloadManager.kt`
Central utility class, follows the `FavoritesManager` / `WatchHistoryManager` pattern.

**SharedPreferences:** `download_prefs`
- `downloaded_movie_ids` — JSON list of downloaded movie IDs
- `downloaded_file_paths` — JSON map of `movieId → absolute file path`

**Compose state (for UI recomposition):**
- `downloadedMovieIds: SnapshotStateList<String>`
- `downloadProgress: SnapshotStateMap<String, Int>` — per-movie progress 0–100

**Key methods:**
- `downloadMovie(movie: Movie, context: Context, onProgress: (Int) -> Unit)` — coroutine; streams video via `HttpURLConnection` into `context.getExternalFilesDir("downloads")/<movieId>.mp4`; persists path on completion
- `deleteDownload(movieId: String)` — deletes file from disk, removes from prefs and state
- `getLocalUri(movieId: String): Uri?` — returns `Uri.fromFile(...)` for local playback, or `null` if not downloaded
- `isDownloaded(movieId: String): Boolean`
- `downloadCount: Int` — derived from `downloadedMovieIds.size`

**Free tier logic (inside `downloadMovie`):**
```
if (!purchaseManager.isUnlimitedDownloads.value && downloadCount >= 3) {
    → emit limit-reached signal (callback or state flag)
    → return without downloading
}
```

#### `ui/screens/DownloadsScreen.kt`
Full-page screen navigated to from the "See All" link in the HomeScreen section.

**Contents:**
- Header: "My Downloads" title + `X of 3 free slots used` subtitle (hidden when premium)
- Progress bar: red fill showing `downloadCount / 3` (hidden when premium)
- Soft upgrade banner: "Unlock Unlimited — ₱200" CTA (hidden when premium)
- LazyColumn of downloaded movies, each row showing:
  - Thumbnail (Coil), title, year + duration + file size
  - **Play** button (red) — navigates to DetailScreen with local URI
  - **Delete** button (red outline trash icon) — calls `DownloadManager.deleteDownload()`
- Empty state: illustration + "No downloads yet. Find a movie and tap ⬇ to save it."

#### `ui/components/DownloadLimitDialog.kt`
Bottom sheet dialog shown when a free user attempts to download beyond the 3-movie limit.

**Contents:**
- Lock icon, "Download Limit Reached" title
- Body: "You've used all 3 free downloads. Unlock unlimited downloads to keep going."
- Primary CTA: "Unlock Unlimited Downloads — ₱200" → triggers `PurchaseManager.launchPurchaseFlow(unlimited)`
- Secondary: "Cancel" text button (dismisses dialog)

#### `ui/components/DownloadButton.kt`
Reusable composable encapsulating all download states.

**States:**
- `Idle` — green outlined "⬇ Download" button
- `Downloading(progress: Int)` — grey "↻ 64%" with progress bar beneath
- `Downloaded` — green filled "✓ Downloaded" button
- `Locked` — shown on movie cards only when limit reached + not premium (lock icon overlay)

---

### Modified Files

#### `util/PurchaseManager.kt`
Add a second product alongside the existing `remove_ads_lifetime`:

- New product ID: `unlimited_downloads_lifetime`
- New state: `isUnlimitedDownloads: MutableState<Boolean>`
- Persisted in SharedPreferences: `unlimited_downloads_purchased` (Boolean)
- `productPriceDownloads: MutableState<String>` — localized price for the new product
- New method `launchDownloadsPurchaseFlow()` added (keeps existing `launchPurchaseFlow()` call sites untouched)
- Purchase restore (`queryPurchasesAsync`) must check both product IDs

#### `ui/screens/HomeScreen.kt`
- Add "MY DOWNLOADS" `MovieSection` row below "Recently Watched"
- Only visible when `downloadManager.downloadCount > 0`
- "See All" link navigates to `downloads` route
- Movie cards in this section show `DownloadButton` in `Downloaded` state (checkmark)

#### `ui/screens/DetailScreen.kt`
- Add `DownloadButton` composable below the existing play controls
- If `downloadManager.isDownloaded(movie.id)`, pass `downloadManager.getLocalUri(movie.id)` to `VideoPlayer` instead of `movie.videoURL`
- When download completes, `VideoPlayer` URI updates automatically via state

#### `ui/components/MovieCard.kt`
- Add small circular download icon overlay in bottom-right corner of thumbnail
- States: green checkmark (downloaded), grey arrow (not downloaded), lock (limit reached, not premium)
- Tapping the icon triggers the same download flow as the detail screen button (checks limit, shows dialog if needed, or starts download)

#### `MainActivity.kt`
- Add `downloads` navigation route: `composable("downloads") { DownloadsScreen(...) }`
- Pass `downloadManager` and `purchaseManager` into `DownloadsScreen`

---

## Data Flow

```
User taps Download icon / button
  ↓
DownloadManager.isDownloaded(movieId)?
  → true:  show Downloaded state (no-op)
  → false:
      PurchaseManager.isUnlimitedDownloads OR downloadCount < 3?
        → YES: launch downloadMovie() coroutine
                 emit progress → DownloadButton shows Downloading state
                 on completion → persist path, update state → Downloaded state
        → NO:  show DownloadLimitDialog
                 user taps "Unlock ₱200" → PurchaseManager.launchDownloadsPurchaseFlow()
                 on purchase success → isUnlimitedDownloads = true → dialog dismisses
                 user taps Download again to start the download
```

---

## File Storage

- **Location:** `context.getExternalFilesDir("downloads")/<movieId>.mp4`
- **Scoped storage:** Private to the app — no `READ/WRITE_EXTERNAL_STORAGE` permission needed
- **Cleanup:** Files are auto-deleted when the app is uninstalled
- **Subtitle files:** Not downloaded (subtitles remain streamed; if offline playback is used without connectivity, subtitles will not load — acceptable for v1)

---

## IAP Products

| Product ID | Price | State key |
|---|---|---|
| `remove_ads_lifetime` | ₱300 | `isAdFree` |
| `unlimited_downloads_lifetime` | ₱200 | `isUnlimitedDownloads` |

Both are one-time non-consumable purchases. Both must be restored in `queryPurchasesAsync`.

---

## Edge Cases

- **Download interrupted** (network lost mid-download): partial file is deleted; state reverts to `Idle`; user can retry
- **App killed during download**: coroutine is cancelled; partial file cleaned up on next `DownloadManager` init via file existence check vs. prefs
- **Movie deleted from Firebase** (remote URL gone): downloaded local copy still plays fine; no impact
- **Storage full**: `IOException` during write → show a toast "Not enough storage space"; revert to `Idle`
- **Premium user** (`isUnlimitedDownloads = true`): no limit check, no limit dialog, progress bar and slot counter hidden in Downloads screen

---

## Verification

1. **Free tier limit:** Download 3 movies → attempt a 4th → `DownloadLimitDialog` appears → Cancel → no download occurs
2. **IAP unlock:** From the dialog, tap "Unlock ₱200" → complete test purchase → 4th download proceeds automatically
3. **Progress:** While downloading, Detail screen shows "↻ X%" with a progress bar; completes to "✓ Downloaded"
4. **Offline playback:** Enable airplane mode → open a downloaded movie → video plays from local file with no buffering
5. **Delete:** On Downloads screen, delete a movie → file removed from disk → card disappears from Downloads section
6. **HomeScreen section:** With 0 downloads, "MY DOWNLOADS" row is hidden. After first download, it appears.
7. **Movie card state:** Downloaded movies show green checkmark overlay on their card everywhere in the app
8. **Purchase restore:** Uninstall + reinstall → `queryPurchasesAsync` restores `isUnlimitedDownloads` — but local downloads are lost (scoped storage wipes on uninstall, acceptable)
