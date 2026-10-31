package com.smouldering_durtles.wk.study

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.google.android.material.shape.MaterialShapeDrawable
import com.smouldering_durtles.wk.Actment
import com.smouldering_durtles.wk.R
import com.smouldering_durtles.wk.activities.BrowseActivity
import com.smouldering_durtles.wk.activities.AboutActivity
import com.smouldering_durtles.wk.activities.SessionActivity
import com.smouldering_durtles.wk.activities.NoApiKeyHelpActivity
import com.smouldering_durtles.wk.db.model.SessionItem
import com.smouldering_durtles.wk.db.model.Subject
import com.smouldering_durtles.wk.db.model.SubjectEntity
import com.smouldering_durtles.wk.enums.*
import com.smouldering_durtles.wk.fragments.*
import com.smouldering_durtles.wk.model.*
import com.smouldering_durtles.wk.ui.ModernUi
import com.smouldering_durtles.wk.util.ThemeUtil
import com.smouldering_durtles.wk.views.SubjectInfoView
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File
import java.lang.reflect.Proxy

/** Real fragments with in-memory session objects. No answer submissions, account fixtures, or DB writes. */
@RunWith(AndroidJUnit4::class)
class StudyPresentationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val prefs = PreferenceManager.getDefaultSharedPreferences(instrumentation.targetContext)
    private val session = Session.getInstance()
    private val fields = listOf("loaded", "type", "state", "items", "questions", "currentItem", "currentQuestion", "answered", "correct", "forceNewFragment")
    private val keys = listOf("modern_settings", "settings_dynamic_color", "notification_permission_asked", "full_session_log", "enable_advanced")
    private lateinit var oldFields: Map<String, Any?>
    private lateinit var oldPrefs: Map<String, Any?>
    private var oldAnswer = ""
    private var oldDump: SubjectInfoDump? = null
    private lateinit var item: SessionItem
    private lateinit var question: Question
    private lateinit var subject: Subject

    @Before fun prepare() {
        oldPrefs = keys.associateWith { prefs.all[it] }
        prefs.edit().putBoolean("modern_settings", true).putBoolean("settings_dynamic_color", false)
            .putBoolean("notification_permission_asked", true).commit()
        instrumentation.runOnMainSync {
            oldFields = fields.associateWith { read(it) }
            oldAnswer = FloatingUiState.getCurrentAnswer()
            oldDump = FloatingUiState.showDumpStage
            subject = Subject(SubjectEntity().apply {
                id = 90071; type = SubjectType.WANIKANI_KANJI; characters = "一"; slug = "一"; level = 1; srsStageId = 1
                meanings = "[{\"meaning\":\"One\",\"primary\":true,\"accepted_answer\":true}]"
                readings = "[{\"reading\":\"いち\",\"type\":\"onyomi\",\"primary\":true,\"accepted_answer\":true}]"
                meaningMnemonic = "A single horizontal line represents one."
                readingMnemonic = "Remember the reading いち."
            })
            item = SessionItem().apply { id = this@StudyPresentationTest.subject.id; this.subject = this@StudyPresentationTest.subject; srsStageId = 1 }
            question = Question(item, QuestionType.WANIKANI_KANJI_MEANING)
            item.addQuestion(question)
            write("loaded", true); write("type", SessionType.SELF_STUDY); write("state", SessionState.ACTIVE)
            write("items", arrayListOf(item)); write("questions", arrayListOf(question))
            write("currentItem", item); write("currentQuestion", question); write("answered", false); write("correct", false)
            FloatingUiState.showDumpStage = SubjectInfoDump.ALL
            FloatingUiState.setCurrentAnswer("")
        }
    }

    @After fun restore() {
        instrumentation.runOnMainSync {
            oldFields.forEach { (name, value) -> write(name, value) }
            FloatingUiState.setCurrentAnswer(oldAnswer)
            FloatingUiState.showDumpStage = oldDump
        }
        val edit = prefs.edit()
        oldPrefs.forEach { (key, value) -> if (value == null) edit.remove(key) else edit.putBoolean(key, value as Boolean) }
        edit.commit()
    }

    @Test fun populatedStudyModesKeepSemanticColorsAndBindings() {
        fixture().use { scenario ->
            scenario.onActivity { activity ->
                val unanswered = session.getNewFragment(null, false)
                mount(activity, unanswered)
                assertTrue(unanswered is UnansweredSessionFragment)
                assertSame(question, unanswered.question)
                val questionView = unanswered.requireView().findViewById<View>(R.id.questionView)
                assertEquals(subject.backgroundColor, (questionView.background as MaterialShapeDrawable).fillColor!!.defaultColor)
                assertEquals(question.title, unanswered.requireView().findViewById<TextView>(R.id.questionType).text.toString())
                assertNotNull(unanswered.requireView().findViewById<EditText>(R.id.questionEdit))
                capture(activity, unanswered.requireView(), "study-question")
                FloatingUiState.setCurrentAnswer("one")
                write("answered", true); write("correct", true)
                val answered = session.getNewFragment(null, false)
                mount(activity, answered)
                assertTrue(answered is AnsweredSessionFragment)
                val answer = answered.requireView().findViewById<TextView>(R.id.questionEdit)
                assertEquals("one", answer.text.toString())
                assertEquals(ThemeUtil.getColor(R.attr.correctColorBackground), (answer.background as MaterialShapeDrawable).fillColor!!.defaultColor)
                assertTrue(androidx.core.graphics.ColorUtils.calculateContrast(answer.currentTextColor,
                    (answer.background as MaterialShapeDrawable).fillColor!!.defaultColor) >= 4.5)
                capture(activity, answered.requireView(), "study-answered")
                write("answered", false)
                val anki = session.getNewFragment(null, true)
                mount(activity, anki)
                assertTrue(anki is AnkiSessionFragment)
                anki.requireView().findViewById<View>(R.id.ankiShowAnswerButton).performClick()
                assertTrue(anki.requireView().findViewById<TextView>(R.id.answer).text.contains("One", ignoreCase = true))
                assertFalse(session.isAnswered) // revealing an answer never grades it
                capture(activity, anki.requireView(), "study-anki")
                write("state", SessionState.IN_LESSON_PRESENTATION)
                val lesson = session.getNewFragment(null, false)
                mount(activity, lesson)
                assertTrue(lesson is LessonSessionFragment)
                assertEquals("Start quiz", lesson.requireView().findViewById<TextView>(R.id.nextButton).text.toString())
                capture(activity, lesson.requireView(), "study-lesson")
                write("state", SessionState.FINISHING)
                val summary = session.getNewFragment(null, false)
                mount(activity, summary)
                assertTrue(summary is SummarySessionFragment)
                assertNotNull(summary.requireView().findViewById<View>(R.id.finishButton))
                for ((panelId, textId) in listOf(R.id.correctSummary to R.id.correctPercentage, R.id.incorrectSummary to R.id.incorrectPercentage)) {
                    val panel = summary.requireView().findViewById<View>(panelId)
                    val label = summary.requireView().findViewById<TextView>(textId)
                    assertTrue(androidx.core.graphics.ColorUtils.calculateContrast(label.currentTextColor,
                        (panel.background as MaterialShapeDrawable).fillColor!!.defaultColor) >= 4.5)
                    val semantic = if (panelId == R.id.correctSummary) R.attr.correctColorBackground else R.attr.incorrectColorBackground
                    assertEquals(ThemeUtil.getColor(semantic), (panel.background as MaterialShapeDrawable).strokeColor!!.defaultColor)
                }
                capture(activity, summary.requireView(), "study-summary")
                assertEquals(0, item.numAnswers)
                assertEquals(SessionItemState.ACTIVE, item.state)
            }
        }
    }

    @Test fun reviewControlsStayOutsidePromptAndScrollWithLargeTextAndImeSpace() {
        fixture().use { scenario -> scenario.onActivity { activity ->
            for ((name, night, scale, width, height, promptHeight) in listOf(
                ReviewSize("light", Configuration.UI_MODE_NIGHT_NO, 1f, 360, 560),
                ReviewSize("fixed-height", Configuration.UI_MODE_NIGHT_NO, 1f, 360, 560, 100),
                ReviewSize("dark-large", Configuration.UI_MODE_NIGHT_YES, 2f, 320, 300),
                ReviewSize("landscape", Configuration.UI_MODE_NIGHT_YES, 1f, 640, 220))) {
                val config = Configuration(activity.resources.configuration).apply {
                    uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or night
                    fontScale = scale
                    orientation = if (width > height) Configuration.ORIENTATION_LANDSCAPE else Configuration.ORIENTATION_PORTRAIT
                }
                val context = androidx.appcompat.view.ContextThemeWrapper(activity.createConfigurationContext(config), ActiveTheme.getCurrentTheme().styleId).apply {
                    theme.applyStyle(R.style.SmokyUiTheme, true)
                }
                val root = LayoutInflater.from(context).inflate(R.layout.fragment_unanswered_question, null)
                StudyStyle.page(root)
                StudyStyle.question(root, subject.backgroundColor)
                val prompt = root.findViewById<View>(R.id.questionView)
                prompt.layoutParams.height = (promptHeight * context.resources.displayMetrics.density).toInt()
                (prompt.layoutParams as android.widget.LinearLayout.LayoutParams).weight = 0f
                root.findViewById<com.smouldering_durtles.wk.views.SubjectInfoButtonView>(R.id.questionText).apply {
                    setSubject(subject)
                    setTransparent(true)
                    setSizeForQuiz(true)
                    setMaxSize((160 * context.resources.displayMetrics.density).toInt(), (80 * context.resources.displayMetrics.density).toInt())
                }
                root.findViewById<TextView>(R.id.progress).text = "0/1"
                root.findViewById<TextView>(R.id.questionType).text = "Kanji meaning"
                root.findViewById<EditText>(R.id.questionEdit).setText("draft answer")
                root.findViewById<View>(R.id.dontKnowButton).visibility = View.VISIBLE
                root.findViewById<TextView>(R.id.specialButton1).apply { visibility = View.VISIBLE; text = "Play audio" }
                root.findViewById<TextView>(R.id.specialButton2).apply { visibility = View.VISIBLE; text = "Ignore typo" }
                // Attach like a real page so stateful Ripple/MaterialShape backgrounds receive enabled state.
                activity.setContentView(root)
                val density = context.resources.displayMetrics.density
                captureSize(activity, root, "study-refined-$name", (width * density).toInt(), (height * density).toInt())
                val glyph = root.findViewById<View>(R.id.questionText)
                assertTrue("The quiz glyph must fit inside the configured prompt height", glyph.height <= prompt.height)
                assertTrue(glyph.top >= 0 && glyph.bottom <= prompt.height)
                assertNull(prompt.findViewById<View>(R.id.submitButton))
                assertNull(prompt.findViewById<View>(R.id.specialButton1))
                val submit = root.findViewById<TextView>(R.id.submitButton)
                assertTrue(submit.isEnabled)
                assertTrue(submit.drawableState.contains(android.R.attr.state_enabled))
                val fill = (submit.background as android.graphics.drawable.RippleDrawable).getDrawable(0) as MaterialShapeDrawable
                assertTrue("Attached primary background must receive enabled state", fill.state.contains(android.R.attr.state_enabled))
                val renderedFill = fill.fillColor!!.getColorForState(fill.state, fill.fillColor!!.defaultColor)
                assertEquals(com.google.android.material.color.MaterialColors.getColor(submit, androidx.appcompat.R.attr.colorPrimary), renderedFill)
                assertTrue(androidx.core.graphics.ColorUtils.calculateContrast(submit.currentTextColor, renderedFill) >= 4.5)
                val dontKnow = root.findViewById<View>(R.id.dontKnowButton)
                assertTrue(submit.left >= dontKnow.right)
                assertTrue(submit.height >= (48 * density).toInt())
                assertTrue(submit.right <= (submit.parent as View).width)
                val scroll = (root as android.view.ViewGroup).getChildAt(0) as androidx.core.widget.NestedScrollView
                if (scale > 1f || width > height) {
                    assertTrue(scroll.getChildAt(0).height > scroll.height)
                    scroll.scrollTo(0, scroll.getChildAt(0).height - scroll.height)
                    val submitBounds = android.graphics.Rect(0, 0, submit.width, submit.height)
                    scroll.offsetDescendantRectToMyCoords(submit, submitBounds)
                    submitBounds.offset(-scroll.scrollX, -scroll.scrollY)
                    assertTrue("Submit must be fully reachable in the limited viewport", submitBounds.top >= 0 && submitBounds.bottom <= scroll.height)
                    assertTrue(submitBounds.left >= 0 && submitBounds.right <= scroll.width)
                    captureSize(activity, root, "study-refined-$name-scrolled", (width * density).toInt(), (height * density).toInt())
                }
                assertEquals("draft answer", root.findViewById<EditText>(R.id.questionEdit).text.toString())
            }
        } }
    }

    @Test fun summaryPercentagesAndActionsFitNarrowLargeText() {
        fixture().use { scenario -> scenario.onActivity { activity ->
            val config = Configuration(activity.resources.configuration).apply { fontScale = 2f }
            val context = androidx.appcompat.view.ContextThemeWrapper(activity.createConfigurationContext(config), ActiveTheme.getCurrentTheme().styleId).apply {
                    theme.applyStyle(R.style.SmokyUiTheme, true)
                }
            val root = LayoutInflater.from(context).inflate(R.layout.fragment_summary, null)
            StudyStyle.page(root)
            StudyStyle.summary(root)
            val density = context.resources.displayMetrics.density
            captureSize(activity, root, "study-summary-large", (320 * density).toInt(), (640 * density).toInt())
            val value = root.findViewById<TextView>(R.id.correctPercentage)
            assertTrue(value.width >= value.paint.measureText(value.text.toString()))
            assertTrue(value.right <= (value.parent as View).width)
            val show = root.findViewById<View>(R.id.showButton)
            val finish = root.findViewById<View>(R.id.finishButton)
            assertTrue(show.right <= finish.left || show.bottom <= finish.top)
            assertTrue(finish.right <= (finish.parent as View).width)
        } }
    }

    private data class ReviewSize(val name: String, val night: Int, val scale: Float, val width: Int, val height: Int, val promptHeight: Int = 140)

    @Test fun answerDraftSurvivesFragmentViewRecreation() {
        fixture().use { scenario ->
            scenario.onActivity { activity ->
                val fragment = session.getNewFragment(null, false)
                mount(activity, fragment)
                fragment.requireView().findViewById<EditText>(R.id.questionEdit).setText("draft answer")
                activity.supportFragmentManager.beginTransaction().detach(fragment).commitNow()
                activity.supportFragmentManager.beginTransaction().attach(fragment).commitNow()
                assertEquals("draft answer", fragment.requireView().findViewById<EditText>(R.id.questionEdit).text.toString())
                assertSame(question, fragment.question)
                assertEquals(0, item.numAnswers)
            }
        }
    }

    @Test fun classicQuestionKeepsItsOriginalSemanticSurface() {
        prefs.edit().putBoolean("modern_settings", false).commit()
        fixture().use { scenario -> scenario.onActivity { activity ->
            val fragment = session.getNewFragment(null, false)
            mount(activity, fragment)
            assertFalse(ModernUi.isModern(fragment.requireContext()))
            val background = fragment.requireView().findViewById<View>(R.id.questionView).background
            assertTrue(background is ColorDrawable)
            assertEquals(subject.backgroundColor, (background as ColorDrawable).color)
        } }
    }

    @Test fun realSessionActivityPreservesQuestionAcrossClassicRoundTrip() {
        val monitor = instrumentation.addMonitor(NoApiKeyHelpActivity::class.java.name,
            Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null), true)
        try {
            ActivityScenario.launch(SessionActivity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    assertTrue(ModernUi.isModern(activity))
                    assertTrue(activity.supportFragmentManager.findFragmentById(R.id.fragment) is UnansweredSessionFragment)
                    activity.findViewById<EditText>(R.id.questionEdit).setText("draft answer")
                }
                instrumentation.waitForIdleSync()
                android.os.SystemClock.sleep(200) // allow the rendered frame to reach SurfaceFlinger
                instrumentation.uiAutomation.takeScreenshot()?.let { bitmap ->
                    File(instrumentation.targetContext.filesDir, "study-real-question-window.png").outputStream().use {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                    }
                    bitmap.recycle()
                }
                scenario.onActivity { activity ->
                    val editor = activity.findViewById<EditText>(R.id.questionEdit)
                    editor.requestFocus()
                    (activity.getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager)
                        .showSoftInput(editor, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
                }
                var imeVisible = false
                for (attempt in 0 until 40) {
                    scenario.onActivity { activity -> imeVisible = androidx.core.view.ViewCompat.getRootWindowInsets(activity.window.decorView)
                        ?.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime()) == true }
                    if (imeVisible) break
                    android.os.SystemClock.sleep(50)
                }
                assertTrue("The emulator IME should be visible for the review layout check", imeVisible)
                scenario.onActivity { activity ->
                    val submit = activity.findViewById<View>(R.id.submitButton)
                    submit.requestRectangleOnScreen(android.graphics.Rect(0, 0, submit.width, submit.height), true)
                }
                instrumentation.waitForIdleSync()
                android.os.SystemClock.sleep(100)
                scenario.onActivity { activity ->
                    for (id in listOf(R.id.questionEdit, R.id.submitButton)) {
                        val control = activity.findViewById<View>(id)
                        val visible = android.graphics.Rect()
                        assertTrue(control.getGlobalVisibleRect(visible))
                        assertEquals(control.height, visible.height())
                    }
                }
                instrumentation.uiAutomation.takeScreenshot()?.let { bitmap ->
                    File(instrumentation.targetContext.filesDir, "study-review-ime.png").outputStream().use {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                    }
                    bitmap.recycle()
                }
                scenario.onActivity { activity -> androidx.core.view.WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                    .hide(androidx.core.view.WindowInsetsCompat.Type.ime()) }
                prefs.edit().putBoolean("modern_settings", false).commit()
                scenario.recreate()
                scenario.onActivity { activity ->
                    assertFalse(ModernUi.isModern(activity))
                    assertEquals("draft answer", activity.findViewById<EditText>(R.id.questionEdit).text.toString())
                    assertTrue(activity.findViewById<View>(R.id.questionView).background is ColorDrawable)
                    assertSame(question, session.currentQuestion)
                }
                prefs.edit().putBoolean("modern_settings", true).commit()
                scenario.recreate()
                scenario.onActivity { activity ->
                    assertTrue(ModernUi.isModern(activity))
                    assertEquals("draft answer", activity.findViewById<EditText>(R.id.questionEdit).text.toString())
                    assertTrue(activity.findViewById<View>(R.id.questionView).background is MaterialShapeDrawable)
                    assertSame(question, session.currentQuestion)
                    assertEquals(0, item.numAnswers)
                }
            }
        } finally { instrumentation.removeMonitor(monitor) }
    }

    @Test fun pagingDetailsReturnsToResultsWithOneBackAction() {
        val monitor = instrumentation.addMonitor(NoApiKeyHelpActivity::class.java.name,
            Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null), true)
        try {
            val intent = Intent(instrumentation.targetContext, BrowseActivity::class.java)
                .putExtra("searchType", 1).putExtra("searchParameters", "offline-study-fixture")
            ActivityScenario.launch<BrowseActivity>(intent).use { scenario -> scenario.onActivity { activity ->
                val manager = activity.supportFragmentManager
                manager.executePendingTransactions()
                assertTrue(manager.findFragmentById(R.id.fragment) is SearchResultFragment)
                val ids = longArrayOf(90071, 90072, 90073)
                activity.loadSubjectInfoFragment(ids[0], ids, FragmentTransitionAnimation.NONE)
                manager.executePendingTransactions()
                assertEquals(1, manager.backStackEntryCount)
                activity.pageSubjectInfoFragment(ids[1], ids, FragmentTransitionAnimation.NONE)
                manager.executePendingTransactions()
                activity.pageSubjectInfoFragment(ids[2], ids, FragmentTransitionAnimation.NONE)
                manager.executePendingTransactions()
                assertEquals(1, manager.backStackEntryCount)
                assertTrue(manager.popBackStackImmediate())
                assertTrue(manager.findFragmentById(R.id.fragment) is SearchResultFragment)
                assertTrue(manager.fragments.none { it is com.smouldering_durtles.wk.fragments.SubjectInfoFragment })
                assertEquals(0, manager.backStackEntryCount)
            } }
        } finally { instrumentation.removeMonitor(monitor) }
    }

    @Test fun relatedSubjectLinksRetainTheirParentIncludingDeepLinks() {
        val monitor = instrumentation.addMonitor(NoApiKeyHelpActivity::class.java.name,
            Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null), true)
        try {
            for (deepLink in listOf(false, true)) {
                val intent = Intent(instrumentation.targetContext, BrowseActivity::class.java)
                if (deepLink) intent.putExtra("id", 90071L)
                else intent.putExtra("searchType", 1).putExtra("searchParameters", "offline-study-fixture")
                ActivityScenario.launch<BrowseActivity>(intent).use { scenario -> scenario.onActivity { activity ->
                    val manager = activity.supportFragmentManager
                    manager.executePendingTransactions()
                    val ids = longArrayOf(90071, 90072, 90073)
                    if (!deepLink) {
                        activity.loadSubjectInfoFragment(ids[0], ids, FragmentTransitionAnimation.NONE)
                        manager.executePendingTransactions()
                    }
                    val parentCount = manager.backStackEntryCount
                    activity.loadSubjectInfoFragment(ids[1], ids, FragmentTransitionAnimation.NONE)
                    manager.executePendingTransactions()
                    assertEquals(parentCount + 1, manager.backStackEntryCount)
                    activity.pageSubjectInfoFragment(ids[2], ids, FragmentTransitionAnimation.NONE)
                    manager.executePendingTransactions()
                    assertEquals(parentCount + 1, manager.backStackEntryCount)
                    assertTrue(manager.popBackStackImmediate())
                    assertEquals(ids[0], manager.findFragmentById(R.id.fragment)!!.requireArguments().getLong("id"))
                    assertEquals(parentCount, manager.backStackEntryCount)
                    if (deepLink) {
                        activity.pageSubjectInfoFragment(ids[1], ids, FragmentTransitionAnimation.NONE)
                        manager.executePendingTransactions()
                        assertEquals(0, manager.backStackEntryCount)
                        assertEquals(ids[1], manager.findFragmentById(R.id.fragment)!!.requireArguments().getLong("id"))
                    }
                } }
            }
        } finally { instrumentation.removeMonitor(monitor) }
    }

    @Test fun sessionLogRetainsCollapseEventAndItemNavigation() {
        prefs.edit().putBoolean("full_session_log", true).putBoolean("enable_advanced", true).commit()
        fixture().use { scenario -> scenario.onActivity { activity ->
            var openedSubject = -1L
            val owner = Proxy.newProxyInstance(Actment::class.java.classLoader, arrayOf(Actment::class.java)) { _, method, args ->
                when (method.name) {
                    "getLifecycle" -> activity.lifecycle
                    "goToSubjectInfo" -> { openedSubject = args!![0] as Long; null }
                    else -> null
                }
            } as Actment
            val adapter = com.smouldering_durtles.wk.adapter.sessionlog.SessionLogAdapter()
            adapter.setActment(owner)
            adapter.addEventStartSession(SessionType.SELF_STUDY)
            adapter.addEventSubmitAnkiCorrect(question) // records an event only; does not submit or grade
            adapter.initialize()
            val list = androidx.recyclerview.widget.RecyclerView(activity).apply {
                layoutManager = androidx.recyclerview.widget.LinearLayoutManager(activity)
                itemAnimator = null
                this.adapter = adapter
            }
            activity.setContentView(list)
            capture(activity, list, "study-session-log")
            val before = adapter.itemCount
            assertTrue(before >= 5)
            val header = list.findViewHolderForAdapterPosition(0)!!
            header.itemView.performClick()
            assertEquals(before - 1, adapter.itemCount)
            assertTrue(adapter.collapsedTags.contains("notstarted"))
            capture(activity, list, "study-session-log-collapsed")
            list.findViewHolderForAdapterPosition(0)!!.itemView.performClick()
            assertEquals(before, adapter.itemCount)
            capture(activity, list, "study-session-log")
            val itemPosition = (0 until adapter.itemCount).first { adapter.getItemViewType(it) == R.id.viewTypeLogItem }
            val itemView = list.findViewHolderForAdapterPosition(itemPosition)!!.itemView
            assertTrue(itemView.findViewById<TextView>(R.id.question1Status).text.contains("Unanswered"))
            itemView.performClick()
            assertEquals(subject.id, openedSubject)
            openedSubject = -1
            val eventPosition = (0 until adapter.itemCount).first { adapter.getItemViewType(it) == R.id.viewTypeLogEvent }
            val eventView = list.findViewHolderForAdapterPosition(eventPosition)!!.itemView
            assertTrue(eventView.findViewById<TextView>(R.id.details).text.contains("Anki correct"))
            eventView.performClick()
            assertEquals(subject.id, openedSubject)
            assertEquals(0, item.numAnswers)
            assertFalse(session.isAnswered)
        } }
    }

    @Test fun detailsKeepReadableRichContentAtLargeDarkText() {
        fixture().use { scenario -> scenario.onActivity { activity ->
            val config = Configuration(activity.resources.configuration).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or Configuration.UI_MODE_NIGHT_YES
                fontScale = 2f
            }
            val context = androidx.appcompat.view.ContextThemeWrapper(activity.createConfigurationContext(config), ActiveTheme.getCurrentTheme().styleId).apply {
                    theme.applyStyle(R.style.SmokyUiTheme, true)
                }
            context.theme.applyStyle(ActiveTheme.getCurrentTheme().styleId, false)
            val info = SubjectInfoView(context)
            info.setPadding(40, 40, 40, 40)
            info.setMaxFontSize(48)
            info.setContainerType(SubjectInfoView.ContainerType.BROWSE)
            info.setSubject(actment(activity), subject)
            val width = (360 * activity.resources.displayMetrics.density).toInt()
            info.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
            info.layout(0, 0, width, info.measuredHeight)
            assertTrue(info.findViewById<TextView>(R.id.meaning).text.contains("One"))
            assertTrue(info.findViewById<TextView>(R.id.meaningMnemonic).text.contains("single horizontal"))
            assertTrue(info.findViewById<View>(R.id.headline).background is MaterialShapeDrawable)
            val bitmap = Bitmap.createBitmap(info.width, info.height, Bitmap.Config.ARGB_8888)
            info.draw(Canvas(bitmap))
            File(activity.filesDir, "study-detail-dark-large.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        } }
    }

    @Test fun cancelingModernSynonymEditorLeavesSubjectUnchanged() {
        fixture().use { scenario ->
            val before = subject.meaningSynonyms.toList()
            scenario.onActivity { activity ->
                val info = SubjectInfoView(activity)
                info.setMaxFontSize(48)
                info.setSubject(actment(activity), subject)
                activity.setContentView(info)
                info.findViewById<View>(R.id.addSynonymButton).performClick()
            }
            onView(withText("Cancel")).inRoot(isDialog()).perform(click())
            assertEquals(before, subject.meaningSynonyms)
        }
    }

    private fun capture(context: Context, view: View, name: String) {
        val metrics = context.resources.displayMetrics
        val height = metrics.heightPixels - (96 * metrics.density).toInt()
        captureSize(context, view, name, metrics.widthPixels, height)
    }

    private fun captureSize(context: Context, view: View, name: String, width: Int, height: Int) {
        view.scrollTo(0, 0)
        forceMeasure(view)
        view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        view.viewTreeObserver.dispatchOnPreDraw() // TextView single-line scrolling resolves here
        refreshDrawableStates(view)
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        File(context.filesDir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    private fun refreshDrawableStates(view: View) {
        view.refreshDrawableState()
        view.jumpDrawablesToCurrentState()
        if (view is android.view.ViewGroup) for (index in 0 until view.childCount) refreshDrawableStates(view.getChildAt(index))
    }

    private fun forceMeasure(view: View) {
        view.forceLayout()
        if (view is android.view.ViewGroup) for (index in 0 until view.childCount) forceMeasure(view.getChildAt(index))
    }

    private fun fixture(): ActivityScenario<AboutActivity> = ActivityScenario.launch(AboutActivity::class.java).also { scenario ->
        scenario.onActivity { activity -> activity.setContentView(FrameLayout(activity).apply { id = R.id.fragment }) }
    }
    private fun mount(activity: AboutActivity, fragment: AbstractSessionFragment) {
        activity.supportFragmentManager.beginTransaction().replace(R.id.fragment, fragment).commitNow()
        fragment.updateViews()
    }
    private fun actment(activity: AboutActivity): Actment = Proxy.newProxyInstance(Actment::class.java.classLoader, arrayOf(Actment::class.java)) { _, method, _ ->
        if (method.name == "getLifecycle") activity.lifecycle else null
    } as Actment
    private fun read(name: String): Any? = Session::class.java.getDeclaredField(name).apply { isAccessible = true }.get(session)
    private fun write(name: String, value: Any?) { Session::class.java.getDeclaredField(name).apply { isAccessible = true }.set(session, value) }
}
