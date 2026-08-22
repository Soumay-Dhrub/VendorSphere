# Implementation Plan: Procurement Lifecycle

## Overview

The plan builds the remaining SRS phases (3–12) on top of the existing foundation and auth modules. It works outward from the shared numeric, state-machine, reference-number, attachment, notification and audit primitives, then walks the lifecycle in dependency order — vendor → purchase request → RFQ → quotation/comparison/selection → purchase order → delivery → invoice/matching → payment → performance and analytics — and finishes with the Next.js screens, the end-to-end integration test and CI wiring.

Backend code is Java 21 / Spring Boot 3.4 under `backend/src/main/java/com/vendorsphere`, tests use JUnit 5, Mockito, jqwik (property tests) and Testcontainers. Frontend code is TypeScript / Next.js 15 under `frontend/src`, tested with Vitest, React Testing Library and fast-check.

Each pure engine is implemented before the service that wraps it, and its property tests sit immediately after it so arithmetic and classification errors surface before any persistence code depends on them.

## Tasks

- [ ] 1. Shared numeric, state and pagination primitives
  - [ ] 1.1 Add property-testing dependency and shared test fixtures
    - Add `net.jqwik:jqwik` as a test-scoped dependency in `backend/pom.xml` alongside the existing JUnit 5, Mockito and Testcontainers dependencies
    - Add a shared `MoneyArbitraries` provider producing money values within `DECIMAL(15,2)`, quantity values within `DECIMAL(12,3)` and rates/scores within 0.00–100.00
    - _Requirements: 36.1, 36.2, 36.3_

  - [ ] 1.2 Implement the `Money` utility in `common.util`
    - Implement `money`, `quantity`, `sumMoney`, `sumQuantity`, `multiply`, `percentOf`, `ratio` and `clampScore` with scale 2 / scale 3 and HALF_UP rounding, treating `null` as zero
    - Enable `spring.jackson.serialization.write-bigdecimal-as-plain` so monetary fields serialize with exactly two decimals
    - _Requirements: 32.5, 32.6, 32.7_

  - [ ]* 1.3 Write property test for money and quantity normalization
    - **Property 1: Money and quantity normalization**
    - **Validates: Requirements 32.5, 32.6**

  - [ ] 1.4 Implement the generic `StateMachine<S>` in `common.util`
    - Implement `of`, `permits`, `targetsFrom` and `assertTransition`, where `assertTransition` raises a `BusinessException` carrying HTTP 409 and a message naming the source and target states
    - _Requirements: 3.2, 8.2, 11.2, 19.2, 24.2, 34.8_

  - [ ] 1.5 Define lifecycle enums and the five transition tables
    - Create `VendorStatus`, `VendorDocumentType`, `PurchaseRequestStatus`, `Priority`, `RfqStatus`, `RfqVendorStatus`, `QuotationStatus`, `PurchaseOrderStatus`, `InvoiceStatus`, `MatchStatus`, `MatchFindingType`, `MatchResolutionState`, `PaymentStatus`, `DocumentExpiryState` and `AttachmentOwnerType` in their module packages with `@Enumerated(EnumType.STRING)` semantics
    - Create `VendorStatusTransitions`, `PurchaseRequestStatusTransitions`, `RfqStatusTransitions`, `PurchaseOrderStatusTransitions` and `InvoiceStatusTransitions` as `StateMachine` instances encoding exactly the listed transitions
    - _Requirements: 3.1, 8.1, 11.1, 19.1, 24.1_

  - [ ]* 1.6 Write property test for the five state machines
    - **Property 2: State machines accept exactly the listed transitions**
    - **Validates: Requirements 3.1, 3.2, 8.1, 8.2, 11.1, 11.2, 19.1, 19.2, 24.1, 24.2**

  - [ ] 1.7 Implement `PageSupport` and `SortWhitelist` in `common.util`
    - Apply page 0 / size 20 defaults, clamp size into `[1, 100]`, reject unknown sort fields with HTTP 400 listing the allowed fields, and map `Page<E>` to the existing `PageResponse`
    - _Requirements: 31.1, 31.2, 31.3, 31.4, 31.5_

  - [ ]* 1.8 Write property test for pagination defaulting and clamping
    - **Property 15: Pagination parameters are defaulted and clamped**
    - **Validates: Requirements 31.3, 31.4, 31.5**

- [ ] 2. Schema migration, concurrency plumbing and reference numbers
  - [ ] 2.1 Write `V2__procurement_lifecycle.sql`
    - Create `reference_sequences`, `evaluation_criteria_weights`, `invoice_match_findings` and `attachments`; add `version` columns to `vendors`, `purchase_requests`, `rfqs`, `quotations`, `purchase_orders` and `invoices`; add the vendor status reason, RFQ cancellation reason, quotation warranty months, PO acknowledgement/overdue/cancellation/closed columns, invoice review comments and notification `event_type`
    - Add the notification dedupe unique index and every list/filter index named in the design, leaving `V1__init_schema.sql` untouched
    - _Requirements: 34.1, 34.2, 34.3, 34.4, 31.6, 32.3, 28.9_

  - [ ] 2.2 Add `CreatedOnlyEntity` and optimistic-lock error mapping
    - Add a `CreatedOnlyEntity` mapped superclass (id + `created_at`) in `common.entity` for tables without `updated_at`
    - Handle `ObjectOptimisticLockingFailureException` in `GlobalExceptionHandler`, returning HTTP 409 and `Record was modified by another user, reload and retry`
    - _Requirements: 32.3, 32.4_

  - [ ] 2.3 Implement `ReferenceNumberGenerator`
    - Add the `ReferenceSequence` entity and repository with a locking `UPDATE ... RETURNING next_value` allocation, and format results as `{PREFIX}-{YYYY}-{NNN}` for the prefixes `VEN`, `PR`, `RFQ`, `PO`, `DEL`, starting at `001` per organization, prefix and year, allocating on the caller's transaction
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6_

  - [ ]* 2.4 Write property test for reference number format and monotonicity
    - **Property 3: Reference number format and sequence monotonicity**
    - **Validates: Requirements 1.1, 1.2, 1.3, 1.4**

  - [ ]* 2.5 Write integration test for concurrent reference allocation
    - Run two concurrent allocations for the same organization, prefix and year against the Testcontainers PostgreSQL instance and assert distinct sequence values
    - _Requirements: 1.6, 36.4_

- [ ] 3. Attachments, notifications and audit logging
  - [ ] 3.1 Implement the attachment subsystem in `common.attachment`
    - Add the `Attachment` entity, repository, `AttachmentProperties`, filesystem `AttachmentStorage` using a random UUID storage key, `AttachmentService` upload/list/download/delete with content-type allowlist (415), 10 MB size gate (413) and owner-record access checks, and `AttachmentController` exposing `GET /attachments/{id}`
    - _Requirements: 33.1, 33.2, 33.3, 33.4, 33.5, 33.6, 33.7_

  - [ ]* 3.2 Write unit tests for attachment gates
    - Cover rejected content types, the 10 MB boundary, and that the storage reference excludes the original filename
    - _Requirements: 33.3, 33.4, 33.5_

  - [ ] 3.3 Implement the notification module
    - Add `Notification` entity, repository, `NotificationEvent` enum covering the sixteen events, `NotificationService` with `createOnce` swallowing the unique-constraint violation, role- and vendor-user fan-out, paged list ordered by creation instant descending, unread filter, mark-read, mark-all-read and unread count, plus `NotificationController`
    - _Requirements: 28.1, 28.2, 28.3, 28.4, 28.5, 28.6, 28.7, 28.8, 28.9, 28.10_

  - [ ]* 3.4 Write integration tests for notification dedupe and ownership
    - Assert a repeated `createOnce` leaves the existing row unchanged, and that marking another user's notification read returns 404 `Notification not found`
    - _Requirements: 28.6, 28.9, 36.4_

  - [ ] 3.5 Implement the audit module
    - Add `AuditLog` entity, repository, `AuditAction` enum covering the twenty-two actions, `AuditService.record` serializing previous/new state to JSON with request IP and user agent inside the caller's transaction, filtered paged `search`, and an `AuditLogController` declaring only `GET /audit-logs` restricted to ADMIN
    - _Requirements: 29.1, 29.2, 29.3, 29.4, 29.5, 29.6, 29.7, 29.8_

  - [ ]* 3.6 Write integration tests for audit append-only behaviour
    - Assert `PUT`/`PATCH`/`DELETE` on the audit endpoint return 405, a non-ADMIN role receives 403, and a failing audit write rolls the business change back with a 500 response
    - _Requirements: 29.7, 29.9, 29.10_

- [ ] 4. Checkpoint - shared primitives
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 5. Vendor management
  - [ ] 5.1 Create vendor entities and repositories
    - Map `Vendor` (with `@Version`), `VendorContact`, `VendorCategory` and `VendorDocument` to the existing tables and add tenant-scoped repositories with `findByIdAndOrganizationId` finders
    - _Requirements: 2.1, 4.1, 4.4, 5.1, 30.10, 32.3_

  - [ ] 5.2 Implement vendor registration, update and detail read
    - Add vendor DTO records and `VendorService` creating vendors with a generated `VEN` code, status PROSPECTIVE, rating 0.00 and registration timestamp; enforce the duplicate-email 409 `Vendor email already registered`; return category name, performance score and expiring-document count on read; surface cross-tenant identifiers as 404 `Vendor not found`
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6_

  - [ ] 5.3 Implement vendor status lifecycle
    - Add `VendorStatusService` driving `VendorStatusTransitions`, requiring a reason for SUSPENDED/BLACKLISTED/INACTIVE with 400 `Status change reason is required`, persisting the reason and recording an audit entry carrying previous status, new status and reason
    - _Requirements: 3.1, 3.2, 3.3, 3.4_

  - [ ] 5.4 Implement `DocumentExpiryEvaluator`
    - Return EXPIRED before today, EXPIRING_SOON within the inclusive 30-day window and VALID otherwise or when the expiry date is absent
    - _Requirements: 5.4_

  - [ ]* 5.5 Write property test for document expiry classification
    - **Property 14: Document expiry classification is total and exclusive**
    - **Validates: Requirement 5.4**

  - [ ] 5.6 Implement vendor contacts and categories
    - Add `VendorContactService` with single-primary enforcement and primary-first / name-ascending ordering, and `VendorCategoryService` with per-organization name uniqueness (409 `Vendor category already exists`) and a delete guard naming the referencing vendor count
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6_

  - [ ] 5.7 Implement vendor documents and the daily expiry job
    - Add `VendorDocumentService` uploading through `AttachmentService` with the six-type allowlist (400 listing accepted types) and derived expiry state, add `SchedulingConfig`, and add `VendorDocumentExpiryJob` at `0 30 0 * * *` notifying ADMIN and PROCUREMENT_OFFICER users for documents expiring in exactly 30, 7 or 1 day
    - _Requirements: 5.1, 5.2, 5.3, 5.5, 5.6_

  - [ ] 5.8 Implement vendor search and listing
    - Add a JPA specification combining case-insensitive company-name contains, category, status and minimum-rating filters, restricted to the actor's organization, sortable by company name, registration timestamp, rating and status through `PageSupport`
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7, 31.1, 31.2_

  - [ ] 5.9 Implement `VendorAccessGuard`
    - Resolve the vendor linked to the current principal through `vendors.user_id`, cache it per request, expose `currentVendorId` and `assertVendorVisible` raising 404 with the caller-supplied message, and restrict vendor users to their own profile
    - _Requirements: 2.7, 30.8, 30.10_

  - [ ] 5.10 Add vendor controllers
    - Add `VendorController` and `VendorCategoryController` covering the vendor, contact, document, status, performance and category endpoints with `@PreAuthorize` role grants and OpenAPI operation metadata, returning `ApiResponse`/`PageResponse` wrappers only
    - _Requirements: 3.5, 30.3, 30.4, 30.5, 34.5, 34.6, 34.7_

  - [ ]* 5.11 Write unit and integration tests for vendor rules
    - Cover the pinned 400/404/409 messages, the transition rejections of 3.2, the 403 for a PROCUREMENT_OFFICER status change, and filter combinations of 6.6
    - _Requirements: 2.2, 2.3, 2.6, 3.2, 3.5, 4.5, 4.6, 6.6, 36.1_

- [ ] 6. Purchase requests
  - [ ] 6.1 Create purchase request entities and repositories
    - Map `PurchaseRequest` (with `@Version`) and `PurchaseRequestItem` to the existing tables with tenant-scoped repositories
    - _Requirements: 7.1, 30.10, 32.3_

  - [ ] 6.2 Implement purchase request authoring
    - Add DTOs and `PurchaseRequestService` creating requests with a generated `PR` number, status DRAFT, actor as requester and MEDIUM priority default; allow item and attachment changes only while DRAFT; store quantities at quantity scale with sort order equal to the current item count; reject non-positive quantities with 400 `Quantity must be greater than zero`
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6_

  - [ ] 6.3 Implement submission and review
    - Drive `PurchaseRequestStatusTransitions`; require at least one item with 400 `Purchase request requires at least one item`; lock items after submission with 409 `Purchase request items are locked after submission`; notify PROCUREMENT_MANAGER users on submission; record reviewer, review timestamp and notes on approval; require a rejection reason with 400 `Rejection reason is required` and notify the requester; return reviewer name, review data and derived RFQ identifiers on read
    - _Requirements: 7.7, 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 8.7, 8.9_

  - [ ] 6.4 Add the purchase request controller
    - Expose creation, update, item CRUD, attachment upload, submit, approve and reject endpoints with `@PreAuthorize` grants that give REQUESTER authoring rights only and return 403 for requester-initiated approval or rejection
    - _Requirements: 8.8, 30.4, 30.6, 34.5, 34.6_

  - [ ]* 6.5 Write unit tests for purchase request rules
    - Cover the item lock, the empty-item submission rejection, the missing rejection reason and the reviewer fields recorded on approval
    - _Requirements: 7.5, 7.7, 8.3, 8.5, 8.6, 36.1_

- [ ] 7. Checkpoint - request authoring path
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 8. RFQ management
  - [ ] 8.1 Create RFQ entities and repositories
    - Map `Rfq` (with `@Version`), `RfqItem` and `RfqVendor` with tenant-scoped repositories and vendor-invitation lookups
    - _Requirements: 9.1, 10.1, 30.10, 32.3_

  - [ ] 8.2 Implement RFQ creation and item management
    - Create RFQs from APPROVED or PROCUREMENT_STARTED purchase requests with a generated `RFQ` number and status DRAFT, copying each PR item into an RFQ item that retains the source item identifier; transition the first-time source PR to PROCUREMENT_STARTED; reject other PR statuses with 409 naming the status; validate `closingDate > openingDate` with 400 `Closing date must be after opening date`; permit header, item and document edits while DRAFT
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6, 9.7_

  - [ ] 8.3 Implement vendor invitations
    - Add `RfqVendorService` performing all-or-nothing validation that each vendor is ACTIVE (409 naming company name and status) and not already invited (409 `Vendor already invited to this RFQ`), notifying vendor users when the RFQ is OPEN, moving INVITED→VIEWED on first vendor read, and returning only OPEN/CLOSED/EVALUATION/AWARDED invited RFQs to vendor users
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5, 10.7_

  - [ ] 8.4 Implement publication, closing and cancellation
    - Drive `RfqStatusTransitions`; require at least one invitation to open with 400 `RFQ requires at least one invited vendor`; require a cancellation reason with 400 `Cancellation reason is required`; reject cancelling an AWARDED RFQ with 409 `Awarded RFQ cannot be cancelled`; on cancellation reject SUBMITTED/UNDER_REVIEW quotations and notify invited vendor users; add `RfqClosingJob` at `0 */5 * * * *` closing overdue OPEN RFQs and nudging non-responding vendors 24–25 hours out
    - _Requirements: 10.6, 11.1, 11.2, 11.3, 11.4, 11.5, 11.6, 11.7, 11.8_

  - [ ] 8.5 Add the RFQ controller
    - Expose RFQ CRUD, item CRUD, vendor invitation, document upload, open, close and cancel endpoints with role grants and OpenAPI metadata
    - _Requirements: 30.4, 30.5, 34.5, 34.6_

  - [ ]* 8.6 Write unit tests for RFQ rules
    - Cover the date validation, the open-without-vendor rejection, the invitation all-or-nothing behaviour and the awarded-cancellation rejection
    - _Requirements: 9.6, 10.2, 10.3, 11.6, 11.8, 36.1_

- [ ] 9. Quotations, comparison, evaluation and selection
  - [ ] 9.1 Create quotation entities and repositories
    - Map `Quotation` (with `@Version`), `QuotationItem`, `VendorEvaluation` and `VendorSelection` with tenant- and vendor-scoped repositories
    - _Requirements: 12.1, 16.13, 17.2, 30.10, 32.3_

  - [ ] 9.2 Implement `QuotationCalculator`
    - Compute item tax amount, item line total, subtotal, tax amount, discount amount and total amount as pure functions over `QuotationItemInput` and shipping amount, all at money scale
    - _Requirements: 13.1, 13.2, 13.3, 13.4, 13.5_

  - [ ]* 9.3 Write property test for quotation total computation
    - **Property 4: Quotation totals are internally consistent and idempotent**
    - **Validates: Requirements 13.1, 13.2, 13.3, 13.4, 13.5**

  - [ ] 9.4 Implement quotation submission and revision
    - Add request records carrying only vendor-supplied primitives (no computed fields) and response records exposing computed figures read-only; enforce the OPEN-and-before-closing window with 409 `RFQ is closed for quotation submission`, one item per RFQ item with a 400 listing unpriced item names, field ranges, and validity date with 400 `Quotation validity date must be on or after the RFQ closing date`; on submission set SUBMITTED, record the instant, move the invitation to RESPONDED, notify PROCUREMENT_OFFICER users and recompute totals on every revision
    - _Requirements: 12.1, 12.2, 12.3, 12.4, 12.5, 12.6, 12.7, 12.8, 12.9, 12.10, 13.6, 13.7_

  - [ ] 9.5 Implement `QuotationVisibilityPolicy`
    - Return only the caller's own quotations to vendor users with 404 `Quotation not found` otherwise, and project prices out of internal-user responses while the RFQ is OPEN while still returning the submitted count
    - _Requirements: 14.1, 14.2, 14.3, 14.5_

  - [ ] 9.6 Implement criteria weight storage
    - Add the `EvaluationCriteriaWeight` entity, repository and service with `CriteriaWeights.DEFAULT` (0.40 / 0.25 / 0.25 / 0.10) as the fallback and `validateSum` raising 400 `Criteria weights must sum to 1.00`
    - _Requirements: 16.9, 16.10, 16.11_

  - [ ]* 9.7 Write property test for criteria weight validation
    - **Property 16: Criteria weights are accepted exactly when they sum to one**
    - **Validates: Requirements 16.9, 16.11**

  - [ ] 9.8 Implement `EvaluationEngine`
    - Compute price, delivery, warranty and performance component scores with the zero/absent and no-completed-order defaults, apply the weights to produce the evaluation score, mark exactly one recommended quotation by highest score then lowest total, and persist a `VendorEvaluation` per scored quotation without touching quotation or RFQ status
    - _Requirements: 16.1, 16.2, 16.3, 16.4, 16.5, 16.6, 16.7, 16.8, 16.12, 16.13, 16.14_

  - [ ]* 9.9 Write property test for evaluation scoring
    - **Property 5: Evaluation scores are bounded and exactly one quotation is recommended**
    - **Validates: Requirements 16.1, 16.2, 16.3, 16.5, 16.6, 16.7, 16.8, 16.12**

  - [ ] 9.10 Implement `ComparisonEngine`
    - Build one row per SUBMITTED/UNDER_REVIEW/SELECTED/REJECTED quotation carrying vendor, header and per-RFQ-item figures, ordered by evaluation score descending then total amount ascending; return an empty row collection with the RFQ summary when no quotation qualifies; reject DRAFT/OPEN RFQs with 409 `Comparison is available after the RFQ closes`
    - _Requirements: 15.1, 15.2, 15.3, 15.4, 15.5, 15.6_

  - [ ]* 9.11 Write property test for comparison table construction
    - **Property 6: Comparison rows are complete and ordered**
    - **Validates: Requirements 15.1, 15.3, 15.5, 15.6**

  - [ ] 9.12 Implement `SelectionService`
    - In one transaction set the target quotation SELECTED, other SUBMITTED/UNDER_REVIEW quotations REJECTED, the RFQ AWARDED and the winning invitation AWARDED; persist the vendor selection record; require a justification with 400 `Selection justification is required`; reject a second award with 409 `RFQ is already awarded` and a WITHDRAWN/REJECTED target with 409 naming the status; notify all invited vendor users of the outcome; record quotation comments on the evaluation record; expose vendor historical performance
    - _Requirements: 17.1, 17.2, 17.3, 17.4, 17.5, 17.6, 17.7, 17.8, 17.9, 17.10, 32.1_

  - [ ] 9.13 Add quotation, comparison, evaluation and selection controllers
    - Expose the quotation, comparison, evaluate, comment, select and criteria-weight endpoints with role grants that deny vendor users the comparison endpoint
    - _Requirements: 14.4, 30.4, 30.5, 30.8, 34.5, 34.6_

  - [ ]* 9.14 Write integration tests for vendor quotation isolation
    - Assert a vendor user receives 404 `Quotation not found` for another vendor's quotation and 403 for the comparison endpoint
    - _Requirements: 14.2, 14.4, 36.4, 36.6_

- [ ] 10. Checkpoint - award path
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 11. Purchase orders
  - [ ] 11.1 Create purchase order entities and repositories
    - Map `PurchaseOrder` (with `@Version`) and `PurchaseOrderItem` with tenant- and vendor-scoped repositories
    - _Requirements: 18.1, 30.10, 32.3_

  - [ ] 11.2 Implement purchase order generation from an award
    - Copy the selected quotation's items and totals into a DRAFT PO with a generated `PO` number, delivery address from the RFQ delivery location, payment terms from the quotation and expected delivery date of generation date plus delivery period, with delivered quantities zeroed; reject an RFQ without a selection with 409 `RFQ has no selected quotation` and an RFQ that already has a PO with 409 naming the existing number; permit DRAFT header edits; run in one transaction
    - _Requirements: 18.1, 18.2, 18.3, 18.4, 18.5, 18.6, 18.7, 18.8, 32.1_

  - [ ] 11.3 Implement purchase order lifecycle
    - Drive `PurchaseOrderStatusTransitions`; on issue record the issuing user and instant and notify vendor users; allow vendor acknowledgement only for the owning vendor with 404 `Purchase order not found` otherwise; block cancellation once any delivered quantity exists with 409 `Purchase order with recorded deliveries cannot be cancelled` and require a reason with 400 `Cancellation reason is required`; allow closing only from DELIVERED with 409 naming the current status; hide DRAFT POs from vendor listings
    - _Requirements: 19.1, 19.2, 19.3, 19.4, 19.5, 19.6, 19.7, 19.8, 19.10_

  - [ ] 11.4 Add the purchase order controller
    - Expose generation, list, detail, update, issue, acknowledge, close and cancel endpoints with role grants and OpenAPI metadata
    - _Requirements: 30.4, 30.8, 34.5, 34.6_

  - [ ]* 11.5 Write unit tests for purchase order rules
    - Cover generation field copying, the two generation rejections, the delivered-quantity cancellation guard and the close-from-non-DELIVERED rejection
    - _Requirements: 18.2, 18.5, 18.6, 19.6, 19.8, 36.1_

- [ ] 12. Deliveries
  - [ ] 12.1 Create delivery entities and repositories
    - Map `Delivery` and `DeliveryItem` with repositories aggregating received, damaged and rejected quantities per purchase order item
    - _Requirements: 20.1, 20.2, 30.10_

  - [ ] 12.2 Implement `DeliveryProgressCalculator`
    - Derive per-item ordered, received, damaged, rejected and outstanding quantities at quantity scale, and derive the purchase order status from the item progress list without regressing from DELIVERED or CLOSED
    - _Requirements: 20.9, 21.1, 21.2, 21.3_

  - [ ]* 12.3 Write property test for delivery quantity conservation
    - **Property 9: Delivery progress conserves quantities**
    - **Validates: Requirements 21.1, 20.9**

  - [ ]* 12.4 Write property test for purchase order status derivation
    - **Property 10: Purchase order status derivation is total and consistent**
    - **Validates: Requirements 21.2, 21.3**

  - [ ] 12.5 Implement delivery recording
    - In one transaction validate item ownership (400 `Delivery item does not belong to the purchase order`), positive received quantity (400 `Received quantity must be greater than zero`), damaged and rejected within received (400 `Damaged and rejected quantities cannot exceed the received quantity`) and cumulative received within ordered (409 naming item, ordered and cumulative); generate the `DEL` number; recompute delivered quantities and PO status; clear the overdue flag; notify PROCUREMENT_OFFICER and FINANCE users; permit repeated deliveries against one item
    - _Requirements: 20.1, 20.2, 20.3, 20.4, 20.5, 20.6, 20.7, 20.8, 20.10, 21.1, 21.2, 21.3, 21.6, 32.1_

  - [ ] 12.6 Implement the overdue delivery job
    - Add `OverdueDeliveryJob` at `0 0 1 * * *` setting the overdue flag on ISSUED/ACKNOWLEDGED POs past their expected delivery date with zero cumulative delivered quantity, notifying PROCUREMENT_OFFICER users only on the transition to true
    - _Requirements: 21.4, 21.5, 21.7_

  - [ ] 12.7 Add the delivery controller
    - Expose delivery recording, delivery listing, detail and per-item delivery progress endpoints with role grants and OpenAPI metadata
    - _Requirements: 20.9, 30.4, 30.8, 34.5, 34.6_

  - [ ]* 12.8 Write unit tests for delivery validation ordering
    - Cover each rejection message, a partial delivery producing PARTIALLY_DELIVERED and a completing delivery producing DELIVERED
    - _Requirements: 20.4, 20.5, 20.6, 20.7, 21.2, 21.3, 36.2_

- [ ] 13. Checkpoint - fulfilment path
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 14. Invoices and three-way matching
  - [ ] 14.1 Create invoice entities and repositories
    - Map `Invoice` (with `@Version`), `InvoiceItem` and `InvoiceMatchFinding` with tenant- and vendor-scoped repositories and a per-vendor invoice-number lookup
    - _Requirements: 22.1, 22.2, 30.10, 32.3_

  - [ ] 14.2 Implement `ThreeWayMatcher`
    - Raise QUANTITY_MISMATCH, PRICE_MISMATCH (0.01 tolerance), MISSING_DELIVERY and DUPLICATE_INVOICE findings from a `MatchInput`, set MATCHED when no finding exists, and otherwise set the status from the highest-precedence finding
    - _Requirements: 23.1, 23.2, 23.3, 23.4, 23.5, 23.6, 23.7_

  - [ ]* 14.3 Write property test for match status precedence
    - **Property 7: Match status follows finding precedence**
    - **Validates: Requirements 23.6, 23.7**

  - [ ]* 14.4 Write property test for finding conditions
    - **Property 8: Match findings are raised exactly when the comparison fails**
    - **Validates: Requirements 23.2, 23.3, 23.4, 23.5**

  - [ ] 14.5 Implement invoice submission with matching
    - In one transaction create the invoice with status SUBMITTED and match status PENDING, compute item line totals and the invoice total server-side, enforce per-vendor invoice-number uniqueness with 409 `Invoice number already exists for this vendor`, due date with 400 `Due date must be on or after the invoice date` and the PO status gate with 409 naming the status, run the matcher, persist findings, notify FINANCE users on submission and FINANCE plus PROCUREMENT_MANAGER users on findings, and restrict vendor listings to the linked vendor
    - _Requirements: 22.1, 22.2, 22.3, 22.4, 22.5, 22.6, 22.7, 22.8, 23.8, 23.9, 23.11, 32.1_

  - [ ] 14.6 Implement invoice review and match override
    - Drive `InvoiceStatusTransitions`; block APPROVED/PARTIALLY_PAID/PAID while any finding is UNRESOLVED with 409 listing the unresolved types; set OVERRIDDEN with actor, instant and justification, requiring one with 400 `Override justification is required`; require a rejection reason with 400 `Rejection reason is required`; record reviewer, timestamp and comments and notify the invoice vendor users
    - _Requirements: 24.1, 24.2, 24.3, 24.4, 24.5, 24.6, 24.7, 24.8_

  - [ ] 14.7 Implement the overdue invoice job
    - Add `OverdueInvoiceJob` at `0 15 1 * * *` moving invoices past their due date with paid amount below total and status other than PAID, REJECTED or OVERDUE to OVERDUE
    - _Requirements: 24.9, 24.10_

  - [ ] 14.8 Wire delivery-triggered match re-evaluation
    - Call the matcher from delivery recording for every SUBMITTED or UNDER_REVIEW invoice on the affected purchase order, replacing that invoice's findings and match status inside the delivery transaction
    - _Requirements: 23.10, 32.1_

  - [ ] 14.9 Add the invoice controller
    - Expose invoice listing, submission, detail, match result, finding override and review endpoints with role grants that return 403 for vendor-initiated approval or rejection
    - _Requirements: 23.9, 24.11, 30.7, 30.8, 34.5, 34.6_

  - [ ]* 14.10 Write unit tests for invoice rules
    - Cover the duplicate invoice number, the due-date validation, the PO status gate, the unresolved-finding approval block and approval after every finding is overridden
    - _Requirements: 22.4, 22.5, 22.6, 24.3, 24.6, 36.2_

- [ ] 15. Payments
  - [ ] 15.1 Create the payment entity and repository
    - Map `Payment` with a repository summing PAID amounts per invoice and aggregating outstanding balances per vendor
    - _Requirements: 25.1, 25.10, 30.10_

  - [ ] 15.2 Implement `PaymentAggregator`
    - Sum PAID payment amounts at money scale ignoring other statuses, and derive PAID, PARTIALLY_PAID or the unchanged current status from the paid and total amounts
    - _Requirements: 25.5, 25.6, 25.7_

  - [ ]* 15.3 Write property test for payment aggregation and invoice status derivation
    - **Property 11: Payment aggregation and invoice status derivation**
    - **Validates: Requirements 25.5, 25.6, 25.7**

  - [ ] 15.4 Implement payment recording and outstanding payables
    - In one transaction validate a positive amount with 400 `Payment amount must be greater than zero`, the invoice status gate with 409 naming the status, and the cumulative cap with 409 naming the invoice total and cumulative paid amount; write the payment with status PAID and the actor; set the invoice paid amount from the aggregator; derive the invoice status; notify the invoice vendor users; permit repeated payments; return organization-total and per-vendor outstanding payables
    - _Requirements: 25.1, 25.2, 25.3, 25.4, 25.5, 25.6, 25.7, 25.8, 25.9, 25.10, 25.11, 32.1_

  - [ ] 15.5 Add the payment controller
    - Expose payment recording, payment listing and outstanding payables endpoints with FINANCE, PROCUREMENT_MANAGER and ADMIN grants
    - _Requirements: 30.5, 30.7, 34.5, 34.6_

  - [ ]* 15.6 Write unit tests for payment rules
    - Cover the over-payment rejection, the invalid invoice status rejection and the PARTIALLY_PAID to PAID progression across two payments
    - _Requirements: 25.3, 25.4, 25.8, 36.2_

- [ ] 16. Checkpoint - settlement path
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 17. Vendor performance and analytics
  - [ ] 17.1 Implement `PerformanceCalculator`
    - Compute the delivery, quality, pricing, responsiveness and fulfilment metrics with the 50.00 zero-denominator default, cap pricing at 100.00, clamp every value into 0.00–100.00, average the five metrics into the overall score, and derive the vendor rating as score divided by 20
    - _Requirements: 26.1, 26.2, 26.3, 26.4, 26.5, 26.6, 26.7, 26.8, 26.11_

  - [ ]* 17.2 Write property test for performance metric bounds and averaging
    - **Property 12: Performance metrics are bounded, defaulted and averaged**
    - **Validates: Requirements 26.6, 26.7, 26.8**

  - [ ]* 17.3 Write property test for vendor rating derivation
    - **Property 13: Vendor rating derives from the performance score**
    - **Validates: Requirement 26.11**

  - [ ] 17.4 Implement `PerformanceEngine` and recalculation hooks
    - Gather the metric aggregates with repository projections, upsert the current calendar month's `VendorPerformanceSnapshot`, write the derived rating back onto the vendor, expose the historical performance read, and invoke recalculation from quotation submission, quotation selection, delivery recording and PO transitions to DELIVERED or CLOSED
    - _Requirements: 17.9, 19.9, 26.9, 26.10, 26.11_

  - [ ] 17.5 Implement `AnalyticsService`
    - Add the dashboard summary and the monthly spend, spend by department, spend by vendor, category distribution, vendor performance and average cycle time reports as read-only aggregate queries, returning zero cycle time when no CLOSED purchase order exists
    - _Requirements: 27.1, 27.2, 27.3, 27.4, 27.5, 27.6, 27.7, 27.8, 27.9, 27.10, 27.11, 27.12, 27.13_

  - [ ] 17.6 Add analytics and performance controllers
    - Expose the dashboard, six report endpoints and the vendor performance endpoint with the role grants of Requirements 27 and 30.5
    - _Requirements: 30.3, 30.5, 34.5, 34.6_

  - [ ]* 17.7 Write integration tests for analytics aggregates
    - Seed invoices, purchase orders and deliveries against the Testcontainers database and assert each dashboard figure and report total
    - _Requirements: 27.1, 27.7, 27.8, 27.9, 27.12, 36.4_

- [ ] 18. Checkpoint - backend complete
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 19. Frontend foundations
  - [ ] 19.1 Set up frontend test tooling and shared UI primitives
    - Add `fast-check` to the dev dependencies beside the existing Vitest and React Testing Library setup, add the shadcn/ui primitives used by the screens, and add `formatMoney` plus a `role-guard` component rendering an access denied message when roles do not match
    - _Requirements: 32.7, 35.12, 35.14, 36.7_

  - [ ] 19.2 Add API modules and TanStack Query hooks
    - Add `src/lib/api/{vendors,purchaseRequests,rfqs,quotations,purchaseOrders,deliveries,invoices,payments,analytics,notifications,audit}.ts` on top of the existing `apiClient`, with typed request/response models mirroring the backend DTOs and query/mutation hooks under `src/lib/hooks`
    - _Requirements: 35.13_

  - [ ] 19.3 Add the authenticated application shell
    - Add the `(app)` route group layout with sidebar navigation, header and a notification bell showing the unread count, wired to the notifications hooks
    - _Requirements: 35.11, 35.13, 35.14_

- [ ] 20. Frontend procurement screens
  - [ ] 20.1 Build the dashboard screen
    - Present the dashboard figures plus the monthly spend, spend by department and spend by vendor reports
    - _Requirements: 35.1, 35.13_

  - [ ] 20.2 Build the vendor list and detail screens
    - Add the searchable, filterable, paginated vendor list linking to a detail screen presenting profile, contacts, documents with expiry state, performance score and the status change control
    - _Requirements: 35.2, 35.3, 35.14_

  - [ ] 20.3 Build the purchase request screens
    - Add the list and detail screens presenting items, attachments, status, review notes and derived RFQ links, plus the authoring and submit controls
    - _Requirements: 35.4, 35.14_

  - [ ] 20.4 Build the RFQ screens
    - Add the list and detail screens presenting items, invited vendors, documents, opening date, closing date and status, plus the invitation and open/close/cancel controls
    - _Requirements: 35.5, 35.14_

  - [ ] 20.5 Build the comparison table and selection confirmation
    - Add `comparison-table.tsx` rendering one column per quotation with totals, delivery period, warranty, performance score, the four component scores, the evaluation score and a single recommended marker, and `selection-confirm-dialog.tsx` blocking submit until a justification is entered
    - _Requirements: 35.6, 35.7, 35.14_

  - [ ]* 20.6 Write property test for comparison table rendering
    - **Property 17: Comparison table rendering matches the quotation data**
    - **Validates: Requirements 35.6, 32.7**

  - [ ]* 20.7 Write Vitest tests for the selection confirmation step
    - Assert submit stays disabled without a justification and that confirming sends the justification
    - _Requirements: 35.7, 36.7_

  - [ ] 20.8 Build the purchase order screens
    - Add the list and detail screens with `delivery-progress.tsx` presenting per-item ordered, received and outstanding quantities, status and delivery history
    - _Requirements: 35.8, 35.14_

  - [ ]* 20.9 Write Vitest test for the delivery progress presentation
    - Assert ordered, received and outstanding values render per item for a partially delivered purchase order
    - _Requirements: 35.8, 36.7_

  - [ ] 20.10 Build the invoice and payment screens
    - Add the invoice list and detail screens with `match-findings.tsx` presenting match status, findings and the override control, the paid amount and payment history, plus the payments screen with outstanding payables
    - _Requirements: 35.9, 35.14_

  - [ ]* 20.11 Write Vitest test for the match finding presentation
    - Assert each finding type, its compared values and its resolution state render, and that the override control appears only for unresolved findings
    - _Requirements: 35.9, 36.7_

  - [ ] 20.12 Build the vendor portal area
    - Add the `(vendor)/vendor-portal` routes presenting RFQ invitations, the quotation submission form without client-side total fields, awarded purchase orders with the acknowledgement control and the invoice submission form
    - _Requirements: 35.10, 35.14, 13.7_

  - [ ] 20.13 Build the notification and audit log screens
    - Add the notification list with mark-as-read and mark-all-read controls and the ADMIN-only audit log screen with actor, entity and date range filters, both guarded by `role-guard`
    - _Requirements: 35.11, 35.12, 35.14_

- [ ] 21. Checkpoint - frontend complete
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 22. End-to-end verification and delivery wiring
  - [ ]* 22.1 Add the Testcontainers integration test base
    - Add an abstract base class provisioning PostgreSQL 16 through Testcontainers, applying the Flyway migrations before each test class and providing an authenticated request helper per role
    - _Requirements: 36.4_

  - [ ]* 22.2 Write the signature scenario integration test
    - Drive purchase request creation and approval, RFQ creation and invitation, three quotation submissions, comparison, selection, purchase order generation and issuance, delivery recording, invoice submission with a passing three-way match, payment recording and vendor performance recalculation, asserting the reference numbers and terminal statuses
    - _Requirements: 36.5, 36.4_

  - [ ]* 22.3 Write role authorization integration tests
    - Assert the 401 for a missing or expired token, the 403 `Access denied` for endpoints outside each role's grants, and that cross-tenant identifiers surface as 404
    - _Requirements: 30.1, 30.2, 30.9, 30.10, 30.11_

  - [ ]* 22.4 Write the optimistic locking integration test
    - Assert a second concurrent update of the same vendor, purchase request, RFQ, quotation, purchase order or invoice returns 409 `Record was modified by another user, reload and retry`
    - _Requirements: 32.3, 32.4_

  - [ ] 22.5 Wire both test suites into continuous integration
    - Update `.github/workflows/ci.yml` to run the backend Maven test suite including the property and Testcontainers tests and the frontend Vitest suite, failing the run on any test failure
    - _Requirements: 36.8_

  - [ ] 22.6 Update runtime configuration and project docs
    - Add the attachment base directory configuration and its Docker Compose volume, and mark SRS phases 3–12 complete in `README.md`
    - _Requirements: 33.7, 34.4_

- [ ] 23. Final checkpoint
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for a faster MVP; the property tests among them are the cheapest way to catch arithmetic and classification regressions, so skip them last
- Each pure engine is implemented directly before its property tests, and both precede the service that wraps it
- Checkpoints sit after each lifecycle stage so a failing stage is caught before the next one builds on it
- Property tests cover the universal arithmetic and classification rules; unit and integration tests cover the pinned messages, HTTP statuses, role grants, tenant isolation, scheduled jobs and the end-to-end scenario

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2", "1.4", "1.7", "2.1"] },
    { "id": 1, "tasks": ["1.3", "1.5", "1.8", "2.2", "19.1"] },
    { "id": 2, "tasks": ["1.6", "2.3", "3.1", "19.2"] },
    { "id": 3, "tasks": ["2.4", "2.5", "3.2", "3.3", "3.5", "19.3"] },
    { "id": 4, "tasks": ["3.4", "3.6", "5.1"] },
    { "id": 5, "tasks": ["5.2", "5.4", "5.6"] },
    { "id": 6, "tasks": ["5.3", "5.5", "5.7", "5.8", "5.9"] },
    { "id": 7, "tasks": ["5.10", "6.1"] },
    { "id": 8, "tasks": ["5.11", "6.2"] },
    { "id": 9, "tasks": ["6.3", "8.1"] },
    { "id": 10, "tasks": ["6.4", "8.2"] },
    { "id": 11, "tasks": ["6.5", "8.3", "9.1", "9.2"] },
    { "id": 12, "tasks": ["8.4", "9.3", "9.6"] },
    { "id": 13, "tasks": ["8.5", "9.4", "9.7", "9.8"] },
    { "id": 14, "tasks": ["8.6", "9.5", "9.9", "9.10"] },
    { "id": 15, "tasks": ["9.11", "9.12", "11.1"] },
    { "id": 16, "tasks": ["9.13", "11.2", "12.1", "12.2"] },
    { "id": 17, "tasks": ["9.14", "11.3", "12.3", "12.4"] },
    { "id": 18, "tasks": ["11.4", "12.5", "14.1", "14.2"] },
    { "id": 19, "tasks": ["11.5", "12.6", "14.3", "14.4"] },
    { "id": 20, "tasks": ["12.7", "14.5", "15.1", "15.2"] },
    { "id": 21, "tasks": ["12.8", "14.6", "15.3", "17.1"] },
    { "id": 22, "tasks": ["14.7", "14.8", "15.4", "17.2", "17.3"] },
    { "id": 23, "tasks": ["14.9", "15.5", "17.4"] },
    { "id": 24, "tasks": ["14.10", "15.6", "17.5"] },
    { "id": 25, "tasks": ["17.6", "20.1", "20.2"] },
    { "id": 26, "tasks": ["17.7", "20.3", "20.4"] },
    { "id": 27, "tasks": ["20.5", "20.8", "20.10", "20.12", "20.13"] },
    { "id": 28, "tasks": ["20.6", "20.7", "20.9", "20.11", "22.1"] },
    { "id": 29, "tasks": ["22.2", "22.3", "22.4"] },
    { "id": 30, "tasks": ["22.5", "22.6"] }
  ]
}
```
