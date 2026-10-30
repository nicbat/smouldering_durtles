package com.smouldering_durtles.wk.test;

import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.smouldering_durtles.wk.R;
import com.smouldering_durtles.wk.activities.AboutActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/** Exercises the shared toolbar without credentials or changes to a study session. */
@RunWith(AndroidJUnit4.class)
public class SearchExpansionTest {
    @Test
    public void firstExpansionFocusesVisibleInputAndCanReopen() {
        try (ActivityScenario<AboutActivity> scenario = ActivityScenario.launch(AboutActivity.class)) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            for (int attempt = 0; attempt < 2; attempt++) {
                scenario.onActivity(activity -> {
                    MenuItem search = activity.getMenu().findItem(R.id.action_search);
                    assertNotNull(search);
                    assertTrue(search.expandActionView());
                });
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                scenario.onActivity(activity -> {
                    MenuItem search = activity.getMenu().findItem(R.id.action_search);
                    EditText input = findInput(search.getActionView());
                    assertNotNull("Search must contain an input", input);
                    assertTrue("One expansion must reveal the input", input.isShown());
                    assertTrue("One expansion must focus the input", input.hasFocus());
                    input.setText("kanji");
                    assertTrue(search.collapseActionView());
                });
            }
        }
    }

    private static EditText findInput(View view) {
        if (view instanceof EditText) return (EditText) view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                EditText input = findInput(group.getChildAt(i));
                if (input != null) return input;
            }
        }
        return null;
    }
}
