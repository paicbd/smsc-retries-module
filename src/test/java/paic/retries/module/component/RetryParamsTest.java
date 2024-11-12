package paic.retries.module.component;

import com.paicbd.smsc.utils.Converter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import redis.clients.jedis.JedisCluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetryParamsTest {
    @Mock
    JedisCluster jedisCluster;

    @InjectMocks
    RetryParams retryParams;

    @Test
    @DisplayName("Setting retry params")
    void initWhenReadRetryParamsThenSetValuesToObject() {
        RetryParams.RetryParamsObject retryParamsObject = new RetryParams.RetryParamsObject();
        retryParamsObject.setMaxDueDelay(86400);
        retryParamsObject.setRetryDelayMultiplier(2);
        retryParamsObject.setFirstRetryDelay(0);

        when(jedisCluster.hget("general_settings", "smsc_retry")).thenReturn(Converter.valueAsString(retryParamsObject));
        retryParams.init();
        assertEquals(0, this.retryParams.getFirstRetryDelay());
        assertEquals(2, this.retryParams.getRetryDelayMultiplier());
        assertEquals(86400, this.retryParams.getMaxDueDelay());
    }

    @Test
    @DisplayName("Validating default retry params")
    void initWhenReadDefaultValuesThenSetValuesToObject() {
        assertEquals(0, this.retryParams.getFirstRetryDelay());
        assertEquals(0, this.retryParams.getRetryDelayMultiplier());
        assertEquals(0, this.retryParams.getMaxDueDelay());
        assertNotNull(this.retryParams.getJedisCluster());
    }
}