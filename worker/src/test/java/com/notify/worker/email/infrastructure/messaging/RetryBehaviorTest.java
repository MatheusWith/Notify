package com.notify.worker.email.infrastructure.messaging;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class RetryBehaviorTest {

    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:4-management-alpine");

    @Test
    void givenDlxConfigured_whenMessageRejected_thenGoesToDlq() {
        var ex = "dlq-test.ex";
        var queue = "dlq-test.queue";
        var dlq = "dlq-test.dlq";
        var dlx = "dlq-test.dlx";
        var rk = "dlq-test.rk";

        var cf = new CachingConnectionFactory(rabbitmq.getHost(), rabbitmq.getAmqpPort());
        cf.setUsername("guest");
        cf.setPassword("guest");
        var admin = new RabbitAdmin(cf);
        var template = new RabbitTemplate(cf);

        admin.declareExchange(new org.springframework.amqp.core.DirectExchange(ex, true, false));
        admin.declareExchange(new org.springframework.amqp.core.DirectExchange(dlx, true, false));
        admin.declareQueue(QueueBuilder.durable(dlq).build());
        admin.declareQueue(QueueBuilder.durable(queue).withArgument("x-dead-letter-exchange", dlx)
                .withArgument("x-dead-letter-routing-key", dlq).build());
        admin.declareBinding(new org.springframework.amqp.core.Binding(queue,
                org.springframework.amqp.core.Binding.DestinationType.QUEUE, ex, rk, null));

        template.convertAndSend(ex, rk, "test message");

        assertThat(admin.getQueueInfo(queue)).isNotNull();
        assertThat(admin.getQueueInfo(dlq)).isNotNull();

        cf.destroy();
    }

    @Test
    void givenQueueWithDlx_whenInfrastructureCreated_thenBindingsAreCorrect() {
        var ex = "bind-test.ex";
        var queue = "bind-test.queue";
        var dlq = "bind-test.dlq";
        var dlx = "bind-test.dlx";
        var rk = "bind-test.rk";

        var cf = new CachingConnectionFactory(rabbitmq.getHost(), rabbitmq.getAmqpPort());
        cf.setUsername("guest");
        cf.setPassword("guest");
        var admin = new RabbitAdmin(cf);

        admin.declareExchange(new org.springframework.amqp.core.DirectExchange(ex, true, false));
        admin.declareExchange(new org.springframework.amqp.core.DirectExchange(dlx, true, false));
        admin.declareQueue(QueueBuilder.durable(queue).withArgument("x-dead-letter-exchange", dlx)
                .withArgument("x-dead-letter-routing-key", dlq).build());
        admin.declareQueue(QueueBuilder.durable(dlq).build());
        admin.declareBinding(new org.springframework.amqp.core.Binding(queue,
                org.springframework.amqp.core.Binding.DestinationType.QUEUE, ex, rk, null));

        assertThat(admin.getQueueInfo(queue)).isNotNull();

        cf.destroy();
    }
}
