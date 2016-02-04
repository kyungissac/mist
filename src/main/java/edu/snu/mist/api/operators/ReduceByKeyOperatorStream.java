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
package edu.snu.mist.api.operators;

import edu.snu.mist.api.ContinuousStream;
import edu.snu.mist.api.StreamType;
import edu.snu.mist.api.functions.MISTBiFunction;

/**
 * This class implements the necessary methods for getting information
 * about reduceByKey operator.
 */
public final class ReduceByKeyOperatorStream<IN, K, V> extends ReduceOperatorStream<IN, K, V> {

  public ReduceByKeyOperatorStream(final ContinuousStream<IN> precedingStream, final int keyFieldIndex,
                                   final Class<K> keyType, final MISTBiFunction<V, V, V> reduceFunc) {
    super(StreamType.OperatorType.REDUCE_BY_KEY, precedingStream, keyFieldIndex, keyType, reduceFunc);
  }
}
