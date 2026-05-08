package com.auction.enums;
public enum UserRole {
ADMIN,
SELLER,
BIDDER;
 public static UserRole fromString(String value) {
    if (value == null) {
      throw new IllegalArgumentException("UserRole không được null");
    }
    // Chuyển về UPPER_CASE rồi tìm trong các Enum constant
    for (UserRole role : values()) {
      if (role.name().equalsIgnoreCase(value)) {
        return role;
      }
    }
    throw new IllegalArgumentException("Không tìm thấy UserRole: " + value);
  }

}
