package eci.edu.dosw.alpha.model;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public enum CampusZone {

    // Edificios
    A,
    B,
    C,
    D,
    E,
    F,
    G,
    H,
    I,

    // Puntos de interés
    CAFE_PLANET,
    DIALIMENTOS,
    NATIVOS,
    HARVIES,
    COLISEO,

    // Entradas
    ENTRADA_PEATONAL,
    ENTRADA_VEHICULAR,

    // Parqueaderos
    PARQUEADERO_NORTE,
    PARQUEADERO_SUR;

    private static final Map<CampusZone, List<CampusZone>> NEARBY = new EnumMap<>(CampusZone.class);

    static {

        // Edificios
        NEARBY.put(A, List.of(B, C, ENTRADA_VEHICULAR));
        NEARBY.put(B, List.of(A, D, G, ENTRADA_PEATONAL));
        NEARBY.put(C, List.of(A, E, NATIVOS));
        NEARBY.put(D, List.of(B, F, DIALIMENTOS, CAFE_PLANET));
        NEARBY.put(E, List.of(C, I, NATIVOS));
        NEARBY.put(F, List.of(D, G, ENTRADA_PEATONAL));
        NEARBY.put(G, List.of(F, B, PARQUEADERO_NORTE));
        NEARBY.put(H, List.of(I, PARQUEADERO_SUR));
        NEARBY.put(I, List.of(H, E));

        // Puntos de interés
        NEARBY.put(CAFE_PLANET, List.of(D, DIALIMENTOS));
        NEARBY.put(DIALIMENTOS, List.of(D, CAFE_PLANET));
        NEARBY.put(NATIVOS, List.of(C, E));
        NEARBY.put(HARVIES, List.of(COLISEO));
        NEARBY.put(COLISEO, List.of(HARVIES));

        // Entradas
        NEARBY.put(ENTRADA_PEATONAL, List.of(B, F, PARQUEADERO_NORTE));
        NEARBY.put(ENTRADA_VEHICULAR, List.of(A, C, PARQUEADERO_SUR));

        // Parqueaderos
        NEARBY.put(PARQUEADERO_NORTE, List.of(G, ENTRADA_PEATONAL));
        NEARBY.put(PARQUEADERO_SUR, List.of(H, ENTRADA_VEHICULAR));
    }

    public List<CampusZone> getNearby() {
        return NEARBY.getOrDefault(this, List.of());
    }
}