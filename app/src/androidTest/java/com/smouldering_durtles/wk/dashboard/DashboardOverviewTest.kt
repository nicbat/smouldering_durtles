package com.smouldering_durtles.wk.dashboard

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File

class DashboardOverviewTest {
    @get:Rule val compose = createComposeRule()

    @Test fun readyActionsDispatchExactlyOnce() {
        var reviews = 0
        var lessons = 0
        var browses = 0
        compose.setContent {
            DashboardTheme(false) {
                DashboardOverview(DashboardState(ready = true, reviews = 42, lessons = 12, level = 8), true,
                    { reviews++ }, { lessons++ }, {}, { browses++ })
            }
        }
        compose.onNodeWithText("Start reviews").performClick()
        compose.onNodeWithText("Start lessons").performClick()
        compose.onNodeWithText("Browse subjects").performClick()
        compose.runOnIdle { assertEquals(1, reviews); assertEquals(1, lessons); assertEquals(1, browses) }
        saveScreenshot("dashboard-light.png")
    }

    @Test fun activeSessionOnlyOffersResume() {
        var resumes = 0
        compose.setContent {
            DashboardTheme(false, dark = true) {
                DashboardOverview(DashboardState(ready = true, reviews = 42, lessons = 12, activeSession = true), true,
                    {}, {}, { resumes++ }, {})
            }
        }
        compose.onNodeWithText("Start reviews").assertDoesNotExist()
        compose.onNodeWithText("Start lessons").assertDoesNotExist()
        compose.onNodeWithText("Resume session").performClick()
        compose.runOnIdle { assertEquals(1, resumes) }
        saveScreenshot("dashboard-dark.png")
    }

    @Test fun busyStateDisablesSessionActions() {
        compose.setContent { DashboardTheme(false) {
            DashboardOverview(DashboardState(ready = true, reviews = 3, lessons = 1), false, {}, {}, {}, {})
        } }
        compose.onNodeWithText("Start reviews").assertIsNotEnabled()
        compose.onNodeWithText("Start lessons").assertIsNotEnabled()
        compose.onNodeWithText("Browse subjects").assertIsNotEnabled()
    }

    @Test fun loadingNeverClaimsQueueIsEmpty() {
        compose.setContent { DashboardTheme(true) {
            DashboardOverview(DashboardState(), true, {}, {}, {}, {})
        } }
        compose.onNodeWithText("Getting your study queue ready…").assertIsDisplayed()
        compose.onNodeWithText("You’re caught up for now.").assertDoesNotExist()
        compose.onNodeWithText("Start reviews").assertDoesNotExist()
    }

    @Test fun vacationSuppressesNewSessionActions() {
        compose.setContent { DashboardTheme(false) {
            DashboardOverview(DashboardState(ready = true, reviews = 3, lessons = 1, vacation = true), true, {}, {}, {}, {})
        } }
        compose.onNodeWithText("Vacation mode is active. Your study queue is paused.").assertIsDisplayed()
        compose.onNodeWithText("Start reviews").assertDoesNotExist()
        compose.onNodeWithText("Start lessons").assertDoesNotExist()
    }

    @Test fun emptyQueueAndLargeTextRemainUsable() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                DashboardTheme(false) {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        DashboardOverview(DashboardState(ready = true), true, {}, {}, {}, {})
                    }
                }
            }
        }
        compose.onNodeWithText("You’re caught up for now.").assertExists()
        compose.onNodeWithText("Browse subjects").performScrollTo().assertIsDisplayed()
        saveScreenshot("dashboard-large-text.png")
    }

    private fun saveScreenshot(name: String) {
        // Synthetic UI state only. Files are private to the disposable test installation.
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(context.filesDir, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
