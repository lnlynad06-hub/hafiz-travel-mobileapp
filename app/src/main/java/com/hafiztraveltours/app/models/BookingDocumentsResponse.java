package com.hafiztraveltours.app.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;

public class BookingDocumentsResponse implements Serializable {
    @SerializedName("status")
    public String status;

    @SerializedName("package_type")
    public String packageType;

    @SerializedName("selected_traveller_id")
    public Integer selectedTravellerId;

    @SerializedName("travellers")
    public List<TravellerDto> travellers;

    @SerializedName("data")
    public List<DocumentDto> data;

    public static class TravellerDto implements Serializable {
        @SerializedName("id")
        public int id;

        @SerializedName("name")
        public String name;

        @SerializedName("relationship")
        public String relationship;

        @SerializedName("role")
        public String role;

        @SerializedName("is_primary")
        public boolean isPrimary;

        @SerializedName("passport_no")
        public String passportNo;
    }
}
