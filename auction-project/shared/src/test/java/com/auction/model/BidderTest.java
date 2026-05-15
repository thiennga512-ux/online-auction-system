package com.auction.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import com.auction.enums.UserRole;

import static org.junit.jupiter.api.Assertions.*;

public class BidderTest {

    private Bidder bidder;

    @BeforeEach
    public void setUp() {
        bidder = new Bidder("testuser", "hashedpass", "test@test.com", "Test User");
    }

    @Test
    public void testConstructor_NewBidder() {
        assertEquals("testuser", bidder.getUsername());
        assertEquals("test@test.com", bidder.getEmail());
        assertEquals(UserRole.BIDDER, bidder.getRole());
        assertEquals(0.0, bidder.getBalance());
        assertEquals(0.0, bidder.getFrozenBalance());
        assertNotNull(bidder.getParticipatedAuctions());
        assertNotNull(bidder.getWatchlist());
        assertEquals(0, bidder.getParticipatedAuctions().size());
    }

    @Test
    public void testConstructor_FromDatabase() {
        LocalDateTime now = LocalDateTime.now();
        Bidder dbBidder = new Bidder("id-123", now, now, "dbuser", "pass", "db@test.com", 
                                     "DB User", true, 1000.0, 200.0, "123 Street");

        assertEquals("id-123", dbBidder.getId());
        assertEquals("dbuser", dbBidder.getUsername());
        assertTrue(dbBidder.isActive());
        assertEquals(1000.0, dbBidder.getBalance());
        assertEquals(200.0, dbBidder.getFrozenBalance());
        assertEquals("123 Street", dbBidder.getShippingAddress());
    }

    @Test
    public void testSettersAndGetters() {
        bidder.setBalance(500.0);
        assertEquals(500.0, bidder.getBalance());

        bidder.setFrozenBalance(100.0);
        assertEquals(100.0, bidder.getFrozenBalance());

        bidder.setShippingAddress("456 Avenue");
        assertEquals("456 Avenue", bidder.getShippingAddress());
    }

    @Test
    public void testWatchlist() {
        List<String> mockWatchlist = Arrays.asList("item1", "item2");
        bidder.setWatchlist(mockWatchlist);
        
        assertEquals(2, bidder.getWatchlist().size());
        assertTrue(bidder.getWatchlist().contains("item1"));
    }

    @Test
    public void testRoleAndDashboard() {
        assertEquals(UserRole.BIDDER, bidder.getRole());
        assertEquals("/views/bidder_dashboard.fxml", bidder.getDashboardView());
    }
}
