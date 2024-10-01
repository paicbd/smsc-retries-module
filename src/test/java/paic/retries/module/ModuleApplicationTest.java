package paic.retries.module;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import paic.retries.module.config.CustomFrameHandler;
import paic.retries.module.config.RetryParams;
import redis.clients.jedis.JedisCluster;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = ModuleApplication.class)
class ModuleApplicationTest {
    @MockBean
    RetryParams retryParams;
    @MockBean
    JedisCluster jedisCluster;
    @MockBean
    CustomFrameHandler customFrameHandler;

    @Test
    void contextLoads() {
        // This test will simply load the application context
    }
}