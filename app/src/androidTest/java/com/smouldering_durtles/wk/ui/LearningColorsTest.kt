package com.smouldering_durtles.wk.ui

import android.content.res.Configuration
import android.graphics.Color
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.graphics.ColorUtils
import androidx.preference.PreferenceManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.material.color.MaterialColors
import com.smouldering_durtles.wk.GlobalSettings
import com.smouldering_durtles.wk.R
import com.smouldering_durtles.wk.db.model.Subject
import com.smouldering_durtles.wk.db.model.SubjectEntity
import com.smouldering_durtles.wk.enums.SubjectCardLayout
import com.smouldering_durtles.wk.enums.SubjectType
import com.smouldering_durtles.wk.model.SubjectCardBinder
import com.smouldering_durtles.wk.views.SubjectInfoButtonView
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

/** Color policy is presentation-only; fixtures never write subject/account data. */
@RunWith(AndroidJUnit4::class)
class LearningColorsTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val base = instrumentation.targetContext
    private val prefs = PreferenceManager.getDefaultSharedPreferences(base)
    private var previous: String? = null
    @Before fun setup() { previous = prefs.getString("learning_colors", null) }
    @After fun restore() {
        prefs.edit().apply { if (previous == null) remove("learning_colors") else putString("learning_colors", previous) }.commit()
    }
    private fun mode(value: String) { prefs.edit().putString("learning_colors", value).commit() }
    private fun context(dark: Boolean = false) = ContextThemeWrapper(base.createConfigurationContext(Configuration(base.resources.configuration).apply {
        uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
    }), R.style.SmokyUiTheme)

    @Test fun subtleDefaultAndNeutralHaveReadableTextInBothModes() {
        prefs.edit().remove("learning_colors").commit()
        assertEquals("soft", GlobalSettings.Display.getLearningColors())
        for (dark in listOf(false, true)) for (selection in listOf("soft", "neutral")) {
            mode(selection)
            val ctx = context(dark)
            val surface = MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorSurfaceContainerLow, 0)
            for (semantic in listOf(0xFFFF00AA.toInt(), 0xFF00AAFF.toInt(), 0xFFAA00FF.toInt(), Color.BLACK, Color.WHITE)) {
                val fill = LearningColors.fill(ctx, semantic)
                assertNotEquals(semantic, fill)
                assertTrue("$selection dark=$dark must preserve text contrast", ColorUtils.calculateContrast(LearningColors.ink(ctx, Color.WHITE), fill) >= 4.5)
                if (selection == "neutral") assertEquals(surface, fill)
            }
        }
    }
    @Test fun originalAndClassicKeepExactCustomColors() {
        val original = 0xFF4B6D8F.toInt()
        mode("original")
        val modern = context()
        assertEquals(original, LearningColors.fill(modern, original))
        assertEquals(original, LearningColors.accent(modern, original))
        assertEquals(original, LearningColors.ink(modern, original))
        val classic = ContextThemeWrapper(base, R.style.WKLightTheme)
        for (selection in listOf("soft", "neutral", "original")) {
            mode(selection)
            assertFalse(LearningColors.isMuted(classic))
            assertEquals(original, LearningColors.fill(classic, original))
            assertEquals(original, LearningColors.accent(classic, original))
        }
    }
    @Test fun subjectGlyphUsesPairedInkWithoutChangingItsPalette() {
        instrumentation.runOnMainSync {
            val subject = subject()
            val originalFill = subject.buttonBackgroundColor
            val originalInk = subject.textColor
            val ctx = context()
            val button = SubjectInfoButtonView(ctx)
            val field = SubjectInfoButtonView::class.java.getDeclaredField("textColor").apply { isAccessible = true }
            for (selection in listOf("soft", "neutral", "original")) {
                mode(selection)
                button.setSubject(subject)
                assertEquals(LearningColors.ink(ctx, originalInk), field.getInt(button))
                assertEquals(originalFill, subject.buttonBackgroundColor)
                assertEquals(originalInk, subject.textColor)
            }
        }
    }
    @Test fun neutralSearchStillIdentifiesSubjectTypeAndKeepsNavigation() {
        mode("neutral")
        instrumentation.runOnMainSync {
            val parent = FrameLayout(ContextThemeWrapper(context(), R.style.SmokySearchTheme))
            val subject = subject()
            val binder = SubjectCardBinder(SubjectCardLayout.NORMAL)
            val card = binder.createView(subject.type, parent)
            var clicked = false
            binder.bind(card, subject, { clicked = true }, true, true)
            assertTrue(card.findViewById<TextView>(R.id.progress).text.toString().contains(subject.type.description))
            card.performClick()
            assertTrue(clicked)
        }
    }
    private fun subject() = Subject(SubjectEntity().apply {
        id = 90098; type = SubjectType.WANIKANI_KANJI; characters = "一"; slug = "一"; level = 1; srsStageId = 1
        meanings = "[{\"meaning\":\"One\",\"primary\":true,\"accepted_answer\":true}]"
        readings = "[{\"reading\":\"いち\",\"type\":\"onyomi\",\"primary\":true,\"accepted_answer\":true}]"
    })
}
