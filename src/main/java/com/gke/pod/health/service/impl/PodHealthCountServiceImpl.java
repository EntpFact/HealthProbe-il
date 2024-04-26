package com.gke.pod.health.service.impl;

import com.gke.pod.health.constants.HealthCheckConstants;
import com.gke.pod.health.entity.PodHealthResponse;
import com.gke.pod.health.service.PodHealthCountService;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.util.Config;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class PodHealthCountServiceImpl implements PodHealthCountService {


    @Value("${health.namespace.value}")
    private String nameSpaceValue;

    @Value("${health.applicationlist}")
    private String applications;

    @Value("${health.application.criteria}")
    private String criteria;


    @Value("#{${health.servicelist}}")
    private Map<String,String> serviceMap;




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

        if(criteria.equalsIgnoreCase(HealthCheckConstants.PERCENTAGE)){
            if(totalHealthyPodCount<(0.7*totalPodCount)){
                podHealthResponse.setApplicationHealthStatus(HealthCheckConstants.NOT_HEALTHY);
            }else{
                podHealthResponse.setApplicationHealthStatus(HealthCheckConstants.HEALTHY);
            }
        }else {
            if(totalPodCount==totalHealthyPodCount)
            {
                podHealthResponse.setApplicationHealthStatus(HealthCheckConstants.HEALTHY);
            }else{
                podHealthResponse.setApplicationHealthStatus(HealthCheckConstants.NOT_HEALTHY);
            }

        }
        return podHealthResponse;
    }

    @Override
    public void checkHealthStatusOfApplication() {

    }

    @Override
    public V1PodList fetchPodList() throws IOException, ApiException {

        ApiClient apiClient= Config.defaultClient();
        CoreV1Api api=new CoreV1Api(apiClient);
        V1PodList podList= api.listNamespacedPod(nameSpaceValue).execute();
        return podList;
    }

    @Override
    public PodHealthResponse fetchApplicationStatus(V1PodList podList) {
        PodHealthResponse podHealthResponse=null;

        Map<String,String> map=new HashMap<>();
        log.info("applications :::::: " + applications);
        List<String> serviceList = Arrays.stream(applications.split(",")).toList();
        log.info(" services list size :::::: " + serviceList.size());

        for (String serviceName : serviceList) {

            int totalHealthPodCountUsingServiceName = getTotalHealthyPodCountUsingServiceName(podList, serviceName);

            log.info("totalHealthPodCountUsingServiceName::::::" + totalHealthPodCountUsingServiceName + "serviceName:::: " + serviceName);
            int healthPodCountOnBasisOfService = getHealthyPodCountUsingServiceName(podList, serviceName);
            log.info("healthPodCountOnBasisOfService::::::" + healthPodCountOnBasisOfService + "serviceName:::: " + serviceName);
            podHealthResponse = getApplicationHealthStatus(totalHealthPodCountUsingServiceName, healthPodCountOnBasisOfService);
            log.info("healthPodCountOnBasisOfService::::::" + healthPodCountOnBasisOfService);

            if(serviceMap.get(serviceName).equalsIgnoreCase(HealthCheckConstants.SERVICE_FLAG)){
                map.put(serviceName,podHealthResponse.getApplicationHealthStatus());
            }
        }
        podHealthResponse.setServiceHealthChecks(map);
        
        String status=checkApplicationStatusBasedOnServiceFlag(map);
        podHealthResponse.setApplicationHealthStatus(status);
        
        return podHealthResponse;
    }

    private String checkApplicationStatusBasedOnServiceFlag(Map<String, String> map) {

        if(map.containsValue(HealthCheckConstants.NOT_HEALTHY)){
            return HealthCheckConstants.NOT_HEALTHY;
        }
        return HealthCheckConstants.HEALTHY;
    }


}
