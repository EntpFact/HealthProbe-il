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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/podhealth")
@Slf4j
public class PodHealthMonitorController {

    @Autowired
    PodHealthCountService podHealthCountService;


    @Value("${health.applicationlist}")
    private String applications;


    @GetMapping(value = "/getHealthyPodCount")
    public ResponseEntity<PodHealthResponse> getCountOfHealthyPods() throws IOException, ApiException {

        PodHealthResponse podHealthResponse=null;
        ApiClient apiClient= Config.defaultClient();
        CoreV1Api api=new CoreV1Api(apiClient);

        String namespace="default";
        V1PodList podList= api.listNamespacedPod(namespace).execute();

        log.info("applications :::::: " + applications);
        List<String> serviceList= Arrays.stream(applications.split(",")).toList();
        log.info(" services list size :::::: " +serviceList.size());

        for (String serviceName:serviceList) {

           int totalHealthPodCountUsingServiceName = podHealthCountService.getTotalHealthyPodCountUsingServiceName(podList,serviceName);

           log.info("totalHealthPodCountUsingServiceName::::::" + totalHealthPodCountUsingServiceName + "serviceName:::: "+serviceName);
           int healthPodCountOnBasisOfService = podHealthCountService.getHealthyPodCountUsingServiceName(podList, serviceName);
           log.info("healthPodCountOnBasisOfService::::::" + healthPodCountOnBasisOfService + "serviceName:::: "+serviceName);
           podHealthResponse = podHealthCountService.getApplicationHealthStatus(totalHealthPodCountUsingServiceName, healthPodCountOnBasisOfService);
           log.info("healthPodCountOnBasisOfService::::::" + healthPodCountOnBasisOfService);
       }
       return new ResponseEntity<>(podHealthResponse, HttpStatus.OK);
    }


}
