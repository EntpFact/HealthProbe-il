package com.gke.pod.health.service.impl;

import com.gke.pod.health.entity.PodHealthResponse;
import com.gke.pod.health.service.PodHealthCountService;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Condition;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1ServiceList;
import io.kubernetes.client.proto.V1;
import io.kubernetes.client.util.Config;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

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
    public int countNumberOfRunningServices() throws IOException, ApiException {

        ApiClient client= Config.defaultClient();
        Configuration.setDefaultApiClient(client);

        CoreV1Api api=new CoreV1Api();
        int runningServiceCount=0;
        V1ServiceList serviceList=api.listServiceForAllNamespaces().execute();
        log.info("serviceList::::" +serviceList);
        for(V1Service service:serviceList.getItems()){
            if(service.getStatus()!=null && service.getStatus().getConditions()!=null);
            {
                for(V1Condition condition:service.getStatus().getConditions()){
                    if("True".equalsIgnoreCase(condition.getStatus()) && "Ready".equalsIgnoreCase(condition.getType())){
                        runningServiceCount++;
                        break;
                    }

            }

            }
        }
        log.info("runningServiceCount:::::"+runningServiceCount);

        return runningServiceCount;
    }
}
