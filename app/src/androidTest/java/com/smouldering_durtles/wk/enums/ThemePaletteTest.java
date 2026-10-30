package com.smouldering_durtles.wk.enums;

import com.smouldering_durtles.wk.GlobalSettings;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.List;
import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class ThemePaletteTest {
    @Test public void guruOverridesUseTheirOwnSlotsInEveryTheme() {
        for (ActiveTheme theme : ActiveTheme.values()) {
            List<Integer> original = GlobalSettings.Display.getThemeCustomizations(theme);
            int[] restored = original.stream().mapToInt(Integer::intValue).toArray();
            int[] colors = restored.clone();
            colors[14] = 0xFF112233;
            colors[15] = 0xFF445566;
            colors[16] = 0xFF778899;
            try {
                GlobalSettings.Display.setThemeCustomizations(theme, colors);
                theme.loadCustomizations();
                assertEquals(theme.name(), colors[14], theme.stageDeepBucketColors[5]);
                assertEquals(theme.name(), colors[15], theme.stageDeepBucketColors[6]);
                assertEquals(theme.name(), colors[16], theme.stageDeepBucketColors[7]);
            } finally {
                GlobalSettings.Display.setThemeCustomizations(theme, restored);
                theme.loadCustomizations();
            }
        }
    }
}
