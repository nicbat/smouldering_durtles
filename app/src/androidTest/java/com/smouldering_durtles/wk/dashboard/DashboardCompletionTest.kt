package com.smouldering_durtles.wk.dashboard

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.material.color.MaterialColors
import com.google.android.material.shape.MaterialShapeDrawable
import com.smouldering_durtles.wk.ui.LearningColors
import com.smouldering_durtles.wk.Actment
import com.smouldering_durtles.wk.R
import com.smouldering_durtles.wk.activities.AboutActivity
import com.smouldering_durtles.wk.db.model.Subject
import com.smouldering_durtles.wk.db.model.SubjectEntity
import com.smouldering_durtles.wk.enums.ActiveTheme
import com.smouldering_durtles.wk.enums.SubjectType
import com.smouldering_durtles.wk.livedata.LiveFirstTimeSetup
import com.smouldering_durtles.wk.livedata.LiveRecentUnlocks
import com.smouldering_durtles.wk.views.LiveSubjectTableView
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File
import java.lang.reflect.Proxy

@RunWith(AndroidJUnit4::class)
class DashboardCompletionTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val prefs = PreferenceManager.getDefaultSharedPreferences(instrumentation.targetContext)
    private val keys = listOf("first_time_setup", "show_recent_unlocks")
    private lateinit var oldPrefs: Map<String, Any?>
    private val oldSetup = LiveFirstTimeSetup.getInstance().get()
    private val oldRecent = LiveRecentUnlocks.getInstance().get()
    private lateinit var actment: Actment
    private val subject = Subject(SubjectEntity().apply {
        id = 90081; type = SubjectType.WANIKANI_KANJI; characters = "明"; slug = "明"; level = 8
        meanings = "[{\"meaning\":\"Bright\",\"primary\":true,\"accepted_answer\":true}]"
        unlockedAt = 1700000000000L
    })
    @Before fun setup() {
        oldPrefs = keys.associateWith { prefs.all[it] }
        prefs.edit().putInt("first_time_setup", 1).putBoolean("show_recent_unlocks", true).commit()
        LiveFirstTimeSetup.getInstance().post(1)
    }
    @After fun restore() {
        val edit = prefs.edit()
        oldPrefs.forEach { (key, value) -> when(value) {
            null -> edit.remove(key)
            is Boolean -> edit.putBoolean(key, value)
            is Int -> edit.putInt(key, value)
        } }
        edit.commit()
        LiveFirstTimeSetup.getInstance().post(oldSetup)
        LiveRecentUnlocks.getInstance().post(oldRecent)
        instrumentation.waitForIdleSync()
    }

    @Test fun recentSubjectsRetainColorsContextIdsAndNavigation() {
        fixture().use { scenario ->
            var clicked = 0L
            scenario.onActivity { activity ->
                actment = Proxy.newProxyInstance(Actment::class.java.classLoader, arrayOf(Actment::class.java)) { _, method, args ->
                    when (method.name) {
                        "getLifecycle" -> activity.lifecycle
                        "goToSubjectInfo" -> { clicked = args!![0] as Long; assertArrayEquals(longArrayOf(subject.id), args[1] as LongArray); null }
                        else -> null
                    }
                } as Actment
                activity.findViewById<LiveSubjectTableView>(R.id.recentUnlocksView).setLifecycleOwner(actment)
            }
            LiveRecentUnlocks.getInstance().post(listOf(subject))
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                val table = activity.findViewById<LiveSubjectTableView>(R.id.recentUnlocksView)
                assertEquals(View.VISIBLE, table.visibility)
                assertEquals(2, table.childCount)
                val row = table.getChildAt(1)
                val label = row.findViewById<TextView>(R.id.rowText)
                val detail = row.findViewById<TextView>(R.id.rowExtra)
                assertEquals(LearningColors.fill(label.context, subject.backgroundColor), (label.background as MaterialShapeDrawable).fillColor!!.defaultColor)
                assertEquals(MaterialColors.getColor(detail, com.google.android.material.R.attr.colorOnSurfaceVariant), detail.currentTextColor)
                row.performClick()
                assertEquals(subject.id, clicked)
                capture(activity, table, "dashboard-modern-subjects")
            }
            LiveRecentUnlocks.getInstance().post(emptyList())
            instrumentation.waitForIdleSync()
            scenario.onActivity { assertEquals(View.GONE, it.findViewById<View>(R.id.recentUnlocksView).visibility) }
        }
    }

    @Test fun errorPanelsAndStatisticsFitDarkLargeText() {
        fixture(true, 2f).use { scenario -> scenario.onActivity { activity ->
            val error = activity.findViewById<LinearLayout>(R.id.apiKeyRejectedView)
            assertEquals(View.GONE, error.visibility) // appearance must not invent an error
            error.visibility = View.VISIBLE
            assertEquals(LinearLayout.VERTICAL, error.orientation)
            val expected = MaterialColors.getColor(error, com.google.android.material.R.attr.colorOnErrorContainer)
            assertEquals(expected, (error.getChildAt(0) as TextView).currentTextColor)
            var retries = 0
            activity.findViewById<View>(R.id.retryApiErrorButton2).setOnClickListener { retries++ }
            activity.findViewById<View>(R.id.retryApiErrorButton2).performClick()
            assertEquals(1, retries)
            capture(activity, error, "dashboard-modern-error-dark-large")
            val table = activity.findViewById<android.widget.TableLayout>(R.id.jlptProgressView)
            table.findViewById<TextView>(R.id.jlptLevel5PrePassed).text = "123"
            capture(activity, table, "dashboard-modern-jlpt-dark-large")
            val value = table.findViewById<TextView>(R.id.jlptLevel5PrePassed)
            assertTrue(value.width > 0)
            assertEquals(0, value.layout.getEllipsisCount(0))
        } }
    }

    private fun fixture(dark: Boolean = false, scale: Float = 1f): ActivityScenario<AboutActivity> {
        return ActivityScenario.launch(AboutActivity::class.java).also { scenario -> scenario.onActivity { activity ->
            val config = Configuration(activity.resources.configuration).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
                fontScale = scale
            }
            val context = ContextThemeWrapper(activity.createConfigurationContext(config), R.style.SmokyDashboardTheme)
            context.theme.applyStyle(ActiveTheme.getCurrentTheme().styleId, false)
            activity.setContentView(LayoutInflater.from(activity).cloneInContext(context).inflate(R.layout.activity_main, null))
            DashboardStyle.apply(activity)
        } }
    }
    private fun capture(activity: AboutActivity, view: View, name: String) {
        val width = (328 * activity.resources.displayMetrics.density).toInt()
        view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        view.layout(0, 0, width, view.measuredHeight)
        val bitmap = Bitmap.createBitmap(width, view.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        File(activity.filesDir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
