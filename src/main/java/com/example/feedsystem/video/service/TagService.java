package com.example.feedsystem.video.service;

import com.example.feedsystem.video.mapper.VideoMapper;
import com.example.feedsystem.video.model.TagDO;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TagService {

    private static final Pattern TAG_PATTERN = Pattern.compile("#([\\p{L}\\p{N}_-]+)");

    private final VideoMapper videoMapper;

    public Set<String> extractTags(String title, String description) {
        Set<String> tags = new LinkedHashSet<>();
        collectTags(tags, title);
        collectTags(tags, description);
        return tags;
    }

    public Long selectOrInsert(String name) {
        TagDO existing = videoMapper.selectTagByName(name);
        if (existing != null) {
            return existing.getId();
        }
        TagDO tag = new TagDO();
        tag.setName(name);
        try {
            videoMapper.insertTag(tag);
            return tag.getId();
        } catch (DuplicateKeyException ex) {
            return videoMapper.selectTagByName(name).getId();
        }
    }

    private void collectTags(Set<String> tags, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        Matcher matcher = TAG_PATTERN.matcher(text);
        while (matcher.find()) {
            tags.add(matcher.group(1));
        }
    }
}
