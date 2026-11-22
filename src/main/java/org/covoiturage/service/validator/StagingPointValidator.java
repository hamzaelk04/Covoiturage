package org.covoiturage.service.validator;

import org.covoiturage.dto.stagingPoint.StagingPointRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Component
public class StagingPointValidator {
    public List<StagingPointRequest> validateAndSort(List<StagingPointRequest> points, LocalDateTime now) {
        if (points == null || points.size() < 2) {
            throw invalid("Un trajet doit contenir au moins deux points d'arrêt.");
        }

        List<StagingPointRequest> sorted = points.stream()
                .sorted(Comparator.comparingInt(StagingPointRequest::getStopOrder))
                .toList();

        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i).getStopOrder() != i) {
                throw invalid("Les ordres des points d'arrêt doivent être 0, 1, 2, ... sans doublon ni trou (0 = départ).");
            }
        }

        StagingPointRequest first = sorted.getFirst();
        if (!first.getPassingTime().isAfter(now)) {
            throw invalid("La date de départ doit être dans le futur.");
        }
        if (first.getPrice().compareTo(BigDecimal.ZERO) != 0) {
            throw invalid("Le prix du point de départ (ordre 0) doit être égal à 0.");
        }

        Set<String> cities = new HashSet<>();
        sorted.forEach(p -> {
            if (!cities.add(p.getCity().toLowerCase(Locale.ROOT))) {
                throw invalid("La ville '" + p.getCity() + "' apparaît plusieurs fois sur le parcours.");
            }
        });

        for (int i = 1; i < sorted.size(); i++) {
            StagingPointRequest prev = sorted.get(i - 1);
            StagingPointRequest curr = sorted.get(i);
            if (!curr.getPassingTime().isAfter(prev.getPassingTime())) {
                throw invalid("L'heure de passage à " + curr.getCity()
                        + " doit être strictement postérieure à celle de " + prev.getCity() + ".");
            }
            if (curr.getPrice().compareTo(prev.getPrice()) <= 0) {
                throw invalid("Le prix à " + curr.getCity()
                        + " doit être strictement supérieur à celui de " + prev.getCity() + ".");
            }
        }
        return sorted;
    }

    private ResponseStatusException invalid(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
