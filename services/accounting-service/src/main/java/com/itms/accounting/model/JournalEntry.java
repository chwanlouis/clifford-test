package com.itms.accounting.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "journal_entries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String entryReference;

    @Column(nullable = false)
    private String tradeReference;

    @Column(nullable = false)
    private String debitAccount;

    @Column(nullable = false)
    private String creditAccount;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private LocalDate entryDate;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JournalEntryType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JournalEntryStatus status;

    private LocalDateTime postedAt;
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (entryReference == null) {
            entryReference = "JNL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
        if (status == null) {
            status = JournalEntryStatus.DRAFT;
        }
    }
}
