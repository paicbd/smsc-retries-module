package paic.retries.module.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.stomp.StompHeaders;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;
import static paic.retries.module.config.CustomFrameHandler.UPDATE_RETRY_PARAMS;

@ExtendWith(MockitoExtension.class)
class CustomFrameHandlerTest {
    @Mock(strictness = Mock.Strictness.LENIENT)
    RetryParams retryParams;
    @Mock
    private StompHeaders stompHeaders;
    CustomFrameHandler customFrameHandler;

    @BeforeEach
    void setUp() {
        this.customFrameHandler = new CustomFrameHandler(retryParams);
    }

    @Test
    void handleFrameLogic() {
        String payload = "systemId123";
        when(stompHeaders.getDestination()).thenReturn(UPDATE_RETRY_PARAMS);
        assertDoesNotThrow(() -> customFrameHandler.handleFrameLogic(stompHeaders, payload));
    }

    @Test
    void destinationNotEquals() {
        String payload = "systemId123";
        when(stompHeaders.getDestination()).thenReturn("UNKNOWN");
        assertDoesNotThrow(() -> customFrameHandler.handleFrameLogic(stompHeaders, payload));
    }
}