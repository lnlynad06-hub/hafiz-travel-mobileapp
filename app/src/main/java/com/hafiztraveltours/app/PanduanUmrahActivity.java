package com.hafiztraveltours.app;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
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
 * Bab 1: Adab Musafir dan Amalan Sunat - READY
 * Bab 2: (not yet available)
 * Bab 3: Pelaksanaan Ibadah Umrah - READY
 * Bab 4-6: (not yet available)
 *
 * TODO: replace placeholder section summaries with real short-form content,
 * and wire each chapter's PDF button to its real (paid) download link once
 * available.
 */
public class PanduanUmrahActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applySavedLocale(newBase));
    }

    private static class SectionItem {
        String title;
        String summary;
        SectionItem(String title, String summary) {
            this.title = title;
            this.summary = summary;
        }
    }

    private static class Chapter {
        String tabLabel;       // "Bab 1"
        String heroTitle;      // shown in pink hero when this chapter is open
        String heroBlurb;
        boolean available;
        List<SectionItem> sections;

        Chapter(String tabLabel, String heroTitle, String heroBlurb, boolean available) {
            this.tabLabel = tabLabel;
            this.heroTitle = heroTitle;
            this.heroBlurb = heroBlurb;
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

        buildChapters();
        buildTabRow();
        selectChapter(0);
    }

    private void buildChapters() {
        // ---- Bab 1: Adab Musafir dan Amalan Sunat ----
        Chapter bab1 = new Chapter("Bab 1", "Adab Musafir dan Amalan Sunat",
                "Adab bermusafir dan solat semasa dalam perjalanan.", true);
        bab1.sections.add(new SectionItem(
                "Adab-Adab Memulakan Perjalanan",
                "A) Mandi Sunat Musafir\n" +
                        "-Dilakukan sebelum keluar untuk bermusafir\n" +
                        "\n" +
                        "Niat Mandi:\n" +
                        "\n" +
                        "نَوْيْتُ الْغُسْلَ لِلسَّفَرِ سُنَّةً لِلَّهِ تَعَالَى\n" +
                        "Sahaja aku mandi sunat untuk bermusafir kerana Allah Ta'ala.\n" +
                        "\n" +
                        "B) Solat Sunat Musafir (2 rakaat)\n" +
                        "-Rakaat 1: Surah al-Fatihah + Surah al-Kafirun\n" +
                        "-Rakaat 2: Surah al-Fatihah + Surah al-Ikhlas\n" +
                        "\n" +
                        "Niat Solat:\n" +
                        "\n" +
                        "أُصَلِّي سُنَّةَ السَّفَرِ رَكْعَتَيْنِ لِلَّهِ تَعَالَى\n" +
                        "Sahaja aku solat sunat musafir dua rakaat kerana Allah Ta'ala.\n" +
                        "\n" +
                        "B) Bacaan Sebelum Keluar Rumah\n" +
                        "-Ayat Kursi (1 kali)\n" +
                        "-Surah Quraisy (1 kali)\n" +
                        "C) Doa Menaiki Kenderaan\n" +
                        "\n" +
                        "اللَّهُمَّ أَنْتَ الصَّاحِبُ فِي السَّفَرِ وَالْخَلِيفَةُ فِي الْأَهْلِ وَالْمَالِ وَالْوَلَدِ وَالصَّحَابِ، اللَّهُمَّ احْفَظْنَا وَإِيَّاهُمْ مِنْ آفَاتِ الدُّنْيَا وَعَاهَاتِهَا، اللَّهُمَّ إِنَّا نَسْأَلُكَ فِي سَفَرِنَا هَذَا الْبِرَّ وَالتَّقْوَى وَمِنَ الْعَمَلِ مَا تَرْضَى، اللَّهُمَّ إِنَّا نَسْأَلُكَ أَنْ تَطْوِيَ لَنَا الْأَرْضَ وَتُؤْمِنَ عَلَيْنَا السَّفَرَ وَأَنْ تَرْزُقَنَا فِي سَفَرِنَا هَذَا سَلَامَةً فِي الْبَدَنِ وَالدِّينِ وَالْمَالِ وَتُبْلِغَنَا عُمْرَةَ بَيْتِكَ وَزِيَارَةَ قَبْرِ نَبِيِّكَ مُحَمَّدٍ صَلَّى اللَّهُ عَلَيْهِ وَسَلَّمَ، اللَّهُمَّ إِنَّا نَعُوذُ بِكَ مِنْ وَعْثَاءِ السَّفَرِ وَكَآبَةِ الْمُنْقَلَبِ بِسُوءِ الْمَنْظَرِ فِي الْأَهْلِ وَالْمَالِ وَالْوَلَدِ وَالصَّحَابِ، اللَّهُمَّ اجْعَلْنَا وَإِيَّاهُمْ فِي جِوَارِكَ وَلَا تَسْلُبْنَا وَإِيَّاهُمْ نِعْمَتَكَ وَلَا تُغَيِّرْ مَا بِنَا وَبِهِمْ مِنْ عَافِيَتِكَ يَا أَرْحَمَ الرَّاحِمِينَ.\n" +
                        "\n"
        ));
        bab1.sections.add(new SectionItem(
                "Solat Qasar",
                "Solat Qasar: Memendekkan solat fardhu 4 rakaat (Zuhur, Asar, Isyak) kepada 2 rakaat.\n" +
                        "\n" +
                        "Syarat Solat Qasar:\n" +
                        "\n" +
                        "- Perjalanan melebihi 2 marhalah (81 km)\n" +
                        "- Mengetahui destinasi\n" +
                        "- Dilaksanakan selepas melepasi daerah/sempadan bandar\n" +
                        "- Perjalanan adalah harus (bukan maksiat)\n" +
                        "- Tidak berniat bermusafir 4 hari atau lebih (tidak masuk hari pergi dan balik)\n" +
                        "\n" +
                        "Pilihan Pelaksanaan:\n" +
                        "\n" +
                        "- Qasar sahaja\n" +
                        "- Jamak sahaja\n" +
                        "- Gabungan Qasar dan Jamak\n" +
                        "Niat Solat Qasar:\n" +
                        "\n"
        ));
        bab1.sections.add(new SectionItem(
                "Solat Jamak",
                "SOLAT JAMAK : Menghimpunkan dua solat fardu dalam satu waktu.\n" +
                        "Solat yang boleh dijamak: Zuhur dengan Asar, dan Maghrib dengan Isyak. Subuh tidak boleh dijamak.\n" +
                        "Dua kaedah:\n" +
                        "\n" +
                        "1.Jamak Taqdim (dilakukan pada waktu pertama)\n" +
                        "- Syarat : Tertib (dahulukan solat waktu pertama), berniat menghimpunkan solat kedua bersama solat pertama di awal takbir atau sebelum salam solat pertama, berturut-turut (muwālat), dan perjalanan berterusan sehingga selesai solat kedua.\n" +
                        "\n" +
                        "\uF06CNiat Jamak Taqdim (tanpa qasar):\n" +
                        "\n" +
                        "- Niat solat Zuhur + Asar\n" +
                        "\n" +
                        "أُصَلِّي فَرْضَ الظُّهْرِ أَرْبَعَ رَكَعَاتٍ مَجْمُوعًا إِلَيْهِ الْعَصْرُ أَدَاءً لِلَّهِ تَعَالَى\n" +
                        "Sahaja aku solat fardhu Zuhur empat rakaat dihimpunkan solat Asar kepadanya tunai kerana Allah Ta’ala  "
        ));
        bab1.sections.add(new SectionItem(
                "Solat Jamak Beserta Qasar",
                "3.SOLAT JAMAK BESERTA QASAR :\n" +
                        "Boleh menghimpunkan dua solat sekaligus dengan memendekkan solat Zuhur, Asar dan Isyak kepada dua rakaat, mengikut syarat qasar dan jamak.\n" +
                        "\n" +
                        "\uF06CNiat bagi Jamak Taqdim & Qasar:\n" +
                        "\n" +
                        "Zuhur + Asar (di waktu Zuhur) – dahulukan Zuhur:\n" +
                        "\n" +
                        "-Niat Zuhur (qasar + jamak):\n" +
                        "\n" +
                        "أُصَلِّي فَرْضَ الظَّهْرِ رَكْعَتَيْنِ قَصْرًا مَجْمُوعًا إِلَيْهِ الْعَصْرُ أَدَاءً لِلَّهِ تَعَالَى\n" +
                        "Sahaja aku solat fardhu Zuhur dua rakaat qasar dihimpunkan Asar kepadanya tunai kerana Allah Ta’ala\n" +
                        "\n" +
                        "Selepas salam, niat Asar (qasar):\n" +
                        "\n" +
                        "أُصَلِّي فَرْضَ الْعَصْرِ رَكْعَتَيْنِ قَصْرًا أَدَاءً لِلَّهِ تَعَالَى\n" +
                        "Sahaja aku solat fardhu Asar dua rakaat qasar tunai kerana Allah Ta’ala"
        ));
        chapters.add(bab1);

        // ---- Bab 2 ----
        Chapter bab2 = new Chapter("Bab 2", "Pengenalan Umrah",
                "Ringkasan pendek pasal Bab 2 di sini.", true);
        bab2.sections.add(new SectionItem(
                "Pengenalan Umrah",
                "\n" +
                        "-Definisi: Ziarah ke Baitullah untuk ibadat dengan syarat tertentu.\n" +
                        "\n" +
                        "-Hukum: Wajib sekali seumur hidup bagi yang mampu.\n" +
                        "\n" +
                        "-Dalil: Hadis (umrah di Ramadhan menyamai haji) & Surah al-Baqarah 196.\n" +
                        "\n" +
                        "-Adab sebelum berangkat (14 perkara): Ikhlas, pelajari ilmu, taubat, bersihkan harta, selesaikan hak, perbaiki diri, jaga qaidah, sumber kewangan halal, bekalan cukup, cari keredhaan, pilih teman sesuai, doa minta umrah mabrur, solat musafir, zikir & tawakal.\n" +
                        "\n" +
                        "-Syarat Wajib: Islam, baligh, berakal, merdeka, perjalanan selamat, berkemampuan. Wanita: izin suami/wali & bersama mahram/wanita dipercayai."
        ));
        chapters.add(bab2);

        // ---- Bab 3: Pelaksanaan Ibadah Umrah ----
        Chapter bab3 = new Chapter("Bab 3", "Pelaksanaan Ibadah Umrah",
                "Langkah-langkah menunaikan ibadah umrah, dari ihram sehingga sa'i.", true);
        bab3.sections.add(new SectionItem(
                "Ihram",
                "\uF06CAmalan Sunat Sebelum Berniat Ihram\n" +
                        "\n" +
                        "-Mandi Sunat Ihram\n" +
                        "-Memakai Pakaian Ihram\n" +
                        "-Memakai Wangi-Wangian\n" +
                        "-Memakai Minyak Rambut\n" +
                        "-Melakukan Solat Sunat Ihram\n" +
                        "\n" +
                        "\uF06CAmalan Sunat Semasa Berniat Ihram\n" +
                        "\n" +
                        "-Menghadap ke kiblat \n" +
                        "-Dalam keadaan berwudhuk\n" +
                        "\n" +
                        "\uF06CAmalan Sunat Selepas Berniat Ihram\n" +
                        "\n" +
                        "-Bertalbiah, berdoa, bacaan Al-Quran, zikir \n" +
                        "-Elakkan berbual kosong\n" +
                        "\n" +
                        "\uF06CIhram Lelaki\n" +
                        "Memakai dua helai kain lepas (tidak bercantum):\n" +
                        "- Sehelai menutup aurat antara pusat dan lutut.\n" +
                        "- Sehelai lagi dijadikan selendang.\n" +
                        "-Disunatkan berwarna putih."
        ));
        bab3.sections.add(new SectionItem(
                "Rukun Umrah",
                "Niat Ihram Umrah\n" +
                        "Definisi: Kewajipan mematuhi larangan ihram bermula dari saat berniat sehingga selesai bergunting/bercukur.\n" +
                        "\n" +
                        "Waktu Berniat:\n" +
                        "-Waktu yang paling afdal (terbaik) ialah ketika jemaah sudah berada di atas kenderaan (kerana mencontohi Rasulullah SAW).\n" +
                        "-Boleh berniat selepas selesai solat sunat ihram.\n" +
                        "-Disunatkan menghadap kiblat ketika melafazkan niat."
        ));
        bab3.sections.add(new SectionItem(
                "Tawaf",
                "Cara Mengerjakan Tawaf\n" +
                        "\n" +
                        "Setibanya jemaah di dalam Masjid Haram untuk melaksanakan ibadah tawaf, mereka hendaklah menuju ke penjuru Hajar Aswad.\n" +
                        "\n" +
                        "Jemaah perlu berjalan menuju ke penjuru Hajar Aswad dengan menghadap ke arahnya dan sunat berniat tawaf.\n" +
                        "\n" +
                        "Lafaz Niat Tawaf Umrah\n" +
                        "\n" +
                        "اللَّهُمَّ إِنِّي أُرِيدُ طَوَافَ بَيْتِكَ الْحَرَامِ فَيَسِّرْهُ لِي وَتَقَبَّلْهُ مِنِّي سَبْعَةَ أَشْوَاطٍ طَوَافَ الْعُمْرَةِ لِلَّهِ تَعَالَى\n" +
                        "\n" +
                        "Setelah itu, hendaklah beristilam kepada Hajar Aswad dengan isyarat tangan. Ucapkanlah:\n" +
                        "\n" +
                        "بِسْمِ اللهِ وَاللهُ أَكْبَرُ وَلِلَّهِ الْحَمْدُ\n" +
                        "\n" +
                        "Jika tidak mampu menghampiri atas sebab orang yang begitu ramai dan sesak, maka beristilamlah kepadanya Hajar Aswad dengan membaca bacaan di atas.\n" +
                        "\n" +
                        "Bagi jemaah dalam ihram, jemaah tidak boleh menyentuh Hajar Aswad kerana ada wangian."
        ));
        bab3.sections.add(new SectionItem(
                "Sa'i",
                "Cara Mengerjakan Saʻie\n" +
                        "\n" +
                        "-Tiba di bukit Safa, Hadap ke arah Kaabah lalu berniat Saʻie.\n" +
                        "\n" +
                        "Lafaz Niat Saʻi Umrah:\n" +
                        "\n" +
                        "اللَّهُمَّ إِنِّي أُرِيدُ أَنْ أَسْعَى بَيْنَ الصَّفَا وَالْمَرُوَةِ سَبْعَةَ أَشْوَاطٍ سَعْيَ الْعُمْرَةِ لِلَّهِ تَعَالَى\n" +
                        "\n" +
                        "Selepas berniat (masih menghadap kiblat), disunatkan bertakbir :\n" +
                        "\n" +
                        "اللَّهُ أَكْبَرُ ، اللَّهُ أَكْبَرُ ، اللَّهُ أَكْبَرُ ، وَلِلَّهِ الْحَمْدُ\n" +
                        "\n" +
                        "Selepas takbir, disunatkan memulakan perjalanan ke bukit Marwah sambil membaca doa:\n" +
                        "\n" +
                        "اللَّهُ أَكْبَرُ عَلَى هَدَانَا وَالْحَمْدُ لِلَّهِ عَلَى مَا أَوْلَانَا لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ يُحْيِي وَيُمِيتُ بِيَدِهِ الْخَيْرُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ، لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ أَنْجَزَ وَعْدَهُ وَنَصَرَ عَبْدَهُ وَهَزَمَ الْأَحْزَابَ وَحْدَهُ لَا إِلَهَ إِلَّا اللَّهُ\n" +
                        "\n" +
                        "-Disunatkan berdoa untuk diri sendiri atau keluarga, sama ada urusan dunia atau akhirat, dengan doa yang difahami.\n" +
                        "\n" +
                        "-Kemudian : Memulakan perjalanan Saʻie ke bukit Marwah."
        ));
        chapters.add(bab3);

        // ---- Bab 4 ----
        Chapter bab4 = new Chapter("Bab 4", "Tawaf Wada’",
                "Ringkasan pendek pasal Bab 4 di sini.", true);
        bab4.sections.add(new SectionItem(
                "Tempoh Menunggu Selepas Tawaf Wada'",
                "\uF06CTempoh Menunggu Selepas Tawaf Wada':\n" +
                        "\n" +
                        "8 jam: Dalam keadaan biasa, dikira dari sampai di penginapan. Jika tidak keluar Makkah dalam 8 jam tanpa sebab musafir, wajib ulang tawaf.\n" +
                        "\n" +
                        "24 jam: Jika kelewatan kenderaan dimaklumkan dalam 24 jam selepas tawaf, tidak perlu ulang. Dikira dari sampai di penginapan.\n"
        ));
        bab4.sections.add(new SectionItem(
                "Tajuk Section 2",
                "Isi ringkas section 2 di sini."
        ));
        chapters.add(bab4);

// ---- Bab 5 ----
        Chapter bab5 = new Chapter("Bab 5", "Tajuk Bab 5 Di Sini",
                "Ringkasan pendek pasal Bab 5 di sini.", true);
        bab5.sections.add(new SectionItem(
                "Tajuk Section 1",
                "Isi ringkas section 1 di sini."
        ));
        bab5.sections.add(new SectionItem(
                "Tajuk Section 2",
                "Isi ringkas section 2 di sini."
        ));
        chapters.add(bab5);

// ---- Bab 6 ----
        Chapter bab6 = new Chapter("Bab 6", "Tajuk Bab 6 Di Sini",
                "Ringkasan pendek pasal Bab 6 di sini.", true);
        bab6.sections.add(new SectionItem(
                "Tajuk Section 1",
                "Isi ringkas section 1 di sini."
        ));
        bab6.sections.add(new SectionItem(
                "Tajuk Section 2",
                "Isi ringkas section 2 di sini."
        ));
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
            tab.setTextSize(13);
            tab.setTypeface(null, Typeface.BOLD);
            tab.setPadding(dp(18), dp(9), dp(18), dp(9));
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

        chapterHeroTitle.setText(chapter.heroTitle);
        chapterHeroBlurb.setText(chapter.heroBlurb);

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
        itemParams.bottomMargin = dp(10);
        item.setLayoutParams(itemParams);
        int padding = dp(14);
        item.setPadding(padding, padding, padding, padding);
        item.setClickable(true);
        item.setFocusable(true);

        TextView title = new TextView(this);
        title.setText(section.title);
        title.setTextSize(15);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(getResources().getColor(R.color.text_dark));

        TextView summary = new TextView(this);
        summary.setText(section.summary);
        summary.setTextSize(13);
        summary.setTextColor(getResources().getColor(R.color.text_gray));
        summary.setLineSpacing(dp(2), 1f);
        summary.setVisibility(View.GONE);
        LinearLayout.LayoutParams summaryParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        summaryParams.topMargin = dp(8);
        summary.setLayoutParams(summaryParams);

        item.addView(title);
        item.addView(summary);

        item.setOnClickListener(v ->
                summary.setVisibility(summary.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));

        return item;
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}