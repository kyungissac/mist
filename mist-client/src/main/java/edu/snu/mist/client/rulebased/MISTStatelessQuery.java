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
package edu.snu.mist.client.rulebased;

import java.util.ArrayList;
import java.util.List;

/**
 * A class which contains information about MISTStatelessRule query.
 */
public final class MISTStatelessQuery {

  private final RuleBasedInput input;
  private final List<StatelessRule> statelessRules;
  private final String superGroupId;
  private final String subGroupId;

  /**
   * Creates an immutable stateless query.
   */
  public MISTStatelessQuery(final RuleBasedInput input,
                            final List<StatelessRule> statelessRules,
                            final String superGroupId,
                            final String subGroupId) {
    this.input = input;
    this.statelessRules = statelessRules;
    this.superGroupId = superGroupId;
    this.subGroupId = subGroupId;
  }

  /**
   * @return input for this query.
   */
  public RuleBasedInput getInput() {
    return input;
  }

  /**
   * @return list of all stateless rules.
   */
  public List<StatelessRule> getStatelessRules() {
    return this.statelessRules;
  }

  /**
   *@return groupId for this query.
   */
  public String getSuperGroupId() {
      return this.superGroupId;
  }

  /**
   *@return groupId for this query.
   */
  public String getSubGroupId() {
    return this.subGroupId;
  }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final MISTStatelessQuery that = (MISTStatelessQuery) o;

        if (input != null ? !input.equals(that.input) : that.input != null) {
            return false;
        }
        if (statelessRules != null ? !statelessRules.equals(that.statelessRules) :
                that.statelessRules != null) {
            return false;
        }
        return superGroupId != null ? superGroupId.equals(that.superGroupId) : that.superGroupId == null;
    }

    @Override
    public int hashCode() {
        int result = input != null ? input.hashCode() : 0;
        result = 31 * result + (statelessRules != null ? statelessRules.hashCode() : 0);
        result = 31 * result + (superGroupId != null ? superGroupId.hashCode() : 0);
        return result;
    }

    /**
   * Builder for MISTStatelessQuery.
   */
  public static class Builder {
    private RuleBasedInput input;
    private final List<StatelessRule> statelessRules;
    private final String superGroupId;
    private final String subGroupId;

    /**
     * Creates a new builder.
     */
    public Builder(final String superGroupId,
                   final String subGroupId) {
      this.input = null;
      this.statelessRules = new ArrayList<>();
      this.superGroupId = superGroupId;
      this.subGroupId = subGroupId;
    }

    /**
     * Sets the input for this stateless query.
     * @param inputParam parameter for input
     * @return builder
     */
    public Builder input(final RuleBasedInput inputParam) {
      this.input = inputParam;
      return this;
    }

    /**
     * Add a stateless rule.
     * @param statelessRule a target rule
     * @return buidler
     */
    public Builder addStatelessRule(final StatelessRule statelessRule) {
      statelessRules.add(statelessRule);
      return this;
    }

    /**
     * Creates an immutable stateless query.
     * @return
     */
    public MISTStatelessQuery build() {
      if (input == null || statelessRules.size() == 0) {
        throw new IllegalStateException("Input or stateless rules are not defined!");
      }
      return new MISTStatelessQuery(input, statelessRules, superGroupId, subGroupId);
    }
  }
}