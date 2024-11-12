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

import java.util.List;

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
        return Converter.paramsToJedisCluster(getJedisClusterParams(properties.getRedisNodes(), properties.getMaxTotal(),
                properties.getMinIdle(), properties.getMaxIdle(), properties.isBlockWhenExhausted()));
    }

    private UtilsRecords.JedisConfigParams getJedisClusterParams(List<String> nodes, int maxTotal, int minIdle, int maxIdle, boolean blockWhenExhausted) {
        return new UtilsRecords.JedisConfigParams(nodes, maxTotal, minIdle, maxIdle, blockWhenExhausted);
    }
}
