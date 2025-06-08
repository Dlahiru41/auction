package com.auction.service;

import com.auction.entity.Auction;
import com.auction.entity.Bid;
import com.auction.entity.User;
import jakarta.ejb.Local;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Local
public interface AuctionService {
    Auction createAuction(String title, String description, String category,
                         BigDecimal startingPrice, Date startTime, Date endTime, Long sellerId);
    Bid placeBid(Long auctionId, Long bidderId, BigDecimal amount, String ipAddress);
    List<Auction> getActiveAuctions();
    List<Auction> getAuctionsByCategory(String category);
    Auction getAuctionDetails(Long auctionId);
    List<Bid> getAuctionBids(Long auctionId);
    Bid getHighestBid(Long auctionId);
    BigDecimal getMinimumBidAmount(Long auctionId);
    int getAuctionBidCount(Long auctionId);
    User authenticateUser(String email, String password);
    User registerUser(String email, String password, String firstName, String lastName);
} 