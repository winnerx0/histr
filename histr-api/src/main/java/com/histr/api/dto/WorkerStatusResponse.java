package com.histr.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WorkerStatusResponse {
    private long queueDepth;
    private String workerHeartbeat;
    private long processedCount;
}
