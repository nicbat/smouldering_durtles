package com.smouldering_durtles.wk.dashboard

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.smouldering_durtles.wk.Actment
import com.smouldering_durtles.wk.GlobalSettings
import com.smouldering_durtles.wk.R
import com.smouldering_durtles.wk.WkApplication
import com.smouldering_durtles.wk.activities.AboutActivity
import com.smouldering_durtles.wk.db.Converters
import com.smouldering_durtles.wk.db.model.Subject
import com.smouldering_durtles.wk.db.model.SubjectEntity
import com.smouldering_durtles.wk.enums.ActiveTheme
import com.smouldering_durtles.wk.enums.SessionPriority
import com.smouldering_durtles.wk.enums.SubjectType
import com.smouldering_durtles.wk.livedata.LiveFirstTimeSetup
import com.smouldering_durtles.wk.livedata.LiveVacationMode
import com.smouldering_durtles.wk.model.*
import com.smouldering_durtles.wk.services.StudyNotificationText
import com.smouldering_durtles.wk.study.AudioButtonState
import com.smouldering_durtles.wk.views.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File
import java.lang.reflect.Proxy

/** Offline synthetic models and emulator-only mute preference; no study answers or account fixtures. */
@RunWith(AndroidJUnit4::class)
class DashboardIterationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val prefs = PreferenceManager.getDefaultSharedPreferences(instrumentation.targetContext)
    private val keys = listOf("first_time_setup", "show_timeline", "show_srs_breakdown", "srs_split_stages", "show_recent_unlocks",
        "show_burned_items", "enable_advanced", "lesson_level_up_first", "review_level_up_first", "modern_settings",
        "settings_dynamic_color", "show_pitch_info", "learning_colors", "lesson_order_priority")
    private lateinit var previous: Map<String, Any?>
    private val oldVacation = LiveVacationMode.getInstance().get()
    private val oldSetup = LiveFirstTimeSetup.getInstance().get()
    private var oldMute = false
    @Before fun setup() {
        previous = keys.associateWith { prefs.all[it] }
        oldMute = WkApplication.getDatabase().propertiesDao().isMuted
        prefs.edit().putInt("first_time_setup", 1).putBoolean("show_timeline", true).putBoolean("show_srs_breakdown", true)
            .putBoolean("show_recent_unlocks", true).putBoolean("show_burned_items", true).putBoolean("srs_split_stages", false)
            .putBoolean("enable_advanced", false).putBoolean("modern_settings", true).putBoolean("settings_dynamic_color", false)
            .putBoolean("show_pitch_info", false).putString("learning_colors", "soft").commit()
        LiveVacationMode.getInstance().post(false); LiveFirstTimeSetup.getInstance().post(1)
        instrumentation.waitForIdleSync()
    }
    @After fun restore() {
        WkApplication.getDatabase().propertiesDao().isMuted = oldMute
        val edit = prefs.edit()
        previous.forEach { (key, value) -> when(value) {
            null -> edit.remove(key)
            is Boolean -> edit.putBoolean(key, value)
            is String -> edit.putString(key, value)
            is Int -> edit.putInt(key, value)
        } }
        edit.commit()
        LiveVacationMode.getInstance().post(oldVacation); LiveFirstTimeSetup.getInstance().post(oldSetup)
    }

    @Test fun levelUpPathUsesUnpassedKanjiPrerequisitesAndPermanentMilestones() {
        val kanji = (1L..10L).map { id -> subject(id, SubjectType.WANIKANI_KANJI, passed = id <= 8, components = "[101,102]") }
        val ids = LevelUpPath.ids(kanji, 8, 60)
        assertEquals(setOf(9L, 10L, 101L, 102L), ids)
        assertTrue(LevelUpPath.includes(subject(101, SubjectType.WANIKANI_RADICAL, level = 7), ids))
        assertFalse(LevelUpPath.includes(subject(101, SubjectType.WANIKANI_RADICAL, passed = true), ids))
        assertFalse(LevelUpPath.includes(subject(101, SubjectType.WANIKANI_VOCAB), ids))
        assertFalse(LevelUpPath.includes(kanji[0], ids)) // passed once, even if its current stage is Apprentice
        val reached = kanji.take(9).mapIndexed { index, item -> if (index == 8) subject(item.id, SubjectType.WANIKANI_KANJI, passed = true) else item } + kanji.last()
        assertTrue(LevelUpPath.ids(reached, 8, 60).isEmpty())
        assertTrue(LevelUpPath.ids(kanji, 8, 8).isEmpty())
        assertTrue(LevelUpPath.ids(emptyList(), 8, 60).isEmpty())
        assertEquals(setOf(1L, 101L), LevelUpPath.ids(listOf(subject(1, SubjectType.WANIKANI_KANJI, components = "[101]")), 8, 60))
    }

    @Test fun priorityWorksWithoutAdvancedModeAndKeepsOtherItemsInStableOrder() {
        prefs.edit().putBoolean("lesson_level_up_first", true).putBoolean("review_level_up_first", true).commit()
        assertEquals(SessionPriority.LEVEL_UP_FIRST, GlobalSettings.AdvancedLesson.getOrderPriority())
        assertEquals(SessionPriority.LEVEL_UP_FIRST, GlobalSettings.AdvancedReview.getOrderPriority())
        val ordinary = subject(1, SubjectType.WANIKANI_VOCAB)
        val radical = subject(3, SubjectType.WANIKANI_RADICAL)
        val passed = subject(2, SubjectType.WANIKANI_KANJI, passed = true)
        val kanji = subject(4, SubjectType.WANIKANI_KANJI)
        val base = Comparator.comparingLong<Subject> { it.id }
        val comparator = SessionPriority.LEVEL_UP_FIRST.getComparator(base, setOf(2L, 3L, 4L), 8, 60)
        assertEquals(listOf(3L, 4L, 1L, 2L), listOf(kanji, ordinary, radical, passed).sortedWith(comparator).map { it.id })
        assertEquals(listOf(1L, 2L, 3L, 4L), listOf(kanji, ordinary, radical, passed)
            .sortedWith(SessionPriority.LEVEL_UP_FIRST.getComparator(base, setOf(3L, 4L), 60, 60)).map { it.id })
        prefs.edit().putBoolean("enable_advanced", true).putString("lesson_order_priority", "LEVEL_UP_FIRST").commit()
        GlobalSettings.setLevelUpPriority(com.smouldering_durtles.wk.enums.SessionType.LESSON, false)
        assertEquals(SessionPriority.NONE, GlobalSettings.AdvancedLesson.getOrderPriority())
    }

    @Test fun databaseCountsUseAvailabilityVisibilitySubscriptionAndPassedMilestones() {
        val database = androidx.room.Room.inMemoryDatabaseBuilder(instrumentation.targetContext,
            com.smouldering_durtles.wk.db.AppDatabase::class.java).allowMainThreadQueries().build()
        try {
            val sql = database.openHelper.writableDatabase
            val defaults = android.content.ContentValues()
            sql.query("PRAGMA table_info(subject)").use { columns ->
                while (columns.moveToNext()) {
                    if (columns.getInt(columns.getColumnIndexOrThrow("notnull")) == 0) continue
                    val name = columns.getString(columns.getColumnIndexOrThrow("name"))
                    if (columns.getString(columns.getColumnIndexOrThrow("type")).contains("TEXT")) defaults.put(name, "")
                    else defaults.put(name, 0)
                }
            }
            fun insert(id: Long, objectName: String, passed: Boolean = false, level: Int = 8, hidden: Boolean = false,
                       lesson: Boolean = false, review: Boolean = false, components: String = "[]") {
                val values = android.content.ContentValues(defaults).apply {
                    put("id", id); put("object", objectName); put("level", level); put("hiddenAt", if (hidden) 1 else 0)
                    put("passedAt", if (passed) 1 else 0); put("passed", passed); put("burnedAt", 0); put("resurrectedAt", 0)
                    put("unlockedAt", if (lesson || review) 1 else 0); put("startedAt", if (review) 1 else 0)
                    put("availableAt", if (review) 9000 else 0); put("componentSubjectIds", components)
                }
                sql.insert("subject", android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE, values)
            }
            for (id in 1L..8L) insert(id, "kanji", passed = true)
            insert(9, "kanji", lesson = true, components = "[101,102,105]")
            insert(10, "kanji", review = true, components = "[101,102,105]")
            insert(101, "radical", lesson = true)
            insert(102, "radical", level = 7, review = true)
            insert(103, "radical", passed = true, review = true)
            insert(104, "vocabulary", lesson = true)
            insert(105, "radical", hidden = true, lesson = true)
            val ids = database.subjectCollectionsDao().getLevelUpIds(8, 60)
            assertEquals(setOf(9L, 10L, 101L, 102L, 105L), ids.toSet())
            assertEquals(2, database.subjectAggregatesDao().countLevelUpLessons(ids, 60))
            assertEquals(2, database.subjectAggregatesDao().countLevelUpReviews(ids, 60, 10000))
            assertEquals(0, database.subjectAggregatesDao().countLevelUpReviews(ids, 60, 8000))
            assertTrue(database.subjectCollectionsDao().getLevelUpIds(8, 8).isEmpty())
            val ctx = database.subjectAggregatesDao().getAlertContext(60, 10000)
            assertEquals(3, ctx.numLessons) // vocab included in the overall queue, excluded from the path
            assertEquals(3, ctx.numReviews) // passed radical included in the queue, excluded from the path
            insert(9, "kanji", passed = true)
            assertTrue(database.subjectCollectionsDao().getLevelUpIds(8, 60).isEmpty()) // 90% now passed
        } finally { database.close() }
    }

    @Test fun timelineCountsAvailableLevelUpWorkWithoutCountingFutureReviews() {
        val timeline = TimeLine(48)
        timeline.addLesson(subject(1, SubjectType.WANIKANI_KANJI), true)
        timeline.addLesson(subject(2, SubjectType.WANIKANI_VOCAB), false)
        val now = timeline.createdAt
        timeline.addReview(review(3, now - 10000), true)
        timeline.addReview(review(4, now - 10000), false)
        timeline.addReview(review(5, now + 25 * com.smouldering_durtles.wk.Constants.HOUR), true)
        assertEquals(1, timeline.numLevelUpLessons)
        assertEquals(1, timeline.numLevelUpReviews)
        assertEquals(2, timeline.numAvailableReviews)
        assertEquals(2, timeline.numAvailableLessons)
    }

    @Test fun notificationTextOmitsZeroCategoriesAndAbsentPathCountsAndReadsOldContexts() {
        val context = instrumentation.targetContext
        val counts = AlertContext().apply { numReviews = 12; numLevelUpReviews = 3 }
        val text = StudyNotificationText(context, counts)
        assertFalse(text.expanded.contains("lesson"))
        assertTrue(text.expanded.contains("12 reviews available"))
        assertTrue(text.expanded.contains("Level-up path: 3 reviews"))
        counts.numLevelUpReviews = 0
        assertFalse(StudyNotificationText(context, counts).expanded.contains("Level-up"))
        counts.numReviews = 0; counts.numLessons = 5; counts.numLevelUpLessons = 2
        val lessons = StudyNotificationText(context, counts)
        assertEquals("New lessons available", lessons.title)
        assertFalse(lessons.expanded.contains("review"))
        assertFalse(lessons.expanded.contains("0 lessons"))
        val old = Converters.getObjectMapper().readValue("""{"numLessons":0,"numReviews":10,"newestAvailableAt":1,"upcomingAvailableAt":2}""", AlertContext::class.java)
        assertEquals(0, old.numLevelUpLessons); assertEquals(0, old.numLevelUpReviews)
    }

    @Test fun disappearingCategoriesAndPathChangesRefreshEvenWithoutANewReview() {
        val rule = com.smouldering_durtles.wk.services.NotificationWorker::class.java
            .getDeclaredMethod("detailsChanged", AlertContext::class.java, AlertContext::class.java).apply { isAccessible = true }
        val before = AlertContext().apply { numLessons = 5; numReviews = 12 }
        val after = AlertContext().apply { numLessons = 0; numReviews = 12 }
        assertEquals(true, rule.invoke(null, before, after))
        before.numLessons = 0; after.numReviews = 8
        assertEquals(false, rule.invoke(null, before, after)) // ordinary count changes still follow the chosen frequency
        after.numLevelUpReviews = 2
        assertEquals(true, rule.invoke(null, before, after))
        after.numLevelUpReviews = 0; after.numReviews = 0
        assertEquals(true, rule.invoke(null, before, after))
    }

    @Test fun currentBacklogDoesNotFlattenFutureHourlyColorsAndBarsShowExactCounts() {
        ActivityScenario.launch(AboutActivity::class.java).use { scenario -> scenario.onActivity { activity ->
            val context = themed(activity)
            val timeline = TimeLine(168)
            repeat(500) { timeline.addReview(review(1000L + it, timeline.createdAt - 10000), false) }
            for (hour in 1 until 168) repeat(hour % 9) { timeline.addReview(review(2000L + hour * 10 + it,
                timeline.firstSlot + hour * com.smouldering_durtles.wk.Constants.HOUR), false) }
            val chart = TimeLineBarChart(context)
            TimeLineBarChart::class.java.getDeclaredMethod("update", TimeLine::class.java).apply { isAccessible = true }.invoke(chart, timeline)
            measure(chart, 360)
            assertTrue(chart.contentDescription.contains("500 available now"))
            assertEquals(8, TimeLineBarChart::class.java.getDeclaredField("maxUpcomingCount").apply { isAccessible = true }.getInt(chart))
            capture(chart, "dashboard2-forecast-backlog")
            for ((scale, width) in listOf(1f to 360, 2f to 280)) {
                val hours = HourlyReviewChart(themed(activity, scale), arrayOf("1 AM", "2 AM", "3 AM"), intArrayOf(0, 3, 120), 500)
                measure(hours, width)
                val rows = (0 until hours.childCount).map { hours.getChildAt(it) }.filterIsInstance<android.widget.LinearLayout>()
                assertEquals(3, rows.size)
                assertEquals(listOf("0", "3", "120"), rows.map { (it.getChildAt(2) as TextView).text.toString() })
                assertTrue(rows.all { it.getChildAt(1).width > 0 })
                capture(hours, "dashboard2-hourly-$scale")
            }
        } }
    }

    @Test fun muteDisablesExistingAndNewListenButtonsAndUnmuteRestoresThem() {
        ActivityScenario.launch(AboutActivity::class.java).use { scenario -> scenario.onActivity { activity ->
            WkApplication.getDatabase().propertiesDao().isMuted = true
            val info = SubjectInfoView(activity).apply { setMaxFontSize(72); setContainerType(SubjectInfoView.ContainerType.BROWSE) }
            info.setSubject(activity, subject(777, SubjectType.WANIKANI_VOCAB))
            activity.setContentView(info)
            val buttons = descendants(info).filterIsInstance<Button>().filter { it.getTag(R.id.audioAction) == true }
            assertTrue(buttons.isNotEmpty()); assertTrue(buttons.all { !it.isEnabled })
            WkApplication.getDatabase().propertiesDao().isMuted = false
            AudioButtonState.refresh(info)
            assertTrue(buttons.all { it.isEnabled })
            AudioButtonState.refresh(info, true)
            assertTrue(buttons.all { !it.isEnabled })
            measure(info, 360)
            capture(info, "dashboard2-muted-listen")
        } }
    }

    @Test fun compactSrsKeyFitsOneRowAndSplitCountsHaveExactSearchActions() {
        ActivityScenario.launch(AboutActivity::class.java).use { scenario -> scenario.onActivity { activity ->
            var search: AdvancedSearchParameters? = null
            val owner = Proxy.newProxyInstance(Actment::class.java.classLoader, arrayOf(Actment::class.java)) { _, method, args ->
                when(method.name) {
                    "getLifecycle" -> activity.lifecycle
                    "goToSearchResult" -> { search = Converters.getObjectMapper().readValue(args!![1] as String, AdvancedSearchParameters::class.java); null }
                    else -> null
                }
            } as Actment
            val srs = SrsBreakDownView(themed(activity))
            srs.setLifecycleOwner(owner)
            val system = SrsSystem(9912, "Stage fixture", 0, 1, 5, 9).apply { for (i in 0L..9L) addStage(i, i * 3600); finish() }
            val model = SrsBreakDown().apply { for (i in 1..9) addCount(system.getStage(i.toLong()), i) }
            SrsBreakDownView::class.java.getDeclaredMethod("update", SrsBreakDown::class.java).apply { isAccessible = true }.invoke(srs, model)
            measure(srs, 360)
            val key = srs.findViewById<ViewGroup>(R.id.srsLegend)
            assertEquals(5, key.childCount)
            assertTrue((0 until key.childCount).all { key.getChildAt(it).top == key.getChildAt(0).top })
            capture(srs, "dashboard2-srs-grouped")
            srs.findViewById<View>(R.id.srsSplitToggle).performClick()
            measure(srs, 360)
            val split = srs.findViewById<ViewGroup>(R.id.srsSplitLegend)
            assertEquals(9, split.childCount)
            val values = SrsDistributionBarView::class.java.getDeclaredField("values").apply { isAccessible = true }.get(srs.findViewById(R.id.srsDistributionBar)) as IntArray
            assertArrayEquals(IntArray(9) { it + 1 }, values)
            split.getChildAt(3).performClick()
            assertTrue(search!!.srsStages.contains("prepass:3"))
            capture(srs, "dashboard2-srs-split")
        } }
    }

    @Test fun recentItemsKeepAllTenTargetsInCompactGridAndRetainNavigation() {
        ActivityScenario.launch(AboutActivity::class.java).use { scenario -> scenario.onActivity { activity ->
            var clicked = -1L
            val owner = Proxy.newProxyInstance(Actment::class.java.classLoader, arrayOf(Actment::class.java)) { _, method, args ->
                if (method.name == "goToSubjectInfo") clicked = args!![0] as Long
                null
            } as Actment
            val table = LiveRecentUnlocksSubjectTableView(themed(activity))
            val subjects = (1L..10L).map { subject(it, if (it % 2 == 0L) SubjectType.WANIKANI_VOCAB else SubjectType.WANIKANI_KANJI) }
            LiveSubjectTableView::class.java.getDeclaredMethod("update", Actment::class.java, List::class.java)
                .apply { isAccessible = true }.invoke(table, owner, subjects)
            measure(table, 360)
            val grid = table.findViewById<ViewGroup>(R.id.recentSubjectGrid)
            assertEquals(10, grid.childCount)
            assertTrue(grid.height <= 180 * grid.resources.displayMetrics.density)
            assertTrue((0 until 10).all { grid.getChildAt(it).height >= 48 * grid.resources.displayMetrics.density - 1 })
            grid.getChildAt(9).performClick()
            assertEquals(10L, clicked)
            capture(table, "dashboard2-recent-unlocks")
        } }
    }

    private fun review(id: Long, date: Long) = subject(id, SubjectType.WANIKANI_KANJI).apply { availableAt = date }
    private fun subject(id: Long, type: SubjectType, level: Int = 8, passed: Boolean = false, components: String = "[]") = Subject(SubjectEntity().apply {
        this.id = id; this.type = type; this.level = level; characters = if (type.isVocabulary) "日本語" else "一"
        srsStageId = 1; passedAt = if (passed) 1 else 0; componentSubjectIds = components
        meanings = """[{"meaning":"Example","primary":true,"accepted_answer":true}]"""
        readings = """[{"reading":"にほんご","primary":true,"accepted_answer":true}]"""
    })
    private fun themed(context: Context, scale: Float = 1f): Context = ContextThemeWrapper(context.createConfigurationContext(
        Configuration(context.resources.configuration).apply { fontScale = scale }), R.style.SmokyDashboardTheme).apply {
        theme.applyStyle(ActiveTheme.getCurrentTheme().styleId, false)
    }
    private fun measure(view: View, width: Int) {
        view.measure(View.MeasureSpec.makeMeasureSpec((width * view.resources.displayMetrics.density).toInt(), View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
    }
    private fun descendants(view: View): List<View> = listOf(view) + if (view is ViewGroup) (0 until view.childCount).flatMap { descendants(view.getChildAt(it)) } else emptyList()
    private fun capture(view: View, name: String) {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        File(instrumentation.targetContext.filesDir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
