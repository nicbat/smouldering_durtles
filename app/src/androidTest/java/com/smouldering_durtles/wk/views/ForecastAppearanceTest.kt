package com.smouldering_durtles.wk.views

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matchers.anything
import org.hamcrest.Matchers.containsString
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.smouldering_durtles.wk.R
import com.smouldering_durtles.wk.activities.AboutActivity
import com.smouldering_durtles.wk.db.model.Subject
import com.smouldering_durtles.wk.db.model.SubjectEntity
import com.smouldering_durtles.wk.enums.ActiveTheme
import com.smouldering_durtles.wk.enums.SubjectType
import com.smouldering_durtles.wk.livedata.LiveVacationMode
import com.smouldering_durtles.wk.model.TimeLine
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File

/** Offline, synthetic forecast rendering. No account, database, session, or review submissions. */
@RunWith(AndroidJUnit4::class)
class ForecastAppearanceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val prefs = PreferenceManager.getDefaultSharedPreferences(instrumentation.targetContext)
    private val keys = listOf("first_time_setup", "show_timeline", "timeline_chart_style", "timeline_chart_size",
        "timeline_chart_size_shown", "timeline_chart_grid_style", "show_waterfall_line", "learning_colors")
    private lateinit var previous: Map<String, Any?>
    private val oldVacation = LiveVacationMode.getInstance().get()

    @Before fun prepare() {
        previous = keys.associateWith { prefs.all[it] }
        prefs.edit().putInt("first_time_setup", 1).putBoolean("show_timeline", true)
            .putString("timeline_chart_style", "ITEM_TYPE").putString("timeline_chart_size", "48")
            .putString("timeline_chart_size_shown", "24").putString("timeline_chart_grid_style", "FOR_BARS")
            .putBoolean("show_waterfall_line", false).putString("learning_colors", "soft").commit()
        LiveVacationMode.getInstance().post(false)
        instrumentation.waitForIdleSync()
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
        LiveVacationMode.getInstance().post(oldVacation)
        instrumentation.waitForIdleSync()
    }

    @Test fun denseHourlyForecastHasNoCollidingTextAtNormalAndDoubleSize() {
        val timeline = populated()
        for ((width, fontScale) in listOf(360 to 1f, 280 to 2f)) {
            fixture(timeline, width, fontScale) { chart ->
                assertTextFits(chart)
                assertTrue(chart.contentDescription.contains("${timeline.timeLine.sumOf { it.size }} reviews"))
                assertTrue(chart.contentDescription.contains("48-hour forecast"))
                capture(chart, "forecast-dense-${width}-${fontScale}")
            }
        }
    }

    @Test fun populatedWeekFitsWithoutScrolling() {
        fixture(populated(168), 360, 1f) { chart ->
            assertTextFits(chart)
            capture(chart, "forecast-populated-week")
        }
    }

    @Test fun zeroForecastAndCumulativeGridFitWithoutInventingCounts() {
        prefs.edit().putString("timeline_chart_grid_style", "FOR_WATERFALL")
            .putBoolean("show_waterfall_line", true).commit()
        fixture(TimeLine(24), 280, 2f) { chart ->
            assertTextFits(chart)
            assertTrue(chart.contentDescription.contains("0 reviews"))
            assertTrue(chart.contentDescription.contains("No reviews scheduled"))
            assertTrue(details(chart, 0).contains("Cumulative: 0 reviews"))
            capture(chart, "forecast-empty-280-2")
        }
        fixture(populated(), 280, 2f) { chart ->
            assertTextFits(chart)
            capture(chart, "forecast-cumulative-280-2")
        }
    }

    @Test fun hourlyDetailsKeepExactBucketsCumulativeAndLevelUpCounts() {
        val timeline = TimeLine(48)
        listOf(SubjectType.WANIKANI_RADICAL, SubjectType.WANIKANI_KANJI,
            SubjectType.WANIKANI_VOCAB, SubjectType.WANIKANI_KANA_VOCAB).forEach { type ->
            timeline.timeLine[0].add(Subject(SubjectEntity().apply { this.type = type }))
        }
        timeline.addReview(Subject(SubjectEntity().apply {
            type = SubjectType.WANIKANI_KANJI
            availableAt = timeline.firstSlot + 25 * com.smouldering_durtles.wk.Constants.HOUR
        }), true)
        val originalPalette = ActiveTheme.getSubjectTypeBucketColors().clone()
        fixture(timeline, 360, 1f) { chart ->
            val first = details(chart, 0)
            assertTrue(first.contains("4 reviews"))
            assertTrue(first.contains("Radical: 1"))
            assertTrue(first.contains("Kanji: 1"))
            assertTrue(first.contains("Vocabulary: 2"))
            assertTrue(first.contains("includes overdue"))
            val later = details(chart, 25)
            assertTrue(later.contains("Cumulative: 5 reviews"))
            assertTrue(later.contains("On the level-up path: 1"))
            assertFalse(later.contains("includes overdue"))
            val firstSlot = TimeLineBarChart::class.java.getDeclaredField("firstSlot").apply { isAccessible = true }
            assertEquals(timeline.firstSlot, firstSlot.getLong(chart))
            assertArrayEquals(originalPalette, ActiveTheme.getSubjectTypeBucketColors())
        }
    }

    @Test fun neutralDailyStripsNeedNoHorizontalScrollingAndKeepAccessibleDetails() {
        prefs.edit().putString("learning_colors", "neutral").putString("timeline_chart_grid_style", "OFF").commit()
        fixture(populated(), 280, 2f) { chart ->
            val advance = TimeLineBarChart::class.java.getDeclaredField("barAdvance").apply { isAccessible = true }.getFloat(chart)
            TimeLineBarChart::class.java.getDeclaredField("scrollOffset").apply { isAccessible = true }.setFloat(chart, advance * 24)
            render(chart)
            assertEquals(advance * 24, 0f, 0f)
            assertTextFits(chart)
            assertTrue(chart.isClickable)
            assertTrue(chart.isFocusable)
            assertTrue(chart.contentDescription.contains("every hour"))
            capture(chart, "forecast-scrolled-neutral-280-2")
        }
    }

    @Test fun accessibleClickOpensHourlyListThenExactBreakdown() {
        ActivityScenario.launch(AboutActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                // Use the real Activity window for dialogs, with the dashboard's scoped theme.
                val context = ContextThemeWrapper(activity, R.style.SmokyDashboardTheme)
                context.theme.applyStyle(ActiveTheme.getCurrentTheme().styleId, false)
                val chart = TimeLineBarChart(context)
                activity.setContentView(chart)
                TimeLineBarChart::class.java.getDeclaredMethod("update", TimeLine::class.java)
                    .apply { isAccessible = true }.invoke(chart, populated())
            }
            // ActivityScenario's RESUMED state can precede actual window focus on API 36.
            var focused = false
            val deadline = System.currentTimeMillis() + 5000
            while (!focused && System.currentTimeMillis() < deadline) {
                instrumentation.waitForIdleSync()
                scenario.onActivity { focused = it.window.decorView.hasWindowFocus() }
                if (!focused) Thread.sleep(50)
            }
            assertTrue("Fixture window must have focus before opening a dialog", focused)
            scenario.onActivity { activity ->
                val chart = activity.findViewById<android.view.ViewGroup>(android.R.id.content).getChildAt(0) as TimeLineBarChart
                assertTrue(chart.performClick())
            }
            onView(withText(R.string.forecast_daily_title)).inRoot(isDialog()).check(matches(isDisplayed()))
            onData(anything()).inRoot(isDialog()).atPosition(0).perform(click())
            onData(anything()).inRoot(isDialog()).atPosition(0).perform(click())
            onView(withText(containsString("Cumulative: 2 reviews"))).inRoot(isDialog()).check(matches(isDisplayed()))
            onView(withText(containsString("includes overdue"))).inRoot(isDialog()).check(matches(isDisplayed()))
            onView(withText(android.R.string.ok)).inRoot(isDialog()).perform(click())
        }
    }

    @Test fun calendarDaysRespectDstAndSevenDayOverviewKeepsLaterDaysReachable() {
        val previousZone = java.util.TimeZone.getDefault()
        try {
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("America/Los_Angeles"))
            val timeline = TimeLine(336)
            val start = java.util.Calendar.getInstance().apply { clear(); set(2026, 10, 1, 0, 0, 0) }.timeInMillis
            TimeLine::class.java.getDeclaredField("firstSlot").apply { isAccessible = true }.setLong(timeline, start)
            for (hour in 0 until timeline.size) timeline.timeLine[hour].add(Subject(SubjectEntity().apply { type = SubjectType.WANIKANI_KANJI }))
            fixture(timeline, 360, 1f) { chart ->
                @Suppress("UNCHECKED_CAST")
                val days = TimeLineBarChart::class.java.getDeclaredField("days").apply { isAccessible = true }.get(chart) as List<Any>
                fun count(day: Any) = day.javaClass.getDeclaredField("count").apply { isAccessible = true }.getInt(day)
                assertEquals(25, count(days[0])) // fall-back day has 25 hours
                assertEquals(24, count(days[1]))
                assertEquals(336, days.sumOf { count(it) })
                @Suppress("UNCHECKED_CAST")
                val bounds = TimeLineBarChart::class.java.getDeclaredField("dayBounds").apply { isAccessible = true }.get(chart) as List<RectF>
                assertEquals(7, bounds.size)
                assertTrue(bounds.all { it.height() >= 48 * chart.resources.displayMetrics.density - 1 })
                assertTrue(chart.contentDescription.contains("336 reviews"))
                assertTextFits(chart)
                capture(chart, "forecast-calendar-week")
            }
        } finally { java.util.TimeZone.setDefault(previousZone) }
    }

    private fun populated(size: Int = 48): TimeLine = TimeLine(size).also { timeline ->
        for (hour in 0 until size) repeat((hour * 7 % 23) + 2) { index ->
            timeline.timeLine[hour].add(Subject(SubjectEntity().apply { type = SubjectType.values()[index % 3] }))
        }
    }

    private fun fixture(timeline: TimeLine, widthDp: Int, scale: Float, check: (TimeLineBarChart) -> Unit) {
        ActivityScenario.launch(AboutActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val config = Configuration(activity.resources.configuration).apply {
                    fontScale = scale
                    uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or Configuration.UI_MODE_NIGHT_YES
                }
                val context = ContextThemeWrapper(activity.createConfigurationContext(config), R.style.SmokyDashboardTheme)
                context.theme.applyStyle(ActiveTheme.getCurrentTheme().styleId, false)
                val chart = TimeLineBarChart(context)
                TimeLineBarChart::class.java.getDeclaredMethod("update", TimeLine::class.java)
                    .apply { isAccessible = true }.invoke(chart, timeline)
                chart.measure(View.MeasureSpec.makeMeasureSpec((widthDp * context.resources.displayMetrics.density).toInt(), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
                chart.layout(0, 0, chart.measuredWidth, chart.measuredHeight)
                render(chart)
                check(chart)
            }
        }
    }

    private fun details(chart: TimeLineBarChart, slot: Int): String =
        TimeLineBarChart::class.java.getDeclaredMethod("slotDetails", Int::class.javaPrimitiveType)
            .apply { isAccessible = true }.invoke(chart, slot) as String

    private fun render(chart: TimeLineBarChart) {
        Bitmap.createBitmap(chart.width, chart.height, Bitmap.Config.ARGB_8888).also {
            chart.draw(Canvas(it))
            it.recycle()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun assertTextFits(chart: TimeLineBarChart) {
        val bounds = TimeLineBarChart::class.java.getDeclaredField("modernTextBounds")
            .apply { isAccessible = true }.get(chart) as List<RectF>
        assertTrue("Expected rendered labels", bounds.size > 5)
        bounds.forEachIndexed { i, rect ->
            assertTrue("Text outside chart: $rect / ${chart.width}x${chart.height}",
                rect.left >= 0 && rect.top >= 0 && rect.right <= chart.width && rect.bottom <= chart.height)
            for (j in 0 until i) assertFalse("Text collision: $rect / ${bounds[j]}", RectF.intersects(rect, bounds[j]))
        }
    }

    private fun capture(chart: TimeLineBarChart, name: String) {
        Bitmap.createBitmap(chart.width, chart.height, Bitmap.Config.ARGB_8888).also { bitmap ->
            chart.draw(Canvas(bitmap))
            File(instrumentation.targetContext.filesDir, "$name.png").outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            bitmap.recycle()
        }
    }
}
