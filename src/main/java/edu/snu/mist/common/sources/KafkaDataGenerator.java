/*
 * Copyright (C) 2016 Seoul National University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.snu.mist.common.sources;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class receives data stream via Kafka.
 * @param <K> the type of kafka record's key
 * @param <V> the type of kafka record's value
 */
public final class KafkaDataGenerator<K, V> implements DataGenerator<ConsumerRecord<K, V>> {

  /**
   * A flag for start.
   */
  private final AtomicBoolean started;

  /**
   * A flag for close.
   */
  private final AtomicBoolean closed;

  /**
   * The kafka topic to monitor.
   */
  private final String topic;

  /**
   * The timeout for consumer polling represented in milliseconds.
   */
  private final int pollTimeout;

  /**
   * The KafkaConsumer configuration.
   */
  private final Map<String, Object> kafkaConsumerConf;

  /**
   * The actual KafkaConsumer which subscribes the target topic.
   */
  private KafkaConsumer<K, V> consumer;

  /**
   * The executor service used to restrict the number of threads for kafka sources.
   */
  private final ExecutorService executorService;

  /**
   * Event generator which is the destination of fetched data.
   */
  private EventGenerator<ConsumerRecord<K, V>> eventGenerator;

  public KafkaDataGenerator(final String topic,
                            final Map<String, Object> kafkaConsumerConf,
                            final ExecutorService executorService,
                            final int pollTimeout) {
    this.started = new AtomicBoolean(false);
    this.closed = new AtomicBoolean(false);
    this.topic = topic;
    this.kafkaConsumerConf = kafkaConsumerConf;
    this.executorService = executorService;
    this.pollTimeout = pollTimeout;
  }

  @Override
  public void start() {
    if (started.compareAndSet(false, true)) {
      if (eventGenerator != null) {
        try {
          // TODO: [MIST-355] support topic having multiple partitions in kafka source
          consumer = new KafkaConsumer<>(kafkaConsumerConf);
          final Collection<String> topicCollection = new LinkedList<>();
          topicCollection.add(topic);
          consumer.subscribe(topicCollection);

          executorService.submit(new Runnable() {
            @Override
            public void run() {
              try {
                while(!closed.get()) {
                  final ConsumerRecords<K, V> consumerRecords = consumer.poll(pollTimeout);
                  for (final ConsumerRecord<K, V> record : consumerRecords) {
                    eventGenerator.emitData(record);
                  }
                }
              } catch (final Exception e) {
                e.printStackTrace();
              } finally {
                consumer.close();
              }
            }
          });
        } catch (final Exception e) {
          e.printStackTrace();
          throw new RuntimeException("Failed to construct kafka consumer", e);
        }
      }
    }
  }

  @Override
  public void close() {
    closed.compareAndSet(false, true);
  }

  @Override
  public void setEventGenerator(final EventGenerator eventGenerator) {
    this.eventGenerator = eventGenerator;
  }
}