package paic.retries.module.config;

import com.paicbd.smsc.ws.FrameHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomFrameHandler implements FrameHandler {
    private final RetryParams retryParams;

    public static final String UPDATE_RETRY_PARAMS = "/app/generalSmsRetry";

    @Override
    public void handleFrameLogic(StompHeaders headers, Object payload) {
        String destination = headers.getDestination();
        Objects.requireNonNull(destination, "Destination cannot be null");
        log.info("From websocket server: {}", payload);
        if (destination.equals(UPDATE_RETRY_PARAMS)) {
            log.info("Updating retry params {}", payload);
            this.retryParams.init();
        } else {
            log.warn("Unknown destination: {}", destination);
        }
    }
}
