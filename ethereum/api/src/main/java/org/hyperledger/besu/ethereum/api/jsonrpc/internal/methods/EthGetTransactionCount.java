/*
 * Copyright ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hyperledger.besu.ethereum.api.jsonrpc.internal.methods;

import org.hyperledger.besu.ethereum.api.jsonrpc.RpcMethod;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.parameters.BlockParameterOrBlockHash;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.results.Quantity;
import org.hyperledger.besu.ethereum.api.query.BlockchainQueries;
import org.hyperledger.besu.ethereum.core.Address;
import org.hyperledger.besu.ethereum.core.Hash;
import org.hyperledger.besu.ethereum.eth.transactions.sorter.AbstractPendingTransactionsSorter;

import java.util.OptionalLong;
import java.util.function.Supplier;

import com.google.common.base.Suppliers;

public class EthGetTransactionCount extends AbstractBlockParameterOrBlockHashMethod {
  private final Supplier<AbstractPendingTransactionsSorter> pendingTransactions;
  private final boolean resultAsDecimal;

  public EthGetTransactionCount(
      final BlockchainQueries blockchain,
      final AbstractPendingTransactionsSorter pendingTransactions) {
    this(Suppliers.ofInstance(blockchain), Suppliers.ofInstance(pendingTransactions), false);
  }

  public EthGetTransactionCount(
      final Supplier<BlockchainQueries> blockchain,
      final Supplier<AbstractPendingTransactionsSorter> pendingTransactions,
      final boolean resultAsDecimal) {
    super(blockchain);
    this.pendingTransactions = pendingTransactions;
    this.resultAsDecimal = resultAsDecimal;
  }

  @Override
  public String getName() {
    return RpcMethod.ETH_GET_TRANSACTION_COUNT.getMethodName();
  }

  @Override
  protected BlockParameterOrBlockHash blockParameterOrBlockHash(
      final JsonRpcRequestContext request) {
    return request.getRequiredParameter(1, BlockParameterOrBlockHash.class);
  }

  @Override
  protected Object pendingResult(final JsonRpcRequestContext request) {
    final Address address = request.getRequiredParameter(0, Address.class);
    final OptionalLong pendingNonce = pendingTransactions.get().getNextNonceForSender(address);
    final long latestNonce =
        getBlockchainQueries()
            .getTransactionCount(
                address, getBlockchainQueries().getBlockchain().getChainHead().getHash());
    return Quantity.create(Math.max(pendingNonce.orElse(0), latestNonce));
  }

  @Override
  protected String resultByBlockHash(final JsonRpcRequestContext request, final Hash blockHash) {
    final Address address = request.getRequiredParameter(0, Address.class);
    final long transactionCount = getBlockchainQueries().getTransactionCount(address, blockHash);

    return resultAsDecimal ? Long.toString(transactionCount) : Quantity.create(transactionCount);
  }
}
