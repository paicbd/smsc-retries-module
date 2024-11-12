package paic.retries.module.component;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.paicbd.smsc.utils.Converter;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisCluster;

import java.util.Objects;

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
        RetryParamsObject retryParams = Converter.stringToObject(retryString, RetryParamsObject.class);
        Objects.requireNonNull(retryParams, "An error occurred while parsing retry parameters");
        this.maxDueDelay = retryParams.getMaxDueDelay();
        this.retryDelayMultiplier = retryParams.getRetryDelayMultiplier();
        this.firstRetryDelay = retryParams.getFirstRetryDelay();

        log.info("RetryParams values: maxDueDelay={}, retryDelayMultiplier={}, firstRetryDelay={}",
                maxDueDelay, retryDelayMultiplier, firstRetryDelay);
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RetryParamsObject {
        @JsonProperty("maxDueDelay")
        private int maxDueDelay;
        @JsonProperty("retryDelayMultiplier")
        private int retryDelayMultiplier;
        @JsonProperty("firstRetryDelay")
        private int firstRetryDelay;
    }
}
