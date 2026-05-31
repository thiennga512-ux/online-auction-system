package com.auction.enums;

public enum ActionType {
    // thao tac User:
    LOGIN,
    LOGOUT,
    REGISTER_USER,
    UPDATE_TO_SELLER,

    // thao tac Item&Auction:
    LIST_ITEM,
    CREATE_AUCTION,
    GET_ACTIVE_AUCTIONS, // lay danh sach phien dang chay
    GET_PENDING_AUCTIONS, // lay danh sach phien cho admin duyet
    GET_AUCTION_BIDS,
    GET_MY_ITEMS,
    GET_MY_AUCTIONS,
    APPROVE_AUCTION,
    REJECT_AUCTION,
    CANCEL_AUCTION,

    // realtime bidding:
    SUBSCRIBE_AUCTION, // Client mở hộp thoại đấu giá-> đăng ký nhận realtime update
    UNSUBSCRIBE_AUCTION, // client đóng hộp thoại
    PLACE_BID, // đặt giá

    // Broadcast(Server->Client)
    NEW_BID_BROADCAST, // Server thông báo có bid mới
    AUCTION_STARTED_BROADCAST, // Server thông báo phiên bắt đầu
    AUCTION_ENDED_BROADCAST, // Server thông báo phiên kết thúc
    NEW_AUCTION_BROADCAST, // Server thông báo có phiên mới

    // ADMIN quản lí User
    GET_ALL_USERS,
    SET_USER_ACTIVE,
    DEPOSIT_BALANCE,

    // Auto Bidding( thêm sau ):
    REGISTER_AUTO_BID,

    // PING
    PING, UPGRADE_TO_SELLER, REGISTER_NOTIFICATION, SELF_DEPOSIT, GLOBAL_NOTIFICATION_BROADCAST, SYNC_BALANCE,
    GET_PENDING_DEPOSITS, APPROVE_DEPOSIT, REJECT_DEPOSIT, DEPOSIT_REQUEST_BROADCAST, ADMIN_DEPOSIT,
    GET_AUCTION_RESULTS;

}