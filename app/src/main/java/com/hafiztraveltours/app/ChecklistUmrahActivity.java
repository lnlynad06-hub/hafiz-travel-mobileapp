package com.hafiztraveltours.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextPaint;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * "Checklist Umrah" - reached from the floating button on PanduanUmrahActivity.
 *
 * Shows the full Umrah checklist (Sebelum berangkat -> Tahallul -> Checklist
 * akhir) as tappable checkboxes grouped by section. Tick state is saved to
 * SharedPreferences so jemaah's progress survives closing the app.
 *
 * All text is sourced from string resources (see checklist_secN_* entries in
 * strings.xml) so the checklist can be translated per language, exactly like
 * PanduanUmrahActivity's Bab content. The two checklist_secN_arabicM lines
 * per section are the exception - keep those byte-for-byte identical across
 * every language file, since they are Quranic/du'a recitation text.
 *
 * Persistence keys use the resource entry name (e.g. "checklist_sec1_item1"),
 * so a jemaah's ticked progress stays stable even if they switch app language
 * mid-way.
 */
public class ChecklistUmrahActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "checklist_umrah_prefs";
    private static final float ARABIC_TEXT_SIZE_SP = 22f;

    /** One line inside a section. */
    private enum BlockType { ITEM, INFO, HEADING, ARABIC, NOTE }

    private static class Block {
        BlockType type;
        int textResId;
        int[] subResIds; // extra indented, non-checkable detail lines under an ITEM
        String prefsKey;  // stable persistence key, only set for ITEM

        Block(BlockType type, int textResId) {
            this.type = type;
            this.textResId = textResId;
        }
    }

    private static class Section {
        int titleResId;
        List<Block> blocks = new ArrayList<>();
        Section(int titleResId) { this.titleResId = titleResId; }
    }

    private LinearLayout checklistContainer;
    private TextView progressText;
    private SharedPreferences prefs;

    private int totalItems = 0;
    private int checkedCount = 0;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applySavedLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checklist_umrah);

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        findViewById(R.id.checklistBackButton).setOnClickListener(v -> finish());
        findViewById(R.id.checklistResetButton).setOnClickListener(v -> resetChecklist());

        checklistContainer = findViewById(R.id.checklistContainer);
        progressText = findViewById(R.id.checklistProgressText);

        List<Section> sections = buildSections();
        renderSections(sections);
        updateProgressText();
    }

    private void resetChecklist() {
        prefs.edit().clear().apply();
        checklistContainer.removeAllViews();
        totalItems = 0;
        checkedCount = 0;
        renderSections(buildSections());
        updateProgressText();
    }

    private void updateProgressText() {
        progressText.setText(checkedCount + " / " + totalItems + " " + getString(R.string.checklist_progress_suffix));
    }

    // ---------------------------------------------------------------------
    // Data
    // ---------------------------------------------------------------------

    private List<Section> buildSections() {
        List<Section> sections = new ArrayList<>();

        Section s1 = new Section(R.string.checklist_sec1_title);
        item(s1, R.string.checklist_sec1_item1);
        item(s1, R.string.checklist_sec1_item2);
        item(s1, R.string.checklist_sec1_item3, R.string.checklist_sec1_item3_sub1, R.string.checklist_sec1_item3_sub2);
        item(s1, R.string.checklist_sec1_item4);
        item(s1, R.string.checklist_sec1_item5);
        item(s1, R.string.checklist_sec1_item6);
        sections.add(s1);

        Section s2 = new Section(R.string.checklist_sec2_title);
        item(s2, R.string.checklist_sec2_item1);
        heading(s2, R.string.checklist_sec2_heading1);
        arabic(s2, R.string.checklist_sec2_arabic1);
        item(s2, R.string.checklist_sec2_item2);
        heading(s2, R.string.checklist_sec2_heading2);
        arabic(s2, R.string.checklist_sec2_arabic2);
        item(s2, R.string.checklist_sec2_item3);
        sections.add(s2);

        Section s3 = new Section(R.string.checklist_sec3_title);
        item(s3, R.string.checklist_sec3_item1);
        item(s3, R.string.checklist_sec3_item2);
        item(s3, R.string.checklist_sec3_item3);
        item(s3, R.string.checklist_sec3_item4);
        item(s3, R.string.checklist_sec3_item5);
        sections.add(s3);

        Section s4 = new Section(R.string.checklist_sec4_title);
        item(s4, R.string.checklist_sec4_item1);
        item(s4, R.string.checklist_sec4_item2);
        item(s4, R.string.checklist_sec4_item3);
        item(s4, R.string.checklist_sec4_item4);
        item(s4, R.string.checklist_sec4_item5);
        sections.add(s4);

        Section s5 = new Section(R.string.checklist_sec5_title);
        heading(s5, R.string.checklist_sec5_heading1);
        item(s5, R.string.checklist_sec5_item1);
        item(s5, R.string.checklist_sec5_item2);
        item(s5, R.string.checklist_sec5_item3);
        item(s5, R.string.checklist_sec5_item4);
        item(s5, R.string.checklist_sec5_item5);
        item(s5, R.string.checklist_sec5_item6);
        item(s5, R.string.checklist_sec5_item7);
        heading(s5, R.string.checklist_sec5_heading2);
        info(s5, R.string.checklist_sec5_info1);
        info(s5, R.string.checklist_sec5_info2);
        info(s5, R.string.checklist_sec5_info3);
        info(s5, R.string.checklist_sec5_info4);
        info(s5, R.string.checklist_sec5_info5);
        heading(s5, R.string.checklist_sec5_heading3);
        info(s5, R.string.checklist_sec5_info6);
        info(s5, R.string.checklist_sec5_info7);
        info(s5, R.string.checklist_sec5_info8);
        info(s5, R.string.checklist_sec5_info9);
        note(s5, R.string.checklist_sec5_note1);
        sections.add(s5);

        Section s6 = new Section(R.string.checklist_sec6_title);
        item(s6, R.string.checklist_sec6_item1);
        item(s6, R.string.checklist_sec6_item2);
        item(s6, R.string.checklist_sec6_item3);
        item(s6, R.string.checklist_sec6_item4);
        item(s6, R.string.checklist_sec6_item5);
        item(s6, R.string.checklist_sec6_item6);
        sections.add(s6);

        Section s7 = new Section(R.string.checklist_sec7_title);
        note(s7, R.string.checklist_sec7_note1);
        item(s7, R.string.checklist_sec7_item1);
        item(s7, R.string.checklist_sec7_item2);
        item(s7, R.string.checklist_sec7_item3);
        item(s7, R.string.checklist_sec7_item4);
        item(s7, R.string.checklist_sec7_item5);
        item(s7, R.string.checklist_sec7_item6);
        item(s7, R.string.checklist_sec7_item7);
        item(s7, R.string.checklist_sec7_item8);
        item(s7, R.string.checklist_sec7_item9);
        heading(s7, R.string.checklist_sec7_heading1);
        info(s7, R.string.checklist_sec7_info1);
        info(s7, R.string.checklist_sec7_info2);
        info(s7, R.string.checklist_sec7_info3);
        info(s7, R.string.checklist_sec7_info4);
        info(s7, R.string.checklist_sec7_info5);
        info(s7, R.string.checklist_sec7_info6);
        info(s7, R.string.checklist_sec7_info7);
        sections.add(s7);

        Section s8 = new Section(R.string.checklist_sec8_title);
        note(s8, R.string.checklist_sec8_note1);
        item(s8, R.string.checklist_sec8_item1);
        item(s8, R.string.checklist_sec8_item2);
        item(s8, R.string.checklist_sec8_item3);
        item(s8, R.string.checklist_sec8_item4);
        sections.add(s8);

        Section s9 = new Section(R.string.checklist_sec9_title);
        note(s9, R.string.checklist_sec9_note1);
        item(s9, R.string.checklist_sec9_item1);
        item(s9, R.string.checklist_sec9_item2);
        item(s9, R.string.checklist_sec9_item3);
        item(s9, R.string.checklist_sec9_item4);
        item(s9, R.string.checklist_sec9_item5);
        item(s9, R.string.checklist_sec9_item6);
        item(s9, R.string.checklist_sec9_item7);
        item(s9, R.string.checklist_sec9_item8);
        item(s9, R.string.checklist_sec9_item9);
        sections.add(s9);

        return sections;
    }

    private void item(Section section, int textResId, int... subResIds) {
        Block block = new Block(BlockType.ITEM, textResId);
        block.prefsKey = getResources().getResourceEntryName(textResId);
        if (subResIds != null && subResIds.length > 0) {
            block.subResIds = subResIds;
        }
        section.blocks.add(block);
        totalItems++;
    }

    private void heading(Section section, int textResId) {
        section.blocks.add(new Block(BlockType.HEADING, textResId));
    }

    private void info(Section section, int textResId) {
        section.blocks.add(new Block(BlockType.INFO, textResId));
    }

    private void note(Section section, int textResId) {
        section.blocks.add(new Block(BlockType.NOTE, textResId));
    }

    private void arabic(Section section, int textResId) {
        section.blocks.add(new Block(BlockType.ARABIC, textResId));
    }

    // ---------------------------------------------------------------------
    // Rendering
    // ---------------------------------------------------------------------

    private void renderSections(List<Section> sections) {
        checkedCount = 0;
        for (Section section : sections) {
            checklistContainer.addView(buildSectionTitle(getString(section.titleResId)));

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable.bg_search_white);
            int pad = dp(16);
            card.setPadding(pad, pad, pad, pad);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardParams.bottomMargin = dp(20);
            card.setLayoutParams(cardParams);

            for (Block block : section.blocks) {
                card.addView(buildBlockView(block));
            }

            checklistContainer.addView(card);
        }
    }

    private TextView buildSectionTitle(String text) {
        TextView title = new TextView(this);
        title.setText(text);
        title.setTextSize(15);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(getResources().getColor(R.color.text_dark));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(8);
        title.setLayoutParams(params);
        return title;
    }

    private android.view.View buildBlockView(Block block) {
        switch (block.type) {
            case ITEM:
                return buildItemRow(block);
            case HEADING:
                return buildHeadingView(getString(block.textResId));
            case INFO:
                return buildInfoBullet(getString(block.textResId));
            case NOTE:
                return buildNoteView(getString(block.textResId));
            case ARABIC:
                return buildArabicView(getString(block.textResId));
            default:
                return new TextView(this);
        }
    }

    private android.view.View buildItemRow(Block block) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.topMargin = dp(4);
        rowParams.bottomMargin = dp(4);
        row.setLayoutParams(rowParams);

        LinearLayout topLine = new LinearLayout(this);
        topLine.setOrientation(LinearLayout.HORIZONTAL);
        topLine.setGravity(Gravity.CENTER_VERTICAL);

        CheckBox checkBox = new CheckBox(this);
        boolean savedChecked = prefs.getBoolean(block.prefsKey, false);
        checkBox.setChecked(savedChecked);
        if (savedChecked) checkedCount++;

        TextView label = new TextView(this);
        label.setText(getString(block.textResId));
        label.setTextSize(15);
        label.setTextColor(getResources().getColor(R.color.text_dark));
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        label.setLayoutParams(labelParams);
        applyStrikethrough(label, savedChecked);

        checkBox.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            prefs.edit().putBoolean(block.prefsKey, isChecked).apply();
            checkedCount += isChecked ? 1 : -1;
            applyStrikethrough(label, isChecked);
            updateProgressText();
        });

        topLine.addView(checkBox);
        topLine.addView(label);
        row.addView(topLine);

        if (block.subResIds != null) {
            for (int subResId : block.subResIds) {
                TextView subView = new TextView(this);
                subView.setText("- " + getString(subResId));
                subView.setTextSize(13);
                subView.setTextColor(getResources().getColor(R.color.text_gray));
                LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                subParams.leftMargin = dp(40);
                subView.setLayoutParams(subParams);
                row.addView(subView);
            }
        }

        return row;
    }

    private void applyStrikethrough(TextView label, boolean checked) {
        if (checked) {
            label.setPaintFlags(label.getPaintFlags() | TextPaint.STRIKE_THRU_TEXT_FLAG);
            label.setAlpha(0.55f);
        } else {
            label.setPaintFlags(label.getPaintFlags() & ~TextPaint.STRIKE_THRU_TEXT_FLAG);
            label.setAlpha(1f);
        }
    }

    private TextView buildHeadingView(String text) {
        TextView heading = new TextView(this);
        heading.setText(text);
        heading.setTextSize(13);
        heading.setTypeface(null, Typeface.BOLD);
        heading.setTextColor(getResources().getColor(R.color.pink_dark));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(10);
        params.bottomMargin = dp(2);
        heading.setLayoutParams(params);
        return heading;
    }

    private TextView buildInfoBullet(String text) {
        TextView info = new TextView(this);
        info.setText("\u2022 " + text);
        info.setTextSize(13);
        info.setTextColor(getResources().getColor(R.color.text_gray));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = dp(8);
        params.topMargin = dp(2);
        info.setLayoutParams(params);
        return info;
    }

    private TextView buildNoteView(String text) {
        TextView note = new TextView(this);
        note.setText(text);
        note.setTextSize(13);
        note.setTypeface(null, Typeface.ITALIC);
        note.setTextColor(getResources().getColor(R.color.text_gray));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(8);
        params.bottomMargin = dp(4);
        note.setLayoutParams(params);
        return note;
    }

    private TextView buildArabicView(String text) {
        TextView arabic = new TextView(this);
        arabic.setText(text);
        arabic.setTextSize(TypedValue.COMPLEX_UNIT_SP, ARABIC_TEXT_SIZE_SP);
        arabic.setTypeface(null, Typeface.BOLD);
        arabic.setTextColor(getResources().getColor(R.color.text_dark));
        arabic.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(4);
        params.bottomMargin = dp(10);
        arabic.setLayoutParams(params);
        return arabic;
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}
