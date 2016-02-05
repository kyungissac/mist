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
package edu.snu.mist.task.operators;

import org.apache.reef.wake.Identifier;

import java.util.logging.Logger;

/**
 * Stateless operator transforms inputs by doing computation
 * and immediately pushes the results to the OutputEmitter without state modification.
 * @param <I> input
 * @param <O> output
 */
public abstract class StatelessOperator<I, O> extends BaseOperator<I, O> {
  private static final Logger LOG = Logger.getLogger(StatelessOperator.class.getName());

  public StatelessOperator(final Identifier queryId,
                           final Identifier operatorId) {
    super(queryId, operatorId);
  }
}
