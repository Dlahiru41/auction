package com.auction.session;

import com.auction.entity.Bid;

import javax.ejb.Remote;
import java.math.BigDecimal;
import java.util.List;

@Remote
public interface BidSessionBeanRemote {
    Bid placeBid(Long auctionId, Long bidderId, BigDecimal amount, String ipAddress);
    List<Bid> findBidsByAuction(Long auctionId);
    Bid findHighestBid(Long auctionId);
    List<Bid> findBidsByUser(Long userId);
    BigDecimal getMinimumBidAmount(Long auctionId);
}