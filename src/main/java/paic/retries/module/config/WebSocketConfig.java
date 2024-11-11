package paic.retries.module.config;

import com.paicbd.smsc.dto.UtilsRecords;
import com.paicbd.smsc.utils.Generated;
import com.paicbd.smsc.ws.SocketClient;
import com.paicbd.smsc.ws.SocketSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import paic.retries.module.component.CustomFrameHandler;
import paic.retries.module.utils.AppProperties;

import java.util.List;

import static paic.retries.module.component.CustomFrameHandler.UPDATE_RETRY_PARAMS;

@Generated
@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebSocketConfig {
    private final AppProperties appProperties;
    private final SocketSession socketSession;
    private final CustomFrameHandler customFrameHandler;

    @Bean
    public SocketClient socketClient() {
        List<String> topicsToSubscribe = List.of(UPDATE_RETRY_PARAMS);
        UtilsRecords.WebSocketConnectionParams wsp = new UtilsRecords.WebSocketConnectionParams(
                appProperties.isWsEnabled(),
                appProperties.getHost(),
                appProperties.getPort(),
                appProperties.getPath(),
                topicsToSubscribe,
                appProperties.getWebsocketHeaderName(),
                appProperties.getWebsocketHeaderValue(),
                appProperties.getWebSocketRetryInterval(),
                "RETRIES-MODULE"
        );
        return new SocketClient(customFrameHandler, wsp, socketSession);
    }
}
