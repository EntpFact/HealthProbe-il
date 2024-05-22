package com.gke.pod.health.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class PodHealthResponse {


    private int totalPodCount;

    private int totalHealthyPodCount;

    private String applicationHealthStatus;


}
