package paic.retries.module.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.paicbd.smsc.utils.Converter;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisCluster;

@Getter
@Slf4j
@Component
@RequiredArgsConstructor
public class RetryParams {
    private final JedisCluster jedisCluster;
    private int maxDueDelay;
    private int retryDelayMultiplier;
    private int firstRetryDelay;

    @PostConstruct
    public void init() {
        String retryString = jedisCluster.hget("general_settings", "smsc_retry");
        log.info("Retry params from Redis: {}", retryString);
        RetryParamsObject retryParams = Converter.stringToObject(retryString, new TypeReference<>() {
        });

        this.maxDueDelay = retryParams.getMaxDueDelay();
        this.retryDelayMultiplier = retryParams.getRetryDelayMultiplier();
        this.firstRetryDelay = retryParams.getFirstRetryDelay();

        log.info("RetryParams values: maxDueDelay={}, retryDelayMultiplier={}, firstRetryDelay={}",
                maxDueDelay, retryDelayMultiplier, firstRetryDelay);
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class RetryParamsObject {
        @JsonProperty("maxDueDelay")
        private int maxDueDelay;
        @JsonProperty("retryDelayMultiplier")
        private int retryDelayMultiplier;
        @JsonProperty("firstRetryDelay")
        private int firstRetryDelay;
    }
}
