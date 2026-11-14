package com.smouldering_durtles.wk.dashboard

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

class DashboardSyncTest {
    @get:Rule val compose = createComposeRule()

    @Test fun nextSessionContainsStudyActionsWithoutSyncInformation() {
        compose.setContent {
            DashboardTheme { DashboardOverview(DashboardState(ready = true, reviews = 3), true, {}, {}, {}) }
        }
        compose.onNodeWithText("3 reviews").assertIsDisplayed()
        for (text in listOf("Sync complete", "Last sync succeeded", "Uploading", "Downloads remaining", "Updating your study data")) {
            compose.onNodeWithText(text, substring = true).assertDoesNotExist()
        }
    }
}
