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
package org.hyperledger.besu.tests.acceptance.dsl.transaction.perm;

import static org.web3j.utils.Numeric.toHexString;

import org.hyperledger.besu.ethereum.core.Address;
import org.hyperledger.besu.ethereum.core.Hash;
import org.hyperledger.besu.plugin.data.EnodeURL;
import org.hyperledger.besu.tests.acceptance.dsl.account.Account;
import org.hyperledger.besu.tests.acceptance.dsl.transaction.NodeRequests;
import org.hyperledger.besu.tests.acceptance.dsl.transaction.Transaction;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;

import org.apache.tuweni.bytes.Bytes;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;

public class NodeSmartContractPermissioningAllowNodeByURLV2Transaction
    implements Transaction<Hash> {

  private final Account sender;
  private final Address contractAddress;
  private final EnodeURL enodeUrl;

  public NodeSmartContractPermissioningAllowNodeByURLV2Transaction(
      final Account sender, final Address contractAddress, final EnodeURL enodeUrl) {
    this.sender = sender;
    this.contractAddress = contractAddress;
    this.enodeUrl = enodeUrl;
  }

  @Override
  public Hash execute(final NodeRequests node) {
    final String signedTransactionData = signedTransactionData();
    try {
      String hash =
          node.eth().ethSendRawTransaction(signedTransactionData).send().getTransactionHash();
      return Hash.fromHexString(hash);
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  private String signedTransactionData() {
    final Bytes payload = createPayload(enodeUrl);

    RawTransaction transaction =
        RawTransaction.createTransaction(
            sender.getNextNonce(),
            BigInteger.valueOf(1_000L),
            BigInteger.valueOf(3_000_000L),
            contractAddress.toString(),
            payload.toString());

    return toHexString(
        TransactionEncoder.signMessage(transaction, sender.web3jCredentialsOrThrow()));
  }

  private Bytes createPayload(final EnodeURL enodeUrl) {
    try {
      final String hexNodeIdString = enodeUrl.getNodeId().toUnprefixedHexString();
      final String address = enodeUrl.toURI().getHost();
      final int port = enodeUrl.getListeningPortOrZero();

      final Function addNodeFunction =
          FunctionEncoder.makeFunction(
              "addEnode",
              List.of("string", "string", "uint16"),
              List.of(hexNodeIdString, address, port),
              Collections.emptyList());
      return Bytes.fromHexString(FunctionEncoder.encode(addNodeFunction));
    } catch (Exception e) {
      throw new RuntimeException("Error adding node to allowlist", e);
    }
  }
}
