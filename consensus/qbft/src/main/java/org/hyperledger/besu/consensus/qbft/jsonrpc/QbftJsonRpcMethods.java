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
package org.hyperledger.besu.consensus.qbft.jsonrpc;

import org.hyperledger.besu.consensus.common.BlockInterface;
import org.hyperledger.besu.consensus.common.EpochManager;
import org.hyperledger.besu.consensus.common.bft.BftBlockInterface;
import org.hyperledger.besu.consensus.common.bft.BftContext;
import org.hyperledger.besu.consensus.common.validator.ValidatorProvider;
import org.hyperledger.besu.consensus.common.validator.blockbased.BlockValidatorProvider;
import org.hyperledger.besu.consensus.qbft.jsonrpc.methods.QbftDiscardValidatorVote;
import org.hyperledger.besu.consensus.qbft.jsonrpc.methods.QbftGetPendingVotes;
import org.hyperledger.besu.consensus.qbft.jsonrpc.methods.QbftGetSignerMetrics;
import org.hyperledger.besu.consensus.qbft.jsonrpc.methods.QbftGetValidatorsByBlockHash;
import org.hyperledger.besu.consensus.qbft.jsonrpc.methods.QbftGetValidatorsByBlockNumber;
import org.hyperledger.besu.consensus.qbft.jsonrpc.methods.QbftProposeValidatorVote;
import org.hyperledger.besu.ethereum.ProtocolContext;
import org.hyperledger.besu.ethereum.api.jsonrpc.RpcApi;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.methods.JsonRpcMethod;
import org.hyperledger.besu.ethereum.api.jsonrpc.methods.ApiGroupJsonRpcMethods;
import org.hyperledger.besu.ethereum.api.query.BlockchainQueries;
import org.hyperledger.besu.ethereum.chain.MutableBlockchain;

import java.util.Map;

public class QbftJsonRpcMethods extends ApiGroupJsonRpcMethods {

  private final ProtocolContext context;

  public QbftJsonRpcMethods(final ProtocolContext context) {
    this.context = context;
  }

  @Override
  protected RpcApi getApiGroup() {
    return QbftRpcApis.QBFT;
  }

  @Override
  protected Map<String, JsonRpcMethod> create() {
    final MutableBlockchain mutableBlockchain = context.getBlockchain();
    final BlockchainQueries blockchainQueries =
        new BlockchainQueries(context.getBlockchain(), context.getWorldStateArchive());
    final BftContext bftContext = context.getConsensusState(BftContext.class);
    final BlockInterface blockInterface = bftContext.getBlockInterface();
    final ValidatorProvider validatorProvider = bftContext.getValidatorProvider();

    // Must create our own voteTallyCache as using this would pollute the main voteTallyCache
    final ValidatorProvider readOnlyValidatorProvider =
        createValidatorProvider(context, mutableBlockchain);

    return mapOf(
        new QbftProposeValidatorVote(validatorProvider),
        new QbftGetValidatorsByBlockNumber(blockchainQueries, readOnlyValidatorProvider),
        new QbftDiscardValidatorVote(validatorProvider),
        new QbftGetValidatorsByBlockHash(context.getBlockchain(), readOnlyValidatorProvider),
        new QbftGetSignerMetrics(readOnlyValidatorProvider, blockInterface, blockchainQueries),
        new QbftGetPendingVotes(validatorProvider));
  }

  private ValidatorProvider createValidatorProvider(
      final ProtocolContext context, final MutableBlockchain blockchain) {
    final BftContext bftContext = context.getConsensusState(BftContext.class);
    final EpochManager epochManager = bftContext.getEpochManager();
    final BftBlockInterface bftBlockInterface = bftContext.getBlockInterface();
    return BlockValidatorProvider.nonForkingValidatorProvider(
        blockchain, epochManager, bftBlockInterface);
  }
}
