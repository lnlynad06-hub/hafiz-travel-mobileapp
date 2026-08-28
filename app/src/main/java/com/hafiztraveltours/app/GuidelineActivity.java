package com.hafiztraveltours.app;

import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * "Guideline Persediaan" page - 6 expandable/collapsible sections (accordion:
 * tap a section header to open/close it, only one row of content shown at a
 * time per section).
 *
 * Section 1 ("Adab Musafir & Solat Ketika Bermusafir") content is transcribed
 * from the uploaded reference document Bab_1_Adab_Musafir.wps (adab, niat,
 * and doa for travel - mandi sunat, solat sunat musafir, solat qasar/jamak,
 * solat menghormati waktu, etc). Sections 2-6 reuse the same checklist
 * categories previously shown in MainActivity's Checklist quick action.
 *
 * TODO: eventually pull per-trip guideline content from the backend instead
 * of this hardcoded list (destination-specific items like "suntikan
 * meningitis" only apply to Umrah/Haj trips, not e.g. a Korea tour).
 */
public class GuidelineActivity extends AppCompatActivity {

    private enum ItemType { SUBHEAD, TEXT, ARABIC, BULLET }

    private static class ContentItem {
        final ItemType type;
        final String text;
        ContentItem(ItemType type, String text) {
            this.type = type;
            this.text = text;
        }
    }

    private static class GuidelineSection {
        final String title;
        final List<ContentItem> items;
        GuidelineSection(String title, List<ContentItem> items) {
            this.title = title;
            this.items = items;
        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applySavedLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guideline);

        findViewById(R.id.guidelineBackButton).setOnClickListener(v -> finish());

        renderSections();
    }

    // ---------- Accordion rendering ----------

    private void renderSections() {
        LinearLayout container = findViewById(R.id.guidelineContentContainer);
        container.removeAllViews();

        for (GuidelineSection section : buildSections()) {
            container.addView(buildSectionView(section));
        }
    }

    private View buildSectionView(GuidelineSection section) {
        LinearLayout sectionWrapper = new LinearLayout(this);
        sectionWrapper.setOrientation(LinearLayout.VERTICAL);
        sectionWrapper.setBackgroundResource(R.drawable.bg_search_white);
        LinearLayout.LayoutParams wrapperParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        wrapperParams.bottomMargin = dp(12);
        sectionWrapper.setLayoutParams(wrapperParams);

        // Header row: title + chevron indicator, tap to toggle
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        int headerPad = dp(16);
        header.setPadding(headerPad, headerPad, headerPad, headerPad);
        header.setClickable(true);
        header.setFocusable(true);

        TextView titleView = new TextView(this);
        titleView.setText(section.title);
        titleView.setTextSize(15);
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);
        titleView.setTextColor(getResources().getColor(R.color.text_dark));
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        titleView.setLayoutParams(titleParams);

        TextView chevron = new TextView(this);
        chevron.setText("\u25BE"); // ▾ collapsed indicator
        chevron.setTextSize(16);
        chevron.setTextColor(getResources().getColor(R.color.pink_dark));

        header.addView(titleView);
        header.addView(chevron);

        // Body: built once, starts collapsed
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        int bodyPadH = dp(16);
        body.setPadding(bodyPadH, 0, bodyPadH, dp(16));
        body.setVisibility(View.GONE);
        for (ContentItem item : section.items) {
            body.addView(buildContentItemView(item));
        }

        header.setOnClickListener(v -> {
            boolean isOpen = body.getVisibility() == View.VISIBLE;
            body.setVisibility(isOpen ? View.GONE : View.VISIBLE);
            chevron.setText(isOpen ? "\u25BE" : "\u25B4"); // ▾ closed / ▴ open
        });

        sectionWrapper.addView(header);
        sectionWrapper.addView(body);
        return sectionWrapper;
    }

    private View buildContentItemView(ContentItem item) {
        TextView view = new TextView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        switch (item.type) {
            case SUBHEAD:
                view.setText(item.text);
                view.setTextSize(14);
                view.setTypeface(null, android.graphics.Typeface.BOLD);
                view.setTextColor(getResources().getColor(R.color.pink_dark));
                params.topMargin = dp(14);
                break;
            case ARABIC:
                view.setText(item.text);
                view.setTextSize(18);
                view.setTextColor(getResources().getColor(R.color.text_dark));
                view.setGravity(Gravity.END);
                view.setLineSpacing(dp(6), 1f);
                params.topMargin = dp(8);
                break;
            case BULLET:
                view.setText("\u2022  " + item.text);
                view.setTextSize(13);
                view.setTextColor(getResources().getColor(R.color.text_dark));
                params.topMargin = dp(6);
                break;
            case TEXT:
            default:
                view.setText(item.text);
                view.setTextSize(13);
                view.setTextColor(getResources().getColor(R.color.text_gray));
                params.topMargin = dp(6);
                break;
        }

        view.setLayoutParams(params);
        return view;
    }

    // ---------- Section data ----------

    private List<GuidelineSection> buildSections() {
        List<GuidelineSection> sections = new ArrayList<>();
        sections.add(buildAdabMusafirSection());
        sections.add(new GuidelineSection("Dokumen Perjalanan", bulletsOnly(
                "Pasport sah 6 bulan ke atas",
                "Bawa pasport bersama",
                "Bawa pasport lama (jika baru perbaharui)",
                "Semak status perjalanan luar negara (SSPI)"
        )));
        sections.add(new GuidelineSection("Ubat-ubatan", bulletsOnly(
                "Panadol",
                "Ubat sakit tekak",
                "Ubat cirit-birit",
                "Ubat tahan muntah",
                "Salonpas",
                "Minyak angin",
                "Olive oil / moisturizer / lipbalm",
                "Heat pad"
        )));
        sections.add(new GuidelineSection("Pakaian", bulletsOnly(
                "Jaket nipis (lapisan sederhana)",
                "Jeans / seluar panjang nipis",
                "Jaket tambahan (jika cuaca sejuk/berangin)",
                "Kasut bertutup & selesa (sneakers)",
                "Sunglasses & cap"
        )));
        sections.add(new GuidelineSection("Peralatan Elektronik", bulletsOnly(
                "Travel adaptor",
                "Powerbank (maks. 20,000mAh - letak handcarry)"
        )));
        sections.add(new GuidelineSection("Kewangan", bulletsOnly(
                "Tukar mata wang mengikut destinasi",
                "Aktifkan kad debit/credit untuk kegunaan luar negara"
        )));
        return sections;
    }

    private List<ContentItem> bulletsOnly(String... items) {
        List<ContentItem> list = new ArrayList<>();
        for (String s : items) {
            list.add(new ContentItem(ItemType.BULLET, s));
        }
        return list;
    }

    /**
     * Transcribed from Bab_1_Adab_Musafir.wps (uploaded reference document).
     */
    private GuidelineSection buildAdabMusafirSection() {
        List<ContentItem> items = new ArrayList<>();

        items.add(new ContentItem(ItemType.SUBHEAD, "1. Adab-Adab Memulakan Perjalanan"));

        items.add(new ContentItem(ItemType.SUBHEAD, "A) Mandi Sunat Musafir"));
        items.add(new ContentItem(ItemType.BULLET, "Dilakukan sebelum keluar untuk bermusafir"));
        items.add(new ContentItem(ItemType.TEXT, "Niat Mandi:"));
        items.add(new ContentItem(ItemType.ARABIC, "\u0646َوَيْتُ الْغُسْلَ لِلسَّفَرِ سُنَّةً لِلَّهِ تَعَالَى"));
        items.add(new ContentItem(ItemType.TEXT, "Sahaja aku mandi sunat untuk bermusafir kerana Allah Ta'ala."));

        items.add(new ContentItem(ItemType.SUBHEAD, "B) Solat Sunat Musafir (2 rakaat)"));
        items.add(new ContentItem(ItemType.BULLET, "Rakaat 1: Surah al-Fatihah + Surah al-Kafirun"));
        items.add(new ContentItem(ItemType.BULLET, "Rakaat 2: Surah al-Fatihah + Surah al-Ikhlas"));
        items.add(new ContentItem(ItemType.TEXT, "Niat Solat:"));
        items.add(new ContentItem(ItemType.ARABIC, "\u0623ُصَلِّي سُنَّةَ السَّفَرِ رَكْعَتَيْنِ لِلَّهِ تَعَالَى"));
        items.add(new ContentItem(ItemType.TEXT, "Sahaja aku solat sunat musafir dua rakaat kerana Allah Ta'ala."));

        items.add(new ContentItem(ItemType.SUBHEAD, "C) Bacaan Sebelum Keluar Rumah"));
        items.add(new ContentItem(ItemType.BULLET, "Ayat Kursi (1 kali)"));
        items.add(new ContentItem(ItemType.BULLET, "Surah Quraisy (1 kali)"));

        items.add(new ContentItem(ItemType.SUBHEAD, "D) Doa Menaiki Kenderaan"));
        items.add(new ContentItem(ItemType.ARABIC,
                "\u0627َللَّهُمَّ أَنْتَ الصَّاحِبُ فِي السَّفَرِ وَالْخَلِيفَةُ فِي الْأَهْلِ وَالْمَالِ وَالْوَلَدِ وَالصَّحَابِ، "
                        + "اللَّهُمَّ احْفَظْنَا وَإِيَّاهُمْ مِنْ آفَاتِ الدُّنْيَا وَعَاهَاتِهَا، اللَّهُمَّ إِنَّا نَسْأَلُكَ فِي سَفَرِنَا هَذَا الْبِرَّ وَالتَّقْوَى "
                        + "وَمِنَ الْعَمَلِ مَا تَرْضَى، اللَّهُمَّ إِنَّا نَسْأَلُكَ أَنْ تَطْوِيَ لَنَا الْأَرْضَ وَتُؤْمِنَ عَلَيْنَا السَّفَرَ وَأَنْ تَرْزُقَنَا "
                        + "فِي سَفَرِنَا هَذَا سَلَامَةً فِي الْبَدَنِ وَالدِّينِ وَالْمَالِ وَتُبْلِغَنَا عُمْرَةَ بَيْتِكَ وَزِيَارَةَ قَبْرِ نَبِيِّكَ مُحَمَّدٍ صَلَّى اللَّهُ عَلَيْهِ وَسَلَّمَ، "
                        + "اللَّهُمَّ إِنَّا نَعُوذُ بِكَ مِنْ وَعْثَاءِ السَّفَرِ وَكَآبَةِ الْمُنْقَلَبِ بِسُوءِ الْمَنْظَرِ فِي الْأَهْلِ وَالْمَالِ وَالْوَلَدِ وَالصَّحَابِ، "
                        + "اللَّهُمَّ اجْعَلْنَا وَإِيَّاهُمْ فِي جِوَارِكَ وَلَا تَسْلُبْنَا وَإِيَّاهُمْ نِعْمَتَكَ وَلَا تُغَيِّرْ مَا بِنَا وَبِهِمْ مِنْ عَافِيَتِكَ يَا أَرْحَمَ الرَّاحِمِينَ."));

        items.add(new ContentItem(ItemType.SUBHEAD, "E) Doa Memohon Pertolongan"));
        items.add(new ContentItem(ItemType.ARABIC,
                "\u0627َللَّهُمَّ بِكَ أَسْتَعِينُ وَعَلَيْكَ أَتَوَكَّلُ، اللَّهُمَّ ذَلِّلْ لِي صُعُوبَةَ أَمْرِي وَسَهِّلْ عَلَيْنَا سَفَرَنَا "
                        + "وَارْزُقْنِي مِنَ الْخَيْرِ أَكْثَرَ مِمَّا أَطْلُبُ وَاصْرِفْ عَنِّي كُلَّ شَرٍّ رَبِّ اشْرَحْ لِي صَدْرِي وَيَسِّرْ لِي أَمْرِي، "
                        + "اللَّهُمَّ إِنِّي أَسْتَحْفِظُكَ وَأَسْتَوْدِعُكَ نَفْسِي وَدِينِي وَأَقَارِبِي وَكُلَّ مَا أَعْطَيْتَنَا بِهِ مِنْ آخِرَةٍ وَدُنْيَا "
                        + "فَاحْفَظْنَا أَجْمَعِينَ مِنْ كُلِّ سُوءٍ، يَا كَرِيمُ، وَصَلَّى اللَّهُ عَلَى سَيِّدِنَا مُحَمَّدٍ، وَعَلَى آلِهِ وَصَحْبِهِ أَجْمَعِينَ وَالْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ."));

        items.add(new ContentItem(ItemType.SUBHEAD, "F) Doa Sebelum Keluar Rumah"));
        items.add(new ContentItem(ItemType.ARABIC,
                "\u0627َللَّهُمَّ إِنِّي أَعُوذُ بِكَ أَنْ أَضِلَّ أَوْ أُضَلَّ أَوْ أَزِلَّ أَوْ أُزَلَّ أَوْ أَظْلِمَ أَوْ أُظْلَمَ أَوْ أَجْهَلَ أَوْ يُجْهَلَ عَلَيَّ"));

        items.add(new ContentItem(ItemType.SUBHEAD, "G) Doa Ketika Keluar Rumah"));
        items.add(new ContentItem(ItemType.ARABIC,
                "\u0628ِسْمِ اللَّهِ تَوَكَّلْتُ عَلَى اللَّهِ، وَلَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ"));

        items.add(new ContentItem(ItemType.SUBHEAD, "H) Doa Dalam Kapal Terbang"));
        items.add(new ContentItem(ItemType.ARABIC,
                "\u0628ِسْمِ اللَّهِ مَجْرَاهَا وَمُرْسَاهَا إِنَّ رَبِّي لَغَفُورٌ رَحِيمٌ"));

        items.add(new ContentItem(ItemType.SUBHEAD, "2. Solat Qasar dan Jamak"));
        items.add(new ContentItem(ItemType.TEXT,
                "Solat Qasar: Memendekkan solat fardhu 4 rakaat (Zuhur, Asar, Isyak) kepada 2 rakaat."));
        items.add(new ContentItem(ItemType.TEXT, "Syarat Solat Qasar:"));
        items.add(new ContentItem(ItemType.BULLET, "Perjalanan melebihi 2 marhalah (81 km)"));
        items.add(new ContentItem(ItemType.BULLET, "Mengetahui destinasi"));
        items.add(new ContentItem(ItemType.BULLET, "Dilaksanakan selepas melepasi daerah/sempadan bandar"));
        items.add(new ContentItem(ItemType.BULLET, "Perjalanan adalah harus (bukan maksiat)"));
        items.add(new ContentItem(ItemType.BULLET, "Tidak berniat bermusafir 4 hari atau lebih (tidak masuk hari pergi dan balik)"));
        items.add(new ContentItem(ItemType.TEXT, "Pilihan Pelaksanaan:"));
        items.add(new ContentItem(ItemType.BULLET, "Qasar sahaja"));
        items.add(new ContentItem(ItemType.BULLET, "Jamak sahaja"));
        items.add(new ContentItem(ItemType.BULLET, "Gabungan Qasar dan Jamak"));

        items.add(new ContentItem(ItemType.SUBHEAD, "Niat Solat Qasar"));
        items.add(new ContentItem(ItemType.TEXT, "1. Niat Solat Qasar Zuhur (2 rakaat)"));
        items.add(new ContentItem(ItemType.ARABIC, "\u0623ُصَلِّي فَرْضَ الظُّهْرِ رَكْعَتَيْنِ قَصْرًا لِلَّهِ تَعَالَى"));
        items.add(new ContentItem(ItemType.TEXT, "Sahaja aku solat fardhu Zuhur dua rakaat qasar tunai kerana Allah Ta'ala"));
        items.add(new ContentItem(ItemType.TEXT, "2. Niat Solat Qasar Asar (2 rakaat)"));
        items.add(new ContentItem(ItemType.ARABIC, "\u0623ُصَلِّي فَرْضَ الْعَصْرِ رَكْعَتَيْنِ قَصْرًا لِلَّهِ تَعَالَى"));
        items.add(new ContentItem(ItemType.TEXT, "Sahaja aku solat fardhu Asar dua rakaat qasar tunai kerana Allah Ta'ala"));
        items.add(new ContentItem(ItemType.TEXT, "3. Niat Solat Qasar Isyak (2 rakaat)"));
        items.add(new ContentItem(ItemType.ARABIC, "\u0623ُصَلِّي فَرْضَ الْعِشَاءِ رَكْعَتَيْنِ قَصْرًا لِلَّهِ تَعَالَى"));
        items.add(new ContentItem(ItemType.TEXT, "Sahaja aku solat fardhu Isyak dua rakaat qasar tunai kerana Allah Ta'ala"));

        items.add(new ContentItem(ItemType.SUBHEAD, "Solat Jamak"));
        items.add(new ContentItem(ItemType.TEXT, "Menghimpunkan dua solat fardu dalam satu waktu."));
        items.add(new ContentItem(ItemType.TEXT,
                "Solat yang boleh dijamak: Zuhur dengan Asar, dan Maghrib dengan Isyak. Subuh tidak boleh dijamak."));

        items.add(new ContentItem(ItemType.SUBHEAD, "1. Jamak Taqdim (dilakukan pada waktu pertama)"));
        items.add(new ContentItem(ItemType.TEXT,
                "Syarat: Tertib (dahulukan solat waktu pertama), berniat menghimpunkan solat kedua bersama solat pertama "
                        + "di awal takbir atau sebelum salam solat pertama, berturut-turut (muw\u0101lat), dan perjalanan berterusan sehingga selesai solat kedua."));
        items.add(new ContentItem(ItemType.TEXT, "Niat solat Zuhur + Asar"));
        items.add(new ContentItem(ItemType.ARABIC,
                "\u0623ُصَلِّي فَرْضَ الظُّهْرِ أَرْبَعَ رَكَعَاتٍ مَجْمُوعًا إِلَيْهِ الْعَصْرُ أَدَاءً لِلَّهِ تَعَالَى"));
        items.add(new ContentItem(ItemType.TEXT,
                "Sahaja aku solat fardhu Zuhur empat rakaat dihimpunkan solat Asar kepadanya tunai kerana Allah Ta'ala"));
        items.add(new ContentItem(ItemType.TEXT, "Niat solat Maghrib + Isyak"));
        items.add(new ContentItem(ItemType.ARABIC,
                "\u0623ُصَلِّي فَرْضَ الْمَغْرِبِ ثَلَاثَ رَكَعَاتٍ مَجْمُوعًا إِلَيْهِ الْعِشَاءُ أَدَاءً لِلَّهِ تَعَالَى"));
        items.add(new ContentItem(ItemType.TEXT,
                "Sahaja aku solat fardhu Maghrib tiga rakaat dihimpunkan solat Isyak kepadanya tunai kerana Allah Ta'ala"));

        items.add(new ContentItem(ItemType.SUBHEAD, "2. Jamak Takhir (dilakukan pada waktu kedua)"));
        items.add(new ContentItem(ItemType.TEXT,
                "Syarat: Berniat di dalam hati untuk menjamakkan solat pertama kepada waktu kedua sebelum habis waktu solat pertama, "
                        + "dan perjalanan berterusan sehingga kedua-dua solat selesai."));
        items.add(new ContentItem(ItemType.TEXT, "Niat solat Asar + Zuhur:"));
        items.add(new ContentItem(ItemType.ARABIC,
                "\u0623ُصَلِّي فَرْضَ الظُّهْرِ أَرْبَعَ رَكَعَاتٍ مَجْمُوعًا إِلَيْهِ الْعَصْرُ أَدَاءً لِلَّهِ تَعَالَى"));
        items.add(new ContentItem(ItemType.TEXT,
                "Sahaja aku solat fardhu Asar empat rakaat dihimpunkan kepada Zuhur tunai kerana Allah Ta'ala"));
        items.add(new ContentItem(ItemType.TEXT, "Niat solat Maghrib + Isyak"));
        items.add(new ContentItem(ItemType.ARABIC,
                "\u0623ُصَلِّي فَرْضَ الْمَغْرِبِ ثَلَاثَ رَكَعَاتٍ مَجْمُوعًا إِلَيْهِ الْعِشَاءُ أَدَاءً لِلَّهِ تَعَالَى"));
        items.add(new ContentItem(ItemType.TEXT,
                "Sahaja aku solat fardhu Isyak empat rakaat dihimpunkan kepada Maghrib tunai kerana Allah Ta'ala"));

        items.add(new ContentItem(ItemType.SUBHEAD, "3. Solat Jamak Beserta Qasar"));
        items.add(new ContentItem(ItemType.TEXT,
                "Boleh menghimpunkan dua solat sekaligus dengan memendekkan solat Zuhur, Asar dan Isyak kepada dua rakaat, "
                        + "mengikut syarat qasar dan jamak."));

        items.add(new ContentItem(ItemType.TEXT, "Niat bagi Jamak Taqdim & Qasar:"));
        items.add(new ContentItem(ItemType.TEXT, "Zuhur + Asar (di waktu Zuhur) \u2013 dahulukan Zuhur:"));
        items.add(new ContentItem(ItemType.TEXT, "Niat Zuhur (qasar + jamak):"));
        items.add(new ContentItem(ItemType.ARABIC,
                "\u0623ُصَلِّي فَرْضَ الظَّهْرِ رَكْعَتَيْنِ قَصْرًا مَجْمُوعًا إِلَيْهِ الْعَصْرُ أَدَاءً لِلَّهِ تَعَالَى"));
        items.add(new ContentItem(ItemType.TEXT,
                "Sahaja aku solat fardhu Zuhur dua rakaat qasar dihimpunkan Asar kepadanya tunai kerana Allah Ta'ala"));
        items.add(new ContentItem(ItemType.TEXT, "Selepas salam, niat Asar (qasar):"));
        items.add(new ContentItem(ItemType.ARABIC,
                "\u0623ُصَلِّي فَرْضَ الْعَصْرِ رَكْعَتَيْنِ قَصْرًا أَدَاءً لِلَّهِ تَعَالَى"));
        items.add(new ContentItem(ItemType.TEXT,
                "Sahaja aku solat fardhu Asar dua rakaat qasar tunai kerana Allah Ta'ala"));

        items.add(new ContentItem(ItemType.TEXT, "Maghrib + Isyak (di waktu Maghrib) \u2013 dahulukan Maghrib:"));
        items.add(new ContentItem(ItemType.TEXT, "Niat Maghrib (jamak, tanpa qasar):"));
        items.add(new ContentItem(ItemType.ARABIC,
                "\u0623ُصَلِّي فَرْضَ الْمَغْرِبِ ثَلَاثَ رَكَعَاتٍ مَجْمُوعًا إِلَيْهِ الْعِشَاءُ أَدَاءً لِلَّهِ تَعَالَى"));
        items.add(new ContentItem(ItemType.TEXT,
                "Sahaja aku solat fardhu Maghrib tiga rakaat qasar dihimpunkan Isyak kepadanya tunai kerana Allah Ta'ala"));
        items.add(new ContentItem(ItemType.TEXT, "Selepas salam, niat Isyak (qasar):"));
        items.add(new ContentItem(ItemType.ARABIC,
                "\u0623ُصَلِّي فَرْضَ الْعِشَاءِ رَكْعَتَيْنِ قَصْرًا أَدَاءً لِلَّهِ تَعَالَى"));
        items.add(new ContentItem(ItemType.TEXT,
                "Sahaja aku solat fardhu Isyak dua rakaat qasar tunai kerana Allah Ta'ala"));

        items.add(new ContentItem(ItemType.TEXT, "Niat bagi Jamak Takhir & Qasar:"));
        items.add(new ContentItem(ItemType.TEXT, "Asar + Zuhur (di waktu Asar) \u2013 dahulukan Asar:"));
        items.add(new ContentItem(ItemType.TEXT, "Niat Asar (qasar + jamak):"));
        items.add(new ContentItem(ItemType.ARABIC,
                "\u0623ُصَلِّي فَرْضَ الْعَصْرِ رَكْعَتَيْنِ قَصْرًا مَجْمُوعًا إِلَيْهِ الظُّهْرُ أَدَاءً لِلَّهِ تَعَالَى"));
        items.add(new ContentItem(ItemType.TEXT,
                "Sahaja aku solat fardhu Asar dua rakaat qasar dihimpunkan kepadanya Zuhur tunai kerana Allah Ta'ala"));
        items.add(new ContentItem(ItemType.TEXT, "Selepas salam, niat Zuhur (qasar):"));
        items.add(new ContentItem(ItemType.ARABIC,
                "\u0623ُصَلِّي فَرْضَ الظُّهْرِ رَكْعَتَيْنِ قَصْرًا أَدَاءً لِلَّهِ تَعَالَى"));
        items.add(new ContentItem(ItemType.TEXT,
                "Sahaja aku solat fardhu Zuhur dua rakaat qasar tunai kerana Allah Ta'ala"));

        items.add(new ContentItem(ItemType.TEXT, "Isyak + Maghrib (di waktu Isyak) \u2013 dahulukan Isyak:"));
        items.add(new ContentItem(ItemType.TEXT, "Niat Isyak (qasar + jamak):"));
        items.add(new ContentItem(ItemType.ARABIC,
                "\u0623ُصَلِّي فَرْضَ الْعِشَاءِ رَكْعَتَيْنِ قَصْرًا مَجْمُوعًا إِلَيْهِ الْمَغْرِبُ أَدَاءً لِلَّهِ تَعَالَى"));
        items.add(new ContentItem(ItemType.TEXT,
                "Sahaja aku solat fardhu Isyak dua rakaat qasar dihimpunkan kepadanya Maghrib tunai kerana Allah Ta'ala"));
        items.add(new ContentItem(ItemType.TEXT, "Selepas salam, niat Maghrib (tanpa qasar):"));
        items.add(new ContentItem(ItemType.ARABIC,
                "\u0623ُصَلِّي فَرْضَ الْمَغْرِبِ ثَلَاثَ رَكَعَاتٍ أَدَاءً لِلَّهِ تَعَالَى"));
        items.add(new ContentItem(ItemType.TEXT,
                "Sahaja aku solat fardhu Maghrib tiga rakaat qasar tunai kerana Allah Ta'ala"));

        items.add(new ContentItem(ItemType.SUBHEAD, "Solat Menghormati Waktu (dalam kapal terbang)"));
        items.add(new ContentItem(ItemType.TEXT,
                "Solat fardu yang dilakukan ketika tiada kemampuan untuk bersuci (air terhad) atau tiada kemudahan menghadap kiblat, "
                        + "terutamanya dalam pesawat. Solat ini wajib diqada (diulang) apabila telah berkemampuan \u2013 jika waktu masih ada, diulang "
                        + "dalam waktu itu; jika waktu sudah luput, diqadakan."));
        items.add(new ContentItem(ItemType.TEXT, "Tatacara:"));
        items.add(new ContentItem(ItemType.BULLET,
                "Niat, contoh: \u201CSahaja aku solat fardu Subuh dua rakaat menghormati waktu kerana Allah Ta'ala.\u201D"));
        items.add(new ContentItem(ItemType.BULLET, "Duduk di tempat duduk dengan tali pinggang keledar dan kasut dibuka."));
        items.add(new ContentItem(ItemType.BULLET, "Tidak perlu menukar posisi untuk menghadap kiblat."));
        items.add(new ContentItem(ItemType.BULLET, "Rukuk: isyarat dengan menundukkan kepala dan badan sedikit, tangan ke lutut."));
        items.add(new ContentItem(ItemType.BULLET,
                "Sujud: isyarat dengan menundukkan kepala dan badan lebih rendah daripada rukuk (tanpa menggunakan meja makan)."));
        items.add(new ContentItem(ItemType.BULLET, "Duduk antara dua sujud, tahiyat dan salam seperti biasa di tempat duduk."));

        items.add(new ContentItem(ItemType.SUBHEAD, "Doa Ketika Kapal Terbang Mendarat"));
        items.add(new ContentItem(ItemType.ARABIC,
                "\u0627َللَّهُمَّ رَبَّ السَّمَاوَاتِ السَّبْعِ وَمَا أَظْلَلْنَ وَرَبَّ الْأَرَضِينَ السَّبْعِ وَمَا أَقْلَلْنَ وَرَبَّ الشَّيَاطِينِ "
                        + "وَمَا أَضْلَلْنَ وَرَبَّ الرِّيَاحِ وَمَا ذَرَيْنَ فَإِنَّا نَسْأَلُكَ خَيْرَ هَٰذِهِ الْقَرْيَةِ وَخَيْرَ أَهْلِهَا وَنَعُوذُ بِكَ مِنْ شَرِّهَا وَشَرِّ أَهْلِهَا وَشَرِّ مَا فِيهَا"));
        items.add(new ContentItem(ItemType.ARABIC,
                "\u0627َللَّهُمَّ صَلِّ عَلَى سَيِّدِنَا مُحَمَّدٍ وَعَلَى آلِ سَيِّدِنَا مُحَمَّدٍ"));

        return new GuidelineSection("Adab Musafir & Solat Ketika Bermusafir", items);
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}