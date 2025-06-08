package com.auction.messaging;

import com.auction.session.AuctionManagerSingleton;
import jakarta.ejb.*;
import jakarta.jms.*;


import java.util.logging.Logger;

@MessageDriven(
        activationConfig = {
                @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
                @ActivationConfigProperty(propertyName = "destination", propertyValue = "java:comp/env/jms/BidTopic"),
                @ActivationConfigProperty(propertyName = "subscriptionDurability", propertyValue = "Durable"),
                @ActivationConfigProperty(propertyName = "clientId", propertyValue = "BidNotificationClient"),
                @ActivationConfigProperty(propertyName = "subscriptionName", propertyValue = "BidNotificationSubscription")
        }
)
public class BidNotificationMDB implements MessageListener {

    private static final Logger logger = Logger.getLogger(BidNotificationMDB.class.getName());

    @EJB
    private AuctionManagerSingleton auctionManager;

    @Override
    public void onMessage(Message message) {
        try {
            if (message instanceof ObjectMessage) {
                ObjectMessage objMessage = (ObjectMessage) message;
                Object messageBody = objMessage.getObject();

                if (messageBody instanceof BidMessage) {
                    BidMessage bidMessage = (BidMessage) messageBody;
                    processBidNotification(bidMessage);
                } else {
                    logger.warning("Received unexpected message type: " + messageBody.getClass().getName());
                }
            } else {
                logger.warning("Received non-ObjectMessage: " + message.getClass().getName());
            }
        } catch (JMSException e) {
            logger.severe("Error processing bid notification: " + e.getMessage());
        } catch (Exception e) {
            logger.severe("Unexpected error in BidNotificationMDB: " + e.getMessage());
        }
    }

    private void processBidNotification(BidMessage bidMessage) {
        try {
            logger.info("Processing bid notification: " + bidMessage);

            auctionManager.incrementBidCount(bidMessage.getAuctionId());

            broadcastToWebClients(bidMessage);

            logger.info("Bid notification processed successfully for auction: " + bidMessage.getAuctionId());

        } catch (Exception e) {
            logger.severe("Error processing bid notification: " + e.getMessage());
            throw new RuntimeException("Failed to process bid notification", e);
        }
    }

    private void broadcastToWebClients(BidMessage bidMessage) {

        logger.info("Broadcasting bid update to web clients for auction: " + bidMessage.getAuctionId() +
                ", amount: " + bidMessage.getBidAmount());
    }
}
