package com.gke.pod.health.service;

import com.gke.pod.health.entity.PodHealthResponse;
import io.kubernetes.client.openapi.models.V1PodList;
import org.springframework.stereotype.Service;


public interface PodHealthCountService {

    public int getTotalPodCount(V1PodList v1PodList);

    public int getHealthyPodCount(V1PodList v1PodList);

    public PodHealthResponse getApplicationHealthStatus(int totalPodCount, int totalHealthyPodCount);


}
