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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.hyperledger.besu.ethereum.api.jsonrpc.internal.JsonRpcRequest;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.response.JsonRpcSuccessResponse;
import org.hyperledger.besu.ethereum.api.query.BlockchainQueries;
import org.hyperledger.besu.ethereum.chain.Blockchain;
import org.hyperledger.besu.ethereum.chain.ChainHead;
import org.hyperledger.besu.ethereum.core.Address;
import org.hyperledger.besu.ethereum.core.Hash;
import org.hyperledger.besu.ethereum.eth.transactions.sorter.AbstractPendingTransactionsSorter;
import org.hyperledger.besu.ethereum.eth.transactions.sorter.BaseFeePendingTransactionsSorter;
import org.hyperledger.besu.ethereum.eth.transactions.sorter.GasPricePendingTransactionsSorter;

import java.util.Arrays;
import java.util.Collection;
import java.util.OptionalLong;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class EthGetTransactionCountTest {

  @Parameterized.Parameter public AbstractPendingTransactionsSorter pendingTransactions;

  private final Blockchain blockchain = mock(Blockchain.class);
  private final BlockchainQueries blockchainQueries = mock(BlockchainQueries.class);
  private final ChainHead chainHead = mock(ChainHead.class);

  private EthGetTransactionCount ethGetTransactionCount;
  private final String pendingTransactionString = "0x00000000000000000000000000000000000000AA";
  private final Object[] pendingParams = new Object[] {pendingTransactionString, "pending"};

  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[][] {
          {mock(GasPricePendingTransactionsSorter.class)},
          {mock(BaseFeePendingTransactionsSorter.class)}
        });
  }

  @Before
  public void setup() {
    ethGetTransactionCount = new EthGetTransactionCount(blockchainQueries, pendingTransactions);
  }

  @Test
  public void shouldUsePendingTransactionsWhenToldTo() {
    final Address address = Address.fromHexString(pendingTransactionString);
    when(pendingTransactions.getNextNonceForSender(address)).thenReturn(OptionalLong.of(12));
    mockGetTransactionCount(address, 7L);
    final JsonRpcRequestContext request =
        new JsonRpcRequestContext(
            new JsonRpcRequest("1", "eth_getTransactionCount", pendingParams));
    final JsonRpcSuccessResponse response =
        (JsonRpcSuccessResponse) ethGetTransactionCount.response(request);
    assertThat(response.getResult()).isEqualTo("0xc");
  }

  @Test
  public void shouldUseLatestTransactionsWhenNoPendingTransactions() {
    final Address address = Address.fromHexString(pendingTransactionString);
    when(pendingTransactions.getNextNonceForSender(address)).thenReturn(OptionalLong.empty());
    mockGetTransactionCount(address, 7L);
    final JsonRpcRequestContext request =
        new JsonRpcRequestContext(
            new JsonRpcRequest("1", "eth_getTransactionCount", pendingParams));
    final JsonRpcSuccessResponse response =
        (JsonRpcSuccessResponse) ethGetTransactionCount.response(request);
    assertThat(response.getResult()).isEqualTo("0x7");
  }

  @Test
  public void shouldUseLatestWhenItIsBiggerThanPending() {
    final Address address = Address.fromHexString(pendingTransactionString);
    mockGetTransactionCount(address, 8);
    when(pendingTransactions.getNextNonceForSender(Address.fromHexString(pendingTransactionString)))
        .thenReturn(OptionalLong.of(4));
    final JsonRpcRequestContext request =
        new JsonRpcRequestContext(
            new JsonRpcRequest("1", "eth_getTransactionCount", pendingParams));
    final JsonRpcSuccessResponse response =
        (JsonRpcSuccessResponse) ethGetTransactionCount.response(request);
    assertThat(response.getResult()).isEqualTo("0x8");
  }

  private void mockGetTransactionCount(final Address address, final long transactionCount) {
    when(blockchainQueries.getBlockchain()).thenReturn(blockchain);
    when(blockchainQueries.getBlockchain().getChainHead()).thenReturn(chainHead);
    when(blockchainQueries.getBlockchain().getChainHead().getHash()).thenReturn(Hash.ZERO);
    when(blockchainQueries.getTransactionCount(address, Hash.ZERO)).thenReturn(transactionCount);
  }
}
