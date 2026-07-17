package eci.edu.dosw.alpha.model;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public enum CampusZone {

    // Edificios académicos (9) — deben coincidir con `locationConstraint` sembrado
    // en GamificationService/MonaCatalogSeeder (EDIFICIO_A..EDIFICIO_I)
    EDIFICIO_A,
    EDIFICIO_B,
    EDIFICIO_C,
    EDIFICIO_D,
    EDIFICIO_E,
    EDIFICIO_F,
    EDIFICIO_G,
    EDIFICIO_H,
    EDIFICIO_I,

    // Cafeterías (4) — CAFETERIA_REGIO coincide con GamificationService.
    // CAFETERIA_2/3/4 son códigos placeholder: pendiente nombre real + coordenadas.
    CAFETERIA_REGIO,
    CAFETERIA_2,
    CAFETERIA_3,
    CAFETERIA_4,

    // Zonas de campus (monas ZEN_MASTER / MARATON_UNIVERSITARIA)
    LAGO,
    REFLEXION,

    // Instalaciones sin mona asociada (solo catálogo de Parches, RF29)
    BIBLIOTECA,
    CANCHA,
    AUDITORIO,
    ENTRADA_PRINCIPAL,
    PARQUEADERO;

    private static final Map<CampusZone, List<CampusZone>> NEARBY = new EnumMap<>(CampusZone.class);

    static {
        NEARBY.put(EDIFICIO_A, List.of(EDIFICIO_B, CAFETERIA_REGIO));
        NEARBY.put(EDIFICIO_B, List.of(EDIFICIO_A, EDIFICIO_C));
        NEARBY.put(EDIFICIO_C, List.of(EDIFICIO_B, EDIFICIO_D));
        NEARBY.put(EDIFICIO_D, List.of(EDIFICIO_C, EDIFICIO_E));
        NEARBY.put(EDIFICIO_E, List.of(EDIFICIO_D, EDIFICIO_F));
        NEARBY.put(EDIFICIO_F, List.of(EDIFICIO_E, EDIFICIO_G));
        NEARBY.put(EDIFICIO_G, List.of(EDIFICIO_F, EDIFICIO_H));
        NEARBY.put(EDIFICIO_H, List.of(EDIFICIO_G, EDIFICIO_I));
        NEARBY.put(EDIFICIO_I, List.of(EDIFICIO_H, CANCHA));
        NEARBY.put(CAFETERIA_REGIO, List.of(EDIFICIO_A, EDIFICIO_B));
        NEARBY.put(CAFETERIA_2, List.of(EDIFICIO_C, EDIFICIO_D));
        NEARBY.put(CAFETERIA_3, List.of(EDIFICIO_E, EDIFICIO_F));
        NEARBY.put(CAFETERIA_4, List.of(EDIFICIO_G, EDIFICIO_H));
        NEARBY.put(LAGO, List.of(REFLEXION));
        NEARBY.put(REFLEXION, List.of(LAGO));
        NEARBY.put(BIBLIOTECA, List.of(EDIFICIO_A, ENTRADA_PRINCIPAL));
        NEARBY.put(CANCHA, List.of(EDIFICIO_I, AUDITORIO));
        NEARBY.put(AUDITORIO, List.of(CANCHA, ENTRADA_PRINCIPAL));
        NEARBY.put(ENTRADA_PRINCIPAL, List.of(EDIFICIO_A, PARQUEADERO));
        NEARBY.put(PARQUEADERO, List.of(ENTRADA_PRINCIPAL));
    }

    public List<CampusZone> getNearby() {
        return NEARBY.getOrDefault(this, List.of());
    }
}
