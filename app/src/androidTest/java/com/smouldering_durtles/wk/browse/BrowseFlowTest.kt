package com.smouldering_durtles.wk.browse

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.EditText
import android.widget.Spinner
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import com.smouldering_durtles.wk.R
import com.smouldering_durtles.wk.activities.AboutActivity
import com.smouldering_durtles.wk.activities.BrowseActivity
import com.smouldering_durtles.wk.activities.NoApiKeyHelpActivity
import com.smouldering_durtles.wk.db.Converters
import com.smouldering_durtles.wk.enums.SearchSortOrder
import com.smouldering_durtles.wk.enums.SubjectType
import com.smouldering_durtles.wk.fragments.BrowseOverviewFragment
import com.smouldering_durtles.wk.fragments.SearchResultFragment
import com.smouldering_durtles.wk.model.AdvancedSearchParameters
import com.smouldering_durtles.wk.ui.SearchStyle
import com.smouldering_durtles.wk.views.AdvancedSearchFormView
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import java.io.File

@RunWith(AndroidJUnit4::class)
class BrowseFlowTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val prefs = PreferenceManager.getDefaultSharedPreferences(instrumentation.targetContext)
    private val keys = listOf("modern_settings", "settings_dynamic_color", "browse_overview_dismissed", "notification_permission_asked")
    private lateinit var previous: Map<String, Any?>
    @Before fun setup() {
        previous = keys.associateWith { prefs.all[it] }
        prefs.edit().putBoolean("modern_settings", true).putBoolean("settings_dynamic_color", false)
            .putBoolean("browse_overview_dismissed", true).putBoolean("notification_permission_asked", true).commit()
    }
    @After fun restore() {
        val edit = prefs.edit()
        previous.forEach { (key, value) -> if (value == null) edit.remove(key) else edit.putBoolean(key, value as Boolean) }
        edit.commit()
    }

    @Test fun allModernFilterValuesRoundTripAndClassicStaysAvailable() {
        ActivityScenario.launch(AboutActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val form = AdvancedSearchFormView(SearchStyle.context(activity))
                assertTrue(form.findViewById<View>(R.id.minLevel) is TextInputEditText)
                assertTrue(form.findViewById<View>(R.id.limitToLeeches) is MaterialSwitch)
                assertTrue(form.findViewById<View>(R.id.searchButton1) is MaterialButton)
                val expected = AdvancedSearchParameters().apply {
                    minLevel = 3; maxLevel = 17; minFrequency = 20; maxFrequency = 500
                    leechesOnly = true; upcomingReviewLessThan = 12; upcomingReviewMoreThan = 2
                    incorrectAnswerWithin = 24; burnedLessThan = 90; burnedMoreThan = 3
                    sortOrder = SearchSortOrder.values().last()
                    starRatings.addAll(listOf(1, 3, 5)); srsStages.addAll(listOf("locked", "initial", "master", "burned"))
                    itemTypes.addAll(SubjectType.values().toList()); jlptLevels.addAll(listOf(0, 3, 5)); joyoGrades.addAll(listOf(1, 3, 7))
                }
                form.injectParameters(expected)
                val mapper = Converters.getObjectMapper()
                assertEquals(mapper.valueToTree<com.fasterxml.jackson.databind.JsonNode>(expected), mapper.valueToTree<com.fasterxml.jackson.databind.JsonNode>(form.extractParameters()))
                form.setSortOrderVisibility(false)
                assertEquals(View.GONE, (form.findViewById<View>(R.id.sortOrder).parent as View).visibility)
                val classic = AdvancedSearchFormView(activity.applicationContext)
                assertFalse(classic.findViewById<View>(R.id.minLevel) is TextInputEditText)
                classic.injectParameters(expected)
                assertEquals(mapper.valueToTree<com.fasterxml.jackson.databind.JsonNode>(expected), mapper.valueToTree<com.fasterxml.jackson.databind.JsonNode>(classic.extractParameters()))
            }
        }
    }

    @Test fun largeDarkFilterRowsFitAndRemainReadable() {
        ActivityScenario.launch(AboutActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val config = Configuration(activity.resources.configuration).apply { fontScale = 2f; uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or Configuration.UI_MODE_NIGHT_YES }
                val context = SearchStyle.context(activity.createConfigurationContext(config))
                val form = AdvancedSearchFormView(context)
                val width = (328 * activity.resources.displayMetrics.density).toInt()
                form.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
                form.layout(0, 0, width, form.measuredHeight)
                for (id in listOf(R.id.minLevel, R.id.maxLevel, R.id.reviewLessHours, R.id.sortOrder, R.id.limitToLeeches)) {
                    val view = form.findViewById<View>(id)
                    assertTrue(view.width > 0)
                    assertTrue("Control should fit its parent", view.right <= (view.parent as View).width)
                }
                for (id in listOf(R.id.reviewLessHours, R.id.reviewMoreHours, R.id.incorrectHours)) {
                    val field = form.findViewById<TextInputEditText>(id)
                    assertEquals("Hours", field.hint.toString())
                    assertEquals("Units must not squeeze the numeric field", width, (field.parent as View).width)
                }
                assertEquals("Days ago", form.findViewById<TextInputEditText>(R.id.burnedLessDays).hint.toString())
                // Capture just the numeric/filter controls; the full form can be many screens tall.
                val bitmap = Bitmap.createBitmap(width, minOf(form.height, 1600), Bitmap.Config.ARGB_8888)
                form.draw(Canvas(bitmap))
                File(activity.filesDir, "browse-filters-dark-large.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                bitmap.recycle()
            }
        }
    }

    @Test fun levelButtonsNavigateAndHaveReadableModernOverview() {
        val monitor = instrumentation.addMonitor(NoApiKeyHelpActivity::class.java.name,
            Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null), true)
        try {
            ActivityScenario.launch<BrowseActivity>(Intent(instrumentation.targetContext, BrowseActivity::class.java)).use { scenario ->
                instrumentation.waitForIdleSync()
                scenario.onActivity { activity ->
                    activity.supportFragmentManager.executePendingTransactions()
                    val fragment = activity.supportFragmentManager.findFragmentById(R.id.fragment) as BrowseOverviewFragment
                    // Supply only the maximum level count, never subjects or account data.
                    val render = BrowseOverviewFragment::class.java.getDeclaredMethod("render", Int::class.javaPrimitiveType)
                    render.isAccessible = true
                    render.invoke(fragment, 60)
                    // Render and inspect in one main-thread turn so a pending DB callback cannot replace this fixture.
                    val root = activity.window.decorView
                    root.measure(View.MeasureSpec.makeMeasureSpec(root.width, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(root.height, View.MeasureSpec.EXACTLY))
                    root.layout(0, 0, root.measuredWidth, root.measuredHeight)
                    val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
                    root.draw(Canvas(bitmap))
                    File(activity.filesDir, "browse-overview-light.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    bitmap.recycle()
                    val table = activity.findViewById<android.widget.TableLayout>(R.id.levelTable)
                    val queryPosition = IntArray(2)
                    val tablePosition = IntArray(2)
                    activity.findViewById<View>(R.id.queryButton).getLocationInWindow(queryPosition)
                    table.getLocationInWindow(tablePosition)
                    assertTrue("Keyword search should be reachable before the 60-level table", queryPosition[1] < tablePosition[1])
                    var levelSeven: View? = null
                    for (row in 0 until table.childCount) {
                        val children = table.getChildAt(row) as android.widget.TableRow
                        for (column in 0 until children.childCount) {
                            val button = children.getChildAt(column)
                            assertTrue(button is MaterialButton)
                            val minTarget = (48 * button.resources.displayMetrics.density).toInt()
                            assertTrue("Level button width keeps a touch target", button.width >= minTarget)
                            assertTrue("Level button height keeps a touch target", button.height >= minTarget)
                            if (button.contentDescription == "Browse level 7") levelSeven = button
                        }
                    }
                    assertNotNull(levelSeven)
                    levelSeven!!.performClick()
                    activity.supportFragmentManager.executePendingTransactions()
                    val result = activity.supportFragmentManager.findFragmentById(R.id.fragment) as SearchResultFragment
                    assertEquals(0, result.requireArguments().getInt("searchType"))
                    assertEquals("7", result.requireArguments().getString("searchParameters"))
                }
            }
        } finally { instrumentation.removeMonitor(monitor) }
    }

    @Test fun queryNavigationRestoresDraftAndAppearanceRoundTrip() {
        val monitor = instrumentation.addMonitor(NoApiKeyHelpActivity::class.java.name,
            Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null), true)
        try {
            ActivityScenario.launch<BrowseActivity>(Intent(instrumentation.targetContext, BrowseActivity::class.java)).use { scenario ->
                scenario.onActivity { activity ->
                    activity.supportFragmentManager.executePendingTransactions()
                    activity.findViewById<EditText>(R.id.query).setText("smoky-local-query")
                    activity.findViewById<EditText>(R.id.minLevel).setText("7")
                    activity.findViewById<View>(R.id.queryButton).performClick()
                    activity.supportFragmentManager.executePendingTransactions()
                    assertTrue(activity.supportFragmentManager.findFragmentById(R.id.fragment) is SearchResultFragment)
                    activity.supportFragmentManager.popBackStackImmediate()
                    assertEquals("smoky-local-query", activity.findViewById<EditText>(R.id.query).text.toString())
                    assertEquals("7", activity.findViewById<EditText>(R.id.minLevel).text.toString())
                }
                scenario.recreate()
                scenario.onActivity { assertEquals("7", it.findViewById<EditText>(R.id.minLevel).text.toString()) }
                prefs.edit().putBoolean("modern_settings", false).commit()
                scenario.recreate()
                scenario.onActivity {
                    assertTrue(it.supportFragmentManager.findFragmentById(R.id.fragment) is BrowseOverviewFragment)
                    assertFalse(it.findViewById<View>(R.id.queryButton) is MaterialButton)
                    assertEquals("7", it.findViewById<EditText>(R.id.minLevel).text.toString())
                }
                prefs.edit().putBoolean("modern_settings", true).commit()
                scenario.recreate()
                scenario.onActivity {
                    assertTrue(it.findViewById<View>(R.id.queryButton) is MaterialButton)
                    assertEquals("7", it.findViewById<EditText>(R.id.minLevel).text.toString())
                }
            }
        } finally { instrumentation.removeMonitor(monitor) }
    }
}
