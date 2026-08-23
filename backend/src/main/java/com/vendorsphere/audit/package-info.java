/**
 * Append-only audit logging (Requirement 29).
 *
 * <p>Three rules shape this module and none of them is a convention:
 * <ul>
 *   <li>{@link com.vendorsphere.audit.repository.AuditLogRepository} extends the bare
 *       {@code Repository} marker and declares one insert plus one filtered read, so no caller can
 *       compile a delete or a rewrite.</li>
 *   <li>{@link com.vendorsphere.audit.controller.AuditLogController} declares only {@code GET}, so
 *       Spring MVC answers write verbs with 405 without any application code.</li>
 *   <li>{@link com.vendorsphere.audit.service.AuditService#record} joins the caller's transaction,
 *       so a failed audit write rolls the business change back.</li>
 * </ul>
 *
 * <p>{@link com.vendorsphere.audit.service.AuditPayloadSerializer} additionally redacts
 * credential-shaped properties before they reach a row that, by design, can never be edited.
 */
package com.vendorsphere.audit;
