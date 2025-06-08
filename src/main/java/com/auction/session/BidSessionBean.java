package com.auction.session;

import com.auction.entity.Auction;
import com.auction.entity.Bid;
import com.auction.entity.BidStatus;
import com.auction.entity.User;
import com.auction.messaging.BidMessage;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.jms.*;
import java.lang.IllegalStateException;
import java.math.BigDecimal;
import java.util.List;
import java.util.logging.Logger;

@Stateless
public class BidSessionBean implements BidSessionBeanLocal, BidSessionBeanRemote {

    private static final Logger logger = Logger.getLogger(BidSessionBean.class.getName());

    @PersistenceContext(unitName = "auctionPU")
    private EntityManager em;

    @EJB
    private AuctionSessionBeanLocal auctionSession;

    @Resource(mappedName = "java:comp/DefaultJMSConnectionFactory")
    private ConnectionFactory connectionFactory;

    @Resource(mappedName = "java:comp/env/jms/BidTopic")
    private Topic bidTopic;

    @Override
    public synchronized Bid placeBid(Long auctionId, Long bidderId, BigDecimal amount, String ipAddress) {
        try {
            Auction auction = auctionSession.findAuctionById(auctionId);
            if (auction == null) {
                throw new IllegalArgumentException("Auction not found");
            }

            if (!auction.isActive()) {
                throw new IllegalStateException("Auction is not active");
            }

            // Validate bid amount
            validateBidAmount(auction, amount);

            User bidder = em.find(User.class, bidderId);
            if (bidder == null) {
                throw new IllegalArgumentException("Bidder not found");
            }

            // Check if bidder is the seller
            if (auction.getSeller().getUserId().equals(bidderId)) {
                throw new IllegalArgumentException("Seller cannot bid on their own auction");
            }

            // Create and persist bid
            Bid bid = new Bid(auction, bidder, amount);
            bid.setBidderIpAddress(ipAddress);
            em.persist(bid);

            // Update auction current price
            auction.setCurrentPrice(amount);
            em.merge(auction);

            // Update previous bids status
            updatePreviousBidsStatus(auction, bid);

            em.flush();

            // Send JMS message for real-time updates
            sendBidNotification(bid);

            logger.info("Bid placed successfully: " + amount + " on auction " + auctionId);
            return bid;

        } catch (Exception e) {
            logger.severe("Error placing bid: " + e.getMessage());
            throw new RuntimeException("Failed to place bid", e);
        }
    }

    private void validateBidAmount(Auction auction, BigDecimal amount) {
        BigDecimal minimumBid = auction.getCurrentPrice().add(auction.getBidIncrement());
        if (amount.compareTo(minimumBid) < 0) {
            throw new IllegalArgumentException("Bid amount must be at least " + minimumBid);
        }
    }

    private void updatePreviousBidsStatus(Auction auction, Bid newBid) {
        Query query = em.createQuery("UPDATE Bid b SET b.status = :outbidStatus WHERE b.auction = :auction AND b.status = :activeStatus AND b.bidId != :newBidId");
        query.setParameter("outbidStatus", BidStatus.OUTBID);
        query.setParameter("auction", auction);
        query.setParameter("activeStatus", BidStatus.ACTIVE);
        query.setParameter("newBidId", newBid.getBidId());
        query.executeUpdate();

        // Set new bid as winning
        newBid.setStatus(BidStatus.WINNING);
    }

    private void sendBidNotification(Bid bid) {
        try (Connection connection = connectionFactory.createConnection();
             Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {

            MessageProducer producer = session.createProducer(bidTopic);

            BidMessage bidMessage = new BidMessage();
            bidMessage.setAuctionId(bid.getAuction().getAuctionId());
            bidMessage.setBidAmount(bid.getAmount());
            bidMessage.setBidderName(bid.getBidder().getFirstName() + " " + bid.getBidder().getLastName());
            bidMessage.setBidTime(bid.getBidTime());

            ObjectMessage message = session.createObjectMessage(bidMessage);
            message.setJMSType("BidUpdate");
            message.setStringProperty("auctionId", bid.getAuction().getAuctionId().toString());

            producer.send(message);
            logger.info("Bid notification sent for auction: " + bid.getAuction().getAuctionId());

        } catch (JMSException e) {
            logger.severe("Failed to send bid notification: " + e.getMessage());
        }
    }

    @Override
    public List<Bid> findBidsByAuction(Long auctionId) {
        Auction auction = auctionSession.findAuctionById(auctionId);
        if (auction == null) {
            throw new IllegalArgumentException("Auction not found");
        }

        Query query = em.createNamedQuery("Bid.findByAuction");
        query.setParameter("auction", auction);
        return query.getResultList();
    }

    @Override
    public Bid findHighestBid(Long auctionId) {
        try {
            Auction auction = auctionSession.findAuctionById(auctionId);
            if (auction == null) {
                return null;
            }

            Query query = em.createNamedQuery("Bid.findHighestBid");
            query.setParameter("auction", auction);
            return (Bid) query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public List<Bid> findBidsByUser(Long userId) {
        Query query = em.createQuery("SELECT b FROM Bid b WHERE b.bidder.userId = :userId ORDER BY b.bidTime DESC");
        query.setParameter("userId", userId);
        return query.getResultList();
    }

    @Override
    public BigDecimal getMinimumBidAmount(Long auctionId) {
        Auction auction = auctionSession.findAuctionById(auctionId);
        if (auction == null) {
            throw new IllegalArgumentException("Auction not found");
        }

        return auction.getCurrentPrice().add(auction.getBidIncrement());
    }
}
