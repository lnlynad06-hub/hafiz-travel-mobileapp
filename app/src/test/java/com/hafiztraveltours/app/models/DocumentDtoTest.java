package com.hafiztraveltours.app.models;

import com.google.gson.Gson;
import com.hafiztraveltours.app.views.DocumentViewerDialog;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DocumentDtoTest {

    @Test
    public void testGsonDeserialization_withPreviewAndDownloadUrls() {
        String json = "{\n" +
                "  \"document_code\": \"passport\",\n" +
                "  \"title\": \"Passport\",\n" +
                "  \"status\": \"submitted\",\n" +
                "  \"document_id\": 101,\n" +
                "  \"file_name\": \"my_passport.jpg\",\n" +
                "  \"mime_type\": \"image/jpeg\",\n" +
                "  \"has_file\": true,\n" +
                "  \"preview_url\": \"http://127.0.0.1:8000/api/v1/documents/101/preview\",\n" +
                "  \"download_url\": \"http://127.0.0.1:8000/api/v1/documents/101/download\"\n" +
                "}";

        Gson gson = new Gson();
        DocumentDto dto = gson.fromJson(json, DocumentDto.class);

        assertNotNull(dto);
        assertEquals("passport", dto.documentCode);
        assertEquals("Passport", dto.title);
        assertEquals(Long.valueOf(101), dto.documentId);
        assertEquals("my_passport.jpg", dto.fileName);
        assertEquals("image/jpeg", dto.mimeType);
        assertTrue(dto.hasFile);
        assertEquals("http://127.0.0.1:8000/api/v1/documents/101/preview", dto.previewUrl);
        assertEquals("http://127.0.0.1:8000/api/v1/documents/101/download", dto.downloadUrl);
        assertEquals("http://127.0.0.1:8000/api/v1/documents/101/preview", dto.getEffectiveUrl());
    }

    @Test
    public void testGetEffectiveUrl_fallbackHierarchy() {
        DocumentDto dto = new DocumentDto();
        assertNull(dto.getEffectiveUrl());

        dto.filePath = "legacy/path.jpg";
        assertEquals("legacy/path.jpg", dto.getEffectiveUrl());

        dto.downloadUrl = "http://api.example.com/download/1";
        assertEquals("http://api.example.com/download/1", dto.getEffectiveUrl());

        dto.previewUrl = "http://api.example.com/preview/1";
        assertEquals("http://api.example.com/preview/1", dto.getEffectiveUrl());
    }

    @Test
    public void testNormalizeDocumentUrl_relativeUrl() {
        String relative = "v1/documents/1/preview";
        String normalized = DocumentViewerDialog.normalizeDocumentUrl(relative);
        assertTrue(normalized.endsWith("v1/documents/1/preview"));
    }

    @Test
    public void testNormalizeDocumentUrl_absoluteUrl() {
        String absolute = "http://127.0.0.1:8000/api/v1/documents/1/preview";
        String normalized = DocumentViewerDialog.normalizeDocumentUrl(absolute);
        assertEquals(absolute, normalized);
    }
}
