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
package edu.snu.mist.task;

import edu.snu.mist.api.sink.parameters.TextSocketSinkParameters;
import edu.snu.mist.api.sources.parameters.TextSocketSourceParameters;
import edu.snu.mist.common.AdjacentListDAG;
import edu.snu.mist.common.DAG;
import edu.snu.mist.common.ExternalJarObjectInputStream;
import edu.snu.mist.common.parameters.QueryId;
import edu.snu.mist.formats.avro.*;
import edu.snu.mist.task.common.parameters.SocketServerIp;
import edu.snu.mist.task.common.parameters.SocketServerPort;
import edu.snu.mist.task.operators.*;
import edu.snu.mist.task.operators.parameters.KeyIndex;
import edu.snu.mist.task.operators.parameters.OperatorId;
import edu.snu.mist.task.sinks.Sink;
import edu.snu.mist.task.sinks.TextSocketSink;
import edu.snu.mist.task.sinks.parameters.SinkId;
import edu.snu.mist.task.sources.Source;
import edu.snu.mist.task.sources.TextSocketSource;
import edu.snu.mist.task.sources.parameters.SourceId;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.SerializationUtils;
import org.apache.reef.io.Tuple;
import org.apache.reef.tang.Injector;
import org.apache.reef.tang.JavaConfigurationBuilder;
import org.apache.reef.tang.Tang;
import org.apache.reef.tang.exceptions.InjectionException;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A default implementation of PhysicalPlanGenerator.
 */
final class DefaultPhysicalPlanGeneratorImpl implements PhysicalPlanGenerator {

  private static final Logger LOG = Logger.getLogger(DefaultPhysicalPlanGeneratorImpl.class.getName());

  private final OperatorIdGenerator operatorIdGenerator;

  @Inject
  private DefaultPhysicalPlanGeneratorImpl(final OperatorIdGenerator operatorIdGenerator) {
    this.operatorIdGenerator = operatorIdGenerator;
  }

  /*
   * This private method makes a TextSocketSource from a source configuration.
   */
  private TextSocketSource getTextSocketSource(final String queryId,
                                               final Map<CharSequence, Object> sourceConf)
    throws IllegalArgumentException, InjectionException {
    final Map<String, Object> sourceConfString = new HashMap<>();
    for (final CharSequence charSeqKey : sourceConf.keySet()) {
      sourceConfString.put(charSeqKey.toString(), sourceConf.get(charSeqKey));
    }
    final JavaConfigurationBuilder cb = Tang.Factory.getTang().newConfigurationBuilder();
    final String socketHostAddress = sourceConfString.get(TextSocketSourceParameters.SOCKET_HOST_ADDRESS).toString();
    final String socketHostPort = sourceConfString.get(TextSocketSourceParameters.SOCKET_HOST_PORT).toString();

    cb.bindNamedParameter(SocketServerIp.class, socketHostAddress);
    cb.bindNamedParameter(SocketServerPort.class, socketHostPort);
    cb.bindNamedParameter(QueryId.class, queryId);
    cb.bindNamedParameter(SourceId.class, operatorIdGenerator.generate());
    return Tang.Factory.getTang().newInjector(cb.build()).getInstance(TextSocketSource.class);
  }

  /*
   * This private method makes a TextSocketSink from a sink configuration.
   */
  private TextSocketSink getTextSocketSink(final String queryId, final Map<CharSequence, Object> sinkConf)
    throws IllegalArgumentException, InjectionException {
    final Map<String, Object> sinkConfString = new HashMap<>();
    for (final CharSequence charSeqKey : sinkConf.keySet()) {
      sinkConfString.put(charSeqKey.toString(), sinkConf.get(charSeqKey));
    }
    final JavaConfigurationBuilder cb = Tang.Factory.getTang().newConfigurationBuilder();
    final String socketHostAddress = sinkConfString.get(TextSocketSinkParameters.SOCKET_HOST_ADDRESS).toString();
    final String socketHostPort = sinkConfString.get(TextSocketSinkParameters.SOCKET_HOST_PORT).toString();
    cb.bindNamedParameter(SocketServerIp.class, socketHostAddress);
    cb.bindNamedParameter(SocketServerPort.class, socketHostPort);
    cb.bindNamedParameter(QueryId.class, queryId);
    cb.bindNamedParameter(SinkId.class, operatorIdGenerator.generate());
    return Tang.Factory.getTang().newInjector(cb.build()).getInstance(TextSocketSink.class);
  }

  /*
   * This private method de-serializes byte-serialized lambdas
   */
  private Object deserializeLambda(final ByteBuffer serializedLambda, final ClassLoader classLoader)
      throws IOException, ClassNotFoundException {
    byte[] serializedByteArray = new byte[serializedLambda.remaining()];
    serializedLambda.get(serializedByteArray);
    if (classLoader == null) {
      return SerializationUtils.deserialize(serializedByteArray);
    } else {
      ExternalJarObjectInputStream stream = new ExternalJarObjectInputStream(
          classLoader, serializedByteArray);
      return stream.readObject();
    }
  }

  /*
   * This private method gets instant operator from the serialized instant operator info.
   */
  private Operator getInstantOperator(final String queryId, final InstantOperatorInfo iOpInfo,
                                      final ClassLoader classLoader)
      throws IllegalArgumentException, InjectionException, IOException, ClassNotFoundException {
    final JavaConfigurationBuilder cb = Tang.Factory.getTang().newConfigurationBuilder();
    cb.bindNamedParameter(QueryId.class, queryId);
    cb.bindNamedParameter(OperatorId.class, operatorIdGenerator.generate());
    final List<ByteBuffer> functionList = iOpInfo.getFunctions();
    switch (iOpInfo.getInstantOperatorType()) {
      case APPLY_STATEFUL: {
        throw new IllegalArgumentException("MISTTask: ApplyStatefulOperator is currently not supported!");
      }
      case FILTER: {
        final Predicate predicate = (Predicate) deserializeLambda(functionList.get(0), classLoader);
        final Injector injector = Tang.Factory.getTang().newInjector(cb.build());
        injector.bindVolatileInstance(Predicate.class, predicate);
        return injector.getInstance(FilterOperator.class);
      }
      case FLAT_MAP: {
        final Function flatMapFunc = (Function) deserializeLambda(functionList.get(0), classLoader);
        final Injector injector = Tang.Factory.getTang().newInjector(cb.build());
        injector.bindVolatileInstance(Function.class, flatMapFunc);
        return injector.getInstance(FlatMapOperator.class);
      }
      case MAP: {
        final Function mapFunc = (Function) deserializeLambda(functionList.get(0), classLoader);
        final Injector injector = Tang.Factory.getTang().newInjector(cb.build());
        injector.bindVolatileInstance(Function.class, mapFunc);
        return injector.getInstance(MapOperator.class);
      }
      case REDUCE_BY_KEY: {
        cb.bindNamedParameter(KeyIndex.class, iOpInfo.getKeyIndex().toString());
        final BiFunction reduceFunc = (BiFunction) deserializeLambda(functionList.get(0), classLoader);
        final Injector injector = Tang.Factory.getTang().newInjector(cb.build());
        injector.bindVolatileInstance(BiFunction.class, reduceFunc);
        return injector.getInstance(ReduceByKeyOperator.class);
      }
      case REDUCE_BY_KEY_WINDOW: {
        throw new IllegalArgumentException("MISTTask: ReduceByKeyWindowOperator is currently not supported!");
      }
      default: {
        throw new IllegalArgumentException("MISTTask: Invalid InstantOperatorType detected!");
      }
    }
  }

  @Override
  public PhysicalPlan<Operator> generate(final Tuple<String, LogicalPlan> queryIdAndLogicalPlan)
      throws IllegalArgumentException, InjectionException {
    final String queryId = queryIdAndLogicalPlan.getKey();
    final LogicalPlan logicalPlan = queryIdAndLogicalPlan.getValue();
    final List<Object> deserializedVertices = new ArrayList<>();
    final Map<Source, Set<Operator>> sourceMap = new HashMap<>();
    final DAG<Operator> operators = new AdjacentListDAG<>();
    final Map<Operator, Set<Sink>> sinkMap = new HashMap<>();

    // Deserialize Jar
    final ClassLoader userQueryClassLoader;
    if (!queryIdAndLogicalPlan.getValue().getIsJarSerialized()) {
      userQueryClassLoader = null;
    } else {
      final String jarFilePath;
      jarFilePath = String.format("./%s.jar", queryId);
      final ByteBuffer byteBufferJar = queryIdAndLogicalPlan.getValue().getJar();
      final byte[] byteArrayJar = new byte[byteBufferJar.remaining()];
      byteBufferJar.get(byteArrayJar);
      try {
        FileUtils.writeByteArrayToFile(new File(jarFilePath), byteArrayJar);
        userQueryClassLoader = new URLClassLoader(new URL[]{new URL(jarFilePath)});
      } catch (IOException e) {
        LOG.log(Level.FINE, "Cannot save jar location ");
        return null;
      }
    }

    // Deserialize vertices
    for (final Vertex vertex : logicalPlan.getVertices()) {
      switch (vertex.getVertexType()) {
        case SOURCE: {
          final SourceInfo sourceInfo = (SourceInfo) vertex.getAttributes();
          switch (sourceInfo.getSourceType()) {
            case TEXT_SOCKET_SOURCE: {
              final TextSocketSource textSocketSource
                  = getTextSocketSource(queryId, sourceInfo.getSourceConfiguration());
              deserializedVertices.add(textSocketSource);
              break;
            }
            case REEF_NETWORK_SOURCE: {
              throw new IllegalArgumentException("MISTTask: REEF_NETWORK_SOURCE is currently not supported!");
            }
            default: {
              throw new IllegalArgumentException("MISTTask: Invalid source generator detected in LogicalPlan!");
            }
          }
          break;
        }
        case INSTANT_OPERATOR: {
          final InstantOperatorInfo iOpInfo = (InstantOperatorInfo) vertex.getAttributes();
          try {
            final Operator operator = getInstantOperator(queryId, iOpInfo, userQueryClassLoader);
            deserializedVertices.add(operator);
            operators.addVertex(operator);
            break;
          } catch (Exception e) {
            LOG.log(Level.FINE, e.toString());
            return null;
          }
        }
        case WINDOW_OPERATOR: {
          throw new IllegalArgumentException("MISTTask: WindowOperator is currently not supported!");
        }
        case SINK: {
          final SinkInfo sinkInfo = (SinkInfo) vertex.getAttributes();
          switch (sinkInfo.getSinkType()) {
            case TEXT_SOCKET_SINK: {
              final TextSocketSink textSocketSink = getTextSocketSink(queryId, sinkInfo.getSinkConfiguration());
              deserializedVertices.add(textSocketSink);
              break;
            }
            case REEF_NETWORK_SINK: {
              throw new IllegalArgumentException("MISTTask: REEF_NETWORK_SINK is currently not supported!");
            }
            default: {
              throw new IllegalArgumentException("MISTTask: Invalid sink detected in LogicalPlan!");
            }
          }
          break;
        }
        default: {
          throw new IllegalArgumentException("MISTTask: Invalid vertex detected in LogicalPlan!");
        }
      }
    }
    // Add edge info to physical plan
    for (final Edge edge : logicalPlan.getEdges()) {
      final int srcIndex = edge.getFrom();
      final Object deserializedSrcVertex = deserializedVertices.get(srcIndex);
      final int dstIndex = edge.getTo();
      final Object deserializedDstVertex = deserializedVertices.get(dstIndex);
      switch (logicalPlan.getVertices().get(srcIndex).getVertexType()) {
        case SOURCE: {
          if (!sourceMap.containsKey(deserializedSrcVertex)) {
            sourceMap.put((Source) deserializedSrcVertex, new HashSet<>());
          }
          sourceMap.get(deserializedSrcVertex).add((Operator) deserializedDstVertex);
          break;
        }
        case INSTANT_OPERATOR: {
          switch (logicalPlan.getVertices().get(dstIndex).getVertexType()) {
            case INSTANT_OPERATOR: {
              operators.addEdge((Operator) deserializedSrcVertex, (Operator) deserializedDstVertex);
              break;
            }
            case WINDOW_OPERATOR: {
              throw new IllegalStateException("MISTTask: WindowOperator is currently not supported but MIST didn't " +
                  "catch it in advance!");
            }
            case SINK: {
              if (!sinkMap.containsKey(deserializedSrcVertex)) {
                sinkMap.put((Operator) deserializedSrcVertex, new HashSet<>());
              }
              sinkMap.get(deserializedSrcVertex).add((Sink) deserializedDstVertex);
              break;
            }
            default: {
              // ToVertex type is Source, but it's illegal!
              throw new IllegalArgumentException("MISTTask: Invalid edge detected! Source cannot have" +
                  " ingoing edges!");
            }
          }
          break;
        }
        case WINDOW_OPERATOR: {
          throw new IllegalStateException("MISTTask: WindowOperator is currently not supported but MIST didn't catch" +
              " it in advance!");
        }
        default: {
          // FromVertex type is guaranteed to be Sink! However, Sink cannot have outgoing edges!
          throw new IllegalArgumentException("MISTTask: Invalid edge detected! Sink cannot have outgoing edges!");
        }
      }
    }
    return new DefaultPhysicalPlanImpl<>(sourceMap, operators, sinkMap);
  }
}