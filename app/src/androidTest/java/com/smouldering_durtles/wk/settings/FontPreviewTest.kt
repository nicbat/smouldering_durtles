package com.smouldering_durtles.wk.settings

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.LayoutInflater
import android.view.View
import android.view.KeyEvent
import android.widget.LinearLayout
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceViewHolder
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.material.slider.Slider
import com.smouldering_durtles.wk.R
import com.smouldering_durtles.wk.activities.AboutActivity
import com.smouldering_durtles.wk.activities.FontSelectionActivity
import com.smouldering_durtles.wk.components.MaterialSliderPreference
import com.smouldering_durtles.wk.util.FontStorageUtil
import com.smouldering_durtles.wk.views.FontSizePreviewView
import com.smouldering_durtles.wk.views.SubjectInfoButtonView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class FontPreviewTest {
    @Test fun fontSliderShowsRealSizingAndUpdatesWithKeyboard() {
        ActivityScenario.launch(AboutActivity::class.java).use { scenario -> scenario.onActivity { activity ->
            val preference = MaterialSliderPreference(activity, null).apply {
                key = "max_font_size_quiz_text"; title = "Main review question"; isPersistent = false
                min = 0; max = 500; value = 100
            }
            val row = LayoutInflater.from(activity).inflate(preference.layoutResource, null)
            preference.onBindViewHolder(PreferenceViewHolder.createInstanceForTests(row))
            val preview = row.findViewById<FontSizePreviewView>(R.id.fontSizePreview)
            assertEquals(View.VISIBLE, preview.visibility)
            assertEquals(2, (0 until preview.childCount).count { preview.getChildAt(it) is SubjectInfoButtonView })
            val slider = row.findViewById<Slider>(R.id.materialPreferenceSlider)
            slider.onKeyDown(KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT))
            slider.onKeyUp(KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_RIGHT))
            val updatedSize = preference.value
            assertTrue(updatedSize > 100)
            assertEquals(updatedSize.toFloat(), slider.value, 0f)
            assertTrue(preview.contentDescription.toString().contains("$updatedSize sp"))
            preference.onPreferenceChangeListener = androidx.preference.Preference.OnPreferenceChangeListener { _, _ -> false }
            slider.onKeyDown(KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT))
            slider.onKeyUp(KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_RIGHT))
            assertEquals(updatedSize, preference.value)
            assertTrue(preview.contentDescription.toString().contains("$updatedSize sp"))
            activity.setContentView(row)
            capture(activity, row, "font-settings-live-preview")
            preview.update("max_font_size_quiz_text", 0)
            assertEquals("Preview: hidden", preview.contentDescription)
            preview.update("font_size_question_edit", 24)
            assertTrue(preview.contentDescription.toString().contains("24 sp"))
        } }
    }

    @Test fun extraBuiltInOptionsLoadAndSelectionStaysIndependent() {
        for (id in listOf("9", "10", "11", "12", "13", "14")) {
            assertNotNull("Font $id must load without downloads", FontStorageUtil.getTypefaceConfiguration(id))
        }
        assertTrue(FontStorageUtil.getTypefaceConfiguration("11")!!.typeface.isBold)
        assertTrue(FontStorageUtil.getTypefaceConfiguration("12")!!.typeface.isBold)
        val prefs = PreferenceManager.getDefaultSharedPreferences(InstrumentationRegistry.getInstrumentation().targetContext)
        val before = prefs.getString("selected_fonts", null)
        try {
            prefs.edit().putString("selected_fonts", "[\"1\",\"11\"]").commit()
            ActivityScenario.launch(FontSelectionActivity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    val list = activity.findViewById<LinearLayout>(R.id.fontList)
                    assertTrue(list.childCount >= 15)
                    capture(activity, activity.window.decorView, "font-selection-dense")
                }
                scenario.recreate()
                assertEquals("[\"1\",\"11\"]", prefs.getString("selected_fonts", null))
            }
        } finally {
            if (before == null) prefs.edit().remove("selected_fonts").commit()
            else prefs.edit().putString("selected_fonts", before).commit()
        }
    }

    private fun capture(context: android.content.Context, view: View, name: String) {
        val width = (360 * context.resources.displayMetrics.density).toInt()
        view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        view.layout(0, 0, width, view.measuredHeight.coerceAtLeast(1))
        val bitmap = Bitmap.createBitmap(width, view.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        File(context.filesDir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
