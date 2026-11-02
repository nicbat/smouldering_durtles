package com.smouldering_durtles.wk.study

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.smouldering_durtles.wk.R
import com.smouldering_durtles.wk.activities.AboutActivity
import com.smouldering_durtles.wk.db.model.SessionItem
import com.smouldering_durtles.wk.db.model.Subject
import com.smouldering_durtles.wk.db.model.SubjectEntity
import com.smouldering_durtles.wk.enums.ActiveTheme
import com.smouldering_durtles.wk.enums.SessionItemState
import com.smouldering_durtles.wk.enums.SessionState
import com.smouldering_durtles.wk.enums.SessionType
import com.smouldering_durtles.wk.enums.SubjectType
import com.smouldering_durtles.wk.fragments.SummarySessionFragment
import com.smouldering_durtles.wk.model.Session
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** In-memory sessions only. No grading, reporting, finishing, or account/database fixture writes. */
@RunWith(AndroidJUnit4::class)
class SummaryPresentationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val prefs = PreferenceManager.getDefaultSharedPreferences(instrumentation.targetContext)
    private val session = Session.getInstance()
    private val fields = listOf("loaded", "type", "state", "items", "questions", "currentItem", "currentQuestion", "answered", "correct", "forceNewFragment")
    private val keys = listOf("modern_settings", "settings_dynamic_color", "notification_permission_asked", "enable_star_ratings")
    private lateinit var oldFields: Map<String, Any?>
    private lateinit var oldPrefs: Map<String, Any?>

    @Before fun prepare() {
        oldPrefs = keys.associateWith { prefs.all[it] }
        prefs.edit().putBoolean("modern_settings", true).putBoolean("settings_dynamic_color", false)
            .putBoolean("notification_permission_asked", true).putBoolean("enable_star_ratings", true).commit()
        instrumentation.runOnMainSync {
            oldFields = fields.associateWith { read(it) }
            write("loaded", true); write("type", SessionType.SELF_STUDY); write("state", SessionState.FINISHING)
            write("items", arrayListOf<SessionItem>())
        }
    }

    @After fun restore() {
        instrumentation.runOnMainSync { oldFields.forEach { (key, value) -> write(key, value) } }
        val edit = prefs.edit()
        oldPrefs.forEach { (key, value) -> if (value == null) edit.remove(key) else edit.putBoolean(key, value as Boolean) }
        edit.commit()
    }

    @Test fun summaryCountsItemsNotWrongAnswerAttemptsAndExcludesAbandoned() {
        fixture().use { scenario -> scenario.onActivity { activity ->
            val correctIds = listOf(R.id.correctRadicals, R.id.correctKanji, R.id.correctVocabulary, R.id.correctKanaVocabulary)
            val errorIds = listOf(R.id.incorrectRadicals, R.id.incorrectKanji, R.id.incorrectVocabulary, R.id.incorrectKanaVocabulary)
            for ((name, errors, expected) in listOf(
                Triple("correct", listOf(0, 0, 0, 0), "100%"),
                Triple("incorrect", listOf(2, 1, 4, 1), "0%"),
                Triple("mixed", listOf(0, 3, 0, 1), "50%"))) {
                val items = items(errors)
                items.add(item(99, SubjectType.WANIKANI_KANJI, 0).apply { state = SessionItemState.ABANDONED })
                write("items", items)
                val fragment = mount(activity)
                val root = fragment.requireView()
                assertEquals(expected, root.findViewById<TextView>(R.id.correctPercentage).text.toString())
                for (i in 0..3) {
                    assertEquals(if (errors[i] == 0) "1" else "0", root.findViewById<TextView>(correctIds[i]).text.toString())
                    assertEquals(if (errors[i] == 0) "0" else "1", root.findViewById<TextView>(errorIds[i]).text.toString())
                }
                assertEquals("${errors.count { it == 0 }} of 4 items · ${errors.count { it > 0 }} with errors",
                    root.findViewById<TextView>(R.id.summaryItemCount).text.toString())
                assertEquals(View.GONE, root.findViewById<View>(R.id.incorrectSummary).visibility)
                assertEquals(0, items[0].numAnswers)
                capture(activity, root, "summary-density-$name", 360, 640)
            }
        } }
    }

    @Test fun emptySummaryDoesNotInventAccuracy() {
        fixture().use { scenario -> scenario.onActivity { activity ->
            val root = mount(activity).requireView()
            assertEquals("—", root.findViewById<TextView>(R.id.correctPercentage).text.toString())
            assertEquals("No items to summarize", root.findViewById<TextView>(R.id.summaryItemCount).text.toString())
            assertEquals(View.GONE, root.findViewById<View>(R.id.summaryAccuracyBar).visibility)
            assertEquals(View.GONE, root.findViewById<View>(R.id.summaryBreakdown).visibility)
        } }
    }

    @Test fun showItemsAndSelectionSurviveSavedFragmentRecreation() {
        fixture().use { scenario -> scenario.onActivity { activity ->
            val items = items(listOf(0, 2, 0, 1))
            write("items", items)
            val fragment = mount(activity)
            val beforeErrors = items.map { it.question1Incorrect }
            assertTrue(fragment.requireView().findViewById<View>(R.id.finishButton).hasOnClickListeners())
            fragment.requireView().findViewById<View>(R.id.showButton).performClick()
            fragment.requireView().findViewById<android.widget.Spinner>(R.id.correctStarSpinner).setSelection(3)
            val savedState = activity.supportFragmentManager.saveFragmentInstanceState(fragment)
            val restored = SummarySessionFragment().apply { setInitialSavedState(savedState) }
            activity.supportFragmentManager.beginTransaction().replace(R.id.fragment, restored).commitNow()
            val root = restored.requireView()
            assertEquals(View.GONE, root.findViewById<View>(R.id.showButton).visibility)
            assertEquals(View.GONE, root.findViewById<View>(R.id.correctSummary).visibility)
            assertEquals(View.VISIBLE, root.findViewById<View>(R.id.correctTable).visibility)
            assertEquals(View.VISIBLE, root.findViewById<View>(R.id.incorrectTable).visibility)
            assertEquals(3, root.findViewById<android.widget.Spinner>(R.id.correctStarSpinner).selectedItemPosition)
            restored.disableInteraction()
            root.findViewById<View>(R.id.finishButton).performClick()
            assertEquals(SessionState.FINISHING, read("state"))
            assertEquals(beforeErrors, items.map { it.question1Incorrect })
        } }
    }

    @Test fun narrowLargeTextFitsAndKeepsActionObjectsCallbacksAndProgress() {
        fixture().use { scenario -> scenario.onActivity { activity ->
            for ((night, suffix) in listOf(Configuration.UI_MODE_NIGHT_NO to "light", Configuration.UI_MODE_NIGHT_YES to "dark")) {
                val config = Configuration(activity.resources.configuration).apply { fontScale = 2f; uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or night }
                val context = ContextThemeWrapper(activity.createConfigurationContext(config), ActiveTheme.getCurrentTheme().styleId)
                context.theme.applyStyle(R.style.SmokyUiTheme, true)
                val root = LayoutInflater.from(context).inflate(R.layout.fragment_summary, null)
                val actions = listOf(R.id.finishButton, R.id.showButton, R.id.specialButton1, R.id.specialButton2, R.id.specialButton3)
                    .map { root.findViewById<TextView>(it) }
                var clicks = 0
                actions.forEach { it.visibility = View.VISIBLE; it.setOnClickListener { clicks++ } }
                actions[2].text = "Ignore typo"; actions[3].text = "Play audio"; actions[4].text = "Undo"
                val progress = root.findViewById<ProgressBar>(R.id.finishProgressBar)
                progress.max = 12; progress.progress = 5; progress.visibility = View.VISIBLE
                StudyStyle.page(root)
                StudyStyle.summary(root)
                SummaryPresentation.bind(root, intArrayOf(1, 8, 2, 1), intArrayOf(1, 10, 3, 1))
                activity.setContentView(root)
                capture(activity, root, "summary-density-large-$suffix", 320, 640)
                actions.forEach {
                    assertSame(it, root.findViewById<View>(it.id))
                    it.performClick()
                    assertTrue("Action fits container", it.left >= 0 && it.right <= (it.parent as View).width)
                }
                assertEquals(5, clicks)
                assertSame(progress, root.findViewById<ProgressBar>(R.id.finishProgressBar))
                assertEquals(12, progress.max); assertEquals(5, progress.progress)
                assertEquals(View.VISIBLE, progress.visibility)
                val eightDp = kotlin.math.round(8 * root.resources.displayMetrics.density).toInt()
                assertEquals(eightDp, progress.height)
                assertTrue("Sync progress needs clearance below actions", progress.top - actions.maxOf { it.bottom } >= eightDp)
                assertTrue("Summary needs clearance below sync progress", root.findViewById<View>(R.id.correctSummary).top - progress.bottom >= eightDp)
                val drawable = progress.progressDrawable as android.graphics.drawable.LayerDrawable
                assertTrue(drawable.findDrawableByLayerId(android.R.id.background) is com.google.android.material.shape.MaterialShapeDrawable)
                assertTrue(drawable.findDrawableByLayerId(android.R.id.progress) is android.graphics.drawable.ClipDrawable)
                assertEquals(com.google.android.material.color.MaterialColors.getColor(progress, androidx.appcompat.R.attr.colorPrimary),
                    progress.progressTintList!!.defaultColor)
                assertEquals(com.google.android.material.color.MaterialColors.getColor(progress, com.google.android.material.R.attr.colorSurfaceContainerHighest),
                    progress.progressBackgroundTintList!!.defaultColor)
                assertTextFits(root.findViewById(R.id.correctSummary))
            }
        } }
    }

    @Test fun classicKeepsBothOriginalPanels() {
        prefs.edit().putBoolean("modern_settings", false).commit()
        fixture().use { scenario -> scenario.onActivity { activity ->
            write("items", items(listOf(0, 1, 0, 1)))
            val root = mount(activity).requireView()
            assertNull(root.findViewById<View>(R.id.summaryAccuracyBar))
            for (id in listOf(R.id.correctSummary, R.id.incorrectSummary)) {
                assertTrue(root.findViewById<View>(id).background is ColorDrawable)
                assertEquals(View.VISIBLE, root.findViewById<View>(id).visibility)
            }
        } }
    }

    private fun items(errors: List<Int>) = arrayListOf<SessionItem>().apply {
        listOf(SubjectType.WANIKANI_RADICAL, SubjectType.WANIKANI_KANJI, SubjectType.WANIKANI_VOCAB, SubjectType.WANIKANI_KANA_VOCAB)
            .forEachIndexed { i, type -> add(item(i, type, errors[i])) }
    }
    private fun item(index: Int, type: SubjectType, errors: Int) = SessionItem().apply {
        id = 91000L + index
        subject = Subject(SubjectEntity().apply {
            id = 91000L + index; this.type = type; characters = "一"; slug = "一"; level = 1; srsStageId = 1
            meanings = "[{\"meaning\":\"One\",\"primary\":true,\"accepted_answer\":true}]"
            readings = "[{\"reading\":\"いち\",\"type\":\"onyomi\",\"primary\":true,\"accepted_answer\":true}]"
        })
        srsStageId = 1; question1Incorrect = errors
    }
    private fun fixture() = ActivityScenario.launch(AboutActivity::class.java).also { scenario ->
        scenario.onActivity { it.setContentView(FrameLayout(it).apply { id = R.id.fragment }) }
    }
    private fun mount(activity: AboutActivity): SummarySessionFragment = SummarySessionFragment().also {
        activity.supportFragmentManager.beginTransaction().replace(R.id.fragment, it).commitNow()
    }
    private fun capture(activity: AboutActivity, root: View, name: String, width: Int, height: Int) {
        val density = root.resources.displayMetrics.density
        forceLayout(root)
        root.measure(View.MeasureSpec.makeMeasureSpec((width * density).toInt(), View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec((height * density).toInt(), View.MeasureSpec.EXACTLY))
        root.layout(0, 0, root.measuredWidth, root.measuredHeight)
        root.viewTreeObserver.dispatchOnPreDraw()
        val image = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        root.draw(Canvas(image))
        File(activity.filesDir, "$name.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        image.recycle()
    }
    private fun forceLayout(view: View) {
        view.forceLayout()
        if (view is ViewGroup) for (i in 0 until view.childCount) forceLayout(view.getChildAt(i))
    }
    private fun assertTextFits(view: View) {
        if (view.visibility != View.VISIBLE) return
        if (view is TextView && view.text.isNotEmpty()) {
            val layout = view.layout
            assertNotNull(layout)
            for (line in 0 until layout.lineCount) assertTrue("Text exceeds width: ${view.text}", layout.getLineWidth(line) <= view.width + 1)
            assertTrue("Text exceeds height: ${view.text}", layout.height <= view.height)
        }
        if (view is ViewGroup) for (i in 0 until view.childCount) assertTextFits(view.getChildAt(i))
    }
    private fun read(name: String): Any? = Session::class.java.getDeclaredField(name).apply { isAccessible = true }.get(session)
    private fun write(name: String, value: Any?) { Session::class.java.getDeclaredField(name).apply { isAccessible = true }.set(session, value) }
}
