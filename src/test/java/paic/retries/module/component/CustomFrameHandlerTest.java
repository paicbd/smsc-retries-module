package paic.retries.module.component;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.stomp.StompHeaders;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static paic.retries.module.component.CustomFrameHandler.UPDATE_RETRY_PARAMS;

@ExtendWith(MockitoExtension.class)
class CustomFrameHandlerTest {
    @Mock
    RetryParams retryParams;

    @Mock
    StompHeaders stompHeaders;

    @InjectMocks
    CustomFrameHandler customFrameHandler;

    @Test
    @DisplayName("Update retry params")
    void handleFrameLogicWhenUpdateRetryParamsThenLoadParams() {
        String payload = "updated";
        when(stompHeaders.getDestination()).thenReturn(UPDATE_RETRY_PARAMS);
        customFrameHandler.handleFrameLogic(stompHeaders, payload);
        verify(retryParams).init();
    }

    @Test
    @DisplayName("Destination not equals to UPDATE_RETRY_PARAMS")
    void handleFrameLogicWhenUnknownDestinationThenDontExecuteAction() {
        String payload = "updated";
        when(stompHeaders.getDestination()).thenReturn("UNKNOWN");
        customFrameHandler.handleFrameLogic(stompHeaders, payload);
        verify(retryParams, never()).init();
    }

}