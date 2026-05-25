# Task Progress - Fix Auction Results Auto-Refresh

## Analysis Summary

After thorough codebase analysis, I've identified the root causes:

### Root Cause 1: Broadcast listener lifecycle gap
When the user navigates to other tabs, `onBeforeHide()` removes the AuctionResultsController's listener. Broadcasts (`AUCTION_RESULTS_UPDATE_BROADCAST`, `SERVER_BROADCAST_REFRESH_RESULTS`) arriving during that time are **completely lost**. When the user returns, a new controller fetches fresh data via `loadAuctionResults()` in `initialize()` - this part works.

### Root Cause 2: No periodic fallback polling
The only way data refreshes is via broadcasts OR manual refresh. If a broadcast is missed for ANY reason (socket hiccup, race condition, multiple rapid broadcasts), there's **no safety net** to recover. The table stays stale until the user manually clicks "Làm mới".

### Root Cause 3: Mock data masks empty state
When server returns empty results list (e.g., no finished auctions yet), `loadMockData()` is called instead of showing an empty table. This is misleading and prevents users from seeing the true empty state.

### Root Cause 4: Race condition on rapid tab switching
When `MainController.switchContent()` is called, the new controller's `initialize()` adds a listener BEFORE the old controller's `onBeforeHide()` removes its listener. If a broadcast or response arrives in this narrow window, it could be processed by both or lost entirely.

## Fix Plan

- [x] Analyze codebase to identify root causes
- [x] Fix AuctionResultsController: Add periodic polling (every 15 seconds) as fallback for missed broadcasts
- [x] Fix AuctionResultsController: Properly handle empty server results instead of showing mock data
- [x] Fix AuctionResultsController: Add throttle mechanism to prevent request spamming
- [x] Fix AuctionResultsController: Properly clean up the polling timer when hidden