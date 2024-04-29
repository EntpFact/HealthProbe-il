package com.gke.pod.health.controller;

import com.gke.pod.health.constants.HealthCheckConstants;
import com.gke.pod.health.entity.PodHealthResponse;
import com.gke.pod.health.service.PodHealthCountService;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1ServiceList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/podhealth")
@Slf4j
public class PodHealthMonitorController {

    @Autowired
    private PodHealthCountService podHealthCountService;

    private PodHealthResponse podHealthResponse = null;


    @GetMapping(value = "/getHealthOfApplication")
    public ResponseEntity<?> getApplicationHealthStatus() {

        Map<String,String> applicationStatus=null;

        try {
            V1PodList podList = podHealthCountService.fetchPodList();
            podHealthResponse = podHealthCountService.fetchApplicationStatus(podList);
            applicationStatus=new HashMap<String,String>();
            applicationStatus.put(HealthCheckConstants.APPLICATION_STATUS,podHealthResponse.getApplicationHealthStatus());


        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(e.getMessage(),HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(applicationStatus, HttpStatus.OK);
    }
}
