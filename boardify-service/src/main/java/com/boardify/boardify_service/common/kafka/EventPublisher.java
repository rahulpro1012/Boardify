package com.boardify.boardify_service.common.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class EventPublisher {
    private final KafkaTemplate<String, Object> template;
    public EventPublisher(KafkaTemplate<String, Object> template) { this.template = template; }

    public void publish(String topic, String key, Object event) {
        template.send(topic, key, event);
    }
}
