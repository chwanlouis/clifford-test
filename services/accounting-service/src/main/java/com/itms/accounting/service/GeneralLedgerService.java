package com.itms.accounting.service;

import com.itms.accounting.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class GeneralLedgerService {

    private final Map<String, Account> accounts = new ConcurrentHashMap<>();
    private final List<JournalEntry> journalEntries = new ArrayList<>();

    /**
     * Post a double-entry journal entry.
     * Debits the debitAccount and credits the creditAccount.
     */
    public JournalEntry postJournalEntry(String tradeReference, String debitAccountCode,
                                          String creditAccountCode, BigDecimal amount,
                                          String currency, String description, JournalEntryType type) {
        log.info("Posting journal entry: debit={}, credit={}, amount={} {}",
                debitAccountCode, creditAccountCode, amount, currency);

        Account debitAccount = getOrCreateAccount(debitAccountCode, currency);
        Account creditAccount = getOrCreateAccount(creditAccountCode, currency);

        // Debit = increase ASSET/EXPENSE, decrease LIABILITY/EQUITY/INCOME
        applyDebit(debitAccount, amount);
        // Credit = decrease ASSET/EXPENSE, increase LIABILITY/EQUITY/INCOME
        applyCredit(creditAccount, amount);

        JournalEntry entry = JournalEntry.builder()
                .tradeReference(tradeReference)
                .debitAccount(debitAccountCode)
                .creditAccount(creditAccountCode)
                .amount(amount)
                .currency(currency)
                .entryDate(LocalDate.now())
                .description(description)
                .type(type)
                .status(JournalEntryStatus.POSTED)
                .postedAt(LocalDateTime.now())
                .build();

        journalEntries.add(entry);
        log.info("Journal entry posted: {}", entry.getEntryReference());
        return entry;
    }

    /**
     * Generate double-entry for a trade buy.
     * DR: Securities/Investments Account
     * CR: Cash/Settlement Account
     */
    public List<JournalEntry> generateTradeBuyEntries(String tradeReference, String portfolio,
                                                       BigDecimal notional, String currency,
                                                       BigDecimal commission) {
        List<JournalEntry> entries = new ArrayList<>();

        // Main entry: buy securities
        entries.add(postJournalEntry(tradeReference,
                "SECURITIES-" + portfolio, "CASH-" + portfolio,
                notional, currency, "Trade purchase: " + tradeReference, JournalEntryType.TRADE_BUY));

        // Commission entry
        if (commission != null && commission.compareTo(BigDecimal.ZERO) > 0) {
            entries.add(postJournalEntry(tradeReference,
                    "COMMISSION-EXPENSE", "CASH-" + portfolio,
                    commission, currency, "Commission: " + tradeReference, JournalEntryType.TRADE_BUY));
        }

        return entries;
    }

    /**
     * Generate double-entry for a trade sell.
     * DR: Cash/Settlement Account
     * CR: Securities/Investments Account + Realized P&L
     */
    public List<JournalEntry> generateTradeSellEntries(String tradeReference, String portfolio,
                                                        BigDecimal proceeds, BigDecimal costBasis,
                                                        String currency) {
        List<JournalEntry> entries = new ArrayList<>();
        BigDecimal realizedPnl = proceeds.subtract(costBasis);

        // Receive cash
        entries.add(postJournalEntry(tradeReference,
                "CASH-" + portfolio, "SECURITIES-" + portfolio,
                proceeds, currency, "Trade sale proceeds: " + tradeReference, JournalEntryType.TRADE_SELL));

        // Book realized P&L
        if (realizedPnl.compareTo(BigDecimal.ZERO) > 0) {
            entries.add(postJournalEntry(tradeReference,
                    "SECURITIES-" + portfolio, "REALIZED-PNL-" + portfolio,
                    realizedPnl, currency, "Realized gain: " + tradeReference, JournalEntryType.REALIZED_PNL));
        } else if (realizedPnl.compareTo(BigDecimal.ZERO) < 0) {
            entries.add(postJournalEntry(tradeReference,
                    "REALIZED-LOSS-" + portfolio, "SECURITIES-" + portfolio,
                    realizedPnl.abs(), currency, "Realized loss: " + tradeReference, JournalEntryType.REALIZED_PNL));
        }

        return entries;
    }

    public List<JournalEntry> getAllEntries() {
        return Collections.unmodifiableList(journalEntries);
    }

    public List<JournalEntry> getEntriesByTradeReference(String tradeReference) {
        return journalEntries.stream()
                .filter(e -> tradeReference.equals(e.getTradeReference()))
                .toList();
    }

    public Map<String, BigDecimal> getTrialBalance() {
        Map<String, BigDecimal> trialBalance = new HashMap<>();
        accounts.forEach((code, account) -> trialBalance.put(code, account.getBalance()));
        return trialBalance;
    }

    private Account getOrCreateAccount(String accountCode, String currency) {
        return accounts.computeIfAbsent(accountCode, code -> Account.builder()
                .accountCode(code)
                .accountName(code)
                .type(inferAccountType(code))
                .currency(currency)
                .balance(BigDecimal.ZERO)
                .build());
    }

    private void applyDebit(Account account, BigDecimal amount) {
        if (account.getType() == AccountType.ASSET || account.getType() == AccountType.EXPENSE) {
            account.setBalance(account.getBalance().add(amount));
        } else {
            account.setBalance(account.getBalance().subtract(amount));
        }
    }

    private void applyCredit(Account account, BigDecimal amount) {
        if (account.getType() == AccountType.ASSET || account.getType() == AccountType.EXPENSE) {
            account.setBalance(account.getBalance().subtract(amount));
        } else {
            account.setBalance(account.getBalance().add(amount));
        }
    }

    private AccountType inferAccountType(String code) {
        if (code.startsWith("CASH") || code.startsWith("SECURITIES")) return AccountType.ASSET;
        if (code.startsWith("REALIZED-PNL") || code.startsWith("DIVIDEND")) return AccountType.INCOME;
        if (code.startsWith("COMMISSION") || code.startsWith("REALIZED-LOSS")) return AccountType.EXPENSE;
        return AccountType.ASSET;
    }
}
