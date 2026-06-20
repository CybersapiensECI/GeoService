package eci.edu.dosw.alpha.model;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public enum CampusZone {

    BLOQUE_A,
    BLOQUE_B,
    BLOQUE_C,
    BLOQUE_D,
    BLOQUE_E,
    BLOQUE_F,
    BIBLIOTECA,
    CAFETERIA,
    CANCHA,
    AUDITORIO,
    ENTRADA_PRINCIPAL,
    PARQUEADERO;

    private static final Map<CampusZone, List<CampusZone>> NEARBY = new EnumMap<>(CampusZone.class);

    static {
        NEARBY.put(BLOQUE_A,         List.of(BLOQUE_B, CAFETERIA));
        NEARBY.put(BLOQUE_B,         List.of(BLOQUE_A, BLOQUE_C, CAFETERIA));
        NEARBY.put(BLOQUE_C,         List.of(BLOQUE_B, BLOQUE_D, BIBLIOTECA));
        NEARBY.put(BLOQUE_D,         List.of(BLOQUE_C, BLOQUE_E));
        NEARBY.put(BLOQUE_E,         List.of(BLOQUE_D, BLOQUE_F, CANCHA));
        NEARBY.put(BLOQUE_F,         List.of(BLOQUE_E, PARQUEADERO));
        NEARBY.put(BIBLIOTECA,       List.of(BLOQUE_C, ENTRADA_PRINCIPAL));
        NEARBY.put(CAFETERIA,        List.of(BLOQUE_A, BLOQUE_B, CANCHA));
        NEARBY.put(CANCHA,           List.of(CAFETERIA, BLOQUE_E, AUDITORIO));
        NEARBY.put(AUDITORIO,        List.of(CANCHA, ENTRADA_PRINCIPAL));
        NEARBY.put(ENTRADA_PRINCIPAL,List.of(AUDITORIO, BIBLIOTECA, PARQUEADERO));
        NEARBY.put(PARQUEADERO,      List.of(ENTRADA_PRINCIPAL, BLOQUE_F));
    }

    public List<CampusZone> getNearby() {
        return NEARBY.getOrDefault(this, List.of());
    }
}
