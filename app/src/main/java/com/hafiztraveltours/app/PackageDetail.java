package com.hafiztraveltours.app;

import java.util.ArrayList;
import java.util.List;

public class PackageDetail {

    public String id;
    public String name;
    public String summaryLine;
    public int durationDays;
    public int nightsCount;
    public String price;
    public String imageUrl;
    public String posterImageUrl;

    public List<NightBreakdown> nightsBreakdown = new ArrayList<>();
    public String departureDatesNote;
    public List<HotelInfo> hotels = new ArrayList<>();
    public List<ItineraryDay> itinerary = new ArrayList<>();
    public List<ImportantNote> importantNotes = new ArrayList<>();
    public List<String> included = new ArrayList<>();
    public List<String> excluded = new ArrayList<>();
    public List<String> packingSummer = new ArrayList<>();
    public List<String> packingWinter = new ArrayList<>();
    public List<PriceOption> priceOptions = new ArrayList<>();
    public List<String> galleryImageUrls = new ArrayList<>();
    public String whatsappMessage;

    public static class NightBreakdown {
        public String city;
        public int nights;
        public NightBreakdown(String city, int nights) {
            this.city = city;
            this.nights = nights;
        }
    }

    public static class HotelInfo {
        public String type; // "mekah" | "madinah" | "flight"
        public String title;
        public String subtitle;
        public HotelInfo(String type, String title, String subtitle) {
            this.type = type;
            this.title = title;
            this.subtitle = subtitle;
        }
    }

    public static class ItineraryDay {
        public int dayNumber;
        public String dayLabel;
        public String tag;
        public String timeNote;
        public String title;
        public String routeText;
        public List<String> highlights = new ArrayList<>();
        public List<String> activities = new ArrayList<>();
        public String hotelNote;
        public String mealNote;
    }

    public static class ImportantNote {
        public String title;
        public String badge;
        public List<String> bullets = new ArrayList<>();
    }

    public static class PriceOption {
        public String price;
        public String occupancyLabel;
        public PriceOption(String price, String occupancyLabel) {
            this.price = price;
            this.occupancyLabel = occupancyLabel;
        }
    }

    public static PackageDetail fromUmrahPackage(UmrahPackage pkg) {
        PackageDetail d = new PackageDetail();
        if (pkg == null) return d;

        d.id = pkg.id;
        d.name = pkg.getDisplayName();
        d.summaryLine = pkg.summary != null && !pkg.summary.trim().isEmpty()
                ? pkg.summary
                : "Nikmati pengalaman perjalanan penuh keselesaan dengan fasiliti terbaik dan bimbingan profesional.";
        d.durationDays = pkg.durationDays > 0 ? pkg.durationDays : 12;
        d.nightsCount = pkg.nightsCount > 0 ? pkg.nightsCount : (d.durationDays - 1);
        d.price = pkg.price != null && !pkg.price.trim().isEmpty() ? pkg.price : "RM 8,990";
        d.imageUrl = pkg.imageUrl != null && !pkg.imageUrl.trim().isEmpty()
                ? pkg.imageUrl
                : "https://images.unsplash.com/photo-1591604129939-f1efa4d9f7fa?w=800&q=80";
        d.posterImageUrl = d.imageUrl;
        d.whatsappMessage = "Salam, saya berminat untuk mengetahui lebih lanjut mengenai " + d.name + " (" + d.price + "). Boleh kongsikan jadual dan kekosongan terkini?";

        boolean isUmrah = ("umrah".equalsIgnoreCase(pkg.category) ||
                (pkg.collectionName != null && pkg.collectionName.contains("umrah")) ||
                d.name.toLowerCase().contains("umrah") ||
                d.name.toLowerCase().contains("ramadhan") ||
                d.name.toLowerCase().contains("syawal"));

        String nameLower = d.name.toLowerCase();

        if (isUmrah) {
            // Umrah Package Sample Details
            d.nightsBreakdown.add(new NightBreakdown("Makkah", 7));
            d.nightsBreakdown.add(new NightBreakdown("Madinah", 5));
            if (nameLower.contains("vip") || nameLower.contains("taif")) {
                d.nightsBreakdown.add(new NightBreakdown("Taif", 1));
            }

            d.departureDatesNote = "Penerbangan dibuka setiap minggu sepanjang musim 1448H / 2026. Tempat adalah terhad mengikut kuota visa.";

            d.hotels.add(new HotelInfo("mekah", "Hotel Makkah (5 Bintang)", "Pullman Zamzam Tower / Movenpick Hajar Hotel (50 meter dari perkarangan Masjidil Haram)"));
            d.hotels.add(new HotelInfo("madinah", "Hotel Madinah (5 Bintang)", "Frontel Al Harithia / Anwar Al Madinah (100 meter dari perkarangan Masjid Nabawi)"));
            d.hotels.add(new HotelInfo("flight", "Penerbangan Terus Eksklusif", "Saudia Airlines / Malaysia Airlines (Penerbangan Terus KLIA - Jeddah/Madinah)"));

            // Itinerary
            ItineraryDay d1 = new ItineraryDay();
            d1.dayNumber = 1;
            d1.dayLabel = "Hari 1";
            d1.tag = "Penerbangan & Ketibaan";
            d1.title = "Kuala Lumpur (KLIA) ✈️ Jeddah ➔ Makkah";
            d1.routeText = "KLIA - King Abdulaziz Airport - Makkah Al-Mukarramah";
            d1.activities.add("Berkumpul di KLIA 4 jam sebelum waktu perlepasan.");
            d1.activities.add("Penerbangan terus ke Jeddah. Berniat Ihram & Talbiah di Miqat Qarnul Manazil di atas pesawat.");
            d1.activities.add("Ketibaan di Jeddah, urusan imigresen dan menaiki bas persiaran berhawa dingin ke Makkah.");
            d1.activities.add("Daftar masuk hotel di Makkah dan berehat seketika.");
            d1.activities.add("Melaksanakan Ibadah Umrah Pertama (Tawaf, Sa'ie & Tahallul) dibimbing oleh Mutawwif.");
            d1.hotelNote = "Pullman Zamzam Makkah";
            d1.mealNote = "Makan di dalam penerbangan & Makan Malam di Hotel";
            d.itinerary.add(d1);

            ItineraryDay d2 = new ItineraryDay();
            d2.dayNumber = 2;
            d2.dayLabel = "Hari 2";
            d2.tag = "Ibadah Makkah";
            d2.title = "Ibadah & Iktikaf di Masjidil Haram";
            d2.routeText = "Masjidil Haram Makkah";
            d2.activities.add("Solat fardhu berjemaah 5 waktu di Masjidil Haram.");
            d2.activities.add("Memperbanyakkan tawaf sunat, bacaan Al-Quran dan doa di hadapan Kaabah.");
            d2.activities.add("Sesi tazkirah rohani dan bimbingan bersama Mutawwif selepas solat Isyak.");
            d2.hotelNote = "Pullman Zamzam Makkah";
            d2.mealNote = "Sarapan, Makan Tengah Hari & Makan Malam di Hotel";
            d.itinerary.add(d2);

            ItineraryDay d3 = new ItineraryDay();
            d3.dayNumber = 3;
            d3.dayLabel = "Hari 3";
            d3.tag = "Ziarah Luar Makkah";
            d3.title = "Ziarah Manasik Haji & Umrah Kedua";
            d3.routeText = "Jabal Thur - Arafah - Muzdalifah - Mina - Jabal Rahmah - Miqat Ja'ranah";
            d3.activities.add("Ziarah Jabal Thur (tempat persembunyian Rasulullah SAW & Abu Bakar RA).");
            d3.activities.add("Melawat Padang Arafah, Jabal Rahmah, tapak khemah Mina dan Muzdalifah.");
            d3.activities.add("Singgah di Miqat Ja'ranah untuk berniat Ihram bagi jemaah yang ingin melakukan Umrah Kedua.");
            d3.activities.add("Kembali ke Masjidil Haram untuk menyempurnakan Tawaf, Sa'ie dan Tahallul.");
            d3.hotelNote = "Pullman Zamzam Makkah";
            d3.mealNote = "Sarapan, Makan Tengah Hari & Makan Malam di Hotel";
            d.itinerary.add(d3);

            ItineraryDay d4 = new ItineraryDay();
            d4.dayNumber = 4;
            d4.dayLabel = "Hari 4";
            d4.tag = "Ziarah Istimewa Taif";
            d4.title = "Lawatan Berhawa Sejuk ke Kota Taif";
            d4.routeText = "Makkah - Taif - Qarnul Manazil - Makkah";
            d4.activities.add("Perjalanan menaiki bas ke tanah tinggi Taif yang nyaman.");
            d4.activities.add("Melawat Masjid Abdullah Ibn Abbas dan Masjid Ku'a.");
            d4.activities.add("Melawat Kilang Penyulingan Minyak & Air Ros Taif yang terkenal.");
            d4.activities.add("Menikmati juadah Nasi Mandhi tradisi Arab di Taif.");
            d4.activities.add("Singgah di Miqat Qarnul Manazil (As-Sail Al-Kabir) untuk berniat Umrah Ketiga.");
            d4.hotelNote = "Pullman Zamzam Makkah";
            d4.mealNote = "Sarapan Hotel, Makan Tengah Hari Taif & Makan Malam Hotel";
            d.itinerary.add(d4);

            ItineraryDay d5 = new ItineraryDay();
            d5.dayNumber = 5;
            d5.dayLabel = "Hari 5";
            d5.tag = "Perjalanan Haramain Train";
            d5.title = "Makkah ➔ Madinah Munawwarah";
            d5.routeText = "Masjidil Haram - Haramain High Speed Rail - Madinah";
            d5.activities.add("Melaksanakan Tawaf Wada' (Tawaf Selamat Tinggal) di Masjidil Haram.");
            d5.activities.add("Daftar keluar hotel dan bertolak ke stesen Kereta Api Laju Haramain.");
            d5.activities.add("Perjalanan pantas dan selesa ke Madinah (kira-kira 2 jam perjalanan).");
            d5.activities.add("Ketibaan di Madinah, daftar masuk hotel dan solat berjemaah di Masjid Nabawi.");
            d5.hotelNote = "Frontel Al Harithia Madinah";
            d5.mealNote = "Sarapan Hotel, Makan Tengah Hari & Makan Malam di Madinah";
            d.itinerary.add(d5);

            ItineraryDay d6 = new ItineraryDay();
            d6.dayNumber = 6;
            d6.dayLabel = "Hari 6";
            d6.tag = "Ziarah Raudhah";
            d6.title = "Ziarah Makam Rasulullah SAW & Raudhah";
            d6.routeText = "Masjid Nabawi - Makam Rasulullah SAW - Perkuburan Baqi'";
            d6.activities.add("Ziarah Makam Rasulullah SAW, Sayyidina Abu Bakar As-Siddiq RA & Sayyidina Umar Al-Khattab RA.");
            d6.activities.add("Ziarah Perkuburan Baqi' bersebelahan Masjid Nabawi.");
            d6.activities.add("Sesi masuk ke Raudhah (Taman Syurga) mengikut slot permit rasmi Nusuk.");
            d6.activities.add("Solat dan munajat di Raudhah bersama bimbingan Mutawwif.");
            d6.hotelNote = "Frontel Al Harithia Madinah";
            d6.mealNote = "Sarapan, Makan Tengah Hari & Makan Malam di Madinah";
            d.itinerary.add(d6);

            ItineraryDay d7 = new ItineraryDay();
            d7.dayNumber = 7;
            d7.dayLabel = "Hari 7";
            d7.tag = "Ziarah Luar Madinah";
            d7.title = "Ziarah Bersejarah Kota Madinah";
            d7.routeText = "Masjid Quba - Jabal Uhud - Masjid Qiblatain - Ladang Kurma";
            d7.activities.add("Melawat Masjid Quba (masjid pertama dalam Islam, solat sunat bernilai pahala 1 umrah).");
            d7.activities.add("Ziarah Jabal Uhud dan Makam 70 Syuhada Perang Uhud termasuk Sayyidina Hamzah RA.");
            d7.activities.add("Melawat Masjid Qiblatain (masjid dua kiblat) dan kawasan Perang Khandaq.");
            d7.activities.add("Singgah membeli-belah di Pasar Kurma Madinah segar.");
            d7.hotelNote = "Frontel Al Harithia Madinah";
            d7.mealNote = "Sarapan, Makan Tengah Hari & Makan Malam di Madinah";
            d.itinerary.add(d7);

            ItineraryDay d8 = new ItineraryDay();
            d8.dayNumber = 8;
            d8.dayLabel = "Hari 8";
            d8.tag = "Kepulangan";
            d8.title = "Madinah ➔ Jeddah ✈️ Kuala Lumpur (KLIA)";
            d8.routeText = "Masjid Nabawi - Jeddah Airport - KLIA";
            d8.activities.add("Ziarah Wada' di Masjid Nabawi.");
            d8.activities.add("Daftar keluar hotel dan bertolak ke Lapangan Terbang.");
            d8.activities.add("Penerbangan pulang ke Kuala Lumpur membawa seribu kenangan manis & Umrah yang Mabrur.");
            d8.hotelNote = "Penerbangan Pulang";
            d8.mealNote = "Sarapan Hotel & Makanan di Penerbangan";
            d.itinerary.add(d8);

            // Important Notes
            ImportantNote n1 = new ImportantNote();
            n1.title = "Syarat Pasport & Visa";
            n1.badge = "Penting";
            n1.bullets.add("Pasport antarabangsa perlu sah sekurang-kurangnya 6 bulan dari tarikh berlepas.");
            n1.bullets.add("Salinan pasport dan gambar berukuran pasport berlatar belakang putih.");
            n1.bullets.add("Visa Umrah rasmi diuruskan sepenuhnya oleh pihak syarikat.");
            d.importantNotes.add(n1);

            ImportantNote n2 = new ImportantNote();
            n2.title = "Kesihatan & Vaksinasi";
            n2.badge = "Kesihatan";
            n2.bullets.add("Wajib suntikan Meningococcal Meningitis ACYW135.");
            n2.bullets.add("Dos lengkap vaksinasi mengikut garis panduan Kementerian Kesihatan Arab Saudi.");
            n2.bullets.add("Sila bawa bekalan ubat-ubatan peribadi yang mencukupi untuk sepanjang tempoh perjalanan.");
            d.importantNotes.add(n2);

            // Inclusions
            d.included.add("Tiket Penerbangan Pergi & Balik (Penerbangan Terus)");
            d.included.add("Penginapan Hotel 5 Bintang di Makkah & Madinah");
            d.included.add("Visa Umrah & Insurans Perlindungan Perjalanan Komprehensif");
            d.included.add("Makan Minum Fullboard (Sarapan, Makan Tengah Hari & Makan Malam)");
            d.included.add("Tiket Kereta Api Berkelajuan Tinggi Haramain Train");
            d.included.add("Pengangkutan Bas Persiaran Berhawa Dingin VIP");
            d.included.add("Bimbingan Mutawwif Berpengalaman & Bertauliah Sepenuh Masa");
            d.included.add("Set Bagasi Eksklusif Hafiz Travel (Beg 24\", Beg Sandang, Beg Kasut, Beg Pasport)");
            d.included.add("Air Zamzam 5 Liter (Tertakluk kepada pelepasan pihak berkuasa)");
            d.included.add("Kursus Bimbingan Ibadah Umrah Intensif Percuma");

            // Exclusions
            d.excluded.add("Perbelanjaan peribadi (dobi, telefon, snek peribadi)");
            d.excluded.add("Caj lebihan berat bagasi penerbangan");
            d.excluded.add("Perkhidmatan upah tolak kerusi roda semasa Tawaf dan Sa'ie");

            // Packing guides
            d.packingSummer.add("2 Pasang Kain Ihram & Tali Pinggang Ihram");
            d.packingSummer.add("Kasut atau selipar bertutup yang selesa");
            d.packingSummer.add("Buku Panduan Doa & Telekung (untuk Muslimah)");
            d.packingSummer.add("Losyen pelembap, sabun & syampu tanpa wangian");
            d.packingSummer.add("Ubat-ubatan peribadi & cermin mata hitam");

            // Pricing room options
            d.priceOptions.add(new PriceOption(d.price, "Bilik Berempat (Quad)"));
            d.priceOptions.add(new PriceOption("RM 12,300", "Bilik Bertiga (Triple)"));
            d.priceOptions.add(new PriceOption("RM 13,800", "Bilik Berdua (Double)"));

            // Gallery
            d.galleryImageUrls.add("https://images.unsplash.com/photo-1591604129939-f1efa4d9f7fa?w=800&q=80");
            d.galleryImageUrls.add("https://images.unsplash.com/photo-1564769625905-50e93615e769?w=800&q=80");
            d.galleryImageUrls.add("https://images.unsplash.com/photo-1565552684305-7e8cb0693a26?w=800&q=80");
            d.galleryImageUrls.add("https://images.unsplash.com/photo-1542838132-92c53300491e?w=800&q=80");

        } else if (nameLower.contains("turki") || nameLower.contains("turkey")) {
            // Turkey Tour Details
            d.nightsBreakdown.add(new NightBreakdown("Istanbul", 3));
            d.nightsBreakdown.add(new NightBreakdown("Cappadocia", 2));
            d.nightsBreakdown.add(new NightBreakdown("Pamukkale", 1));
            d.nightsBreakdown.add(new NightBreakdown("Bursa", 1));

            d.departureDatesNote = "Penerbangan dibuka untuk musim Bunga (Spring), Musim Luruh (Autumn) & Musim Sejuk (Winter).";

            d.hotels.add(new HotelInfo("hotel", "Crowne Plaza Istanbul Old City (5⭐)", "Terletak berhampiran Grand Bazaar & Blue Mosque"));
            d.hotels.add(new HotelInfo("hotel", "Dinler Cave Hotel Cappadocia (5⭐)", "Pengalaman menginap di hotel gua eksklusif dengan pemandangan lembah"));
            d.hotels.add(new HotelInfo("hotel", "Colossae Thermal Hotel Pamukkale (5⭐)", "Kemudahan kolam air panas mineral semula jadi"));

            ItineraryDay t1 = new ItineraryDay();
            t1.dayNumber = 1;
            t1.dayLabel = "Hari 1";
            t1.tag = "Penerbangan";
            t1.title = "Kuala Lumpur ✈️ Istanbul";
            t1.routeText = "KLIA - Istanbul Grand Airport";
            t1.activities.add("Berkumpul di KLIA dan berlepas ke Istanbul menaiki Turkish Airlines / Emirates.");
            t1.activities.add("Ketibaan di Istanbul, disambut pemandu pelancong dan daftar masuk hotel.");
            t1.hotelNote = "Crowne Plaza Istanbul";
            t1.mealNote = "Makan di dalam penerbangan & Makan Malam";
            d.itinerary.add(t1);

            ItineraryDay t2 = new ItineraryDay();
            t2.dayNumber = 2;
            t2.dayLabel = "Hari 2";
            t2.tag = "Kota Bersejarah";
            t2.title = "Istanbul ➔ Bursa ➔ Kusadasi";
            t2.routeText = "Blue Mosque - Hagia Sophia - Grand Mosque Bursa";
            t2.activities.add("Melawat Blue Mosque (Sultanahmet Camii) & Hagia Sophia.");
            t2.activities.add("Menyeberang Laut Marmara ke Bursa menaiki feri.");
            t2.activities.add("Melawat Grand Mosque Bursa dan Pasar Sutera Koza Han.");
            t2.hotelNote = "Hotel Kusadasi (5⭐)";
            t2.mealNote = "Sarapan, Makan Tengah Hari & Makan Malam Halal";
            d.itinerary.add(t2);

            ItineraryDay t3 = new ItineraryDay();
            t3.dayNumber = 3;
            t3.dayLabel = "Hari 3";
            t3.tag = "Keahiban Semula Jadi";
            t3.title = "Kusadasi ➔ Pamukkale (Cotton Castle)";
            t3.routeText = "Ephesus - Hierapolis - Pamukkale Travertines";
            t3.activities.add("Melawat kota purba Ephesus & Teater Purba Greco-Roman.");
            t3.activities.add("Meneruskan perjalanan ke Pamukkale, melawat Cotton Castle berteras putih.");
            t3.activities.add("Mandi air panas mineral di Kolam Antik Cleopatra.");
            t3.hotelNote = "Colossae Thermal Pamukkale (5⭐)";
            t3.mealNote = "Sarapan, Makan Tengah Hari & Makan Malam Halal";
            d.itinerary.add(t3);

            ItineraryDay t4 = new ItineraryDay();
            t4.dayNumber = 4;
            t4.dayLabel = "Hari 4";
            t4.tag = "Lembah Dongeng";
            t4.title = "Pamukkale ➔ Konya ➔ Cappadocia";
            t4.routeText = "Konya Mevlana Museum - Caravanserai - Cappadocia";
            t4.activities.add("Melawat Muzium Mevlana Rumi di Konya.");
            t4.activities.add("Singgah di Caravanserai Sultanhani (hotel pedagang Laluan Sutera).");
            t4.activities.add("Ketibaan di Cappadocia, tanah ajaib formasi batuan gunung berapi.");
            t4.hotelNote = "Cave Hotel Cappadocia (5⭐)";
            t4.mealNote = "Sarapan, Makan Tengah Hari & Makan Malam Halal";
            d.itinerary.add(t4);

            ItineraryDay t5 = new ItineraryDay();
            t5.dayNumber = 5;
            t5.dayLabel = "Hari 5";
            t5.tag = "Hot Air Balloon";
            t5.title = "Penerbangan Belon Udara Panas & Lembah Goreme";
            t5.routeText = "Hot Air Balloon Sunrise - Goreme - Kaymakli Underground City";
            t5.activities.add("Pilihan menaiki Hot Air Balloon menikmati matahari terbit menakjubkan.");
            t5.activities.add("Melawat Lembah Goreme, Uchisar Rock Castle & Pigeon Valley.");
            t5.activities.add("Meneroka Kota Bawah Tanah Kaymakli 8 tingkat.");
            t5.hotelNote = "Cave Hotel Cappadocia (5⭐)";
            t5.mealNote = "Sarapan, Makan Tengah Hari & Makan Malam Halal";
            d.itinerary.add(t5);

            ItineraryDay t6 = new ItineraryDay();
            t6.dayNumber = 6;
            t6.dayLabel = "Hari 6";
            t6.tag = "Bosphorus Cruise";
            t6.title = "Cappadocia ➔ Istanbul Bosphorus";
            t6.routeText = "Ankara Tuz Golu - Istanbul Private Bosphorus Cruise";
            t6.activities.add("Singgah di Tasik Garam Tuz Golu berlatar pemandangan putih memukau.");
            t6.activities.add("Ketibaan di Istanbul, menaiki Private Bosphorus Cruise menyusuri selat antara Asia & Eropah.");
            t6.activities.add("Membeli-belah di Spice Bazaar & Grand Bazaar.");
            t6.hotelNote = "Crowne Plaza Istanbul (5⭐)";
            t6.mealNote = "Sarapan, Makan Tengah Hari & Makan Malam Halal";
            d.itinerary.add(t6);

            ItineraryDay t7 = new ItineraryDay();
            t7.dayNumber = 7;
            t7.dayLabel = "Hari 7";
            t7.tag = "Kepulangan";
            t7.title = "Istanbul ✈️ Kuala Lumpur";
            t7.routeText = "Taksim Square - Istanbul Grand Airport - KLIA";
            t7.activities.add("Masa bebas di Taksim Square & Istiklal Street.");
            t7.activities.add("Bertolak ke Lapangan Terbang Antarabangsa Istanbul untuk penerbangan pulang.");
            t7.hotelNote = "Penerbangan Pulang";
            t7.mealNote = "Sarapan Hotel & Makanan di Penerbangan";
            d.itinerary.add(t7);

            // Inclusions
            d.included.add("Tiket Penerbangan Antarabangsa Pergi & Balik");
            d.included.add("Penginapan Hotel 5 Bintang Sepanjang Perjalanan");
            d.included.add("Makan Minum 100% Halal Diiktiraf (Fullboard)");
            d.included.add("Private Bosphorus Cruise Eksklusif");
            d.included.add("Tiket Masuk Semua Tarikan Pelancongan Utama");
            d.included.add("Pemandu Pelancong Profesional Berbahasa Melayu/Inggeris");
            d.included.add("Pengangkutan Bas Persiaran Berhawa Dingin VIP dengan WiFi Percuma");
            d.included.add("Insurans Perlindungan Perjalanan Komprehensif");

            // Exclusions
            d.excluded.add("Penerbangan Hot Air Balloon Cappadocia (Pilihan / Optional)");
            d.excluded.add("Perbelanjaan peribadi dan tipping pemandu pelancong");

            // Price Options
            d.priceOptions.add(new PriceOption(d.price, "Bilik Berdua (Twin / Double)"));
            d.priceOptions.add(new PriceOption("RM 4,990", "Kanak-Kanak Tanpa Katil"));
            d.priceOptions.add(new PriceOption("RM 6,790", "Bilik Perseorangan (Single)"));

            // Gallery
            d.galleryImageUrls.add("https://images.unsplash.com/photo-1541432901042-2d8bd64b4a9b?w=800&q=80");
            d.galleryImageUrls.add("https://images.unsplash.com/photo-1524231757912-21f4fe3a7200?w=800&q=80");
            d.galleryImageUrls.add("https://images.unsplash.com/photo-1527838832700-5059252407fa?w=800&q=80");
            d.galleryImageUrls.add("https://images.unsplash.com/photo-1570939274717-7eda259b50ed?w=800&q=80");

        } else {
            // General / Asian / European Tour Details
            d.nightsBreakdown.add(new NightBreakdown("Bandar Utama", 4));
            d.nightsBreakdown.add(new NightBreakdown("Destinasi Alam", 3));

            d.departureDatesNote = "Penerbangan dibuka mengikut musim percutian sekolah dan cuti umum.";

            d.hotels.add(new HotelInfo("hotel", "Hotel Antarabangsa (4/5 Bintang)", "Bilik selesa dengan kemudahan moden berhampiran pusat tumpuan"));
            d.hotels.add(new HotelInfo("hotel", "Resort Percutian Premium", "Penginapan santai dengan pemandangan semula jadi menakjubkan"));

            ItineraryDay g1 = new ItineraryDay();
            g1.dayNumber = 1;
            g1.dayLabel = "Hari 1";
            g1.tag = "Ketibaan";
            g1.title = "Kuala Lumpur ✈️ Destinasi";
            g1.routeText = "KLIA - Lapangan Terbang Destinasi";
            g1.activities.add("Berkumpul di KLIA dan berlepas ke destinasi percutian idaman.");
            g1.activities.add("Ketibaan, disambut pemandu pelancong mesra dan daftar masuk hotel.");
            g1.hotelNote = "Hotel Utama (4/5⭐)";
            g1.mealNote = "Makan di dalam penerbangan & Makan Malam Halal";
            d.itinerary.add(g1);

            ItineraryDay g2 = new ItineraryDay();
            g2.dayNumber = 2;
            g2.dayLabel = "Hari 2";
            g2.tag = "Lawatan Bandar";
            g2.title = "Jelajah Tarikan Utama & Budaya";
            g2.routeText = "Pusat Bandar - Mercu Tanda Terkenal - Tapak Warisan";
            g2.activities.add("Melawat mercu tanda ikonik dan tapak warisan bersejarah.");
            g2.activities.add("Menikmati juadah halal tempatan yang lazat.");
            g2.activities.add("Sesi bergambar di lokasi pemandangan paling popular.");
            g2.hotelNote = "Hotel Utama (4/5⭐)";
            g2.mealNote = "Sarapan, Makan Tengah Hari & Makan Malam Halal";
            d.itinerary.add(g2);

            ItineraryDay g3 = new ItineraryDay();
            g3.dayNumber = 3;
            g3.dayLabel = "Hari 3";
            g3.tag = "Alam Semula Jadi";
            g3.title = "Eksplorasi Keindahan Alam & Taman Tema";
            g3.routeText = "Tanah Tinggi / Pantai - Taman Rekreasi";
            g3.activities.add("Menghirup udara segar dan menikmati pemandangan alam memukau.");
            g3.activities.add("Aktiviti santai bersama keluarga dan rakan-rakan.");
            g3.hotelNote = "Resort Percutian (4/5⭐)";
            g3.mealNote = "Sarapan, Makan Tengah Hari & Makan Malam Halal";
            d.itinerary.add(g3);

            ItineraryDay g4 = new ItineraryDay();
            g4.dayNumber = 4;
            g4.dayLabel = "Hari 4";
            g4.tag = "Membeli-belah & Kepulangan";
            g4.title = "Masa Bebas Membeli-belah ✈️ Kuala Lumpur";
            g4.routeText = "Pasar Tempatan / Mall - Lapangan Terbang - KLIA";
            g4.activities.add("Membeli-belah cenderamata unik dan barangan tempatan.");
            g4.activities.add("Bertolak ke Lapangan Terbang untuk penerbangan pulang ke tanah air.");
            g4.hotelNote = "Penerbangan Pulang";
            g4.mealNote = "Sarapan Hotel & Makanan Penerbangan";
            d.itinerary.add(g4);

            // Inclusions
            d.included.add("Tiket Penerbangan Antarabangsa Pergi & Balik");
            d.included.add("Penginapan Hotel 4/5 Bintang Sepanjang Lawatan");
            d.included.add("Semua Makanan Halal (Sarapan, Makan Tengah Hari, Makan Malam)");
            d.included.add("Tiket Masuk ke Semua Tempat Lawatan Berjadual");
            d.included.add("Pengangkutan Bas Persiaran Berhawa Dingin Selesa");
            d.included.add("Pemandu Pelancong Berpengalaman & Mesra Muslim");
            d.included.add("Insurans Perlindungan Perjalanan Komprehensif");

            // Exclusions
            d.excluded.add("Perbelanjaan peribadi (dobi, snek tambahan, cenderamata)");
            d.excluded.add("Aktiviti pilihan luar jadual (*optional tours*)");

            // Price Options
            d.priceOptions.add(new PriceOption(d.price, "Bilik Berdua (Twin Sharing)"));
            d.priceOptions.add(new PriceOption("Diskaun 15%", "Kanak-Kanak Dengan Katil"));

            // Gallery
            d.galleryImageUrls.add(d.imageUrl);
            d.galleryImageUrls.add("https://images.unsplash.com/photo-1541432901042-2d8bd64b4a9b?w=800&q=80");
            d.galleryImageUrls.add("https://images.unsplash.com/photo-1591604129939-f1efa4d9f7fa?w=800&q=80");
        }

        return d;
    }
}
