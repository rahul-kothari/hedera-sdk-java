package com.hedera.hashgraph.sdk.examples;

import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.HederaStatusException;
import com.hedera.hashgraph.sdk.TransactionId;
import com.hedera.hashgraph.sdk.TransactionReceipt;
import com.hedera.hashgraph.sdk.account.AccountId;
import com.hedera.hashgraph.sdk.crypto.ed25519.Ed25519PrivateKey;
import com.hedera.hashgraph.sdk.file.FileCreateTransaction;
import com.hedera.hashgraph.sdk.file.FileDeleteTransaction;
import com.hedera.hashgraph.sdk.file.FileId;
import com.hedera.hashgraph.sdk.file.FileInfo;
import com.hedera.hashgraph.sdk.file.FileInfoQuery;

import java.io.FileNotFoundException;
import java.util.Objects;

import io.github.cdimascio.dotenv.Dotenv;

public final class DeleteFile {

    // see `.env.sample` in the repository root for how to specify these values
    // or set environment variables with the same names
    private static final AccountId OPERATOR_ID = AccountId.fromString(Objects.requireNonNull(Dotenv.load().get("OPERATOR_ID")));
    private static final Ed25519PrivateKey OPERATOR_KEY = Ed25519PrivateKey.fromString(Objects.requireNonNull(Dotenv.load().get("OPERATOR_KEY")));
    private static final String HEDERA_NETWORK = Dotenv.load().get("HEDERA_NETWORK");
    private static final String CONFIG_FILE = Dotenv.load().get("CONFIG_FILE");

    private DeleteFile() { }

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

        // The file is required to be a byte array,
        // you can easily use the bytes of a file instead.
        byte[] fileContents = "Hedera hashgraph is great!".getBytes();

        TransactionId txId = new FileCreateTransaction()
            .addKey(OPERATOR_KEY.publicKey)
            .setContents(fileContents)
            .setMaxTransactionFee(new Hbar(2))
            .execute(client);

        TransactionReceipt receipt = txId.getReceipt(client);
        FileId newFileId = receipt.getFileId();

        System.out.println("file: " + newFileId);

        // now delete the file
        TransactionId fileDeleteTxnId = new FileDeleteTransaction()
            .setFileId(newFileId)
            .execute(client);

        // if this doesn't throw then the transaction was a success
        fileDeleteTxnId.getReceipt(client);

        System.out.println("File deleted successfully.");

        FileInfo fileInfo = new FileInfoQuery()
            .setFileId(newFileId)
            .execute(client);

        // note the above fileInfo will fail with FILE_DELETED due to a known issue on Hedera

    }
}
