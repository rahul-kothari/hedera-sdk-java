package com.hedera.hashgraph.sdk.examples;

import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.HederaStatusException;
import com.hedera.hashgraph.sdk.TransactionId;
import com.hedera.hashgraph.sdk.account.*;
import com.hedera.hashgraph.sdk.crypto.ed25519.Ed25519PrivateKey;

import java.io.FileNotFoundException;
import java.util.Objects;

import io.github.cdimascio.dotenv.Dotenv;

public final class UpdateAccountPublicKey {

    // see `.env.sample` in the repository root for how to specify these values
    // or set environment variables with the same names
    private static final AccountId OPERATOR_ID = AccountId.fromString(Objects.requireNonNull(Dotenv.load().get("OPERATOR_ID")));
    private static final Ed25519PrivateKey OPERATOR_KEY = Ed25519PrivateKey.fromString(Objects.requireNonNull(Dotenv.load().get("OPERATOR_KEY")));
    private static final String HEDERA_NETWORK = Dotenv.load().get("HEDERA_NETWORK");
    private static final String CONFIG_FILE = Dotenv.load().get("CONFIG_FILE");

    private UpdateAccountPublicKey() { }

    public static void main(String[] args) throws HederaStatusException {
        Client client;

        if (HEDERA_NETWORK != null && HEDERA_NETWORK.equals("previewnet")) {
            client = Client.forPreviewnet();
        } else {
            try {
                client = Client.fromFile(CONFIG_FILE != null ? CONFIG_FILE : "");
            } catch (FileNotFoundException e) {
                client = Client.forTestnet();
            }
        }

        client.setOperator(OPERATOR_ID, OPERATOR_KEY);
        client.setMaxTransactionFee(800000000);

        // First, we create a new account so we don't affect our account

        Ed25519PrivateKey originalKey = Ed25519PrivateKey.generate();
        TransactionId acctTransactionId = new AccountCreateTransaction()
            .setMaxTransactionFee(1_000_000_000)
            .setKey(originalKey.publicKey)
            .setInitialBalance(new Hbar(1))
            .execute(client);

        System.out.println("transaction ID: " + acctTransactionId);
        AccountId accountId = acctTransactionId.getReceipt(client).getAccountId();
        System.out.println("account = " + accountId);
        // Next, we update the key

        Ed25519PrivateKey newKey = Ed25519PrivateKey.generate();

        System.out.println(" :: update public key of account " + accountId);
        System.out.println("set key = " + newKey.publicKey);

        TransactionId transactionId = new AccountUpdateTransaction()
            .setAccountId(accountId)
            .setKey(newKey.publicKey)
            .build(client)
            // Sign with the previous key and the new key
            .sign(originalKey)
            .sign(newKey)
            .execute(client);

        System.out.println("transaction ID: " + transactionId);

        // (important!) wait for the transaction to complete by querying the receipt
        transactionId.getReceipt(client);

        // Now we fetch the account information to check if the key was changed
        System.out.println(" :: getAccount and check our current key");

        AccountInfo info = new AccountInfoQuery()
            .setAccountId(accountId)
            .execute(client);

        System.out.println("key = " + info.key);
    }
}
