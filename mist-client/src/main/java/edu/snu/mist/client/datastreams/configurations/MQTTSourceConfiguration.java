/*
 * Copyright (C) 2018 Seoul National University
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
package edu.snu.mist.client.datastreams.configurations;

import edu.snu.mist.common.SerializeUtils;
import edu.snu.mist.common.functions.MISTFunction;
import edu.snu.mist.common.parameters.MQTTBrokerURI;
import edu.snu.mist.common.parameters.MQTTTopic;
import edu.snu.mist.common.parameters.SerializedTimestampExtractUdf;
import org.apache.reef.io.Tuple;
import org.apache.reef.tang.Configuration;
import org.apache.reef.tang.Configurations;
import org.apache.reef.tang.formats.*;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.IOException;

/**
 * Configuration class for setting up MQTT sources.
 * This class is agnostic to which broker implementation the source uses.
 */
public final class MQTTSourceConfiguration extends ConfigurationModuleBuilder {

  /**
   * The parameter for MQTT broker URI.
   */
  public static final RequiredParameter<String> MQTT_BROKER_URI = new RequiredParameter<>();

  /**
   * The parameter for MQTT topic name.
   */
  public static final RequiredParameter<String> MQTT_TOPIC = new RequiredParameter<>();

  /**
   * The parameter for timestamp extract object.
   */
  public static final OptionalParameter<String> TIMESTAMP_EXTRACT_OBJECT = new OptionalParameter<>();

  /**
   * The parameter for timestamp extract function.
   */
  public static final OptionalImpl<MISTFunction> TIMESTAMP_EXTRACT_FUNC = new OptionalImpl<>();

  private static final ConfigurationModule CONF = new MQTTSourceConfiguration()
      .bindNamedParameter(MQTTBrokerURI.class, MQTT_BROKER_URI)
      .bindNamedParameter(MQTTTopic.class, MQTT_TOPIC)
      .bindNamedParameter(SerializedTimestampExtractUdf.class, TIMESTAMP_EXTRACT_OBJECT)
      .bindImplementation(MISTFunction.class, TIMESTAMP_EXTRACT_FUNC)
      .build();

  /**
   * Gets teh builder for Configuration construction.
   * @return the builder
   */
  public static MQTTSourceConfigurationBuilder newBuilder() {
    return new MQTTSourceConfigurationBuilder();
  }

  /**
   * A builder class for making source configuration for MQTT connections.
   */
  public static final class MQTTSourceConfigurationBuilder {

    private String mqttBrokerURI;
    private String mqttTopic;
    private MISTFunction<MqttMessage, Tuple<MqttMessage, Long>> extractFunc;
    private Class<? extends MISTFunction<MqttMessage, Tuple<MqttMessage, Long>>> extractFuncClass;
    private Configuration extractFuncConf;

    public SourceConfiguration build() {

      if (extractFunc != null && extractFuncClass != null) {
        throw new IllegalArgumentException("Cannot bind both extractFunc and extractFuncClass");
      }

      if (mqttBrokerURI == null || mqttTopic == null) {
        throw new IllegalArgumentException("MQTT broker URI and topic name should be set");
      }

      try {
        if (extractFunc == null && extractFuncClass == null) {
          return new SourceConfiguration(CONF
              .set(MQTT_BROKER_URI, mqttBrokerURI)
              .set(MQTT_TOPIC, mqttTopic)
              .build(), SourceConfiguration.SourceType.MQTT);
        } else if (extractFunc != null && extractFuncClass == null) {
          return new SourceConfiguration(CONF
              .set(MQTT_BROKER_URI, mqttBrokerURI)
              .set(MQTT_TOPIC, mqttTopic)
              .set(TIMESTAMP_EXTRACT_OBJECT, SerializeUtils.serializeToString(extractFunc))
              .build(), SourceConfiguration.SourceType.MQTT);
        } else {
          return new SourceConfiguration(Configurations.merge(CONF
              .set(MQTT_BROKER_URI, mqttBrokerURI)
              .set(MQTT_TOPIC, mqttTopic)
              .set(TIMESTAMP_EXTRACT_FUNC, extractFuncClass)
              .build(), extractFuncConf), SourceConfiguration.SourceType.MQTT);
        }
      } catch (final IOException e) {
        e.printStackTrace();
        throw new RuntimeException(e);
      }
    }

    /**
     * Sets the broker URI with specified port.
     * @param mqttBrokerURIParam the mqtt broker URI given by users
     * @return builder
     */
    public MQTTSourceConfigurationBuilder setBrokerURI(final String mqttBrokerURIParam) {
     this.mqttBrokerURI = mqttBrokerURIParam;
      return this;
    }

    /**
     * Sets the mqtt topic.
     * @param mqttTopicParam the mqtt topic given by user
     * @return builder
     */
    public MQTTSourceConfigurationBuilder setTopic(final String mqttTopicParam) {
      this.mqttTopic = mqttTopicParam;
      return this;
    }

    /**
     * Sets the configuration for the extracting timestamp in event-time input data function to the given function.
     * This is an optional setting for event-time processing.
     * @param function the function given by users which they want to set
     * @return the configured SourceBuilder
     */
    public MQTTSourceConfigurationBuilder setTimestampExtractionFunction(
        final MISTFunction<MqttMessage, Tuple<MqttMessage, Long>> function) {
      this.extractFunc = function;
      return this;
    }

    /**
     * Sets the timestamp extract function with its class and configuration.
     * This is an optional setting for event-time processing.
     * @param functionClass the class of the timestamp extract function
     * @param functionConf the configuration of the extract function
     * @return the configured SourceBuilder
     */
    public MQTTSourceConfigurationBuilder setTimestampExtractionFunction(
        final Class<? extends MISTFunction<MqttMessage, Tuple<MqttMessage, Long>>> functionClass,
        final Configuration functionConf) {
      this.extractFuncClass = functionClass;
      this.extractFuncConf = functionConf;
      return this;
    }
  }
}