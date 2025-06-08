package com.auction.messaging;

import com.auction.session.AuctionManagerSingleton;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import java.util.logging.Logger;

@MessageDriven(
        activationConfig = {
                @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
                @ActivationConfigProperty(propertyName = "destination", propertyValue = "java:comp/env/jms/AuctionEventQueue")
        }
)
public class AuctionEventMDB implements MessageListener {

    private static final Logger logger = Logger.getLogger(AuctionEventMDB.class.getName());

    @EJB
    private AuctionManagerSingleton auctionManager;

    @Override
    public void onMessage(Message message) {
        try {
            if (message instanceof TextMessage) {
                TextMessage textMessage = (TextMessage) message;
                String eventType = message.getStringProperty("eventType");
                String auctionId = message.getStringProperty("auctionId");
                String category = message.getStringProperty("category");

                processAuctionEvent(eventType, auctionId, category, textMessage.getText());
            } else {
                logger.warning("Received non-TextMessage: " + message.getClass().getName());
            }
        } catch (JMSException e) {
            logger.severe("Error processing auction event: " + e.getMessage());
        } catch (Exception e) {
            logger.severe("Unexpected error in AuctionEventMDB: " + e.getMessage());
        }
    }

    private void processAuctionEvent(String eventType, String auctionId, String category, String messageText) {
        try {
            logger.info("Processing auction event: " + eventType + " for auction: " + auctionId);

            switch (eventType) {
                case "AUCTION_CREATED":
                    handleAuctionCreated(auctionId, category);
                    break;
                case "AUCTION_STARTED":
                    handleAuctionStarted(auctionId);
                    break;
                case "AUCTION_ENDED":
                    handleAuctionEnded(auctionId);
                    break;
                case "AUCTION_CANCELLED":
                    handleAuctionCancelled(auctionId);
                    break;
                default:
                    logger.warning("Unknown event type: " + eventType);
            }

        } catch (Exception e) {
            logger.severe("Error processing auction event: " + e.getMessage());
            throw new RuntimeException("Failed to process auction event", e);
        }
    }

    private void handleAuctionCreated(String auctionId, String category) {
        if (category != null) {
            auctionManager.incrementCategoryCounter(category);
        }
        logger.info("Handled auction creation for auction: " + auctionId);
    }

    private void handleAuctionStarted(String auctionId) {
        // Initialize bid count for the auction
        if (auctionId != null) {
            auctionManager.incrementBidCount(Long.parseLong(auctionId));
        }
        logger.info("Handled auction start for auction: " + auctionId);
    }

    private void handleAuctionEnded(String auctionId) {
        // Clean up could be handled here or by the singleton's scheduled method
        logger.info("Handled auction end for auction: " + auctionId);
    }

    private void handleAuctionCancelled(String auctionId) {
        logger.info("Handled auction cancellation for auction: " + auctionId);
    }
}