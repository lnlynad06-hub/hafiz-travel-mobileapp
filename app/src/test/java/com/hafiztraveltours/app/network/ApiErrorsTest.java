package com.hafiztraveltours.app.network;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import org.junit.Test;
import retrofit2.Response;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class ApiErrorsTest {

    @Test
    public void testIsAccountUnverified_returnsTrue_whenCodeIsAccountUnverified() {
        String json = "{\"status\":\"error\",\"code\":\"ACCOUNT_UNVERIFIED\",\"message\":\"Please verify your account before logging in.\"}";
        Response<?> response = Response.error(403, ResponseBody.create(MediaType.parse("application/json"), json));

        assertTrue(ApiErrors.isAccountUnverified(response));
    }

    @Test
    public void testIsAccountUnverified_returnsFalse_whenSuspendedOrGeneric403() {
        String json = "{\"status\":\"error\",\"message\":\"Your account has been suspended. Please contact administrator.\"}";
        Response<?> response = Response.error(403, ResponseBody.create(MediaType.parse("application/json"), json));

        assertFalse(ApiErrors.isAccountUnverified(response));
    }

    @Test
    public void testIsAccountUnverified_returnsFalse_whenNot403() {
        String json = "{\"status\":\"error\",\"message\":\"Invalid email or password.\"}";
        Response<?> response = Response.error(401, ResponseBody.create(MediaType.parse("application/json"), json));

        assertFalse(ApiErrors.isAccountUnverified(response));
    }

    @Test
    public void testGetRawErrorBody_cachesBodyAcrossMultipleCalls() {
        String json = "{\"status\":\"error\",\"message\":\"Invalid credentials.\"}";
        Response<?> response = Response.error(401, ResponseBody.create(MediaType.parse("application/json"), json));

        String firstRead = ApiErrors.getRawErrorBody(response);
        String secondRead = ApiErrors.getRawErrorBody(response);

        assertEquals(json, firstRead);
        assertEquals(json, secondRead);
    }
}
