package com.auction.session;

import com.auction.entity.AuctionStatus;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;


import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

@Singleton
@Startup
@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)
@Lock(LockType.READ)
public class AuctionManagerSingleton {

    private static final Logger logger = Logger.getLogger(AuctionManagerSingleton.class.getName());

    @PersistenceContext(unitName = "auctionPU")
    private EntityManager em;

    // Shared application state
    private final ConcurrentHashMap<Long, Integer> activeAuctionBidCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> categoryCounters = new ConcurrentHashMap<>();
    private volatile boolean systemMaintenance = false;
    private volatile Date lastSystemUpdate;

    @PostConstruct
    public void initialize() {
        logger.info("Initializing Auction Manager Singleton...");
        loadActiveAuctionStats();
        initializeCategoryCounters();
        lastSystemUpdate = new Date();
        logger.info("Auction Manager Singleton initialized successfully");
    }

    @PreDestroy
    public void cleanup() {
        logger.info("Shutting down Auction Manager Singleton...");
        activeAuctionBidCounts.clear();
        categoryCounters.clear();
    }

    @Lock(LockType.WRITE)
    public void incrementBidCount(Long auctionId) {
        activeAuctionBidCounts.merge(auctionId, 1, Integer::sum);
        logger.fine("Incremented bid count for auction " + auctionId +
                " to " + activeAuctionBidCounts.get(auctionId));
    }

    public Integer getBidCount(Long auctionId) {
        return activeAuctionBidCounts.getOrDefault(auctionId, 0);
    }

    @Lock(LockType.WRITE)
    public void incrementCategoryCounter(String category) {
        categoryCounters.computeIfAbsent(category, k -> new AtomicInteger(0)).incrementAndGet();
    }

    public int getCategoryCount(String category) {
        AtomicInteger counter = categoryCounters.get(category);
        return counter != null ? counter.get() : 0;
    }

    @Lock(LockType.WRITE)
    public void setSystemMaintenance(boolean maintenance) {
        this.systemMaintenance = maintenance;
        logger.info("System maintenance mode set to: " + maintenance);
    }

    public boolean isSystemMaintenance() {
        return systemMaintenance;
    }

    @Schedule(hour = "*", minute = "*/5", persistent = false)
    @Lock(LockType.WRITE)
    public void performPeriodicMaintenance() {
        try {
            logger.info("Performing periodic auction maintenance...");

            // End expired auctions
            endExpiredAuctions();

            // Clean up old bid counts for ended auctions
            cleanupEndedAuctionStats();

            // Update system timestamp
            lastSystemUpdate = new Date();

            logger.info("Periodic maintenance completed successfully");
        } catch (Exception e) {
            logger.severe("Error during periodic maintenance: " + e.getMessage());
        }
    }

    private void endExpiredAuctions() {
        Query query = em.createQuery(
                "UPDATE Auction a SET a.status = :endedStatus " +
                        "WHERE a.status = :activeStatus AND a.endTime < :currentTime"
        );
        query.setParameter("endedStatus", AuctionStatus.ENDED);
        query.setParameter("activeStatus", AuctionStatus.ACTIVE);
        query.setParameter("currentTime", new Date());

        int updatedCount = query.executeUpdate();
        if (updatedCount > 0) {
            logger.info("Ended " + updatedCount + " expired auctions");
        }
    }

    private void cleanupEndedAuctionStats() {
        Query query = em.createQuery(
                "SELECT a.auctionId FROM Auction a WHERE a.status = :endedStatus OR a.status = :cancelledStatus"
        );
        query.setParameter("endedStatus", AuctionStatus.ENDED);
        query.setParameter("cancelledStatus", AuctionStatus.CANCELLED);

        @SuppressWarnings("unchecked")
        List<Long> endedAuctionIds = query.getResultList();

        for (Long auctionId : endedAuctionIds) {
            activeAuctionBidCounts.remove(auctionId);
        }
    }

    private void loadActiveAuctionStats() {
        Query query = em.createQuery(
                "SELECT a.auctionId, COUNT(b.bidId) FROM Auction a " +
                        "LEFT JOIN a.bids b " +
                        "WHERE a.status = :activeStatus " +
                        "GROUP BY a.auctionId"
        );
        query.setParameter("activeStatus", AuctionStatus.ACTIVE);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        for (Object[] result : results) {
            Long auctionId = (Long) result[0];
            Long bidCount = (Long) result[1];
            activeAuctionBidCounts.put(auctionId, bidCount.intValue());
        }

        logger.info("Loaded stats for " + activeAuctionBidCounts.size() + " active auctions");
    }

    private void initializeCategoryCounters() {
        Query query = em.createQuery(
                "SELECT a.category, COUNT(a.auctionId) FROM Auction a " +
                        "WHERE a.status = :activeStatus " +
                        "GROUP BY a.category"
        );
        query.setParameter("activeStatus", AuctionStatus.ACTIVE);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        for (Object[] result : results) {
            String category = (String) result[0];
            Long count = (Long) result[1];
            categoryCounters.put(category, new AtomicInteger(count.intValue()));
        }

        logger.info("Initialized category counters for " + categoryCounters.size() + " categories");
    }

    public Date getLastSystemUpdate() {
        return lastSystemUpdate;
    }

    public ConcurrentHashMap<String, Integer> getAllCategoryCounts() {
        ConcurrentHashMap<String, Integer> result = new ConcurrentHashMap<>();
        categoryCounters.forEach((category, counter) -> result.put(category, counter.get()));
        return result;
    }

    public int getTotalActiveBids() {
        return activeAuctionBidCounts.values().stream().mapToInt(Integer::intValue).sum();
    }
}

