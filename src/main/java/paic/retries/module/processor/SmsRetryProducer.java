package paic.retries.module.processor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.paicbd.smsc.dto.MessageEvent;
import com.paicbd.smsc.utils.Converter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import paic.retries.module.utils.AppProperties;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import redis.clients.jedis.JedisCluster;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmsRetryProducer {
    private final AppProperties appProperties;
    private final JedisCluster jedisCluster;

    @Async
    @Scheduled(fixedRateString = "${sms.processor.interval}")
    public void smsProducer() {
        String listName = String.valueOf(System.currentTimeMillis() / 1000);
        List<String> smsList = jedisCluster.lpop(listName, (int) jedisCluster.llen(listName));
        if (Objects.isNull(smsList) || smsList.isEmpty()) {
            return;
        }
        log.info("Putting {} messages to the queue", smsList.size());
        Collection<MessageEvent> submitSmEvents = smsList.stream()
                .map(message -> Converter.stringToObject(message, new TypeReference<MessageEvent>() {
                }))
                .toList();

        Flux.fromIterable(submitSmEvents)
                .parallel()
                .runOn(Schedulers.boundedElastic())
                .doOnNext(submitSmEvent -> {
                    log.debug("To GlobalSmsList message {}", submitSmEvent.getMessageId());
                    if (submitSmEvent.isNetworkNotifyError()) {
                        String hashName = submitSmEvent.getMsisdn() + appProperties.getSs7HashTableRetry();
                        log.info("Getting message from redis hash > {}", hashName);
                        String ss7ListIsPresent = jedisCluster.hget(hashName, submitSmEvent.getMessageId());
                        if (Objects.isNull(ss7ListIsPresent)) {
                            return;
                        }
                    }

                    switch (submitSmEvent.getDestProtocol().toLowerCase()) {
                        case "smpp" -> jedisCluster.lpush(appProperties.getSmppListName(), submitSmEvent.toString());
                        case "http" -> jedisCluster.lpush(appProperties.getHttpListName(), submitSmEvent.toString());
                        case "ss7" -> jedisCluster.lpush(appProperties.getSs7ListName(), submitSmEvent.toString());
                        case "diameter" ->
                                jedisCluster.lpush(appProperties.getDiameterListName(), submitSmEvent.toString());
                        default -> log.error("Unknown protocol {}", submitSmEvent.getDestProtocol());
                    }
                })
                .subscribe();
    }
}
