package com.smouldering_durtles.wk.dashboard

import android.text.format.DateFormat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.LifecycleOwner
import com.smouldering_durtles.wk.WkApplication
import com.smouldering_durtles.wk.GlobalSettings
import com.smouldering_durtles.wk.R
import com.smouldering_durtles.wk.api.ApiState
import com.smouldering_durtles.wk.livedata.*
import com.smouldering_durtles.wk.model.Session
import java.util.Date

/** Adapts existing LiveData to presentation state; never starts or modifies a session itself. */
class DashboardController private constructor() {
    private var state by mutableStateOf(DashboardState())
    private var actionsEnabled by mutableStateOf(false)

    fun setInteractionEnabled(enabled: Boolean) { actionsEnabled = enabled }

    companion object {
        @JvmStatic
        fun attach(view: ComposeView, owner: LifecycleOwner, reviews: Runnable, lessons: Runnable,
                   resume: Runnable, showTitle: Boolean = true): DashboardController {
            val controller = DashboardController()
            val context = view.context
            fun refresh() {
                val timeline = LiveTimeLine.getInstance().get()
                val session = Session.getInstance()
                val ready = GlobalSettings.getFirstTimeSetup() != 0 && session.isLoaded
                val counts = LiveTaskCounts.getInstance().get()
                val apiState = LiveApiState.getInstance().get()
                controller.state = DashboardState(
                    ready = ready,
                    lessons = timeline.numAvailableLessons,
                    reviews = timeline.numAvailableReviews,
                    levelUpLessons = timeline.numLevelUpLessons,
                    levelUpReviews = timeline.numLevelUpReviews,
                    activeSession = !session.isInactive,
                    vacation = LiveVacationMode.getInstance().get(),
                    level = LiveLevelDuration.getInstance().get().level,
                    daysAtLevel = LiveLevelDuration.getInstance().get().daysAtCurrentLevel.toDouble()
                        .takeIf { GlobalSettings.getFirstTimeSetup() != 0 && it.isFinite() && it >= 0 },
                    syncing = counts.syncCount > 0,
                    pendingUploads = counts.uploadCount,
                    audioDownloads = counts.audioCount,
                    pitchDownloads = counts.pitchInfoCount,
                    offline = !WkApplication.getInstance().onlineStatus.canCallApi(),
                    synced = apiState == ApiState.OK && counts.syncCount == 0,
                    syncError = apiState in listOf(ApiState.ERROR, ApiState.API_KEY_REJECTED, ApiState.API_KEY_MISSING)
                )
            }
            LiveTimeLine.getInstance().observe(owner) { refresh() }
            LiveSessionState.getInstance().observe(owner) { refresh() }
            LiveFirstTimeSetup.getInstance().observe(owner) { refresh() }
            LiveVacationMode.getInstance().observe(owner) { refresh() }
            LiveLevelDuration.getInstance().observe(owner) { refresh() }
            LiveTaskCounts.getInstance().observe(owner) { refresh() }
            LiveApiState.getInstance().observe(owner) { refresh() }
            refresh()
            view.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            view.setContent {
                DashboardTheme() {
                    DashboardOverview(controller.state, controller.actionsEnabled,
                        { reviews.run() }, { lessons.run() }, { resume.run() },
                        showTitle = showTitle)
                }
            }
            return controller
        }
    }
}
