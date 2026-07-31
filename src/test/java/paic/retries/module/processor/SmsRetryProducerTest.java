package paic.retries.module.processor;

import com.paicbd.smsc.dto.MessageEvent;
import com.paicbd.smsc.scylla.ScyllaManager;
import com.paicbd.smsc.utils.GeneralSmscConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.ONE_SECOND;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmsRetryProducerTest {

    @Mock
    ScyllaManager scyllaManager;

    @Mock
    KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    SmsRetryProducer producer;

    @Test
    @DisplayName("Validating when the list is empty")
    void smsProducerWhenListIsEmptyThenDoNothing() {
        when(scyllaManager.selectAllMessageRetriesBySendTime(anyLong())).thenReturn(new ArrayList<>());
        producer.smsProducer();
        verifyNoMoreInteractions(scyllaManager);
        verifyNoMoreInteractions(kafkaTemplate);
    }

    @ParameterizedTest
    @ValueSource(strings = {"SMPP", "HTTP", "SS7", "DIAMETER"})
    @DisplayName("Adding the message into respective protocol queue")
    void smsProducerWhenListIsNotEmptyThenPutMessageIntoQueue(String protocol) {
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
                .smscMessagePriority(GeneralSmscConstants.HIGH_PRIORITY)
                .destNetworkId(5)
                .errorCode(500) // HTTP Error Code
                .build();
        when(scyllaManager.selectAllMessageRetriesBySendTime(anyLong())).thenReturn(List.of(messageEventTaken.toString()));
        producer.smsProducer();
        toSleep();
        ArgumentCaptor<String> kafkaTopicCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(kafkaTopicCaptor.capture(), anyString());
        assertTrue(kafkaTopicCaptor.getValue().contains(messageEventTaken.getDestProtocol().toLowerCase()));
    }

    @Test
    @DisplayName("Validating when the destination protocol is unknown")
    void smsConsumerWhenDestinationProtocolIsUnknownThenDoNothing() {
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
                .errorCode(500) // HTTP Error Code
                .build();
        when(scyllaManager.selectAllMessageRetriesBySendTime(anyLong())).thenReturn(List.of(messageEventTaken.toString()));
        producer.smsProducer();
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    @DisplayName("Don't put the message in the SS7 queue when the SS7 client sent message")
    void smsProducerWhenIsNetworkNotifyErrorAndSS7HashIsNullThenDoNothing() {
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
                .errorCode(500) // HTTP Error Code
                .build();
        when(scyllaManager.selectAllMessageRetriesBySendTime(anyLong())).thenReturn(List.of(messageEventTaken.toString()));
        when(scyllaManager.getMessageFromErrorNetworksByKeyAndMessageId(
                messageEventTaken.getMsisdn() + GeneralSmscConstants.NETWORK_ERROR_SUFFIX_HASH_NAME, messageEventTaken.getMessageId()))
                .thenReturn(Collections.emptyList());
        producer.smsProducer();
        toSleep();
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    @DisplayName("Putting the message in the SS7 queue when the SS7 hash is not null")
    void smsProducerWhenIsNetworkNotifyErrorAndSS7HashIsNotNullThenPutMessageIntoQueue() {
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
                .smscMessagePriority(GeneralSmscConstants.MEDIUM_PRIORITY)
                .udhBytes(new byte[0])
                .udhRaw(new HashSet<>())
                .errorCode(500) // HTTP Error Code
                .build();
        var resultList = List.of(messageEventTaken.toString());
        when(scyllaManager.selectAllMessageRetriesBySendTime(anyLong())).thenReturn(resultList);

        String key = messageEventTaken.getMsisdn() + GeneralSmscConstants.NETWORK_ERROR_SUFFIX_HASH_NAME;
        when(scyllaManager.getMessageFromErrorNetworksByKeyAndMessageId(key, messageEventTaken.getMessageId()))
                .thenReturn(resultList);
        producer.smsProducer();
        toSleep();
        verify(kafkaTemplate).send(anyString(), eq(messageEventTaken.toString()));
    }

    private static void toSleep() {
        await().atMost(ONE_SECOND).until(() -> true);
    }
}