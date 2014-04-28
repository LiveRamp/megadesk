/**
 *  Copyright 2014 LiveRamp
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.liveramp.megadesk.recipes.aggregator;

import com.liveramp.megadesk.base.transaction.BaseTransactionExecutor;
import com.liveramp.megadesk.core.state.Variable;
import com.liveramp.megadesk.core.transaction.Transaction;
import com.liveramp.megadesk.core.transaction.TransactionExecutor;
import com.liveramp.megadesk.recipes.transaction.Alter;
import com.liveramp.megadesk.recipes.transaction.Write;

public class InterProcessAggregator<AGGREGATE> {

  private final Aggregator<AGGREGATE> aggregator;
  private final Variable<AGGREGATE> variable;
  private final TransactionExecutor executor;
  private AGGREGATE aggregate;

  public InterProcessAggregator(Variable<AGGREGATE> variable, Aggregator<AGGREGATE> aggregator) {
    this.variable = variable;
    this.aggregator = aggregator;
    this.executor = new BaseTransactionExecutor();
    this.aggregate = aggregator.initialValue();
  }

  public void reset() {
    resetRemote();
    resetLocal();
  }

  public void resetLocal() {
    this.aggregate = aggregator.initialValue();
  }

  public void resetRemote() {
    execute(new Write<AGGREGATE>(variable, aggregator.initialValue()));
  }

  public AGGREGATE aggregateLocal(AGGREGATE value) {
    aggregate = aggregator.aggregate(aggregate, value);
    return aggregate;
  }

  public AGGREGATE aggregateRemote() {
    AGGREGATE result = execute(new Aggregate<AGGREGATE>(variable, aggregator, aggregate));
    resetLocal();
    return result;
  }

  private <T> T execute(Transaction<T> t) {
    try {
      return executor.execute(t);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static class Aggregate<AGGREGATE> extends Alter<AGGREGATE> {

    private final Aggregator<AGGREGATE> aggregator;
    private final AGGREGATE partialAggregate;

    private Aggregate(Variable<AGGREGATE> variable, Aggregator<AGGREGATE> aggregator, AGGREGATE partialAggregate) {
      super(variable);
      this.aggregator = aggregator;
      this.partialAggregate = partialAggregate;
    }

    @Override
    protected AGGREGATE alter(AGGREGATE aggregate) {
      return aggregator.aggregate(aggregate, partialAggregate);
    }
  }
}