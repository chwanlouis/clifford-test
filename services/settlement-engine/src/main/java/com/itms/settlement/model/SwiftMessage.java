package com.itms.settlement.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SwiftMessage {
    private String messageType;     // e.g. MT103, MT202, MT540, MT541
    private String senderBic;
    private String receiverBic;
    private String transactionReference;
    private String valueDate;
    private String amount;
    private String currency;
    private String messageBody;
    private LocalDateTime generatedAt;
    private boolean isMx;           // true for ISO 20022 MX messages
}
