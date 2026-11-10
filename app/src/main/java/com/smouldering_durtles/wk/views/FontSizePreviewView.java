package com.smouldering_durtles.wk.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.material.color.MaterialColors;
import com.smouldering_durtles.wk.GlobalSettings;
import com.smouldering_durtles.wk.db.model.Subject;
import com.smouldering_durtles.wk.db.model.SubjectEntity;
import com.smouldering_durtles.wk.enums.SubjectType;
import com.smouldering_durtles.wk.model.TypefaceConfiguration;
import com.smouldering_durtles.wk.ui.ModernUi;
import com.smouldering_durtles.wk.util.FontStorageUtil;

/** Inline samples use the same glyph fitter and selected typeface as real study questions. */
public final class FontSizePreviewView extends LinearLayout {
    private final SubjectInfoButtonView kanji;
    private final SubjectInfoButtonView vocabulary;
    private final TextView text;

    public FontSizePreviewView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
        int padding = ModernUi.dp(this, 8);
        setPadding(padding, padding, padding, padding);
        setBackground(ModernUi.surface(this, MaterialColors.getColor(this,
                com.google.android.material.R.attr.colorSurfaceContainerLow), 8));
        kanji = sample(context, "漢字", SubjectType.WANIKANI_KANJI);
        vocabulary = sample(context, "日本語を学ぶ", SubjectType.WANIKANI_VOCAB);
        text = new TextView(context);
        text.setTextColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface));
        addView(kanji, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        addView(vocabulary, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        addView(text, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    private SubjectInfoButtonView sample(Context context, String characters, SubjectType type) {
        SubjectEntity entity = new SubjectEntity();
        entity.characters = characters;
        entity.slug = characters;
        entity.type = type;
        SubjectInfoButtonView glyph = new SubjectInfoButtonView(context);
        glyph.setSubject(new Subject(entity));
        glyph.setTransparent(true);
        glyph.setTextShadowEnabled(false);
        glyph.setElevation(0);
        glyph.setLongClickable(false);
        glyph.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        return glyph;
    }

    public static boolean supports(String key) {
        return key != null && (key.startsWith("max_font_size_") || key.startsWith("font_size_"));
    }

    public void update(String key, int value) {
        boolean heading = key.startsWith("max_font_size_");
        kanji.setVisibility(heading && value > 0 ? VISIBLE : GONE);
        vocabulary.setVisibility(heading && value > 0 ? VISIBLE : GONE);
        text.setVisibility(!heading || value == 0 ? VISIBLE : GONE);
        if (heading && value > 0) {
            TypefaceConfiguration face = TypefaceConfiguration.DEFAULT;
            boolean mainQuestion = "max_font_size_quiz_text".equals(key);
            if (mainQuestion) {
                for (String id : GlobalSettings.Font.getSelectedFonts()) {
                    TypefaceConfiguration selected = FontStorageUtil.getTypefaceConfiguration(id);
                    if (selected != null) { face = selected; break; }
                }
            }
            // The adaptive lesson/browse headline caps its glyph at 72sp.
            int effectiveSize = "max_font_size_lesson".equals(key) || "max_font_size_browse".equals(key)
                    ? Math.min(value, 72) : value;
            int maximum = Math.round(effectiveSize * getResources().getDisplayMetrics().scaledDensity);
            for (SubjectInfoButtonView glyph : new SubjectInfoButtonView[] {kanji, vocabulary}) {
                glyph.setTypefaceConfiguration(face);
                glyph.setSizeForQuiz(mainQuestion);
                glyph.setMaxSize(Integer.MAX_VALUE, maximum);
                LayoutParams params = (LayoutParams) glyph.getLayoutParams();
                params.height = mainQuestion ? com.smouldering_durtles.wk.study.StudyStyle.questionHeight(
                        this, GlobalSettings.Display.getQuizQuestionViewHeight()) : LayoutParams.WRAP_CONTENT;
                glyph.setLayoutParams(params);
            }
            setContentDescription("Preview: 漢字, 日本語を学ぶ. Maximum " + value + " sp; long words fit the available width."
                    + (mainQuestion ? " Uses the selected review question height."
                    : effectiveSize < value ? " The compact heading uses up to 72 sp." : ""));
        } else {
            text.setText(value == 0 ? "Hidden" : "font_size_question_edit".equals(key)
                    ? "かんじ / kanji" : "Reading: かんじ\nMeaning: kanji");
            text.setTextSize(value == 0 ? 14 : value);
            setContentDescription(value == 0 ? "Preview: hidden" : "Preview at " + value + " sp");
        }
    }
}
