package com.auction.session;

import com.auction.entity.Auction;
import com.auction.entity.User;
import jakarta.ejb.Local;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Local
public interface AuctionSessionBeanLocal {
    Auction createAuction(String title, String description, String category,
                          BigDecimal startingPrice, Date startTime, Date endTime, User seller);
    List<Auction> findActiveAuctions();
    List<Auction> findAuctionsByCategory(String category);
    List<Auction> findAuctionsEndingSoon(Date endTime);
    Auction findAuctionById(Long auctionId);
    Auction updateAuction(Auction auction);
    void startAuction(Long auctionId);
    void endAuction(Long auctionId);
    void cancelAuction(Long auctionId);
}

