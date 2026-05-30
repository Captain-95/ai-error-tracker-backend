package com.errortracker.queue;

import com.errortracker.dto.kafka.ErrorEventMessage;
import com.errortracker.service.ErrorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class QueueProcessor {

    private final RedisMessageQueue queue;
    private final ErrorService errorService;

    // Process queue every 2 seconds
    @Scheduled(fixedDelay = 2000)
    public void processQueue() {
        int processed = 0;

        // Process up to 10 errors per cycle
        while (processed < 10) {
            ErrorEventMessage message = queue.pop();
            if (message == null) break;

            try {
                errorService.processIncomingError(message);
                processed++;
            } catch (Exception e) {
                log.error("Failed to process error: {}", e.getMessage());
            }
        }

        if (processed > 0) {
            log.info("Processed {} errors from queue", processed);
        }
    }
}