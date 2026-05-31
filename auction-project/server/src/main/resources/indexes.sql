CREATE INDEX idx_bids_session ON bids(auction_session_id);
CREATE INDEX idx_sessions_status ON auction_sessions(status);
CREATE INDEX idx_items_seller ON items(seller_id);
