package paic.retries.module.processor;

import com.paicbd.smsc.dto.MessageEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import paic.retries.module.utils.AppProperties;
import redis.clients.jedis.JedisCluster;

import java.util.ArrayList;
import java.util.List;

import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.ONE_SECOND;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmsRetryProducerTest {
    @Mock
    JedisCluster jedisCluster;

    @Mock
    AppProperties properties;

    @InjectMocks
    SmsRetryProducer producer;

    @Test
    @DisplayName("Validating when the list is empty")
    void smsProducerWhenListIsEmptyThenDoNothing() {
        when(jedisCluster.llen(anyString())).thenReturn(0L);
        when(jedisCluster.lpop(anyString(), eq(0))).thenReturn(new ArrayList<>());
        producer.smsProducer();
        verify(jedisCluster, never()).lpush(anyString(), anyString());
    }

    @Test
    @DisplayName("Validating when the list is null")
    void smsProducerWhenListIsNullThenDoNothing() {
        when(jedisCluster.llen(anyString())).thenReturn(0L);
        when(jedisCluster.lpop(anyString(), eq(0))).thenReturn(null);
        producer.smsProducer();
        verify(jedisCluster, never()).lpush(anyString(), anyString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"SMPP", "HTTP", "SS7", "DIAMETER"})
    @DisplayName("Adding the message into respective protocol queue")
    void smsProducerWhenListIsNotEmptyThenPutMessageIntoQueue(String protocol) {
        String listName = anyString();
        when(jedisCluster.llen(listName)).thenReturn(1L);
        switch (protocol.toLowerCase()) {
            case "smpp" -> when(properties.getSmppListName()).thenReturn(protocol.toLowerCase().concat("_message"));
            case "http" -> when(properties.getHttpListName()).thenReturn(protocol.toLowerCase().concat("_message"));
            case "ss7" -> when(properties.getSs7ListName()).thenReturn(protocol.toLowerCase().concat("_message"));
            default ->
                    when(properties.getDiameterListName()).thenReturn(protocol.toLowerCase().concat("_message"));
        }
        MessageEvent messageEventTaken = MessageEvent.builder()
                .messageId("1719421854353-11028072268459")
                .msisdn("50510201020")
                .systemId("systemId")
                .deliverSmId("1")
                .sourceAddrTon(1)
                .sourceAddrNpi(4)
                .sourceAddr("50510201020")
                .destAddrTon(1)
                .destAddrNpi(4)
                .destinationAddr("50582368999")
                .isNetworkNotifyError(false)
                .destProtocol(protocol)
                .errorCode("500") // HTTP Error Code
                .build();
        when(jedisCluster.lpop(anyString(), anyInt())).thenReturn(List.of(messageEventTaken.toString()));
        producer.smsProducer();
        toSleep();
        verify(jedisCluster).lpush(protocol.toLowerCase().concat("_message"), messageEventTaken.toString());
    }

    @Test
    @DisplayName("Validating when the destination protocol is unknown")
    void smsConsumerWhenDestinationProtocolIsUnknownThenDoNothing() {
        String listName = anyString();
        when(jedisCluster.llen(listName)).thenReturn(1L);
        MessageEvent messageEventTaken = MessageEvent.builder()
                .messageId("1719421854353-11028072268459")
                .msisdn("50510201020")
                .systemId("systemId")
                .deliverSmId("1")
                .sourceAddrTon(1)
                .sourceAddrNpi(4)
                .sourceAddr("50510201020")
                .destAddrTon(1)
                .destAddrNpi(4)
                .destinationAddr("50582368999")
                .isNetworkNotifyError(false)
                .destProtocol("")
                .errorCode("500") // HTTP Error Code
                .build();
        when(jedisCluster.lpop(anyString(), anyInt())).thenReturn(List.of(messageEventTaken.toString()));
        producer.smsProducer();
        verify(jedisCluster, never()).lpush(anyString(), eq(messageEventTaken.toString()));
    }

    @Test
    @DisplayName("Don't put the message in the SS7 queue when the SS7 client sent message")
    void smsProducerWhenIsNetworkNotifyErrorAndSS7HashIsNullThenDoNothing() {
        String listName = anyString();
        when(jedisCluster.llen(listName)).thenReturn(1L);
        MessageEvent messageEventTaken = MessageEvent.builder()
                .messageId("1719421854353-11028072268459")
                .msisdn("50510201020")
                .systemId("systemId")
                .deliverSmId("1")
                .sourceAddrTon(1)
                .sourceAddrNpi(4)
                .sourceAddr("50510201020")
                .destAddrTon(1)
                .destAddrNpi(4)
                .destinationAddr("50582368999")
                .isNetworkNotifyError(true)
                .destProtocol("SS7")
                .errorCode("500") // HTTP Error Code
                .build();
        when(jedisCluster.lpop(anyString(), anyInt())).thenReturn(List.of(messageEventTaken.toString()));
        when(properties.getSs7HashTableRetry()).thenReturn("_absent_subscriber");
        when(jedisCluster.hget(messageEventTaken.getMsisdn() + "_absent_subscriber", messageEventTaken.getMessageId())).thenReturn(null);
        producer.smsProducer();
        toSleep();
        verifyNoMoreInteractions(jedisCluster);
    }

    @Test
    @DisplayName("Putting the message in the SS7 queue when the SS7 hash is not null")
    void smsProducerWhenIsNetworkNotifyErrorAndSS7HashIsNotNullThenPutMessageIntoQueue() {
        String listName = anyString();
        when(jedisCluster.llen(listName)).thenReturn(1L);
        when(properties.getSs7ListName()).thenReturn("ss7_message");
        MessageEvent messageEventTaken = MessageEvent.builder()
                .messageId("1719421854353-11028072268459")
                .msisdn("50510201020")
                .systemId("1")
                .deliverSmId("1")
                .sourceAddrNpi(1)
                .sourceAddr("50510201020")
                .destAddrTon(1)
                .destAddrNpi(1)
                .destinationAddr("50582368999")
                .isNetworkNotifyError(true)
                .destProtocol("SS7")
                .errorCode("500") // HTTP Error Code
                .build();
        when(properties.getSs7HashTableRetry()).thenReturn("_absent_subscriber");
        when(jedisCluster.lpop(anyString(), anyInt())).thenReturn(List.of(messageEventTaken.toString()));
        String key = messageEventTaken.getMsisdn() + "_absent_subscriber";
        when(jedisCluster.hget(key, messageEventTaken.getMessageId())).thenReturn(messageEventTaken.toString());
        producer.smsProducer();
        toSleep();
        verify(jedisCluster).lpush(anyString(), eq(messageEventTaken.toString()));
    }

    private static void toSleep() {
        await().atMost(ONE_SECOND).until(() -> true);
    }
}