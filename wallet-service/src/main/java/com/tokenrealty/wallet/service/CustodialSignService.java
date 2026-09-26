package com.tokenrealty.wallet.service;

import com.tokenrealty.wallet.crypto.KmsWalletEncryptionService;
import com.tokenrealty.wallet.dto.WalletDtos.SignTransactionRequest;
import com.tokenrealty.wallet.dto.WalletDtos.SignTransactionResponse;
import com.tokenrealty.wallet.entity.InvestorWallet;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.utils.Numeric;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustodialSignService {

    private final WalletService walletService;
    private final KmsWalletEncryptionService encryptionService;

    @Value("${tokenrealty.wallet.blockchain.chain-id:31337}")
    private long chainId;

    @Transactional(readOnly = true)
    public SignTransactionResponse signTransaction(UUID investorId, SignTransactionRequest request) {
        InvestorWallet wallet = walletService.getCustodialWallet(investorId);
        String privateKey = encryptionService.decrypt(wallet.getEncryptedPrivateKey());
        Credentials credentials = Credentials.create(privateKey);

        RawTransaction rawTransaction = RawTransaction.createTransaction(
                request.nonce(),
                request.gasPrice(),
                request.gasLimit(),
                request.to(),
                request.value(),
                request.data() != null ? request.data() : "");

        byte[] signed = TransactionEncoder.signMessage(rawTransaction, chainId, credentials);
        return new SignTransactionResponse(Numeric.toHexString(signed), wallet.getWalletAddress());
    }
}
