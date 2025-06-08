package com.auction.session;

import com.auction.entity.Auction;
import com.auction.entity.AuctionStatus;
import com.auction.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import javax.ejb.Stateless;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

@Stateless
public class AuctionSessionBean implements AuctionSessionBeanLocal, AuctionSessionBeanRemote {

    private static final Logger logger = Logger.getLogger(AuctionSessionBean.class.getName());

    @PersistenceContext(unitName = "auctionPU")
    private EntityManager em;

    @Override
    public Auction createAuction(String title, String description, String category,
                                 BigDecimal startingPrice, Date startTime, Date endTime, User seller) {
        try {
            Auction auction = new Auction(title, description, category, startingPrice, startTime, endTime, seller);
            em.persist(auction);
            em.flush();

            logger.info("Created new auction: " + title);
            return auction;
        } catch (Exception e) {
            logger.severe("Error creating auction: " + e.getMessage());
            throw new RuntimeException("Failed to create auction", e);
        }
    }

    @Override
    public List<Auction> findActiveAuctions() {
        Query query = em.createNamedQuery("Auction.findActive");
        return query.getResultList();
    }

    @Override
    public List<Auction> findAuctionsByCategory(String category) {
        Query query = em.createNamedQuery("Auction.findByCategory");
        query.setParameter("category", category);
        return query.getResultList();
    }

    @Override
    public List<Auction> findAuctionsEndingSoon(Date endTime) {
        Query query = em.createNamedQuery("Auction.findEndingSoon");
        query.setParameter("endTime", endTime);
        return query.getResultList();
    }

    @Override
    public Auction findAuctionById(Long auctionId) {
        return em.find(Auction.class, auctionId);
    }

    @Override
    public Auction updateAuction(Auction auction) {
        return em.merge(auction);
    }

    @Override
    public void startAuction(Long auctionId) {
        Auction auction = findAuctionById(auctionId);
        if (auction != null && auction.getStatus() == AuctionStatus.PENDING) {
            auction.setStatus(AuctionStatus.ACTIVE);
            em.merge(auction);
            logger.info("Started auction: " + auctionId);
        }
    }

    @Override
    public void endAuction(Long auctionId) {
        Auction auction = findAuctionById(auctionId);
        if (auction != null && auction.getStatus() == AuctionStatus.ACTIVE) {
            auction.setStatus(AuctionStatus.ENDED);
            em.merge(auction);
            logger.info("Ended auction: " + auctionId);
        }
    }

    @Override
    public void cancelAuction(Long auctionId) {
        Auction auction = findAuctionById(auctionId);
        if (auction != null) {
            auction.setStatus(AuctionStatus.CANCELLED);
            em.merge(auction);
            logger.info("Cancelled auction: " + auctionId);
        }
    }
}