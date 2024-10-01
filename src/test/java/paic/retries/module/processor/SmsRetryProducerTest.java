package paic.retries.module.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import paic.retries.module.utils.AppProperties;
import redis.clients.jedis.JedisCluster;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmsRetryProducerTest {
    @Mock(strictness = Mock.Strictness.LENIENT)
    JedisCluster jedisCluster;
    @Mock(strictness = Mock.Strictness.LENIENT)
    AppProperties properties;
    SmsRetryProducer producer;

    @BeforeEach
    void setUp() {
        this.producer = new SmsRetryProducer(properties, jedisCluster);
    }

    @Test
    void emptyList() {
        String listName = anyString();
        when(jedisCluster.llen(listName)).thenReturn(0L);
        when(jedisCluster.lpop(listName, 0)).thenReturn(new ArrayList<>());
        assertDoesNotThrow(() -> producer.smsProducer());
    }

    @ParameterizedTest
    @ValueSource(strings = {"SMPP", "HTTP", "SS7", "DIAMETER", ""})
    void smsConsumer(String type) {
        String listName = anyString();
        when(jedisCluster.llen(listName)).thenReturn(1L);
        when(jedisCluster.lpop(anyString(), anyInt())).thenReturn(List.of(
                "{\"is_network_notify_error\":false,\"dest_protocol\":\"" + type + "\"}"
        ));
        assertDoesNotThrow(() -> producer.smsProducer());
    }

    @Test
    void IsNetworkNotifyError() {
        String listName = anyString();
        when(jedisCluster.llen(listName)).thenReturn(1L);
        when(jedisCluster.lpop(anyString(), anyInt())).thenReturn(List.of(
                "{\"message_id\":\"1\",\"is_network_notify_error\":true,\"dest_protocol\":\"SS7\"}"
        ));
        assertDoesNotThrow(() -> producer.smsProducer());
    }
}