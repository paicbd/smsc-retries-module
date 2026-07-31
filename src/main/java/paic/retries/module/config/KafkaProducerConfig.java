package paic.retries.module.config;

import lombok.Generated;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import paic.retries.module.utils.AppProperties;

import java.util.HashMap;
import java.util.Map;


@Generated
@Configuration
@RequiredArgsConstructor
public class KafkaProducerConfig {
    private final AppProperties appProperties;

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, appProperties.getKafkaBootstrapServers());
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, appProperties.getKafkaBootstrapServers());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        return createContainerFactory(appProperties.getKafkaListenerConcurrency());
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> highPriorityKafkaListenerContainerFactory() {
        return createContainerFactory(appProperties.getKafkaHighPriorityConcurrency());
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> mediumPriorityKafkaListenerContainerFactory() {
        return createContainerFactory(appProperties.getKafkaMediumPriorityConcurrency());
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> lowPriorityKafkaListenerContainerFactory() {
        return createContainerFactory(appProperties.getKafkaLowPriorityConcurrency());
    }

    private ConcurrentKafkaListenerContainerFactory<String, String> createContainerFactory(int concurrency) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setBatchListener(true);
        factory.setConcurrency(concurrency);
        factory.getContainerProperties().setShutdownTimeout(10000);
        return factory;
    }
}
