package com.worklog.infrastructure.eventstore;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.worklog.domain.DomainEvent;
import com.worklog.domain.workday.events.TimeBlockEnded;
import com.worklog.domain.workday.events.TimeBlockStarted;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class EventSerializer {

    private static final Map<String, Class<? extends DomainEvent>> EVENT_TYPES = Map.of(
            "TimeBlockStarted", TimeBlockStarted.class,
            "TimeBlockEnded", TimeBlockEnded.class
    );

    private final ObjectMapper objectMapper;

    public EventSerializer(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String serialize(final DomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (final JsonProcessingException e) {
            throw new EventSerializationException("Failed to serialize event " + event.getClass().getSimpleName(), e);
        }
    }

    public DomainEvent deserialize(final String type, final String payload) {
        final Class<? extends DomainEvent> eventClass = EVENT_TYPES.get(type);
        if (eventClass == null) {
            throw new EventSerializationException("Unknown event type: " + type);
        }
        try {
            return objectMapper.readValue(payload, eventClass);
        } catch (final JsonProcessingException e) {
            throw new EventSerializationException("Failed to deserialize event of type " + type, e);
        }
    }

    public String typeOf(final DomainEvent event) {
        return event.getClass().getSimpleName();
    }

    public static class EventSerializationException extends RuntimeException {
        public EventSerializationException(final String message) { super(message); }
        public EventSerializationException(final String message, final Throwable cause) { super(message, cause); }
    }
}
