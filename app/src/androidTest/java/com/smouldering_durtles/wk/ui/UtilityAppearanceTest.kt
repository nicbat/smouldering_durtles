package com.smouldering_durtles.wk.ui

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
import com.smouldering_durtles.wk.R
import com.smouldering_durtles.wk.activities.AboutActivity
import com.smouldering_durtles.wk.db.model.AudioDownloadStatus
import com.smouldering_durtles.wk.views.DownloadAudioBracketView
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import java.io.File

/** Presentation-only checks: never invoke download, import/export, burn or restore actions. */
@RunWith(AndroidJUnit4::class)
class UtilityAppearanceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val prefs = PreferenceManager.getDefaultSharedPreferences(instrumentation.targetContext)
    private val keys = listOf("modern_settings", "settings_dynamic_color")
    private lateinit var previous: Map<String, Any?>
    @Before fun setup() {
        previous = keys.associateWith { prefs.all[it] }
        prefs.edit().putBoolean("modern_settings", true).putBoolean("settings_dynamic_color", false).commit()
    }
    @After fun restore() {
        val edit = prefs.edit()
        previous.forEach { (key, value) -> if (value == null) edit.remove(key) else edit.putBoolean(key, value as Boolean) }
        edit.commit()
    }

    @Test fun utilityLayoutsInflateInDarkLargeTextWithoutMissingControls() {
        val layouts = listOf(R.layout.activity_no_api_key_help, R.layout.activity_data_import_export,
            R.layout.activity_download_audio_modern, R.layout.activity_burn, R.layout.activity_resurrect,
            R.layout.activity_keyboard_help, R.layout.activity_digraph_help, R.layout.activity_font_import,
            R.layout.activity_font_selection, R.layout.activity_theme_customization_modern)
        ActivityScenario.launch(AboutActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val config = Configuration(activity.resources.configuration).apply { fontScale = 2f; uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or Configuration.UI_MODE_NIGHT_YES }
                val context = ContextThemeWrapper(activity.createConfigurationContext(config), R.style.SmokyUiTheme)
                val width = (360 * activity.resources.displayMetrics.density).toInt()
                val height = (800 * activity.resources.displayMetrics.density).toInt()
                layouts.forEach { layout ->
                    val root = LayoutInflater.from(activity).cloneInContext(context).inflate(layout, null)
                    ModernUi.style(root)
                    root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
                    root.layout(0, 0, root.measuredWidth, root.measuredHeight)
                    assertNotNull("Utility screen must retain toolbar", root.findViewById<View>(R.id.toolbar))
                    assertEquals(width, root.width)
                    if (layout == R.layout.activity_download_audio_modern) {
                        assertFalse(root.findViewById<View>(R.id.abortMoveButton).isEnabled)
                        assertTrue(root.findViewById<Button>(R.id.moveButton).minHeight >= (48 * context.resources.displayMetrics.density).toInt())
                        val actions = root.findViewById<View>(R.id.moveButton).parent as android.widget.LinearLayout
                        val caption = (actions.parent as android.view.ViewGroup).getChildAt(0)
                        assertTrue("Caption should use the full row width", caption.width >= actions.width)
                        assertTrue("Actions should sit below their caption", actions.top >= caption.bottom)
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        root.draw(Canvas(bitmap))
                        File(activity.filesDir, "utility-download-dark-large.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                        bitmap.recycle()
                    }
                }
            }
        }
    }

    @Test fun syntheticDownloadBracketKeepsCountsAndFinishedState() {
        ActivityScenario.launch(AboutActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val row = DownloadAudioBracketView(activity)
                val pending = AudioDownloadStatus().apply { level = 4; numTotal = 12; numNoAudio = 2; numMissingAudio = 3; numPartialAudio = 2; numFullAudio = 5 }
                val outside = AudioDownloadStatus().apply { level = 20; numTotal = 99; numMissingAudio = 99 }
                row.setBracket(listOf(pending, outside), 1, 5)
                ModernUi.style(row)
                assertEquals("Levels 1-5", row.findViewById<TextView>(R.id.rangeLabel).text.toString())
                assertEquals("7/10 available, 2 partially done", row.findViewById<TextView>(R.id.label).text.toString())
                pending.numMissingAudio = 0; pending.numPartialAudio = 0; pending.numFullAudio = 10
                row.setBracket(listOf(pending), 1, 5)
                assertEquals("Download finished", row.findViewById<TextView>(R.id.label).text.toString())
                assertFalse(row.findViewById<View>(R.id.downloadButton).isEnabled)
            }
        }
    }

    @Test fun actualUtilityActivityCanRoundTripClassic() {
        ActivityScenario.launch(AboutActivity::class.java).use { scenario ->
            scenario.onActivity { assertTrue(ModernUi.isModern(it)); assertNotNull(it.findViewById<View>(R.id.document)) }
            prefs.edit().putBoolean("modern_settings", false).commit()
            scenario.recreate()
            scenario.onActivity { assertFalse(ModernUi.isModern(it)); assertNotNull(it.findViewById<View>(R.id.document)) }
            prefs.edit().putBoolean("modern_settings", true).commit()
            scenario.recreate()
            scenario.onActivity { assertTrue(ModernUi.isModern(it)); assertNotNull(it.findViewById<View>(R.id.document)) }
        }
    }
}
