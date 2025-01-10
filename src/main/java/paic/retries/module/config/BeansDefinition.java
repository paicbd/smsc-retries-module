package paic.retries.module.config;

import com.paicbd.smsc.dto.UtilsRecords;
import com.paicbd.smsc.utils.Converter;
import com.paicbd.smsc.utils.Generated;
import com.paicbd.smsc.ws.SocketSession;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import paic.retries.module.utils.AppProperties;
import redis.clients.jedis.JedisCluster;

@Generated
@Configuration
@RequiredArgsConstructor
public class BeansDefinition {
    private final AppProperties properties;

    @Bean
    public SocketSession socketSession() {
        return new SocketSession("retry-module");
    }

    @Bean
    public JedisCluster jedisCluster() {
        return Converter.paramsToJedisCluster(
                new UtilsRecords.JedisConfigParams(properties.getRedisNodes(), properties.getRedisMaxTotal(),
                        properties.getRedisMaxIdle(), properties.getRedisMinIdle(),
                        properties.isRedisBlockWhenExhausted(), properties.getRedisConnectionTimeout(),
                        properties.getRedisSoTimeout(), properties.getRedisMaxAttempts(),
                        properties.getRedisUser(), properties.getRedisPassword())
        );
    }
}
