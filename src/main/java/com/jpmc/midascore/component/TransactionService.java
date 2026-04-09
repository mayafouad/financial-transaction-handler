package com.jpmc.midascore.component;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.repository.TransactionRecordRepository;
import com.jpmc.midascore.repository.UserRepository;

import jakarta.transaction.Transactional;

import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.foundation.Transaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class TransactionService {

    private final UserRepository userRepository;
    private final Logger log = LoggerFactory.getLogger(TransactionService.class);
    private final TransactionRecordRepository transactionRecordRepository;
    private final IncentiveService incentiveService;

    public TransactionService(UserRepository userRepository,
                              TransactionRecordRepository transactionRecordRepository,
                            IncentiveService incentiveService) {
        this.userRepository = userRepository;
        this.transactionRecordRepository = transactionRecordRepository;
        this.incentiveService = incentiveService;
    }

    @Transactional
    public void process(Transaction transaction) {
        Optional<UserRecord> senderOpt    = Optional.ofNullable(userRepository.findById(transaction.getSenderId()));
        Optional<UserRecord> recipientOpt = Optional.ofNullable(userRepository.findById(transaction.getRecipientId()));

        // Validate: both users must exist
        if (senderOpt.isEmpty() || recipientOpt.isEmpty()) return;

        UserRecord sender    = senderOpt.get();
        UserRecord recipient = recipientOpt.get();

        // Validate: sender must have sufficient balance
        if (sender.getBalance() < transaction.getAmount()) return;

        log.info("{} -> processing transaction: {} -> {} {}: ${}",
            sender.getName(),
            sender.getBalance(),
            recipient.getName(),
            recipient.getBalance(),
            transaction.getAmount());

        
        float incentive = incentiveService.getIncentive(transaction);

        // Adjust balances
        sender.setBalance(sender.getBalance() - transaction.getAmount());
        recipient.setBalance(recipient.getBalance() + transaction.getAmount() + incentive);

        userRepository.save(sender);
        userRepository.save(recipient);

        // Record the transaction
        TransactionRecord record = new TransactionRecord(sender, recipient, transaction.getAmount(), incentive);
        transactionRecordRepository.save(record);

        log.info("{} -> balance check: {} , {} -> balance check: {}", 
        sender.getName(), 
        sender.getBalance(),
        recipient.getName(),
        recipient.getBalance());
    }
}