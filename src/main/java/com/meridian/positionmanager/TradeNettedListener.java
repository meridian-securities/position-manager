package com.meridian.positionmanager;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listens for netted trade events and updates the position book.
 * Consumes from TRADE.NETTED to maintain real-time position state.
 */
@Component
public class TradeNettedListener {

    private static final Logger log = LoggerFactory.getLogger(TradeNettedListener.class);

    private final Map<String, Integer> positionBook = new ConcurrentHashMap<>();

    @JmsListener(destination = "TRADE.NETTED")
    public void handleNettedTrade(Message message) throws JMSException {
        if (!(message instanceof TextMessage textMessage)) {
            log.warn("Received non-text message on TRADE.NETTED, ignoring");
            return;
        }

        String body = textMessage.getText();
        String messageId = message.getJMSMessageID();

        log.info("Netted trade received: messageId={}, body={}", messageId, body);

        // Extract trade details and update position book
        String instrument = extractField(body, "instrument");
        String side = extractField(body, "side");
        int nettedQuantity = extractIntField(body, "nettedQuantity");

        int delta = "BUY".equalsIgnoreCase(side) ? nettedQuantity : -nettedQuantity;
        int newPosition = positionBook.merge(instrument, delta, Integer::sum);

        log.info("Position updated: instrument={}, side={}, delta={}, newPosition={}",
                instrument, side, delta, newPosition);
    }

    private String extractField(String json, String field) {
        int idx = json.indexOf("\"" + field + "\"");
        if (idx == -1) return "";
        int start = json.indexOf("\"", idx + field.length() + 2) + 1;
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }

    private int extractIntField(String json, String field) {
        int idx = json.indexOf("\"" + field + "\"");
        if (idx == -1) return 0;
        int start = json.indexOf(":", idx) + 1;
        int end = json.length();
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == ',' || c == '}') {
                end = i;
                break;
            }
        }
        return Integer.parseInt(json.substring(start, end).trim());
    }
}
