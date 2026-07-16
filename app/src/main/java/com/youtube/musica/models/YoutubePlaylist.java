package com.youtube.musica.models;

public class YoutubePlaylist {
    private String id;
    private String title;
    private String description;
    private String thumbnailUrl;
    private String videoId; // If it's a single video instead of a playlist

    public YoutubePlaylist(String id, String title, String description, String thumbnailUrl, String videoId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        this.videoId = videoId;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public String getVideoId() { return videoId; }
}
