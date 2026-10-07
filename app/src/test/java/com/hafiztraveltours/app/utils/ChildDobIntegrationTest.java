package com.hafiztraveltours.app.utils;

import com.hafiztraveltours.app.models.BookingRequest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class ChildDobIntegrationTest {

    @Test
    public void testMultipleChildrenIndependentDobState() {
        BookingRequest.ChildConfig child1 = new BookingRequest.ChildConfig();
        child1.dateOfBirth = "2018-05-14";

        BookingRequest.ChildConfig child2 = new BookingRequest.ChildConfig();
        child2.dateOfBirth = "2021-09-20";

        assertNotEquals(child1.dateOfBirth, child2.dateOfBirth);
        assertEquals("2018-05-14", child1.dateOfBirth);
        assertEquals("2021-09-20", child2.dateOfBirth);

        int ageChild1 = PackagePricingCalculator.calculateAge(child1.dateOfBirth);
        int ageChild2 = PackagePricingCalculator.calculateAge(child2.dateOfBirth);

        assertNotEquals(ageChild1, ageChild2);
    }
}
