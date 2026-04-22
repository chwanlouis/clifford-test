package com.itms.settlement.service;

import com.itms.settlement.model.SwiftMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class SwiftMessageService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyMMdd");

    /**
     * Generate MT103 - Single Customer Credit Transfer (cash payment).
     */
    public SwiftMessage generateMT103(String senderBic, String receiverBic, String ref,
                                       BigDecimal amount, String currency, LocalDate valueDate,
                                       String beneficiaryName, String beneficiaryAccount) {
        log.info("Generating MT103 from {} to {}", senderBic, receiverBic);
        String body = String.format(
                ":20:%s\n:23B:CRED\n:32A:%s%s%s\n:50K:%s\n:59:%s\n%s",
                ref, valueDate.format(DATE_FMT), currency,
                amount.toPlainString().replace(".", ","),
                senderBic, beneficiaryAccount, beneficiaryName);

        return SwiftMessage.builder()
                .messageType("MT103")
                .senderBic(senderBic)
                .receiverBic(receiverBic)
                .transactionReference(ref)
                .valueDate(valueDate.format(DATE_FMT))
                .amount(amount.toPlainString())
                .currency(currency)
                .messageBody(body)
                .generatedAt(LocalDateTime.now())
                .isMx(false)
                .build();
    }

    /**
     * Generate MT202 - Financial Institution Transfer (interbank).
     */
    public SwiftMessage generateMT202(String senderBic, String receiverBic, String ref,
                                       BigDecimal amount, String currency, LocalDate valueDate) {
        log.info("Generating MT202 from {} to {}", senderBic, receiverBic);
        String body = String.format(
                ":20:%s\n:21:%s\n:32A:%s%s%s\n:53A:%s\n:57A:%s\n:58A:%s",
                ref, ref + "-REL",
                valueDate.format(DATE_FMT), currency, amount.toPlainString().replace(".", ","),
                senderBic, receiverBic, receiverBic);

        return SwiftMessage.builder()
                .messageType("MT202")
                .senderBic(senderBic)
                .receiverBic(receiverBic)
                .transactionReference(ref)
                .valueDate(valueDate.format(DATE_FMT))
                .amount(amount.toPlainString())
                .currency(currency)
                .messageBody(body)
                .generatedAt(LocalDateTime.now())
                .isMx(false)
                .build();
    }

    /**
     * Generate MT540 - Receive Free (securities settlement).
     */
    public SwiftMessage generateMT540(String senderBic, String receiverBic, String ref,
                                       String isin, BigDecimal quantity, LocalDate settlementDate,
                                       String safekeepingAccount) {
        log.info("Generating MT540 (Receive Free) for ISIN: {}", isin);
        String body = String.format(
                ":16R:GENL\n:20C::SEME//%s\n:16R:LINK\n:16S:LINK\n:16S:GENL\n" +
                ":16R:TRADDET\n:98A::SETT//%s\n:35B:ISIN %s\n:16S:TRADDET\n" +
                ":16R:FIAC\n:36B::SETT//UNIT/%s\n:97A::SAFE//%s\n:16S:FIAC",
                ref, settlementDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                isin, quantity.toPlainString(), safekeepingAccount);

        return SwiftMessage.builder()
                .messageType("MT540")
                .senderBic(senderBic)
                .receiverBic(receiverBic)
                .transactionReference(ref)
                .valueDate(settlementDate.format(DATE_FMT))
                .amount(quantity.toPlainString())
                .currency("N/A")
                .messageBody(body)
                .generatedAt(LocalDateTime.now())
                .isMx(false)
                .build();
    }

    /**
     * Generate MT541 - Receive Against Payment (DvP).
     */
    public SwiftMessage generateMT541(String senderBic, String receiverBic, String ref,
                                       String isin, BigDecimal quantity, BigDecimal settlementAmount,
                                       String currency, LocalDate settlementDate,
                                       String safekeepingAccount) {
        log.info("Generating MT541 (Receive vs Payment) for ISIN: {}", isin);
        String body = String.format(
                ":16R:GENL\n:20C::SEME//%s\n:16S:GENL\n" +
                ":16R:TRADDET\n:98A::SETT//%s\n:35B:ISIN %s\n:16S:TRADDET\n" +
                ":16R:FIAC\n:36B::SETT//UNIT/%s\n:97A::SAFE//%s\n:16S:FIAC\n" +
                ":16R:SETDET\n:19A::SETT//%s%s\n:16S:SETDET",
                ref, settlementDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                isin, quantity.toPlainString(), safekeepingAccount,
                currency, settlementAmount.toPlainString().replace(".", ","));

        return SwiftMessage.builder()
                .messageType("MT541")
                .senderBic(senderBic)
                .receiverBic(receiverBic)
                .transactionReference(ref)
                .valueDate(settlementDate.format(DATE_FMT))
                .amount(settlementAmount.toPlainString())
                .currency(currency)
                .messageBody(body)
                .generatedAt(LocalDateTime.now())
                .isMx(false)
                .build();
    }
}
