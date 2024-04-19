package com.gke.pod.health.service;

import com.gke.pod.health.entity.PodHealthResponse;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1PodList;
import org.springframework.stereotype.Service;

import java.io.IOException;


public interface PodHealthCountService {

    public int getTotalPodCount(V1PodList v1PodList);

    public int getHealthyPodCount(V1PodList v1PodList);

    int getTotalHealthyPodCountUsingServiceName(V1PodList v1PodList,String serviceName);

    int getHealthyPodCountUsingServiceName(V1PodList v1PodList,String serviceName);

    public PodHealthResponse getApplicationHealthStatus(int totalPodCount, int totalHealthyPodCount);




}
