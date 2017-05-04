/*
 * Copyright (C) 2017 Seoul National University
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
package edu.snu.mist.core.task.globalsched;

import edu.snu.mist.core.parameters.ThreadNumLimit;
import edu.snu.mist.core.task.MistPubSubEventHandler;
import edu.snu.mist.core.task.eventProcessors.EventProcessorManager;
import edu.snu.mist.core.task.eventProcessors.parameters.DefaultNumEventProcessors;
import edu.snu.mist.core.task.globalsched.metrics.GlobalSchedGlobalMetrics;
import edu.snu.mist.core.task.globalsched.parameters.*;
import edu.snu.mist.core.task.metrics.EventProcessorNumAssigner;
import edu.snu.mist.core.task.metrics.MetricUpdateEvent;
import org.apache.reef.tang.annotations.Parameter;

import javax.inject.Inject;

/**
 * This is a EventProcessorNumAssigner assigns global event processors.
 * If the total event number is enough but the CPU load is quite low,
 * then this handler will create event processors more.
 * Else if the total event number is quite low and the CPU load is also low,
 * then this handler will close some event processors.
 * Also, it will do additive increase and additive decrease of the number of event processors.
 */
public final class AIADEventProcessorNumAssigner implements EventProcessorNumAssigner {

  /**
   * The limit of the total number of executor threads.
   */
  private final int threadNumLimit;

  /**
   * The default number of event processors.
   */
  private final int defaultNumEventProcessors;

  /**
   * The high threshold of the number of events.
   * If there are more events than this value, then the system will be regard as having many events.
   */
  private final long eventNumHighThreshold;

  /**
   * The low threshold of the number of events.
   * If there are less events than this value, then the system will be regarded as having few events.
   */
  private final long eventNumLowThreshold;

  /**
   * The low threshold of the CPU utilization.
   * If the CPU utilization is lower than than this value, then the system will be regarded as under utilized.
   */
  private final double cpuUtilLowThreshold;

  /**
   * The number of increasing event processors during the addition phase.
   */
  private final int increaseNum;

  /**
   * The number of decreasing event processors during the subtraction phase.
   */
  private final int decreaseNum;

  /**
   * Event processor manager that manages event processors globally.
   */
  private final EventProcessorManager eventProcessorManager;

  /**
   * A metric contains global information.
   * The number of events and the cpu utilization of the whole system in this metric will be used.
   */
  private final GlobalSchedGlobalMetrics metrics;

  @Inject
  private AIADEventProcessorNumAssigner(
      @Parameter(DefaultNumEventProcessors.class) final int defaultNumEventProcessors,
      @Parameter(ThreadNumLimit.class) final int threadNumLimit,
      @Parameter(EventNumHighThreshold.class) final long eventNumHighThreshold,
      @Parameter(EventNumLowThreshold.class) final long eventNumLowThreshold,
      @Parameter(CpuUtilLowThreshold.class) final double cpuUtilLowThreshold,
      @Parameter(EventProcessorIncreaseNum.class) final int increaseNum,
      @Parameter(EventProcessorDecreaseNum.class) final int decreaseNum,
      final EventProcessorManager eventProcessorManager,
      final GlobalSchedGlobalMetrics globalMetrics,
      final MistPubSubEventHandler pubSubEventHandler) {
    this.defaultNumEventProcessors = defaultNumEventProcessors;
    this.threadNumLimit = threadNumLimit;
    this.eventNumHighThreshold = eventNumHighThreshold;
    this.eventNumLowThreshold = eventNumLowThreshold;
    this.cpuUtilLowThreshold = cpuUtilLowThreshold;
    this.increaseNum = increaseNum;
    this.decreaseNum = decreaseNum;
    this.eventProcessorManager = eventProcessorManager;
    this.metrics = globalMetrics;
    pubSubEventHandler.getPubSubEventHandler().subscribe(MetricUpdateEvent.class, this);
  }

  /**
   * Assign event processor number.
   */
  @Override
  public void onNext(final MetricUpdateEvent metricUpdateEvent) {
    if (metrics.getCpuUtilMetric().getSystemCpuUtil() < cpuUtilLowThreshold) {
      if (metrics.getNumEventAndWeightMetric().getNumEvents() > eventNumHighThreshold) {
        // If the cpu utilization is low in spite of enough events,
        // the event processors could be blocked by some operations such as I/O.
        // In that case, we should increase the number of event processors.
        final int currentEventProcessorsNum = eventProcessorManager.getEventProcessors().size();
        if (currentEventProcessorsNum + increaseNum  < threadNumLimit) {
          eventProcessorManager.adjustEventProcessorNum(currentEventProcessorsNum + increaseNum);
        } else if (currentEventProcessorsNum + increaseNum > threadNumLimit) {
          eventProcessorManager.adjustEventProcessorNum(threadNumLimit);
        }
      } else if (metrics.getNumEventAndWeightMetric().getNumEvents() < eventNumLowThreshold) {
        // If the cpu utilization is low and there are few events,
        // then there might be too many event processors.
        // The decrease will be additive because we do not have to react rapidly to the idle state.
        final int currentEventProcessorsNum = eventProcessorManager.getEventProcessors().size();
        if (currentEventProcessorsNum - decreaseNum > defaultNumEventProcessors) {
          eventProcessorManager.adjustEventProcessorNum(currentEventProcessorsNum - decreaseNum);
        } else if (currentEventProcessorsNum - decreaseNum < defaultNumEventProcessors) {
          eventProcessorManager.adjustEventProcessorNum(defaultNumEventProcessors);
        }
      }
    }
  }
}