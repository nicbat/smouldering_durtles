package com.smouldering_durtles.wk.study

import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.material.color.MaterialColors
import com.smouldering_durtles.wk.R
import com.smouldering_durtles.wk.activities.AboutActivity
import com.smouldering_durtles.wk.db.model.Subject
import com.smouldering_durtles.wk.db.model.SubjectEntity
import com.smouldering_durtles.wk.enums.ActiveTheme
import com.smouldering_durtles.wk.fragments.SubjectInfoFragment
import com.smouldering_durtles.wk.model.SrsSystem
import com.smouldering_durtles.wk.ui.ModernUi
import com.smouldering_durtles.wk.views.SubjectInfoView
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class UiFeedbackTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val prefs = PreferenceManager.getDefaultSharedPreferences(instrumentation.targetContext)
    private val keys = listOf("modern_settings", "settings_dynamic_color", "hide_sentence_translations", "show_pitch_info")
    private lateinit var previous: Map<String, Any?>
    @Before fun setup() {
        previous = keys.associateWith { prefs.all[it] }
        prefs.edit().putBoolean("modern_settings", true).putBoolean("settings_dynamic_color", false)
            .putBoolean("hide_sentence_translations", true).putBoolean("show_pitch_info", false).commit()
    }
    @After fun restore() {
        val edit = prefs.edit()
        previous.forEach { (key, value) -> if (value == null) edit.remove(key) else edit.putBoolean(key, value as Boolean) }
        edit.commit()
    }

    @Test fun sentencesToggleBothWaysAndCopyOriginalJapaneseAndEnglishAfterBinding() {
        ActivityScenario.launch(AboutActivity::class.java).use { scenario -> scenario.onActivity { activity ->
            val info = SubjectInfoView(activity).apply { setContainerType(SubjectInfoView.ContainerType.BROWSE) }
            activity.setContentView(info)
            val subject = vocabulary()
            info.setSubject(activity, subject)
            val english = info.findViewById<TextView>(R.id.contextSentenceEnglish0)
            val japanese = info.findViewById<TextView>(R.id.contextSentenceJapanese0)
            assertNotNull(english.paint.maskFilter)
            english.performClick()
            assertNull(english.paint.maskFilter)
            info.layoutSubject(true)
            assertNull(english.paint.maskFilter) // same-subject refresh keeps the user's reveal
            english.performClick()
            assertNotNull(english.paint.maskFilter)
            val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            assertTrue(japanese.performLongClick())
            assertEquals("日本語の例文です。", clipboard.primaryClip!!.getItemAt(0).text.toString())
            assertTrue(english.performLongClick())
            assertEquals("This is an example sentence.", clipboard.primaryClip!!.getItemAt(0).text.toString())
            assertNotNull(english.paint.maskFilter)
            prefs.edit().putBoolean("hide_sentence_translations", false).commit()
            info.layoutSubject(true)
            assertNull(english.paint.maskFilter)
            assertFalse(english.isClickable)
            assertTrue(english.performLongClick())
        } }
    }

    @Test fun modernButtonsStayFlatWhilePressedFocusedDisabledAndRestyled() {
        ActivityScenario.launch(AboutActivity::class.java).use { scenario -> scenario.onActivity { activity ->
            val root = LayoutInflater.from(activity).inflate(R.layout.fragment_lesson, null)
            activity.setContentView(root)
            StudyStyle.page(root)
            LessonStyle.navigation(root, 1, 3)
            val buttons = listOf(root.findViewById<Button>(R.id.previousButton), root.findViewById<Button>(R.id.nextButton),
                Button(activity).apply { text = "Listen" }, Button(activity).apply { text = "Show items" })
            for (button in buttons) {
                ModernUi.button(button)
                assertNull(button.stateListAnimator)
                button.isPressed = true; button.requestFocus(); button.jumpDrawablesToCurrentState()
                assertEquals(0f, button.elevation, 0f)
                assertEquals(0f, button.translationZ, 0f)
                button.isPressed = false; button.isEnabled = false
                ModernUi.button(button)
                assertNull(button.stateListAnimator)
                assertEquals(0f, button.translationZ, 0f)
            }
            measure(root, 360, 640)
            save(root, "feedback-buttons-flat")
        } }
    }

    @Test fun browsingDetailsUseCompactHeadlineDisclosureAndModernDividers() {
        ActivityScenario.launch(AboutActivity::class.java).use { scenario -> scenario.onActivity { activity ->
            activity.setContentView(android.widget.FrameLayout(activity).apply { id = R.id.fragment })
            val fragment = SubjectInfoFragment()
            activity.supportFragmentManager.beginTransaction().replace(R.id.fragment, fragment).commitNow()
            val root = fragment.requireView()
            val info = root.findViewById<SubjectInfoView>(R.id.subjectInfo)
            info.setContainerType(SubjectInfoView.ContainerType.BROWSE)
            info.setMaxFontSize(100)
            info.setSubject(activity, vocabulary())
            measure(root, 360, 640)
            assertNotNull(root.findViewById<View>(R.id.lessonHeadlineRow))
            val details = root.findViewById<Button>(R.id.lessonDetailsToggle)
            assertEquals(View.GONE, root.findViewById<View>(R.id.detailsTable).visibility)
            details.performClick()
            assertEquals(View.VISIBLE, root.findViewById<View>(R.id.detailsTable).visibility)
            measure(root, 360, 640)
            save(root, "feedback-related-details")
        } }
    }

    @Test fun srsNotifierShowsBothStagesAndDoesNotCoverQuestionContentAtLargeText() {
        ActivityScenario.launch(AboutActivity::class.java).use { scenario -> scenario.onActivity { activity ->
            val system = SrsSystem(9091, "Notifier fixture", 0, 1, 5, 9).apply {
                for (i in 0L..9L) addStage(i, i * 3600)
                finish()
            }
            for ((dark, scale, width) in listOf(Triple(false, 1f, 360), Triple(true, 2f, 280))) {
                val config = Configuration(activity.resources.configuration).apply {
                    fontScale = scale
                    uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                        if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
                }
                val context = ContextThemeWrapper(activity.createConfigurationContext(config), ActiveTheme.getCurrentTheme().styleId)
                context.theme.applyStyle(R.style.SmokyUiTheme, true)
                val root = LayoutInflater.from(context).inflate(R.layout.fragment_answered_question, null)
                activity.setContentView(root)
                StudyStyle.page(root)
                val subject = vocabulary()
                root.findViewById<com.smouldering_durtles.wk.views.SubjectInfoButtonView>(R.id.questionText).apply {
                    setSubject(subject); setSizeSp(72)
                }
                StudyStyle.question(root, subject.backgroundColor)
                root.findViewById<TextView>(R.id.progress).text = "3 of 12"
                root.findViewById<TextView>(R.id.questionType).text = "Vocabulary meaning"
                root.findViewById<TextView>(R.id.questionEdit).apply {
                    text = "Japanese language"
                    StudyStyle.answerFeedback(this, 0xFFB6EDCC.toInt())
                }
                root.findViewById<Button>(R.id.nextButton).isEnabled = true
                root.findViewById<Button>(R.id.nextButton2).isEnabled = true
                root.findViewById<SubjectInfoView>(R.id.subjectInfo).apply {
                    setContainerType(SubjectInfoView.ContainerType.BROWSE); setMaxFontSize(100); setSubject(activity, subject)
                }
                val notifier = root.findViewById<TextView>(R.id.levelUpToastText)
                for ((old, new) in listOf(4L to 5L, 7L to 5L, 8L to 9L)) {
                    SrsStageNotifier.bind(notifier, system.getStage(old), system.getStage(new))
                    notifier.visibility = View.VISIBLE
                    measure(root, width, 640)
                    assertTrue(notifier.text.contains(system.getStage(old).name))
                    assertTrue(notifier.text.contains(system.getStage(new).name))
                    assertNotSame(root.findViewById<View>(R.id.questionView), notifier.parent)
                    assertEquals(View.ACCESSIBILITY_LIVE_REGION_POLITE, notifier.accessibilityLiveRegion)
                    assertTrue(notifier.layout.height <= notifier.height - notifier.compoundPaddingTop - notifier.compoundPaddingBottom)
                    save(notifier, "feedback-notifier-$dark-$old-$new")
                }
            }
        } }
    }

    private fun vocabulary() = Subject(SubjectEntity().apply {
        id = 90911; type = com.smouldering_durtles.wk.enums.SubjectType.WANIKANI_VOCAB
        characters = "日本語"; slug = "日本語"; level = 1; srsStageId = 5
        meanings = """[{"meaning":"Japanese language","primary":true,"accepted_answer":true}]"""
        readings = """[{"reading":"にほんご","primary":true,"accepted_answer":true}]"""
        meaningMnemonic = "Remember this meaning."
        contextSentences = """[{"ja":"日本語の例文です。","en":"This is an example sentence."}]"""
    })
    private fun measure(view: View, width: Int, height: Int) {
        val density = view.resources.displayMetrics.density
        view.measure(View.MeasureSpec.makeMeasureSpec((width * density).toInt(), View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec((height * density).toInt(), View.MeasureSpec.EXACTLY))
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
    }
    private fun save(view: View, name: String) {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        File(instrumentation.targetContext.filesDir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
