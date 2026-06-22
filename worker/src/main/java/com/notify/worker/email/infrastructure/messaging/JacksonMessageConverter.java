package com.notify.worker.email.infrastructure.messaging;

import java.lang.reflect.Type;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.SmartMessageConverter;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public class JacksonMessageConverter implements SmartMessageConverter {

    private final ObjectMapper objectMapper;

    public JacksonMessageConverter() {
        this(JsonMapper.builder().build());
    }

    public JacksonMessageConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Message toMessage(Object object, MessageProperties messageProperties) throws MessageConversionException {
        try {
            byte[] body = objectMapper.writeValueAsBytes(object);
            messageProperties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            return new Message(body, messageProperties);
        } catch (Exception e) {
            throw new MessageConversionException("Failed to serialize to JSON: " + e.getMessage(), e);
        }
    }

    @Override
    public Object fromMessage(Message message) throws MessageConversionException {
        try {
            MessageProperties properties = message.getMessageProperties();
            Type inferredType = properties.getInferredArgumentType();
            if (inferredType == null) {
                throw new MessageConversionException("No inferred argument type available for deserialization");
            }
            JavaType javaType = objectMapper.constructType(inferredType);
            return objectMapper.readValue(message.getBody(), javaType);
        } catch (MessageConversionException e) {
            throw e;
        } catch (Exception e) {
            throw new MessageConversionException("Failed to deserialize: " + e.getMessage(), e);
        }
    }

    @Override
    public Object fromMessage(Message message, Object typeHint) throws MessageConversionException {
        try {
            JavaType javaType;
            if (typeHint instanceof Class<?> clazz) {
                javaType = objectMapper.constructType(clazz);
            } else if (typeHint instanceof Type type) {
                javaType = objectMapper.constructType(type);
            } else {
                throw new MessageConversionException("Unsupported type hint: " + typeHint);
            }
            return objectMapper.readValue(message.getBody(), javaType);
        } catch (MessageConversionException e) {
            throw e;
        } catch (Exception e) {
            throw new MessageConversionException("Failed to deserialize: " + e.getMessage(), e);
        }
    }
}
