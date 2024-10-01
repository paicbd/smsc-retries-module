package paic.retries.module.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class AppPropertiesTest {
    @InjectMocks
    private AppProperties properties;

    @BeforeEach
    void setUp() throws Exception {
        injectField("redisNodes", Arrays.asList("node1", "node2", "node3"));
        injectField("maxTotal", 20);
        injectField("maxIdle", 20);
        injectField("minIdle", 1);
        injectField("blockWhenExhausted", true);
        injectField("host", "localhost");
        injectField("port", 9976);
        injectField("path", "/ws");
        injectField("wsEnabled", true);
        injectField("websocketHeaderName", "Authorization");
        injectField("websocketHeaderValue", "Authorization");
        injectField("webSocketRetryInterval", 10);
        injectField("smsRetryListName", "sms_retry");
        injectField("smsRetryInterval", 1000);
        injectField("smsProcessorInterval", 1000);
        injectField("smppListName", "smpp_list");
        injectField("httpListName", "http_list");
        injectField("ss7ListName", "ss7_list");
        injectField("diameterListName", "diameter_list");
        injectField("ss7HashTableRetry", "msisdn_absent_subscriber");
    }

    private void injectField(String fieldName, Object value) throws Exception {
        Field field = AppProperties.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(properties, value);
    }

    @Test
    void testProperties() {
        List<String> expectedRedisNodes = Arrays.asList("node1", "node2", "node3");
        assertEquals(expectedRedisNodes,properties.getRedisNodes());
        assertEquals(20, properties.getMaxTotal());
        assertEquals(20, properties.getMaxIdle());
        assertEquals(1, properties.getMinIdle());
        assertTrue(properties.isBlockWhenExhausted());
        assertEquals("localhost", properties.getHost());
        assertEquals(9976, properties.getPort());
        assertEquals("/ws", properties.getPath());
        assertTrue(properties.isWsEnabled());
        assertEquals("Authorization", properties.getWebsocketHeaderName());
        assertEquals("Authorization", properties.getWebsocketHeaderValue());
        assertEquals(10, properties.getWebSocketRetryInterval());
        assertEquals("sms_retry", properties.getSmsRetryListName());
        assertEquals(1000, properties.getSmsRetryInterval());
        assertEquals(1000, properties.getSmsProcessorInterval());
        assertEquals("smpp_list", properties.getSmppListName());
        assertEquals("http_list", properties.getHttpListName());
        assertEquals("ss7_list", properties.getSs7ListName());
        assertEquals("diameter_list", properties.getDiameterListName());
        assertEquals("msisdn_absent_subscriber", properties.getSs7HashTableRetry());
    }
}