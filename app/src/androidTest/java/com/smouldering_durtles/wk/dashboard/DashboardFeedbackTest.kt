package com.smouldering_durtles.wk.dashboard

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.smouldering_durtles.wk.Actment
import com.smouldering_durtles.wk.R
import com.smouldering_durtles.wk.activities.AboutActivity
import com.smouldering_durtles.wk.db.model.Subject
import com.smouldering_durtles.wk.db.model.SubjectEntity
import com.smouldering_durtles.wk.enums.ActiveTheme
import com.smouldering_durtles.wk.enums.SubjectType
import com.smouldering_durtles.wk.livedata.LiveFirstTimeSetup
import com.smouldering_durtles.wk.views.LiveSubjectTableView
import org.junit.*
import org.junit.Assert.*
import java.io.File
import java.lang.reflect.Proxy

class DashboardFeedbackTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val prefs = PreferenceManager.getDefaultSharedPreferences(instrumentation.targetContext)
    private val panels = listOf(R.id.modernDashboard, R.id.timeLineBarChart, R.id.srsBreakDownView,
        R.id.levelProgressView, R.id.post60ProgressView, R.id.joyoProgressView, R.id.jlptProgressView,
        R.id.lessonReviewBreakdownView, R.id.recentUnlocksView, R.id.criticalConditionView,
        R.id.burnedItemsView, R.id.keyboardHelpView, R.id.firstTimeSetupView, R.id.apiKeyRejectedView)
    private val keys = panels.map { "dashboard_collapsed_" + instrumentation.targetContext.resources.getResourceEntryName(it) } +
        listOf("first_time_setup", "show_critical_condition")
    private lateinit var previous: Map<String, Any?>
    private val oldSetup = LiveFirstTimeSetup.getInstance().get()
    @Before fun setup() {
        previous = keys.associateWith { prefs.all[it] }
        val edit = prefs.edit()
        keys.forEach { edit.remove(it) }
        edit.putInt("first_time_setup", 1).putBoolean("show_critical_condition", true).commit()
        LiveFirstTimeSetup.getInstance().post(1)
        instrumentation.waitForIdleSync()
    }
    @After fun restore() {
        val edit = prefs.edit()
        previous.forEach { (key, value) -> when (value) {
            null -> edit.remove(key)
            is Boolean -> edit.putBoolean(key, value)
            is Int -> edit.putInt(key, value)
        } }
        edit.commit()
        LiveFirstTimeSetup.getInstance().post(oldSetup)
        instrumentation.waitForIdleSync()
    }

    @Test fun everyPanelCollapsesPersistsAndSurvivesDataVisibilityChanges() {
        ActivityScenario.launch(AboutActivity::class.java).use { scenario -> scenario.onActivity { activity ->
            fun inflate() {
                val context = ContextThemeWrapper(activity, R.style.SmokyDashboardTheme)
                context.theme.applyStyle(ActiveTheme.getCurrentTheme().styleId, false)
                activity.setContentView(LayoutInflater.from(context).inflate(R.layout.activity_main, null))
                DashboardStyle.apply(activity)
            }
            inflate()
            for (id in panels) {
                val body = activity.findViewById<View>(id)
                body.visibility = View.VISIBLE
                val host = body.parent as View
                val panel = host.parent as CollapsibleDashboardPanel
                measure(panel)
                assertTrue("Heading needs the card gutter", panel.getChildAt(0).left >= 12 * panel.resources.displayMetrics.density)
                (panel.getChildAt(0) as TextView).performClick()
                measure(panel)
                assertEquals(0, host.height)
                assertEquals(View.VISIBLE, body.visibility)
                assertTrue(panel.height >= 48 * panel.resources.displayMetrics.density)
                body.visibility = View.GONE
                measure(panel)
                assertEquals(0, panel.height)
                body.visibility = View.VISIBLE
                measure(panel)
                assertEquals(0, host.height)
            }
            inflate() // a fresh view tree, as after recreation
            for (id in panels) {
                val body = activity.findViewById<View>(id)
                val host = body.parent as View
                assertEquals(0, host.layoutParams.height)
                val panel = host.parent as CollapsibleDashboardPanel
                panel.getChildAt(0).performClick()
                assertEquals(ViewGroup.LayoutParams.WRAP_CONTENT, host.layoutParams.height)
            }
        } }
    }

    @Test fun criticalGridKeepsAccuracyAllTargetsAndSubjectNavigationAtLargeText() {
        ActivityScenario.launch(AboutActivity::class.java).use { scenario -> scenario.onActivity { activity ->
            for (scale in listOf(1f, 2f)) {
                val configuration = Configuration(activity.resources.configuration).apply {
                    fontScale = scale
                    uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                        if (scale == 2f) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
                }
                val context = ContextThemeWrapper(activity.createConfigurationContext(configuration), R.style.SmokyDashboardTheme)
                context.theme.applyStyle(ActiveTheme.getCurrentTheme().styleId, false)
                var clicked = -1L
                val subjects = (1L..10L).map { id -> Subject(SubjectEntity().apply {
                    this.id = id; type = if (id % 2 == 0L) SubjectType.WANIKANI_VOCAB else SubjectType.WANIKANI_KANJI
                    characters = if (id % 2 == 0L) "日本語" else "一"
                    meanings = """[{"meaning":"Example","primary":true,"accepted_answer":true}]"""
                }) }
                val owner = Proxy.newProxyInstance(Actment::class.java.classLoader, arrayOf(Actment::class.java)) { _, method, args ->
                    if (method.name == "goToSubjectInfo") {
                        clicked = args!![0] as Long
                        assertArrayEquals(subjects.map { it.id }.toLongArray(), args[1] as LongArray)
                    }
                    null
                } as Actment
                activity.setContentView(LayoutInflater.from(context).inflate(R.layout.activity_main, null))
                DashboardStyle.apply(activity)
                val table = activity.findViewById<LiveSubjectTableView>(R.id.criticalConditionView)
                LiveSubjectTableView::class.java.getDeclaredMethod("update", Actment::class.java, List::class.java)
                    .apply { isAccessible = true }.invoke(table, owner, subjects)
                val panel = table.parent.parent as View
                measure(panel, if (scale == 1f) 360 else 280)
                val grid = table.findViewById<ViewGroup>(R.id.recentSubjectGrid)
                assertEquals(10, grid.childCount)
                for (i in 0 until grid.childCount) {
                    val cell = grid.getChildAt(i)
                    assertTrue(cell.contentDescription.contains("%"))
                    assertTrue(cell.width >= 48 * cell.resources.displayMetrics.density)
                    assertTrue(cell.height >= 48 * cell.resources.displayMetrics.density)
                }
                grid.getChildAt(9).performClick()
                assertEquals(10L, clicked)
                save(panel, "feedback-critical-$scale")
                (panel as ViewGroup).getChildAt(0).performClick()
                measure(panel, if (scale == 1f) 360 else 280)
                save(panel, "feedback-critical-collapsed-$scale")
                panel.getChildAt(0).performClick()
            }
        } }
    }

    private fun measure(view: View, width: Int = 360) {
        view.measure(View.MeasureSpec.makeMeasureSpec((width * view.resources.displayMetrics.density).toInt(), View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
    }
    private fun save(view: View, name: String) {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        File(instrumentation.targetContext.filesDir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
