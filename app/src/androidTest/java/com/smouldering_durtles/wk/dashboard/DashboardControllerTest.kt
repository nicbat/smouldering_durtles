package com.smouldering_durtles.wk.dashboard

import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.smouldering_durtles.wk.activities.AboutActivity
import org.junit.Rule
import org.junit.Test

/** Verifies the actual LiveData bridge inside the app's legacy activity/theme lifecycle. */
class DashboardControllerTest {
    @get:Rule val compose = createAndroidComposeRule<AboutActivity>()

    @Test fun existingActivityCanHostOverviewAndPropagateInteractionGuard() {
        lateinit var controller: DashboardController
        compose.activityRule.scenario.onActivity { activity ->
            val view = ComposeView(activity)
            activity.setContentView(view)
            controller = DashboardController.attach(view, activity, Runnable {}, Runnable {}, Runnable {}, Runnable {}, false)
            controller.setInteractionEnabled(true)
        }
        compose.onNodeWithText("Your next session").assertIsDisplayed()
        compose.onNodeWithText("Browse subjects").assertIsEnabled()
        compose.runOnIdle { controller.setInteractionEnabled(false) }
        compose.onNodeWithText("Browse subjects").assertIsNotEnabled()
    }
}
