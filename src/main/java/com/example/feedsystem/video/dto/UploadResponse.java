package com.example.feedsystem.video.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UploadResponse {

    private String url;

    @JsonProperty("play_url")
    private String playUrl;

    @JsonProperty("cover_url")
    private String coverUrl;

    public static UploadResponse video(String url) {
        return new UploadResponse(url, url, null);
    }

    public static UploadResponse cover(String url) {
        return new UploadResponse(url, null, url);
    }
}
