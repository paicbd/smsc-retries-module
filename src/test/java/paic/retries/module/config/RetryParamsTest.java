package paic.retries.module.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import redis.clients.jedis.JedisCluster;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetryParamsTest {
    @Mock(strictness = Mock.Strictness.LENIENT)
    JedisCluster jedisCluster;
    RetryParams retryParams;

    @BeforeEach
    void setUp() {
        this.retryParams = new RetryParams(jedisCluster);
    }

    @Test
    void init() {
        when(jedisCluster.hget("general_settings", "smsc_retry")).thenReturn("{\"maxDueDelay\":1,\"retryDelayMultiplier:\":1,\"firstRetryDelay\":1}");
        assertDoesNotThrow(() -> retryParams.init());
    }

    @Test
    void retryParams() {
        assertEquals(0, this.retryParams.getFirstRetryDelay());
        assertEquals(0, this.retryParams.getRetryDelayMultiplier());
        assertEquals(0, this.retryParams.getMaxDueDelay());
        assertNotNull(this.retryParams.getJedisCluster());
    }
}