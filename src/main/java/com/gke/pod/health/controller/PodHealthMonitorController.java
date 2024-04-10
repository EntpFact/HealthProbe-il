package com.gke.pod.health.controller;

import com.gke.pod.health.entity.PodHealthResponse;
import com.gke.pod.health.service.PodHealthCountService;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.util.Config;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/podhealth")
@Slf4j
public class PodHealthMonitorController {

    @Autowired
    PodHealthCountService podHealthCountService;


    @GetMapping(value = "/getHealthyPodCount")
    public ResponseEntity<PodHealthResponse> getCountOfHealthyPods() throws IOException, ApiException {

        ApiClient apiClient= Config.defaultClient();
        CoreV1Api api=new CoreV1Api(apiClient);

        String namespace="default";
        V1PodList podList= api.listNamespacedPod(namespace).execute();

   /*     int totalPodCount= podHealthCountService.getTotalPodCount(podList);
        int healthPodCount= podHealthCountService.getHealthyPodCount(podList);*/

       int totalHealthPodCountUsingServiceName= podHealthCountService.getTotalHealthyPodCountUsingServiceName(podList,"pod-healthcheck-demo-app");

       log.info("totalHealthPodCountUsingServiceName::::::"+totalHealthPodCountUsingServiceName);
        int healthPodCountOnBasisOfService=podHealthCountService.getHealthyPodCountUsingServiceName(podList,"pod-healthcheck-demo-app");

        log.info("healthPodCountOnBasisOfService::::::"+healthPodCountOnBasisOfService);
        PodHealthResponse podHealthResponse=podHealthCountService.getApplicationHealthStatus(totalHealthPodCountUsingServiceName,healthPodCountOnBasisOfService);

        return new ResponseEntity<>(podHealthResponse, HttpStatus.OK);
    }


}
