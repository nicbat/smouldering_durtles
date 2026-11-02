package com.smouldering_durtles.wk.dashboard

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import android.widget.TableLayout
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.smouldering_durtles.wk.R
import com.smouldering_durtles.wk.activities.AboutActivity
import com.smouldering_durtles.wk.enums.ActiveTheme
import com.smouldering_durtles.wk.livedata.LiveFirstTimeSetup
import com.smouldering_durtles.wk.model.JlptProgress
import com.smouldering_durtles.wk.model.JoyoProgress
import com.smouldering_durtles.wk.views.CoverageProgressRowView
import com.smouldering_durtles.wk.views.JlptProgressView
import com.smouldering_durtles.wk.views.JoyoProgressView
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File
import java.text.NumberFormat

/** In-memory aggregate fixtures; no account data or database writes. */
@RunWith(AndroidJUnit4::class)
class CoverageProgressTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val prefs = PreferenceManager.getDefaultSharedPreferences(instrumentation.targetContext)
    private val keys = listOf("first_time_setup", "show_joyo_progress", "show_jlpt_progress", "modern_settings", "notification_permission_asked")
    private lateinit var oldPrefs: Map<String, Any?>
    private val oldSetup = LiveFirstTimeSetup.getInstance().get()

    @Before fun setup() {
        oldPrefs = keys.associateWith { prefs.all[it] }
        prefs.edit().putInt("first_time_setup", 1).putBoolean("show_joyo_progress", true)
            .putBoolean("show_jlpt_progress", true).putBoolean("modern_settings", false)
            .putBoolean("notification_permission_asked", true).commit()
        LiveFirstTimeSetup.getInstance().post(1)
        instrumentation.waitForIdleSync()
    }
    @After fun restore() {
        val edit = prefs.edit()
        oldPrefs.forEach { (key, value) -> when (value) {
            null -> edit.remove(key)
            is Boolean -> edit.putBoolean(key, value)
            is Int -> edit.putInt(key, value)
        } }
        edit.commit()
        LiveFirstTimeSetup.getInstance().post(oldSetup)
        instrumentation.waitForIdleSync()
    }

    @Test fun compactPanelsKeepEveryBucketAndExistingDisplayOrder() {
        ActivityScenario.launch(AboutActivity::class.java).use { scenario -> scenario.onActivity { activity ->
            for (joyo in listOf(true, false)) {
                val context = context(activity)
                val panel = if (joyo) JoyoProgressView(context) else JlptProgressView(context)
                val model = if (joyo) JoyoProgress() else JlptProgress()
                seed(model, intArrayOf(8, 5, 12, 3))
                update(panel, model)
                DashboardStyle.card(panel)
                activity.setContentView(panel)
                measure(panel, 360)
                val rows = rows(panel)
                assertEquals(if (joyo) 7 else 5, rows.size)
                assertEquals(if (joyo) "Grade 1" else "N5", label(rows.first()))
                assertEquals(if (joyo) "Secondary school" else "N1", label(rows.last()))
                for (row in rows) {
                    assertEquals("15 / 28", row.findViewById<TextView>(R.id.coverageSummary).text.toString())
                    assertTrue(row.contentDescription.contains("Locked: 8"))
                    assertTrue(row.contentDescription.contains("In progress: 5"))
                    assertTrue(row.contentDescription.contains("Passed (not burned): 12"))
                    assertTrue(row.contentDescription.contains("Burned: 3"))
                    assertTrue(row.height >= dp(context, 48))
                    assertEquals(dp(context, 6), row.findViewById<View>(R.id.coverageBar).height)
                }
                assertTrue("Normal-size data must fit compact rows", panel.height <= dp(context, (if (joyo) 7 else 5) * 56 + 120))
                assertTextFits(panel)
                capture(panel, if (joyo) "coverage-joyo-light" else "coverage-jlpt-light")
            }
        } }
    }

    @Test fun rowDialogShowsExactBucketsAfterRebinding() {
        ActivityScenario.launch(AboutActivity::class.java).use { scenario ->
            lateinit var row: CoverageProgressRowView
            scenario.onActivity { activity ->
                val panel = JoyoProgressView(ContextThemeWrapper(activity, R.style.SmokyDashboardTheme))
                activity.setContentView(panel)
                row = panel.findViewById(R.id.joyoGrade1Row)
                row.setCounts(8, 5, 12, 3)
                row.performClick()
            }
            onView(withText("Locked: 8\nIn progress: 5\nPassed (not burned): 12\nBurned: 3"))
                .inRoot(isDialog()).check(matches(isDisplayed()))
            onView(withText(android.R.string.ok)).inRoot(isDialog()).perform(click())
            scenario.onActivity { row.setCounts(0, 0, 0, 28); row.performClick() }
            onView(withText("Locked: 0\nIn progress: 0\nPassed (not burned): 0\nBurned: 28"))
                .inRoot(isDialog()).check(matches(isDisplayed()))
            onView(withText(android.R.string.ok)).inRoot(isDialog()).perform(click())
        }
    }

    @Test fun narrowLargeTextStacksAndTotalsDoNotOverflow() {
        ActivityScenario.launch(AboutActivity::class.java).use { scenario -> scenario.onActivity { activity ->
            for (dark in listOf(false, true)) {
                val context = context(activity, 2f, dark)
                val panel = JoyoProgressView(context)
                val data = JoyoProgress()
                seed(data, intArrayOf(2_000_000_000, 2_000_000_000, 2_000_000_000, 2_000_000_000))
                update(panel, data)
                DashboardStyle.card(panel)
                activity.setContentView(panel)
                measure(panel, 280)
                val format = NumberFormat.getIntegerInstance()
                assertEquals("${format.format(4_000_000_000L)} / ${format.format(8_000_000_000L)}",
                    rows(panel)[0].findViewById<TextView>(R.id.coverageSummary).text.toString())
                assertTextFits(panel)
                capture(panel, "coverage-joyo-large-${if (dark) "dark" else "light"}")
            }
        } }
    }

    @Test fun emptyHiddenAndClassicPanelsKeepTheirContracts() {
        ActivityScenario.launch(AboutActivity::class.java).use { scenario -> scenario.onActivity { activity ->
            for (joyo in listOf(true, false)) {
                val panel = if (joyo) JoyoProgressView(context(activity)) else JlptProgressView(context(activity))
                val data = if (joyo) JoyoProgress() else JlptProgress()
                update(panel, data)
                assertEquals(View.VISIBLE, panel.visibility)
                for (row in rows(panel)) assertEquals("No tracked kanji", row.findViewById<TextView>(R.id.coverageSummary).text.toString())
                val key = if (joyo) "show_joyo_progress" else "show_jlpt_progress"
                prefs.edit().putBoolean(key, false).commit()
                update(panel, data)
                assertEquals(View.GONE, panel.visibility)
                prefs.edit().putBoolean(key, true).commit()
                update(panel, null)
                assertEquals(View.GONE, panel.visibility)
                update(panel, data)
                assertEquals(View.VISIBLE, panel.visibility)
                val classic = if (joyo) JoyoProgressView(activity) else JlptProgressView(activity)
                seed(data, intArrayOf(8, 0, 12, 3))
                update(classic, data)
                assertTrue(rows(classic).isEmpty())
                assertEquals("8", classic.findViewById<TextView>(if (joyo) R.id.joyoGrade1Locked else R.id.jlptLevel1Locked).text.toString())
                assertEquals("", classic.findViewById<TextView>(if (joyo) R.id.joyoGrade1PrePassed else R.id.jlptLevel1PrePassed).text.toString())
            }
        } }
    }

    private fun context(activity: AboutActivity, scale: Float = 1f, dark: Boolean = false): Context {
        val config = Configuration(activity.resources.configuration).apply {
            fontScale = scale
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        }
        return ContextThemeWrapper(activity.createConfigurationContext(config), R.style.SmokyDashboardTheme).also {
            it.theme.applyStyle(ActiveTheme.getCurrentTheme().styleId, false)
        }
    }
    private fun seed(model: Any, counts: IntArray) {
        listOf("lockedCount", "prePassedCount", "passedCount", "burnedCount").forEachIndexed { index, name ->
            val field = model.javaClass.getDeclaredField(name).apply { isAccessible = true }
            (field.get(model) as IntArray).fill(counts[index])
        }
    }
    private fun update(view: TableLayout, model: Any?) {
        val modelClass = if (view is JoyoProgressView) JoyoProgress::class.java else JlptProgress::class.java
        view.javaClass.getDeclaredMethod("update", modelClass).apply { isAccessible = true }.invoke(view, model)
    }
    private fun rows(panel: TableLayout) = (0 until panel.childCount).map { panel.getChildAt(it) }.filterIsInstance<CoverageProgressRowView>()
    private fun label(row: CoverageProgressRowView) = row.findViewById<TextView>(R.id.coverageLabel).text.toString()
    private fun dp(context: Context, value: Int) = Math.round(context.resources.displayMetrics.density * value)
    private fun measure(view: View, width: Int) {
        view.measure(View.MeasureSpec.makeMeasureSpec(dp(view.context, width), View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
    }
    private fun assertTextFits(view: View) {
        if (view is TextView) {
            assertTrue(view.layout.height <= view.height - view.compoundPaddingTop - view.compoundPaddingBottom)
            for (line in 0 until view.layout.lineCount) {
                assertEquals(0, view.layout.getEllipsisCount(line))
                assertTrue(view.layout.getLineWidth(line) <= view.width - view.compoundPaddingLeft - view.compoundPaddingRight + 1)
            }
        }
        if (view is ViewGroup) for (i in 0 until view.childCount) assertTextFits(view.getChildAt(i))
    }
    private fun capture(view: View, name: String) {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        File(instrumentation.targetContext.filesDir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
