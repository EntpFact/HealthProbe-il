package com.gke.pod.health.service.impl;

import com.gke.pod.health.config.KafkaConfig;
import com.gke.pod.health.constants.HealthCheckConstants;
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
import org.springframework.boot.actuate.health.Health;
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

    @Autowired
    KafkaConfig kafkaConfig;




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

        if((criteria != null || criteria != "") && criteria.equalsIgnoreCase(HealthCheckConstants.PERCENTAGE)){
            log.info("in statusCheckForPecentage");
            return statusCheckForPecentage(totalPodCount, totalHealthyPodCount, podHealthResponse);
        } else {
            log.info("in statusCheckForOther");
            return statusCheckForOther(totalPodCount, totalHealthyPodCount, podHealthResponse);
        }

    }

    private static PodHealthResponse statusCheckForOther(int totalPodCount, int totalHealthyPodCount, PodHealthResponse podHealthResponse) {
        log.info("totalPodCount==totalHealthyPodCount");
        return (totalPodCount==totalHealthyPodCount)?checkHealthy(podHealthResponse):checkNotHealthy(podHealthResponse);

    }

    private static PodHealthResponse statusCheckForPecentage(int totalPodCount, int totalHealthyPodCount, PodHealthResponse podHealthResponse) {
        log.info("totalHealthyPodCount < (0.7 * totalPodCount");
        return (totalHealthyPodCount < (0.7 * totalPodCount))?checkNotHealthy(podHealthResponse):checkHealthy(podHealthResponse);
    }



    private static  PodHealthResponse checkHealthy(PodHealthResponse podHealthResponse){
        log.info("in checkHealthy");
       podHealthResponse.setApplicationHealthStatus(HealthCheckConstants.HEALTHY);
       return podHealthResponse;
    }

    private static  PodHealthResponse checkNotHealthy(PodHealthResponse podHealthResponse){
        log.info("in checkNotHealthy");
        podHealthResponse.setApplicationHealthStatus(HealthCheckConstants.NOT_HEALTHY);
        return podHealthResponse;
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

        log.info("with latest code 9 may");

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

            if (serviceMap.get(serviceName).equalsIgnoreCase(HealthCheckConstants.SERVICE_FLAG)) {
                map.put(serviceName, podHealthResponse.getApplicationHealthStatus());
            }
        }
        String status=checkApplicationStatusBasedOnServiceFlag(map);
        podHealthResponse.setApplicationHealthStatus(status);

        return podHealthResponse;

    }

    @Override
    public Health getKafkaHealth() {
        return kafkaConfig.kafkaHealthIndicator().health();
    }

    @Override
    public Map<String, Object> fetchOverAllStatus(PodHealthResponse podHealthResponse, Health health) {

        Map<String,Map<String,String>> kafkaOverAllStatus=new HashMap<>();
        Map<String,String> kafkaStatus=new HashMap<>();
        kafkaStatus.put("Status",health.getStatus().toString());
        kafkaOverAllStatus.put("Kafka",kafkaStatus);
        Map<String,Map<String,String>> kubenetesOverAllStatus=new HashMap<>();
        Map<String,String> kubernetesStatus=new HashMap<>();
        kubernetesStatus.put("Status",health.getStatus().toString());
        kubenetesOverAllStatus.put("Kubernetes",kubernetesStatus);
        Map<String,Object> finalOutput=new HashMap<>();
        finalOutput.put("Kafka",kafkaOverAllStatus);
        finalOutput.put("Kubernetes",kubenetesOverAllStatus);


        if(kafkaStatus.containsValue(HealthCheckConstants.NOT_HEALTHY)||kubernetesStatus.containsValue(HealthCheckConstants.NOT_HEALTHY)){
            finalOutput.put("Status",HealthCheckConstants.HEALTHY);
        }else{
            finalOutput.put("Status",HealthCheckConstants.NOT_HEALTHY);
        }
        return finalOutput;
    }
    private String checkApplicationStatusBasedOnServiceFlag(Map<String, String> map) {
        if(map!=null && map.containsValue(HealthCheckConstants.NOT_HEALTHY)){
                return HealthCheckConstants.NOT_HEALTHY;
            }
            return HealthCheckConstants.HEALTHY;

        }






}
