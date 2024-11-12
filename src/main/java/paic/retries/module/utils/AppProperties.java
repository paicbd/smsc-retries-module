package paic.retries.module.utils;

import com.paicbd.smsc.utils.Generated;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Generated
@Getter
@Component
public class AppProperties {
    @Value("#{'${redis.cluster.nodes}'.split(',')}")
    private List<String> redisNodes;

    @Value("${redis.threadPool.maxTotal}")
    private int maxTotal;

    @Value("${redis.threadPool.maxIdle}")
    private int maxIdle;

    @Value("${redis.threadPool.minIdle}")
    private int minIdle;

    @Value("${redis.threadPool.blockWhenExhausted}")
    private boolean blockWhenExhausted;

    @Value("${websocket.server.host}")
    private String host;

    @Value("${websocket.server.port}")
    private int port;

    @Value("${websocket.server.path}")
    private String path;

    @Value("${websocket.server.enabled}")
    private boolean wsEnabled;

    @Value("${websocket.header.name}")
    private String websocketHeaderName;

    @Value("${websocket.header.value}")
    private String websocketHeaderValue;

    @Value("${websocket.retry.intervalSeconds}")
    private int webSocketRetryInterval;

    @Value("${sms.retry.listName}")
    private String smsRetryListName;

    @Value("${sms.retry.interval}")
    private int smsRetryInterval;

    @Value("${sms.processor.interval}")
    private int smsProcessorInterval;

    @Value("${smpp.list.name}")
    private String smppListName;

    @Value("${http.list.name}")
    private String httpListName;

    @Value("${ss7.list.name}")
    private String ss7ListName;

    @Value("${diameter.list.name}")
    private String diameterListName;

    @Value("${ss7.hash.table.retry}")
    private String ss7HashTableRetry;
}
