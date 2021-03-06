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
package edu.snu.mist.core.task.groupaware.eventprocessor;

import edu.snu.mist.core.task.groupaware.Group;
import edu.snu.mist.core.task.groupaware.GroupEvent;
import org.apache.reef.tang.annotations.DefaultImplementation;
import org.apache.reef.wake.EventHandler;

import java.util.Collection;

/**
 * This is an interface that picks a next group for processing queries.
 */
@DefaultImplementation(BlockingQueueGroupSelector.class)
public interface NextGroupSelector extends EventHandler<GroupEvent>, AutoCloseable {

  /**
   * Select the next group that will be processed.
   * The events of queries within the group will be executed.
   * The group info should have non-blocking operator chain manager
   * in order to reselect another operator chain manager when there are no active operator chain managers.
   * @return group info that will be executed next
   */
  Group getNextExecutableGroup();

  /**
   * Re-schedule the group to the selector.
   * @param groupInfo group info
   * @param miss true if the group has no active chain
   */
  void reschedule(Group groupInfo, boolean miss);

  /**
   * Re-schedule the groups to the selector.
   * @param groupInfos group infos
   */
  void reschedule(Collection<Group> groupInfos);

  /**
   * Remove the dispatched group.
   * @param group dispatched group
   * @return true if the dispatched group is removed
   */
  boolean removeDispatchedGroup(Group group);
}