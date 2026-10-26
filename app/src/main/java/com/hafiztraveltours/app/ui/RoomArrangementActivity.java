package com.hafiztraveltours.app.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.models.BookingRequest;
import com.hafiztraveltours.app.utils.HapticUtil;
import com.hafiztraveltours.app.utils.PackagePricingCalculator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class RoomArrangementActivity extends BaseActivity {

    public static final String EXTRA_BOOKING_REQUEST = "extra_booking_request";

    public static class TravellerSlot implements Serializable {
        public int index;
        public String displayName;
        public String gender; // "Male" or "Female"
        public String type; // "Adult", "Child", "Infant"
        public String dateOfBirth = "";
        public int age = 0;
        public Boolean withBed = null;
        public boolean isLead = false;

        public boolean isMale() {
            return "Male".equalsIgnoreCase(gender);
        }

        public boolean isFemale() {
            return "Female".equalsIgnoreCase(gender);
        }
    }

    private BookingRequest bookingRequest;
    private final List<TravellerSlot> travellerSlots = new ArrayList<>();
    private final List<BookingRequest.RoomConfig> rooms = new ArrayList<>();

    private TextView txtPackageName;
    private TextView badgePackageType;
    private TextView txtDepartureDate;
    private TextView txtTotalTravellersHeader;
    private TextView txtTotalRoomsCount;

    // Family Room Option card views
    private SwitchMaterial switchFamilyRoom;
    private TextView txtFamilyRoomStatusBadge;
    private TextView txtFamilyRoomPolicyDesc;

    // Unassigned section
    private LinearLayout cardUnassignedTravellers;
    private TextView txtUnassignedTitle;
    private TextView badgeUnassignedCount;
    private LinearLayout containerUnassignedTravellers;

    // Rooms list container
    private LinearLayout containerRoomsList;
    private TextView txtValidationWarning;
    private View btnProceedToTravellers;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_arrangement);

        bookingRequest = (BookingRequest) getIntent().getSerializableExtra(EXTRA_BOOKING_REQUEST);
        if (bookingRequest == null) {
            finish();
            return;
        }

        initViews();
        setupTravellerSlots();
        setupInitialRooms();
        renderUi();
    }

    private void initViews() {
        findViewById(R.id.btnBack).setOnClickListener(v -> {
            HapticUtil.click(v);
            finish();
        });

        txtPackageName = findViewById(R.id.txtPackageName);
        badgePackageType = findViewById(R.id.badgePackageType);
        txtDepartureDate = findViewById(R.id.txtDepartureDate);
        txtTotalTravellersHeader = findViewById(R.id.txtTotalTravellersHeader);
        txtTotalRoomsCount = findViewById(R.id.txtTotalRoomsCount);

        switchFamilyRoom = findViewById(R.id.switchFamilyRoom);
        txtFamilyRoomStatusBadge = findViewById(R.id.txtFamilyRoomStatusBadge);
        txtFamilyRoomPolicyDesc = findViewById(R.id.txtFamilyRoomPolicyDesc);

        cardUnassignedTravellers = findViewById(R.id.cardUnassignedTravellers);
        txtUnassignedTitle = findViewById(R.id.txtUnassignedTitle);
        badgeUnassignedCount = findViewById(R.id.badgeUnassignedCount);
        containerUnassignedTravellers = findViewById(R.id.containerUnassignedTravellers);
        containerRoomsList = findViewById(R.id.containerRoomsList);
        txtValidationWarning = findViewById(R.id.txtValidationWarning);
        btnProceedToTravellers = findViewById(R.id.btnProceedToTravellers);

        if (switchFamilyRoom != null) {
            switchFamilyRoom.setChecked(bookingRequest.isFamilyRoomEnabled);
            updateFamilyRoomOptionUi();

            switchFamilyRoom.setOnCheckedChangeListener((buttonView, isChecked) -> {
                HapticUtil.click(buttonView);
                if (isChecked) {
                    bookingRequest.isFamilyRoomEnabled = true;
                    updateFamilyRoomOptionUi();
                    new AlertDialog.Builder(this)
                            .setTitle(getString(R.string.dialog_family_room_enabled_title))
                            .setMessage(getString(R.string.dialog_family_room_enabled_msg, getRoomLabel(bookingRequest.getBaseRoomType())))
                            .setPositiveButton(getString(R.string.btn_regenerate), (d, w) -> {
                                generateRoomsForCurrentPolicy();
                                renderUi();
                            })
                            .setNegativeButton(getString(R.string.btn_keep_existing), (d, w) -> {
                                renderUi();
                            })
                            .show();
                } else {
                    handleDisableFamilyRoom();
                }
            });
        }

        findViewById(R.id.btnAddAnotherRoom).setOnClickListener(v -> {
            HapticUtil.click(v);
            showAddRoomDialog();
        });

        btnProceedToTravellers.setOnClickListener(v -> {
            HapticUtil.click(v);
            validateAndProceed();
        });

        View bottomBar = findViewById(R.id.bottomActionBar);
        if (bottomBar != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(bottomBar, (v, insets) -> {
                androidx.core.graphics.Insets navInsets = insets.getInsets(
                        androidx.core.view.WindowInsetsCompat.Type.systemBars() |
                        androidx.core.view.WindowInsetsCompat.Type.ime()
                );
                int basePad = dp(16);
                v.setPadding(
                        v.getPaddingLeft(),
                        v.getPaddingTop(),
                        v.getPaddingRight(),
                        navInsets.bottom > 0 ? navInsets.bottom + dp(10) : basePad
                );
                return insets;
            });
        }
    }

    private void updateFamilyRoomOptionUi() {
        if (switchFamilyRoom == null || txtFamilyRoomStatusBadge == null || txtFamilyRoomPolicyDesc == null) return;
        boolean enabled = bookingRequest.isFamilyRoomEnabled;
        if (enabled) {
            txtFamilyRoomStatusBadge.setText(getString(R.string.badge_family_room_enabled));
            txtFamilyRoomStatusBadge.setBackgroundResource(R.drawable.bg_status_verified);
            txtFamilyRoomStatusBadge.setTextColor(Color.parseColor("#1B5E20"));
            txtFamilyRoomPolicyDesc.setText(getString(R.string.family_room_on_desc));
        } else {
            txtFamilyRoomStatusBadge.setText(getString(R.string.badge_strict_gender));
            txtFamilyRoomStatusBadge.setBackgroundResource(R.drawable.bg_chip_minimal);
            txtFamilyRoomStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.brand_magenta));
            txtFamilyRoomPolicyDesc.setText(getString(R.string.family_room_off_desc));
        }
    }

    private void handleDisableFamilyRoom() {
        boolean mixedOrFamilyFound = false;

        // 1. Identify and invalidate family rooms
        List<BookingRequest.RoomConfig> toRemove = new ArrayList<>();
        for (BookingRequest.RoomConfig r : rooms) {
            if (r.isFamilyRoom()) {
                toRemove.add(r);
                mixedOrFamilyFound = true;
            } else if (r.isMaleRoom()) {
                // Remove any female occupants
                List<Integer> femaleOccupants = new ArrayList<>();
                if (r.assignedTravellerIndices != null) {
                    for (int idx : r.assignedTravellerIndices) {
                        TravellerSlot slot = findSlotByIndex(idx);
                        if (slot != null && slot.isFemale()) {
                            femaleOccupants.add(idx);
                        }
                    }
                    if (!femaleOccupants.isEmpty()) {
                        r.assignedTravellerIndices.removeAll(femaleOccupants);
                        mixedOrFamilyFound = true;
                    }
                }
            } else if (r.isFemaleRoom()) {
                // Remove any male occupants
                List<Integer> maleOccupants = new ArrayList<>();
                if (r.assignedTravellerIndices != null) {
                    for (int idx : r.assignedTravellerIndices) {
                        TravellerSlot slot = findSlotByIndex(idx);
                        if (slot != null && slot.isMale()) {
                            maleOccupants.add(idx);
                        }
                    }
                    if (!maleOccupants.isEmpty()) {
                        r.assignedTravellerIndices.removeAll(maleOccupants);
                        mixedOrFamilyFound = true;
                    }
                }
            }
        }

        rooms.removeAll(toRemove);

        // Re-index remaining room numbers
        for (int i = 0; i < rooms.size(); i++) {
            rooms.get(i).roomNumber = i + 1;
        }

        bookingRequest.isFamilyRoomEnabled = false;
        updateFamilyRoomOptionUi();
        renderUi();

        if (mixedOrFamilyFound) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.dialog_family_room_disabled_title))
                    .setMessage(getString(R.string.family_toggle_off_warning))
                    .setPositiveButton(getString(R.string.btn_regenerate_rooms), (d, w) -> {
                        generateRoomsForCurrentPolicy();
                        renderUi();
                    })
                    .setNegativeButton(getString(R.string.btn_arrange_manually), null)
                    .show();
        } else {
            Toast.makeText(this, getString(R.string.toast_strict_gender_enabled), Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isUmrah() {
        return bookingRequest.isUmrahPackage();
    }

    private String getDepartureDate() {
        return bookingRequest.selectedDepartureDate != null ? bookingRequest.selectedDepartureDate : "";
    }

    private void setupTravellerSlots() {
        travellerSlots.clear();
        int maleCount = bookingRequest.adultMaleCount;
        int femaleCount = bookingRequest.adultFemaleCount;
        if (maleCount <= 0 && femaleCount <= 0) {
            maleCount = Math.max(1, bookingRequest.adultPaxCount);
        }

        int slotIdx = 0;

        // 1. Lead Traveller
        TravellerSlot lead = new TravellerSlot();
        lead.index = slotIdx++;
        lead.isLead = true;
        if (maleCount > 0) {
            lead.gender = "Male";
            maleCount--;
        } else {
            lead.gender = "Female";
            femaleCount--;
        }
        lead.type = "Adult";
        String leadGenderLabel = "Male".equals(lead.gender) ? getString(R.string.gender_male) : getString(R.string.gender_female);
        lead.displayName = getString(R.string.traveller_lead_format, leadGenderLabel);
        travellerSlots.add(lead);

        // 2. Remaining Adult Males
        for (int m = 0; m < maleCount; m++) {
            TravellerSlot slot = new TravellerSlot();
            slot.index = slotIdx++;
            slot.gender = "Male";
            slot.type = "Adult";
            slot.displayName = getString(R.string.traveller_slot_format, slot.index + 1, getString(R.string.gender_male));
            travellerSlots.add(slot);
        }

        // 3. Remaining Adult Females
        for (int f = 0; f < femaleCount; f++) {
            TravellerSlot slot = new TravellerSlot();
            slot.index = slotIdx++;
            slot.gender = "Female";
            slot.type = "Adult";
            slot.displayName = getString(R.string.traveller_slot_format, slot.index + 1, getString(R.string.gender_female));
            travellerSlots.add(slot);
        }

        // 4. Children (Each child has configured gender)
        String depDateIso = getDepartureDate();
        if (bookingRequest.childConfigs != null) {
            for (int c = 0; c < bookingRequest.childConfigs.size(); c++) {
                BookingRequest.ChildConfig cfg = bookingRequest.childConfigs.get(c);
                TravellerSlot slot = new TravellerSlot();
                slot.index = slotIdx++;
                slot.dateOfBirth = cfg.dateOfBirth != null ? cfg.dateOfBirth : "";
                slot.age = PackagePricingCalculator.calculateAge(slot.dateOfBirth, depDateIso);
                slot.type = slot.age < 2 ? "Infant" : "Child";
                slot.gender = (cfg.gender != null && !cfg.gender.isEmpty()) ? cfg.gender : "Male";
                slot.withBed = cfg.withBed;
                String ageLabel = slot.age < 2 ? getString(R.string.age_under_2_short) : getString(R.string.age_years_short, slot.age);
                String genderLabel = "Female".equalsIgnoreCase(slot.gender) ? getString(R.string.gender_female) : getString(R.string.gender_male);
                slot.displayName = getString(R.string.child_slot_format, c + 1, ageLabel, genderLabel);
                travellerSlots.add(slot);
            }
        }
    }

    private void setupInitialRooms() {
        rooms.clear();
        if (bookingRequest.rooms != null && !bookingRequest.rooms.isEmpty()) {
            for (BookingRequest.RoomConfig rc : bookingRequest.rooms) {
                if (rc.gender == null || rc.gender.isEmpty()) {
                    rc.gender = rc.isFamilyRoom() ? "family" : "male";
                }
            }
            rooms.addAll(bookingRequest.rooms);
            return;
        }

        generateRoomsForCurrentPolicy();
    }

    private void generateRoomsForCurrentPolicy() {
        rooms.clear();
        String baseCategory = bookingRequest.getBaseRoomType();
        int capacity = bookingRequest.getBaseRoomCapacity();

        if (bookingRequest.isFamilyRoomEnabled) {
            // SCENARIO B: FAMILY ROOM ON
            // Generate Family Room groups of the SAME selected category (e.g. Quint Family)
            int roomNum = 1;
            int paxIdx = 0;
            while (paxIdx < travellerSlots.size()) {
                BookingRequest.RoomConfig famRoom = new BookingRequest.RoomConfig(roomNum++, baseCategory, "family", capacity);
                for (int i = 0; i < capacity && paxIdx < travellerSlots.size(); i++) {
                    famRoom.assignedTravellerIndices.add(travellerSlots.get(paxIdx++).index);
                }
                rooms.add(famRoom);
            }
            if (rooms.isEmpty()) {
                rooms.add(new BookingRequest.RoomConfig(1, baseCategory, "family", capacity));
            }
        } else {
            // SCENARIO A: FAMILY ROOM OFF (STRICT MALE / FEMALE SEPARATION)
            List<TravellerSlot> maleList = new ArrayList<>();
            List<TravellerSlot> femaleList = new ArrayList<>();
            for (TravellerSlot s : travellerSlots) {
                if (s.isFemale()) femaleList.add(s);
                else maleList.add(s);
            }

            int roomNum = 1;

            // 1. Male Rooms of the SAME selected category (e.g. Quint Male)
            int maleIdx = 0;
            while (maleIdx < maleList.size()) {
                BookingRequest.RoomConfig mr = new BookingRequest.RoomConfig(roomNum++, baseCategory, "male", capacity);
                for (int i = 0; i < capacity && maleIdx < maleList.size(); i++) {
                    mr.assignedTravellerIndices.add(maleList.get(maleIdx++).index);
                }
                rooms.add(mr);
            }

            // 2. Female Rooms of the SAME selected category (e.g. Quint Female)
            int femaleIdx = 0;
            while (femaleIdx < femaleList.size()) {
                BookingRequest.RoomConfig fr = new BookingRequest.RoomConfig(roomNum++, baseCategory, "female", capacity);
                for (int i = 0; i < capacity && femaleIdx < femaleList.size(); i++) {
                    fr.assignedTravellerIndices.add(femaleList.get(femaleIdx++).index);
                }
                rooms.add(fr);
            }

            if (rooms.isEmpty()) {
                rooms.add(new BookingRequest.RoomConfig(1, baseCategory, "male", capacity));
            }
        }
    }

    private int getRoomCapacity(String roomType) {
        if (roomType == null) return 2;
        switch (roomType.toLowerCase().trim()) {
            case "quint":
            case "family_5":
                return 5;
            case "quad":
            case "family_4":
                return 4;
            case "triple":
                return 3;
            case "double":
                return 2;
            case "single":
                return 1;
            default:
                return 2;
        }
    }

    private String getRoomLabel(String roomType) {
        if (roomType == null) return getString(R.string.room_default_label);
        switch (roomType.toLowerCase().trim()) {
            case "quint":
            case "family_5":
                return getString(R.string.room_type_quint_label);
            case "quad":
            case "family_4":
                return getString(R.string.room_type_quad_label);
            case "triple":
                return getString(R.string.room_type_triple_label);
            case "double":
                return getString(R.string.room_type_double_label);
            case "single":
                return getString(R.string.room_type_single_label);
            default:
                return roomType.toUpperCase();
        }
    }

    private List<Integer> getUnassignedTravellerIndices() {
        List<Integer> unassigned = new ArrayList<>();
        for (TravellerSlot slot : travellerSlots) {
            boolean assigned = false;
            for (BookingRequest.RoomConfig room : rooms) {
                if (room.assignedTravellerIndices != null && room.assignedTravellerIndices.contains(slot.index)) {
                    assigned = true;
                    break;
                }
            }
            if (!assigned) {
                unassigned.add(slot.index);
            }
        }
        return unassigned;
    }

    private TravellerSlot findSlotByIndex(int index) {
        for (TravellerSlot s : travellerSlots) {
            if (s.index == index) return s;
        }
        return null;
    }

    private void renderUi() {
        txtPackageName.setText(bookingRequest.packageName);
        badgePackageType.setText(isUmrah() ? getString(R.string.badge_package_umrah) : getString(R.string.badge_package_tour));
        if (bookingRequest.selectedDepartureDate != null && !bookingRequest.selectedDepartureDate.isEmpty()) {
            txtDepartureDate.setText(getString(R.string.passenger_departure_format, bookingRequest.selectedDepartureDate));
            txtDepartureDate.setVisibility(View.VISIBLE);
        } else {
            txtDepartureDate.setVisibility(View.GONE);
        }
        txtTotalTravellersHeader.setText(getString(R.string.total_travellers_count_format, travellerSlots.size()));
        int roomCount = rooms.size();
        txtTotalRoomsCount.setText(roomCount == 1 ? getString(R.string.room_count_single) : getString(R.string.room_count_plural, roomCount));

        updateFamilyRoomOptionUi();
        renderUnassignedSection();
        renderRoomsList();
        updateValidationState();
    }

    private void renderUnassignedSection() {
        containerUnassignedTravellers.removeAllViews();
        List<Integer> unassigned = getUnassignedTravellerIndices();

        if (unassigned.isEmpty()) {
            cardUnassignedTravellers.setVisibility(View.GONE);
            return;
        }

        cardUnassignedTravellers.setVisibility(View.VISIBLE);
        txtUnassignedTitle.setText(getString(R.string.unassigned_title_format, unassigned.size()));
        badgeUnassignedCount.setText(getString(R.string.badge_needs_assignment, unassigned.size()));

        for (int idx : unassigned) {
            TravellerSlot slot = findSlotByIndex(idx);
            if (slot == null) continue;

            LinearLayout chip = new LinearLayout(this);
            chip.setOrientation(LinearLayout.HORIZONTAL);
            chip.setGravity(Gravity.CENTER_VERTICAL);
            chip.setBackgroundResource(R.drawable.bg_luxury_form_field);
            chip.setPadding(dp(12), dp(10), dp(12), dp(10));
            chip.setClickable(true);
            chip.setFocusable(true);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(6);
            chip.setLayoutParams(lp);

            TextView txtName = new TextView(this);
            txtName.setText(slot.displayName);
            txtName.setTextSize(13);
            txtName.setTypeface(null, Typeface.BOLD);
            txtName.setTextColor(ContextCompat.getColor(this, R.color.text_dark));
            LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            txtName.setLayoutParams(nameLp);
            chip.addView(txtName);

            // Gender / Bed Badge
            TextView badge = new TextView(this);
            String badgeText = slot.isFemale() ? getString(R.string.tag_female) : getString(R.string.tag_male);
            if (Boolean.TRUE.equals(slot.withBed)) badgeText += " " + getString(R.string.tag_with_bed);
            else if (Boolean.FALSE.equals(slot.withBed)) badgeText += " " + getString(R.string.tag_no_bed);
            badge.setText(badgeText);
            badge.setTextSize(11);
            badge.setTypeface(null, Typeface.BOLD);
            badge.setTextColor(ContextCompat.getColor(this, slot.isFemale() ? R.color.pink_dark : R.color.brand_magenta));
            badge.setBackgroundResource(R.drawable.bg_chip_minimal);
            badge.setPadding(dp(6), dp(2), dp(6), dp(2));
            chip.addView(badge);

            TextView btnAssign = new TextView(this);
            btnAssign.setText(getString(R.string.btn_assign));
            btnAssign.setTextSize(12);
            btnAssign.setTypeface(null, Typeface.BOLD);
            btnAssign.setTextColor(ContextCompat.getColor(this, R.color.brand_magenta));
            btnAssign.setPadding(dp(10), 0, 0, 0);
            chip.addView(btnAssign);

            chip.setOnClickListener(v -> {
                HapticUtil.click(v);
                showAssignTravellerDialog(slot);
            });

            containerUnassignedTravellers.addView(chip);
        }
    }

    private void renderRoomsList() {
        containerRoomsList.removeAllViews();

        List<BookingRequest.RoomConfig> maleRooms = new ArrayList<>();
        List<BookingRequest.RoomConfig> femaleRooms = new ArrayList<>();
        List<BookingRequest.RoomConfig> familyRooms = new ArrayList<>();

        for (BookingRequest.RoomConfig r : rooms) {
            if (r.isFamilyRoom()) familyRooms.add(r);
            else if (r.isFemaleRoom()) femaleRooms.add(r);
            else maleRooms.add(r);
        }

        // Section 1: MALE ROOMS
        renderRoomSection("♂ " + getString(R.string.section_male_rooms), maleRooms, "male", "#1E40AF");

        // Section 2: FEMALE ROOMS
        renderRoomSection("♀ " + getString(R.string.section_female_rooms), femaleRooms, "female", "#BE185D");

        // Section 3: FAMILY ROOMS (Only displayed when family room is enabled)
        if (bookingRequest.isFamilyRoomEnabled) {
            renderRoomSection("👨‍👩‍👧 " + getString(R.string.section_family_rooms), familyRooms, "family", "#047857");
        }
    }

    private void renderRoomSection(String sectionTitle, List<BookingRequest.RoomConfig> sectionRooms, String categoryKey, String headerColorHex) {
        LinearLayout sectionContainer = new LinearLayout(this);
        sectionContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams scLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        scLp.topMargin = dp(14);
        sectionContainer.setLayoutParams(scLp);

        // Header Row
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.setPadding(0, 0, 0, dp(6));

        TextView txtTitle = new TextView(this);
        txtTitle.setText(sectionTitle);
        txtTitle.setTextSize(14);
        txtTitle.setTypeface(null, Typeface.BOLD);
        txtTitle.setTextColor(Color.parseColor(headerColorHex));
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        txtTitle.setLayoutParams(tLp);
        headerRow.addView(txtTitle);

        TextView badgeCount = new TextView(this);
        int totalOcc = 0;
        int totalCap = 0;
        for (BookingRequest.RoomConfig r : sectionRooms) {
            totalOcc += r.getOccupancy();
            totalCap += r.capacity;
        }
        badgeCount.setText(getString(R.string.section_room_count_summary, sectionRooms.size(), totalOcc, totalCap));
        badgeCount.setTextSize(11);
        badgeCount.setTypeface(null, Typeface.BOLD);
        badgeCount.setTextColor(ContextCompat.getColor(this, R.color.text_gray));
        headerRow.addView(badgeCount);

        sectionContainer.addView(headerRow);

        // Family surcharge note
        if ("family".equalsIgnoreCase(categoryKey)) {
            TextView surchargeNotice = new TextView(this);
            surchargeNotice.setText(getString(R.string.family_room_surcharge_notice));
            surchargeNotice.setTextSize(11);
            surchargeNotice.setTextColor(Color.parseColor("#047857"));
            LinearLayout.LayoutParams snLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            snLp.bottomMargin = dp(6);
            surchargeNotice.setLayoutParams(snLp);
            sectionContainer.addView(surchargeNotice);
        }

        // Room Cards
        if (sectionRooms.isEmpty()) {
            TextView emptyTxt = new TextView(this);
            String catName = categoryKey.equals("male") ? getString(R.string.category_name_male) : (categoryKey.equals("female") ? getString(R.string.category_name_female) : getString(R.string.category_name_family));
            emptyTxt.setText(getString(R.string.section_empty_rooms_format, catName));
            emptyTxt.setTextSize(12);
            emptyTxt.setTextColor(ContextCompat.getColor(this, R.color.text_gray));
            emptyTxt.setBackgroundResource(R.drawable.bg_luxury_form_field);
            emptyTxt.setPadding(dp(12), dp(10), dp(12), dp(10));
            sectionContainer.addView(emptyTxt);
        } else {
            for (BookingRequest.RoomConfig room : sectionRooms) {
                int globalIndex = rooms.indexOf(room);
                sectionContainer.addView(buildRoomCardView(room, globalIndex));
            }
        }

        containerRoomsList.addView(sectionContainer);
    }

    private View buildRoomCardView(BookingRequest.RoomConfig room, int roomIndex) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_detail_card);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.topMargin = dp(6);
        cardParams.bottomMargin = dp(6);
        card.setLayoutParams(cardParams);

        // Header Row
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        String genderPrefix = room.isFamilyRoom() ? getString(R.string.prefix_family_room) : (room.isFemaleRoom() ? getString(R.string.prefix_female_room) : getString(R.string.prefix_male_room));
        title.setText(getString(R.string.room_card_header_with_gender, room.roomNumber, getRoomLabel(room.roomType).toUpperCase(), genderPrefix));
        title.setTextSize(13);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(ContextCompat.getColor(this, R.color.text_dark));
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        title.setLayoutParams(titleLp);
        headerRow.addView(title);

        // Occupancy Badge
        TextView occBadge = new TextView(this);
        int occ = room.getOccupancy();
        int cap = room.capacity;
        occBadge.setText(getString(R.string.room_occupied_count_format, occ, cap));
        occBadge.setTextSize(11);
        occBadge.setTypeface(null, Typeface.BOLD);
        occBadge.setPadding(dp(8), dp(3), dp(8), dp(3));
        if (occ > cap) {
            occBadge.setBackgroundResource(R.drawable.bg_status_pending);
            occBadge.setTextColor(ContextCompat.getColor(this, R.color.pink_dark));
        } else if (occ == cap) {
            occBadge.setBackgroundResource(R.drawable.bg_status_verified);
            occBadge.setTextColor(Color.parseColor("#1B5E20"));
        } else {
            occBadge.setBackgroundResource(R.drawable.bg_chip_minimal);
            occBadge.setTextColor(ContextCompat.getColor(this, R.color.brand_magenta));
        }
        headerRow.addView(occBadge);

        card.addView(headerRow);

        // Remaining spaces indicator
        int remaining = cap - occ;
        if (remaining > 0) {
            TextView txtRemaining = new TextView(this);
            txtRemaining.setText(getString(R.string.room_vacancies_format, remaining, occ));
            txtRemaining.setTextSize(11);
            txtRemaining.setTextColor(ContextCompat.getColor(this, R.color.text_gray));
            LinearLayout.LayoutParams remLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            remLp.topMargin = dp(2);
            txtRemaining.setLayoutParams(remLp);
            card.addView(txtRemaining);
        }

        // Tour Single Surcharge note
        if (!isUmrah() && "single".equalsIgnoreCase(room.roomType)) {
            TextView surchargeNote = new TextView(this);
            surchargeNote.setText("• " + getString(R.string.room_single_surcharge_info));
            surchargeNote.setTextSize(11);
            surchargeNote.setTextColor(ContextCompat.getColor(this, R.color.brand_magenta));
            LinearLayout.LayoutParams snParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            snParams.topMargin = dp(4);
            surchargeNote.setLayoutParams(snParams);
            card.addView(surchargeNote);
        }

        // Visual Occupancy Meter
        ProgressBar pb = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        pb.setMax(cap);
        pb.setProgress(Math.min(occ, cap));
        pb.setProgressTintList(android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(this, occ > cap ? R.color.error_red : (occ == cap ? R.color.brand_magenta : R.color.brand_magenta))
        ));
        pb.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.card_border)
        ));
        LinearLayout.LayoutParams pbLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(4));
        pbLp.topMargin = dp(8);
        pbLp.bottomMargin = dp(4);
        pb.setLayoutParams(pbLp);
        card.addView(pb);

        if (remaining > 0) {
            TextView txtNotice = new TextView(this);
            txtNotice.setText(getString(R.string.room_sharing_notice));
            txtNotice.setTextSize(10);
            txtNotice.setTextColor(ContextCompat.getColor(this, R.color.text_gray));
            LinearLayout.LayoutParams notLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            notLp.bottomMargin = dp(6);
            txtNotice.setLayoutParams(notLp);
            card.addView(txtNotice);
        }

        // Warning if over capacity
        if (occ > cap) {
            TextView warn = new TextView(this);
            warn.setText(getString(R.string.err_room_over_capacity_warn, (occ - cap)));
            warn.setTextSize(11);
            warn.setTypeface(null, Typeface.BOLD);
            warn.setTextColor(ContextCompat.getColor(this, R.color.pink_dark));
            warn.setBackgroundResource(R.drawable.bg_status_pending);
            warn.setPadding(dp(8), dp(4), dp(8), dp(4));
            LinearLayout.LayoutParams warnLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            warnLp.bottomMargin = dp(8);
            warn.setLayoutParams(warnLp);
            card.addView(warn);
        }

        // Occupants list
        LinearLayout occupantsContainer = new LinearLayout(this);
        occupantsContainer.setOrientation(LinearLayout.VERTICAL);

        if (room.assignedTravellerIndices == null || room.assignedTravellerIndices.isEmpty()) {
            TextView emptyTxt = new TextView(this);
            emptyTxt.setText(getString(R.string.room_empty_in_list));
            emptyTxt.setTextSize(12);
            emptyTxt.setTextColor(ContextCompat.getColor(this, R.color.text_gray));
            emptyTxt.setPadding(0, dp(4), 0, dp(4));
            occupantsContainer.addView(emptyTxt);
        } else {
            for (int slotIdx : room.assignedTravellerIndices) {
                TravellerSlot slot = findSlotByIndex(slotIdx);
                if (slot == null) continue;

                LinearLayout occupantRow = new LinearLayout(this);
                occupantRow.setOrientation(LinearLayout.HORIZONTAL);
                occupantRow.setGravity(Gravity.CENTER_VERTICAL);
                occupantRow.setBackgroundResource(R.drawable.bg_luxury_form_field);
                occupantRow.setPadding(dp(10), dp(8), dp(10), dp(8));

                LinearLayout.LayoutParams occLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                occLp.bottomMargin = dp(4);
                occupantRow.setLayoutParams(occLp);

                TextView nameTxt = new TextView(this);
                nameTxt.setText(slot.displayName);
                nameTxt.setTextSize(12);
                nameTxt.setTypeface(null, slot.isLead ? Typeface.BOLD : Typeface.NORMAL);
                nameTxt.setTextColor(ContextCompat.getColor(this, R.color.text_dark));
                LinearLayout.LayoutParams nLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                nameTxt.setLayoutParams(nLp);
                occupantRow.addView(nameTxt);

                // Badge info
                TextView tagBadge = new TextView(this);
                String tag = slot.isFemale() ? getString(R.string.tag_female) : getString(R.string.tag_male);
                if (Boolean.TRUE.equals(slot.withBed)) tag += " " + getString(R.string.tag_with_bed);
                else if (Boolean.FALSE.equals(slot.withBed)) tag += " " + getString(R.string.tag_no_bed);
                tagBadge.setText(tag);
                tagBadge.setTextSize(10);
                tagBadge.setTextColor(ContextCompat.getColor(this, slot.isFemale() ? R.color.pink_dark : R.color.brand_magenta));
                tagBadge.setBackgroundResource(R.drawable.bg_chip_minimal);
                tagBadge.setPadding(dp(6), dp(2), dp(6), dp(2));
                occupantRow.addView(tagBadge);

                // Manage occupant button
                TextView btnManage = new TextView(this);
                btnManage.setText(getString(R.string.btn_manage));
                btnManage.setTextSize(11);
                btnManage.setTypeface(null, Typeface.BOLD);
                btnManage.setTextColor(ContextCompat.getColor(this, R.color.brand_magenta));
                btnManage.setPadding(dp(10), dp(2), 0, dp(2));
                occupantRow.addView(btnManage);

                occupantRow.setOnClickListener(v -> {
                    HapticUtil.click(v);
                    showManageOccupantDialog(slot, roomIndex);
                });

                occupantsContainer.addView(occupantRow);
            }
        }

        card.addView(occupantsContainer);

        // Divider
        View divider = new View(this);
        divider.setBackgroundColor(ContextCompat.getColor(this, R.color.card_border));
        LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        dLp.topMargin = dp(10);
        dLp.bottomMargin = dp(10);
        divider.setLayoutParams(dLp);
        card.addView(divider);

        // Actions row: Change Type, Change Category, Remove Room
        LinearLayout actionsRow = new LinearLayout(this);
        actionsRow.setOrientation(LinearLayout.HORIZONTAL);
        actionsRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView btnChangeType = new TextView(this);
        btnChangeType.setText(getString(R.string.btn_change_type));
        btnChangeType.setTextSize(11);
        btnChangeType.setTypeface(null, Typeface.BOLD);
        btnChangeType.setTextColor(ContextCompat.getColor(this, R.color.brand_magenta));
        btnChangeType.setBackgroundResource(R.drawable.bg_whatsapp_secondary_button);
        btnChangeType.setPadding(dp(10), dp(6), dp(10), dp(6));
        btnChangeType.setOnClickListener(v -> {
            HapticUtil.click(v);
            showChangeRoomTypeDialog(roomIndex);
        });
        actionsRow.addView(btnChangeType);

        TextView btnChangeCategory = new TextView(this);
        btnChangeCategory.setText(getString(R.string.btn_change_gender));
        btnChangeCategory.setTextSize(11);
        btnChangeCategory.setTypeface(null, Typeface.BOLD);
        btnChangeCategory.setTextColor(ContextCompat.getColor(this, R.color.brand_magenta));
        btnChangeCategory.setBackgroundResource(R.drawable.bg_whatsapp_secondary_button);
        btnChangeCategory.setPadding(dp(10), dp(6), dp(10), dp(6));
        LinearLayout.LayoutParams catParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        catParams.leftMargin = dp(6);
        btnChangeCategory.setLayoutParams(catParams);
        btnChangeCategory.setOnClickListener(v -> {
            HapticUtil.click(v);
            showChangeRoomCategoryDialog(roomIndex);
        });
        actionsRow.addView(btnChangeCategory);

        View spacer = new View(this);
        LinearLayout.LayoutParams spLp = new LinearLayout.LayoutParams(0, 0, 1f);
        spacer.setLayoutParams(spLp);
        actionsRow.addView(spacer);

        if (rooms.size() > 1) {
            TextView btnRemove = new TextView(this);
            btnRemove.setText(getString(R.string.btn_remove));
            btnRemove.setTextSize(11);
            btnRemove.setTextColor(ContextCompat.getColor(this, R.color.pink_dark));
            btnRemove.setPadding(dp(8), dp(6), dp(8), dp(6));
            btnRemove.setOnClickListener(v -> {
                HapticUtil.click(v);
                confirmRemoveRoom(roomIndex);
            });
            actionsRow.addView(btnRemove);
        }

        card.addView(actionsRow);
        return card;
    }

    private void showAddRoomDialog() {
        List<String> categoryOptions = new ArrayList<>();
        final List<String> categoryKeys = new ArrayList<>();

        categoryOptions.add(getString(R.string.room_category_male));
        categoryKeys.add("male");

        categoryOptions.add(getString(R.string.room_category_female));
        categoryKeys.add("female");

        if (bookingRequest.isFamilyRoomEnabled) {
            categoryOptions.add(getString(R.string.room_category_family));
            categoryKeys.add("family");
        }

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_select_room_category))
                .setItems(categoryOptions.toArray(new String[0]), (d, which) -> {
                    String selectedGender = categoryKeys.get(which);
                    promptRoomTypeForNewRoom(selectedGender);
                })
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .show();
    }

    private void promptRoomTypeForNewRoom(String roomGender) {
        String[] options;
        String[] typeKeys;

        if (isUmrah()) {
            // Strictly NO Single Room for Umrah!
            options = new String[]{
                    getString(R.string.room_type_quint_label),
                    getString(R.string.room_type_quad_label),
                    getString(R.string.room_type_triple_label),
                    getString(R.string.room_type_double_label)
            };
            typeKeys = new String[]{"quint", "quad", "triple", "double"};
        } else {
            options = new String[]{
                    getString(R.string.room_type_triple_label),
                    getString(R.string.room_type_double_label),
                    getString(R.string.room_type_single_tour_label)
            };
            typeKeys = new String[]{"triple", "double", "single"};
        }

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_select_room_type))
                .setItems(options, (d, which) -> {
                    String selectedType = typeKeys[which];
                    int nextRoomNum = rooms.size() + 1;
                    int cap = getRoomCapacity(selectedType);
                    BookingRequest.RoomConfig newRoom = new BookingRequest.RoomConfig(nextRoomNum, selectedType, roomGender, cap);
                    rooms.add(newRoom);
                    renderUi();
                    Toast.makeText(this, getString(R.string.toast_room_added_format, nextRoomNum), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .show();
    }

    private void showChangeRoomCategoryDialog(int roomIndex) {
        BookingRequest.RoomConfig room = rooms.get(roomIndex);
        List<String> options = new ArrayList<>();
        final List<String> keys = new ArrayList<>();

        options.add(getString(R.string.room_category_male));
        keys.add("male");

        options.add(getString(R.string.room_category_female));
        keys.add("female");

        if (bookingRequest.isFamilyRoomEnabled) {
            options.add(getString(R.string.room_category_family));
            keys.add("family");
        }

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_change_room_gender_title, room.roomNumber))
                .setItems(options.toArray(new String[0]), (d, which) -> {
                    String newGender = keys.get(which);
                    // Check if existing occupants conflict
                    if ("male".equalsIgnoreCase(newGender)) {
                        for (int idx : room.assignedTravellerIndices) {
                            TravellerSlot slot = findSlotByIndex(idx);
                            if (slot != null && slot.isFemale()) {
                                showInvalidAssignmentDialog(getString(R.string.err_change_gender_has_female));
                                return;
                            }
                        }
                    } else if ("female".equalsIgnoreCase(newGender)) {
                        for (int idx : room.assignedTravellerIndices) {
                            TravellerSlot slot = findSlotByIndex(idx);
                            if (slot != null && slot.isMale()) {
                                showInvalidAssignmentDialog(getString(R.string.err_change_gender_has_male));
                                return;
                            }
                        }
                    }
                    room.gender = newGender;
                    renderUi();
                })
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .show();
    }

    private void confirmRemoveRoom(int roomIndex) {
        if (rooms.size() <= 1) {
            Toast.makeText(this, getString(R.string.toast_min_one_room), Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_delete_room_title, roomIndex + 1))
                .setMessage(getString(R.string.dialog_delete_room_msg))
                .setPositiveButton(getString(R.string.btn_remove), (d, w) -> {
                    rooms.remove(roomIndex);
                    for (int i = 0; i < rooms.size(); i++) {
                        rooms.get(i).roomNumber = i + 1;
                    }
                    renderUi();
                    Toast.makeText(this, getString(R.string.toast_room_deleted), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .show();
    }

    private void showChangeRoomTypeDialog(int roomIndex) {
        BookingRequest.RoomConfig room = rooms.get(roomIndex);
        String[] options;
        String[] typeKeys;

        if (isUmrah()) {
            options = new String[]{
                    getString(R.string.room_type_quint_label),
                    getString(R.string.room_type_quad_label),
                    getString(R.string.room_type_triple_label),
                    getString(R.string.room_type_double_label)
            };
            typeKeys = new String[]{"quint", "quad", "triple", "double"};
        } else {
            options = new String[]{
                    getString(R.string.room_type_triple_label),
                    getString(R.string.room_type_double_label),
                    getString(R.string.room_type_single_tour_label)
            };
            typeKeys = new String[]{"triple", "double", "single"};
        }

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_select_room_type_for_room, room.roomNumber))
                .setItems(options, (d, which) -> {
                    String selectedType = typeKeys[which];
                    room.roomType = selectedType;
                    room.roomCategory = selectedType;
                    room.capacity = getRoomCapacity(selectedType);
                    renderUi();
                })
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .show();
    }

    private void showAssignTravellerDialog(TravellerSlot slot) {
        String[] roomChoices = new String[rooms.size()];
        for (int i = 0; i < rooms.size(); i++) {
            BookingRequest.RoomConfig r = rooms.get(i);
            String genderTag = r.isFamilyRoom() ? getString(R.string.room_category_family_plain) : (r.isFemaleRoom() ? getString(R.string.gender_female) : getString(R.string.gender_male));
            roomChoices[i] = getString(R.string.room_choice_format, r.roomNumber, genderTag, getRoomLabel(r.roomType), r.getOccupancy(), r.capacity);
        }

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_assign_slot_title, slot.displayName))
                .setItems(roomChoices, (d, which) -> {
                    BookingRequest.RoomConfig targetRoom = rooms.get(which);

                    // Gender assignment validation
                    if (targetRoom.isMaleRoom() && slot.isFemale()) {
                        showInvalidAssignmentDialog(getString(R.string.err_male_room_female_traveller));
                        return;
                    }
                    if (targetRoom.isFemaleRoom() && slot.isMale()) {
                        showInvalidAssignmentDialog(getString(R.string.err_female_room_male_traveller));
                        return;
                    }

                    // Capacity check
                    if (targetRoom.getOccupancy() >= targetRoom.capacity) {
                        showInvalidAssignmentDialog(getString(R.string.err_room_full, targetRoom.roomNumber, targetRoom.capacity));
                        return;
                    }

                    // Remove from any other room
                    for (BookingRequest.RoomConfig r : rooms) {
                        if (r.assignedTravellerIndices != null) {
                            r.assignedTravellerIndices.remove(Integer.valueOf(slot.index));
                        }
                    }

                    targetRoom.assignedTravellerIndices.add(slot.index);
                    renderUi();
                })
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .show();
    }

    private void showManageOccupantDialog(TravellerSlot slot, int currentRoomIndex) {
        List<String> options = new ArrayList<>();
        final List<Integer> targetRoomIndices = new ArrayList<>();

        for (int i = 0; i < rooms.size(); i++) {
            if (i != currentRoomIndex) {
                BookingRequest.RoomConfig r = rooms.get(i);
                String genderTag = r.isFamilyRoom() ? getString(R.string.room_category_family_plain) : (r.isFemaleRoom() ? getString(R.string.gender_female) : getString(R.string.gender_male));
                options.add(getString(R.string.action_move_to_room, r.roomNumber, genderTag, getRoomLabel(r.roomType)));
                targetRoomIndices.add(i);
            }
        }
        options.add(getString(R.string.action_unassign_to_pool));

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_manage_slot_title, slot.displayName))
                .setItems(options.toArray(new String[0]), (d, which) -> {
                    if (which < targetRoomIndices.size()) {
                        int targetIdx = targetRoomIndices.get(which);
                        BookingRequest.RoomConfig targetRoom = rooms.get(targetIdx);

                        // Gender validation
                        if (targetRoom.isMaleRoom() && slot.isFemale()) {
                            showInvalidAssignmentDialog(getString(R.string.err_male_room_female_traveller));
                            return;
                        }
                        if (targetRoom.isFemaleRoom() && slot.isMale()) {
                            showInvalidAssignmentDialog(getString(R.string.err_female_room_male_traveller));
                            return;
                        }

                        // Capacity check
                        if (targetRoom.getOccupancy() >= targetRoom.capacity) {
                            showInvalidAssignmentDialog(getString(R.string.err_room_full, targetRoom.roomNumber, targetRoom.capacity));
                            return;
                        }

                        // Remove from current room and move to target
                        rooms.get(currentRoomIndex).assignedTravellerIndices.remove(Integer.valueOf(slot.index));
                        targetRoom.assignedTravellerIndices.add(slot.index);
                    } else {
                        // Unassign
                        rooms.get(currentRoomIndex).assignedTravellerIndices.remove(Integer.valueOf(slot.index));
                    }
                    renderUi();
                })
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .show();
    }

    private void showInvalidAssignmentDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_invalid_assignment_title))
                .setMessage(message)
                .setPositiveButton(getString(R.string.btn_understand), null)
                .show();
    }

    private boolean validateArrangement(boolean showErrors) {
        List<Integer> unassigned = getUnassignedTravellerIndices();
        if (!unassigned.isEmpty()) {
            if (showErrors) {
                txtValidationWarning.setText(getString(R.string.err_unassigned_travellers, unassigned.size()));
                txtValidationWarning.setVisibility(View.VISIBLE);
                Toast.makeText(this, getString(R.string.toast_unassigned_travellers_format, unassigned.size()), Toast.LENGTH_SHORT).show();
            }
            return false;
        }

        for (BookingRequest.RoomConfig r : rooms) {
            if (r.assignedTravellerIndices == null || r.assignedTravellerIndices.isEmpty()) {
                if (showErrors) {
                    txtValidationWarning.setText(getString(R.string.err_room_empty, r.roomNumber));
                    txtValidationWarning.setVisibility(View.VISIBLE);
                    Toast.makeText(this, getString(R.string.toast_room_no_occupants_format, r.roomNumber), Toast.LENGTH_SHORT).show();
                }
                return false;
            }

            if (r.isOverCapacity()) {
                if (showErrors) {
                    txtValidationWarning.setText(getString(R.string.err_room_over_capacity, r.roomNumber, (r.getOccupancy() - r.capacity)));
                    txtValidationWarning.setVisibility(View.VISIBLE);
                    Toast.makeText(this, getString(R.string.toast_room_over_capacity_format, r.roomNumber), Toast.LENGTH_SHORT).show();
                }
                return false;
            }

            if (isUmrah() && "single".equalsIgnoreCase(r.roomType)) {
                if (showErrors) {
                    txtValidationWarning.setText(getString(R.string.err_umrah_single_forbidden));
                    txtValidationWarning.setVisibility(View.VISIBLE);
                    Toast.makeText(this, getString(R.string.err_umrah_single_forbidden), Toast.LENGTH_LONG).show();
                }
                return false;
            }

            // GENDER SEPARATION VALIDATION
            if (r.isMaleRoom()) {
                for (int slotIdx : r.assignedTravellerIndices) {
                    TravellerSlot slot = findSlotByIndex(slotIdx);
                    if (slot != null && slot.isFemale()) {
                        if (showErrors) {
                            txtValidationWarning.setText(getString(R.string.err_room_has_female, r.roomNumber) + " " + getString(R.string.err_gender_mixed_in_standard));
                            txtValidationWarning.setVisibility(View.VISIBLE);
                            Toast.makeText(this, getString(R.string.toast_room_has_female_format, r.roomNumber), Toast.LENGTH_LONG).show();
                        }
                        return false;
                    }
                }
            } else if (r.isFemaleRoom()) {
                for (int slotIdx : r.assignedTravellerIndices) {
                    TravellerSlot slot = findSlotByIndex(slotIdx);
                    if (slot != null && slot.isMale()) {
                        if (showErrors) {
                            txtValidationWarning.setText(getString(R.string.err_room_has_male, r.roomNumber) + " " + getString(R.string.err_gender_mixed_in_standard));
                            txtValidationWarning.setVisibility(View.VISIBLE);
                            Toast.makeText(this, getString(R.string.toast_room_has_male_format, r.roomNumber), Toast.LENGTH_LONG).show();
                        }
                        return false;
                    }
                }
            } else if (r.isFamilyRoom()) {
                if (!bookingRequest.isFamilyRoomEnabled) {
                    if (showErrors) {
                        txtValidationWarning.setText(getString(R.string.err_family_room_not_allowed, r.roomNumber));
                        txtValidationWarning.setVisibility(View.VISIBLE);
                    }
                    return false;
                }
            }
        }

        txtValidationWarning.setVisibility(View.GONE);
        return true;
    }

    private void updateValidationState() {
        validateArrangement(false);
    }

    private void validateAndProceed() {
        if (!validateArrangement(true)) {
            return;
        }

        // Synchronize rooms to bookingRequest
        bookingRequest.rooms = new ArrayList<>(rooms);

        // First room type as primary room label for backward compatibility
        if (!rooms.isEmpty()) {
            bookingRequest.roomType = rooms.get(0).roomType;
            bookingRequest.roomLabel = getRoomLabel(rooms.get(0).roomType);
        }

        // Launch PassengerDetailsActivity
        Intent intent = new Intent(this, PassengerDetailsActivity.class);
        intent.putExtra(PassengerDetailsActivity.EXTRA_BOOKING_REQUEST, bookingRequest);
        startActivity(intent);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
