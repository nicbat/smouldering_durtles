package com.smouldering_durtles.wk.dashboard

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.compose.ui.platform.ComposeView
import androidx.core.widget.NestedScrollView
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.smouldering_durtles.wk.Actment
import com.smouldering_durtles.wk.R
import com.smouldering_durtles.wk.activities.AboutActivity
import com.smouldering_durtles.wk.db.Converters
import com.smouldering_durtles.wk.db.model.Subject
import com.smouldering_durtles.wk.db.model.SubjectEntity
import com.smouldering_durtles.wk.enums.ActiveTheme
import com.smouldering_durtles.wk.enums.SubjectType
import com.smouldering_durtles.wk.livedata.*
import com.smouldering_durtles.wk.model.*
import com.smouldering_durtles.wk.views.*
import com.smouldering_durtles.wk.ui.LearningColors
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File
import java.lang.reflect.Proxy

/** Synthetic view models through the real chart observers; no database/account fixtures. */
@RunWith(AndroidJUnit4::class)
class DashboardAppearanceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val prefs = PreferenceManager.getDefaultSharedPreferences(instrumentation.targetContext)
    private val keys = listOf("first_time_setup", "show_timeline", "show_srs_breakdown", "show_level_progression", "timeline_chart_style")
    private lateinit var previous: Map<String, Any?>
    private val oldTimeline = LiveTimeLine.getInstance().get()
    private val oldProgress = LiveLevelProgress.getInstance().get()
    private val oldSrs = LiveSrsBreakDown.getInstance().get()
    private val oldVacation = LiveVacationMode.getInstance().get()
    private var search: AdvancedSearchParameters? = null
    private lateinit var actment: Actment

    @Before fun prepare() {
        previous = keys.associateWith { prefs.all[it] }
        prefs.edit().putInt("first_time_setup", 1).putBoolean("show_timeline", true)
            .putBoolean("show_srs_breakdown", true).putBoolean("show_level_progression", true)
            .putString("timeline_chart_style", "ITEM_TYPE").commit()
        LiveVacationMode.getInstance().post(false)
    }

    @After fun restore() {
        val edit = prefs.edit()
        previous.forEach { (key, value) -> when(value) {
            null -> edit.remove(key)
            is Boolean -> edit.putBoolean(key, value)
            is Int -> edit.putInt(key, value)
            is String -> edit.putString(key, value)
        } }
        edit.commit()
        LiveTimeLine.getInstance().post(oldTimeline)
        LiveLevelProgress.getInstance().post(oldProgress)
        LiveSrsBreakDown.getInstance().post(oldSrs)
        LiveVacationMode.getInstance().post(oldVacation)
        instrumentation.waitForIdleSync()
    }

    @Test fun populatedChartsKeepCountsColorsAndSearchActions() {
        fixture().use { scenario ->
            scenario.onActivity { activity ->
                assertEquals("232", activity.findViewById<TextView>(R.id.breakdownBucket0Count).text.toString())
                val swatch = activity.findViewById<View>(R.id.stageSwatch0)
                assertEquals(LearningColors.accent(swatch.context, ActiveTheme.getShallowStageBucketColors5()[0]),
                    (swatch.background as ColorDrawable).color)
                activity.findViewById<View>(R.id.breakdownBucket2View).performClick()
                assertTrue(search!!.srsStages.contains("master"))
                val level = activity.findViewById<LevelProgressView>(R.id.levelProgressView)
                assertEquals(5, level.childCount) // heading + three subject rows + legend
                val row = level.getChildAt(1) as LevelProgressRowView
                assertEquals("21 of 40 passed", row.findViewById<TextView>(R.id.levelProgressCount).text.toString())
                row.performClick()
                assertEquals(8, search!!.minLevel)
                assertEquals(8, search!!.maxLevel)
                assertEquals(1, search!!.itemTypes.size)
            }
            capture(scenario, "dashboard-populated", R.id.modernDashboard)
            capture(scenario, "dashboard-forecast", R.id.timeLineBarChart)
            capture(scenario, "dashboard-levels", R.id.levelProgressView)
        }
    }

    @Test fun hiddenChartsRemoveHeadingsAndSpacingAndCanReturn() {
        fixture().use { scenario ->
            prefs.edit().putBoolean("show_timeline", false).putBoolean("show_srs_breakdown", false).commit()
            LiveTimeLine.getInstance().ping()
            LiveSrsBreakDown.getInstance().ping()
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                for (id in listOf(R.id.timeLineBarChart, R.id.srsBreakDownView)) {
                    val chart = activity.findViewById<View>(id)
                    assertEquals(View.GONE, chart.visibility)
                    val panel = chart.parent as View
                    panel.measure(View.MeasureSpec.makeMeasureSpec(900, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
                    assertEquals(0, panel.measuredHeight)
                }
            }
            prefs.edit().putBoolean("show_timeline", true).putBoolean("show_srs_breakdown", true).commit()
            LiveTimeLine.getInstance().ping()
            LiveSrsBreakDown.getInstance().ping()
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity -> assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.timeLineBarChart).visibility) }
        }
    }

    @Test fun largeTextAndDarkChartsRemainReadable() {
        fixture(dark = true, fontScale = 2f).use { scenario ->
            capture(scenario, "dashboard-large-dark-forecast", R.id.timeLineBarChart)
            capture(scenario, "dashboard-large-dark-levels", R.id.levelProgressView)
            scenario.onActivity { activity ->
                val chart = activity.findViewById<TimeLineBarChart>(R.id.timeLineBarChart)
                assertTrue(chart.height > 0)
                val row = activity.findViewById<LevelProgressView>(R.id.levelProgressView).getChildAt(1)
                val label = row.findViewById<TextView>(R.id.label)
                assertTrue(label.layout.getEllipsisCount(0) == 0)
            }
        }
    }

    @Test fun classicKeepsOriginalChartLayouts() {
        ActivityScenario.launch(AboutActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertFalse(DashboardStyle.isModern(activity))
                assertNull(SrsBreakDownView(activity).findViewById<View>(R.id.stageSwatch0))
                assertNull(LevelProgressRowView(activity).findViewById<View>(R.id.levelProgressCount))
            }
        }
    }

    private fun fixture(dark: Boolean = false, fontScale: Float = 1f): ActivityScenario<AboutActivity> {
        val scenario = ActivityScenario.launch(AboutActivity::class.java)
        scenario.onActivity { activity ->
            val config = Configuration(activity.resources.configuration).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                    if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
                this.fontScale = fontScale
            }
            val context: Context = ContextThemeWrapper(activity.createConfigurationContext(config), R.style.SmokyDashboardTheme)
            // The legacy custom attributes still exist alongside the scoped Material ones.
            context.theme.applyStyle(ActiveTheme.getCurrentTheme().styleId, false)
            activity.setContentView(LayoutInflater.from(activity).cloneInContext(context).inflate(R.layout.activity_main, null))
            for (id in listOf(R.id.classicDashboardHeader, R.id.keyboardHelpView, R.id.firstTimeSetupView,
                R.id.joyoProgressView, R.id.jlptProgressView, R.id.post60ProgressView, R.id.recentUnlocksView,
                R.id.criticalConditionView, R.id.burnedItemsView)) activity.findViewById<View>(id).visibility = View.GONE
            DashboardStyle.apply(activity)
            val overview = activity.findViewById<ComposeView>(R.id.modernDashboard)
            overview.visibility = View.VISIBLE
            overview.setContent {
                DashboardTheme(false, dark) {
                    DashboardOverview(DashboardState(ready = true, level = 8, daysAtLevel = 4.5,
                        reviews = 42, lessons = 12), true, {}, {}, {}, {})
                }
            }
            actment = Proxy.newProxyInstance(Actment::class.java.classLoader, arrayOf(Actment::class.java)) { _, method, args ->
                when(method.name) {
                    "getLifecycle" -> activity.lifecycle
                    "goToSearchResult" -> { search = Converters.getObjectMapper().readValue(args!![1] as String, AdvancedSearchParameters::class.java); null }
                    else -> null
                }
            } as Actment
            activity.findViewById<TimeLineBarChart>(R.id.timeLineBarChart).setLifecycleOwner(activity)
            activity.findViewById<SrsBreakDownView>(R.id.srsBreakDownView).setLifecycleOwner(actment)
            activity.findViewById<LevelProgressView>(R.id.levelProgressView).setLifecycleOwner(actment)
        }
        val timeline = TimeLine(48)
        for (hour in 0 until 48) repeat((hour * 7 % 23) + 2) { index ->
            timeline.timeLine[hour].add(Subject(SubjectEntity().apply { type = SubjectType.values()[index % 3] }))
        }
        val progress = LevelProgress(8)
        progress.entries.removeAll { it.level != 8 }
        progress.entries.forEach { intArrayOf(21, 4, 0, 3, 0, 2, 0, 1, 5, 4).copyInto(it.buckets) }
        val system = SrsSystem(9000, "Offline fixture", 0, 1, 5, 9)
        for (i in 0L..9L) system.addStage(i, i * 3600)
        system.finish()
        val srs = SrsBreakDown()
        listOf(1L, 5L, 7L, 8L, 9L).zip(listOf(232, 104, 81, 62, 16)).forEach { (stage, count) -> srs.addCount(system.getStage(stage), count) }
        LiveTimeLine.getInstance().post(timeline)
        LiveLevelProgress.getInstance().post(progress)
        LiveSrsBreakDown.getInstance().post(srs)
        instrumentation.waitForIdleSync()
        return scenario
    }

    private fun capture(scenario: ActivityScenario<AboutActivity>, name: String, id: Int) {
        instrumentation.uiAutomation.waitForIdle(500, 5000)
        scenario.onActivity { activity ->
            val view = activity.findViewById<View>(id)
            val scroll = activity.findViewById<NestedScrollView>(R.id.mainScrollView)
            val target = if (id == R.id.timeLineBarChart) view.parent as View else view
            scroll.scrollTo(0, target.top)

        }
        instrumentation.waitForIdleSync()
        instrumentation.uiAutomation.waitForIdle(500, 5000)
        if (id == R.id.levelProgressView) {
            scenario.onActivity { activity ->
                val view = activity.findViewById<LevelProgressView>(id)
                val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                view.draw(android.graphics.Canvas(bitmap))
                File(instrumentation.targetContext.filesDir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                bitmap.recycle()
                for (i in 1 until view.childCount - 1) assertTrue(view.getChildAt(i).bottom <= view.height)
            }
            return
        }
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        File(instrumentation.targetContext.filesDir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
