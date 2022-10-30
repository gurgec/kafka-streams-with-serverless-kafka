package com.upstash;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Branched;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.UUID;

import static com.upstash.NotificationType.EMAIL;
import static com.upstash.NotificationType.PUSH_NOTIFICATION;
import static com.upstash.NotificationType.SMS;

public class NotificationApp {

    private static final Logger logger
            = LoggerFactory.getLogger(NotificationApp.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        logger.info("Starting main by sending notifications");
        NotificationProducer notificationProducer = new NotificationProducer();

        NotificationDTO pushNotificationDTO = new NotificationDTO(UUID.randomUUID(), PUSH_NOTIFICATION, "This is a push notification", "device_id");
        notificationProducer.sendNotification(pushNotificationDTO, loadConfigFromFiles());
        logger.info("Push Notification sent: {}", pushNotificationDTO);

        NotificationDTO smsNotificationDTO = new NotificationDTO(UUID.randomUUID(), SMS, "This is a sms notification", "phone_number");
        notificationProducer.sendNotification(smsNotificationDTO, loadConfigFromFiles());
        logger.info("SMS Notification sent: {}", smsNotificationDTO);

        NotificationDTO emailNotificationDTO = new NotificationDTO(UUID.randomUUID(), EMAIL, "This is a email notification", "email_address");
        notificationProducer.sendNotification(emailNotificationDTO, loadConfigFromFiles());
        logger.info("Email Notification sent: {}", smsNotificationDTO);

        logger.info("Starting Kafka Streams for consuming notification topic");
        startKafkaStreams(loadConfigFromFiles());
    }


    private static Properties loadConfigFromFiles() throws IOException {
        String configFile = "application.properties";
        final Properties cfg = new Properties();
        try (InputStream inputStream = ClassLoader.getSystemClassLoader()
                .getResourceAsStream(configFile)) {
            cfg.load(inputStream);
        }
        return cfg;
    }

    public static void startKafkaStreams(Properties propertiesFromFile) {
        try {
            Properties kafkaStreamsProperties = getKafkaStreamsProperties(propertiesFromFile);

            String notificationTopicName = propertiesFromFile.get("kafka.notification.topic").toString();
            String pushNotificationTopicName = propertiesFromFile.get("kafka.pushnotification.topic").toString();
            String smsTopicName = propertiesFromFile.get("kafka.sms.topic").toString();
            String emailTopicName = propertiesFromFile.get("kafka.email.topic").toString();


            final StreamsBuilder builder = new StreamsBuilder();
            final KStream<String, String> notificationRecord = builder.stream(notificationTopicName, Consumed.with(Serdes.String(), Serdes.String()));

            notificationRecord.split().branch(
                            (id, notification) -> {
                                try {
                                    NotificationDTO notificationDTO = objectMapper.readValue(notification, NotificationDTO.class);
                                    return PUSH_NOTIFICATION.equals(notificationDTO.getNotificationType());

                                } catch (JsonProcessingException e) {
                                    throw new RuntimeException(e);
                                }
                            }, Branched.withConsumer(ks -> ks.to(pushNotificationTopicName)))
                    .branch((id, notification) -> {
                        try {
                            NotificationDTO notificationDTO = objectMapper.readValue(notification, NotificationDTO.class);
                            return NotificationType.SMS.equals(notificationDTO.getNotificationType());

                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                    }, Branched.withConsumer(ks -> ks.to(smsTopicName)))
                    .branch((id, notification) -> {
                        try {
                            NotificationDTO notificationDTO = objectMapper.readValue(notification, NotificationDTO.class);
                            return NotificationType.EMAIL.equals(notificationDTO.getNotificationType());

                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                    }, Branched.withConsumer(ks -> ks.to(emailTopicName)));

            KafkaStreams streams = new KafkaStreams(builder.build(), kafkaStreamsProperties);
            streams.start();
            Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private static Properties getKafkaStreamsProperties(Properties propertiesFromFile) {
        var kafkaStreamsProperties = new Properties();

        kafkaStreamsProperties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, propertiesFromFile.get("kafka.bootstrap.servers"));
        kafkaStreamsProperties.put("sasl.mechanism", propertiesFromFile.get("kafka.sasl.mechanism"));
        kafkaStreamsProperties.put("security.protocol", propertiesFromFile.get("kafka.security.protocol"));
        kafkaStreamsProperties.put("sasl.jaas.config", propertiesFromFile.get("kafka.sasl.jaas.config"));

        kafkaStreamsProperties.put(StreamsConfig.APPLICATION_ID_CONFIG, "notification-streams");
        kafkaStreamsProperties.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);
        kafkaStreamsProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return kafkaStreamsProperties;
    }


}
