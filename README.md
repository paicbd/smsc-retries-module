# Retries Module

The `retries-module` service is responsible for managing the retry logic for undelivered or failed SMS messages in the SMSC environment. It handles retries across multiple messaging protocols, including SMPP, HTTP, and SS7. The service leverages Redis for storing retry information and provides WebSocket support for real-time monitoring. With its configurable retry intervals and protocol-specific queue handling, this module helps ensure reliable message delivery by retrying failed messages at specified intervals.

## Main Responsibilities

- **Message Retry Handling**: Manages retry logic for messages that fail to be delivered across multiple protocols.
- **Protocol Support**: Handles different message queues for SMPP, HTTP, and SS7.
- **Redis Integration**: Uses Redis for storing message state and retry information.
- **WebSocket Support**: Provides a WebSocket server for real-time communication and monitoring of retry processes.
- **JMX Monitoring**: Enables monitoring via JMX (Java Management Extensions).

## Key Configurable Variables

### JVM Settings
- `JVM_XMS`: Minimum heap size for the Java Virtual Machine. Default: `512 MB` (`-Xms512m`).
- `JVM_XMX`: Maximum heap size for the JVM. Default: `1024 MB` (`-Xmx1024m`).

### Redis Cluster Configuration
- `CLUSTER_NODES`: A list of Redis cluster nodes. Default: `"localhost:7000,localhost:7001,localhost:7002,localhost:7003,localhost:7004,localhost:7005,localhost:7006,localhost:7007,localhost:7008,localhost:7009"`.

### Thread Pool Settings
- `THREAD_POOL_MAX_TOTAL`: Maximum number of threads allowed in the pool. Default: `20`.
- `THREAD_POOL_MAX_IDLE`: Maximum number of idle threads in the pool. Default: `20`.
- `THREAD_POOL_MIN_IDLE`: Minimum number of idle threads in the pool. Default: `1`.
- `THREAD_POOL_BLOCK_WHEN_EXHAUSTED`: Blocks further operations when the thread pool is exhausted. Default: `true`.

### WebSocket Server Configuration
- `WEBSOCKET_SERVER_ENABLED`: Enables the WebSocket server. Default: `true`.
- `WEBSOCKET_SERVER_HOST`: Host for the WebSocket server. Default: `{HOST_IP_ADDRESS}`.
- `WEBSOCKET_SERVER_PORT`: Port for the WebSocket server. Default: `9087`.
- `WEBSOCKET_SERVER_PATH`: Path for the WebSocket server. Default: `/ws`.
- `WEBSOCKET_SERVER_RETRY_INTERVAL`: Retry interval for WebSocket connections (in seconds). Default: `10`.
- `WEBSOCKET_HEADER_NAME`: Name of the WebSocket authorization header. Default: `Authorization`.
- `WEBSOCKET_HEADER_VALUE`: Value for the WebSocket authorization header. Default: `{WEBSOCKET_HEADER_VALUE}`.

### Retry Processing Configuration
- `RETRY_MESSAGES_QUEUE`: Name of the queue for retrying messages. Default: `sms_retry`.
- `RETRY_INTERVAL`: Time interval between retries (in milliseconds). Default: `1000 ms`.
- `PROCESSOR_INTERVAL`: Interval for processing retry logic (in milliseconds). Default: `1000 ms`.

### Protocol-Specific Queue Names
- `SMPP_QUEUE_NAME`: Queue name for SMPP messages. Default: `smpp_message`.
- `HTTP_QUEUE_NAME`: Queue name for HTTP messages. Default: `http_message`.
- `SS7_QUEUE_NAME`: Queue name for SS7 messages. Default: `ss7_message`.
- `DIAMETER_QUEUE_NAME`: Queue name for Diameter messages. Default: `diameter_message`.

### SS7 Specific Settings
- `SS7_HASH_TABLE_RETRY`: Hash table for retrying failed SS7 messages (e.g., absent subscribers). Default: `msisdn_absent_subscriber`.

### JMX Configuration
- `ENABLE_JMX`: Enables JMX monitoring. Default: `true`.
- `IP_JMX`: IP address for JMX monitoring. Default: `127.0.0.1`.
- `JMX_PORT`: Port for JMX monitoring. Default: `9010`.

## Docker Compose Example

```yaml
services:
  retries-module:
    image: paic/retries-module:latest
    ulimits:
      nofile:
        soft: 1000000
        hard: 1000000
    environment:
      JVM_XMS: "-Xms512m"
      JVM_XMX: "-Xmx1024m"
      SERVER_PORT: 9898
      APPLICATION_NAME: "module"
      # Redis Configuration
      CLUSTER_NODES: "localhost:7000,localhost:7001,localhost:7002,localhost:7003,localhost:7004,localhost:7005,localhost:7006,localhost:7007,localhost:7008,localhost:7009"
      THREAD_POOL_MAX_TOTAL: 20
      THREAD_POOL_MAX_IDLE: 20
      THREAD_POOL_MIN_IDLE: 1
      THREAD_POOL_BLOCK_WHEN_EXHAUSTED: true
      # WebSocket Configuration
      WEBSOCKET_SERVER_ENABLED: true
      WEBSOCKET_SERVER_HOST: "{HOST_IP_ADDRESS}"
      WEBSOCKET_SERVER_PORT: 9087
      WEBSOCKET_SERVER_PATH: "/ws"
      WEBSOCKET_SERVER_RETRY_INTERVAL: 10
      WEBSOCKET_HEADER_NAME: "Authorization"
      WEBSOCKET_HEADER_VALUE: "{WEBSOCKET_HEADER_VALUE}"
      # Retry Processing
      RETRY_MESSAGES_QUEUE: "sms_retry"
      RETRY_INTERVAL: 1000
      PROCESSOR_INTERVAL: 1000
      # Protocol-Specific Queue Names
      SMPP_QUEUE_NAME: "smpp_message"
      HTTP_QUEUE_NAME: "http_message"
      SS7_QUEUE_NAME: "ss7_message"
      DIAMETER_QUEUE_NAME: "diameter_message"
      # SS7 Retry Table
      SS7_HASH_TABLE_RETRY: "msisdn_absent_subscriber"
      # JMX Configuration
      ENABLE_JMX: "true"
      IP_JMX: "127.0.0.1"
      JMX_PORT: "9010"
    volumes:
      - /opt/paic/smsc-docker/retries/smsc-retries-module-docker/resources/conf/logback.xml:/opt/paic/SMSC_RETRIES_MODULE/conf/logback.xml
    network_mode: host
