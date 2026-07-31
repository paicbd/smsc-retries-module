package paic.retries.module.utils;

import com.paicbd.smsc.utils.Generated;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Generated
@Getter
@Component
public class AppProperties {
    @Value("#{'${redis.cluster.nodes}'.split(',')}")
    private List<String> redisNodes;

    @Value("${redis.threadPool.maxTotal}")
    private int redisMaxTotal;

    @Value("${redis.threadPool.maxIdle}")
    private int redisMaxIdle;

    @Value("${redis.threadPool.minIdle}")
    private int redisMinIdle;

    @Value("${redis.threadPool.blockWhenExhausted}")
    private boolean redisBlockWhenExhausted;

    @Value("${redis.connection.timeout:0}")
    private int redisConnectionTimeout;

    @Value("${redis.so.timeout:0}")
    private int redisSoTimeout;

    @Value("${redis.maxAttempts:0}")
    private int redisMaxAttempts;

    @Value("${redis.connection.password:}")
    private String redisPassword;

    @Value("${redis.connection.user:}")
    private String redisUser;

    @Value("${websocket.server.host}")
    private String host;

    @Value("${websocket.server.port}")
    private int port;

    @Value("${websocket.server.path}")
    private String path;

    @Value("${websocket.server.enabled}")
    private boolean wsEnabled;

    @Value("${websocket.header.name}")
    private String websocketHeaderName;

    @Value("${websocket.header.value}")
    private String websocketHeaderValue;

    @Value("${websocket.retry.intervalSeconds}")
    private int webSocketRetryInterval;

    @Value("${spring.kafka.bootstrap-servers}")
    private String kafkaBootstrapServers;

    @Value("${spring.kafka.listener.concurrency}")
    private int kafkaListenerConcurrency;

    @Value("${kafka.listener.high.concurrency:3}")
    private int kafkaHighPriorityConcurrency;

    @Value("${kafka.listener.medium.concurrency:2}")
    private int kafkaMediumPriorityConcurrency;

    @Value("${kafka.listener.low.concurrency:1}")
    private int kafkaLowPriorityConcurrency;

    @Value("${scylla.contact.points}")
    private String contactPoints;

    @Value("${scylla.datacenter}")
    private String localDatacenter;

    @Value("${scylla.user}")
    private String username;

    @Value("${scylla.password}")
    private String password;
}
