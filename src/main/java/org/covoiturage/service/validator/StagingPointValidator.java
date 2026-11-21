package org.covoiturage.service.validator;

import org.covoiturage.dto.stagingPoint.StagingPointRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Component
public class StagingPointValidator {
    public List<StagingPointRequest> validateAndSort(List<StagingPointRequest> points, LocalDateTime now) {
        if (points == null || points.size() < 2) {
//            throw new InvalidRequestException("Un trajet doit contenir au moins deux points d'arrêt.");
            throw new RuntimeException();
        }

        List<StagingPointRequest> sorted = points.stream()
                .sorted(Comparator.comparingInt(StagingPointRequest::getStopOrder))
                .toList();

        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i).getStopOrder() != i) {
//                throw new InvalidRequestException(
//                        "Les ordres des points d'arrêt doivent être 0, 1, 2, ... sans doublon ni trou (0 = départ).");
                throw new RuntimeException();
            }
        }

        StagingPointRequest first = sorted.getFirst();
        if (!first.getPassingTime().isAfter(now)) {
//            throw new InvalidRequestException("La date de départ doit être dans le futur.");
            throw new RuntimeException();
        }
        if (first.getPrice().compareTo(BigDecimal.ZERO) != 0) {
//            throw new InvalidRequestException("Le prix du point de départ (ordre 0) doit être égal à 0.");
            throw new RuntimeException();
        }

        Set<String> cities = new HashSet<>();
        sorted.forEach(p -> {
            if (!cities.add(p.getCity().toLowerCase(Locale.ROOT))) {
//                throw new InvalidRequestException("La ville '" + p.city() + "' apparaît plusieurs fois sur le parcours.");
                throw new RuntimeException();
            }
        });

        for (int i = 1; i < sorted.size(); i++) {
            StagingPointRequest prev = sorted.get(i - 1);
            StagingPointRequest curr = sorted.get(i);
            if (!curr.getPassingTime().isAfter(prev.getPassingTime())) {
//                throw new InvalidRequestException("L'heure de passage à " + curr.city()
//                        + " doit être strictement postérieure à celle de " + prev.city() + ".");
                throw new RuntimeException();
            }
            if (curr.getPrice().compareTo(prev.getPrice()) <= 0) {
//                throw new InvalidRequestException("Le prix à " + curr.city()
//                        + " doit être strictement supérieur à celui de " + prev.city() + ".");
                throw new RuntimeException();
            }
        }
        return sorted;
    }

}
