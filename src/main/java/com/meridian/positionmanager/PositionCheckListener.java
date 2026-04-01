package com.meridian.positionmanager;

import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.listener.adapter.JmsResponse;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Request/reply server for position check requests.
 * Listens on POS.CHECK.REQ and replies to the caller's replyTo destination.
 */
@Component
public class PositionCheckListener {

    private static final Logger log = LoggerFactory.getLogger(PositionCheckListener.class);

    private final Map<String, Integer> positionBook = new ConcurrentHashMap<>();

    public PositionCheckListener() {
        // Seed some demo positions
        positionBook.put("AAPL", 5000);
        positionBook.put("MSFT", 12000);
        positionBook.put("GOOGL", 3000);
        positionBook.put("TSLA", 800);
    }

    @JmsListener(destination = "POS.CHECK.REQ")
    public JmsResponse<String> handlePositionCheck(Message message) throws JMSException {
        String correlationId = message.getJMSCorrelationID();
        Destination replyTo = message.getJMSReplyTo();

        String body = "";
        if (message instanceof TextMessage textMessage) {
            body = textMessage.getText();
        }

        log.info("Position check request received: correlationId={}, body={}", correlationId, body);

        // Look up position for the requested instrument
        String instrument = extractInstrument(body);
        int availableQuantity = positionBook.getOrDefault(instrument, 0);
        int requestedQuantity = extractQuantity(body);
        boolean sufficient = availableQuantity >= requestedQuantity;

        String reply = "{\"requestId\":\"" + correlationId + "\","
                + "\"instrument\":\"" + instrument + "\","
                + "\"sufficient\":" + sufficient + ","
                + "\"availableQuantity\":" + availableQuantity + ","
                + "\"requestedQuantity\":" + requestedQuantity + "}";

        log.info("Position check reply: correlationId={}, sufficient={}, available={}", correlationId, sufficient, availableQuantity);

        return JmsResponse.forDestination(reply, replyTo);
    }

    private String extractInstrument(String json) {
        // Simple JSON field extraction — production code would use Jackson
        int idx = json.indexOf("\"instrument\"");
        if (idx == -1) return "UNKNOWN";
        int start = json.indexOf("\"", idx + 12) + 1;
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }

    private int extractQuantity(String json) {
        int idx = json.indexOf("\"quantity\"");
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
