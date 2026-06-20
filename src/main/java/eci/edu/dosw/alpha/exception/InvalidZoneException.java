package eci.edu.dosw.alpha.exception;

public class InvalidZoneException extends RuntimeException {

    public InvalidZoneException(String zone) {
        super("Zona inválida: '" + zone + "'. Consulte GET /api/zone/catalog para ver las zonas disponibles.");
    }
}
