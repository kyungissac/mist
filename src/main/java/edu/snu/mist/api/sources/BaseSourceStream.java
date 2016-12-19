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
package edu.snu.mist.api.sources;

import edu.snu.mist.api.AvroVertexSerializable;
import edu.snu.mist.api.ContinuousStreamImpl;
import edu.snu.mist.api.SerializedType;
import edu.snu.mist.api.StreamType;
import edu.snu.mist.api.sources.builder.SourceConfiguration;
import edu.snu.mist.api.sources.builder.WatermarkConfiguration;
import edu.snu.mist.api.sources.parameters.SourceSerializeInfo;
import edu.snu.mist.common.DAG;
import edu.snu.mist.formats.avro.*;
import org.apache.commons.lang.SerializationUtils;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Stream interface for streams created from various stream sources.
 */
public abstract class BaseSourceStream<T> extends ContinuousStreamImpl<T> {
  /**
   * The value for source configuration.
   */
  private final SourceConfiguration<T> sourceConfiguration;

  /**
   * The type of this source.
   */
  private final StreamType.SourceType sourceType;

  /**
   * The value for watermark configuration.
   */
  private final WatermarkConfiguration watermarkConfiguration;

  BaseSourceStream(final StreamType.SourceType sourceType,
                   final SourceConfiguration<T> sourceConfiguration,
                   final DAG<AvroVertexSerializable, StreamType.Direction> dag,
                   final WatermarkConfiguration<T> watermarkConfiguration) {
    super(StreamType.ContinuousType.SOURCE, dag);
    this.sourceType = sourceType;
    this.sourceConfiguration = sourceConfiguration;
    this.watermarkConfiguration = watermarkConfiguration;
  }

  protected abstract SourceTypeEnum getSourceTypeEnum();

  /**
   * Gets the type enum of watermark.
   * @return the type enum of watermark
   */
  private WatermarkTypeEnum getWatermarkTypeEnum() {
    if (watermarkConfiguration.getWatermarkType() == StreamType.WatermarkType.PERIODIC) {
      return WatermarkTypeEnum.PERIODIC;
    } else {
      return WatermarkTypeEnum.PUNCTUATED;
    }
  }

  @Override
  public Vertex getSerializedVertex() {
    final Vertex.Builder vertexBuilder = Vertex.newBuilder();
    vertexBuilder.setVertexType(VertexTypeEnum.SOURCE);
    final SourceInfo.Builder sourceInfoBuilder = SourceInfo.newBuilder();
    sourceInfoBuilder.setSourceType(getSourceTypeEnum());
    sourceInfoBuilder.setWatermarkType(getWatermarkTypeEnum());
    // Serialize SourceConfiguration in SourceInfo
    final Map<String, Object> serializedSourceConf = new HashMap<>();
    for (final String confKey: sourceConfiguration.getConfigurationKeys()) {
      final Object value = sourceConfiguration.getConfigurationValue(confKey);
      if (SourceSerializeInfo.getAvroSerializedTypeInfo(confKey) != SerializedType.AvroType.BYTES) {
        serializedSourceConf.put(confKey, value);
      } else {
        serializedSourceConf.put(confKey, ByteBuffer.wrap(SerializationUtils.serialize((Serializable) value)));
      }
    }
    // Serialize WatermarkConfiguration in SourceInfo
    final Map<String, Object> serializedWatermarkConf = new HashMap<>();
    for (final String confKey: watermarkConfiguration.getConfigurationKeys()) {
      final Object value = watermarkConfiguration.getConfigurationValue(confKey);
      if (SourceSerializeInfo.getAvroSerializedTypeInfo(confKey) != SerializedType.AvroType.BYTES) {
        serializedWatermarkConf.put(confKey, value);
      } else {
        serializedWatermarkConf.put(confKey, ByteBuffer.wrap(SerializationUtils.serialize((Serializable) value)));
      }
    }
    sourceInfoBuilder.setSourceConfiguration(serializedSourceConf);
    sourceInfoBuilder.setWatermarkConfiguration(serializedWatermarkConf);
    vertexBuilder.setAttributes(sourceInfoBuilder.build());
    return vertexBuilder.build();
  }
}