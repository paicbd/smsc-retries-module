package paic.retries.module.processor;

import com.paicbd.smsc.dto.MessageEvent;
import com.paicbd.smsc.utils.Converter;

import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.ONE_SECOND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import paic.retries.module.component.RetryParams;
import paic.retries.module.utils.AppProperties;
import redis.clients.jedis.JedisCluster;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmsRetryConsumerTest {
    private static long currentTimeSeconds;

    @Mock
    JedisCluster jedisCluster;

    @Mock
    AppProperties properties;

    @Mock
    RetryParams retryParams;

    @InjectMocks
    SmsRetryConsumer smsRetryConsumer;

    @BeforeEach
    void initValues() {
        currentTimeSeconds = System.currentTimeMillis() / 1000;
        when(properties.getSmsRetryListName()).thenReturn("sms_retry");
        when(this.jedisCluster.llen(properties.getSmsRetryListName())).thenReturn(1L);
    }

    @Test
    @DisplayName("Adding the message into the retry list when it is a first retry")
    void smsConsumerWhenIsFirstRetryThenCheckValues() {
        int maxDueDelay = 86400;
        int delayMultiplier = 2;
        int firstRetryDelay = 10;
        when(this.retryParams.getMaxDueDelay()).thenReturn(maxDueDelay);
        when(this.retryParams.getRetryDelayMultiplier()).thenReturn(delayMultiplier);
        when(this.retryParams.getFirstRetryDelay()).thenReturn(firstRetryDelay);
        MessageEvent messageEventTaken = MessageEvent.builder()
                .messageId("1719421854353-11028072268459")
                .systemId("systemId")
                .deliverSmId("1")
                .sourceAddrNpi(1)
                .sourceAddr("50510201020")
                .destAddrTon(1)
                .destAddrNpi(1)
                .destinationAddr("50582368999")
                .errorCode("500") // HTTP Error Code
                .validityPeriod(360)
                .accumulatedTime(0)
                .dueDelay(0)
                .retryNumber(1)
                .build();
        when(jedisCluster.lpop(properties.getSmsRetryListName(), 1)).thenReturn(List.of(messageEventTaken.toString()));
        smsRetryConsumer.smsConsumer();
        toSleep();
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> listNameCaptor = ArgumentCaptor.forClass(String.class);
        verify(jedisCluster).rpush(listNameCaptor.capture(), messageCaptor.capture());
        String message = messageCaptor.getValue();
        MessageEvent messageEvent = Converter.stringToObject(message, MessageEvent.class);
        assertNotNull(messageEvent);
        assertTrue(messageEvent.isRetry());
        assertEquals(10, messageEvent.getAccumulatedTime());
        assertEquals(10, messageEvent.getDueDelay());
        assertTrue(Long.parseLong(listNameCaptor.getValue()) > currentTimeSeconds);
    }

    @Test
    @DisplayName("Adding the message into the retry list when it is not a first retry")
    void smsConsumerWhenIsNotFirstRetryThenCheckValues() {
        int maxDueDelay = 86400;
        int delayMultiplier = 2;
        int firstRetryDelay = 10;
        when(this.retryParams.getMaxDueDelay()).thenReturn(maxDueDelay);
        when(this.retryParams.getRetryDelayMultiplier()).thenReturn(delayMultiplier);
        when(this.retryParams.getFirstRetryDelay()).thenReturn(firstRetryDelay);
        MessageEvent messageEventTaken = MessageEvent.builder()
                .messageId("1719421854353-11028072268459")
                .systemId("systemId")
                .deliverSmId("1")
                .sourceAddrNpi(1)
                .sourceAddr("50510201020")
                .destAddrTon(1)
                .destAddrNpi(1)
                .destinationAddr("50582368999")
                .errorCode("500") // HTTP Error Code
                .validityPeriod(160)
                .dueDelay(10)
                .accumulatedTime(10)
                .retryNumber(2)
                .build();
        when(jedisCluster.lpop(properties.getSmsRetryListName(), 1)).thenReturn(List.of(messageEventTaken.toString()));
        smsRetryConsumer.smsConsumer();
        toSleep();
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> listNameCaptor = ArgumentCaptor.forClass(String.class);
        verify(jedisCluster).rpush(listNameCaptor.capture(), messageCaptor.capture());
        String message = messageCaptor.getValue();
        MessageEvent messageEvent = Converter.stringToObject(message, MessageEvent.class);
        assertNotNull(messageEvent);
        assertTrue(messageEvent.isRetry());
        assertEquals(30, messageEvent.getAccumulatedTime());
        assertEquals(20, messageEvent.getDueDelay());
        assertTrue(Long.parseLong(listNameCaptor.getValue()) > currentTimeSeconds);
    }

    @Test
    @DisplayName("Validating when the retry list size is equals to zero")
    void smsConsumerWhenListSizeIsZeroThenDoNothing() {
        when(this.jedisCluster.llen(properties.getSmsRetryListName())).thenReturn(0L);
        smsRetryConsumer.smsConsumer();
        verify(jedisCluster, never()).lpop(eq(properties.getSmsRetryListName()), anyInt());
    }

    @Test
    @DisplayName("Validating when validity period less that first retry value")
    void smsConsumerWhenValidityPeriodLessThatFirstRetryThenDoNothing() {
        int firstRetryDelay = 20;
        when(this.retryParams.getFirstRetryDelay()).thenReturn(firstRetryDelay);
        MessageEvent messageEventTaken = MessageEvent.builder()
                .messageId("1719421854353-11028072268459")
                .systemId("systemId")
                .deliverSmId("1")
                .sourceAddrNpi(1)
                .sourceAddr("50510201020")
                .destAddrTon(1)
                .destAddrNpi(1)
                .destinationAddr("50582368999")
                .errorCode("500") // HTTP Error Code
                .validityPeriod(10)
                .accumulatedTime(0)
                .dueDelay(0)
                .retryNumber(1)
                .build();
        when(jedisCluster.lpop(properties.getSmsRetryListName(), 1)).thenReturn(List.of(messageEventTaken.toString()));
        smsRetryConsumer.smsConsumer();
        toSleep();
        assertEquals(0, messageEventTaken.getAccumulatedTime());
        assertEquals(0, messageEventTaken.getDueDelay());
        verify(jedisCluster, never()).rpush(anyString(), anyString());
    }

    @Test
    @DisplayName("Setting is last retry when next elapsed time is greater or equals that the max due delay")
    void smsConsumerWhenElapsedTimeIsGreaterThanMaxDueDelayThenSetLastRetry() {
        int maxDueDelay = 900;
        int delayMultiplier = 2;
        int firstRetryDelay = 10;
        when(this.retryParams.getMaxDueDelay()).thenReturn(maxDueDelay);
        when(this.retryParams.getRetryDelayMultiplier()).thenReturn(delayMultiplier);
        when(this.retryParams.getFirstRetryDelay()).thenReturn(firstRetryDelay);
        MessageEvent messageEventTaken = MessageEvent.builder()
                .messageId("1719421854353-11028072268459")
                .systemId("systemId")
                .deliverSmId("1")
                .sourceAddrNpi(1)
                .sourceAddr("50510201020")
                .destAddrTon(1)
                .destAddrNpi(1)
                .destinationAddr("50582368999")
                .errorCode("500") // HTTP Error Code
                .validityPeriod(3000)
                .dueDelay(160)
                .accumulatedTime(310)
                .retryNumber(6)
                .build();
        when(jedisCluster.lpop(properties.getSmsRetryListName(), 1)).thenReturn(List.of(messageEventTaken.toString()));
        smsRetryConsumer.smsConsumer();
        toSleep();
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> listNameCaptor = ArgumentCaptor.forClass(String.class);
        verify(jedisCluster).rpush(listNameCaptor.capture(), messageCaptor.capture());
        String message = messageCaptor.getValue();
        MessageEvent messageEvent = Converter.stringToObject(message, MessageEvent.class);
        assertNotNull(messageEvent);
        assertTrue(messageEvent.isRetry());
        assertTrue(messageEvent.isLastRetry());
        assertEquals(630, messageEvent.getAccumulatedTime());
        assertEquals(320, messageEvent.getDueDelay());
        assertTrue(Long.parseLong(listNameCaptor.getValue()) > currentTimeSeconds);
    }

    @Test
    @DisplayName("Setting is last retry when next elapsed time is greater or equals that the validity period")
    void smsConsumerWhenElapsedTimeIsGreaterThanValidityPeriodThenSetLastRetry() {
        int maxDueDelay = 3000;
        int delayMultiplier = 2;
        int firstRetryDelay = 10;
        when(this.retryParams.getMaxDueDelay()).thenReturn(maxDueDelay);
        when(this.retryParams.getRetryDelayMultiplier()).thenReturn(delayMultiplier);
        when(this.retryParams.getFirstRetryDelay()).thenReturn(firstRetryDelay);

        MessageEvent messageEventTaken = MessageEvent.builder()
                .messageId("1719421854353-11028072268459")
                .systemId("systemId")
                .deliverSmId("1")
                .sourceAddrNpi(1)
                .sourceAddr("50510201020")
                .destAddrTon(1)
                .destAddrNpi(1)
                .destinationAddr("50582368999")
                .errorCode("500") // HTTP Error Code
                .validityPeriod(300)
                .dueDelay(40)
                .accumulatedTime(70)
                .retryNumber(4)
                .build();
        when(jedisCluster.lpop(properties.getSmsRetryListName(), 1)).thenReturn(List.of(messageEventTaken.toString()));
        smsRetryConsumer.smsConsumer();
        toSleep();
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> listNameCaptor = ArgumentCaptor.forClass(String.class);
        verify(jedisCluster).rpush(listNameCaptor.capture(), messageCaptor.capture());
        String message = messageCaptor.getValue();
        MessageEvent messageEvent = Converter.stringToObject(message, MessageEvent.class);
        assertNotNull(messageEvent);
        assertTrue(messageEvent.isRetry());
        assertTrue(messageEvent.isLastRetry());
        assertEquals(150, messageEvent.getAccumulatedTime());
        assertEquals(80, messageEvent.getDueDelay());
        assertTrue(Long.parseLong(listNameCaptor.getValue()) > currentTimeSeconds);
    }

    private static void toSleep() {
        await().atMost(ONE_SECOND).until(() -> true);
    }
}