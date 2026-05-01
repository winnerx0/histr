package com.histr.api.service;

import com.histr.api.dto.WorkerStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WorkerStatusService {

    private final StringRedisTemplate redis;

    @Value("${app.worker-heartbeat-key:worker:heartbeat}")
    private String heartbeatKey;

    @Value("${app.worker-processed-count-key:worker:processed_count}")
    private String processedCountKey;

    public WorkerStatusResponse getStatus() {
        Long queueDepth = redis.opsForList().size("transactions");
        String heartbeat = redis.opsForValue().get(heartbeatKey);
        String processedRaw = redis.opsForValue().get(processedCountKey);

        return new WorkerStatusResponse(
                queueDepth != null ? queueDepth : 0L,
                heartbeat,
                processedRaw != null ? Long.parseLong(processedRaw) : 0L
        );
    }
}
