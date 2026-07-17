package eci.edu.dosw.alpha.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Notifica a GamificationService cuando un usuario entra a una geocerca del
 * campus, vía HTTP (POST /api/v1/gamification/activity). GeoService no tiene
 * cliente RabbitMQ propio: se usa la URL ya configurada en
 * `gamification.service.url` (application.properties).
 */
@Service
public class GamificationActivityClient {

    private static final Logger log = LoggerFactory.getLogger(GamificationActivityClient.class);

    private final RestTemplate restTemplate;
    private final String activityUrl;

    public GamificationActivityClient(RestTemplate restTemplate,
                                       @Value("${gamification.service.url}") String gamificationServiceUrl) {
        this.restTemplate = restTemplate;
        this.activityUrl = gamificationServiceUrl + "/api/v1/gamification/activity";
    }

    /**
     * Fire-and-forget: no debe bloquear ni romper el flujo de WebSocket de
     * ubicación si GamificationService está caído o lento.
     */
    @Async
    public void notifyCheckin(String userId, String activityType, String locationCode) {
        try {
            Map<String, Object> body = Map.of(
                    "userId", userId,
                    "activityType", activityType,
                    "metadata", Map.of("locationCode", locationCode)
            );
            restTemplate.postForEntity(activityUrl, body, Void.class);
        } catch (Exception e) {
            log.warn("No se pudo notificar check-in a GamificationService (userId={}, locationCode={}): {}",
                    userId, locationCode, e.getMessage());
        }
    }
}
