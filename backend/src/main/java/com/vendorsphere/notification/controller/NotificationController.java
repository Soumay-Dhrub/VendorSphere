package com.vendorsphere.notification.controller;

import com.vendorsphere.common.dto.ApiResponse;
import com.vendorsphere.common.dto.PageResponse;
import com.vendorsphere.common.util.PageSupport;
import com.vendorsphere.common.util.SortWhitelist;
import com.vendorsphere.notification.dto.NotificationResponse;
import com.vendorsphere.notification.dto.UnreadCountResponse;
import com.vendorsphere.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Notification endpoints. Every one is addressed to the calling user: the service resolves the
 * recipient from the security context, so no path or query parameter can widen the view to another
 * user's notifications. That is why the only authorization rule here is authentication - a
 * notification is readable by exactly one user regardless of role.
 */
@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications")
public class NotificationController {

    /** Requirement 31.5: sortable fields, with the default matching Requirement 28.3. */
    private static final SortWhitelist SORTABLE = SortWhitelist.of("createdAt", "read", "title");

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Requirements 28.3 and 28.4. {@code direction} defaults to descending rather than the platform
     * default of ascending, so an unparameterized call returns the newest notifications first.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List the calling user's notifications, newest first")
    public ApiResponse<PageResponse<NotificationResponse>> list(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        return ApiResponse.ok(notificationService.list(
                unreadOnly, PageSupport.pageable(page, size, sort, direction, SORTABLE)));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Count the calling user's unread notifications")
    public ApiResponse<UnreadCountResponse> unreadCount() {
        return ApiResponse.ok(new UnreadCountResponse(notificationService.unreadCount()));
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mark one of the calling user's notifications as read")
    public ApiResponse<Void> markRead(@PathVariable UUID id) {
        notificationService.markRead(id);
        return ApiResponse.ok("Notification marked as read", null);
    }

    @PatchMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mark every notification of the calling user as read")
    public ApiResponse<Void> markAllRead() {
        notificationService.markAllRead();
        return ApiResponse.ok("All notifications marked as read", null);
    }
}
