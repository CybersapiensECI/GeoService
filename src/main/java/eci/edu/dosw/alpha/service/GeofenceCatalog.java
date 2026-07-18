package eci.edu.dosw.alpha.service;

import eci.edu.dosw.alpha.model.CampusZone;
import eci.edu.dosw.alpha.model.GeoFenceZone;
import org.springframework.stereotype.Component;

import java.util.List;

import static eci.edu.dosw.alpha.model.CampusZone.*;

/**
 * Catálogo de geocercas reales del campus (Escuela Colombiana de Ingeniería
 * Julio Garavito, Bogotá). Coordenadas y radios verificados, ver
 * MONAS_GEOLOCALIZACION.md. activityType coincide 1:1 con ActivityType de
 * GamificationService.
 */
@Component
public class GeofenceCatalog {

    private static final String BUILDING_CHECKIN = "BUILDING_CHECKIN";
    private static final String CAFETERIA_CHECKIN = "CAFETERIA_CHECKIN";
    private static final String CAMPUS_ZONE_CHECKIN = "CAMPUS_ZONE_CHECKIN";

    private final List<GeoFenceZone> zones = List.of(
            zone(EDIFICIO_A, 4.782699855684707, -74.04264918379, 20, BUILDING_CHECKIN),
            zone(EDIFICIO_B, 4.7830399485075255, -74.04270964402168, 20, BUILDING_CHECKIN),
            zone(EDIFICIO_C, 4.782389074571274, -74.04247753949225, 20, BUILDING_CHECKIN),
            zone(EDIFICIO_D, 4.783130774183006, -74.04354547558216, 20, BUILDING_CHECKIN),
            zone(EDIFICIO_E, 4.782681035143461, -74.04385407917442, 20, BUILDING_CHECKIN),
            zone(EDIFICIO_F, 4.783590080825507, -74.04332219644444, 20, BUILDING_CHECKIN),
            zone(EDIFICIO_G, 4.78349313377599, -74.04288042271563, 20, BUILDING_CHECKIN),
            zone(EDIFICIO_H, 4.781775204352766, -74.04493800488173, 20, BUILDING_CHECKIN),
            zone(EDIFICIO_I, 4.781918241337216, -74.04435269456233, 20, BUILDING_CHECKIN),
            zone(CAFETERIA_REGIO, 4.782980848558539, -74.04400996864427, 15, CAFETERIA_CHECKIN),
            zone(CAFETERIA_2, 4.78323330585694, -74.0446541217452, 20, CAFETERIA_CHECKIN),
            zone(CAFETERIA_3, 4.783855976183257, -74.04583457286611, 15, CAFETERIA_CHECKIN),
            zone(CAFETERIA_4, 4.7823632440346735, -74.04299435757378, 15, CAFETERIA_CHECKIN),
            zone(LAGO, 4.783042637971193, -74.04428261122634, 30, CAMPUS_ZONE_CHECKIN),
            zone(REFLEXION, 4.78303796045251, -74.04448109469203, 15, CAMPUS_ZONE_CHECKIN),
            // Todo el campus (centro geometrico de las zonas anteriores,
            // radio que cubre los 9 edificios y las cafeterias): "estar en
            // la universidad" tambien cuenta como actividad, aunque el
            // usuario no pise ninguna zona puntual.
            zone(UNIVERSIDAD, 4.78285, -74.0442, 250, CAMPUS_ZONE_CHECKIN)
    );

    private static GeoFenceZone zone(CampusZone zone, double lat, double lng, double radiusMeters, String activityType) {
        return new GeoFenceZone(zone, lat, lng, radiusMeters, activityType);
    }

    public List<GeoFenceZone> getZones() {
        return zones;
    }
}
