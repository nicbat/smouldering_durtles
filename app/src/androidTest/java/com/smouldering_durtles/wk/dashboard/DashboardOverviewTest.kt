package com.smouldering_durtles.wk.dashboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File

class DashboardOverviewTest {
    @get:Rule val compose = createComposeRule()

    @Test fun embeddedOverviewKeepsReadableTextAcrossThemesAndStates() {
        var dark by androidx.compose.runtime.mutableStateOf(false)
        var dynamic by androidx.compose.runtime.mutableStateOf(false)
        var state by androidx.compose.runtime.mutableStateOf(DashboardState())
        var expectedInk = Color.Unspecified
        var surface = Color.Unspecified
        compose.setContent {
            DashboardTheme(dynamic, dark) {
                expectedInk = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                surface = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerLow
                // The real parent is an Android View: its background does not provide Compose content color.
                Box(Modifier.background(surface)) {
                    DashboardOverview(state, true, {}, {}, {}, {}, showTitle = false)
                }
            }
        }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val ready = DashboardState(ready = true, reviews = 42, lessons = 12, upcoming = "Next review fixture")
        val cases = listOf(
            DashboardState() to context.getString(com.smouldering_durtles.wk.R.string.dashboard_loading),
            DashboardState(ready = true) to context.getString(com.smouldering_durtles.wk.R.string.dashboard_caught_up),
            ready.copy(activeSession = true) to context.getString(com.smouldering_durtles.wk.R.string.dashboard_session_waiting),
            ready.copy(vacation = true) to context.getString(com.smouldering_durtles.wk.R.string.dashboard_vacation),
            ready to "Next review fixture")
        for (useDark in listOf(true, false)) for (useDynamic in listOf(false, true)) {
            for ((model, message) in cases) {
                compose.runOnIdle { dark = useDark; dynamic = useDynamic; state = model }
                compose.waitForIdle()
                val labels = listOf(message) + if (model.ready) listOf("Reviews ready", "Lessons ready") else emptyList()
                for (label in labels) {
                    val layouts = mutableListOf<TextLayoutResult>()
                    compose.onNodeWithText(label).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                    val actualInk = layouts.single().layoutInput.style.color
                    assertEquals("$label (dark=$useDark, dynamic=$useDynamic)", expectedInk, actualInk)
                    assertTrue("$label must contrast with the card", androidx.core.graphics.ColorUtils.calculateContrast(
                        actualInk.toArgb(), surface.toArgb()) >= 4.5)
                }
            }
            saveScreenshot("dashboard-embedded-dark-$useDark-dynamic-$useDynamic.png")
        }
    }

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

    @Test fun levelUpCountsAndIndependentPriorityControlsAreVisible() {
        var lessons by androidx.compose.runtime.mutableStateOf(false)
        var reviews by androidx.compose.runtime.mutableStateOf(false)
        compose.setContent {
            DashboardTheme(false) {
                DashboardOverview(DashboardState(ready = true, reviews = 42, lessons = 12, level = 8,
                    levelUpLessons = 2, levelUpReviews = 6, prioritizeLessons = lessons, prioritizeReviews = reviews), true,
                    {}, {}, {}, {}, { l, r -> lessons = l; reviews = r })
            }
        }
        compose.onNodeWithText("2 on level-up path").assertIsDisplayed()
        compose.onNodeWithText("6 on level-up path").assertIsDisplayed()
        saveScreenshot("dashboard2-level-up-overview.png")
        compose.onNodeWithText("Level-up priority").performClick()
        compose.onNodeWithText("Prioritize level-up lessons").performClick()
        compose.runOnIdle { assertTrue(lessons); assertFalse(reviews) }
        compose.onNodeWithText("Prioritize level-up reviews").performClick()
        compose.runOnIdle { assertTrue(lessons); assertTrue(reviews) }
        saveScreenshot("dashboard2-level-up-options.png", dialog = true)
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

    private fun saveScreenshot(name: String, dialog: Boolean = false) {
        // Synthetic UI state only. Files are private to the disposable test installation.
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val bitmap = (if (dialog) compose.onNode(isDialog()) else compose.onRoot())
            .captureToImage().asAndroidBitmap()
        File(context.filesDir, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
