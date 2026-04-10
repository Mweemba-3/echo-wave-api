package com.example.echo_wave.models;

import java.util.List;

public class HomeSection {
    private String sectionId;
    private String title;
    private SectionType type;
    private List<?> items;
    private String seeAllAction;

    public enum SectionType {
        RECENTLY_PLAYED,
        MADE_FOR_YOU,
        QUICK_PICKS,
        TOP_ARTISTS,
        POPULAR_ALBUMS,
        NEW_RELEASES,
        GENRES,
        RECOMMENDED_SONGS
    }

    public HomeSection(String sectionId, String title, SectionType type, List<?> items) {
        this.sectionId = sectionId;
        this.title = title;
        this.type = type;
        this.items = items;
    }

    // Getters and setters
    public String getSectionId() { return sectionId; }
    public void setSectionId(String sectionId) { this.sectionId = sectionId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public SectionType getType() { return type; }
    public void setType(SectionType type) { this.type = type; }

    public List<?> getItems() { return items; }
    public void setItems(List<?> items) { this.items = items; }

    public String getSeeAllAction() { return seeAllAction; }
    public void setSeeAllAction(String seeAllAction) { this.seeAllAction = seeAllAction; }
}