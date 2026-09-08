package com.example.feedsystem.video.service;

import com.example.feedsystem.common.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FfprobeServiceTests {
    private final FfprobeService probe = new FfprobeService(new ObjectMapper(), "ffprobe", 1);
    private static final String SILENT = """
        {"format":{"duration":"1.5"},"streams":[
          {"codec_type":"video","codec_name":"h264","width":320,"height":240,"disposition":{"attached_pic":0}}
        ]}
        """;

    @Test void silentVideoIsValid() { assertDoesNotThrow(() -> probe.validateJson(SILENT)); }
    @Test void returnsSilentVideoMetadataAndOriginalJson() {
        var result = probe.validateJson(SILENT);
        assertEquals(new java.math.BigDecimal("1.500"), result.durationSeconds());
        assertEquals(320, result.width());
        assertEquals(240, result.height());
        assertEquals("h264", result.videoCodec());
        assertFalse(result.hasAudio());
        assertNull(result.audioCodec());
        assertEquals(SILENT, result.rawJson());
    }
    @Test void findsAudioAfterVideoAndPrefersStreamDuration() {
        var result = probe.validateJson("""
            {"format":{"duration":"3"},"streams":[
              {"codec_type":"video","codec_name":"h264","width":320,"height":240,"duration":"2"},
              {"codec_type":"audio","codec_name":"aac"}]}
            """);
        assertTrue(result.hasAudio());
        assertEquals("aac", result.audioCodec());
        assertEquals(new java.math.BigDecimal("2.000"), result.durationSeconds());
    }
    @Test void audioOnlyIsRejected() {
        assertThrows(BusinessException.class, () -> probe.validateJson("""
            {"format":{"duration":"2"},"streams":[{"codec_type":"audio","codec_name":"aac"}]}
            """));
    }
    @Test void coverPictureIsNotVideo() {
        assertThrows(BusinessException.class, () -> probe.validateJson(SILENT.replace("\"attached_pic\":0", "\"attached_pic\":1")));
    }
    @Test void invalidDimensionsOrDurationAreRejected() {
        assertThrows(BusinessException.class, () -> probe.validateJson(SILENT.replace("320", "0")));
        assertThrows(BusinessException.class, () -> probe.validateJson(SILENT.replace("1.5", "0")));
        assertThrows(BusinessException.class, () -> probe.validateJson(SILENT.replace("1.5", "NaN")));
    }
    @Test void missingExecutableFailsClosed() throws Exception {
        FfprobeService service = spy(probe);
        doThrow(new IOException("missing")).when(service).start(any());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE,
                assertThrows(BusinessException.class, () -> service.validate(Path.of("video.mp4"))).getStatus());
    }
    @Test void successfulProcessOutputIsValidated() throws Exception {
        Process process = mock(Process.class);
        when(process.getOutputStream()).thenReturn(new ByteArrayOutputStream());
        when(process.getInputStream()).thenReturn(new ByteArrayInputStream(SILENT.getBytes()));
        when(process.waitFor(1, TimeUnit.SECONDS)).thenReturn(true);
        FfprobeService service = spy(probe);
        doReturn(process).when(service).start(any());
        assertDoesNotThrow(() -> service.validate(Path.of("video.mp4")));
    }
    @Test void timedOutProcessIsKilled() throws Exception {
        Process process = mock(Process.class);
        when(process.getOutputStream()).thenReturn(new ByteArrayOutputStream());
        when(process.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(process.isAlive()).thenReturn(true);
        FfprobeService service = spy(probe);
        doReturn(process).when(service).start(any());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE,
                assertThrows(BusinessException.class, () -> service.validate(Path.of("video.mp4"))).getStatus());
        verify(process).destroyForcibly();
    }
}
