package com.smouldering_durtles.wk.services;

import android.content.Context;
import com.smouldering_durtles.wk.R;
import com.smouldering_durtles.wk.model.AlertContext;
import java.util.ArrayList;
import java.util.List;

/** Zero-count categories are omitted from both compact and expanded notifications. */
public final class StudyNotificationText {
    public final String title, summary, expanded, levelUp;
    public StudyNotificationText(Context context, AlertContext counts) {
        List<String> available = new ArrayList<>();
        if (counts.getNumLessons() > 0) available.add(context.getString(R.string.notification_lessons, counts.getNumLessons()));
        if (counts.getNumReviews() > 0) available.add(context.getString(R.string.notification_reviews, counts.getNumReviews()));
        summary = join(available);
        title = context.getString(counts.getNumLessons() > 0
                ? counts.getNumReviews() > 0 ? R.string.notification_both_title : R.string.notification_lessons_title
                : R.string.notification_reviews_title);
        List<String> path = new ArrayList<>();
        if (counts.getNumLessons() > 0 && counts.getNumLevelUpLessons() > 0)
            path.add(context.getString(R.string.notification_path_lessons, Math.min(counts.getNumLessons(), counts.getNumLevelUpLessons())));
        if (counts.getNumReviews() > 0 && counts.getNumLevelUpReviews() > 0)
            path.add(context.getString(R.string.notification_path_reviews, Math.min(counts.getNumReviews(), counts.getNumLevelUpReviews())));
        levelUp = path.isEmpty() ? "" : context.getString(R.string.notification_level_up, join(path));
        expanded = summary + (levelUp.isEmpty() ? "" : "\n" + levelUp);
    }
    private static String join(List<String> parts) {
        StringBuilder text = new StringBuilder();
        for (String part : parts) { if (text.length() > 0) text.append(" · "); text.append(part); }
        return text.toString();
    }
}
