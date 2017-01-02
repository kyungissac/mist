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
package edu.snu.mist.api.datastreams;

import edu.snu.mist.api.AvroVertexSerializable;
import edu.snu.mist.common.functions.ApplyStatefulFunction;
import edu.snu.mist.common.functions.MISTBiFunction;
import edu.snu.mist.common.functions.MISTFunction;
import edu.snu.mist.common.windows.WindowData;
import edu.snu.mist.common.DAG;
import edu.snu.mist.formats.avro.Direction;
import edu.snu.mist.formats.avro.Vertex;
import edu.snu.mist.formats.avro.VertexTypeEnum;
import edu.snu.mist.formats.avro.WindowOperatorInfo;

/**
 * The abstract class for describing WindowedStream.
 */
public abstract class WindowedStreamImpl<T> extends MISTStreamImpl<WindowData<T>> implements WindowedStream<T>  {


  protected WindowedStreamImpl(final DAG<AvroVertexSerializable, Direction> dag) {
    super(dag);
  }

  @Override
  public <K, V> ReduceByKeyWindowOperatorStream<T, K, V> reduceByKeyWindow(
      final int keyFieldNum,
      final Class<K> keyType,
      final MISTBiFunction<V, V, V> reduceFunc) {
    final ReduceByKeyWindowOperatorStream<T, K, V> downStream =
        new ReduceByKeyWindowOperatorStream<>(keyFieldNum, keyType, reduceFunc, dag);
    dag.addVertex(downStream);
    dag.addEdge(this, downStream, Direction.LEFT);
    return downStream;
  }

  @Override
  public <R> AggregateWindowOperatorStream<T, R> aggregateWindow(final MISTFunction<WindowData<T>, R> aggregateFunc) {
    final AggregateWindowOperatorStream<T, R> downStream =
        new AggregateWindowOperatorStream<>(aggregateFunc, dag);
    dag.addVertex(downStream);
    dag.addEdge(this, downStream, Direction.LEFT);
    return downStream;
  }

  @Override
  public <R> ApplyStatefulWindowOperatorStream<T, R> applyStatefulWindow(
      final ApplyStatefulFunction<T, R> applyStatefulFunction) {
    final ApplyStatefulWindowOperatorStream<T, R> downStream =
        new ApplyStatefulWindowOperatorStream<>(applyStatefulFunction, dag);
    dag.addVertex(downStream);
    dag.addEdge(this, downStream, Direction.LEFT);
    return downStream;
  }

  /**
   * @return the window operator info.
   */
  protected abstract WindowOperatorInfo getWindowOpInfo();

  @Override
  public Vertex getSerializedVertex() {
    final Vertex.Builder vertexBuilder = Vertex.newBuilder();
    vertexBuilder.setVertexType(VertexTypeEnum.WINDOW_OPERATOR);
    vertexBuilder.setAttributes(getWindowOpInfo());
    return vertexBuilder.build();
  }
}