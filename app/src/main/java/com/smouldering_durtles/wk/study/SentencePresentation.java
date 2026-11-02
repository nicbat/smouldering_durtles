package com.smouldering_durtles.wk.study;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.os.Build;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.smouldering_durtles.wk.R;

/** Tap toggles translation visibility; long press copies the original sentence in either language. */
public final class SentencePresentation {
    private SentencePresentation() { }

    public static void bind(TextView japanese, TextView english, String original, String translation, boolean hide) {
        if (original == null) original = "";
        if (translation == null) translation = "";
        copyOnHold(japanese, original);
        copyOnHold(english, translation);
        Object previous = english.getTag(R.id.sentenceTranslation);
        if (!translation.equals(previous)) english.setTag(R.id.sentenceRevealed, false);
        english.setTag(R.id.sentenceTranslation, translation);
        english.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        english.setOnClickListener(hide ? v -> {
            english.setTag(R.id.sentenceRevealed, !Boolean.TRUE.equals(english.getTag(R.id.sentenceRevealed)));
            visibility(english, true);
        } : null);
        english.setClickable(hide);
        english.setFocusable(true);
        visibility(english, hide);
    }

    private static void visibility(TextView view, boolean hide) {
        boolean blurred = hide && !Boolean.TRUE.equals(view.getTag(R.id.sentenceRevealed));
        view.getPaint().setMaskFilter(blurred ? new BlurMaskFilter(view.getTextSize() / 3,
                BlurMaskFilter.Blur.NORMAL) : null);
        view.setContentDescription(blurred ? view.getContext().getString(R.string.sentence_hidden) : null);
        ViewCompat.replaceAccessibilityAction(view, AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK,
                view.getContext().getString(blurred ? R.string.sentence_reveal : R.string.sentence_hide),
                hide ? (v, args) -> { v.performClick(); return true; } : null);
        view.invalidate();
    }

    private static void copyOnHold(TextView view, String text) {
        view.setOnLongClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) v.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard == null) return false;
            clipboard.setPrimaryClip(ClipData.newPlainText(v.getContext().getString(R.string.sentence_clip_label), text));
            if (Build.VERSION.SDK_INT < 33) Toast.makeText(v.getContext(), R.string.sentence_copied, Toast.LENGTH_SHORT).show();
            return true;
        });
        ViewCompat.replaceAccessibilityAction(view, AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_LONG_CLICK,
                view.getContext().getString(R.string.sentence_copy), (v, args) -> v.performLongClick());
    }
}
