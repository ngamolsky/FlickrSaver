package com.example.android.flickrsaver;

class FlickrSearchResponse {

    private String id;
    private String server;
    private String farm;
    private String secret;

    FlickrSearchResponse(String id, String server,
            String farm, String secret) {
        this.id = id;
        this.server = server;
        this.farm = farm;
        this.secret = secret;
    }

    String getId() {
        return id;
    }

    String getServer() {
        return server;
    }

    String getFarm() {
        return farm;
    }

    String getSecret() {
        return secret;
    }


}
