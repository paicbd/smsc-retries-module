package paic.retries.module.processor;

import com.paicbd.smsc.dto.ErrorCodeMapping;
import com.paicbd.smsc.dto.MessageEvent;
import com.paicbd.smsc.kafka.KafkaConsumerConstants;
import com.paicbd.smsc.kafka.KafkaTopicsConstants;
import com.paicbd.smsc.scylla.ScyllaManager;
import com.paicbd.smsc.utils.Converter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import paic.retries.module.component.RetryParams;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmsRetryConsumer {
    private static final ErrorCodeMapping defaultErrorCode = new ErrorCodeMapping(98, 98, "EXPIRED");

    private final RetryParams retryParams;
    private final ScyllaManager scyllaManager;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @KafkaListener(
            topics = KafkaTopicsConstants.RETRIES_HIGH_TOPIC,
            groupId = KafkaConsumerConstants.RETRIES_HIGH_GROUP_ID,
            containerFactory = "highPriorityKafkaListenerContainerFactory")
    public void processHighPriorityMessage(List<String> smsList) {
        this.smsConsumer(smsList);
    }

    @KafkaListener(
            topics = KafkaTopicsConstants.RETRIES_MEDIUM_TOPIC,
            groupId = KafkaConsumerConstants.RETRIES_MEDIUM_GROUP_ID,
            containerFactory = "mediumPriorityKafkaListenerContainerFactory")
    public void processMediumPriorityMessage(List<String> smsList) {
        this.smsConsumer(smsList);
    }

    @KafkaListener(
            topics = KafkaTopicsConstants.RETRIES_LOW_TOPIC,
            groupId = KafkaConsumerConstants.RETRIES_LOW_GROUP_ID,
            containerFactory = "lowPriorityKafkaListenerContainerFactory")
    public void processLowPriorityMessage(List<String> smsList) {
        this.smsConsumer(smsList);
    }


    private void smsConsumer(List<String> smsList) {
        Collection<MessageEvent> messageEventList = smsList.stream()
                .map(message -> Converter.stringToObject(message, MessageEvent.class))
                .filter(Objects::nonNull)
                .toList();

        long currentTimeSeconds = getCurrentTimeSeconds();
        Flux.fromIterable(messageEventList)
                .parallel()
                .runOn(Schedulers.boundedElastic())
                .flatMap(messageEvent -> {
                    log.info("Message to Process: {}", messageEvent.getMessageId());
                    long receivedTimeSeconds = this.getReceivedTimeSeconds(messageEvent.getId());
                    if (receivedTimeSeconds == 0) {
                        log.error("Error on process retry for message: {}, it can not get the received time", messageEvent.getMessageId());
                        return Flux.empty();
                    }

                    if (messageEvent.getValidityPeriod() < this.retryParams.getFirstRetryDelay()) {
                        log.error("Error on process retry for message: {}, the first delay ({}) is > at validity period of the message ({})",
                                messageEvent.getMessageId(), this.retryParams.getFirstRetryDelay(), messageEvent.getValidityPeriod());
                        log.info("Sending Delivery Receipt with EXPIRED status for message: {}", messageEvent.getMessageId());
                        this.kafkaTemplate.send(KafkaTopicsConstants.PRE_DELIVER_TOPIC, messageEvent.createDeliveryReceiptMessage(defaultErrorCode, "Message expired before first retry").toString());
                        return Flux.empty();
                    }
                    long expiredTime = receivedTimeSeconds + messageEvent.getValidityPeriod();

                    messageEvent.setRetry(true);
                    int retryNumber = messageEvent.getRetryNumber();
                    int currentDueDelay = (retryNumber == 1)
                            ? this.retryParams.getFirstRetryDelay()
                            : this.retryParams.getRetryDelayMultiplier() * messageEvent.getDueDelay();

                    messageEvent.setDueDelay(currentDueDelay);
                    messageEvent.setAccumulatedTime(messageEvent.getAccumulatedTime() + currentDueDelay);
                    int nextDueDelay = currentDueDelay * this.retryParams.getRetryDelayMultiplier();


                    // Elapsed time on all retries is the sum of accumulated time and next due delay
                    int nextElapsedTimeOnAllRetries = messageEvent.getAccumulatedTime() + nextDueDelay;
                    long sendTime = currentTimeSeconds + currentDueDelay;
                    long nextSendTime = sendTime + nextDueDelay;
                    if ((nextElapsedTimeOnAllRetries >= this.retryParams.getMaxDueDelay()) ||
                            (nextSendTime >= expiredTime)) {
                        messageEvent.setLastRetry(true);
                    }

                    this.scyllaManager.insertIntoRetriesTable(sendTime, messageEvent.toString());
                    log.info("ReceivedTime: {} - ExpiredTime: {} - SendTime: {}  - NextSendTime: {} IsLastRetry: {} - " +
                                    "ConfiguredValidityPeriod: {} - NextElapseTimeOnAllRetries: {} - AccumulatedTime: {} - NextDueDelay: {}",
                            new Date(receivedTimeSeconds * 1000), new Date(expiredTime * 1000), new Date(sendTime * 1000), new Date(nextSendTime * 1000), messageEvent.isLastRetry(), messageEvent.getValidityPeriod(), nextElapsedTimeOnAllRetries, messageEvent.getAccumulatedTime(), nextDueDelay);
                    return Flux.empty();
                })
                .subscribe();
    }

    private long getCurrentTimeSeconds() {
        return System.currentTimeMillis() / 1000;
    }

    private long getReceivedTimeSeconds(String idEvent) {
        try {
            return Long.parseLong(idEvent.split("-")[0]) / 1000;
        } catch (Exception ex) {
            log.error("Error on get the received Time from idEvent: {}", idEvent, ex);
        }
        return 0;

    }
}
