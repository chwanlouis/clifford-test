package com.itms.tradecapture;

import com.itms.tradecapture.model.*;
import com.itms.tradecapture.service.TradeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class TradeCaptureApplicationTest {

    @Autowired
    private TradeService tradeService;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void contextLoads() {
    }

    @Test
    void captureTradeSuccess() {
        Trade trade = Trade.builder()
                .instrument("AAPL")
                .isin("US0378331005")
                .side(TradeSide.BUY)
                .quantity(new BigDecimal("100"))
                .price(new BigDecimal("150.00"))
                .currency("USD")
                .counterparty("Goldman Sachs")
                .trader("trader1")
                .portfolio("PORT-001")
                .assetClass(AssetClass.EQUITY)
                .source(TradeSource.MANUAL)
                .tradeDate(LocalDateTime.now())
                .settlementDate(LocalDateTime.now().plusDays(2))
                .build();

        Trade captured = tradeService.captureTrade(trade);

        assertThat(captured.getId()).isNotNull();
        assertThat(captured.getTradeReference()).startsWith("TRD-");
        assertThat(captured.getStatus()).isEqualTo(TradeStatus.PENDING_VALIDATION);
    }
}
