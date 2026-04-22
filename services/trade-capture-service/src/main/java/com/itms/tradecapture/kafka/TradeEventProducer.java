package com.itms.tradecapture.kafka;

import com.itms.tradecapture.model.Trade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class TradeEventProducer {

    private static final String TOPIC_TRADE_CREATED = "trade.created";
    private static final String TOPIC_TRADE_STATUS_CHANGED = "trade.status.changed";
    private static final String TOPIC_TRADE_AMENDED = "trade.amended";
    private static final String TOPIC_TRADE_CANCELLED = "trade.cancelled";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishTradeCreatedEvent(Trade trade) {
        log.info("Publishing trade created event: {}", trade.getTradeReference());
        kafkaTemplate.send(TOPIC_TRADE_CREATED, trade.getTradeReference(), trade);
    }

    public void publishTradeStatusChangedEvent(Trade trade) {
        log.info("Publishing trade status changed event: {} -> {}", trade.getTradeReference(), trade.getStatus());
        kafkaTemplate.send(TOPIC_TRADE_STATUS_CHANGED, trade.getTradeReference(), trade);
    }

    public void publishTradeAmendedEvent(Trade trade) {
        log.info("Publishing trade amended event: {}", trade.getTradeReference());
        kafkaTemplate.send(TOPIC_TRADE_AMENDED, trade.getTradeReference(), trade);
    }

    public void publishTradeCancelledEvent(Trade trade) {
        log.info("Publishing trade cancelled event: {}", trade.getTradeReference());
        kafkaTemplate.send(TOPIC_TRADE_CANCELLED, trade.getTradeReference(), trade);
    }
}
