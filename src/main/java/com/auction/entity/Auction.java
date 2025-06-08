package com.auction.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "auctions")
@NamedQueries({
        @NamedQuery(name = "Auction.findActive",
                query = "SELECT a FROM Auction a WHERE a.status = 'ACTIVE' AND a.endTime > CURRENT_TIMESTAMP"),
        @NamedQuery(name = "Auction.findByCategory",
                query = "SELECT a FROM Auction a WHERE a.category = :category AND a.status = 'ACTIVE'"),
        @NamedQuery(name = "Auction.findEndingSoon",
                query = "SELECT a FROM Auction a WHERE a.status = 'ACTIVE' AND a.endTime BETWEEN CURRENT_TIMESTAMP AND :endTime")
})
public class Auction implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long auctionId;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal startingPrice;

    @Column(precision = 10, scale = 2)
    private BigDecimal currentPrice;

    @Column(precision = 10, scale = 2)
    private BigDecimal reservePrice;

    @Column(precision = 5, scale = 2)
    private BigDecimal bidIncrement = new BigDecimal("1.00");

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date startTime;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuctionStatus status = AuctionStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @OneToMany(mappedBy = "auction", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("bidTime DESC")
    private List<Bid> bids;

    @Version
    private Long version;

    // Constructors
    public Auction() {}

    public Auction(String title, String description, String category,
                   BigDecimal startingPrice, Date startTime, Date endTime, User seller) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.startingPrice = startingPrice;
        this.currentPrice = startingPrice;
        this.startTime = startTime;
        this.endTime = endTime;
        this.seller = seller;
    }

    // Business Methods
    public boolean isActive() {
        return status == AuctionStatus.ACTIVE && new Date().before(endTime);
    }

    public boolean hasReachedReserve() {
        return reservePrice == null ||
                (currentPrice != null && currentPrice.compareTo(reservePrice) >= 0);
    }

    // Getters and Setters
    public Long getAuctionId() { return auctionId; }
    public void setAuctionId(Long auctionId) { this.auctionId = auctionId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public BigDecimal getStartingPrice() { return startingPrice; }
    public void setStartingPrice(BigDecimal startingPrice) { this.startingPrice = startingPrice; }

    public BigDecimal getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }

    public BigDecimal getReservePrice() { return reservePrice; }
    public void setReservePrice(BigDecimal reservePrice) { this.reservePrice = reservePrice; }

    public BigDecimal getBidIncrement() { return bidIncrement; }
    public void setBidIncrement(BigDecimal bidIncrement) { this.bidIncrement = bidIncrement; }

    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }

    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }

    public AuctionStatus getStatus() { return status; }
    public void setStatus(AuctionStatus status) { this.status = status; }

    public User getSeller() { return seller; }
    public void setSeller(User seller) { this.seller = seller; }

    public List<Bid> getBids() { return bids; }
    public void setBids(List<Bid> bids) { this.bids = bids; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}