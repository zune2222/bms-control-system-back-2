package com.bms.config;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

@Configuration
public class MqttConfig {

    @Value("${mqtt.broker.host}")
    private String brokerHost;

    @Value("${mqtt.broker.port}")
    private int brokerPort;

    @Value("${mqtt.broker.username}")
    private String username;

    @Value("${mqtt.broker.password}")
    private String password;

    @Value("${mqtt.topics.bms-status}")
    private String bmsStatusTopic;

    @Value("${mqtt.topics.bms-control}")
    private String bmsControlTopic;

    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions options = new MqttConnectOptions();
        
        options.setServerURIs(new String[] { "tcp://" + brokerHost + ":" + brokerPort });
        options.setCleanSession(true);
        options.setConnectionTimeout(30);
        options.setKeepAliveInterval(60);
        
        if (username != null && !username.isEmpty()) {
            options.setUserName(username);
            options.setPassword(password.toCharArray());
        }
        
        factory.setConnectionOptions(options);
        return factory;
    }

    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel mqttOutboundChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageProducer inbound() {
        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(
                        "bms-server-" + System.currentTimeMillis(),
                        mqttClientFactory(),
                        bmsStatusTopic, bmsControlTopic);

        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(1);
        adapter.setOutputChannel(mqttInputChannel());
        return adapter;
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttOutboundChannel")
    public MessageHandler outbound() {
        MqttPahoMessageHandler messageHandler =
                new MqttPahoMessageHandler("bms-server-out-" + System.currentTimeMillis(), mqttClientFactory());
        messageHandler.setAsync(true);
        messageHandler.setDefaultTopic(bmsControlTopic);
        return messageHandler;
    }
}
