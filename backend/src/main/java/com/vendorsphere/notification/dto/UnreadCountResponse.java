package com.vendorsphere.notification.dto;

/** Unread notification count of the calling user (Requirement 28.8). */
public record UnreadCountResponse(long unreadCount) {
}
