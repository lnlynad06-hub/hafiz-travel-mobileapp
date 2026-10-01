package com.hafiztraveltours.app.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class CancellationQuoteDto implements Serializable {
    @SerializedName("is_cancellable")
    public boolean isCancellable;

    @SerializedName("working_days_to_departure")
    public Integer workingDaysToDeparture;

    @SerializedName("departure_date")
    public String departureDate;

    @SerializedName("total_amount")
    public double totalAmount;

    @SerializedName("paid_amount")
    public double paidAmount;

    @SerializedName("required_deposit")
    public double requiredDeposit;

    @SerializedName("charge_percentage")
    public double chargePercentage;

    @SerializedName("charge_amount")
    public double chargeAmount;

    @SerializedName("refund_amount")
    public double refundAmount;

    @SerializedName("policy_rule")
    public String policyRule;

    @SerializedName("policy_terms")
    public String policyTerms;
}
