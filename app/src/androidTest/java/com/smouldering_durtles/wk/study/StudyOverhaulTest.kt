package com.smouldering_durtles.wk.study

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smouldering_durtles.wk.R
import com.smouldering_durtles.wk.activities.AboutActivity
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
                listOf("Apprentice", "Guru", "Master", "Enlightened").forEach { grid.addChoice(it, 0xff8b5ac2.toInt()) {} }
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
    @Test fun customReviewSelectionRestoresChoicesAcrossRecreation() {
        ActivityScenario.launch(AboutActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                SessionSelectionDialog.show(activity, false)
                activity.supportFragmentManager.executePendingTransactions()
                val host = activity.supportFragmentManager.findFragmentByTag("sessionSelection")!!
                val selection = host.javaClass.getDeclaredField("selection").apply { isAccessible = true }.get(host)!!
                val types = selection.javaClass.getDeclaredField("types").apply { isAccessible = true }.get(selection) as BooleanArray
                types[0] = false; types[2] = false
                val stages = selection.javaClass.getDeclaredField("stages").apply { isAccessible = true }.get(selection) as BooleanArray
                stages[2] = false; stages[3] = false
            }
            scenario.recreate()
            scenario.onActivity { activity ->
                val host = activity.supportFragmentManager.findFragmentByTag("sessionSelection") as androidx.fragment.app.DialogFragment
                val selection = host.javaClass.getDeclaredField("selection").apply { isAccessible = true }.get(host)!!
                assertArrayEquals(booleanArrayOf(false, true, false), selection.javaClass.getDeclaredField("types").apply { isAccessible = true }.get(selection) as BooleanArray)
                assertArrayEquals(booleanArrayOf(true, true, false, false), selection.javaClass.getDeclaredField("stages").apply { isAccessible = true }.get(selection) as BooleanArray)
                val root = host.requireDialog().window!!.decorView
                measure(root, 360); capture(root, File(activity.filesDir, "overhaul-custom-reviews.png"))
                host.dismiss(); activity.supportFragmentManager.executePendingTransactions()
            }
        }
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
