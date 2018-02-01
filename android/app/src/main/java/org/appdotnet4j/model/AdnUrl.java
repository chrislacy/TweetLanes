package org.appdotnet4j.model;

import twitter4j.URLEntity;

public class AdnUrl implements URLEntity {

    private String text;
    private String url;

    public AdnUrl(String text, String url) {
        this.text = text;
        this.url = url;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public String getURL() {
        return url;
    }

    @Override
    public String getExpandedURL() {
        return url;
    }

    @Override
    public String getDisplayURL() {
        return text;
    }

    @Override
    public int getStart() {
        return 0;
    }

    @Override
    public int getEnd() {
        return 0;
    }
}
