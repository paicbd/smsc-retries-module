package paic.retries.module.processor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.paicbd.smsc.dto.MessageEvent;
import com.paicbd.smsc.utils.Converter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import paic.retries.module.config.RetryParams;
import paic.retries.module.utils.AppProperties;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import redis.clients.jedis.JedisCluster;

import java.util.Collection;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmsRetryConsumer {
    private final AppProperties appProperties;
    private final JedisCluster jedisCluster;
    private final RetryParams retryParams;

    @Async
    @Scheduled(fixedRateString = "${sms.retry.interval}")
    public void smsConsumer() {
        int size = listSize();
        if (size == 0) {
            log.debug("No messages to process");
            return;
        }

        List<String> smsList = this.jedisCluster.lpop(appProperties.getSmsRetryListName(), size);
        Collection<MessageEvent> submitSmEvents = smsList.stream()
                .map(message -> Converter.stringToObject(message, new TypeReference<MessageEvent>() {
                }))
                .toList();

        long currentTimeSeconds = getCurrentTimeSeconds();
        Flux.fromIterable(submitSmEvents)
                .parallel()
                .runOn(Schedulers.boundedElastic())
                .flatMap(submitSmEvent -> {
                    log.info("Message to Process: {}", submitSmEvent.getMessageId());
                    if (Integer.parseInt(submitSmEvent.getValidityPeriod()) < this.retryParams.getFirstRetryDelay()) {
                        return Flux.empty();
                    }

                    submitSmEvent.setRetry(true);
                    int retryNumber = submitSmEvent.getRetryNumber();
                    int currentDueDelay;
                    if (retryNumber == 1) {
                        currentDueDelay = this.retryParams.getFirstRetryDelay();
                    } else {
                        currentDueDelay = this.retryParams.getRetryDelayMultiplier() * submitSmEvent.getDueDelay();
                    }

                    submitSmEvent.setDueDelay(currentDueDelay);
                    submitSmEvent.setAccumulatedTime(submitSmEvent.getAccumulatedTime() + currentDueDelay);
                    int nextDueDelay = currentDueDelay * this.retryParams.getRetryDelayMultiplier();
                    int validityPeriod = Integer.parseInt(submitSmEvent.getValidityPeriod());

                    // Elapsed time on all retries is the sum of accumulated time and next due delay
                    int nextElapsedTimeOnAllRetries = submitSmEvent.getAccumulatedTime() + nextDueDelay;
                    if ((nextElapsedTimeOnAllRetries >= this.retryParams.getMaxDueDelay()) ||
                            (nextElapsedTimeOnAllRetries >= validityPeriod)) {
                        submitSmEvent.setLastRetry(true);
                    }

                    String listToPut = String.valueOf(currentTimeSeconds + currentDueDelay);
                    this.jedisCluster.rpush(listToPut, submitSmEvent.toString());
                    log.info("NextElapseTimeOnAllRetries: {} - AccumulatedTime: {} - NextDueDelay: {} - ConfiguredValidityPeriod: {} - LastRetry: {}",
                            nextElapsedTimeOnAllRetries, submitSmEvent.getAccumulatedTime(), nextDueDelay, validityPeriod, submitSmEvent.isLastRetry());
                    log.info("Putted to list: {}", listToPut);
                    return Flux.empty();
                })
                .subscribe();
    }

    private int listSize() {
        return (int) this.jedisCluster.llen(appProperties.getSmsRetryListName());
    }

    private long getCurrentTimeSeconds() {
        return System.currentTimeMillis() / 1000;
    }
}
