package com.smouldering_durtles.wk.search

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.smouldering_durtles.wk.Actment
import com.smouldering_durtles.wk.R
import com.smouldering_durtles.wk.activities.AboutActivity
import com.smouldering_durtles.wk.activities.BrowseActivity
import com.smouldering_durtles.wk.activities.NoApiKeyHelpActivity
import com.smouldering_durtles.wk.adapter.search.*
import com.smouldering_durtles.wk.db.model.Subject
import com.smouldering_durtles.wk.db.model.SubjectEntity
import com.smouldering_durtles.wk.enums.SubjectCardLayout
import com.smouldering_durtles.wk.enums.SubjectType
import com.smouldering_durtles.wk.fragments.BrowseOverviewFragment
import com.smouldering_durtles.wk.fragments.SearchResultFragment
import com.smouldering_durtles.wk.model.SubjectCardBinder
import com.smouldering_durtles.wk.ui.SearchStyle
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import java.io.File
import java.lang.reflect.Proxy

/** Synthetic subjects stay in memory; no token, database writes, or study submissions. */
@RunWith(AndroidJUnit4::class)
class SearchAppearanceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val prefs = PreferenceManager.getDefaultSharedPreferences(instrumentation.targetContext)
    private val keys = listOf("modern_settings", "settings_dynamic_color", "search_result_dismissed", "notification_permission_asked")
    private lateinit var previous: Map<String, Any?>
    private val fragment = SearchResultFragment()
    private lateinit var list: RecyclerView
    private lateinit var adapter: SearchResultAdapter

    @Before fun setup() {
        previous = keys.associateWith { prefs.all[it] }
        prefs.edit().putBoolean("modern_settings", true).putBoolean("settings_dynamic_color", false)
            .putBoolean("search_result_dismissed", true).putBoolean("notification_permission_asked", true).commit()
    }
    @After fun restore() {
        val edit = prefs.edit()
        previous.forEach { (key, value) -> if (value == null) edit.remove(key) else edit.putBoolean(key, value as Boolean) }
        edit.commit()
    }

    @Test fun populatedResultsKeepGroupingCountsAndCollapseState() {
        fixture().use { scenario ->
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                assertEquals(4, adapter.numSubjects)
                assertEquals(7, adapter.itemCount) // vocabulary and kana vocabulary share the existing group
                assertEquals(1, adapter.getItemSpanSize(1, 2))
                val card = list.findViewHolderForAdapterPosition(1)!!.itemView as MaterialCardView
                assertEquals("Ground", card.findViewById<TextView>(R.id.meaning).text.toString())
                assertEquals(MaterialColors.getColor(card, com.google.android.material.R.attr.colorSurfaceContainerLow), card.cardBackgroundColor.defaultColor)
                val ids = adapter.subjectIds.copyOf()
                list.findViewHolderForAdapterPosition(0)!!.itemView.performClick()
                assertEquals(6, adapter.itemCount)
                assertEquals(1, adapter.collapsedTags.size)
                assertArrayEquals(ids, adapter.subjectIds)
                save(activity, "search-light", list.rootView)
            }
            instrumentation.waitForIdleSync()
            scenario.onActivity {
                list.findViewHolderForAdapterPosition(0)!!.itemView.performClick()
                assertEquals(7, adapter.itemCount)
                assertTrue(adapter.collapsedTags.isEmpty())
            }
        }
    }

    @Test fun cardsKeepSubjectNavigationAndDoNotChangeOtherScreens() {
        fixture().use { scenario ->
            scenario.onActivity { activity ->
                val binder = SubjectCardBinder(SubjectCardLayout.NORMAL)
                assertFalse(binder.createView(SubjectType.WANIKANI_RADICAL, android.widget.FrameLayout(activity)) is MaterialCardView)
                val selected = subjects()[2]
                var clicked: Long? = null
                var contextIds: LongArray? = null
                val actment = Proxy.newProxyInstance(Actment::class.java.classLoader, arrayOf(Actment::class.java)) { _, method, args ->
                    when (method.name) {
                        "getLifecycle" -> activity.lifecycle
                        "goToSubjectInfo" -> { clicked = args!![0] as Long; contextIds = args[1] as LongArray; null }
                        else -> null
                    }
                } as Actment
                val view = binder.createView(selected.type, list)
                val holder = SubjectItemViewHolder(adapter, view, binder, actment)
                holder.bind(SubjectItem(selected))
                view.performClick()
                assertEquals(selected.id, clicked)
                assertArrayEquals(adapter.subjectIds, contextIds)
                clicked = null
                view.findViewById<View>(R.id.button).performClick()
                assertEquals(selected.id, clicked)
                assertEquals(selected.id, view.findViewById<View>(R.id.button).getTag(R.id.subjectId))
                assertTrue(view.findViewById<View>(R.id.button).contentDescription.contains(selected.characters!!))
            }
        }
    }

    @Test fun longTextAndDarkCardsFitAtDoubleFontSize() {
        fixture(dark = true, fontScale = 2f).use { scenario ->
            scenario.onActivity { activity ->
                assertEquals(1, (list.layoutManager as GridLayoutManager).spanCount)
                val subject = subjects()[2]
                val binder = SubjectCardBinder(SubjectCardLayout.NORMAL)
                val card = binder.createView(subject.type, list) as MaterialCardView
                binder.bind(card, subject, {}, true, true)
                val width = (328 * activity.resources.displayMetrics.density).toInt()
                card.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
                card.layout(0, 0, width, card.measuredHeight)
                for (id in listOf(R.id.meaning, R.id.reading, R.id.progress)) {
                    val text = card.findViewById<TextView>(id)
                    assertTrue(text.width > 0)
                    assertTrue(text.bottom <= (text.parent as View).height)
                    for (line in 0 until text.lineCount) assertEquals(0, text.layout.getEllipsisCount(line))
                }
                assertFalse(MaterialColors.isColorLight(card.cardBackgroundColor.defaultColor))
                save(activity, "search-dark-large-card", card)
            }
        }
    }

    @Test fun responsiveGridKeepsHeadersAndLongTitlesFullWidth() {
        fixture().use { scenario -> scenario.onActivity { activity ->
            val results = arrayListOf(
                subject(90101, SubjectType.WANIKANI_KANJI, "一", "One", "いち"),
                subject(90102, SubjectType.WANIKANI_KANJI, "二", "Two", "に"),
                subject(90103, SubjectType.WANIKANI_KANJI, "三", "Three", "さん"),
                subject(90104, SubjectType.WANIKANI_KANJI, "四", "Four", "し"),
                subject(90105, SubjectType.WANIKANI_VOCAB, "一生懸命", "With all one’s might; as hard as one can", "いっしょうけんめい"),
                subject(90106, SubjectType.WANIKANI_KANA_VOCAB, "ありがとう", "Thank you", null),
                subject(90107, SubjectType.WANIKANI_KANA_VOCAB, "よろしくおねがいします", "Please treat me well", null)
            )
            adapter.setResult(results)
            (list.parent as View).findViewById<TextView>(R.id.numHits).text = "7 subjects"
            layoutList(360)
            val manager = list.layoutManager as GridLayoutManager
            assertEquals("360dp phones have two readable columns", 2, manager.spanCount)
            assertEquals(2, adapter.getItemSpanSize(0, 2))
            val first = list.findViewHolderForAdapterPosition(1)!!.itemView
            val second = list.findViewHolderForAdapterPosition(2)!!.itemView
            assertEquals(first.top, second.top)
            assertTrue(first.right < second.left)
            assertTrue(first.width < list.width / 2)
            val progress = first.findViewById<TextView>(R.id.progress)
            assertEquals("Kanji · Level 1\nApprentice I", progress.text.toString())
            assertEquals("Keep the SRS stage together instead of orphaning its numeral", 2, progress.lineCount)
            val vocabPosition = (0 until adapter.itemCount).first { adapter.getItemViewType(it) == R.id.viewTypeResultVocabulary }
            val vocab = list.findViewHolderForAdapterPosition(vocabPosition)!!.itemView
            val meaning = vocab.findViewById<TextView>(R.id.meaning)
            assertTrue("Long meanings wrap in a grid cell", meaning.lineCount > 1)
            for (line in 0 until meaning.lineCount) assertEquals(0, meaning.layout.getEllipsisCount(line))
            assertEquals("Long Japanese terms keep a full-width card", 2, adapter.getItemSpanSize(adapter.itemCount - 1, 2))
            save(activity, "search-grid-light", list)
            val ids = adapter.subjectIds.copyOf()
            adapter.setShowingForm(true, manager)
            assertEquals("Refine form spans every column", 2, adapter.getItemSpanSize(0, 2))
            adapter.setShowingForm(false, manager)
            layoutList(280)
            assertEquals("Narrow windows fall back to one column", 1, manager.spanCount)
            layoutList(720)
            assertEquals("Wide windows increase density without oversized cards", 4, manager.spanCount)
            val wideCards = (1..4).map { list.findViewHolderForAdapterPosition(it)!!.itemView }
            assertTrue("All four kanji share one wide-screen row", wideCards.all { it.top == wideCards.first().top })
            wideCards.zipWithNext().forEach { (left, right) -> assertTrue(left.right < right.left) }
            assertTrue(wideCards.last().right <= list.width - list.paddingRight)
            assertArrayEquals(ids, adapter.subjectIds)
            save(activity, "search-grid-wide", list)
        } }
    }

    @Test fun darkGridUsesOneColumnAtLargeFontAndKeepsClassicSpans() {
        fixture(dark = true, fontScale = 2f).use { scenario -> scenario.onActivity { activity ->
            layoutList(360)
            assertEquals(1, (list.layoutManager as GridLayoutManager).spanCount)
            save(activity, "search-grid-dark-large", list)
            adapter.setModern(false)
            assertEquals(1, adapter.getItemSpanSize(1, 4)) // original radical cell
            assertEquals(4, adapter.getItemSpanSize(5, 4)) // original vocabulary row
        } }
    }

    @Test fun singletonSearchGroupsFlattenWithoutLosingCountsOrderOrCollapseTags() {
        fixture().use { scenario -> scenario.onActivity {
            adapter.setSortOrder(com.smouldering_durtles.wk.enums.SearchSortOrder.STAGE_TYPE)
            val a = subject(90301, SubjectType.WANIKANI_VOCAB, "一", "One", "いち")
            val b = subject(90302, SubjectType.WANIKANI_VOCAB, "二", "Two", "に", 2)
            // Use distinct levels too, independent of the built-in SRS fixture.
            adapter.setSortOrder(com.smouldering_durtles.wk.enums.SearchSortOrder.LEVEL_TYPE)
            adapter.setResult(arrayListOf(a, b))
            assertEquals(4, adapter.itemCount) // two levels, with no repeated Vocabulary subheading
            assertEquals(R.id.viewTypeResultLevelHeader, adapter.getItemViewType(0))
            assertEquals(R.id.viewTypeResultVocabulary, adapter.getItemViewType(1))
            assertArrayEquals(longArrayOf(a.id, b.id), adapter.subjectIds)
            adapter.setCollapsedTags(listOf("1 WANIKANI_VOCAB"))
            adapter.setResult(arrayListOf(a, b))
            assertEquals(4, adapter.itemCount) // an obsolete hidden heading cannot hide its subjects
            assertTrue(adapter.collapsedTags.contains("1 WANIKANI_VOCAB"))
            adapter.setResult(arrayListOf(a, subject(90303, SubjectType.WANIKANI_KANJI, "一", "One", "いち")))
            assertEquals(3, adapter.itemCount) // one level is hidden, two type headings remain; vocab stays collapsed
            adapter.setCollapsedTags(emptyList())
            adapter.setResult(arrayListOf(a))
            assertEquals(1, adapter.itemCount)
            assertEquals(1, adapter.numSubjects)
            adapter.setModern(false)
            adapter.setResult(arrayListOf(a))
            assertEquals(3, adapter.itemCount) // Classic retains the original tree presentation
        } }
    }

    @Test fun oneSrsStageAcrossTypesAndOneTypeAcrossStagesLoseRedundantNesting() {
        fixture().use { scenario -> scenario.onActivity {
            adapter.setSortOrder(com.smouldering_durtles.wk.enums.SearchSortOrder.STAGE_TYPE)
            val masterVocab = subject(90401, SubjectType.WANIKANI_VOCAB, "一", "One", "いち", itemStage = 7)
            val masterKanji = subject(90402, SubjectType.WANIKANI_KANJI, "一", "One", "いち", itemStage = 7)
            adapter.setResult(arrayListOf(masterVocab, masterKanji))
            assertEquals(4, adapter.itemCount) // type groups only, no redundant Master parent
            assertEquals(R.id.viewTypeResultItemTypeHeader, adapter.getItemViewType(0))
            val guruVocab = subject(90403, SubjectType.WANIKANI_VOCAB, "二", "Two", "に", itemStage = 5)
            adapter.setResult(arrayListOf(masterVocab, guruVocab))
            assertEquals(4, adapter.itemCount) // SRS groups only, no repeated Vocabulary children
            assertEquals(R.id.viewTypeResultSrsStageHeader, adapter.getItemViewType(0))
            assertEquals(R.id.viewTypeResultVocabulary, adapter.getItemViewType(1))
            assertArrayEquals(longArrayOf(guruVocab.id, masterVocab.id), adapter.subjectIds)
        } }
    }

    private fun layoutList(widthDp: Int) {
        val density = list.resources.displayMetrics.density
        val width = (widthDp * density).toInt()
        val height = (1400 * density).toInt()
        // The first layout updates the span count from the actual viewport; the second lays out that grid.
        repeat(2) {
            list.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
            list.layout(0, 0, width, height)
        }
    }

    @Test fun realBrowseBackStackSearchInputAndClassicRecreation() {
        val monitor = instrumentation.addMonitor(NoApiKeyHelpActivity::class.java.name,
            Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null), true)
        try {
            ActivityScenario.launch<BrowseActivity>(Intent(instrumentation.targetContext, BrowseActivity::class.java)).use { scenario ->
                scenario.onActivity {
                    it.supportFragmentManager.executePendingTransactions()
                    assertTrue(it.supportFragmentManager.findFragmentById(R.id.fragment) is BrowseOverviewFragment)
                    it.loadSearchResultFragment(null, 1, "smoky-no-matching-fixture")
                    it.supportFragmentManager.executePendingTransactions()
                }
                // Wait for the read-only local search's async callback, with a bounded deadline.
                val deadline = System.currentTimeMillis() + 5000
                var finished = false
                while (!finished && System.currentTimeMillis() < deadline) {
                    instrumentation.waitForIdleSync()
                    scenario.onActivity { finished = !it.findViewById<TextView>(R.id.numHits).text.toString().contains("Searching") }
                    if (!finished) Thread.sleep(50)
                }
                assertTrue("Search callback should complete", finished)
                scenario.onActivity { activity ->
                    assertTrue(activity.findViewById<TextView>(R.id.numHits).text.startsWith("No results"))
                    assertTrue(SearchStyle.isModern(activity.findViewById<View>(R.id.resultView).context))
                    assertFalse(SearchStyle.isModern(activity)) // no theme leakage into shared detail/study Views
                    val search = activity.menu!!.findItem(R.id.action_search)
                    assertTrue(search.expandActionView())
                }
                instrumentation.waitForIdleSync()
                scenario.onActivity { activity ->
                    val search = activity.menu!!.findItem(R.id.action_search)
                    val input = search.actionView as SearchView
                    assertFalse(input.isIconified)
                    assertEquals("smoky-no-matching-fixture", input.query.toString())
                    search.collapseActionView()
                    save(activity, "search-empty", activity.window.decorView)
                }
                scenario.recreate()
                scenario.onActivity { assertTrue(it.supportFragmentManager.findFragmentById(R.id.fragment) is SearchResultFragment) }
                prefs.edit().putBoolean("modern_settings", false).commit()
                scenario.recreate()
                scenario.onActivity { activity ->
                    assertFalse(SearchStyle.isModern(activity.findViewById<View>(R.id.resultView).context))
                    assertTrue(activity.supportFragmentManager.popBackStackImmediate())
                    assertTrue(activity.supportFragmentManager.findFragmentById(R.id.fragment) is BrowseOverviewFragment)
                }
                prefs.edit().putBoolean("modern_settings", true).commit()
                scenario.recreate()
                scenario.onActivity { assertTrue(SearchStyle.isModern(it.toolbar!!.context)) }
            }
        } finally { instrumentation.removeMonitor(monitor) }
    }

    private fun fixture(dark: Boolean = false, fontScale: Float = 1f): ActivityScenario<AboutActivity> {
        val scenario = ActivityScenario.launch(AboutActivity::class.java)
        scenario.onActivity { activity ->
            val config = Configuration(activity.resources.configuration).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
                this.fontScale = fontScale
            }
            val context: Context = SearchStyle.context(activity.createConfigurationContext(config))
            val root = LayoutInflater.from(activity).cloneInContext(context).inflate(R.layout.fragment_search_result_modern, null)
            root.findViewById<TextView>(R.id.numHits).text = "Search preview · 4 subjects"
            list = root.findViewById(R.id.resultView)
            list.itemAnimator = null // Capture final geometry, not an in-flight collapse/resize animation.
            adapter = SearchResultAdapter(fragment)
            adapter.setModern(true)
            SearchStyle.configureResultGrid(list, adapter)
            list.adapter = adapter
            adapter.setResult(subjects())
            activity.setContentView(root)
        }
        instrumentation.waitForIdleSync()
        return scenario
    }

    private fun subjects() = arrayListOf(
        subject(90001, SubjectType.WANIKANI_RADICAL, "一", "Ground", null),
        subject(90002, SubjectType.WANIKANI_KANJI, "一", "One", "いち"),
        subject(90003, SubjectType.WANIKANI_VOCAB, "一生懸命", "With all one’s might; as hard as one can", "いっしょうけんめい"),
        subject(90004, SubjectType.WANIKANI_KANA_VOCAB, "ありがとう", "Thank you", null)
    )
    private fun subject(id: Long, type: SubjectType, text: String, meaning: String, reading: String?, itemLevel: Int = 1, itemStage: Long = 1) = Subject(SubjectEntity().apply {
        this.id = id; this.type = type; characters = text; slug = text; level = itemLevel
        meanings = "[{\"meaning\":\"$meaning\",\"primary\":true,\"accepted_answer\":true}]"
        readings = if (reading == null) "[]" else "[{\"reading\":\"$reading\",\"primary\":true,\"accepted_answer\":true}]"
        srsStageId = itemStage
    })
    private fun save(context: Context, name: String, view: View) {
        if (view.width <= 0 || view.height <= 0) return
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        if (view is RecyclerView) {
            // A detached list omits its parent surface; real window roots paint their own background.
            canvas.drawColor(MaterialColors.getColor(view.context,
                com.google.android.material.R.attr.colorSurface, android.graphics.Color.TRANSPARENT))
        }
        view.draw(canvas)
        File(context.filesDir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
