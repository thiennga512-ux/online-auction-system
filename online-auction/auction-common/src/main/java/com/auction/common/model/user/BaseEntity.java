package com.auction.common.model.user;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * BaseEntity — Stub chưa được sử dụng.
 * Hiện tại User đã tự quản lý id và createdAt.
 * Giữ lại để tham khảo nếu cần refactor sau.
 */
public abstract class BaseEntity {
    private final String id;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected BaseEntity() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    protected void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
