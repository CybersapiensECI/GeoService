package eci.edu.dosw.alpha.model;

import lombok.Data;

@Data
public class UserZonePreference {

    private String userId;
    private CampusZone campusZone;
    private boolean geoLocationEnabled;
    private long updatedAt;
}
