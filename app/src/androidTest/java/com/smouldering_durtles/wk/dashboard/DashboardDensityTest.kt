package com.smouldering_durtles.wk.dashboard

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import androidx.core.graphics.ColorUtils
import com.smouldering_durtles.wk.ui.LearningColors
import com.google.android.material.color.MaterialColors
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.action.ViewActions.click
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.smouldering_durtles.wk.Actment
import com.smouldering_durtles.wk.R
import com.smouldering_durtles.wk.activities.AboutActivity
import com.smouldering_durtles.wk.db.Converters
import com.smouldering_durtles.wk.enums.ActiveTheme
import com.smouldering_durtles.wk.model.AdvancedSearchParameters
import com.smouldering_durtles.wk.model.LevelProgress
import com.smouldering_durtles.wk.model.SrsBreakDown
import com.smouldering_durtles.wk.model.SrsSystem
import com.smouldering_durtles.wk.util.WeakLcoRef
import com.smouldering_durtles.wk.views.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File
import java.lang.reflect.Proxy

/** Synthetic, in-memory chart models. No network, account writes, or study submissions. */
@RunWith(AndroidJUnit4::class)
class DashboardDensityTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val prefs = PreferenceManager.getDefaultSharedPreferences(instrumentation.targetContext)
    private val keys = listOf("first_time_setup", "show_srs_breakdown", "show_level_progression", "learning_colors", "show_post60_subsections", "show_post60_reverse")
    private lateinit var previous: Map<String, Any?>

    @Before fun setup() {
        previous = keys.associateWith { prefs.all[it] }
        prefs.edit().putInt("first_time_setup", 1).putBoolean("show_srs_breakdown", true)
            .putBoolean("show_level_progression", true).putString("learning_colors", "soft").commit()
    }
    @After fun restore() {
        val edit = prefs.edit()
        previous.forEach { (key, value) -> when (value) {
            null -> edit.remove(key)
            is String -> edit.putString(key, value)
            is Boolean -> edit.putBoolean(key, value)
            is Int -> edit.putInt(key, value)
        } }
        edit.commit()
    }

    @Test fun compactProgressAndDistributionKeepExactCountsAndSearchActions() {
        ActivityScenario.launch(AboutActivity::class.java).use { scenario -> scenario.onActivity { activity ->
            val context = context(activity)
            var search: AdvancedSearchParameters? = null
            val owner = Proxy.newProxyInstance(Actment::class.java.classLoader, arrayOf(Actment::class.java)) { _, method, args ->
                when (method.name) {
                    "getLifecycle" -> activity.lifecycle
                    "goToSearchResult" -> { search = Converters.getObjectMapper().readValue(args!![1] as String, AdvancedSearchParameters::class.java); null }
                    else -> null
                }
            } as Actment
            val row = LevelProgressRowView(context)
            val entry = LevelProgress(8).entries.first { it.level == 8 }
            intArrayOf(21, 4, 0, 3, 0, 2, 0, 1, 5, 4).copyInto(entry.buckets)
            row.setEntry(WeakLcoRef(owner), entry)
            measure(row, 328)
            assertEquals("21 of 40 passed", row.findViewById<TextView>(R.id.levelProgressCount).text.toString())
            assertEquals(dp(context, 6), row.findViewById<View>(R.id.bar).height)
            assertTrue("Normal progress row should fit in 56dp", row.height <= dp(context, 56))
            row.performClick()
            assertEquals(8, search!!.minLevel)
            assertEquals(entry.type, search!!.itemTypes.single())

            val srs = SrsBreakDownView(context)
            srs.setLifecycleOwner(owner)
            update(srs, srsModel(intArrayOf(232, 104, 81, 62, 16)))
            measure(srs, 328)
            assertEquals("495 items", srs.findViewById<TextView>(R.id.srsDistributionTotal).text.toString())
            val apprentice = srs.findViewById<View>(R.id.breakdownBucket0View)
            val guru = srs.findViewById<View>(R.id.breakdownBucket1View)
            assertEquals(apprentice.top, guru.top)
            assertTrue(apprentice.right <= guru.left)
            for (id in listOf(R.id.breakdownBucket0View, R.id.breakdownBucket1View, R.id.breakdownBucket2View,
                R.id.breakdownBucket3View, R.id.breakdownBucket4View)) assertTrue(srs.findViewById<View>(id).height >= dp(context, 48))
            srs.findViewById<View>(R.id.breakdownBucket2View).performClick()
            assertTrue(search!!.srsStages.contains("master"))
            assertTrue("Distribution should be shorter than five old 56dp rows", srs.height < dp(context, 240))
            capture(srs, "dashboard-density-srs")
            capture(row, "dashboard-density-progress")
        } }
    }

    @Test fun narrowLargeTextStacksWithoutClippingAndHandlesLargeCounts() {
        ActivityScenario.launch(AboutActivity::class.java).use { scenario -> scenario.onActivity { activity ->
            val context = context(activity, 2f, true)
            val row = LevelProgressRowView(context)
            val entry = LevelProgress(8).entries.first { it.level == 8 }
            entry.buckets.fill(200_000_000)
            row.setEntry(null, entry)
            measure(row, 280)
            val label = row.findViewById<TextView>(R.id.label)
            val count = row.findViewById<TextView>(R.id.levelProgressCount)
            assertTrue(count.top >= label.bottom)
            assertTextFits(count)
            val srs = SrsBreakDownView(context)
            update(srs, srsModel(IntArray(5) { 500_000_000 }))
            measure(srs, 280)
            assertEquals("2,500,000,000 items", srs.findViewById<TextView>(R.id.srsDistributionTotal).text.toString())
            val apprentice = srs.findViewById<View>(R.id.breakdownBucket0View)
            val guru = srs.findViewById<View>(R.id.breakdownBucket1View)
            assertTrue(guru.left >= apprentice.right || guru.top >= apprentice.bottom)
            assertAllTextFits(srs)
            capture(srs, "dashboard-density-srs-large-dark")
            capture(row, "dashboard-density-progress-large-dark")
        } }
    }

    @Test fun emptyAndHiddenDistributionStaySafeAndCanReturn() {
        ActivityScenario.launch(AboutActivity::class.java).use { scenario -> scenario.onActivity { activity ->
            val context = context(activity)
            val srs = SrsBreakDownView(context)
            update(srs, SrsBreakDown())
            measure(srs, 280)
            assertEquals("No SRS items yet", srs.findViewById<TextView>(R.id.srsDistributionTotal).text.toString())
            capture(srs, "dashboard-density-empty")
            prefs.edit().putBoolean("show_srs_breakdown", false).commit()
            update(srs, SrsBreakDown())
            assertEquals(View.GONE, srs.visibility)
            prefs.edit().putBoolean("show_srs_breakdown", true).commit()
            update(srs, srsModel(intArrayOf(1, 0, 0, 0, 0)))
            assertEquals(View.VISIBLE, srs.visibility)
        } }
    }

    @Test fun neutralDistributionRetainsVisibleStageBoundaries() {
        prefs.edit().putString("learning_colors", "neutral").commit()
        ActivityScenario.launch(AboutActivity::class.java).use { scenario -> scenario.onActivity { activity ->
            val context = context(activity)
            val bar = SrsDistributionBarView(context, null)
            bar.setValues(intArrayOf(1, 1, 1, 1, 1))
            bar.measure(View.MeasureSpec.makeMeasureSpec(dp(context, 300), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(dp(context, 12), View.MeasureSpec.EXACTLY))
            bar.layout(0, 0, bar.measuredWidth, bar.measuredHeight)
            val bitmap = Bitmap.createBitmap(bar.width, bar.height, Bitmap.Config.ARGB_8888)
            bar.draw(Canvas(bitmap))
            assertEquals(com.google.android.material.color.MaterialColors.getColor(bar,
                com.google.android.material.R.attr.colorSurfaceContainerHighest), bitmap.getPixel(Math.round(bar.width / 5f), bar.height / 2))
            assertEquals(com.google.android.material.color.MaterialColors.getColor(bar,
                androidx.appcompat.R.attr.colorPrimary), bitmap.getPixel(bar.width / 10, bar.height / 2))
            bitmap.recycle()
        } }
    }

    @Test fun levelMarkerKeepsRoundedUpNinetyPercentTarget() {
        ActivityScenario.launch(AboutActivity::class.java).use { scenario -> scenario.onActivity { activity ->
            val context = context(activity)
            val bar = LevelProgressBarView(context)
            bar.setValues(intArrayOf(9, 2, 0, 0, 0, 0, 0, 0, 0, 0))
            bar.setShowTarget(true)
            measure(bar, 300)
            val bitmap = Bitmap.createBitmap(bar.width, bar.height, Bitmap.Config.ARGB_8888)
            bar.draw(Canvas(bitmap))
            // 90% of eleven requires ten passed; keep the original rounding rule.
            val targetX = 10 * bar.width / 11
            assertEquals(com.google.android.material.color.MaterialColors.getColor(bar,
                com.google.android.material.R.attr.colorOnSurface), bitmap.getPixel(targetX, bar.height / 2))
            bitmap.recycle()
        } }
    }

    @Test fun post60ColorsFollowModeWithoutChangingCountsOrderOrSourcePalettes() {
        ActivityScenario.launch(AboutActivity::class.java).use { scenario -> scenario.onActivity { activity ->
            val context = context(activity)
            val deep = ActiveTheme.getStageDeepBucketColors().clone()
            val shallow = ActiveTheme.getShallowStageBucketColors7().clone()
            val data = srsModel(intArrayOf(23, 11, 8, 6, 2))
            for (mode in listOf("original", "soft", "neutral")) {
                prefs.edit().putString("learning_colors", mode).commit()
                for (subsections in listOf(false, true)) for (reversed in listOf(false, true)) {
                    prefs.edit().putBoolean("show_post60_subsections", subsections)
                        .putBoolean("show_post60_reverse", reversed).commit()
                    val source = if (subsections) deep else shallow
                    val expectedValues = IntArray(source.size) { if (subsections) data.getPost60DeepCount(it) else data.getPost60ShallowCount(it) }
                    val expectedColors = source.map { LearningColors.accent(context, it) }.toIntArray()
                    if (reversed) { expectedValues.reverse(); expectedColors.reverse() }
                    val bar = Post60ProgressBarView(context)
                    bar.setBreakdown(data)
                    measure(bar, 328)
                    assertArrayEquals(expectedValues, intField(bar, "values"))
                    assertArrayEquals(expectedColors, intField(bar, "colors"))
                    val inks = intField(bar, "textColors")
                    expectedColors.forEachIndexed { i, fill ->
                        val actualFill = ColorUtils.compositeColors(fill, MaterialColors.getColor(bar,
                            com.google.android.material.R.attr.colorSurfaceContainerLow))
                        assertTrue(ColorUtils.calculateContrast(inks[i], actualFill) >= 4.5)
                    }
                    val bitmap = Bitmap.createBitmap(bar.width, bar.height, Bitmap.Config.ARGB_8888)
                    bar.draw(Canvas(bitmap))
                    val first = expectedValues.indexOfFirst { it > 0 }
                    val middle = expectedValues[first] * bar.width / expectedValues.sum() / 2
                    assertEquals(expectedColors[first], bitmap.getPixel(middle, 0))
                    bitmap.recycle()
                }
            }
            assertArrayEquals(deep, ActiveTheme.getStageDeepBucketColors())
            assertArrayEquals(shallow, ActiveTheme.getShallowStageBucketColors7())
            val classic = Post60ProgressBarView(activity)
            classic.setBreakdown(data)
            measure(classic, 328)
            assertArrayEquals(deep.reversedArray(), intField(classic, "colors"))
        } }
    }

    @Test fun post60DetailsKeepNamesAndExactCountsInBothDisplayOrders() {
        prefs.edit().putString("learning_colors", "neutral").commit()
        ActivityScenario.launch(AboutActivity::class.java).use { scenario ->
            for (deep in listOf(false, true)) for (reversed in listOf(false, true)) {
                prefs.edit().putBoolean("show_post60_subsections", deep)
                    .putBoolean("show_post60_reverse", reversed).commit()
                val rows = if (deep) listOf("Locked: 0", "Initiate: 0", "Apprentice I: 23", "Apprentice II: 0",
                    "Apprentice III: 0", "Apprentice IV: 0", "Guru I: 11", "Guru II: 0", "Master: 8", "Enlightened: 6", "Burned: 2")
                else listOf("Locked: 0", "Initiate: 0", "Apprentice: 23", "Guru: 11", "Master: 8", "Enlightened: 6", "Burned: 2")
                val expected = (if (reversed) rows.reversed() else rows).joinToString("\n")
                scenario.onActivity { activity ->
                    val themed = ContextThemeWrapper(activity, R.style.SmokyDashboardTheme)
                    themed.theme.applyStyle(ActiveTheme.getCurrentTheme().styleId, false)
                    val panel = Post60ProgressView(themed)
                    activity.setContentView(panel)
                    val bar = panel.findViewById<Post60ProgressBarView>(R.id.bar)
                    bar.setBreakdown(srsModel(intArrayOf(23, 11, 8, 6, 2)))
                    measure(panel, 328)
                    assertEquals(expected, bar.contentDescription.toString())
                    assertEquals(dp(themed, 24), bar.height)
                    val details = panel.findViewById<View>(R.id.post60StageCounts)
                    assertTrue(details.height >= dp(themed, 48))
                    details.performClick()
                }
                onView(withText(expected)).inRoot(isDialog()).check(matches(isDisplayed()))
                onView(withText(android.R.string.ok)).inRoot(isDialog()).perform(click())
            }
            scenario.onActivity { activity ->
                assertNull(Post60ProgressView(activity).findViewById<View>(R.id.post60StageCounts))
            }
        }
    }

    @Test fun lessonReviewCountsUseReadableScopedSubjectColors() {
        ActivityScenario.launch(AboutActivity::class.java).use { scenario -> scenario.onActivity { activity ->
            val context = context(activity)
            val fills = ActiveTheme.getSubjectTypeBackgroundColors().clone()
            val inks = ActiveTheme.getSubjectTypeTextColors().clone()
            val ids = listOf(intArrayOf(R.id.headerRad, R.id.headerKan, R.id.headerVoc),
                intArrayOf(R.id.lessonCurrentRad, R.id.lessonCurrentKan, R.id.lessonCurrentVoc),
                intArrayOf(R.id.lessonPastRad, R.id.lessonPastKan, R.id.lessonPastVoc),
                intArrayOf(R.id.reviewCurrentRad, R.id.reviewCurrentKan, R.id.reviewCurrentVoc),
                intArrayOf(R.id.reviewPastRad, R.id.reviewPastKan, R.id.reviewPastVoc))
            for (mode in listOf("original", "soft", "neutral")) {
                prefs.edit().putString("learning_colors", mode).commit()
                val table = LessonReviewBreakdownView(context)
                for (row in ids) row.forEachIndexed { i, id ->
                    val cell = table.findViewById<TextView>(id)
                    val fill = (cell.background as ColorDrawable).color
                    assertEquals(LearningColors.fill(context, fills[i]), fill)
                    val actualFill = ColorUtils.compositeColors(fill, MaterialColors.getColor(cell,
                        com.google.android.material.R.attr.colorSurfaceContainerLow))
                    assertTrue(ColorUtils.calculateContrast(cell.currentTextColor, actualFill) >= 4.5)
                    if (id != R.id.headerRad && id != R.id.headerKan && id != R.id.headerVoc) assertEquals(View.VISIBLE, cell.visibility)
                }
            }
            val classic = LessonReviewBreakdownView(activity)
            for (row in ids) row.forEachIndexed { i, id ->
                val cell = classic.findViewById<TextView>(id)
                assertEquals(fills[i], (cell.background as ColorDrawable).color)
                assertEquals(inks[i], cell.currentTextColor)
            }
            assertArrayEquals(fills, ActiveTheme.getSubjectTypeBackgroundColors())
            assertArrayEquals(inks, ActiveTheme.getSubjectTypeTextColors())
        } }
    }

    private fun intField(view: View, field: String): IntArray = view.javaClass.getDeclaredField(field).run {
        isAccessible = true
        get(view) as IntArray
    }

    @Test fun classicKeepsFullHeightProgressAndOriginalDistribution() {
        ActivityScenario.launch(AboutActivity::class.java).use { scenario -> scenario.onActivity { activity ->
            val bar = LevelProgressBarView(activity)
            measure(bar, 280)
            assertEquals(dp(activity, 24), bar.height)
            assertNull(SrsBreakDownView(activity).findViewById<View>(R.id.srsDistributionBar))
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
    private fun srsModel(counts: IntArray): SrsBreakDown {
        val system = SrsSystem(9000, "Offline density fixture", 0, 1, 5, 9)
        for (i in 0L..9L) system.addStage(i, i * 3600)
        system.finish()
        return SrsBreakDown().apply { listOf(1L, 5L, 7L, 8L, 9L).forEachIndexed { i, stage -> addCount(system.getStage(stage), counts[i]) } }
    }
    private fun update(view: SrsBreakDownView, model: SrsBreakDown) {
        SrsBreakDownView::class.java.getDeclaredMethod("update", SrsBreakDown::class.java).apply { isAccessible = true }.invoke(view, model)
    }
    private fun dp(context: Context, value: Int) = (value * context.resources.displayMetrics.density).toInt()
    private fun measure(view: View, width: Int) {
        view.measure(View.MeasureSpec.makeMeasureSpec(dp(view.context, width), View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
    }
    private fun assertTextFits(view: TextView) {
        assertTrue(view.layout.height <= view.height - view.compoundPaddingTop - view.compoundPaddingBottom)
        for (i in 0 until view.layout.lineCount) assertEquals(0, view.layout.getEllipsisCount(i))
    }
    private fun assertAllTextFits(view: View) {
        if (view is TextView) assertTextFits(view)
        if (view is ViewGroup) for (i in 0 until view.childCount) assertAllTextFits(view.getChildAt(i))
    }
    private fun capture(view: View, name: String) {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        File(instrumentation.targetContext.filesDir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
