package com.example.feedsystem.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.web.multipart.MultipartFile;

public class PathMultipartFile implements MultipartFile {
    private final Path path;
    private final String name;
    private final String originalFilename;
    private final String contentType;

    public PathMultipartFile(Path path, String name, String originalFilename, String contentType) {
        this.path = path;
        this.name = name;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getOriginalFilename() {
        return originalFilename;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public boolean isEmpty() {
        try {
            return Files.size(path) == 0;
        } catch (IOException ex) {
            return true;
        }
    }

    @Override
    public long getSize() {
        try {
            return Files.size(path);
        } catch (IOException ex) {
            return 0;
        }
    }

    @Override
    public byte[] getBytes() throws IOException {
        return Files.readAllBytes(path);
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return Files.newInputStream(path);
    }

    @Override
    public void transferTo(File dest) throws IOException, IllegalStateException {
        Files.copy(path, dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public void transferTo(Path dest) throws IOException, IllegalStateException {
        Files.copy(path, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }
}
