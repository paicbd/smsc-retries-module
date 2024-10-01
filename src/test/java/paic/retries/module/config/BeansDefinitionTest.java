package paic.retries.module.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import paic.retries.module.utils.AppProperties;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BeansDefinitionTest {
    @Mock
    AppProperties appProperties;

    @InjectMocks
    BeansDefinition beansDefinition;

    @Test
    void testJedisClusterCreation() {
        when(appProperties.getRedisNodes()).thenReturn(List.of("localhost:6379", "localhost:6380"));
        when(appProperties.getMaxTotal()).thenReturn(10);
        when(appProperties.getMinIdle()).thenReturn(1);
        when(appProperties.getMaxIdle()).thenReturn(5);
        when(appProperties.isBlockWhenExhausted()).thenReturn(true);
        assertNull(beansDefinition.jedisCluster());
    }

    @Test
    void testSocketSession() {
        assertNotNull(beansDefinition.socketSession());
    }
}