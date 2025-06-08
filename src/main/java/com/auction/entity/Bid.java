package com.auction.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "bids", indexes = {
        @Index(name = "idx_auction_bid_time", columnList = "auction_id, bid_time"),
        @Index(name = "idx_bidder_bid_time", columnList = "bidder_id, bid_time")
})
@NamedQueries({
        @NamedQuery(name = "Bid.findByAuction",
                query = "SELECT b FROM Bid b WHERE b.auction = :auction ORDER BY b.bidTime DESC"),
        @NamedQuery(name = "Bid.findHighestBid",
                query = "SELECT b FROM Bid b WHERE b.auction = :auction AND b.amount = (SELECT MAX(b2.amount) FROM Bid b2 WHERE b2.auction = :auction)")
})
public class Bid implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bidId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bidder_id", nullable = false)
    private User bidder;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date bidTime;

    @Column
    private String bidderIpAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BidStatus status = BidStatus.ACTIVE;

    // Constructors
    public Bid() {
        this.bidTime = new Date();
    }

    public Bid(Auction auction, User bidder, BigDecimal amount) {
        this();
        this.auction = auction;
        this.bidder = bidder;
        this.amount = amount;
    }

    // Getters and Setters
    public Long getBidId() { return bidId; }
    public void setBidId(Long bidId) { this.bidId = bidId; }

    public Auction getAuction() { return auction; }
    public void setAuction(Auction auction) { this.auction = auction; }

    public User getBidder() { return bidder; }
    public void setBidder(User bidder) { this.bidder = bidder; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public Date getBidTime() { return bidTime; }
    public void setBidTime(Date bidTime) { this.bidTime = bidTime; }

    public String getBidderIpAddress() { return bidderIpAddress; }
    public void setBidderIpAddress(String bidderIpAddress) { this.bidderIpAddress = bidderIpAddress; }

    public BidStatus getStatus() { return status; }
    public void setStatus(BidStatus status) { this.status = status; }
}
