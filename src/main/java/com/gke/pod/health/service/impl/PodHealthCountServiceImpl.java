package com.gke.pod.health.service.impl;


//import com.gke.pod.health.config.KafkaConfig;

import com.gke.pod.health.config.KafkaConfigNew;
import com.gke.pod.health.constants.HealthCheckConstants;
import com.gke.pod.health.entity.PodHealthResponse;
import com.gke.pod.health.service.PodHealthCountService;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ContainerStatus;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.util.Config;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class PodHealthCountServiceImpl implements PodHealthCountService {


    private final AdminClient adminClient;

    private final DataSource dataSource;

    private final KafkaConfigNew kafkaConfigNew;


    @Value("${health.namespace.value}")
    private String nameSpaceValue;

    @Value("${health.applicationlist}")
    private String applications;

    @Value("${health.application.criteria}")
    private String criteria;


    @Value("#{${health.servicelist}}")
    private Map<String,String> serviceMap;

    public PodHealthCountServiceImpl(KafkaConfigNew kafkaConfigNew, AdminClient adminClient, DataSource dataSource, KafkaConfigNew kafkaConfigNew1) {
        this.adminClient = adminClient;

        this.dataSource = dataSource;
        this.kafkaConfigNew = kafkaConfigNew1;
    }


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

        int counter=0;
        for (V1Pod pod1 : v1PodList.getItems()) {

            log.info("pod name::::::" + pod1.getMetadata().getName());
            for (V1ContainerStatus v1ContainerStatus : pod1.getStatus().getContainerStatuses()) {
                log.info("containerName:::" + v1ContainerStatus.getName());
                log.info("containerstatus::::" + v1ContainerStatus.getState());
                if (v1ContainerStatus.getState().getRunning()!=null) {

                    log.info("container with running status found::::::"+v1ContainerStatus.getName());
                    counter++;
                }
            }
        }
        log.info("count of running containers::::"+counter);
       if(counter>0) {
           return (int) v1PodList.getItems().stream().filter(pod -> pod.getMetadata().getLabels().containsKey("app") && pod.getMetadata().getLabels().get("app").equalsIgnoreCase(serviceName)
                   && pod.getStatus().getPhase().equalsIgnoreCase("Running")).count();
       }else{
           return 0;
       }

    }
    @Override
    public PodHealthResponse getApplicationHealthStatus(int totalPodCount, int totalHealthyPodCount) {


        PodHealthResponse podHealthResponse=new PodHealthResponse();
        podHealthResponse.setTotalPodCount(totalPodCount);
        podHealthResponse.setTotalHealthyPodCount(totalHealthyPodCount);

        if((criteria != null || criteria != "") && criteria.equalsIgnoreCase(HealthCheckConstants.PERCENTAGE)){
            return statusCheckForPecentage(totalPodCount, totalHealthyPodCount, podHealthResponse);
        } else {
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

       podHealthResponse.setApplicationHealthStatus(HealthCheckConstants.HEALTHY);
       return podHealthResponse;
    }

    private static  PodHealthResponse checkNotHealthy(PodHealthResponse podHealthResponse){

        podHealthResponse.setApplicationHealthStatus(HealthCheckConstants.NOT_HEALTHY);
        return podHealthResponse;
    }




    @Override
    public V1PodList fetchPodList() throws IOException, ApiException {

        ApiClient apiClient= Config.defaultClient();
        CoreV1Api api=new CoreV1Api(apiClient);
        V1PodList podList= api.listNamespacedPod(nameSpaceValue).execute();
        log.info("podList:::::"+podList);
        return podList;
    }



    @Override
    public PodHealthResponse fetchApplicationStatus(V1PodList podList) {
        PodHealthResponse podHealthResponse=null;
        Map<String,String> map=new HashMap<>();
        List<String> serviceList = Arrays.stream(applications.split(",")).toList();


        for (String serviceName : serviceList) {

            int totalHealthPodCountUsingServiceName = getTotalHealthyPodCountUsingServiceName(podList, serviceName);




            log.info("totalHealthPodCountUsingServiceName::::::" + totalHealthPodCountUsingServiceName + "serviceName:::: " + serviceName);
            int healthPodCountOnBasisOfService = getHealthyPodCountUsingServiceName(podList, serviceName);
            log.info("healthyPodCountOnBasisOfService::::::" + healthPodCountOnBasisOfService + "serviceName:::: " + serviceName);
            podHealthResponse = getApplicationHealthStatus(totalHealthPodCountUsingServiceName, healthPodCountOnBasisOfService);


            if (serviceMap.get(serviceName).equalsIgnoreCase(HealthCheckConstants.SERVICE_FLAG)) {
                map.put(serviceName, podHealthResponse.getApplicationHealthStatus());
            }
        }
        String status=checkApplicationStatusBasedOnServiceFlag(map);
        podHealthResponse.setApplicationHealthStatus(status);

        return podHealthResponse;

    }
    @Override
    public Map<String, Object> fetchOverAllStatus(PodHealthResponse podHealthResponse, String health, String yugabyteDBStatus) {


        Map<String,String> kafkaStatus=new HashMap<>();
        kafkaStatus.put(HealthCheckConstants.STATUS,health);
        Map<String,String> kubernetesStatus=new HashMap<>();
        kubernetesStatus.put(HealthCheckConstants.STATUS,podHealthResponse.getApplicationHealthStatus());
        Map<String,String> yugabyeStatus=new HashMap<>();
        yugabyeStatus.put(HealthCheckConstants.STATUS,  yugabyteDBStatus);
        Map<String,Object> finalOutput=new HashMap<>();
        finalOutput.put(HealthCheckConstants.KAFKA,kafkaStatus);
        finalOutput.put(HealthCheckConstants.KUBERNETES,kubernetesStatus);
        finalOutput.put(HealthCheckConstants.YUGABYTE,yugabyeStatus);

        if(kafkaStatus.containsValue(HealthCheckConstants.NOT_HEALTHY) || kubernetesStatus.containsValue(HealthCheckConstants.NOT_HEALTHY) || yugabyeStatus.containsValue(HealthCheckConstants.NOT_HEALTHY)){
            finalOutput.put(HealthCheckConstants.STATUS,HealthCheckConstants.NOT_HEALTHY);
        }else{
            finalOutput.put(HealthCheckConstants.STATUS,HealthCheckConstants.HEALTHY);
        }
        return finalOutput;
    }

    @Override
    public String fetchYugabyteDBStatus() {

        try  {
            if (dataSource.getConnection().isValid(HealthCheckConstants.DATASOURCE_TIMEOUT)) {
                return HealthCheckConstants.HEALTHY;
            } else {

                return HealthCheckConstants.NOT_HEALTHY;
            }

        } catch (Exception e) {
            return HealthCheckConstants.NOT_HEALTHY;

        }

        }


       @Override
       public String getKafkaStatus() {

        try{
            DescribeClusterResult describeClusterRequest=adminClient.describeCluster();
            describeClusterRequest.nodes().get(HealthCheckConstants.TIMEOUT,TimeUnit.MILLISECONDS);
            return HealthCheckConstants.HEALTHY;
        }catch (Exception e){
            e.printStackTrace();
            return HealthCheckConstants.NOT_HEALTHY;
        }

    }

    private String checkApplicationStatusBasedOnServiceFlag(Map<String, String> map) {
        log.info("service Map::::::"+map);
        if(map!=null && map.containsValue(HealthCheckConstants.NOT_HEALTHY)){
                return HealthCheckConstants.NOT_HEALTHY;
            }
            return HealthCheckConstants.HEALTHY;
    }
}
