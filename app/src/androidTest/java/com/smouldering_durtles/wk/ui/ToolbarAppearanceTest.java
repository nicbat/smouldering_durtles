package com.smouldering_durtles.wk.ui;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.SharedPreferences;
import android.view.View;
import androidx.preference.PreferenceManager;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.smouldering_durtles.wk.R;
import com.smouldering_durtles.wk.activities.AboutActivity;
import com.smouldering_durtles.wk.activities.MainActivity;
import com.smouldering_durtles.wk.activities.NoApiKeyHelpActivity;
import com.smouldering_durtles.wk.dashboard.DashboardStyle;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.Map;
import static androidx.test.espresso.Espresso.*;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static androidx.test.espresso.matcher.RootMatchers.isPlatformPopup;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ToolbarAppearanceTest {
    @Test public void overflowStillNavigatesToSettingsAndBack() {
        try (ActivityScenario<AboutActivity> scenario = ActivityScenario.launch(AboutActivity.class)) {
            openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().getTargetContext());
            onView(withText("Settings")).inRoot(isPlatformPopup()).perform(click());
            onView(withText("Display settings")).check(matches(isDisplayed()));
            pressBack();
            scenario.onActivity(activity -> assertFalse(activity.isFinishing()));
        }
    }

    @Test public void realDashboardSwitchesThemeAndChartLayoutsOnRecreation() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(instrumentation.getTargetContext());
        Map<String, ?> before = prefs.getAll();
        prefs.edit().putBoolean("modern_dashboard", true).putBoolean("dashboard_dynamic_color", false).putBoolean("notification_permission_asked", true).commit();
        // Keep the no-token redirect on the test host. Never supply a fake or real API token.
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(NoApiKeyHelpActivity.class.getName(),
                new Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null), true);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                assertTrue(DashboardStyle.isModern(activity));
                assertNotNull(activity.findViewById(R.id.stageSwatch0));
                assertEquals(View.VISIBLE, activity.findViewById(R.id.modernDashboard).getVisibility());
                assertEquals(R.style.SmokyOverflowTheme, activity.getToolbar().getPopupTheme());
            });
            prefs.edit().putBoolean("modern_dashboard", false).commit();
            scenario.recreate();
            scenario.onActivity(activity -> {
                assertFalse(DashboardStyle.isModern(activity));
                assertNull(activity.findViewById(R.id.stageSwatch0));
                assertEquals(View.GONE, activity.findViewById(R.id.modernDashboard).getVisibility());
                assertEquals(View.VISIBLE, activity.findViewById(R.id.classicDashboardHeader).getVisibility());
            });
        } finally {
            instrumentation.removeMonitor(monitor);
            SharedPreferences.Editor edit = prefs.edit();
            for (String key : new String[] {"modern_dashboard", "dashboard_dynamic_color", "notification_permission_asked"}) {
                if (before.containsKey(key)) edit.putBoolean(key, (Boolean) before.get(key));
                else edit.remove(key);
            }
            edit.commit();
        }
    }
}
