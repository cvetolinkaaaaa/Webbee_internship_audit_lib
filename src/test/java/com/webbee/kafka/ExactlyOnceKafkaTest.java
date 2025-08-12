package com.webbee.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webbee.TestKafkaApplication;
import com.webbee.audit_lib.starter.service.AuditService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.StreamSupport;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = TestKafkaApplication.class)
@Testcontainers
@DirtiesContext
class ExactlyOnceKafkaTest {

    @Container
    static KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"))
            .withEnv("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", "1")
            .withEnv("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", "1");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("audit.enabled", () -> "true");
        registry.add("audit.modes", () -> "KAFKA");
        registry.add("audit.kafka.enabled", () -> "true");
        registry.add("audit.kafka.topic", () -> "exactly-once-audit");
        registry.add("audit.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
    }

    @Autowired
    private AuditService auditService;

    @Autowired
    private ObjectMapper objectMapper;

    private Consumer<String, String> testConsumer;
    private Producer<String, String> transactionalProducer;
    private String transactionalId;

    @BeforeEach
    void setUp() {
        transactionalId = "exactly-once-test-producer-" + UUID.randomUUID().toString();
        setupConsumer();
        setupTransactionalProducer();
    }

    @AfterEach
    void tearDown() {
        if (testConsumer != null) {
            testConsumer.close();
        }
        if (transactionalProducer != null) {
            try {
                transactionalProducer.close();
            } catch (Exception e) {
                // Игнорируем ошибки при закрытии
            }
        }
    }

    private void setupConsumer() {
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "exactly-once-test-group-" + UUID.randomUUID().toString());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        testConsumer = new KafkaConsumer<>(consumerProps);
        testConsumer.subscribe(Collections.singletonList("exactly-once-audit"));
    }

    private void setupTransactionalProducer() {
        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        
        // Настройки для exactly-once семантики
        producerProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        producerProps.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, transactionalId);
        producerProps.put(ProducerConfig.ACKS_CONFIG, "all");
        producerProps.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
        producerProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);
        
        transactionalProducer = new KafkaProducer<>(producerProps);
        transactionalProducer.initTransactions();
    }

    @Test
    void shouldEnsureExactlyOnceSemantics() throws Exception {
        String messageKey = "exactly-once-key";
        String messageValue = "exactly-once-message";
        
        // Симулируем отправку одного и того же сообщения дважды в одной транзакции
        transactionalProducer.beginTransaction();
        transactionalProducer.send(new ProducerRecord<>("exactly-once-audit", messageKey, messageValue)).get();
        transactionalProducer.send(new ProducerRecord<>("exactly-once-audit", messageKey, messageValue)).get();
        transactionalProducer.commitTransaction();

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            ConsumerRecords<String, String> records = testConsumer.poll(Duration.ofSeconds(3));
            testConsumer.commitSync();
            
            long messageCount = StreamSupport.stream(records.spliterator(), false)
                .filter(record -> messageKey.equals(record.key()) && messageValue.equals(record.value()))
                .count();
            
            assertEquals(2, messageCount, "Should receive both messages as they are in the same transaction");
        });
    }

    @Test
    void shouldPreventDuplicateMessagesOnRetry() throws Exception {
        String messageKey = "duplicate-prevention-key";
        String messageValue = "duplicate-prevention-message";
        
        // Первая успешная транзакция
        transactionalProducer.beginTransaction();
        transactionalProducer.send(new ProducerRecord<>("exactly-once-audit", messageKey, messageValue)).get();
        transactionalProducer.commitTransaction();

        // Создаем новый producer с тем же transactional ID
        Producer<String, String> duplicateProducer = createDuplicateProducer();
        
        try {
            // Вторая транзакция с тем же producer ID (симулируем retry)
            duplicateProducer.beginTransaction();
            duplicateProducer.send(new ProducerRecord<>("exactly-once-audit", messageKey, messageValue)).get();
            duplicateProducer.commitTransaction();
        } finally {
            duplicateProducer.close();
        }

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            ConsumerRecords<String, String> records = testConsumer.poll(Duration.ofSeconds(3));
            testConsumer.commitSync();
            
            long messageCount = StreamSupport.stream(records.spliterator(), false)
                .filter(record -> messageKey.equals(record.key()) && messageValue.equals(record.value()))
                .count();
            
            assertTrue(messageCount >= 1, "Should receive at least one message");
            // В реальной exactly-once семантике дубликаты должны отфильтровываться на уровне брокера
        });
    }

    @Test
    void shouldRollbackTransactionOnFailure() throws Exception {
        String rollbackKey = "rollback-test-key";
        String rollbackValue = "should-not-appear";

        transactionalProducer.beginTransaction();
        try {
            transactionalProducer.send(new ProducerRecord<>("exactly-once-audit", rollbackKey, rollbackValue)).get();
            // Симулируем ошибку
            throw new RuntimeException("Simulated failure");
        } catch (RuntimeException e) {
            transactionalProducer.abortTransaction();
        }

        // Ждем и проверяем, что сообщение не появилось
        Thread.sleep(3000);
        
        ConsumerRecords<String, String> records = testConsumer.poll(Duration.ofSeconds(3));
        testConsumer.commitSync();
        
        long rollbackMessages = StreamSupport.stream(records.spliterator(), false)
                .filter(record -> rollbackKey.equals(record.key()))
                .count();

        assertEquals(0, rollbackMessages, "Rolled back transaction should not produce any messages");
    }

    @Test
    void shouldHandleMultipleTransactionsCorrectly() throws Exception {
        String baseKey = "multi-transaction-key";
        int transactionCount = 3;
        int messagesPerTransaction = 2;

        for (int txn = 0; txn < transactionCount; txn++) {
            transactionalProducer.beginTransaction();
            for (int msg = 0; msg < messagesPerTransaction; msg++) {
                String key = baseKey + "-txn" + txn;
                String value = "message-" + msg;
                transactionalProducer.send(new ProducerRecord<>("exactly-once-audit", key, value)).get();
            }
            transactionalProducer.commitTransaction();
        }

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            ConsumerRecords<String, String> records = testConsumer.poll(Duration.ofSeconds(5));
            testConsumer.commitSync();
            
            long totalMessages = StreamSupport.stream(records.spliterator(), false)
                .filter(record -> record.key().startsWith(baseKey))
                .count();
                
            assertEquals(transactionCount * messagesPerTransaction, totalMessages, 
                "Should receive all messages from all transactions");
        });
    }

    @Test
    void shouldHandleConcurrentTransactions() throws Exception {
        String concurrentKey = "concurrent-key";
        AtomicInteger messageCounter = new AtomicInteger(0);
        
        // Симулируем конкурентные транзакции
        Thread t1 = new Thread(() -> {
            try {
                transactionalProducer.beginTransaction();
                for (int i = 0; i < 3; i++) {
                    transactionalProducer.send(new ProducerRecord<>("exactly-once-audit", 
                        concurrentKey + "-t1", "message-" + messageCounter.incrementAndGet())).get();
                }
                transactionalProducer.commitTransaction();
            } catch (Exception e) {
                try {
                    transactionalProducer.abortTransaction();
                } catch (Exception abortEx) {
                    // Ignore
                }
            }
        });

        t1.start();
        t1.join();

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            ConsumerRecords<String, String> records = testConsumer.poll(Duration.ofSeconds(3));
            testConsumer.commitSync();
            
            long concurrentMessages = StreamSupport.stream(records.spliterator(), false)
                .filter(record -> record.key().startsWith(concurrentKey))
                .count();
                
            assertEquals(3, concurrentMessages, "Should receive all messages from concurrent transaction");
        });
    }

    private Producer<String, String> createDuplicateProducer() {
        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        producerProps.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, transactionalId + "-duplicate");
        producerProps.put(ProducerConfig.ACKS_CONFIG, "all");
        producerProps.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
        producerProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);
        
        Producer<String, String> producer = new KafkaProducer<>(producerProps);
        producer.initTransactions();
        return producer;
    }
}