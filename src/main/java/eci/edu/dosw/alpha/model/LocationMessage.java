package eci.edu.dosw.alpha.model;

import lombok.Data;

@Data
public class LocationMessage {

    private String userId;
    private double lat;
    private double lng;
    private long timestamp;

    public String getUserId(){
        return userId;
    }
}