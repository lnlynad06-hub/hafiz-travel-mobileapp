package com.hafiztraveltours.app.ui;

import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.models.*;
import com.hafiztraveltours.app.adapters.*;
import com.hafiztraveltours.app.network.*;
import com.hafiztraveltours.app.services.*;
import com.hafiztraveltours.app.utils.*;
import com.hafiztraveltours.app.views.*;
import com.hafiztraveltours.app.ui.*;


import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * "Panduan Umrah" - standalone page reached from featureGuideline on
 * MainActivity. Content is split into Bab (chapters) via tabs at the top,
 * since each Bab has its own separate paid PDF download.
 *
 * Text is now sourced from string resources (per-language strings.xml) so it
 * can be translated - only the Arabic du'a/niat wording stays IDENTICAL
 * across every language, since translating recitation text is not done here.
 *
 * Text sizes bumped up throughout for readability by older jemaah. Arabic
 * du'a/niat lines are additionally enlarged + bolded automatically (see
 * buildBodyWithArabicEmphasis) since they sit on their own line in every
 * string resource - no need to hand-edit each strings.xml per language.
 *
 * UPDATED: layout is now wrapped in a FrameLayout with a floating
 * "Checklist Umrah" button (umrahChecklistFab) pinned bottom-end, on top of
 * the scrolling content. This class now wires that button up.
 */
public class PanduanUmrahActivity extends AppCompatActivity {

    // How much bigger the Arabic du'a lines are vs the surrounding body text (17sp)
    private static final float ARABIC_TEXT_SIZE_SP = 30f;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applySavedLocale(newBase));
    }

    private static class SectionItem {
        int titleResId;
        int bodyResId;
        SectionItem(int titleResId, int bodyResId) {
            this.titleResId = titleResId;
            this.bodyResId = bodyResId;
        }
    }

    /** Adds extra vertical room around an Arabic line so harakat (tanda baris atas/bawah
     *  seperti fatha, kasra, damma, sukun) tidak terpotong bila font besar. */
    private static class ArabicLineHeightSpan implements android.text.style.LineHeightSpan {
        private final int extraPx;
        ArabicLineHeightSpan(int extraPx) { this.extraPx = extraPx; }

        @Override
        public void chooseHeight(CharSequence text, int start, int end, int spanstartv,
                                 int lineHeight, android.graphics.Paint.FontMetricsInt fm) {
            fm.ascent -= extraPx;
            fm.top -= extraPx;
            fm.descent += extraPx;
            fm.bottom += extraPx;
        }
    }

    private static class Chapter {
        String tabLabel;       // "Bab 1" - kept as plain text, matches numbering everywhere
        int heroTitleResId;
        int heroBlurbResId;
        boolean available;
        List<SectionItem> sections;

        Chapter(String tabLabel, int heroTitleResId, int heroBlurbResId, boolean available) {
            this.tabLabel = tabLabel;
            this.heroTitleResId = heroTitleResId;
            this.heroBlurbResId = heroBlurbResId;
            this.available = available;
            this.sections = new ArrayList<>();
        }
    }

    private List<Chapter> chapters = new ArrayList<>();
    private List<TextView> tabViews = new ArrayList<>();
    private int selectedChapterIndex = 0;

    private LinearLayout chapterTabRow;
    private TextView chapterHeroTitle, chapterHeroBlurb;
    private LinearLayout guidelineSectionsContainer;
    private LinearLayout downloadPdfButton;
    private TextView downloadPdfTitle, downloadPdfSubtitle;
    private View checklistBannerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_panduan_umrah);

        findViewById(R.id.panduanBackButton).setOnClickListener(v -> finish());

        chapterTabRow = findViewById(R.id.chapterTabRow);
        chapterHeroTitle = findViewById(R.id.chapterHeroTitle);
        chapterHeroBlurb = findViewById(R.id.chapterHeroBlurb);
        guidelineSectionsContainer = findViewById(R.id.guidelineSectionsContainer);
        downloadPdfButton = findViewById(R.id.downloadPdfButton);
        downloadPdfTitle = findViewById(R.id.downloadPdfTitle);
        downloadPdfSubtitle = findViewById(R.id.downloadPdfSubtitle);
        checklistBannerButton = findViewById(R.id.checklistBannerButton);
        checklistBannerButton.setOnClickListener(v -> openChecklistUmrah());
        findViewById(R.id.checklistBannerCard).setOnClickListener(v -> openChecklistUmrah());

        buildChapters();
        buildTabRow();
        selectChapter(0);
    }

    /** Handles tap on the floating "Checklist Umrah" button - opens the checklist screen. */
    private void openChecklistUmrah() {
        startActivity(new Intent(this, ChecklistUmrahActivity.class));
    }

    /**
     * Buat umrahChecklistFab boleh drag ke mana-mana dalam skrin (macam chat
     * head). Guna raw touch coordinates supaya movement betul walaupun view
     * ada translation sedia ada. Tap (movement kecil) tetap trigger click
     * biasa via performClick() - tak duplicate logik buka checklist.
     */

    private void buildChapters() {
        // ---- Bab 1: Adab Musafir dan Amalan Sunat ----
        Chapter bab1 = new Chapter("Bab 1", R.string.bab1_hero_title, R.string.bab1_hero_blurb, true);
        bab1.sections.add(new SectionItem(R.string.bab1_s1_title, R.string.bab1_s1_body));
        bab1.sections.add(new SectionItem(R.string.bab1_s2_title, R.string.bab1_s2_body));
        bab1.sections.add(new SectionItem(R.string.bab1_s3_title, R.string.bab1_s3_body));
        bab1.sections.add(new SectionItem(R.string.bab1_s4_title, R.string.bab1_s4_body));
        chapters.add(bab1);

        // ---- Bab 2 ----
        Chapter bab2 = new Chapter("Bab 2", R.string.bab2_hero_title, R.string.bab2_hero_blurb, true);
        bab2.sections.add(new SectionItem(R.string.bab2_s1_title, R.string.bab2_s1_body));
        chapters.add(bab2);

        // ---- Bab 3: Pelaksanaan Ibadah Umrah ----
        Chapter bab3 = new Chapter("Bab 3", R.string.bab3_hero_title, R.string.bab3_hero_blurb, true);
        bab3.sections.add(new SectionItem(R.string.bab3_s1_title, R.string.bab3_s1_body));
        bab3.sections.add(new SectionItem(R.string.bab3_s2_title, R.string.bab3_s2_body));
        bab3.sections.add(new SectionItem(R.string.bab3_s3_title, R.string.bab3_s3_body));
        bab3.sections.add(new SectionItem(R.string.bab3_s4_title, R.string.bab3_s4_body));
        chapters.add(bab3);

        // ---- Bab 4 ----
        Chapter bab4 = new Chapter("Bab 4", R.string.bab4_hero_title, R.string.bab4_hero_blurb, true);
        bab4.sections.add(new SectionItem(R.string.bab4_s1_title, R.string.bab4_s1_body));
        bab4.sections.add(new SectionItem(R.string.bab4_s2_title, R.string.bab4_s2_body));
        bab4.sections.add(new SectionItem(R.string.bab4_s3_title, R.string.bab4_s3_body));
        bab4.sections.add(new SectionItem(R.string.bab4_s4_title, R.string.bab4_s4_body));
        chapters.add(bab4);

        // ---- Bab 5 ----
        Chapter bab5 = new Chapter("Bab 5", R.string.bab5_hero_title, R.string.bab5_hero_blurb, true);
        bab5.sections.add(new SectionItem(R.string.bab5_s1_title, R.string.bab5_s1_body));
        bab5.sections.add(new SectionItem(R.string.bab5_s2_title, R.string.bab5_s2_body));
        bab5.sections.add(new SectionItem(R.string.bab5_s3_title, R.string.bab5_s3_body));
        bab5.sections.add(new SectionItem(R.string.bab5_s4_title, R.string.bab5_s4_body));
        bab5.sections.add(new SectionItem(R.string.bab5_s5_title, R.string.bab5_s5_body));
        chapters.add(bab5);

        // ---- Bab 6 ----
        Chapter bab6 = new Chapter("Bab 6", R.string.bab6_hero_title, R.string.bab6_hero_blurb, true);
        bab6.sections.add(new SectionItem(R.string.bab6_s1_title, R.string.bab6_s1_body));
        bab6.sections.add(new SectionItem(R.string.bab6_s2_title, R.string.bab6_s2_body));
        chapters.add(bab6);
    }

    private void buildTabRow() {
        chapterTabRow.removeAllViews();
        tabViews.clear();

        for (int i = 0; i < chapters.size(); i++) {
            Chapter chapter = chapters.get(i);
            final int index = i;

            TextView tab = new TextView(this);
            tab.setText(chapter.tabLabel);
            tab.setTextSize(16); // was 13
            tab.setTypeface(null, Typeface.BOLD);
            tab.setPadding(dp(20), dp(11), dp(20), dp(11));
            tab.setClickable(true);
            tab.setFocusable(true);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMarginEnd(dp(8));
            tab.setLayoutParams(params);

            tab.setOnClickListener(v -> {
                if (chapter.available) {
                    selectChapter(index);
                } else {
                    Toast.makeText(this, chapter.tabLabel + " akan datang", Toast.LENGTH_SHORT).show();
                }
            });

            chapterTabRow.addView(tab);
            tabViews.add(tab);
        }
    }

    private void selectChapter(int index) {
        selectedChapterIndex = index;
        Chapter chapter = chapters.get(index);

        // Update tab styling (active vs inactive vs unavailable)
        for (int i = 0; i < tabViews.size(); i++) {
            TextView tab = tabViews.get(i);
            Chapter c = chapters.get(i);
            if (!c.available) {
                tab.setBackgroundResource(R.drawable.circle_bg_light);
                tab.setTextColor(getResources().getColor(R.color.field_border));
            } else if (i == selectedChapterIndex) {
                tab.setBackgroundResource(R.drawable.bg_button_pink);
                tab.setTextColor(getResources().getColor(R.color.white));
            } else {
                tab.setBackgroundResource(R.drawable.circle_bg_light);
                tab.setTextColor(getResources().getColor(R.color.pink_dark));
            }
        }

        if (!chapter.available) return;

        chapterHeroTitle.setText(getString(chapter.heroTitleResId));
        chapterHeroBlurb.setText(getString(chapter.heroBlurbResId));

        guidelineSectionsContainer.removeAllViews();
        for (SectionItem section : chapter.sections) {
            guidelineSectionsContainer.addView(buildSectionItem(section));
        }

        downloadPdfTitle.setText(getString(R.string.guideline_pdf_title));
        downloadPdfSubtitle.setText(chapter.tabLabel + " - " + getString(R.string.guideline_pdf_subtitle));
        downloadPdfButton.setOnClickListener(v ->
                Toast.makeText(this, "Muat turun PDF " + chapter.tabLabel + " - akan datang", Toast.LENGTH_SHORT).show());
    }

    private View buildSectionItem(SectionItem section) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setBackgroundResource(R.drawable.bg_search_white);
        LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        itemParams.bottomMargin = dp(12);
        item.setLayoutParams(itemParams);
        int padding = dp(16);
        item.setPadding(padding, padding, padding, padding);
        item.setClickable(true);
        item.setFocusable(true);

        TextView title = new TextView(this);
        title.setText(getString(section.titleResId));
        title.setTextSize(19); // was 15 - bigger for elderly jemaah
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(getResources().getColor(R.color.text_dark));

        TextView summary = new TextView(this);
        // FIX: Arabic du'a/niat lines are now auto-detected and enlarged (24sp,
        // bold) while the surrounding explanation text stays at 17sp.
        summary.setText(buildBodyWithArabicEmphasis(getString(section.bodyResId)));
        summary.setTextSize(17); // base size - was 13, bigger for elderly jemaah
        summary.setTextColor(getResources().getColor(R.color.text_gray));
        summary.setLineSpacing(dp(4), 1.15f); // slightly more line spacing for readability
        summary.setVisibility(View.GONE);
        LinearLayout.LayoutParams summaryParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        summaryParams.topMargin = dp(10);
        summary.setLayoutParams(summaryParams);

        item.addView(title);
        item.addView(summary);

        item.setOnClickListener(v ->
                summary.setVisibility(summary.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));

        return item;
    }

    /**
     * Scans the body text line by line (strings.xml already puts each Arabic
     * du'a/niat on its own \n-separated line) and enlarges + bolds any line
     * that consists entirely of Arabic-script letters, leaving every other
     * line (the explanation text, in whichever language is active) at the
     * TextView's normal size. Works automatically for every language file
     * since the detection is by Unicode script, not by hardcoded position.
     */
    private CharSequence buildBodyWithArabicEmphasis(String text) {
        int arabicPx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, ARABIC_TEXT_SIZE_SP, getResources().getDisplayMetrics());
        int extraLineRoom = dp(6); // ruang ekstra atas/bawah untuk harakat

        String[] lines = text.split("\n", -1);
        SpannableStringBuilder builder = new SpannableStringBuilder();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int start = builder.length();
            builder.append(line);

            if (isArabicLine(line)) {
                builder.setSpan(new AbsoluteSizeSpan(arabicPx), start, builder.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.setSpan(new StyleSpan(Typeface.BOLD), start, builder.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.setSpan(new ArabicLineHeightSpan(extraLineRoom), start, builder.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            if (i < lines.length - 1) {
                builder.append("\n");
            }
        }
        return builder;
    }

    /** True only if every letter on this line is Arabic script (diacritics/spaces ignored). */
    private boolean isArabicLine(String line) {
        String trimmed = line.trim();
        if (trimmed.isEmpty()) return false;

        int letterCount = 0;
        int arabicCount = 0;
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (Character.isLetter(c)) {
                letterCount++;
                if ((c >= 0x0600 && c <= 0x06FF)   // Arabic
                        || (c >= 0x0750 && c <= 0x077F) // Arabic Supplement
                        || (c >= 0xFB50 && c <= 0xFDFF) // Arabic Presentation Forms-A
                        || (c >= 0xFE70 && c <= 0xFEFF)) { // Arabic Presentation Forms-B
                    arabicCount++;
                }
            }
        }
        return letterCount > 0 && arabicCount == letterCount;
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}
