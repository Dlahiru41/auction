package com.auction.session;

import com.auction.entity.Bid;
import jakarta.ejb.Local;

import java.math.BigDecimal;
import java.util.List;

@Local
public interface BidSessionBeanLocal {
    Bid placeBid(Long auctionId, Long bidderId, BigDecimal amount, String ipAddress);
    List<Bid> findBidsByUser(Long userId);
    BigDecimal getMinimumBidAmount(Long auctionId);
    Bid findHighestBid(Long auctionId);
    List<Bid> findBidsByAuction(Long auctionId);
}