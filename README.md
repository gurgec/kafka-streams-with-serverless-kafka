# Kafka Streams With Serverless Kafka
The definition of the repository is here.

https://medium.com/@gokhan-gurgec/kafka-streams-with-serverless-kafka-44311350185a

## Requirements and Dependencies

- Upstash Kafka Cluster

- Java

- application.properties
    ```
    kafka.bootstrap.servers=<taken from upstash cluster>
    kafka.sasl.mechanism=<taken from upstash cluster>
    kafka.security.protocol=<taken from upstash cluster>
    kafka.sasl.jaas.config=<taken from upstash cluster>
    kafka.notification.topic=notification
    kafka.pushnotification.topic=pushnotification
    kafka.sms.topic=sms
    kafka.email.topic=email
    ```
## Running the application
```./gradlew clean run```
