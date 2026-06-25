package com.example.feedsystem.video.service;

import com.example.feedsystem.common.BusinessException;
import com.example.feedsystem.storage.PathMultipartFile;
import com.example.feedsystem.storage.StorageService;
import com.example.feedsystem.video.dto.ChunkStatusResponse;
import com.example.feedsystem.video.dto.ChunkUploadSession;
import com.example.feedsystem.video.dto.InitChunkUploadRequest;
import com.example.feedsystem.video.dto.InitChunkUploadResponse;
import com.example.feedsystem.video.dto.UploadResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChunkUploadService {

    private static final long MAX_VIDEO_SIZE = 200L * 1024 * 1024;
    private static final Duration SESSION_TTL = Duration.ofHours(24);

    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final VideoUploadService videoUploadService;
    private final StorageService storageService;

    @Value("${feedsystem.upload.root:.run/uploads}")
    private String uploadRoot;

    public InitChunkUploadResponse init(Long accountId, InitChunkUploadRequest request) {
        if (!request.getFilename().toLowerCase().endsWith(".mp4")) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "invalid file format");
        }
        if (request.getFileSize() > MAX_VIDEO_SIZE) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "file too large");
        }
        String hashKey = hashKey(accountId, request.getFileHash());
        String existingUploadId = redisGet(hashKey);
        if (existingUploadId != null) {
            ChunkUploadSession existing = requireSession(existingUploadId);
            return new InitChunkUploadResponse(existingUploadId, uploadedChunks(existing));
        }

        ChunkUploadSession session = new ChunkUploadSession();
        session.setUploadId(UUID.randomUUID().toString().replace("-", ""));
        session.setAccountId(accountId);
        session.setFilename(request.getFilename());
        session.setFileSize(request.getFileSize());
        session.setChunkSize(request.getChunkSize());
        session.setTotalChunks(request.getTotalChunks());
        session.setFileHash(request.getFileHash());
        saveSession(session);
        redisSet(hashKey, session.getUploadId());
        return new InitChunkUploadResponse(session.getUploadId(), List.of());
    }

    public Integer uploadChunk(Long accountId, String uploadId, Integer chunkIndex, String chunkHash, MultipartFile file) {
        ChunkUploadSession session = requireOwnedSession(accountId, uploadId);
        if (chunkIndex < 0 || chunkIndex >= session.getTotalChunks()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "invalid chunk index");
        }
        Path chunkPath = chunkDirectory(uploadId).resolve(chunkIndex.toString());
        if (isChunkUploaded(uploadId, chunkIndex) && Files.exists(chunkPath)) {
            long uploadedCount = uploadedChunkCount(uploadId);
            log.info("Chunk already uploaded: uploadId={}, chunkIndex={}, uploadedCount={}, totalChunks={}",
                    uploadId, chunkIndex, uploadedCount, session.getTotalChunks());
            return chunkIndex;
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "file is required");
        }
        String actualHash = md5(file);
        if (!actualHash.equalsIgnoreCase(chunkHash)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "invalid chunk hash");
        }

        Path directory = chunkDirectory(uploadId);
        try {
            Files.createDirectories(directory);
            file.transferTo(chunkPath);
        } catch (IOException ex) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "failed to save chunk");
        }
        long uploadedCount = markChunkUploaded(uploadId, chunkIndex);
        log.info("Chunk uploaded: uploadId={}, chunkIndex={}, uploadedCount={}, totalChunks={}",
                uploadId, chunkIndex, uploadedCount, session.getTotalChunks());
        return chunkIndex;
    }

    public ChunkStatusResponse status(Long accountId, String uploadId) {
        ChunkUploadSession session = requireOwnedSession(accountId, uploadId);
        List<Integer> uploaded = uploadedChunks(session);
        log.info("Chunk status: uploadId={}, chunkIndex={}, uploadedCount={}, totalChunks={}",
                uploadId, "-", uploaded.size(), session.getTotalChunks());
        return new ChunkStatusResponse(uploadId, uploaded, session.getTotalChunks());
    }

    public UploadResponse complete(Long accountId, String uploadId) {
        ChunkUploadSession session = requireOwnedSession(accountId, uploadId);
        long uploadedCount = uploadedChunkCountWithLegacyMigration(session);
        log.info("Completing chunk upload: uploadId={}, chunkIndex={}, uploadedCount={}, totalChunks={}",
                uploadId, "-", uploadedCount, session.getTotalChunks());
        if (uploadedCount != session.getTotalChunks()) {
            log.warn("Chunk upload incomplete: uploadId={}, chunkIndex={}, uploadedCount={}, totalChunks={}",
                    uploadId, "-", uploadedCount, session.getTotalChunks());
            throw new BusinessException(HttpStatus.BAD_REQUEST, "not all chunks uploaded");
        }
        verifyChunkFiles(session);

        String objectKey = videoUploadService.videoObjectKey(accountId);
        Path target = chunkDirectory(uploadId).resolve("merged.mp4");
        try {
            mergeChunks(session, target);
            storageService.upload(new PathMultipartFile(target, "file", session.getFilename(), "video/mp4"), objectKey);
            deleteDirectory(chunkDirectory(uploadId));
            deleteUploadState(session);
        } catch (IOException ex) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "failed to merge chunks");
        }
        return UploadResponse.video(storageService.getUrl(objectKey));
    }

    private void mergeChunks(ChunkUploadSession session, Path target) throws IOException {
        try (OutputStream outputStream = Files.newOutputStream(target)) {
            for (int i = 0; i < session.getTotalChunks(); i++) {
                Path chunk = chunkDirectory(session.getUploadId()).resolve(Integer.toString(i));
                if (!Files.exists(chunk)) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "not all chunks uploaded");
                }
                Files.copy(chunk, outputStream);
            }
        } catch (IOException | RuntimeException ex) {
            Files.deleteIfExists(target);
            throw ex;
        }
    }

    private ChunkUploadSession requireOwnedSession(Long accountId, String uploadId) {
        ChunkUploadSession session = requireSession(uploadId);
        if (!accountId.equals(session.getAccountId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "chunk session forbidden");
        }
        return session;
    }

    private ChunkUploadSession requireSession(String uploadId) {
        String value = redisGet(sessionKey(uploadId));
        if (value == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "chunk session not found");
        }
        try {
            return objectMapper.readValue(value, ChunkUploadSession.class);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "chunk session not found");
        }
    }

    private void saveSession(ChunkUploadSession session) {
        try {
            redisTemplate.opsForValue().set(sessionKey(session.getUploadId()), objectMapper.writeValueAsString(session), SESSION_TTL);
            redisTemplate.expire(hashKey(session.getAccountId(), session.getFileHash()), SESSION_TTL);
        } catch (JsonProcessingException | DataAccessException ex) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "chunk upload unavailable");
        }
    }

    private boolean isChunkUploaded(String uploadId, Integer chunkIndex) {
        try {
            return Boolean.TRUE.equals(redisTemplate.opsForSet()
                    .isMember(chunkSetKey(uploadId), chunkIndex.toString()));
        } catch (DataAccessException ex) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "chunk upload unavailable");
        }
    }

    private long markChunkUploaded(String uploadId, Integer chunkIndex) {
        try {
            redisTemplate.opsForSet().add(chunkSetKey(uploadId), chunkIndex.toString());
            redisTemplate.expire(chunkSetKey(uploadId), SESSION_TTL);
            Long count = redisTemplate.opsForSet().size(chunkSetKey(uploadId));
            return count == null ? 0 : count;
        } catch (DataAccessException ex) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "chunk upload unavailable");
        }
    }

    private long uploadedChunkCount(String uploadId) {
        try {
            Long count = redisTemplate.opsForSet().size(chunkSetKey(uploadId));
            return count == null ? 0 : count;
        } catch (DataAccessException ex) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "chunk upload unavailable");
        }
    }

    private long uploadedChunkCountWithLegacyMigration(ChunkUploadSession session) {
        migrateLegacyUploadedChunks(session);
        return uploadedChunkCount(session.getUploadId());
    }

    private List<Integer> uploadedChunks(ChunkUploadSession session) {
        migrateLegacyUploadedChunks(session);
        try {
            Set<String> members = redisTemplate.opsForSet().members(chunkSetKey(session.getUploadId()));
            if (members == null || members.isEmpty()) {
                return List.of();
            }
            return members.stream().map(Integer::valueOf).sorted().toList();
        } catch (DataAccessException | NumberFormatException ex) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "chunk upload unavailable");
        }
    }

    private void migrateLegacyUploadedChunks(ChunkUploadSession session) {
        if (uploadedChunkCount(session.getUploadId()) > 0 || session.getUploadedChunks().isEmpty()) {
            return;
        }
        try {
            String[] legacyChunks = session.getUploadedChunks().stream()
                    .map(String::valueOf)
                    .toArray(String[]::new);
            redisTemplate.opsForSet().add(chunkSetKey(session.getUploadId()), legacyChunks);
            redisTemplate.expire(chunkSetKey(session.getUploadId()), SESSION_TTL);
        } catch (DataAccessException ex) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "chunk upload unavailable");
        }
    }

    private void verifyChunkFiles(ChunkUploadSession session) {
        Path directory = chunkDirectory(session.getUploadId());
        for (int i = 0; i < session.getTotalChunks(); i++) {
            Path chunk = directory.resolve(Integer.toString(i));
            if (!Files.isRegularFile(chunk)) {
                log.warn("Chunk file missing: uploadId={}, chunkIndex={}, uploadedCount={}, totalChunks={}",
                        session.getUploadId(), i, uploadedChunkCount(session.getUploadId()), session.getTotalChunks());
                throw new BusinessException(HttpStatus.BAD_REQUEST, "not all chunks uploaded");
            }
        }
    }

    private void deleteUploadState(ChunkUploadSession session) {
        try {
            redisTemplate.delete(List.of(
                    sessionKey(session.getUploadId()),
                    hashKey(session.getAccountId(), session.getFileHash()),
                    chunkSetKey(session.getUploadId())
            ));
        } catch (DataAccessException ex) {
            log.warn("Failed to delete chunk upload Redis state: uploadId={}", session.getUploadId(), ex);
        }
    }

    private String redisGet(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (DataAccessException ex) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "chunk upload unavailable");
        }
    }

    private void redisSet(String key, String value) {
        try {
            redisTemplate.opsForValue().set(key, value, SESSION_TTL);
        } catch (DataAccessException ex) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "chunk upload unavailable");
        }
    }

    private String md5(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            try (InputStream inputStream = file.getInputStream();
                 DigestInputStream digestInputStream = new DigestInputStream(inputStream, digest)) {
                digestInputStream.transferTo(OutputStream.nullOutputStream());
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException | IOException ex) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "failed to calculate chunk hash");
        }
    }

    private Path chunkDirectory(String uploadId) {
        return Path.of(uploadRoot).resolve("tmp").resolve(uploadId);
    }

    private String sessionKey(String uploadId) {
        return "v1:chunk_upload:" + uploadId;
    }

    private String hashKey(Long accountId, String fileHash) {
        return "v1:chunk_upload_hash:" + accountId + ":" + fileHash;
    }

    private String chunkSetKey(String uploadId) {
        return "v1:chunk_upload_chunks:" + uploadId;
    }

    private void deleteDirectory(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        try (var paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                }
            });
        }
    }
}
