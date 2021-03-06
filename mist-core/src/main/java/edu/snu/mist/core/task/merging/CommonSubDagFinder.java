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
package edu.snu.mist.core.task.merging;

import edu.snu.mist.common.graph.DAG;
import edu.snu.mist.common.graph.MISTEdge;
import edu.snu.mist.core.task.ConfigVertex;
import edu.snu.mist.core.task.ExecutionDag;
import edu.snu.mist.core.task.ExecutionVertex;
import org.apache.reef.tang.annotations.DefaultImplementation;

import java.util.Map;

/**
 * This is the interface for the algorithm that finds a common sub-dag between execution dag and submitted dag.
 */
@DefaultImplementation(DfsCommonSubDagFinder.class)
interface CommonSubDagFinder {

  /**
   * Return a map that has ConfigVertex of the submitted query as a key and
   * the corresponding ExecutionVertex of the execution dag as a value.
   * This map holds which execution vertex in the submitted dag is equal to the vertex in the execution dag
   * @param executionDag execution dag that is currently running (destination of merging)
   * @param submittedDag submitted dag that is newly submitted (src of merging)
   * @return map that holds the common sub-dag vertices
   */
  Map<ConfigVertex, ExecutionVertex> findSubDag(ExecutionDag executionDag,
                                                DAG<ConfigVertex, MISTEdge> submittedDag);
}
