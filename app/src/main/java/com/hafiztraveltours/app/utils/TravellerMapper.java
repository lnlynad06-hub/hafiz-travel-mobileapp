package com.hafiztraveltours.app.utils;

import android.content.Context;

import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.models.BookingRequest;
import com.hafiztraveltours.app.models.CreateBookingRequest;
import com.hafiztraveltours.app.network.UserDto;

/**
 * Explicit person/traveller mappings (H3). No reflection, no magic conversion.
 *
 * <p>Model roles (do NOT merge across these boundaries):
 * <ul>
 *   <li>{@link UserDto} — API <b>in</b> DTO (account profile from Laravel). Never mutated by booking.</li>
 *   <li>{@link BookingRequest.Passenger} — booking <b>snapshot</b> (mutable, Serializable, frozen at
 *       validation time so later profile edits cannot mutate historical booking data).</li>
 *   <li>{@link CreateBookingRequest.TravellerRequest} — API <b>out</b> DTO (snake_case JSON for
 *       POST /v1/bookings). Field names are contractual — do not rename.</li>
 *   <li>BookingDetailDto.TravellerInfo / ProfileStatsDto.UserInfo — read-back projections, not mapped.</li>
 * </ul>
 *
 * <p>Known contract limits (backend has no fields for these — reported, not worked around):
 * address, passport/ic document paths and emergency contacts are carried in the snapshot
 * but have no TravellerRequest counterpart and are therefore not sent.
 */
public final class TravellerMapper {

    private TravellerMapper() {}

    private static String orEmpty(String s) {
        return s != null ? s : "";
    }

    public static String defaultTitle(Context context) {
        return context != null ? context.getString(R.string.default_title_mr) : "Mr";
    }

    public static String defaultCountry(Context context) {
        return context != null ? context.getString(R.string.default_country) : "Malaysia";
    }

    public static String defaultNationality(Context context) {
        return context != null ? context.getString(R.string.default_nationality) : "Malaysian";
    }

    /**
     * Lead traveller snapshot from the account profile. Phone/email/address fall back to
     * the supplied session values when the profile lacks them (same as before).
     */
    public static BookingRequest.Passenger leadFromUser(
            Context context, UserDto user,
            String phoneFallback, String emailFallback, String addressFallback) {
        BookingRequest.Passenger lead = new BookingRequest.Passenger();
        lead.isLead = true;
        lead.title = defaultTitle(context);
        lead.fullName = user != null ? orEmpty(user.name) : "";
        lead.icNumber = user != null ? orEmpty(user.icNumber) : "";
        lead.passportNumber = user != null ? orEmpty(user.passportNumber) : "";
        lead.passportExpiryDate = user != null ? orEmpty(user.passportExpiryDate) : "";
        lead.issuingCountry = (user != null && user.issuingCountry != null)
                ? user.issuingCountry : defaultCountry(context);
        lead.gender = user != null ? orEmpty(user.gender) : "";
        lead.dateOfBirth = user != null ? orEmpty(user.dateOfBirth) : "";
        lead.nationality = (user != null && user.nationality != null)
                ? user.nationality : defaultNationality(context);
        lead.clothesSize = user != null ? orEmpty(user.clothesSize) : "";
        lead.icPassportNumber = !lead.passportNumber.isEmpty() ? lead.passportNumber : lead.icNumber;
        lead.phoneNumber = (user != null && user.phone != null) ? user.phone : orEmpty(phoneFallback);
        lead.email = (user != null && user.email != null) ? user.email : orEmpty(emailFallback);
        lead.address = (user != null && user.address != null) ? user.address : orEmpty(addressFallback);
        lead.isComplete = true;
        return lead;
    }

    /**
     * Snapshot passenger → API traveller. {@code forceLead} preserves the historical
     * rule that the first traveller is always flagged lead.
     */
    public static CreateBookingRequest.TravellerRequest toTravellerRequest(
            BookingRequest.Passenger p, boolean forceLead) {
        CreateBookingRequest.TravellerRequest tr = new CreateBookingRequest.TravellerRequest();
        if (p == null) {
            tr.isLead = forceLead;
            return tr;
        }
        tr.title = p.title;
        tr.fullName = p.fullName;
        tr.icNumber = p.icNumber;
        tr.passportNumber = p.passportNumber;
        tr.passportExpiryDate = p.passportExpiryDate;
        tr.issuingCountry = p.issuingCountry;
        tr.gender = p.gender;
        tr.dateOfBirth = p.dateOfBirth;
        tr.nationality = p.nationality;
        tr.clothesSize = p.clothesSize;
        tr.mahramIndex = p.mahramIndex;
        tr.relationship = p.relationship;
        tr.icPassport = p.icPassportNumber;
        tr.phone = p.phoneNumber;
        tr.email = p.email;
        tr.isLead = p.isLead || forceLead;
        return tr;
    }
}
