package eci.edu.dosw.alpha.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ZoneResponse {

    private String currentZone;
    private List<String> nearbyParches;
}
