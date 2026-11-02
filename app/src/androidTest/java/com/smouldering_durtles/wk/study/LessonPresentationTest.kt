package com.smouldering_durtles.wk.study

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
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
import com.smouldering_durtles.wk.enums.QuestionType
import com.smouldering_durtles.wk.enums.SessionState
import com.smouldering_durtles.wk.enums.SessionType
import com.smouldering_durtles.wk.enums.SubjectInfoDump
import com.smouldering_durtles.wk.enums.SubjectType
import com.smouldering_durtles.wk.fragments.LessonSessionFragment
import com.smouldering_durtles.wk.model.FloatingUiState
import com.smouldering_durtles.wk.model.Question
import com.smouldering_durtles.wk.model.Session
import com.smouldering_durtles.wk.views.PitchInfoView
import com.smouldering_durtles.wk.views.SubjectInfoButtonView
import com.smouldering_durtles.wk.views.SubjectInfoView
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Real lesson bindings, with session objects confined to memory and no lesson/answer submissions. */
@RunWith(AndroidJUnit4::class)
class LessonPresentationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val prefs = PreferenceManager.getDefaultSharedPreferences(instrumentation.targetContext)
    private val session = Session.getInstance()
    private val fields = listOf("loaded", "type", "state", "items", "questions", "currentItem", "currentQuestion", "answered", "correct", "forceNewFragment")
    private val keys = listOf("modern_settings", "settings_dynamic_color", "notification_permission_asked",
        "hide_lesson_readings", "autoplay_lesson_presentation", "show_pitch_info", "max_font_size_lesson", "show_onyomi_in_katakana")
    private lateinit var oldFields: Map<String, Any?>
    private lateinit var oldPrefs: Map<String, Any?>
    private var oldDump: SubjectInfoDump? = null
    private var oldAnswer = ""
    private var oldAudioPlayed = false
    private lateinit var items: ArrayList<SessionItem>
    private lateinit var questions: ArrayList<Question>

    @Before fun prepare() {
        oldPrefs = keys.associateWith { prefs.all[it] }
        prefs.edit().putBoolean("modern_settings", true).putBoolean("settings_dynamic_color", false)
            .putBoolean("notification_permission_asked", true).putBoolean("hide_lesson_readings", false)
            .putBoolean("autoplay_lesson_presentation", false).putBoolean("show_pitch_info", false)
            .putBoolean("show_onyomi_in_katakana", false)
            .putInt("max_font_size_lesson", 128).commit()
        instrumentation.runOnMainSync {
            oldFields = fields.associateWith { read(it) }
            oldDump = FloatingUiState.showDumpStage
            oldAnswer = FloatingUiState.getCurrentAnswer()
            oldAudioPlayed = FloatingUiState.audioPlayed
            val subjects = listOf(
                subject(0x80000038L, SubjectType.WANIKANI_RADICAL, "", "Image radical", null),
                subject(90102, SubjectType.WANIKANI_KANJI, "一", "One", "いち"),
                subject(90103, SubjectType.WANIKANI_VOCAB, "一生懸命", "With all one's might; as hard as one can", "いっしょうけんめい")
            )
            items = ArrayList(subjects.map { subject -> SessionItem().apply {
                id = subject.id; this.subject = subject; srsStageId = 1
            } })
            questions = ArrayList(items.mapIndexed { index, item ->
                Question(item, listOf(QuestionType.WANIKANI_RADICAL_NAME, QuestionType.WANIKANI_KANJI_MEANING,
                    QuestionType.WANIKANI_VOCAB_MEANING)[index]).also { item.addQuestion(it) }
            })
            write("loaded", true); write("type", SessionType.LESSON); write("state", SessionState.IN_LESSON_PRESENTATION)
            write("items", items); write("questions", questions); write("answered", false); write("correct", false)
            select(0)
        }
    }

    @After fun restore() {
        instrumentation.runOnMainSync {
            oldFields.forEach { (name, value) -> write(name, value) }
            FloatingUiState.showDumpStage = oldDump
            FloatingUiState.setCurrentAnswer(oldAnswer)
            FloatingUiState.audioPlayed = oldAudioPlayed
        }
        val edit = prefs.edit()
        oldPrefs.forEach { (key, value) -> when (value) {
            null -> edit.remove(key)
            is Boolean -> edit.putBoolean(key, value)
            is Int -> edit.putInt(key, value)
            is String -> edit.putString(key, value)
        } }
        edit.commit()
    }

    @Test fun firstMiddleAndLastLessonsKeepBindingsAndDisabledNavigationGuards() {
        fixture().use { scenario -> scenario.onActivity { activity ->
            for (index in items.indices) {
                select(index)
                val fragment = mount(activity)
                val root = fragment.requireView() as LinearLayout
                fragment.enableInteraction()
                measure(root, 360, 640)
                val previous = root.findViewById<Button>(R.id.previousButton)
                val next = root.findViewById<Button>(R.id.nextButton)
                val progress = root.findViewById<TextView>(R.id.progress)
                val scroll = root.findViewById<View>(R.id.scrollView)
                val navigation = root.findViewById<View>(R.id.lessonNavigation)
                assertSame(items[index], fragment.item)
                assertSame(items[index].subject, fragment.subject)
                if (index == 0) {
                    assertTrue(fragment.subject!!.needsTitleImage())
                    val glyph = root.findViewById<SubjectInfoButtonView>(R.id.button)
                    assertNotNull(SubjectInfoButtonView::class.java.getDeclaredField("image").apply { isAccessible = true }.get(glyph))
                    assertTrue(glyph.width > 0 && glyph.height > 0)
                }
                assertEquals(if (index == 0) View.GONE else View.VISIBLE, previous.visibility)
                assertEquals(if (index == items.lastIndex) "Start quiz" else "Next", next.text.toString())
                assertEquals("Lesson ${index + 1} of ${items.size}", progress.text.toString())
                assertSame(root.getChildAt(root.childCount - 1), navigation)
                assertTrue(progress.bottom <= scroll.top)
                assertTrue(scroll.bottom <= navigation.top)
                assertTrue(next.hasOnClickListeners())
                assertTrue(previous.hasOnClickListeners())
                fragment.disableInteraction()
                next.performClick(); previous.performClick()
                assertSame(items[index], session.currentItem)
                assertEquals(SessionState.IN_LESSON_PRESENTATION, read("state"))
                assertTrue(items.all { it.numAnswers == 0 })
                fragment.enableInteraction()
                assertTrue(next.isEnabled)
                save(root, "lesson-first-middle-last-$index")
            }
        } }
    }

    @Test fun acceptedPrimaryKanjiReadingsRemainDistinctFromAllReadingsAfterReveal() {
        prefs.edit().putBoolean("hide_lesson_readings", true).commit()
        fixture().use { scenario -> scenario.onActivity { activity ->
            select(1)
            FloatingUiState.showDumpStage = null
            val root = mount(activity).requireView()
            assertEquals(View.GONE, root.findViewById<View>(R.id.reading).visibility)
            assertEquals(View.GONE, root.findViewById<View>(R.id.onYomi).visibility)
            assertEquals(View.GONE, root.findViewById<View>(R.id.kunYomi).visibility)
            root.findViewById<View>(R.id.revealButton).performClick()
            val header = root.findViewById<TextView>(R.id.reading)
            assertEquals(View.VISIBLE, header.visibility)
            assertTrue(header.text.contains("いち"))
            assertTrue(header.text.contains("いつ"))
            assertFalse(header.text.contains("ひとつ"))
            val rich = header.text as android.text.Spanned
            assertTrue(rich.getSpans(0, rich.length, android.text.style.StyleSpan::class.java).any {
                it.style == android.graphics.Typeface.BOLD && rich.subSequence(rich.getSpanStart(it), rich.getSpanEnd(it)).contains("いち")
            })
            val on = root.findViewById<TextView>(R.id.onYomi)
            val kun = root.findViewById<TextView>(R.id.kunYomi)
            assertEquals(View.VISIBLE, on.visibility)
            assertEquals(View.VISIBLE, kun.visibility)
            assertTrue(on.text.contains("いち") && on.text.contains("いつ"))
            assertTrue(kun.text.contains("ひとつ"))
            assertEquals(0, items[1].numAnswers)
        } }
    }

    @Test fun presentationReusesNavigationObjectsAndListeners() {
        fixture().use { scenario -> scenario.onActivity { activity ->
            val root = LayoutInflater.from(activity).inflate(R.layout.fragment_lesson, null)
            val previous = root.findViewById<Button>(R.id.previousButton)
            val next = root.findViewById<Button>(R.id.nextButton)
            var back = 0
            var forward = 0
            previous.setOnClickListener { back++ }
            next.setOnClickListener { forward++ }
            StudyStyle.page(root)
            LessonStyle.navigation(root, 2, 3)
            LessonStyle.navigation(root, 2, 3)
            assertSame(previous, root.findViewById<Button>(R.id.previousButton))
            assertSame(next, root.findViewById<Button>(R.id.nextButton))
            previous.performClick(); next.performClick()
            assertEquals(1, back)
            assertEquals(1, forward)
            assertEquals(1, descendants(root).count { it.id == R.id.lessonNavigation })
        } }
    }

    @Test fun hiddenReadingsAndExpandedDetailsSurviveRevealAndFragmentRestoration() {
        prefs.edit().putBoolean("hide_lesson_readings", true).commit()
        fixture().use { scenario -> scenario.onActivity { activity ->
            select(2)
            FloatingUiState.showDumpStage = null
            val fragment = mount(activity)
            var root = fragment.requireView()
            val info = root.findViewById<SubjectInfoView>(R.id.subjectInfo)
            assertEquals(View.GONE, info.findViewById<View>(R.id.reading).visibility)
            assertEquals(View.GONE, info.findViewById<View>(R.id.readingMnemonic).visibility)
            assertEquals(View.GONE, info.findViewById<View>(R.id.detailsTable).visibility)
            val details = info.findViewById<Button>(R.id.lessonDetailsToggle)
            details.performClick()
            assertEquals(View.VISIBLE, info.findViewById<View>(R.id.detailsTable).visibility)
            assertTrue(LessonStyle.isDetailsExpanded(root))
            val reveal = info.findViewById<View>(R.id.revealButton)
            assertEquals(View.VISIBLE, reveal.visibility)
            reveal.performClick()
            assertEquals(View.VISIBLE, info.findViewById<View>(R.id.readingMnemonic).visibility)
            assertEquals(View.VISIBLE, info.findViewById<View>(R.id.reading).visibility)
            assertTrue(LessonStyle.isDetailsExpanded(root))
            assertTrue(info.findViewById<TextView>(R.id.meaningMnemonic).text.contains("Remember this meaning"))
            assertTrue(info.findViewById<View>(R.id.addSynonymButton).hasOnClickListeners())
            val saved = activity.supportFragmentManager.saveFragmentInstanceState(fragment)
            val restored = LessonSessionFragment.newInstance(items[2].id).apply { setInitialSavedState(saved) }
            activity.supportFragmentManager.beginTransaction().replace(R.id.fragment, restored).commitNow()
            root = restored.requireView()
            assertTrue(LessonStyle.isDetailsExpanded(root))
            assertEquals(View.VISIBLE, root.findViewById<View>(R.id.detailsTable).visibility)
            assertEquals(SessionState.IN_LESSON_PRESENTATION, read("state"))
            assertTrue(items.all { it.numAnswers == 0 })
        } }
    }

    @Test fun longVocabularyPitchAudioAndNavigationFitNarrowLargeTextAndLandscape() {
        fixture().use { scenario -> scenario.onActivity { activity ->
            select(2)
            val fragment = mount(activity)
            for ((name, dark, scale, width, height, pitch) in listOf(
                Size("light", false, 1f, 360, 640, false),
                Size("large-dark", true, 2f, 320, 640, false),
                Size("landscape", false, 1f, 640, 300, true))) {
                prefs.edit().putBoolean("show_pitch_info", pitch).commit()
                val context = themed(activity, dark, scale, width, height)
                val root = fragment.onCreateView(LayoutInflater.from(context), FrameLayout(context), null)!!
                fragment.onViewCreatedLocal(root, null)
                fragment.enableInteraction()
                activity.setContentView(root)
                measure(root, width, height)
                val headline = root.findViewById<ViewGroup>(R.id.headline)
                val glyph = headline.findViewById<SubjectInfoButtonView>(R.id.button)
                assertTrue(glyph.width > 0 && glyph.height > 0)
                assertTrue(glyph.width <= headline.width)
                val audio = descendants(headline).filterIsInstance<Button>().filter { it.id != R.id.revealButton }
                assertTrue("Vocabulary audio remains available", audio.isNotEmpty())
                assertTrue(audio.all { it.hasOnClickListeners() })
                if (pitch) assertTrue(descendants(headline).any { it is PitchInfoView })
                save(root, "lesson-refined-$name")
                assertVisibleTextFits(root)
                val footer = root.findViewById<View>(R.id.lessonNavigation)
                assertTrue(footer.bottom <= root.height)
                assertTrue(root.findViewById<View>(R.id.nextButton).height >= dp(context, 48))
                val scroll = root.findViewById<android.widget.ScrollView>(R.id.scrollView)
                scroll.scrollTo(0, scroll.getChildAt(0).height)
                save(root, "lesson-refined-$name-bottom")
                assertEquals(SessionState.IN_LESSON_PRESENTATION, read("state"))
            }
        } }
    }

    @Test fun smallerGlyphPreferenceAndClassicPresentationRemainAvailable() {
        prefs.edit().putInt("max_font_size_lesson", 32).commit()
        fixture().use { scenario -> scenario.onActivity { activity ->
            select(1)
            val fragment = mount(activity)
            val root = fragment.requireView()
            measure(root, 360, 640)
            val glyph = root.findViewById<SubjectInfoButtonView>(R.id.button)
            val textHeight = SubjectInfoButtonView::class.java.getDeclaredField("textHeight").apply { isAccessible = true }.getInt(glyph)
            assertTrue(textHeight <= 32 * glyph.resources.displayMetrics.scaledDensity + 1)
            prefs.edit().putInt("max_font_size_lesson", 0).commit()
            val hidden = mount(activity).requireView()
            measure(hidden, 360, 640)
            assertEquals(View.GONE, hidden.findViewById<View>(R.id.button).visibility)
            assertTrue(hidden.findViewById<TextView>(R.id.meaning).text.contains("One"))
        } }
        prefs.edit().putBoolean("modern_settings", false).commit()
        fixture().use { scenario -> scenario.onActivity { activity ->
            select(1)
            val fragment = mount(activity)
            val root = fragment.requireView() as LinearLayout
            measure(root, 360, 640)
            assertSame(root.getChildAt(0), root.findViewById<View>(R.id.nextButton).parent)
            assertEquals("2/3", root.findViewById<TextView>(R.id.progress).text.toString())
            assertNull(root.findViewById<View>(R.id.lessonDetailsToggle))
            assertEquals(View.VISIBLE, root.findViewById<View>(R.id.detailsTable).visibility)
            save(root, "lesson-classic")
        } }
    }

    private fun select(index: Int) {
        write("currentItem", items[index]); write("currentQuestion", questions[index])
        FloatingUiState.showDumpStage = SubjectInfoDump.ALL
        FloatingUiState.setCurrentAnswer("")
        FloatingUiState.audioPlayed = false
    }
    private fun subject(id: Long, type: SubjectType, characters: String, meaning: String, reading: String?) = Subject(SubjectEntity().apply {
        this.id = id; this.type = type; this.characters = characters; slug = characters; level = 1; srsStageId = 1
        meanings = "[{\"meaning\":\"$meaning\",\"primary\":true,\"accepted_answer\":true}]"
        if (reading != null) {
            readings = "[{\"reading\":\"$reading\",\"primary\":true,\"accepted_answer\":true}]"
            readingMnemonic = "Remember this reading, then say it aloud before moving on."
        }
        if (type == SubjectType.WANIKANI_KANJI) readings = """[{"reading":"いち","type":"onyomi","primary":true,"accepted_answer":true},{"reading":"いつ","type":"onyomi","primary":false,"accepted_answer":true},{"reading":"ひとつ","type":"kunyomi","primary":false,"accepted_answer":false}]"""
        meaningMnemonic = "Remember this meaning by imagining the character in a familiar scene."
        if (type == SubjectType.WANIKANI_VOCAB) pitchInfo = "[[\"いっしょうけんめい\",null,5]]"
    })
    private fun fixture() = ActivityScenario.launch(AboutActivity::class.java).also { scenario ->
        scenario.onActivity { it.setContentView(FrameLayout(it).apply { id = R.id.fragment }) }
    }
    private fun mount(activity: AboutActivity): LessonSessionFragment {
        val fragment = LessonSessionFragment.newInstance(session.currentItem!!.id)
        activity.supportFragmentManager.beginTransaction().replace(R.id.fragment, fragment).commitNow()
        return fragment
    }
    private data class Size(val name: String, val dark: Boolean, val scale: Float, val width: Int, val height: Int, val pitch: Boolean)
    private fun themed(activity: AboutActivity, dark: Boolean, scale: Float, width: Int, height: Int): Context {
        val config = Configuration(activity.resources.configuration).apply {
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
            fontScale = scale; screenWidthDp = width; screenHeightDp = height
            orientation = if (width > height) Configuration.ORIENTATION_LANDSCAPE else Configuration.ORIENTATION_PORTRAIT
        }
        return ContextThemeWrapper(activity.createConfigurationContext(config), ActiveTheme.getCurrentTheme().styleId).apply {
            theme.applyStyle(R.style.SmokyUiTheme, true)
        }
    }
    private fun dp(context: Context, value: Int) = (value * context.resources.displayMetrics.density).toInt()
    private fun measure(view: View, width: Int, height: Int) {
        descendants(view).forEach { it.forceLayout(); it.refreshDrawableState(); it.jumpDrawablesToCurrentState() }
        view.measure(View.MeasureSpec.makeMeasureSpec(dp(view.context, width), View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(dp(view.context, height), View.MeasureSpec.EXACTLY))
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        view.viewTreeObserver.dispatchOnPreDraw()
    }
    private fun descendants(view: View): List<View> = listOf(view) + if (view is ViewGroup) (0 until view.childCount).flatMap { descendants(view.getChildAt(it)) } else emptyList()
    private fun assertVisibleTextFits(root: View) {
        for (view in descendants(root)) {
            if (view.visibility != View.VISIBLE || !view.isShown || view !is TextView || view.text.isEmpty()) continue
            val layout = view.layout ?: continue
            assertTrue("Text height clipped: ${view.text}", layout.height <= view.height - view.compoundPaddingTop - view.compoundPaddingBottom)
            for (line in 0 until layout.lineCount) {
                assertEquals("Text ellipsized: ${view.text}", 0, layout.getEllipsisCount(line))
                // Layout.getLineWidth includes trailing whitespace; getLineMax measures visible extent.
                val available = view.width - view.compoundPaddingLeft - view.compoundPaddingRight
                assertTrue("Text width clipped: visible=${layout.getLineMax(line)} raw=${layout.getLineWidth(line)} available=$available line=$line text=${view.text.subSequence(layout.getLineStart(line), layout.getLineEnd(line))}",
                    layout.getLineMax(line) <= available + 1)
            }
        }
    }
    private fun save(view: View, name: String) {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        File(instrumentation.targetContext.filesDir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
    private fun read(name: String): Any? = Session::class.java.getDeclaredField(name).apply { isAccessible = true }.get(session)
    private fun write(name: String, value: Any?) { Session::class.java.getDeclaredField(name).apply { isAccessible = true }.set(session, value) }
}
