package com.tokenrealty.wallet.repository;

import com.tokenrealty.wallet.entity.InvestorWallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvestorWalletRepository extends JpaRepository<InvestorWallet, UUID> {

    List<InvestorWallet> findByInvestorIdOrderByPrimaryDescCreatedAtAsc(UUID investorId);

    Optional<InvestorWallet> findByInvestorIdAndPrimaryTrue(UUID investorId);

    Optional<InvestorWallet> findByInvestorIdAndWalletType(UUID investorId, InvestorWallet.WalletType walletType);

    boolean existsByWalletAddressIgnoreCase(String walletAddress);

    boolean existsByInvestorIdAndWalletType(UUID investorId, InvestorWallet.WalletType walletType);
}
