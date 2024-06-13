package com.example.findit;

public class ImageData {
    private String name;
    private String imageUrl;
    private String location;
    private String creationDate;

    public ImageData() {
        // Default constructor
    }

    public ImageData(String name, String imageUrl, String location, String creationDate) {
        this.name = name;
        this.imageUrl = imageUrl;
        this.location = location;
        this.creationDate = creationDate;
    }

    public String getName() {
        return name;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getLocation() {
        return location;
    }

    public String getCreationDate() {
        return creationDate;
    }
}
