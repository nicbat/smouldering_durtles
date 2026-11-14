package com.smouldering_durtles.wk.dashboard

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

class DashboardSyncTest {
    @get:Rule val compose = createComposeRule()

    @Test fun mediaDownloadsDoNotKeepSyncBusyAndOfflineUploadsRemainVisible() {
        val state = mutableStateOf(DashboardState(ready = true, synced = true, audioDownloads = 8))
        compose.setContent { DashboardTheme { DashboardOverview(state.value, true, {}, {}, {}) } }
        compose.onNodeWithText("Sync complete").assertIsDisplayed()
        compose.onNodeWithText("Downloads remaining · 8 audio · 0 pitch").assertIsDisplayed()
        compose.runOnIdle {
            state.value = state.value.copy(synced = false, syncing = true, pendingUploads = 3, offline = true)
        }
        compose.onNodeWithText("Saved on this device · 3 updates waiting for connection").assertIsDisplayed()
        compose.onNodeWithText("Sync complete").assertDoesNotExist()
        compose.runOnIdle { state.value = state.value.copy(offline = false) }
        compose.onNodeWithText("Uploading saved updates · 3 remaining").assertIsDisplayed()
        compose.runOnIdle { state.value = state.value.copy(syncError = true) }
        compose.onNodeWithText("Sync needs attention · check Sync & downloads").assertIsDisplayed()
        compose.onNodeWithText("Uploading saved updates · 3 remaining").assertDoesNotExist()
    }
}
