package com.auction.messaging;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

public class BidMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long auctionId;
    private BigDecimal bidAmount;
    private String bidderName;
    private Date bidTime;
    private String messageType = "BID_UPDATE";

    public BidMessage() {}

    public BidMessage(Long auctionId, BigDecimal bidAmount, String bidderName, Date bidTime) {
        this.auctionId = auctionId;
        this.bidAmount = bidAmount;
        this.bidderName = bidderName;
        this.bidTime = bidTime;
    }

    // Getters and Setters
    public Long getAuctionId() { return auctionId; }
    public void setAuctionId(Long auctionId) { this.auctionId = auctionId; }

    public BigDecimal getBidAmount() { return bidAmount; }
    public void setBidAmount(BigDecimal bidAmount) { this.bidAmount = bidAmount; }

    public String getBidderName() { return bidderName; }
    public void setBidderName(String bidderName) { this.bidderName = bidderName; }

    public Date getBidTime() { return bidTime; }
    public void setBidTime(Date bidTime) { this.bidTime = bidTime; }

    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }

    @Override
    public String toString() {
        return "BidMessage{" +
                "auctionId=" + auctionId +
                ", bidAmount=" + bidAmount +
                ", bidderName='" + bidderName + '\'' +
                ", bidTime=" + bidTime +
                ", messageType='" + messageType + '\'' +
                '}';
    }
}
