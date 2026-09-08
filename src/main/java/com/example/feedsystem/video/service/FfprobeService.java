package com.example.feedsystem.video.service;

import com.example.feedsystem.common.BusinessException;
import com.example.feedsystem.video.dto.VideoProbeResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FfprobeService {
    private final ObjectMapper mapper;
    private final String executable;
    private final long timeoutSeconds;
    private final Semaphore permits = new Semaphore(2);

    public FfprobeService(ObjectMapper mapper,
            @Value("${feedsystem.media.ffprobe-path:ffprobe}") String executable,
            @Value("${feedsystem.media.probe-timeout-seconds:20}") long timeoutSeconds) {
        this.mapper = mapper;
        this.executable = executable;
        this.timeoutSeconds = Math.max(1, timeoutSeconds);
    }

    public VideoProbeResult validate(Path file) {
        if (!permits.tryAcquire()) throw unavailable("media validation busy; retry later");
        Process process = null;
        ExecutorService reader = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "ffprobe-output");
            thread.setDaemon(true);
            return thread;
        });
        try {
            process = start(file);
            process.getOutputStream().close();
            Process running = process;
            Future<byte[]> output = reader.submit(() -> {
                try (var stream = running.getInputStream()) {
                    byte[] bytes = stream.readNBytes(65537);
                    if (bytes.length > 65536) throw new IOException("ffprobe output too large");
                    return bytes;
                }
            });
            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                throw unavailable("media validation timed out; retry later");
            }
            if (process.exitValue() != 0) throw invalid("unrecognized or damaged video");
            return validateJson(new String(output.get(1, TimeUnit.SECONDS), StandardCharsets.UTF_8));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw unavailable("media validation interrupted");
        } catch (IOException | ExecutionException | TimeoutException ex) {
            log.warn("FFprobe execution failed: executable={}", executable, ex);
            throw unavailable("ffprobe unavailable; check executable path and server logs");
        } finally {
            if (process != null && process.isAlive()) process.destroyForcibly();
            reader.shutdownNow();
            permits.release();
        }
    }

    // No shell interpolation, URLs, playlists, or network protocols.
    Process start(Path file) throws IOException {
        return new ProcessBuilder(executable, "-v", "error",
                "-protocol_whitelist", "file", "-format_whitelist", "mov",
                "-show_entries", "format=duration:stream=codec_type,codec_name,width,height,duration:stream_disposition=attached_pic",
                "-of", "json", file.toAbsolutePath().normalize().toString())
                .redirectError(ProcessBuilder.Redirect.DISCARD).start();
    }

    VideoProbeResult validateJson(String json) {
        final JsonNode root;
        try {
            root = mapper.readTree(json);
        } catch (IOException ex) {
            throw unavailable("invalid ffprobe response");
        }
        if (root == null) throw invalid("missing video metadata");
        boolean hasAudio = false;
        String audioCodec = null;
        for (JsonNode stream : root.path("streams")) {
            if ("audio".equals(stream.path("codec_type").asText())) {
                hasAudio = true;
                String codec = stream.path("codec_name").asText();
                audioCodec = codec.isBlank() || "unknown".equals(codec) ? null : codec;
                break;
            }
        }
        for (JsonNode stream : root.path("streams")) {
            if (!"video".equals(stream.path("codec_type").asText())
                    || stream.path("disposition").path("attached_pic").asInt() == 1) continue;
            String codec = stream.path("codec_name").asText();
            if (codec.isBlank() || "unknown".equals(codec)
                    || stream.path("width").asInt() <= 0 || stream.path("height").asInt() <= 0) continue;
            double duration = stream.path("duration").asDouble(0);
            if (!(duration > 0)) duration = root.path("format").path("duration").asDouble(0);
            if (Double.isFinite(duration) && duration > 0) {
                BigDecimal seconds = BigDecimal.valueOf(duration).setScale(3, RoundingMode.HALF_UP);
                if (seconds.signum() <= 0 || seconds.precision() > 12) continue;
                return new VideoProbeResult(seconds, stream.path("width").asInt(),
                        stream.path("height").asInt(), codec, hasAudio, audioCodec, json);
            }
        }
        // Audio is deliberately optional; attached cover pictures do not count as video.
        throw invalid("video requires a valid video stream, dimensions and positive duration");
    }

    private BusinessException invalid(String message) {
        return new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, message);
    }
    private BusinessException unavailable(String message) {
        return new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, message);
    }
}
