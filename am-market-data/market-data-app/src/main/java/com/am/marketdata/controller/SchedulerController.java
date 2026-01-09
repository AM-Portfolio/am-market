package com.am.marketdata.controller;

import com.am.marketdata.scheduler.service.AbstractMarketDataScheduler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/v1/scheduler")
@RequiredArgsConstructor
@Tag(name = "Scheduler Control", description = "APIs to manage and trigger backend schedulers manually")
public class SchedulerController {

    // Inject all beans that extend AbstractMarketDataScheduler
    private final List<AbstractMarketDataScheduler<?>> schedulers;

    @GetMapping("/jobs")
    @Operation(summary = "List all available schedulers")
    public ResponseEntity<List<String>> listSchedulers() {
        List<String> names = schedulers.stream()
                .map(AbstractMarketDataScheduler::getSchedulerName)
                .collect(Collectors.toList());
        return ResponseEntity.ok(names);
    }

    @PostMapping("/trigger/{jobName}")
    @Operation(summary = "Manually trigger a specific scheduler")
    public ResponseEntity<Map<String, String>> triggerScheduler(@PathVariable String jobName) {
        log.info("Received manual trigger request for scheduler: {}", jobName);

        Optional<AbstractMarketDataScheduler<?>> schedulerOpt = schedulers.stream()
                .filter(s -> s.getSchedulerName().equalsIgnoreCase(jobName))
                .findFirst();

        if (schedulerOpt.isPresent()) {
            new Thread(() -> {
                try {
                    log.info("Manually executing scheduler: {}", jobName);
                    schedulerOpt.get().scheduleProcessing();
                } catch (Exception e) {
                    log.error("Error in manual execution of {}", jobName, e);
                }
            }).start();

            Map<String, String> response = new HashMap<>();
            response.put("message", "Scheduler '" + jobName + "' triggered successfully in background.");
            response.put("status", "STARTED");
            return ResponseEntity.ok(response);
        } else {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Scheduler not found");
            error.put("jobName", jobName);
            return ResponseEntity.badRequest().body(error);
        }
    }
}
