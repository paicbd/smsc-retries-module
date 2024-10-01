package paic.retries.module.config;

import com.paicbd.smsc.ws.SocketSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import paic.retries.module.utils.AppProperties;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSocketConfigTest {
    @Mock
    AppProperties appProperties;
    @Mock
    SocketSession socketSession;
    @Mock
    CustomFrameHandler customFrameHandler;
    WebSocketConfig config;

    @BeforeEach
    void setUp() {
        this.config = new WebSocketConfig(appProperties, socketSession, customFrameHandler);
    }

    @Test
    void socketClient() {
        when(appProperties.isWsEnabled()).thenReturn(true);
        when(appProperties.getHost()).thenReturn("localhost");
        when(appProperties.getPort()).thenReturn(9999);
        when(appProperties.getPath()).thenReturn("/ws");
        when(appProperties.getWebsocketHeaderName()).thenReturn("header name");
        when(appProperties.getWebsocketHeaderValue()).thenReturn("header value");
        when(appProperties.getWebSocketRetryInterval()).thenReturn(10);

        assertDoesNotThrow(() -> config.socketClient());
    }
}