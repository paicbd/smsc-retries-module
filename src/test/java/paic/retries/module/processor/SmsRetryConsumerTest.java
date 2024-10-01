package paic.retries.module.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import paic.retries.module.config.RetryParams;
import paic.retries.module.utils.AppProperties;
import redis.clients.jedis.JedisCluster;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmsRetryConsumerTest {
    @Mock(strictness = Mock.Strictness.LENIENT)
    JedisCluster jedisCluster;
    @Mock(strictness = Mock.Strictness.LENIENT)
    AppProperties properties;
    @Mock(strictness = Mock.Strictness.LENIENT)
    RetryParams retryParams;

    SmsRetryConsumer smsRetryConsumer;

    @BeforeEach
    void setUp() {
        this.smsRetryConsumer = new SmsRetryConsumer(properties, jedisCluster, retryParams);
    }

    @Test
    void smsConsumer() {
        when(properties.getSmsRetryListName()).thenReturn("sms_retry");
        when(this.jedisCluster.llen(properties.getSmsRetryListName())).thenReturn(1L);
        when(this.retryParams.getFirstRetryDelay()).thenReturn(20);
        when(this.retryParams.getRetryDelayMultiplier()).thenReturn(2);
        when(jedisCluster.lpop(properties.getSmsRetryListName(), 1)).thenReturn(List.of(
                "{\"system_id\":\"1\",\"deliver_sm_id\":1,\"source_addr_npi\":1,\"source_addr\":\"50510201020\",\"dest_addr_ton\":1,\"dest_addr_npi\":1,\"destination_addr\":\"50582368999\",\"status\":null,\"error_code\":null,\"optional_parameters\":null,\"check_submit_sm_response\":true,\"validity_period\":30,\"due_delay\":20,\"accumulated_time\":0,\"retry_number\":0}"
        ));
        assertDoesNotThrow(() -> smsRetryConsumer.smsConsumer());
    }

    @Test
    void retryNumberEqual1() {
        when(properties.getSmsRetryListName()).thenReturn("sms_retry");
        when(this.jedisCluster.llen(properties.getSmsRetryListName())).thenReturn(1L);
        when(this.retryParams.getFirstRetryDelay()).thenReturn(20);
        when(this.retryParams.getRetryDelayMultiplier()).thenReturn(2);
        when(jedisCluster.lpop(properties.getSmsRetryListName(), 1)).thenReturn(List.of(
                "{\"system_id\":\"1\",\"deliver_sm_id\":1,\"source_addr_npi\":1,\"source_addr\":\"50510201020\",\"dest_addr_ton\":1,\"dest_addr_npi\":1,\"destination_addr\":\"50582368999\",\"status\":null,\"error_code\":null,\"optional_parameters\":null,\"check_submit_sm_response\":true,\"validity_period\":30,\"due_delay\":20,\"accumulated_time\":0,\"retry_number\":1}"
        ));
        assertDoesNotThrow(() -> smsRetryConsumer.smsConsumer());
    }

    @Test
    void listSizeZero() {
        when(properties.getSmsRetryListName()).thenReturn("sms_retry");
        when(this.jedisCluster.llen(properties.getSmsRetryListName())).thenReturn(0L);
        assertDoesNotThrow(() -> smsRetryConsumer.smsConsumer());
    }

    @Test
    void validityPeriodLessThatFirstRetry() {
        when(properties.getSmsRetryListName()).thenReturn("sms_retry");
        when(this.jedisCluster.llen(properties.getSmsRetryListName())).thenReturn(1L);
        when(this.retryParams.getFirstRetryDelay()).thenReturn(20);
        when(jedisCluster.lpop(properties.getSmsRetryListName(), 1)).thenReturn(List.of(
                "{\"system_id\":\"1\",\"deliver_sm_id\":1,\"source_addr_npi\":1,\"source_addr\":\"50510201020\",\"dest_addr_ton\":1,\"dest_addr_npi\":1,\"destination_addr\":\"50582368999\",\"status\":null,\"error_code\":null,\"optional_parameters\":null,\"check_submit_sm_response\":true,\"validity_period\":10,\"due_delay\":20}"
        ));
        assertDoesNotThrow(() -> smsRetryConsumer.smsConsumer());
    }

    @Test
    void nextElapsedTimeOnAllRetriesLessOrEqualsMaxDueDelay() {
        when(properties.getSmsRetryListName()).thenReturn("sms_retry");
        when(this.jedisCluster.llen(properties.getSmsRetryListName())).thenReturn(1L);
        when(this.retryParams.getFirstRetryDelay()).thenReturn(10);
        when(this.retryParams.getRetryDelayMultiplier()).thenReturn(2);
        when(this.retryParams.getMaxDueDelay()).thenReturn(3000);
        when(jedisCluster.lpop(properties.getSmsRetryListName(), 1)).thenReturn(List.of(
                "{\"message_id\":\"1\",\"system_id\":\"1\",\"deliver_sm_id\":1,\"source_addr_npi\":1,\"source_addr\":\"50510201020\",\"dest_addr_ton\":1,\"dest_addr_npi\":1,\"destination_addr\":\"50582368999\",\"status\":null,\"error_code\":null,\"optional_parameters\":null,\"check_submit_sm_response\":true,\"validity_period\":3000,\"due_delay\":20,\"accumulated_time\":30,\"retry_number\":2}"
        ));
        assertDoesNotThrow(() -> smsRetryConsumer.smsConsumer());
    }
}