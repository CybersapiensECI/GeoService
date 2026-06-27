package eci.edu.dosw.alpha.model;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public enum CampusZone {

    // Bloques académicos
    BLOQUE_A,
    BLOQUE_B,
    BLOQUE_C,
    BLOQUE_D,
    BLOQUE_E,
    BLOQUE_F,

    // Instalaciones
    BIBLIOTECA,
    CAFETERIA,
    CANCHA,
    AUDITORIO,

    // Accesos y servicios
    ENTRADA_PRINCIPAL,
    PARQUEADERO;

    private static final Map<CampusZone, List<CampusZone>> NEARBY = new EnumMap<>(CampusZone.class);

    static {
        NEARBY.put(BLOQUE_A, List.of(BLOQUE_B, CAFETERIA));
        NEARBY.put(BLOQUE_B, List.of(BLOQUE_A, BLOQUE_C));
        NEARBY.put(BLOQUE_C, List.of(BLOQUE_B, BLOQUE_D));
        NEARBY.put(BLOQUE_D, List.of(BLOQUE_C, BLOQUE_E));
        NEARBY.put(BLOQUE_E, List.of(BLOQUE_D, BLOQUE_F));
        NEARBY.put(BLOQUE_F, List.of(BLOQUE_E, CANCHA));
        NEARBY.put(BIBLIOTECA, List.of(BLOQUE_A, ENTRADA_PRINCIPAL));
        NEARBY.put(CAFETERIA, List.of(BLOQUE_A, BLOQUE_B));
        NEARBY.put(CANCHA, List.of(BLOQUE_F, AUDITORIO));
        NEARBY.put(AUDITORIO, List.of(CANCHA, ENTRADA_PRINCIPAL));
        NEARBY.put(ENTRADA_PRINCIPAL, List.of(BLOQUE_A, PARQUEADERO));
        NEARBY.put(PARQUEADERO, List.of(ENTRADA_PRINCIPAL));
    }

    public List<CampusZone> getNearby() {
        return NEARBY.getOrDefault(this, List.of());
    }
}
