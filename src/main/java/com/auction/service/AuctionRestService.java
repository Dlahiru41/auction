package com.auction.service;

import com.auction.entity.Auction;
import com.auction.entity.Bid;
import com.auction.entity.User;
import com.auction.session.AuctionManagerSingleton;
import jakarta.enterprise.context.RequestScoped;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Path("/auctions")
@Stateless
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuctionRestService {

    private static final Logger logger = Logger.getLogger(AuctionRestService.class.getName());
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @EJB
    private AuctionService auctionService;

    @EJB
    private AuctionManagerSingleton auctionManager;

    @Context
    private HttpServletRequest request;

    /**
     * Get all active auctions
     * GET /api/auctions/
     */
    @GET
    @Path("/")
    public Response getActiveAuctions() {
        try {
            List<Auction> auctions = auctionService.getActiveAuctions();

            JsonObjectBuilder responseBuilder = Json.createObjectBuilder()
                    .add("success", true)
                    .add("count", auctions.size())
                    .add("timestamp", dateFormat.format(new Date()));

            // Convert auctions to JSON array
            JsonArrayBuilder auctionsArrayBuilder = Json.createArrayBuilder();
            for (Auction auction : auctions) {
                JsonObject auctionJson = Json.createObjectBuilder()
                        .add("id", auction.getAuctionId())
                        .add("title", auction.getTitle())
                        .add("description", auction.getDescription() != null ? auction.getDescription() : "")
                        .add("category", auction.getCategory())
                        .add("currentPrice", auction.getCurrentPrice().toString())
                        .add("startingPrice", auction.getStartingPrice().toString())
                        .add("bidIncrement", auction.getBidIncrement().toString())
                        .add("bidCount", auctionManager.getBidCount(auction.getAuctionId()))
                        .add("status", auction.getStatus().toString())
                        .add("endTime", dateFormat.format(auction.getEndTime()))
                        .add("timeRemaining", calculateTimeRemaining(auction.getEndTime()))
                        .add("seller", auction.getSeller().getFirstName() + " " + auction.getSeller().getLastName())
                        .build();
                auctionsArrayBuilder.add(auctionJson);
            }

            responseBuilder.add("auctions", auctionsArrayBuilder.build());

            return Response.ok(responseBuilder.build()).build();

        } catch (Exception e) {
            logger.severe("Error retrieving active auctions: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Json.createObjectBuilder()
                            .add("success", false)
                            .add("error", "Failed to retrieve auctions")
                            .add("timestamp", dateFormat.format(new Date()))
                            .build())
                    .build();
        }
    }

    /**
     * Get auctions by category
     * GET /api/auctions/category/{category}
     */
    @GET
    @Path("/category/{category}")
    public Response getAuctionsByCategory(@PathParam("category") String category) {
        try {
            List<Auction> auctions = auctionService.getAuctionsByCategory(category);

            JsonArrayBuilder auctionsArrayBuilder = Json.createArrayBuilder();
            for (Auction auction : auctions) {
                JsonObject auctionJson = createAuctionSummaryJson(auction);
                auctionsArrayBuilder.add(auctionJson);
            }

            JsonObject response = Json.createObjectBuilder()
                    .add("success", true)
                    .add("category", category)
                    .add("count", auctions.size())
                    .add("auctions", auctionsArrayBuilder.build())
                    .add("timestamp", dateFormat.format(new Date()))
                    .build();

            return Response.ok(response).build();

        } catch (Exception e) {
            logger.severe("Error retrieving auctions by category: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createErrorResponse("Failed to retrieve auctions by category"))
                    .build();
        }
    }

    /**
     * Get detailed auction information
     * GET /api/auctions/{id}
     */
    @GET
    @Path("/{id}")
    public Response getAuctionDetails(@PathParam("id") Long auctionId) {
        try {
            Auction auction = auctionService.getAuctionDetails(auctionId);
            if (auction == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Json.createObjectBuilder()
                                .add("success", false)
                                .add("error", "Auction not found")
                                .add("timestamp", dateFormat.format(new Date()))
                                .build())
                        .build();
            }

            Bid highestBid = auctionService.getHighestBid(auctionId);
            BigDecimal minimumBid = auctionService.getMinimumBidAmount(auctionId);
            List<Bid> recentBids = auctionService.getAuctionBids(auctionId);

            // Build recent bids array (last 5 bids)
            JsonArrayBuilder recentBidsBuilder = Json.createArrayBuilder();
            int bidCount = Math.min(recentBids.size(), 5);
            for (int i = 0; i < bidCount; i++) {
                Bid bid = recentBids.get(i);
                JsonObject bidJson = Json.createObjectBuilder()
                        .add("amount", bid.getAmount().toString())
                        .add("bidder", bid.getBidder().getFirstName() + " " +
                                bid.getBidder().getLastName().substring(0, 1) + ".")
                        .add("bidTime", dateFormat.format(bid.getBidTime()))
                        .add("status", bid.getStatus().toString())
                        .build();
                recentBidsBuilder.add(bidJson);
            }

            JsonObject response = Json.createObjectBuilder()
                    .add("success", true)
                    .add("auction", Json.createObjectBuilder()
                            .add("id", auction.getAuctionId())
                            .add("title", auction.getTitle())
                            .add("description", auction.getDescription() != null ? auction.getDescription() : "")
                            .add("category", auction.getCategory())
                            .add("startingPrice", auction.getStartingPrice().toString())
                            .add("currentPrice", auction.getCurrentPrice().toString())
                            .add("reservePrice", auction.getReservePrice() != null ?
                                    auction.getReservePrice().toString() : "Not set")
                            .add("bidIncrement", auction.getBidIncrement().toString())
                            .add("minimumBid", minimumBid.toString())
                            .add("bidCount", auctionManager.getBidCount(auctionId))
                            .add("totalBids", recentBids.size())
                            .add("status", auction.getStatus().toString())
                            .add("startTime", dateFormat.format(auction.getStartTime()))
                            .add("endTime", dateFormat.format(auction.getEndTime()))
                            .add("timeRemaining", calculateTimeRemaining(auction.getEndTime()))
                            .add("isActive", auction.isActive())
                            .add("hasReachedReserve", auction.hasReachedReserve())
                            .add("seller", Json.createObjectBuilder()
                                    .add("name", auction.getSeller().getFirstName() + " " +
                                            auction.getSeller().getLastName())
                                    .add("email", auction.getSeller().getEmail())
                                    .build())
                            .add("highestBidder", highestBid != null ?
                                    highestBid.getBidder().getFirstName() + " " +
                                            highestBid.getBidder().getLastName() : "None")
                            .add("recentBids", recentBidsBuilder.build())
                            .build())
                    .add("timestamp", dateFormat.format(new Date()))
                    .build();

            return Response.ok(response).build();

        } catch (Exception e) {
            logger.severe("Error retrieving auction details: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createErrorResponse("Failed to retrieve auction details"))
                    .build();
        }
    }

    /**
     * Place a bid on an auction
     * POST /api/auctions/{id}/bids
     */
    @POST
    @Path("/{id}/bids")
    public Response placeBid(@PathParam("id") Long auctionId, JsonObject bidData) {
        try {
            // Validate input data
            if (!bidData.containsKey("bidderId") || !bidData.containsKey("amount")) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(createErrorResponse("Missing required fields: bidderId and amount"))
                        .build();
            }

            // Extract bid information
            Long bidderId = Long.valueOf(bidData.getInt("bidderId"));
            BigDecimal amount = new BigDecimal(bidData.getString("amount"));
            String ipAddress = request.getRemoteAddr();

            // Validate bid amount
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(createErrorResponse("Bid amount must be positive"))
                        .build();
            }

            Bid bid = auctionService.placeBid(auctionId, bidderId, amount, ipAddress);

            JsonObject response = Json.createObjectBuilder()
                    .add("success", true)
                    .add("message", "Bid placed successfully")
                    .add("bid", Json.createObjectBuilder()
                            .add("id", bid.getBidId())
                            .add("auctionId", bid.getAuction().getAuctionId())
                            .add("amount", bid.getAmount().toString())
                            .add("bidTime", dateFormat.format(bid.getBidTime()))
                            .add("status", bid.getStatus().toString())
                            .add("bidder", bid.getBidder().getFirstName() + " " +
                                    bid.getBidder().getLastName())
                            .build())
                    .add("auctionUpdate", Json.createObjectBuilder()
                            .add("newCurrentPrice", bid.getAuction().getCurrentPrice().toString())
                            .add("newBidCount", auctionManager.getBidCount(auctionId))
                            .add("nextMinimumBid", auctionService.getMinimumBidAmount(auctionId).toString())
                            .build())
                    .add("timestamp", dateFormat.format(new Date()))
                    .build();

            return Response.status(Response.Status.CREATED).entity(response).build();

        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Json.createObjectBuilder()
                            .add("success", false)
                            .add("error", e.getMessage())
                            .add("timestamp", dateFormat.format(new Date()))
                            .build())
                    .build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Json.createObjectBuilder()
                            .add("success", false)
                            .add("error", e.getMessage())
                            .add("timestamp", dateFormat.format(new Date()))
                            .build())
                    .build();
        } catch (Exception e) {
            logger.severe("Error placing bid: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createErrorResponse("Failed to place bid"))
                    .build();
        }
    }

    /**
     * Get bid history for an auction
     * GET /api/auctions/{id}/bids
     */
    @GET
    @Path("/{id}/bids")
    public Response getAuctionBids(@PathParam("id") Long auctionId,
                                   @QueryParam("limit") @DefaultValue("20") int limit,
                                   @QueryParam("offset") @DefaultValue("0") int offset) {
        try {
            List<Bid> allBids = auctionService.getAuctionBids(auctionId);

            // Apply pagination
            int startIndex = Math.min(offset, allBids.size());
            int endIndex = Math.min(startIndex + limit, allBids.size());
            List<Bid> paginatedBids = allBids.subList(startIndex, endIndex);

            JsonArrayBuilder bidsArrayBuilder = Json.createArrayBuilder();
            for (Bid bid : paginatedBids) {
                JsonObject bidJson = Json.createObjectBuilder()
                        .add("id", bid.getBidId())
                        .add("amount", bid.getAmount().toString())
                        .add("bidder", bid.getBidder().getFirstName() + " " +
                                bid.getBidder().getLastName().substring(0, 1) + ".")
                        .add("bidTime", dateFormat.format(bid.getBidTime()))
                        .add("status", bid.getStatus().toString())
                        .build();
                bidsArrayBuilder.add(bidJson);
            }

            JsonObject response = Json.createObjectBuilder()
                    .add("success", true)
                    .add("auctionId", auctionId)
                    .add("totalBids", allBids.size())
                    .add("returnedBids", paginatedBids.size())
                    .add("offset", offset)
                    .add("limit", limit)
                    .add("bids", bidsArrayBuilder.build())
                    .add("timestamp", dateFormat.format(new Date()))
                    .build();

            return Response.ok(response).build();

        } catch (Exception e) {
            logger.severe("Error retrieving auction bids: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createErrorResponse("Failed to retrieve auction bids"))
                    .build();
        }
    }

    /**
     * Create a new auction
     * POST /api/auctions/
     */
    @POST
    @Path("/")
    public Response createAuction(JsonObject auctionData) {
        try {
            // Validate required fields
            String[] requiredFields = {"title", "description", "category", "startingPrice",
                    "startTime", "endTime", "sellerId"};
            for (String field : requiredFields) {
                if (!auctionData.containsKey(field)) {
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(createErrorResponse("Missing required field: " + field))
                            .build();
                }
            }

            // Extract auction data
            String title = auctionData.getString("title");
            String description = auctionData.getString("description");
            String category = auctionData.getString("category");
            BigDecimal startingPrice = new BigDecimal(auctionData.getString("startingPrice"));
            Long sellerId = Long.valueOf(auctionData.getInt("sellerId"));

            // Parse dates
            Date startTime = dateFormat.parse(auctionData.getString("startTime"));
            Date endTime = dateFormat.parse(auctionData.getString("endTime"));

            Auction auction = auctionService.createAuction(title, description, category,
                    startingPrice, startTime, endTime, sellerId);

            JsonObject response = Json.createObjectBuilder()
                    .add("success", true)
                    .add("message", "Auction created successfully")
                    .add("auction", createAuctionSummaryJson(auction))
                    .add("timestamp", dateFormat.format(new Date()))
                    .build();

            return Response.status(Response.Status.CREATED).entity(response).build();

        } catch (ParseException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(createErrorResponse("Invalid date format. Use: yyyy-MM-dd HH:mm:ss"))
                    .build();
        } catch (NumberFormatException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(createErrorResponse("Invalid number format"))
                    .build();
        } catch (Exception e) {
            logger.severe("Error creating auction: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createErrorResponse("Failed to create auction"))
                    .build();
        }
    }

    /**
     * User authentication
     * POST /api/auctions/auth/login
     */
    @POST
    @Path("/auth/login")
    public Response authenticateUser(JsonObject credentials) {
        try {
            if (!credentials.containsKey("email") || !credentials.containsKey("password")) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(createErrorResponse("Missing email or password"))
                        .build();
            }

            String email = credentials.getString("email");
            String password = credentials.getString("password");

            User user = auctionService.authenticateUser(email, password);

            if (user == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(createErrorResponse("Invalid credentials"))
                        .build();
            }

            JsonObject response = Json.createObjectBuilder()
                    .add("success", true)
                    .add("message", "Authentication successful")
                    .add("user", Json.createObjectBuilder()
                            .add("id", user.getUserId())
                            .add("email", user.getEmail())
                            .add("firstName", user.getFirstName())
                            .add("lastName", user.getLastName())
                            .add("isActive", user.isActive())
                            .build())
                    .add("timestamp", dateFormat.format(new Date()))
                    .build();

            return Response.ok(response).build();

        } catch (Exception e) {
            logger.severe("Error during authentication: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createErrorResponse("Authentication failed"))
                    .build();
        }
    }

    /**
     * User registration
     * POST /api/auctions/auth/register
     */
    @POST
    @Path("/auth/register")
    public Response registerUser(JsonObject userData) {
        try {
            String[] requiredFields = {"email", "password", "firstName", "lastName"};
            for (String field : requiredFields) {
                if (!userData.containsKey(field)) {
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(createErrorResponse("Missing required field: " + field))
                            .build();
                }
            }

            String email = userData.getString("email");
            String password = userData.getString("password");
            String firstName = userData.getString("firstName");
            String lastName = userData.getString("lastName");

            User user = auctionService.registerUser(email, password, firstName, lastName);

            JsonObject response = Json.createObjectBuilder()
                    .add("success", true)
                    .add("message", "User registered successfully")
                    .add("user", Json.createObjectBuilder()
                            .add("id", user.getUserId())
                            .add("email", user.getEmail())
                            .add("firstName", user.getFirstName())
                            .add("lastName", user.getLastName())
                            .build())
                    .add("timestamp", dateFormat.format(new Date()))
                    .build();

            return Response.status(Response.Status.CREATED).entity(response).build();

        } catch (Exception e) {
            logger.severe("Error during registration: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createErrorResponse("Registration failed"))
                    .build();
        }
    }

    /**
     * Get system status and statistics
     * GET /api/auctions/system/status
     */
    @GET
    @Path("/system/status")
    public Response getSystemStatus() {
        try {
            Map<String, Integer> categoryCounts = auctionManager.getAllCategoryCounts();

            JsonObjectBuilder categoryBuilder = Json.createObjectBuilder();
            for (Map.Entry<String, Integer> entry : categoryCounts.entrySet()) {
                categoryBuilder.add(entry.getKey(), entry.getValue());
            }

            JsonObject response = Json.createObjectBuilder()
                    .add("success", true)
                    .add("systemStatus", Json.createObjectBuilder()
                            .add("maintenance", auctionManager.isSystemMaintenance())
                            .add("lastUpdate", dateFormat.format(auctionManager.getLastSystemUpdate()))
                            .add("totalActiveBids", auctionManager.getTotalActiveBids())
                            .add("uptime", System.currentTimeMillis())
                            .build())
                    .add("statistics", Json.createObjectBuilder()
                            .add("categoryCounts", categoryBuilder.build())
                            .add("serverTime", dateFormat.format(new Date()))
                            .build())
                    .add("timestamp", dateFormat.format(new Date()))
                    .build();

            return Response.ok(response).build();

        } catch (Exception e) {
            logger.severe("Error retrieving system status: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createErrorResponse("Failed to retrieve system status"))
                    .build();
        }
    }

    /**
     * Search auctions
     * GET /api/auctions/search?q={query}&category={category}
     */
    @GET
    @Path("/search")
    public Response searchAuctions(@QueryParam("q") String query,
                                   @QueryParam("category") String category,
                                   @QueryParam("limit") @DefaultValue("20") int limit) {
        try {
            List<Auction> allAuctions;

            if (category != null && !category.trim().isEmpty()) {
                allAuctions = auctionService.getAuctionsByCategory(category);
            } else {
                allAuctions = auctionService.getActiveAuctions();
            }

            // Filter by query if provided
            List<Auction> filteredAuctions = allAuctions;
            if (query != null && !query.trim().isEmpty()) {
                String lowerQuery = query.toLowerCase();
                filteredAuctions = allAuctions.stream()
                        .filter(auction ->
                                auction.getTitle().toLowerCase().contains(lowerQuery) ||
                                        auction.getDescription().toLowerCase().contains(lowerQuery))
                        .limit(limit)
                        .collect(java.util.stream.Collectors.toList());
            }

            JsonArrayBuilder resultsBuilder = Json.createArrayBuilder();
            for (Auction auction : filteredAuctions) {
                resultsBuilder.add(createAuctionSummaryJson(auction));
            }

            JsonObject response = Json.createObjectBuilder()
                    .add("success", true)
                    .add("query", query != null ? query : "")
                    .add("category", category != null ? category : "")
                    .add("totalResults", filteredAuctions.size())
                    .add("results", resultsBuilder.build())
                    .add("timestamp", dateFormat.format(new Date()))
                    .build();

            return Response.ok(response).build();

        } catch (Exception e) {
            logger.severe("Error searching auctions: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createErrorResponse("Search failed"))
                    .build();
        }
    }

    // Helper Methods

    private JsonObject createAuctionSummaryJson(Auction auction) {
        return Json.createObjectBuilder()
                .add("id", auction.getAuctionId())
                .add("title", auction.getTitle())
                .add("category", auction.getCategory())
                .add("currentPrice", auction.getCurrentPrice().toString())
                .add("bidCount", auctionManager.getBidCount(auction.getAuctionId()))
                .add("status", auction.getStatus().toString())
                .add("endTime", dateFormat.format(auction.getEndTime()))
                .add("timeRemaining", calculateTimeRemaining(auction.getEndTime()))
                .add("isActive", auction.isActive())
                .build();
    }

    private JsonObject createErrorResponse(String errorMessage) {
        return Json.createObjectBuilder()
                .add("success", false)
                .add("error", errorMessage)
                .add("timestamp", dateFormat.format(new Date()))
                .build();
    }

    private String calculateTimeRemaining(Date endTime) {
        long currentTime = System.currentTimeMillis();
        long endTimeMillis = endTime.getTime();
        long remainingMillis = endTimeMillis - currentTime;

        if (remainingMillis <= 0) {
            return "Expired";
        }

        long days = remainingMillis / (24 * 60 * 60 * 1000);
        long hours = (remainingMillis % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000);
        long minutes = (remainingMillis % (60 * 60 * 1000)) / (60 * 1000);

        if (days > 0) {
            return String.format("%dd %dh %dm", days, hours, minutes);
        } else if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else {
            return String.format("%dm", minutes);
        }
    }
}