package com.smouldering_durtles.wk.views

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import androidx.appcompat.view.ContextThemeWrapper
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.smouldering_durtles.wk.R
import com.smouldering_durtles.wk.activities.AboutActivity
import com.smouldering_durtles.wk.db.model.Subject
import com.smouldering_durtles.wk.db.model.SubjectEntity
import com.smouldering_durtles.wk.enums.SubjectType
import com.smouldering_durtles.wk.livedata.LiveVacationMode
import com.smouldering_durtles.wk.model.TimeLine
import com.smouldering_durtles.wk.Constants.HOUR
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File

/** Synthetic hourly timelines: counts, backlog, large text and accessible horizontal paging. */
@RunWith(AndroidJUnit4::class)
class ForecastAppearanceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val prefs = PreferenceManager.getDefaultSharedPreferences(instrumentation.targetContext)
    private val keys = listOf("first_time_setup", "show_timeline")
    private lateinit var previous: Map<String, Any?>
    private val oldVacation = LiveVacationMode.getInstance().get()

    @Before fun prepare() {
        previous = keys.associateWith { prefs.all[it] }
        prefs.edit().putInt("first_time_setup", 1).putBoolean("show_timeline", true).commit()
        LiveVacationMode.getInstance().post(false)
        instrumentation.waitForIdleSync()
    }
    @After fun restore() {
        val edit = prefs.edit()
        previous.forEach { (key, value) -> when(value) {
            null -> edit.remove(key)
            is Boolean -> edit.putBoolean(key, value)
            is Int -> edit.putInt(key, value)
        } }
        edit.commit()
        LiveVacationMode.getInstance().post(oldVacation)
    }

    @Test fun backlogIsIncludedOnceInCumulativeButDoesNotFlattenFutureBars() {
        val timeline = TimeLine(24)
        repeat(500) { timeline.addReview(subject(timeline.createdAt - 10000), false) }
        repeat(7) { timeline.addReview(subject(timeline.firstSlot + 5 * HOUR), false) }
        fixture(timeline, 360, 1f) { chart ->
            assertEquals(7, field(chart, "maxBarCount"))
            val counts = field(chart, "counts") as IntArray
            val cumulative = field(chart, "cumulative") as IntArray
            assertEquals(0, counts[0])
            assertEquals(7, counts[5])
            assertEquals(500, cumulative[0])
            assertEquals(507, cumulative.last())
            assertTrue(chart.contentDescription.contains("507 waiting including 500 due now"))
            capture(chart, "forecast-backlog")
        }
    }

    @Test fun denseTimelineScrollsToLastHourAndLargeTextFits() {
        val timeline = TimeLine(168)
        for (hour in 0 until 168) repeat(hour % 9) { timeline.timeLine[hour].add(subject(0)) }
        for ((width, scale) in listOf(360 to 1f, 280 to 2f)) fixture(timeline, width, scale) { chart ->
            val before = field(chart, "scrollOffset") as Float
            assertTrue(chart.performAccessibilityAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD, null))
            assertTrue((field(chart, "scrollOffset") as Float) > before)
            val offset = field(chart, "scrollOffset") as Float
            TimeLineBarChart::class.java.getDeclaredMethod("update", TimeLine::class.java).apply { isAccessible = true }.invoke(chart, timeline)
            assertEquals(offset, field(chart, "scrollOffset") as Float, .1f)
            repeat(40) { chart.performAccessibilityAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD, null) }
            chart.draw(Canvas(Bitmap.createBitmap(chart.width, chart.height, Bitmap.Config.ARGB_8888)))
            val max = TimeLineBarChart::class.java.getDeclaredMethod("maxScroll").apply { isAccessible = true }.invoke(chart) as Float
            assertEquals(max, field(chart, "scrollOffset") as Float, .1f)
            assertEquals(24, (field(chart, "counts") as IntArray).size)
            chart.setHorizon(168)
            assertEquals(7, (field(chart, "counts") as IntArray).size)
            (field(chart, "times") as LongArray).forEach { timestamp ->
                val calendar = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
                assertEquals(0, calendar.get(java.util.Calendar.HOUR_OF_DAY))
            }
            chart.setHorizon(0)
            assertEquals((0 until 168).sumOf { it % 9 }, (field(chart, "counts") as IntArray).sum())
            chart.setHorizon(24)
            assertTextFits(chart)
            capture(chart, "forecast-timeline-$width-$scale")
        }
    }

    @Test fun emptyForecastHasZeroCumulativeAndReadableLabels() {
        fixture(TimeLine(24), 280, 2f) { chart ->
            assertTrue((field(chart, "cumulative") as IntArray).all { it == 0 })
            assertTrue(chart.contentDescription.contains("0 waiting including 0 due now"))
            assertTextFits(chart)
            capture(chart, "forecast-empty-280-2")
        }
    }

    private fun subject(at: Long) = Subject(SubjectEntity().apply { type = SubjectType.WANIKANI_KANJI; availableAt = at })
    private fun field(chart: TimeLineBarChart, name: String): Any = TimeLineBarChart::class.java.getDeclaredField(name).apply { isAccessible = true }.get(chart)!!
    private fun fixture(timeline: TimeLine, widthDp: Int, scale: Float, check: (TimeLineBarChart) -> Unit) {
        ActivityScenario.launch(AboutActivity::class.java).use { scenario -> scenario.onActivity { activity ->
            val config = Configuration(activity.resources.configuration).apply { fontScale = scale }
            val context = ContextThemeWrapper(activity.createConfigurationContext(config), R.style.SmokyDashboardTheme)
            val chart = TimeLineBarChart(context)
            TimeLineBarChart::class.java.getDeclaredMethod("update", TimeLine::class.java).apply { isAccessible = true }.invoke(chart, timeline)
            chart.measure(View.MeasureSpec.makeMeasureSpec((widthDp * context.resources.displayMetrics.density).toInt(), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
            chart.layout(0, 0, chart.measuredWidth, chart.measuredHeight)
            Bitmap.createBitmap(chart.width, chart.height, Bitmap.Config.ARGB_8888).also { chart.draw(Canvas(it)); it.recycle() }
            check(chart)
        } }
    }
    @Suppress("UNCHECKED_CAST") private fun assertTextFits(chart: TimeLineBarChart) {
        val bounds = field(chart, "modernTextBounds") as List<RectF>
        assertTrue(bounds.isNotEmpty())
        bounds.forEach { assertTrue(it.left >= 0 && it.right <= chart.width && it.bottom < (field(chart, "plotTop") as Float)) }
    }
    private fun capture(chart: TimeLineBarChart, name: String) {
        Bitmap.createBitmap(chart.width, chart.height, Bitmap.Config.ARGB_8888).also { bitmap ->
            val canvas = Canvas(bitmap)
            chart.draw(canvas)
            val cached = field(chart, "captions")
            val start = System.nanoTime()
            repeat(100) { chart.draw(canvas) }
            val elapsed = (System.nanoTime() - start) / 100.0 / 1_000_000.0
            assertSame("Repeated frames reuse caption layouts", cached, field(chart, "captions"))
            File(instrumentation.targetContext.filesDir, "$name-profile.txt").writeText("Android emulator, software Canvas, 100 warmed draws: $elapsed ms/draw. No before baseline; not a device frame-rate benchmark.\n")
            File(instrumentation.targetContext.filesDir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }
}
