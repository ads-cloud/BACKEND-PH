package com.ph.backend.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "ph_asamblea_exchange";
    public static final String QUEUE_QUORUM = "ph_quorum_queue";
    public static final String QUEUE_VOTO = "ph_voto_queue";
    public static final String QUEUE_EVENTS = "ph_events_queue";

    public static final String ROUTING_KEY_QUORUM = "asamblea.quorum.actualizado";
    public static final String ROUTING_KEY_VOTO = "asamblea.voto.registrado";
    public static final String ROUTING_KEY_EVENTS_PATTERN = "ph.event.#";

    @Bean
    public TopicExchange asambleaTopicExchange() {
        return new TopicExchange(EXCHANGE_NAME, true, false);
    }

    @Bean
    public Queue quorumQueue() {
        return QueueBuilder.durable(QUEUE_QUORUM).build();
    }

    @Bean
    public Queue votoQueue() {
        return QueueBuilder.durable(QUEUE_VOTO).build();
    }

    @Bean
    public Queue eventsQueue() {
        return QueueBuilder.durable(QUEUE_EVENTS).build();
    }

    @Bean
    public Binding bindingQuorum(Queue quorumQueue, TopicExchange asambleaTopicExchange) {
        return BindingBuilder.bind(quorumQueue).to(asambleaTopicExchange).with(ROUTING_KEY_QUORUM);
    }

    @Bean
    public Binding bindingVoto(Queue votoQueue, TopicExchange asambleaTopicExchange) {
        return BindingBuilder.bind(votoQueue).to(asambleaTopicExchange).with(ROUTING_KEY_VOTO);
    }

    @Bean
    public Binding bindingEvents(Queue eventsQueue, TopicExchange asambleaTopicExchange) {
        return BindingBuilder.bind(eventsQueue).to(asambleaTopicExchange).with(ROUTING_KEY_EVENTS_PATTERN);
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}
