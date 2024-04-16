package com.gke.pod.health.service.impl;

import com.gke.pod.health.entity.PodHealthResponse;
import com.gke.pod.health.service.PodHealthCountService;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.proto.V1;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
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
    public int getTotalHealthyPodCountUsingServiceName(V1PodList v1PodList,String serviceName) {
        return (int) v1PodList.getItems().stream().filter(pod->pod.getMetadata().getLabels().containsKey("app")&& pod.getMetadata().getLabels().get("app").equalsIgnoreCase(serviceName)).count();
    }

    @Override
    public int getHealthyPodCountUsingServiceName(V1PodList v1PodList,String serviceName) {
        return (int) v1PodList.getItems().stream().filter(pod->pod.getMetadata().getLabels().containsKey("app")&& pod.getMetadata().getLabels().get("app").equalsIgnoreCase(serviceName)
       && pod.getStatus().getPhase().equalsIgnoreCase("Running")).count();
    }







    @Override
    public PodHealthResponse getApplicationHealthStatus(int totalPodCount, int totalHealthyPodCount) {

        PodHealthResponse podHealthResponse=new PodHealthResponse();
        podHealthResponse.setTotalPodCount(totalPodCount);
        podHealthResponse.setTotalHealthyPodCount(totalHealthyPodCount);
        if(totalHealthyPodCount<(0.7*totalPodCount))
        {
            podHealthResponse.setApplicationHealthStatus("Application is not in healthy state");
        }else{
            podHealthResponse.setApplicationHealthStatus("Application is in healthy state");
        }
        return podHealthResponse;
    }

    @Override
    public int countNumberOfRunningServices() {

        KubernetesClient kubernetesClient=new DefaultKubernetesClient();
        ServiceList serviceList=kubernetesClient.services().list();
        log.info("serviceList:::::::::"+ serviceList);
        int totalService=serviceList.getItems().size();
        log.info("serviceCount:::::"+ totalService);
        return (int) serviceList.getItems().stream().filter(service -> service.getStatus().equals("Running")).count();

    }
}
