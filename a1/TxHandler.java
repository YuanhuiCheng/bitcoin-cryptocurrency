import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TxHandler {

    private UTXOPool utxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        // IMPLEMENT THIS
        this.utxoPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool,
     * (2) the signatures on each input of {@code tx} are valid,
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     * values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        // IMPLEMENT THIS
        List<Transaction.Input> txIns = tx.getInputs();
        Set<UTXO> utxoSet = new HashSet<>();
        double inValSum = 0;
        double outValSum = 0;
        for (int idx = 0; idx < txIns.size(); idx++) {
            Transaction.Input txIn = txIns.get(idx);
            UTXO prevUtxo = new UTXO(txIn.prevTxHash, txIn.outputIndex);

            // (1) check if the tx's outputs are in the UTXO pool
            if (!utxoPool.contains(prevUtxo)) return false;

            // (2) check if the signatures on each input of the tx are valid
            Transaction.Output prevTxOut = utxoPool.getTxOutput(prevUtxo);
            if (!Crypto.verifySignature(prevTxOut.address, tx.getRawDataToSign(idx), txIn.signature)) {
                return false;
            }

            // (3) check if no UTXO is claimed multiple times by tx
            if (!utxoSet.add(prevUtxo)) return false;

            inValSum += prevTxOut.value;
        }

        for (Transaction.Output txOut : tx.getOutputs()) {
            // (4) check if all of tx's output values are non-negative
            if (txOut.value < 0) return false;

            outValSum += txOut.value;
        }
        // check if the sum of tx's input values is greater than or equal to the sum of it's output
        return inValSum >= outValSum;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
        List<Transaction> txs = new ArrayList<>();

        for (Transaction tx : possibleTxs) {
            if (isValidTx(tx)) {
                txs.add(tx);

                for (Transaction.Input txIn : tx.getInputs()) {
                    UTXO spentUtxo = new UTXO(txIn.prevTxHash, txIn.outputIndex);
                    utxoPool.removeUTXO(spentUtxo);
                }

                List<Transaction.Output> txOuts = tx.getOutputs();
                for (int idx = 0; idx < txOuts.size(); idx++) {
                    UTXO receivedUtxo = new UTXO(tx.getHash(), idx);
                    utxoPool.addUTXO(receivedUtxo, txOuts.get(idx));
                }
            }
        }
        return txs.toArray(new Transaction[txs.size()]);
    }

}
