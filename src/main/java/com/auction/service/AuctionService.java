package com.auction.service;

import com.auction.entity.Auction;
import com.auction.entity.Bid;
import com.auction.entity.User;
import com.auction.session.AuctionManagerSingleton;
import com.auction.session.AuctionSessionBeanLocal;
import com.auction.session.BidSessionBeanLocal;
import com.auction.session.UserSessionBeanLocal;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class AuctionService {

    private static final Logger logger = Logger.getLogger(AuctionService.class.getName());

    @EJB
    private UserSessionBeanLocal userSession;

    @EJB
    private AuctionSessionBeanLocal auctionSession;

    @EJB
    private BidSessionBeanLocal bidSession;

    @EJB
    private AuctionManagerSingleton auctionManager;

    public Auction createAuction(String title, String description, String category,
                                 BigDecimal startingPrice, Date startTime, Date endTime, Long sellerId) {
        try {
            // Check system maintenance
            if (auctionManager.isSystemMaintenance()) {
                throw new IllegalStateException("System is under maintenance. Please try again later.");
            }

            User seller = userSession.findUserById(sellerId);
            if (seller == null || !seller.isActive()) {
                throw new IllegalArgumentException("Invalid or inactive seller");
            }

            // Validate auction times
            validateAuctionTimes(startTime, endTime);

            Auction auction = auctionSession.createAuction(title, description, category,
                    startingPrice, startTime, endTime, seller);

            // Update category counter
            auctionManager.incrementCategoryCounter(category);

            logger.info("Auction created successfully: " + auction.getAuctionId());
            return auction;

        } catch (Exception e) {
            logger.severe("Error creating auction: " + e.getMessage());
            throw new RuntimeException("Failed to create auction", e);
        }
    }

    public Bid placeBid(Long auctionId, Long bidderId, BigDecimal amount, String ipAddress) {
        try {
            // Check system maintenance
            if (auctionManager.isSystemMaintenance()) {
                throw new IllegalStateException("System is under maintenance. Please try again later.");
            }

            Bid bid = bidSession.placeBid(auctionId, bidderId, amount, ipAddress);

            logger.info("Bid placed successfully: " + amount + " on auction " + auctionId + " by user " + bidderId);
            return bid;

        } catch (Exception e) {
            logger.severe("Error placing bid: " + e.getMessage());
            throw new RuntimeException("Failed to place bid", e);
        }
    }

    public List<Auction> getActiveAuctions() {
        return auctionSession.findActiveAuctions();
    }

    public List<Auction> getAuctionsByCategory(String category) {
        return auctionSession.findAuctionsByCategory(category);
    }

    public Auction getAuctionDetails(Long auctionId) {
        return auctionSession.findAuctionById(auctionId);
    }

    public List<Bid> getAuctionBids(Long auctionId) {
        return bidSession.findBidsByAuction(auctionId);
    }

    public Bid getHighestBid(Long auctionId) {
        return bidSession.findHighestBid(auctionId);
    }

    public BigDecimal getMinimumBidAmount(Long auctionId) {
        return bidSession.getMinimumBidAmount(auctionId);
    }

    public int getAuctionBidCount(Long auctionId) {
        return auctionManager.getBidCount(auctionId);
    }

    public User authenticateUser(String email, String password) {
        return userSession.authenticateUser(email, password);
    }

    public User registerUser(String email, String password, String firstName, String lastName) {
        return userSession.createUser(email, password, firstName, lastName);
    }

    private void validateAuctionTimes(Date startTime, Date endTime) {
        Date now = new Date();

        if (startTime.before(now)) {
            throw new IllegalArgumentException("Start time cannot be in the past");
        }

        if (endTime.before(startTime)) {
            throw new IllegalArgumentException("End time must be after start time");
        }

        // Minimum auction duration: 1 hour
        long duration = endTime.getTime() - startTime.getTime();
        if (duration < 3600000) { // 1 hour in milliseconds
            throw new IllegalArgumentException("Auction duration must be at least 1 hour");
        }

        // Maximum auction duration: 30 days
        if (duration > 2592000000L) { // 30 days in milliseconds
            throw new IllegalArgumentException("Auction duration cannot exceed 30 days");
        }
    }
}