package com.smouldering_durtles.wk.dashboard

import android.text.format.DateFormat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.LifecycleOwner
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
                   resume: Runnable, browse: Runnable, dynamicColor: Boolean): DashboardController {
            val controller = DashboardController()
            val context = view.context
            fun refresh() {
                val timeline = LiveTimeLine.getInstance().get()
                val session = Session.getInstance()
                val ready = GlobalSettings.getFirstTimeSetup() != 0 && session.isLoaded
                val next = when {
                    timeline.hasUpcomingReviews() -> timeline.upcomingReviewDate
                    timeline.hasLongTermUpcomingReviews() -> timeline.longTermUpcomingReviewDate
                    else -> 0L
                }
                val upcoming = if (next > 0) context.getString(R.string.dashboard_next_review,
                    DateFormat.getMediumDateFormat(context).format(Date(next)),
                    DateFormat.getTimeFormat(context).format(Date(next))) else ""
                controller.state = DashboardState(
                    ready = ready,
                    lessons = timeline.numAvailableLessons,
                    reviews = timeline.numAvailableReviews,
                    activeSession = !session.isInactive,
                    vacation = LiveVacationMode.getInstance().get(),
                    level = LiveLevelDuration.getInstance().get().level,
                    daysAtLevel = LiveLevelDuration.getInstance().get().daysAtCurrentLevel.toDouble()
                        .takeIf { GlobalSettings.getFirstTimeSetup() != 0 && it.isFinite() && it >= 0 },
                    upcoming = upcoming,
                    syncing = !LiveTaskCounts.getInstance().get().isEmpty,
                    syncError = LiveApiState.getInstance().get() in listOf(ApiState.ERROR, ApiState.API_KEY_REJECTED)
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
                DashboardTheme(dynamicColor) {
                    DashboardOverview(controller.state, controller.actionsEnabled,
                        { reviews.run() }, { lessons.run() }, { resume.run() }, { browse.run() })
                }
            }
            return controller
        }
    }
}
