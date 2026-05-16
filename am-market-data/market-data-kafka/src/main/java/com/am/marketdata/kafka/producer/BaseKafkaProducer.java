package com.am.marketdata.kafka.producer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import com.am.observability.mdc.MdcKeys;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
public class BaseKafkaProducer<T> {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public BaseKafkaProducer(
            @org.springframework.beans.factory.annotation.Autowired(required = false) KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Send an event to a Kafka topic
     * 
     * @param topic The topic to send the event to
     * @param event The event to send
     */
    public void send(String topic, T event) {
        if (event == null) {
            log.error("Cannot send null event to Kafka");
            throw new IllegalArgumentException("Event cannot be null");
        }

        if (kafkaTemplate == null) {
            log.warn("Kafka is disabled. Message not sent to topic: {}", topic);
            return;
        }

        try {
            log.debug("Sending event to Kafka topic: {}", topic);
            
            ProducerRecord<String, Object> record = new ProducerRecord<>(topic, event);
            String correlationId = MDC.get(MdcKeys.CORRELATION_ID);
            if (correlationId != null) {
                record.headers().add(MdcKeys.CORRELATION_ID, correlationId.getBytes());
            }

            kafkaTemplate.send(record)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.debug("Message sent successfully topic={} partition={} offset={}",
                                    result.getRecordMetadata().topic(),
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        } else {
                            log.error("Failed to send message topic={}", topic, ex);
                        }
                    });
        } catch (Exception e) {
            log.error("Failed to send event topic={}", topic, e);
            throw new RuntimeException("Failed to send event to Kafka", e);
        }
    }

    /**
     * Send an event with additional metadata to a Kafka topic
     * 
     * @param event     The event to send
     * @param topic     The topic to send to
     * @param eventType The type of event
     * @param timestamp The timestamp of the event
     */
    public void sendEvent(T event, String topic, String eventType, LocalDateTime timestamp) {
        if (kafkaTemplate == null) {
            log.warn("Kafka is disabled. Message not sent to topic: {}", topic);
            return;
        }

        try {
            log.info("Sending event to Kafka. EventType: {}, Timestamp: {}", eventType, timestamp);

            RecordHeaders headers = new RecordHeaders();
            headers.add("eventType", eventType.getBytes());
            headers.add("timestamp", String.valueOf(timestamp).getBytes());

            long timestampMillis = timestamp
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();

            ProducerRecord<String, Object> record = new ProducerRecord<>(topic, null, timestampMillis, eventType, event,
                    headers);
            
            String correlationId = MDC.get(MdcKeys.CORRELATION_ID);
            if (correlationId != null) {
                record.headers().add(MdcKeys.CORRELATION_ID, correlationId.getBytes());
            }

            kafkaTemplate.send(record)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Message sent successfully topic={} partition={} offset={}",
                                    result.getRecordMetadata().topic(),
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        } else {
                            log.error("Failed to send message topic={}", topic, ex);
                        }
                    });
        } catch (Exception e) {
            log.error("Failed to send event to Kafka", e);
            throw new RuntimeException("Failed to send event to Kafka", e);
        }
    }
}
