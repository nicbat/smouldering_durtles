package com.smouldering_durtles.wk.study

import android.app.Activity
import android.app.Instrumentation
import androidx.test.platform.app.InstrumentationRegistry
import com.smouldering_durtles.wk.activities.NoApiKeyHelpActivity
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smouldering_durtles.wk.R
import com.smouldering_durtles.wk.activities.AboutActivity
import com.smouldering_durtles.wk.activities.SessionSelectionActivity
import com.smouldering_durtles.wk.db.model.Subject
import com.smouldering_durtles.wk.db.model.SubjectEntity
import com.smouldering_durtles.wk.enums.SubjectType
import com.smouldering_durtles.wk.model.AdvancedSearchParameters
import com.smouldering_durtles.wk.ui.ChoiceGrid
import com.smouldering_durtles.wk.views.*
import com.smouldering_durtles.wk.util.SearchUtil
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class StudyOverhaulTest {
    @Test fun fourStageChoicesAndPitchStayInsideNarrowLargeTextBounds() {
        ActivityScenario.launch(AboutActivity::class.java).use { scenario -> scenario.onActivity { activity ->
            for (scale in listOf(1f, 2f)) {
                val config = Configuration(activity.resources.configuration).apply { fontScale = scale }
                val context = ContextThemeWrapper(activity.createConfigurationContext(config), R.style.SmokyUiTheme)
                val grid = ChoiceGrid(context)
                listOf("Apprentice", "Guru", "Master", "Enlightened").forEach { grid.addChoice("$it\n123 due", 0xff8b5ac2.toInt()) {} }
                measure(grid, 320)
                for (i in 0 until grid.childCount) {
                    val button = grid.getChildAt(i) as Button
                    assertTrue(button.left >= 0 && button.right <= grid.width)
                    for (line in 0 until button.layout.lineCount) assertTrue(button.layout.getLineWidth(line) <= button.width)
                }
                val pitch = PitchInfoView(context)
                pitch.setPitchInfo("いっしょうけんめい", listOf(com.smouldering_durtles.wk.model.PitchInfo("いっしょうけんめい", null, 5)))
                measure(pitch, 280)
                assertEquals("中高 · Rise–fall", PitchInfoView.patternLabel(5, 8))
                assertEquals("平板 · Flat", PitchInfoView.patternLabel(0, 4))
                capture(grid, File(activity.filesDir, "overhaul-stages-$scale.png"))
                capture(pitch, File(activity.filesDir, "overhaul-pitch-$scale.png"))
            }
        } }
    }
    @Test fun choicesKeepSemanticFillsContrastAndGroupSpacingAfterRestyling() {
        ActivityScenario.launch(AboutActivity::class.java).use { scenario -> scenario.onActivity { activity ->
            for (night in listOf(Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES)) {
                val config = Configuration(activity.resources.configuration).apply { uiMode = night }
                val context = ContextThemeWrapper(activity.createConfigurationContext(config), R.style.SmokyUiTheme)
                val column = android.widget.LinearLayout(context).apply { orientation = android.widget.LinearLayout.VERTICAL }
                val grids = (0..1).map {
                    ChoiceGrid(context).also { grid ->
                        listOf(0xff0098f0.toInt(), 0xffe80092.toInt(), 0xff9808f3.toInt()).forEachIndexed { i, color ->
                            ChoiceGrid.selected(grid.addChoice(listOf("Radicals", "Kanji", "Vocabulary")[i], color) {}, true)
                        }
                        column.addView(grid)
                    }
                }
                com.smouldering_durtles.wk.ui.ModernUi.style(column)
                measure(column, 360)
                val first = grids[0].getChildAt(0) as Button
                fun fill(button: Button): Int {
                    val shape = (button.background as android.graphics.drawable.RippleDrawable).getDrawable(0)
                        as com.google.android.material.shape.MaterialShapeDrawable
                    return shape.fillColor!!.getColorForState(button.drawableState, 0)
                }
                val selectedFill = fill(first)
                val painted = Bitmap.createBitmap(first.width, first.height, Bitmap.Config.ARGB_8888)
                first.background.setBounds(0, 0, first.width, first.height)
                first.background.draw(Canvas(painted))
                assertEquals(selectedFill, painted.getPixel(com.smouldering_durtles.wk.ui.ModernUi.dp(first, 16), first.height / 2))
                painted.recycle()
                assertNotEquals(selectedFill, fill(grids[0].getChildAt(1) as Button))
                for (grid in grids) for (i in 0 until grid.childCount) {
                    val button = grid.getChildAt(i) as Button
                    assertTrue(androidx.core.graphics.ColorUtils.calculateContrast(button.currentTextColor, fill(button)) >= 4.5)
                }
                val lastBottom = (0 until grids[0].childCount).maxOf { grids[0].getChildAt(it).bottom }
                assertTrue(grids[1].top + grids[1].getChildAt(0).top - lastBottom >= com.smouldering_durtles.wk.ui.ModernUi.dp(first, 20))
                ChoiceGrid.selected(first, false)
                assertNotEquals(selectedFill, fill(first))
                assertTrue(androidx.core.graphics.ColorUtils.calculateContrast(first.currentTextColor, fill(first)) >= 4.5)
                com.smouldering_durtles.wk.ui.ModernUi.style(column)
                assertFalse(first.isSelected)
                assertNotEquals(selectedFill, fill(first))
            }
        } }
    }

    @Test fun strokeOrderStartsStaticAndSupportsStepResetAndSequence() {
        ActivityScenario.launch(AboutActivity::class.java).use { scenario -> scenario.onActivity { activity ->
            val diagram = StrokeDiagramView(activity)
            diagram.setStrokeData(listOf("M10,20 L90,20", "M50,10 L50,90"))
            diagram.setAnimated(false)
            measure(diagram, 200)
            capture(diagram, File(activity.filesDir, "overhaul-strokes.png"))
            diagram.reset(); assertEquals("0 / 2", diagram.positionLabel)
            diagram.step(); assertEquals("1 / 2", diagram.positionLabel)
            diagram.step(); diagram.step(); assertEquals("2 / 2", diagram.positionLabel)
            diagram.showSequence()
            capture(diagram, File(activity.filesDir, "overhaul-strokes-all.png"))
            assertEquals("2 / 2", diagram.positionLabel)
        } }
    }
    @Test fun metadataUsesExactPartsOfSpeechAndDistinctMissingPitch() {
        val subject = Subject(SubjectEntity().apply {
            type = SubjectType.WANIKANI_VOCAB; characters = "大学"; partsOfSpeech = "[\"noun\"]"
            readings = "[{\"reading\":\"だいがく\",\"primary\":true,\"accepted_answer\":true}]"
            pitchInfo = "[[\"だいがく\",null,0]]"
        })
        val method = SearchUtil::class.java.getDeclaredMethod("matchesMetadata", Subject::class.java, AdvancedSearchParameters::class.java).apply { isAccessible = true }
        val filter = AdvancedSearchParameters().apply { partOfSpeech = "noun"; pitchPattern = "flat" }
        assertTrue(method.invoke(null, subject, filter) as Boolean)
        filter.partOfSpeech = "pronoun"; assertFalse(method.invoke(null, subject, filter) as Boolean)
        filter.partOfSpeech = ""; filter.pitchPattern = "unknown"; assertFalse(method.invoke(null, subject, filter) as Boolean)
    }
    @Test fun reviewSelectionHidesSubjectsAndAnswersWhileLessonsKeepTheirPreview() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val monitor = instrumentation.addMonitor(NoApiKeyHelpActivity::class.java.name,
            Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null), true)
        try {
            for (lessons in listOf(false, true)) {
                val intent = Intent(ApplicationProvider.getApplicationContext(), SessionSelectionActivity::class.java)
                    .putExtra("lessons", lessons)
                ActivityScenario.launch<SessionSelectionActivity>(intent).use { scenario -> scenario.onActivity { activity ->
                    val subject = Subject(SubjectEntity().apply {
                        type = SubjectType.WANIKANI_KANJI; characters = "機密"; level = 1
                        meanings = "[{\"meaning\":\"Secret fixture answer\",\"primary\":true,\"accepted_answer\":true}]"
                    })
                    for (field in listOf("selected", "pool")) {
                        SessionSelectionActivity::class.java.getDeclaredField(field).apply { isAccessible = true }
                            .set(activity, listOf(subject))
                    }
                    SessionSelectionActivity::class.java.getDeclaredMethod("showPreview").apply { isAccessible = true }.invoke(activity)
                    fun texts(view: View): List<String> = when (view) {
                        is android.widget.TextView -> listOf(view.text.toString())
                        is ViewGroup -> (0 until view.childCount).flatMap { texts(view.getChildAt(it)) }
                        else -> emptyList()
                    }
                    val displayed = texts(activity.findViewById(R.id.selectionContent))
                    assertEquals(lessons, displayed.any { it.contains("機密") })
                    assertEquals(lessons, displayed.any { it.contains("Secret fixture answer") })
                    assertTrue(displayed.contains(if (lessons) "1 lesson selected" else "1 review selected"))
                    assertTrue(activity.findViewById<Button>(R.id.selectionStart).minimumHeight >=
                        com.smouldering_durtles.wk.ui.ModernUi.dp(activity.findViewById(R.id.selectionStart), 56))
                } }
            }
        } finally { instrumentation.removeMonitor(monitor) }
    }

    @Test fun reviewStageTogglesKeepCountsAndUpdateWithoutLoadingOrRebuildingOtherButtons() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val monitor = instrumentation.addMonitor(NoApiKeyHelpActivity::class.java.name,
            Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null), true)
        try {
            val intent = Intent(ApplicationProvider.getApplicationContext(), SessionSelectionActivity::class.java)
                .putExtra("lessons", false)
            ActivityScenario.launch<SessionSelectionActivity>(intent).use { scenario -> scenario.onActivity { activity ->
                fun field(name: String) = SessionSelectionActivity::class.java.getDeclaredField(name).apply { isAccessible = true }
                val fixtures = listOf(
                    Subject(SubjectEntity().apply { id = 99001; type = SubjectType.WANIKANI_KANJI; level = 1; srsStageId = 1 }),
                    Subject(SubjectEntity().apply { id = 99002; type = SubjectType.WANIKANI_VOCAB; level = 2; srsStageId = 5 })
                )
                field("generation").setInt(activity, field("generation").getInt(activity) + 1)
                field("pool").set(activity, fixtures)
                field("poolLevel").setInt(activity, 1)
                field("poolLoaded").setBoolean(activity, true)
                field("visible").setBoolean(activity, true)
                val refresh = SessionSelectionActivity::class.java.getDeclaredMethod("refresh").apply { isAccessible = true }
                refresh.invoke(activity)
                val buttons = field("stageButtons").get(activity) as List<Button>
                val headline = field("coverage").get(activity) as android.widget.TextView
                val details = field("coverageDetails").get(activity) as android.widget.TextView
                val observed = mutableListOf<String>()
                headline.addTextChangedListener(object : android.text.TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { observed.add(s.toString()) }
                    override fun afterTextChanged(s: android.text.Editable?) {}
                })
                assertEquals("2 reviews selected", headline.text.toString())
                val guruBackground = buttons[1].background
                repeat(4) {
                    buttons[0].performClick()
                    assertEquals("1 review selected", headline.text.toString())
                    assertEquals("Apprentice\n1 due", buttons[0].text.toString())
                    assertTrue(details.text.isNotEmpty())
                    buttons[0].performClick()
                    assertEquals("2 reviews selected", headline.text.toString())
                    assertEquals("✓ Apprentice\n1 due", buttons[0].text.toString())
                    assertSame(guruBackground, buttons[1].background)
                }
                assertFalse(observed.any { it.contains("Loading") || it.isEmpty() })
                assertEquals(0, (field("preview").get(activity) as ViewGroup).childCount)
                val types = field("typeButtons").get(activity) as List<Button>
                types[1].performClick()
                assertEquals("✓ Apprentice\n0 due", buttons[0].text.toString())
                assertEquals("✓ Guru\n1 due", buttons[1].text.toString())
                types[1].performClick()
                val scope = (activity.findViewById<ViewGroup>(R.id.selectionContent)).getChildAt(2) as ChoiceGrid
                (scope.getChildAt(1) as Button).performClick()
                assertEquals("✓ Apprentice\n1 due", buttons[0].text.toString())
                assertEquals("✓ Guru\n0 due", buttons[1].text.toString())
            } }
        } finally { instrumentation.removeMonitor(monitor) }
    }

    @Test fun customReviewSelectionRestoresChoicesAcrossRecreation() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val monitor = instrumentation.addMonitor(NoApiKeyHelpActivity::class.java.name,
            Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null), true)
        try {
            val intent = Intent(ApplicationProvider.getApplicationContext(), SessionSelectionActivity::class.java).putExtra("lessons", false)
            ActivityScenario.launch<SessionSelectionActivity>(intent).use { scenario ->
                scenario.onActivity { activity ->
                    val types = SessionSelectionActivity::class.java.getDeclaredField("types").apply { isAccessible = true }.get(activity) as BooleanArray
                    types[0] = false; types[2] = false
                    val stages = SessionSelectionActivity::class.java.getDeclaredField("stages").apply { isAccessible = true }.get(activity) as BooleanArray
                    stages[2] = false; stages[3] = false
                }
                scenario.recreate()
                scenario.onActivity { activity ->
                    assertArrayEquals(booleanArrayOf(false, true, false), SessionSelectionActivity::class.java.getDeclaredField("types").apply { isAccessible = true }.get(activity) as BooleanArray)
                    assertArrayEquals(booleanArrayOf(true, true, false, false), SessionSelectionActivity::class.java.getDeclaredField("stages").apply { isAccessible = true }.get(activity) as BooleanArray)
                    val choices = activity.findViewById<ViewGroup>(R.id.selectionContent).getChildAt(1) as ChoiceGrid
                    val foreground = (choices.getChildAt(0) as Button).currentTextColor
                    val shape = ((choices.getChildAt(0) as Button).background as android.graphics.drawable.RippleDrawable)
                        .getDrawable(0) as com.google.android.material.shape.MaterialShapeDrawable
                    val background = shape.fillColor!!.getColorForState(choices.getChildAt(0).drawableState, 0)
                    assertTrue(androidx.core.graphics.ColorUtils.calculateContrast(foreground, background) >= 4.5)
                    val root = activity.window.decorView
                    capture(root, File(activity.filesDir, "overhaul-custom-reviews.png"))
                }
            }
        } finally { instrumentation.removeMonitor(monitor) }
    }
    private fun measure(view: View, dp: Int) {
        view.measure(View.MeasureSpec.makeMeasureSpec((dp * view.resources.displayMetrics.density).toInt(), View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
    }
    private fun capture(view: View, file: File) {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap)); file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }; bitmap.recycle()
    }
}
