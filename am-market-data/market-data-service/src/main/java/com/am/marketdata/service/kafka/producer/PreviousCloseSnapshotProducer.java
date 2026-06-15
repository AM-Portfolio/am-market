package com.am.marketdata.service.kafka.producer;

import com.am.marketdata.common.model.events.PreviousCloseSnapshotEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Publishes {@link PreviousCloseSnapshotEvent} messages to the
 * {@code am-previous-close-snapshot} Kafka topic.
 *
 * <p>Only instantiated when Kafka is explicitly enabled
 * ({@code app.kafka.enabled=true}). When disabled (default in local dev),
 * the bean is absent and callers receive an empty Optional — the scheduler
 * continues to function normally without any Kafka dependency.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = false)
@ConditionalOnBean(KafkaTemplate.class)
public class PreviousCloseSnapshotProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.previous-close-snapshot:am-previous-close-snapshot}")
    private String topic;

    /**
     * Fire-and-forget publish of a previous-close snapshot for a single symbol.
     * Uses the symbol as the Kafka message key to ensure ordered delivery per symbol.
     *
     * @param event the snapshot event to publish (must not be null)
     */
    public void publish(PreviousCloseSnapshotEvent event) {
        if (event == null) {
            return;
        }
        try {
            log.info("Publishing previous-close snapshot symbol={} topic={}", event.getId(), topic);
            kafkaTemplate.send(topic, event.getId(), event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.debug("Previous-close snapshot sent symbol={} partition={} offset={}",
                                    event.getId(),
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        } else {
                            log.error("Failed to send previous-close snapshot symbol={}", event.getId(), ex);
                        }
                    });
        } catch (Exception e) {
            log.error("Error publishing previous-close snapshot symbol={}", event.getId(), e);
        }
    }
}
