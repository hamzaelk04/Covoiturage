package org.covoiturage.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.covoiturage.mapper.StagingPointMapper;
import org.covoiturage.repository.RouteRepository;
import org.covoiturage.repository.StagingPointRepository;
import org.springframework.stereotype.Service;

@Slf4j @Service @RequiredArgsConstructor
public class StagingPointService {
    private final StagingPointRepository stagingPointRepository;
    private final RouteRepository routeRepository;
    private final StagingPointMapper stagingPointMapper;


}
