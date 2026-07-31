package paic.retries.module.processor;

import com.paicbd.smsc.dto.MessageEvent;
import com.paicbd.smsc.kafka.KafkaUtils;
import com.paicbd.smsc.scylla.ScyllaManager;
import com.paicbd.smsc.utils.Converter;
import com.paicbd.smsc.utils.GeneralSmscConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmsRetryProducer {
    private final ScyllaManager scyllaManager;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Async
    @Scheduled(fixedRateString = "1000")
    public void smsProducer() {
        long currentTimeSeconds = System.currentTimeMillis() / 1000;
        List<String> smsList = scyllaManager.selectAllMessageRetriesBySendTime(currentTimeSeconds);
        if (smsList.isEmpty()) {
            return;
        }
        scyllaManager.deleteAllMessageRetriesBySendTime(currentTimeSeconds);
        log.info("Putting {} messages to kafka", smsList.size());
        Collection<MessageEvent> messageEvents = smsList.stream()
                .map(message -> Converter.stringToObject(message, MessageEvent.class))
                .filter(Objects::nonNull)
                .toList();

        Flux.fromIterable(messageEvents)
                .parallel()
                .runOn(Schedulers.boundedElastic())
                .doOnNext(messageEvent -> {
                    log.debug("To GlobalSmsList message {}", messageEvent.getMessageId());
                    if (messageEvent.isNetworkNotifyError()) {
                        String hashName = messageEvent.getMsisdn() + GeneralSmscConstants.NETWORK_ERROR_SUFFIX_HASH_NAME;
                        log.info("Getting message from scylla hash -> {}", hashName);
                        var responseList = scyllaManager.getMessageFromErrorNetworksByKeyAndMessageId(hashName, messageEvent.getMessageId());
                        if (responseList.isEmpty()) {
                            return;
                        }
                    }
                    String topicToSend = KafkaUtils.getMessageDestinationTopic(messageEvent);
                    log.debug("Sending message to topic {}", topicToSend);
                    kafkaTemplate.send(topicToSend, messageEvent.toString());
                })
                .subscribe();
    }
}
