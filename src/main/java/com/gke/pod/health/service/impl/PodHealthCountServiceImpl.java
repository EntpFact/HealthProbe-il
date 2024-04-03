package com.gke.pod.health.service.impl;

import com.gke.pod.health.entity.PodHealthResponse;
import com.gke.pod.health.service.PodHealthCountService;
import io.kubernetes.client.openapi.models.V1PodList;
import lombok.extern.java.Log;
import org.springframework.stereotype.Service;

@Service
public class PodHealthCountServiceImpl implements PodHealthCountService {


    @Override
    public int getTotalPodCount(V1PodList v1PodList) {
        return (int) v1PodList.getItems().stream().count();
    }

    @Override
    public int getHealthyPodCount(V1PodList v1PodList) {
        return (int) v1PodList.getItems().stream().filter(pod->"Running".equalsIgnoreCase(pod.getStatus().getPhase())).count();
    }

    @Override
    public PodHealthResponse getApplicationHealthStatus(int totalPodCount, int totalHealthyPodCount) {

        PodHealthResponse podHealthResponse=new PodHealthResponse();
        podHealthResponse.setTotalPodCount(totalPodCount);
        podHealthResponse.setTotalHealthyPodCount(totalHealthyPodCount);
        if(totalHealthyPodCount<=3)
        {
            podHealthResponse.setApplicationHealthStatus("Application is not in healthy state");
        }else{
            podHealthResponse.setApplicationHealthStatus("Application is in healthy state");
        }
        return podHealthResponse;
    }
}
