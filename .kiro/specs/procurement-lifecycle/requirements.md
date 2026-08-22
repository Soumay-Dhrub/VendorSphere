# Requirements Document

## Introduction

VendorSphere is a B2B vendor, RFQ and procurement management platform built as a Spring Boot 3.4 / Java 21 modular monolith with a Next.js/TypeScript frontend and a PostgreSQL 16 database. Phase 1 (foundation: build, Docker, Flyway, CI) and Phase 2 (authentication: JWT, refresh tokens, RBAC, users, organizations, departments) are implemented.

This specification covers the remaining scope of SRS v1.0 — the complete procurement lifecycle from vendor onboarding through purchase requests, RFQs, vendor quotations, quotation comparison, vendor selection, purchase orders, deliveries, invoices, three-way matching, payments, vendor performance scoring, analytics, in-app notifications and audit logging, plus the frontend screens that expose these capabilities.

The signature end-to-end scenario the finished system supports: a department requests 20 laptops, a procurement officer raises RFQ-2026-001, three vendors submit quotations, the platform normalizes and compares them, a procurement manager explicitly selects one vendor, the platform generates PO-2026-001, the vendor delivers, finance receives an invoice, the PO/delivery/invoice three-way match passes, payment is recorded, and the vendor's performance score is recalculated.

The existing V1 Flyway migration already defines most lifecycle tables. This feature adds the domain logic, DTO/service/controller layers, remaining schema objects (attachments, evaluation criteria weights, match findings, optimistic-lock version columns, additional indexes) and the frontend.

Explicitly out of scope: Kafka, Kubernetes, Redis, Elasticsearch, microservices, blockchain, OCR, real banking or payment-gateway integration, GST filing, full accounting, inventory or warehouse management, AI chatbot, native mobile applications, email delivery of notifications, and generic workflow configuration.

## Glossary

### Systems and Components

- **VendorSphere_API**: The Spring Boot backend application exposing REST endpoints under `/api/v1`.
- **VendorSphere_Web**: The Next.js frontend application.
- **Reference_Number_Generator**: The backend component that allocates human-readable business identifiers such as vendor codes, purchase request numbers, RFQ numbers, purchase order numbers and delivery numbers.
- **Vendor_Service**: The backend component in package `com.vendorsphere.vendor` responsible for vendors, vendor contacts, vendor categories and vendor documents.
- **Purchase_Request_Service**: The backend component in package `com.vendorsphere.procurement` responsible for purchase requests and their line items.
- **RFQ_Service**: The backend component in package `com.vendorsphere.rfq` responsible for RFQs, RFQ items and vendor invitations.
- **Quotation_Service**: The backend component in package `com.vendorsphere.quotation` responsible for vendor quotations and quotation items.
- **Comparison_Engine**: The backend component that normalizes quotations for an RFQ and produces a comparison table.
- **Evaluation_Engine**: The backend component that computes weighted evaluation scores and identifies a recommended quotation.
- **Selection_Service**: The backend component that records the awarded quotation for an RFQ.
- **Purchase_Order_Service**: The backend component in package `com.vendorsphere.purchaseorder` responsible for purchase orders and their items.
- **Delivery_Service**: The backend component in package `com.vendorsphere.delivery` responsible for deliveries and delivery items.
- **Invoice_Service**: The backend component in package `com.vendorsphere.invoice` responsible for invoices and invoice items.
- **Matching_Engine**: The backend component that performs three-way matching between purchase order items, delivery items and invoice items.
- **Payment_Service**: The backend component in package `com.vendorsphere.payment` responsible for payment records.
- **Performance_Engine**: The backend component in package `com.vendorsphere.analytics` that computes vendor performance metrics and scores.
- **Analytics_Service**: The backend component in package `com.vendorsphere.analytics` that computes dashboard figures and analytics reports.
- **Notification_Service**: The backend component in package `com.vendorsphere.notification` responsible for in-app notifications.
- **Audit_Service**: The backend component in package `com.vendorsphere.audit` responsible for audit log entries.
- **Authorization_Layer**: Spring Security method-level and endpoint-level authorization configured in `SecurityConfig` together with `SecurityUtils`.
- **Attachment_Service**: The backend component that stores uploaded files and their metadata for vendor documents, purchase request attachments, RFQ documents, quotation documents, delivery proofs and invoice documents.

### Domain Terms

- **Organization**: The tenant that owns all procurement data. Every business record belongs to exactly one Organization.
- **Vendor**: A supplier registered by an Organization, holding one of the statuses PROSPECTIVE, ACTIVE, SUSPENDED, BLACKLISTED or INACTIVE.
- **Vendor_User**: A user account holding the VENDOR role and linked to exactly one Vendor through `vendors.user_id`.
- **Purchase_Request**: A department requirement holding one of the statuses DRAFT, SUBMITTED, UNDER_REVIEW, APPROVED, REJECTED, PROCUREMENT_STARTED or COMPLETED.
- **RFQ**: A request for quotation holding one of the statuses DRAFT, OPEN, CLOSED, EVALUATION, AWARDED or CANCELLED.
- **Quotation**: A vendor's priced response to an RFQ holding one of the statuses DRAFT, SUBMITTED, UNDER_REVIEW, SELECTED, REJECTED or WITHDRAWN.
- **Purchase_Order**: A commitment to a Vendor holding one of the statuses DRAFT, ISSUED, ACKNOWLEDGED, PARTIALLY_DELIVERED, DELIVERED, CLOSED or CANCELLED.
- **Delivery**: A recorded goods receipt event against a Purchase_Order, containing one or more delivery items.
- **Invoice**: A Vendor's payment claim against a Purchase_Order holding one of the statuses SUBMITTED, UNDER_REVIEW, APPROVED, REJECTED, PARTIALLY_PAID, PAID or OVERDUE.
- **Match_Status**: The three-way matching outcome of an Invoice, holding one of the values PENDING, MATCHED, QUANTITY_MISMATCH, PRICE_MISMATCH, MISSING_DELIVERY or DUPLICATE_INVOICE.
- **Match_Finding**: A single recorded three-way matching discrepancy, carrying a finding type, the affected purchase order item, the compared values and a resolution state.
- **Payment**: A recorded settlement against an Invoice holding one of the statuses PENDING, PARTIALLY_PAID, PAID or FAILED.
- **Evaluation_Score**: A value in the range 0.00 to 100.00 computed for one Quotation as the weighted sum of its price score, delivery score, warranty score and vendor performance score.
- **Criteria_Weights**: The four decimal weights applied to price, delivery, vendor performance and warranty when computing an Evaluation_Score. The four weights sum to 1.00.
- **Performance_Score**: A value in the range 0.00 to 100.00 summarising a Vendor's on-time delivery, quality, pricing competitiveness, responsiveness and fulfilment metrics.
- **Procurement_Cycle_Time**: The number of whole days between the creation timestamp of a Purchase_Request and the timestamp at which the resulting Purchase_Order reaches status CLOSED.
- **Money_Scale**: A `java.math.BigDecimal` scale of 2 with rounding mode HALF_UP.
- **Quantity_Scale**: A `java.math.BigDecimal` scale of 3 with rounding mode HALF_UP.
- **Actor**: The authenticated user whose JWT accompanies a request, resolved through `SecurityUtils.getCurrentUser()`.
- **ApiResponse**: The response envelope record `com.vendorsphere.common.dto.ApiResponse`.
- **PageResponse**: The paginated payload record `com.vendorsphere.common.dto.PageResponse`.

## Requirements

### Requirement 1: Reference Number Generation

**User Story:** As a procurement officer, I want every business record to carry a readable and unique reference number, so that I can identify and discuss records without using database identifiers.

#### Acceptance Criteria

1. WHEN the Reference_Number_Generator allocates a reference number, THE Reference_Number_Generator SHALL produce a value in the format `{PREFIX}-{YYYY}-{NNN}` where `PREFIX` identifies the record type, `YYYY` is the four-digit calendar year of allocation and `NNN` is a zero-padded sequence of at least three digits.
2. THE Reference_Number_Generator SHALL use the prefix `VEN` for vendor codes, `PR` for purchase request numbers, `RFQ` for RFQ numbers, `PO` for purchase order numbers and `DEL` for delivery numbers.
3. WHEN the Reference_Number_Generator allocates a reference number for a record type, THE Reference_Number_Generator SHALL set `NNN` to one greater than the highest sequence previously allocated for the same Organization, record type and calendar year.
4. WHEN the Reference_Number_Generator allocates the first reference number for a given Organization, record type and calendar year, THE Reference_Number_Generator SHALL set `NNN` to `001`.
5. THE Reference_Number_Generator SHALL allocate each reference number within the database transaction of the record that carries the reference number.
6. IF two concurrent requests allocate a reference number for the same Organization, record type and calendar year, THEN THE Reference_Number_Generator SHALL assign distinct sequence values to the two requests.

### Requirement 2: Vendor Registration and Profile Management

**User Story:** As a procurement officer, I want to register and maintain vendor profiles, so that the organization has a single source of supplier information.

#### Acceptance Criteria

1. WHEN a user holding the ADMIN or PROCUREMENT_OFFICER role submits a vendor registration request containing company name, contact person, email, phone, address, tax identifier and category identifier, THE Vendor_Service SHALL create a Vendor for the Actor's Organization with status PROSPECTIVE, rating 0.00, a generated vendor code and a registration timestamp set to the creation instant.
2. IF a vendor registration or update request omits company name or email, THEN THE Vendor_Service SHALL reject the request with HTTP status 400 and a field-level validation message.
3. IF a vendor registration request supplies an email address already used by another Vendor in the same Organization, THEN THE Vendor_Service SHALL reject the request with HTTP status 409 and the message `Vendor email already registered`.
4. WHEN a user holding the ADMIN or PROCUREMENT_OFFICER role submits a vendor update request, THE Vendor_Service SHALL apply the supplied company name, contact person, email, phone, address, tax identifier and category identifier to the Vendor and leave the Vendor status and rating unchanged.
5. WHEN a request retrieves a Vendor by identifier, THE Vendor_Service SHALL return the vendor profile, the vendor category name, the current Performance_Score and the count of vendor documents whose expiry date is within 30 days of the request date.
6. IF a request references a Vendor identifier that belongs to a different Organization than the Actor's Organization, THEN THE Vendor_Service SHALL respond with HTTP status 404 and the message `Vendor not found`.
7. WHERE a Vendor_User account is linked to a Vendor, THE Vendor_Service SHALL restrict that Vendor_User to reading and updating the linked Vendor profile only.

### Requirement 3: Vendor Status Lifecycle

**User Story:** As a procurement manager, I want vendor status changes to follow a controlled lifecycle, so that only qualified vendors participate in procurement.

#### Acceptance Criteria

1. THE Vendor_Service SHALL permit the vendor status transitions PROSPECTIVE→ACTIVE, PROSPECTIVE→INACTIVE, ACTIVE→SUSPENDED, ACTIVE→BLACKLISTED, ACTIVE→INACTIVE, SUSPENDED→ACTIVE, SUSPENDED→BLACKLISTED, SUSPENDED→INACTIVE, BLACKLISTED→INACTIVE and INACTIVE→ACTIVE.
2. IF a status change request specifies a source and target status pair outside the permitted transitions listed in acceptance criterion 3.1, THEN THE Vendor_Service SHALL reject the request with HTTP status 409 and a message naming the source status and the target status.
3. WHEN a user holding the ADMIN or PROCUREMENT_MANAGER role changes a Vendor status with a supplied reason, THE Vendor_Service SHALL persist the new status and record an audit log entry containing the previous status, the new status and the reason.
4. IF a status change request to SUSPENDED, BLACKLISTED or INACTIVE omits a reason, THEN THE Vendor_Service SHALL reject the request with HTTP status 400 and the message `Status change reason is required`.
5. WHEN a user holding only the PROCUREMENT_OFFICER role requests a Vendor status change, THE Authorization_Layer SHALL respond with HTTP status 403.

### Requirement 4: Vendor Contacts and Categories

**User Story:** As a procurement officer, I want to maintain multiple contacts per vendor and a category taxonomy, so that I can reach the right person and group vendors by supply area.

#### Acceptance Criteria

1. WHEN a user holding the ADMIN or PROCUREMENT_OFFICER role adds a vendor contact containing name, email, phone and designation, THE Vendor_Service SHALL create the vendor contact against the specified Vendor.
2. WHEN a vendor contact is created or updated with the primary contact flag set to true, THE Vendor_Service SHALL set the primary contact flag to false on all other contacts of the same Vendor.
3. WHEN a request lists the contacts of a Vendor, THE Vendor_Service SHALL return the contacts ordered with the primary contact first and remaining contacts ordered by name ascending.
4. WHEN a user holding the ADMIN or PROCUREMENT_OFFICER role creates a vendor category with a name and description, THE Vendor_Service SHALL create the vendor category for the Actor's Organization.
5. IF a vendor category creation request supplies a name already used by another vendor category in the same Organization, THEN THE Vendor_Service SHALL reject the request with HTTP status 409 and the message `Vendor category already exists`.
6. IF a request deletes a vendor category that is referenced by at least one Vendor, THEN THE Vendor_Service SHALL reject the request with HTTP status 409 and a message stating the number of vendors referencing the vendor category.

### Requirement 5: Vendor Documents and Expiry Tracking

**User Story:** As a procurement officer, I want to store vendor compliance documents with expiry dates, so that I can act before a document lapses.

#### Acceptance Criteria

1. WHEN a user holding the ADMIN or PROCUREMENT_OFFICER role or the linked Vendor_User uploads a vendor document with a document type, a file and an optional expiry date, THE Vendor_Service SHALL store the document metadata against the Vendor and record the upload timestamp.
2. THE Vendor_Service SHALL accept the vendor document types GST_CERTIFICATE, REGISTRATION_CERTIFICATE, TAX_DOCUMENT, COMPLIANCE_CERTIFICATE, BANK_DETAILS and AGREEMENT.
3. IF a vendor document upload request supplies a document type outside the list in acceptance criterion 5.2, THEN THE Vendor_Service SHALL reject the request with HTTP status 400 and a message listing the accepted document types.
4. WHEN a request lists the documents of a Vendor, THE Vendor_Service SHALL return for each document an expiry state of VALID when the expiry date is absent or more than 30 days after the request date, EXPIRING_SOON when the expiry date is between the request date and 30 days after the request date inclusive, and EXPIRED when the expiry date is before the request date.
5. WHEN the daily document expiry evaluation runs, THE Notification_Service SHALL create one in-app notification for each user holding the ADMIN or PROCUREMENT_OFFICER role in the Organization for each vendor document whose expiry date falls exactly 30, 7 or 1 day after the evaluation date.
6. THE Vendor_Service SHALL run the document expiry evaluation once per calendar day at 00:30 UTC.

### Requirement 6: Vendor Search and Listing

**User Story:** As a procurement officer, I want to search and filter the vendor list, so that I can find suitable suppliers quickly.

#### Acceptance Criteria

1. WHEN a request lists vendors, THE Vendor_Service SHALL return a PageResponse of vendor summaries restricted to the Actor's Organization.
2. WHERE a company name search term is supplied, THE Vendor_Service SHALL return only vendors whose company name contains the search term using case-insensitive matching.
3. WHERE a category identifier filter is supplied, THE Vendor_Service SHALL return only vendors assigned to that vendor category.
4. WHERE a status filter is supplied, THE Vendor_Service SHALL return only vendors whose status equals the supplied status.
5. WHERE a minimum rating filter is supplied, THE Vendor_Service SHALL return only vendors whose rating is greater than or equal to the supplied value.
6. WHERE two or more of the filters named in acceptance criteria 6.2 through 6.5 are supplied, THE Vendor_Service SHALL return only vendors satisfying every supplied filter.
7. THE Vendor_Service SHALL accept sorting by company name, registration timestamp, rating and status in ascending or descending direction.

### Requirement 7: Purchase Request Authoring

**User Story:** As a requester, I want to raise a purchase requirement with line items and supporting files, so that procurement can act on my department's need.

#### Acceptance Criteria

1. WHEN a user holding the REQUESTER, PROCUREMENT_OFFICER, PROCUREMENT_MANAGER or ADMIN role creates a purchase request containing title, department identifier, business justification, required date, priority and estimated budget, THE Purchase_Request_Service SHALL create a Purchase_Request with status DRAFT, a generated request number, the Actor as requester and the Actor's Organization.
2. THE Purchase_Request_Service SHALL accept the priority values LOW, MEDIUM, HIGH and URGENT and SHALL apply MEDIUM when the priority is absent.
3. WHILE a Purchase_Request holds status DRAFT, THE Purchase_Request_Service SHALL permit the requester and users holding the PROCUREMENT_OFFICER, PROCUREMENT_MANAGER or ADMIN role to add, update and remove purchase request items.
4. WHEN a purchase request item is created with item name, quantity, unit and specification, THE Purchase_Request_Service SHALL store the quantity at Quantity_Scale and assign a sort order equal to the current item count of the Purchase_Request.
5. IF a purchase request item is created or updated with a quantity less than or equal to zero, THEN THE Purchase_Request_Service SHALL reject the request with HTTP status 400 and the message `Quantity must be greater than zero`.
6. WHILE a Purchase_Request holds status DRAFT, THE Purchase_Request_Service SHALL permit the requester to attach files to the Purchase_Request.
7. IF a request submits a Purchase_Request that holds fewer than one purchase request item, THEN THE Purchase_Request_Service SHALL reject the request with HTTP status 400 and the message `Purchase request requires at least one item`.

### Requirement 8: Purchase Request Submission and Review

**User Story:** As a procurement manager, I want to review, approve or reject submitted purchase requests with recorded comments, so that spending decisions are deliberate and traceable.

#### Acceptance Criteria

1. THE Purchase_Request_Service SHALL permit the purchase request status transitions DRAFT→SUBMITTED, SUBMITTED→UNDER_REVIEW, UNDER_REVIEW→APPROVED, UNDER_REVIEW→REJECTED, APPROVED→PROCUREMENT_STARTED and PROCUREMENT_STARTED→COMPLETED.
2. IF a purchase request status change specifies a source and target status pair outside the permitted transitions listed in acceptance criterion 8.1, THEN THE Purchase_Request_Service SHALL reject the request with HTTP status 409 and a message naming the source status and the target status.
3. WHEN a Purchase_Request transitions from DRAFT to SUBMITTED, THE Purchase_Request_Service SHALL reject subsequent requests that add, update or remove purchase request items with HTTP status 409 and the message `Purchase request items are locked after submission`.
4. WHEN a Purchase_Request transitions from DRAFT to SUBMITTED, THE Notification_Service SHALL create one in-app notification for each user holding the PROCUREMENT_MANAGER role in the Organization.
5. WHEN a user holding the PROCUREMENT_MANAGER or ADMIN role approves a Purchase_Request, THE Purchase_Request_Service SHALL set the status to APPROVED and record the Actor as reviewer, the decision instant as review timestamp and the supplied comments as review notes.
6. IF a user holding the PROCUREMENT_MANAGER or ADMIN role rejects a Purchase_Request without supplying a reason, THEN THE Purchase_Request_Service SHALL reject the request with HTTP status 400 and the message `Rejection reason is required`.
7. WHEN a user holding the PROCUREMENT_MANAGER or ADMIN role rejects a Purchase_Request with a reason, THE Purchase_Request_Service SHALL set the status to REJECTED, record the reason as review notes and create an in-app notification for the requester.
8. WHEN a user holding only the REQUESTER role requests approval or rejection of a Purchase_Request, THE Authorization_Layer SHALL respond with HTTP status 403.
9. WHEN a requester retrieves a Purchase_Request that the requester created, THE Purchase_Request_Service SHALL return the current status, the reviewer name when a reviewer is recorded, the review timestamp when present, the review notes when present and the identifiers of RFQs derived from the Purchase_Request.

### Requirement 9: RFQ Creation and Item Management

**User Story:** As a procurement officer, I want to raise an RFQ from an approved purchase request, so that I can collect competing quotations for an authorised requirement.

#### Acceptance Criteria

1. WHEN a user holding the PROCUREMENT_OFFICER, PROCUREMENT_MANAGER or ADMIN role creates an RFQ referencing a Purchase_Request that holds status APPROVED or PROCUREMENT_STARTED, THE RFQ_Service SHALL create an RFQ with status DRAFT, a generated RFQ number, the Actor as creator, and the supplied title, description, opening date, closing date, currency, delivery location and terms.
2. IF an RFQ creation request references a Purchase_Request whose status is other than APPROVED or PROCUREMENT_STARTED, THEN THE RFQ_Service SHALL reject the request with HTTP status 409 and a message naming the current purchase request status.
3. WHEN an RFQ is created from a Purchase_Request, THE RFQ_Service SHALL copy each purchase request item into an RFQ item carrying the same item name, quantity, unit, specification and sort order, and SHALL record the source purchase request item identifier on each RFQ item.
4. WHEN the first RFQ referencing a Purchase_Request is created, THE Purchase_Request_Service SHALL transition that Purchase_Request from APPROVED to PROCUREMENT_STARTED.
5. WHILE an RFQ holds status DRAFT, THE RFQ_Service SHALL permit users holding the PROCUREMENT_OFFICER, PROCUREMENT_MANAGER or ADMIN role to add, update and remove RFQ items and to edit the RFQ title, description, opening date, closing date, currency, delivery location and terms.
6. IF an RFQ creation or update request supplies a closing date earlier than or equal to the opening date, THEN THE RFQ_Service SHALL reject the request with HTTP status 400 and the message `Closing date must be after opening date`.
7. WHILE an RFQ holds status DRAFT, THE RFQ_Service SHALL permit users holding the PROCUREMENT_OFFICER, PROCUREMENT_MANAGER or ADMIN role to attach specification and terms documents to the RFQ.

### Requirement 10: RFQ Vendor Invitation

**User Story:** As a procurement officer, I want to invite selected active vendors to an RFQ, so that qualified suppliers can submit quotations.

#### Acceptance Criteria

1. WHEN a user holding the PROCUREMENT_OFFICER, PROCUREMENT_MANAGER or ADMIN role invites one or more vendors to an RFQ, THE RFQ_Service SHALL create one RFQ vendor invitation per vendor with status INVITED, the invitation instant and the Actor as inviter.
2. IF an invitation request includes a Vendor whose status is other than ACTIVE, THEN THE RFQ_Service SHALL reject the whole request with HTTP status 409 and a message naming the vendor company name and the vendor status.
3. IF an invitation request includes a Vendor already invited to the same RFQ, THEN THE RFQ_Service SHALL reject the whole request with HTTP status 409 and the message `Vendor already invited to this RFQ`.
4. WHEN an RFQ vendor invitation is created and the RFQ holds status OPEN, THE Notification_Service SHALL create one in-app notification for each Vendor_User linked to the invited Vendor.
5. WHEN a Vendor_User first retrieves an RFQ to which the linked Vendor is invited, THE RFQ_Service SHALL transition that RFQ vendor invitation from INVITED to VIEWED.
6. IF a request transitions an RFQ from DRAFT to OPEN while the RFQ holds fewer than one vendor invitation, THEN THE RFQ_Service SHALL reject the request with HTTP status 400 and the message `RFQ requires at least one invited vendor`.
7. WHEN a Vendor_User lists RFQs, THE RFQ_Service SHALL return only RFQs to which the linked Vendor holds an invitation and whose status is OPEN, CLOSED, EVALUATION or AWARDED.

### Requirement 11: RFQ Publication, Closing and Cancellation

**User Story:** As a procurement officer, I want RFQs to open, close and cancel under controlled rules, so that the bidding window is fair and auditable.

#### Acceptance Criteria

1. THE RFQ_Service SHALL permit the RFQ status transitions DRAFT→OPEN, OPEN→CLOSED, CLOSED→EVALUATION, EVALUATION→AWARDED, DRAFT→CANCELLED, OPEN→CANCELLED, CLOSED→CANCELLED and EVALUATION→CANCELLED.
2. IF an RFQ status change specifies a source and target status pair outside the permitted transitions listed in acceptance criterion 11.1, THEN THE RFQ_Service SHALL reject the request with HTTP status 409 and a message naming the source status and the target status.
3. WHEN the scheduled RFQ closing evaluation runs and an RFQ holds status OPEN with a closing date earlier than the evaluation instant, THE RFQ_Service SHALL transition that RFQ to CLOSED.
4. THE RFQ_Service SHALL run the RFQ closing evaluation every 5 minutes.
5. WHEN the scheduled RFQ closing evaluation runs and an RFQ holds status OPEN with a closing date between 24 and 25 hours after the evaluation instant, THE Notification_Service SHALL create one in-app notification for each Vendor_User linked to an invited Vendor that has not submitted a Quotation for that RFQ.
6. IF a request cancels an RFQ without supplying a reason, THEN THE RFQ_Service SHALL reject the request with HTTP status 400 and the message `Cancellation reason is required`.
7. WHEN a user holding the PROCUREMENT_MANAGER or ADMIN role cancels an RFQ with a reason, THE RFQ_Service SHALL set the RFQ status to CANCELLED, persist the reason, set every Quotation of that RFQ whose status is SUBMITTED or UNDER_REVIEW to REJECTED, and create one in-app notification for each Vendor_User linked to an invited Vendor.
8. IF a request cancels an RFQ that holds status AWARDED, THEN THE RFQ_Service SHALL reject the request with HTTP status 409 and the message `Awarded RFQ cannot be cancelled`.

### Requirement 12: Vendor Quotation Submission

**User Story:** As a vendor, I want to submit a priced quotation against each RFQ item, so that I can compete for the order.

#### Acceptance Criteria

1. WHILE an RFQ holds status OPEN and the request instant is earlier than the RFQ closing date, THE Quotation_Service SHALL permit a Vendor_User linked to an invited Vendor to create a Quotation for that RFQ.
2. WHEN a Vendor_User submits a Quotation, THE Quotation_Service SHALL require one quotation item for each RFQ item of the source RFQ.
3. IF a quotation submission omits a quotation item for at least one RFQ item, THEN THE Quotation_Service SHALL reject the request with HTTP status 400 and a message listing the item names lacking a price.
4. WHEN a Vendor_User submits a Quotation, THE Quotation_Service SHALL store per quotation item the quantity, unit price, tax rate and discount amount, and per Quotation the shipping amount, delivery period in days, payment terms, warranty text, warranty duration in months, validity date and notes.
5. IF a quotation submission supplies a unit price less than zero, a tax rate outside the range 0.00 to 100.00, a discount amount less than zero or a shipping amount less than zero, THEN THE Quotation_Service SHALL reject the request with HTTP status 400 and a field-level validation message.
6. IF a quotation submission supplies a validity date earlier than the RFQ closing date, THEN THE Quotation_Service SHALL reject the request with HTTP status 400 and the message `Quotation validity date must be on or after the RFQ closing date`.
7. WHEN a Quotation is submitted, THE Quotation_Service SHALL set the Quotation status to SUBMITTED, record the submission instant, transition the corresponding RFQ vendor invitation to RESPONDED, and create one in-app notification for each user holding the PROCUREMENT_OFFICER role in the Organization.
8. WHILE an RFQ holds status OPEN and the request instant is earlier than the RFQ closing date, THE Quotation_Service SHALL permit the submitting Vendor_User to revise the Quotation and its items and SHALL recompute the stored totals on each revision.
9. IF a Vendor_User creates or revises a Quotation at or after the RFQ closing date, THEN THE Quotation_Service SHALL reject the request with HTTP status 409 and the message `RFQ is closed for quotation submission`.
10. WHILE an RFQ holds status OPEN, THE Quotation_Service SHALL permit the submitting Vendor_User to attach documents to the Quotation.

### Requirement 13: Server-Side Quotation Total Computation

**User Story:** As a procurement manager, I want quotation totals computed by the platform, so that comparison figures cannot be manipulated by a vendor client.

#### Acceptance Criteria

1. WHEN a Quotation is created or revised, THE Quotation_Service SHALL compute each quotation item tax amount as quantity multiplied by unit price multiplied by tax rate divided by 100, at Money_Scale.
2. WHEN a Quotation is created or revised, THE Quotation_Service SHALL compute each quotation item line total as quantity multiplied by unit price plus the item tax amount minus the item discount amount, at Money_Scale.
3. WHEN a Quotation is created or revised, THE Quotation_Service SHALL compute the Quotation subtotal as the sum over quotation items of quantity multiplied by unit price, at Money_Scale.
4. WHEN a Quotation is created or revised, THE Quotation_Service SHALL compute the Quotation tax amount as the sum of quotation item tax amounts and the Quotation discount amount as the sum of quotation item discount amounts, at Money_Scale.
5. WHEN a Quotation is created or revised, THE Quotation_Service SHALL compute the Quotation total amount as subtotal plus tax amount minus discount amount plus shipping amount, at Money_Scale.
6. WHEN a quotation request payload carries values for quotation item tax amount, quotation item line total, Quotation subtotal, Quotation tax amount, Quotation discount amount or Quotation total amount, THE Quotation_Service SHALL replace every supplied value with the value computed by acceptance criteria 13.1 through 13.5.
7. THE Quotation_Service SHALL expose quotation item tax amount, quotation item line total, subtotal, tax amount, discount amount and total amount as read-only fields in every quotation response DTO.

### Requirement 14: Quotation Confidentiality Between Vendors

**User Story:** As a vendor, I want my pricing to stay private, so that competitors cannot see my bid.

#### Acceptance Criteria

1. WHEN a Vendor_User requests a Quotation, THE Quotation_Service SHALL return the Quotation only when the Quotation belongs to the Vendor linked to that Vendor_User.
2. IF a Vendor_User requests a Quotation belonging to another Vendor, THEN THE Quotation_Service SHALL respond with HTTP status 404 and the message `Quotation not found`.
3. WHEN a Vendor_User lists quotations for an RFQ, THE Quotation_Service SHALL return only quotations belonging to the Vendor linked to that Vendor_User.
4. IF a Vendor_User requests the quotation comparison of an RFQ, THEN THE Authorization_Layer SHALL respond with HTTP status 403.
5. WHILE an RFQ holds status OPEN, THE Quotation_Service SHALL return the count of submitted quotations for that RFQ to users holding the PROCUREMENT_OFFICER, PROCUREMENT_MANAGER or ADMIN role and SHALL exclude quotation prices from responses to those users until the RFQ status is CLOSED or later.

### Requirement 15: Quotation Comparison

**User Story:** As a procurement officer, I want a normalized side-by-side comparison of all quotations for an RFQ, so that I can assess bids on equivalent terms.

#### Acceptance Criteria

1. WHEN a user holding the PROCUREMENT_OFFICER, PROCUREMENT_MANAGER or ADMIN role requests the comparison of an RFQ whose status is CLOSED, EVALUATION or AWARDED, THE Comparison_Engine SHALL return one comparison row for each Quotation of that RFQ whose status is SUBMITTED, UNDER_REVIEW, SELECTED or REJECTED.
2. THE Comparison_Engine SHALL include in each comparison row the vendor company name, the vendor Performance_Score, the Quotation total amount, the Quotation subtotal, the Quotation tax amount, the Quotation discount amount, the Quotation shipping amount, the delivery period in days, the warranty duration in months, the payment terms and the validity date.
3. THE Comparison_Engine SHALL include in each comparison row one per-item entry for each RFQ item carrying the RFQ item name, the RFQ item quantity, the quoted unit price and the quoted line total.
4. IF a request asks for the comparison of an RFQ whose status is DRAFT or OPEN, THEN THE Comparison_Engine SHALL reject the request with HTTP status 409 and the message `Comparison is available after the RFQ closes`.
5. IF a request asks for the comparison of an RFQ that holds fewer than one Quotation with status SUBMITTED, UNDER_REVIEW, SELECTED or REJECTED, THEN THE Comparison_Engine SHALL respond with an empty comparison row collection and the RFQ summary.
6. THE Comparison_Engine SHALL order comparison rows by Evaluation_Score descending and, for equal Evaluation_Scores, by Quotation total amount ascending.

### Requirement 16: Evaluation Scoring and Criteria Weighting

**User Story:** As a procurement manager, I want each quotation scored on price, delivery, warranty and vendor history, so that I can see an objective ranking and a recommendation.

#### Acceptance Criteria

1. WHEN the Evaluation_Engine scores the quotations of an RFQ, THE Evaluation_Engine SHALL compute the price score of each Quotation as the lowest total amount among the scored quotations divided by that Quotation total amount, multiplied by 100, at Money_Scale.
2. WHEN the Evaluation_Engine scores the quotations of an RFQ, THE Evaluation_Engine SHALL compute the delivery score of each Quotation as the shortest delivery period in days among the scored quotations divided by that Quotation delivery period in days, multiplied by 100, at Money_Scale.
3. WHEN the Evaluation_Engine scores the quotations of an RFQ, THE Evaluation_Engine SHALL compute the warranty score of each Quotation as that Quotation warranty duration in months divided by the longest warranty duration in months among the scored quotations, multiplied by 100, at Money_Scale.
4. WHEN the Evaluation_Engine scores the quotations of an RFQ, THE Evaluation_Engine SHALL set the performance score of each Quotation to the current Performance_Score of the quoting Vendor.
5. WHERE a quoting Vendor holds no completed Purchase_Order, THE Evaluation_Engine SHALL set the performance score of that Quotation to 50.00.
6. WHERE a Quotation carries a delivery period in days equal to zero or absent, THE Evaluation_Engine SHALL set the delivery score of that Quotation to 0.00.
7. WHERE a Quotation carries a warranty duration in months equal to zero or absent, THE Evaluation_Engine SHALL set the warranty score of that Quotation to 0.00.
8. WHEN the Evaluation_Engine scores a Quotation, THE Evaluation_Engine SHALL compute the Evaluation_Score as the price score multiplied by the price weight, plus the delivery score multiplied by the delivery weight, plus the performance score multiplied by the performance weight, plus the warranty score multiplied by the warranty weight, at Money_Scale.
9. THE Evaluation_Engine SHALL apply the default Criteria_Weights price 0.40, delivery 0.25, performance 0.25 and warranty 0.10 when the Organization holds no stored Criteria_Weights.
10. WHERE an Organization holds stored Criteria_Weights, THE Evaluation_Engine SHALL apply the stored weights.
11. IF a request stores Criteria_Weights whose four values sum to a value other than 1.00, THEN THE Evaluation_Engine SHALL reject the request with HTTP status 400 and the message `Criteria weights must sum to 1.00`.
12. WHEN the Evaluation_Engine scores the quotations of an RFQ, THE Evaluation_Engine SHALL mark as recommended exactly one Quotation, being the Quotation with the highest Evaluation_Score and, for equal Evaluation_Scores, the lowest total amount.
13. THE Evaluation_Engine SHALL persist for each scored Quotation the price score, delivery score, warranty score, performance score, Evaluation_Score and recommended flag as a vendor evaluation record.
14. THE Evaluation_Engine SHALL leave the Quotation status and the RFQ status unchanged when scoring quotations.

### Requirement 17: Vendor Selection and Award

**User Story:** As a procurement manager, I want to select exactly one winning quotation with a recorded justification, so that the award decision is deliberate and traceable.

#### Acceptance Criteria

1. WHEN a user holding the PROCUREMENT_MANAGER or ADMIN role selects a Quotation of an RFQ whose status is CLOSED or EVALUATION, THE Selection_Service SHALL set that Quotation status to SELECTED, set every other Quotation of the RFQ whose status is SUBMITTED or UNDER_REVIEW to REJECTED, set the RFQ status to AWARDED and set the RFQ vendor invitation of the selected Vendor to AWARDED.
2. WHEN a Quotation is selected, THE Selection_Service SHALL persist a vendor selection record containing the RFQ identifier, the selected Quotation identifier, the selected Vendor identifier, the Actor as selecting user, the selection instant and the supplied justification.
3. IF a selection request omits a justification, THEN THE Selection_Service SHALL reject the request with HTTP status 400 and the message `Selection justification is required`.
4. IF a selection request targets an RFQ that already holds a vendor selection record, THEN THE Selection_Service SHALL reject the request with HTTP status 409 and the message `RFQ is already awarded`.
5. IF a selection request targets a Quotation whose status is WITHDRAWN or REJECTED, THEN THE Selection_Service SHALL reject the request with HTTP status 409 and a message naming the quotation status.
6. WHEN a user holding the PROCUREMENT_MANAGER, PROCUREMENT_OFFICER or ADMIN role records a comment against a Quotation, THE Selection_Service SHALL persist the comment on the vendor evaluation record of that Quotation.
7. THE Selection_Service SHALL require an explicit selection request for every award and SHALL leave the RFQ status unchanged when the Evaluation_Engine marks a Quotation as recommended.
8. WHEN a Quotation is selected, THE Notification_Service SHALL create one in-app notification for each Vendor_User linked to an invited Vendor of that RFQ, stating whether the linked Vendor was selected.
9. WHEN a user holding the PROCUREMENT_MANAGER or ADMIN role requests the historical performance of a Vendor, THE Performance_Engine SHALL return the five performance metrics, the Performance_Score, the count of awarded purchase orders and the count of completed deliveries for that Vendor.
10. THE Selection_Service SHALL execute the operations described in acceptance criteria 17.1 and 17.2 within one database transaction.

### Requirement 18: Purchase Order Generation

**User Story:** As a procurement officer, I want a purchase order generated from the winning quotation, so that the commitment mirrors the accepted bid without retyping.

#### Acceptance Criteria

1. WHEN a user holding the PROCUREMENT_OFFICER, PROCUREMENT_MANAGER or ADMIN role generates a Purchase_Order from an RFQ that holds a vendor selection record, THE Purchase_Order_Service SHALL create a Purchase_Order with status DRAFT, a generated purchase order number, the selected Vendor, the source RFQ identifier and the selected Quotation identifier.
2. WHEN a Purchase_Order is generated, THE Purchase_Order_Service SHALL create one purchase order item for each quotation item of the selected Quotation carrying the item name, quantity, unit price, tax rate, tax amount and line total of the quotation item, and SHALL set the delivered quantity of each purchase order item to zero.
3. WHEN a Purchase_Order is generated, THE Purchase_Order_Service SHALL set the Purchase_Order subtotal, tax amount and total amount to the corresponding values of the selected Quotation.
4. WHEN a Purchase_Order is generated, THE Purchase_Order_Service SHALL set the delivery address to the RFQ delivery location, the payment terms to the Quotation payment terms and the expected delivery date to the generation date plus the Quotation delivery period in days.
5. IF a generation request targets an RFQ that holds no vendor selection record, THEN THE Purchase_Order_Service SHALL reject the request with HTTP status 409 and the message `RFQ has no selected quotation`.
6. IF a generation request targets an RFQ that already holds a Purchase_Order, THEN THE Purchase_Order_Service SHALL reject the request with HTTP status 409 and a message naming the existing purchase order number.
7. WHILE a Purchase_Order holds status DRAFT, THE Purchase_Order_Service SHALL permit users holding the PROCUREMENT_OFFICER, PROCUREMENT_MANAGER or ADMIN role to edit the delivery address, expected delivery date, payment terms and terms and conditions.
8. THE Purchase_Order_Service SHALL execute purchase order generation within one database transaction.

### Requirement 19: Purchase Order Lifecycle

**User Story:** As a procurement officer, I want purchase order status to reflect issuance, vendor acknowledgement and delivery progress, so that I can see commitment state at a glance.

#### Acceptance Criteria

1. THE Purchase_Order_Service SHALL permit the purchase order status transitions DRAFT→ISSUED, ISSUED→ACKNOWLEDGED, ISSUED→PARTIALLY_DELIVERED, ACKNOWLEDGED→PARTIALLY_DELIVERED, ACKNOWLEDGED→DELIVERED, PARTIALLY_DELIVERED→DELIVERED, DELIVERED→CLOSED, DRAFT→CANCELLED, ISSUED→CANCELLED, ACKNOWLEDGED→CANCELLED and PARTIALLY_DELIVERED→CANCELLED.
2. IF a purchase order status change specifies a source and target status pair outside the permitted transitions listed in acceptance criterion 19.1, THEN THE Purchase_Order_Service SHALL reject the request with HTTP status 409 and a message naming the source status and the target status.
3. WHEN a user holding the PROCUREMENT_OFFICER, PROCUREMENT_MANAGER or ADMIN role issues a Purchase_Order, THE Purchase_Order_Service SHALL set the status to ISSUED, record the Actor as issuing user and the issue instant, and create one in-app notification for each Vendor_User linked to the Purchase_Order Vendor.
4. WHEN a Vendor_User linked to the Purchase_Order Vendor acknowledges a Purchase_Order that holds status ISSUED, THE Purchase_Order_Service SHALL set the status to ACKNOWLEDGED and record the acknowledgement instant.
5. IF a Vendor_User acknowledges a Purchase_Order belonging to another Vendor, THEN THE Purchase_Order_Service SHALL respond with HTTP status 404 and the message `Purchase order not found`.
6. IF a request cancels a Purchase_Order whose cumulative delivered quantity across purchase order items is greater than zero, THEN THE Purchase_Order_Service SHALL reject the request with HTTP status 409 and the message `Purchase order with recorded deliveries cannot be cancelled`.
7. IF a request cancels a Purchase_Order without supplying a reason, THEN THE Purchase_Order_Service SHALL reject the request with HTTP status 400 and the message `Cancellation reason is required`.
8. IF a request closes a Purchase_Order whose status is other than DELIVERED, THEN THE Purchase_Order_Service SHALL reject the request with HTTP status 409 and a message naming the current purchase order status.
9. WHEN a Purchase_Order transitions to CLOSED, THE Performance_Engine SHALL recalculate the Performance_Score of the Purchase_Order Vendor.
10. WHEN a Vendor_User lists purchase orders, THE Purchase_Order_Service SHALL return only purchase orders whose Vendor is the Vendor linked to that Vendor_User and whose status is other than DRAFT.

### Requirement 20: Delivery Recording

**User Story:** As a procurement officer, I want to record each goods receipt including partial deliveries and damaged or rejected quantities, so that receipt history is accurate.

#### Acceptance Criteria

1. WHEN a user holding the PROCUREMENT_OFFICER, PROCUREMENT_MANAGER or ADMIN role records a Delivery against a Purchase_Order whose status is ISSUED, ACKNOWLEDGED or PARTIALLY_DELIVERED, THE Delivery_Service SHALL create a Delivery with a generated delivery number, the supplied delivery date, the supplied notes, the supplied proof of delivery document reference and the Actor as receiver.
2. WHEN a Delivery is recorded, THE Delivery_Service SHALL create one delivery item for each supplied purchase order item entry carrying the received quantity, damaged quantity, rejected quantity and notes, at Quantity_Scale.
3. THE Delivery_Service SHALL permit two or more Delivery records against the same purchase order item.
4. IF a delivery item supplies a received quantity less than or equal to zero, THEN THE Delivery_Service SHALL reject the request with HTTP status 400 and the message `Received quantity must be greater than zero`.
5. IF a delivery item supplies a damaged quantity or rejected quantity greater than the received quantity of the same delivery item, THEN THE Delivery_Service SHALL reject the request with HTTP status 400 and the message `Damaged and rejected quantities cannot exceed the received quantity`.
6. IF a delivery item raises the cumulative received quantity of a purchase order item above the ordered quantity of that purchase order item, THEN THE Delivery_Service SHALL reject the request with HTTP status 409 and a message naming the item name, the ordered quantity and the cumulative received quantity.
7. IF a delivery item references a purchase order item belonging to a different Purchase_Order than the Delivery, THEN THE Delivery_Service SHALL reject the request with HTTP status 400 and the message `Delivery item does not belong to the purchase order`.
8. WHEN a Delivery is recorded, THE Notification_Service SHALL create one in-app notification for each user holding the PROCUREMENT_OFFICER role in the Organization and for each user holding the FINANCE role in the Organization.
9. WHEN a request retrieves the delivery history of a Purchase_Order, THE Delivery_Service SHALL return per purchase order item the ordered quantity, the cumulative received quantity, the cumulative damaged quantity, the cumulative rejected quantity and the outstanding quantity.
10. THE Delivery_Service SHALL execute delivery recording and purchase order progress derivation within one database transaction.

### Requirement 21: Delivery Progress Derivation and Overdue Flagging

**User Story:** As a procurement officer, I want purchase order delivery progress derived automatically and late deliveries flagged, so that I can chase suppliers without manual bookkeeping.

#### Acceptance Criteria

1. WHEN a Delivery is recorded, THE Delivery_Service SHALL set the delivered quantity of each affected purchase order item to the sum of received quantities across all delivery items referencing that purchase order item.
2. WHEN the delivered quantity of every purchase order item of a Purchase_Order is greater than or equal to the ordered quantity of that purchase order item, THE Purchase_Order_Service SHALL set the Purchase_Order status to DELIVERED.
3. WHILE at least one purchase order item of a Purchase_Order holds a delivered quantity greater than zero and less than the ordered quantity, THE Purchase_Order_Service SHALL set the Purchase_Order status to PARTIALLY_DELIVERED.
4. WHEN the scheduled overdue delivery evaluation runs and a Purchase_Order holds status ISSUED or ACKNOWLEDGED with an expected delivery date earlier than the evaluation date and a cumulative delivered quantity of zero across all purchase order items, THE Delivery_Service SHALL set the delivery overdue flag of that Purchase_Order to true.
5. WHEN the scheduled overdue delivery evaluation sets the delivery overdue flag of a Purchase_Order to true and the flag was previously false, THE Notification_Service SHALL create one in-app notification for each user holding the PROCUREMENT_OFFICER role in the Organization.
6. WHEN a Delivery is recorded against a Purchase_Order whose delivery overdue flag is true, THE Delivery_Service SHALL set the delivery overdue flag of that Purchase_Order to false.
7. THE Delivery_Service SHALL run the overdue delivery evaluation once per calendar day at 01:00 UTC.

### Requirement 22: Invoice Submission

**User Story:** As a vendor, I want to submit an invoice against a purchase order, so that finance can process my payment.

#### Acceptance Criteria

1. WHEN a Vendor_User linked to the Purchase_Order Vendor, or a user holding the FINANCE, PROCUREMENT_OFFICER or ADMIN role, submits an Invoice against a Purchase_Order whose status is ACKNOWLEDGED, PARTIALLY_DELIVERED, DELIVERED or CLOSED, THE Invoice_Service SHALL create an Invoice with status SUBMITTED, match status PENDING and the supplied invoice number, invoice date, due date, subtotal, tax amount, discount amount, total amount and invoice document reference.
2. WHEN an Invoice is submitted, THE Invoice_Service SHALL create one invoice item for each supplied entry carrying the referenced purchase order item, item name, quantity, unit price, tax amount and line total.
3. WHEN an Invoice is submitted, THE Invoice_Service SHALL compute each invoice item line total as quantity multiplied by unit price plus the invoice item tax amount, at Money_Scale, and SHALL compute the Invoice total amount as the sum of invoice item line totals minus the discount amount, at Money_Scale.
4. IF an Invoice submission supplies an invoice number already used by the same Vendor within the same Organization, THEN THE Invoice_Service SHALL reject the request with HTTP status 409 and the message `Invoice number already exists for this vendor`.
5. IF an Invoice submission supplies a due date earlier than the invoice date, THEN THE Invoice_Service SHALL reject the request with HTTP status 400 and the message `Due date must be on or after the invoice date`.
6. IF an Invoice submission targets a Purchase_Order whose status is DRAFT, ISSUED or CANCELLED, THEN THE Invoice_Service SHALL reject the request with HTTP status 409 and a message naming the current purchase order status.
7. WHEN an Invoice is submitted, THE Notification_Service SHALL create one in-app notification for each user holding the FINANCE role in the Organization.
8. WHEN a Vendor_User lists invoices, THE Invoice_Service SHALL return only invoices whose Vendor is the Vendor linked to that Vendor_User.

### Requirement 23: Three-Way Matching

**User Story:** As a finance user, I want the platform to compare purchase order, delivery and invoice figures automatically, so that I catch over-billing before payment.

#### Acceptance Criteria

1. WHEN an Invoice is submitted, THE Matching_Engine SHALL compare for each invoice item the ordered quantity and unit price of the referenced purchase order item, the cumulative received quantity of that purchase order item, and the invoiced quantity and unit price.
2. IF an invoice item carries an invoiced quantity greater than the cumulative received quantity of the referenced purchase order item, THEN THE Matching_Engine SHALL record a Match_Finding of type QUANTITY_MISMATCH carrying the item name, the invoiced quantity and the cumulative received quantity.
3. IF an invoice item carries a unit price whose absolute difference from the unit price of the referenced purchase order item is greater than 0.01, THEN THE Matching_Engine SHALL record a Match_Finding of type PRICE_MISMATCH carrying the item name, the invoiced unit price and the purchase order unit price.
4. IF the Purchase_Order referenced by an Invoice holds no Delivery record, THEN THE Matching_Engine SHALL record a Match_Finding of type MISSING_DELIVERY carrying the purchase order number.
5. IF the Purchase_Order referenced by an Invoice already holds another Invoice whose match status is MATCHED and whose invoice item quantities and unit prices equal those of the submitted Invoice, THEN THE Matching_Engine SHALL record a Match_Finding of type DUPLICATE_INVOICE carrying the invoice number of the previously matched Invoice.
6. WHEN the Matching_Engine records no Match_Finding for an Invoice, THE Matching_Engine SHALL set the Invoice match status to MATCHED.
7. WHEN the Matching_Engine records one or more Match_Findings for an Invoice, THE Matching_Engine SHALL set the Invoice match status to the type of the highest-precedence Match_Finding using the precedence order DUPLICATE_INVOICE, MISSING_DELIVERY, QUANTITY_MISMATCH, PRICE_MISMATCH.
8. WHEN the Matching_Engine records one or more Match_Findings for an Invoice, THE Notification_Service SHALL create one in-app notification for each user holding the FINANCE role in the Organization and for each user holding the PROCUREMENT_MANAGER role in the Organization.
9. WHEN a user holding the FINANCE, PROCUREMENT_MANAGER or ADMIN role requests the match result of an Invoice, THE Matching_Engine SHALL return the Invoice match status, every recorded Match_Finding with its type, compared values and resolution state, and the per-item ordered, received and invoiced quantities.
10. WHEN a Delivery is recorded against a Purchase_Order that holds at least one Invoice whose status is SUBMITTED or UNDER_REVIEW, THE Matching_Engine SHALL re-evaluate the match result of each such Invoice.
11. THE Matching_Engine SHALL execute invoice submission and match evaluation within one database transaction.

### Requirement 24: Invoice Review, Match Override and Overdue Flagging

**User Story:** As a finance user, I want approval blocked while a match exception is unresolved unless I override it with justification, so that exceptions are handled consciously.

#### Acceptance Criteria

1. THE Invoice_Service SHALL permit the invoice status transitions SUBMITTED→UNDER_REVIEW, UNDER_REVIEW→APPROVED, UNDER_REVIEW→REJECTED, APPROVED→PARTIALLY_PAID, APPROVED→PAID, PARTIALLY_PAID→PAID, SUBMITTED→OVERDUE, UNDER_REVIEW→OVERDUE, APPROVED→OVERDUE, PARTIALLY_PAID→OVERDUE, OVERDUE→PARTIALLY_PAID and OVERDUE→PAID.
2. IF an invoice status change specifies a source and target status pair outside the permitted transitions listed in acceptance criterion 24.1, THEN THE Invoice_Service SHALL reject the request with HTTP status 409 and a message naming the source status and the target status.
3. IF a request sets an Invoice status to APPROVED, PARTIALLY_PAID or PAID while that Invoice holds at least one Match_Finding whose resolution state is UNRESOLVED, THEN THE Invoice_Service SHALL reject the request with HTTP status 409 and a message listing the unresolved Match_Finding types.
4. WHEN a user holding the FINANCE or ADMIN role overrides a Match_Finding with a justification, THE Matching_Engine SHALL set the resolution state of that Match_Finding to OVERRIDDEN and SHALL record the Actor, the override instant and the justification.
5. IF an override request omits a justification, THEN THE Matching_Engine SHALL reject the request with HTTP status 400 and the message `Override justification is required`.
6. WHEN every Match_Finding of an Invoice holds resolution state OVERRIDDEN, THE Invoice_Service SHALL permit the transition from UNDER_REVIEW to APPROVED.
7. IF a user holding the FINANCE or ADMIN role rejects an Invoice without supplying a reason, THEN THE Invoice_Service SHALL reject the request with HTTP status 400 and the message `Rejection reason is required`.
8. WHEN a user holding the FINANCE or ADMIN role approves or rejects an Invoice, THE Invoice_Service SHALL record the Actor as reviewer, the decision instant as review timestamp and the supplied comments, and SHALL create one in-app notification for each Vendor_User linked to the Invoice Vendor.
9. WHEN the scheduled overdue invoice evaluation runs and an Invoice holds a due date earlier than the evaluation date, a paid amount less than the total amount and a status other than PAID, REJECTED or OVERDUE, THE Invoice_Service SHALL set that Invoice status to OVERDUE.
10. THE Invoice_Service SHALL run the overdue invoice evaluation once per calendar day at 01:15 UTC.
11. WHEN a user holding only the VENDOR role requests approval or rejection of an Invoice, THE Authorization_Layer SHALL respond with HTTP status 403.

### Requirement 25: Payment Recording and Outstanding Payables

**User Story:** As a finance user, I want to record payments against invoices and see total outstanding liabilities, so that I know what the organization still owes.

#### Acceptance Criteria

1. WHEN a user holding the FINANCE or ADMIN role records a Payment against an Invoice whose status is APPROVED, PARTIALLY_PAID or OVERDUE, THE Payment_Service SHALL create a Payment carrying the supplied amount, payment date, payment reference, payment method and notes, with status PAID and the Actor as recording user.
2. THE Payment_Service SHALL permit two or more Payment records against the same Invoice.
3. IF a Payment amount is less than or equal to zero, THEN THE Payment_Service SHALL reject the request with HTTP status 400 and the message `Payment amount must be greater than zero`.
4. IF a Payment raises the cumulative paid amount of an Invoice above the Invoice total amount, THEN THE Payment_Service SHALL reject the request with HTTP status 409 and a message naming the invoice total amount and the cumulative paid amount.
5. WHEN a Payment is recorded, THE Payment_Service SHALL set the Invoice paid amount to the sum of the amounts of all Payments of that Invoice whose status is PAID, at Money_Scale.
6. WHEN the Invoice paid amount is greater than zero and less than the Invoice total amount, THE Invoice_Service SHALL set the Invoice status to PARTIALLY_PAID.
7. WHEN the Invoice paid amount equals the Invoice total amount, THE Invoice_Service SHALL set the Invoice status to PAID.
8. IF a request records a Payment against an Invoice whose status is SUBMITTED, UNDER_REVIEW, REJECTED or PAID, THEN THE Payment_Service SHALL reject the request with HTTP status 409 and a message naming the current invoice status.
9. WHEN a Payment is recorded, THE Notification_Service SHALL create one in-app notification for each Vendor_User linked to the Invoice Vendor.
10. WHEN a user holding the FINANCE, PROCUREMENT_MANAGER or ADMIN role requests outstanding payables, THE Payment_Service SHALL return the sum of total amount minus paid amount across all invoices of the Organization whose status is APPROVED, PARTIALLY_PAID or OVERDUE, at Money_Scale, together with the same sum grouped by Vendor.
11. THE Payment_Service SHALL execute payment recording and invoice status derivation within one database transaction.

### Requirement 26: Vendor Performance Scoring

**User Story:** As a procurement manager, I want each vendor scored on delivery, quality, pricing, responsiveness and fulfilment, so that past behaviour informs future awards.

#### Acceptance Criteria

1. WHEN the Performance_Engine calculates the delivery metric of a Vendor, THE Performance_Engine SHALL compute the count of deliveries of that Vendor whose delivery date is on or before the expected delivery date of the Purchase_Order, divided by the count of all deliveries of that Vendor, multiplied by 100, at Money_Scale.
2. WHEN the Performance_Engine calculates the quality metric of a Vendor, THE Performance_Engine SHALL compute 100 minus the sum of damaged and rejected quantities across all delivery items of that Vendor divided by the sum of received quantities across all delivery items of that Vendor, multiplied by 100, at Money_Scale.
3. WHEN the Performance_Engine calculates the pricing metric of a Vendor, THE Performance_Engine SHALL compute for each Quotation of that Vendor the mean total amount of all quotations for the same RFQ divided by that Quotation total amount, average those ratios across the vendor quotations, multiply by 100, cap the result at 100.00 and round at Money_Scale.
4. WHEN the Performance_Engine calculates the responsiveness metric of a Vendor, THE Performance_Engine SHALL compute the count of quotations submitted by that Vendor before the closing date of the corresponding RFQ, divided by the count of RFQ vendor invitations issued to that Vendor, multiplied by 100, at Money_Scale.
5. WHEN the Performance_Engine calculates the fulfilment metric of a Vendor, THE Performance_Engine SHALL compute the count of purchase orders of that Vendor whose status is DELIVERED or CLOSED, divided by the count of purchase orders of that Vendor whose status is other than DRAFT and CANCELLED, multiplied by 100, at Money_Scale.
6. WHERE the denominator of a metric named in acceptance criteria 26.1 through 26.5 equals zero, THE Performance_Engine SHALL set that metric to 50.00.
7. WHEN the Performance_Engine calculates the Performance_Score of a Vendor, THE Performance_Engine SHALL compute the arithmetic mean of the delivery, quality, pricing, responsiveness and fulfilment metrics, at Money_Scale.
8. THE Performance_Engine SHALL constrain every metric and the Performance_Score to the range 0.00 to 100.00 inclusive.
9. WHEN a Delivery is recorded, a Quotation is submitted, a Quotation is selected, a Purchase_Order transitions to DELIVERED, or a Purchase_Order transitions to CLOSED, THE Performance_Engine SHALL recalculate the metrics and Performance_Score of the affected Vendor and SHALL persist them as a vendor performance snapshot for the current calendar month.
10. WHEN the Performance_Engine persists a vendor performance snapshot for a Vendor and calendar month that already holds a snapshot, THE Performance_Engine SHALL replace the metric values and recalculation timestamp of the existing snapshot.
11. WHEN the Performance_Engine recalculates the Performance_Score of a Vendor, THE Vendor_Service SHALL set the Vendor rating to the Performance_Score divided by 20, at a scale of 2 with rounding mode HALF_UP.

### Requirement 27: Dashboard Figures and Analytics Reports

**User Story:** As a procurement manager, I want dashboard figures and spend analytics, so that I can monitor procurement health without exporting data.

#### Acceptance Criteria

1. WHEN a user holding the ADMIN, PROCUREMENT_MANAGER, PROCUREMENT_OFFICER or FINANCE role requests the dashboard summary, THE Analytics_Service SHALL return the total spend, the count of active RFQs, the count of open purchase orders, the count of pending deliveries, the count of outstanding invoices, the count of overdue invoices and the count of active vendors for the Actor's Organization.
2. THE Analytics_Service SHALL compute total spend as the sum of paid amounts across all invoices of the Organization, at Money_Scale.
3. THE Analytics_Service SHALL compute the count of active RFQs as the count of RFQs of the Organization whose status is OPEN or EVALUATION.
4. THE Analytics_Service SHALL compute the count of open purchase orders as the count of purchase orders of the Organization whose status is ISSUED, ACKNOWLEDGED or PARTIALLY_DELIVERED.
5. THE Analytics_Service SHALL compute the count of pending deliveries as the count of purchase orders of the Organization whose status is ISSUED, ACKNOWLEDGED or PARTIALLY_DELIVERED and whose cumulative delivered quantity is less than the cumulative ordered quantity.
6. THE Analytics_Service SHALL compute the count of outstanding invoices as the count of invoices of the Organization whose status is APPROVED, PARTIALLY_PAID or OVERDUE.
7. WHEN a user holding the ADMIN, PROCUREMENT_MANAGER or FINANCE role requests the monthly spend report for a date range, THE Analytics_Service SHALL return one entry per calendar month in the range carrying the month and the sum of invoice paid amounts whose payment dates fall in that month, at Money_Scale.
8. WHEN a user holding the ADMIN, PROCUREMENT_MANAGER or FINANCE role requests the spend by department report, THE Analytics_Service SHALL return one entry per Department carrying the department name and the sum of purchase order total amounts whose source purchase request belongs to that Department, at Money_Scale.
9. WHEN a user holding the ADMIN, PROCUREMENT_MANAGER or FINANCE role requests the spend by vendor report, THE Analytics_Service SHALL return one entry per Vendor carrying the vendor company name and the sum of purchase order total amounts of that Vendor, at Money_Scale, ordered by that sum descending.
10. WHEN a user holding the ADMIN, PROCUREMENT_MANAGER or PROCUREMENT_OFFICER role requests the purchase category distribution report, THE Analytics_Service SHALL return one entry per vendor category carrying the category name, the count of purchase orders whose Vendor belongs to that category and the sum of those purchase order total amounts.
11. WHEN a user holding the ADMIN, PROCUREMENT_MANAGER or PROCUREMENT_OFFICER role requests the vendor performance report, THE Analytics_Service SHALL return one entry per Vendor carrying the vendor company name, the delivery metric, the responsiveness metric and the Performance_Score.
12. WHEN a user holding the ADMIN, PROCUREMENT_MANAGER or PROCUREMENT_OFFICER role requests the average procurement cycle time, THE Analytics_Service SHALL return the arithmetic mean Procurement_Cycle_Time across all purchase orders of the Organization whose status is CLOSED, rounded to one decimal place.
13. WHERE the Organization holds no Purchase_Order with status CLOSED, THE Analytics_Service SHALL return zero for the average procurement cycle time.

### Requirement 28: In-App Notifications

**User Story:** As a platform user, I want an in-app notification list with read state, so that I notice events relevant to my role.

#### Acceptance Criteria

1. WHEN the Notification_Service creates a notification, THE Notification_Service SHALL persist the recipient user identifier, a title, a message, the related entity type, the related entity identifier, a read flag set to false and the creation instant.
2. THE Notification_Service SHALL create notifications for the events purchase request submitted, purchase request approved, purchase request rejected, vendor invited to RFQ, RFQ closing within 24 hours, quotation submitted, vendor selected, purchase order issued, delivery recorded, invoice submitted, invoice approved, invoice rejected, payment recorded, invoice match exception raised, overdue delivery detected and vendor document expiring.
3. WHEN a user requests the notification list, THE Notification_Service SHALL return a PageResponse of notifications addressed to that user ordered by creation instant descending.
4. WHERE an unread filter is supplied, THE Notification_Service SHALL return only notifications whose read flag is false.
5. WHEN a user marks a notification as read, THE Notification_Service SHALL set the read flag of that notification to true.
6. IF a user marks a notification addressed to another user as read, THEN THE Notification_Service SHALL respond with HTTP status 404 and the message `Notification not found`.
7. WHEN a user marks all notifications as read, THE Notification_Service SHALL set the read flag to true on every notification addressed to that user.
8. WHEN a user requests the unread notification count, THE Notification_Service SHALL return the count of notifications addressed to that user whose read flag is false.
9. WHEN the Notification_Service creates a notification a second time for the same recipient, entity type, entity identifier and event type, THE Notification_Service SHALL leave the existing notification unchanged and SHALL create no additional notification.
10. THE Notification_Service SHALL deliver notifications through the VendorSphere_API only and SHALL send no email for the MVP scope.

### Requirement 29: Audit Logging

**User Story:** As an administrator, I want an append-only audit trail of critical business changes, so that decisions and state changes remain traceable.

#### Acceptance Criteria

1. WHEN a state-changing operation listed in acceptance criterion 29.2 completes, THE Audit_Service SHALL persist an audit log entry carrying the Organization identifier, the Actor identifier, an action name, the entity type, the entity identifier, the previous value as JSON, the new value as JSON, the request IP address, the request user agent and the creation instant.
2. THE Audit_Service SHALL record audit log entries for vendor creation, vendor update, vendor status change, purchase request submission, purchase request approval, purchase request rejection, RFQ creation, RFQ status change, RFQ cancellation, vendor invitation, quotation submission, quotation revision, vendor selection, purchase order generation, purchase order issuance, purchase order cancellation, delivery recording, invoice submission, invoice approval, invoice rejection, match finding override and payment recording.
3. WHEN a user holding the ADMIN role requests the audit log, THE Audit_Service SHALL return a PageResponse of audit log entries of the Actor's Organization ordered by creation instant descending.
4. WHERE an actor identifier filter is supplied, THE Audit_Service SHALL return only audit log entries whose Actor identifier equals the supplied value.
5. WHERE an entity type filter or an entity identifier filter is supplied, THE Audit_Service SHALL return only audit log entries matching every supplied value.
6. WHERE a date range filter is supplied, THE Audit_Service SHALL return only audit log entries whose creation instant falls within the supplied range inclusive.
7. WHEN a user holding a role other than ADMIN requests the audit log, THE Authorization_Layer SHALL respond with HTTP status 403.
8. THE Audit_Service SHALL expose create and read operations for audit log entries and SHALL expose no update or delete operation.
9. WHEN a request uses the HTTP method PUT, PATCH or DELETE against an audit log endpoint, THE VendorSphere_API SHALL respond with HTTP status 405.
10. WHEN a state-changing operation completes and the Audit_Service fails to persist an audit log entry, THE VendorSphere_API SHALL roll back the state-changing operation and respond with HTTP status 500.

### Requirement 30: Role-Based Authorization

**User Story:** As an administrator, I want every procurement endpoint guarded by role, so that users act only within their mandate.

#### Acceptance Criteria

1. THE Authorization_Layer SHALL require a valid JWT access token on every endpoint under `/api/v1` except the paths already listed as public in `SecurityConfig`.
2. IF a request carries no JWT access token or an expired JWT access token, THEN THE VendorSphere_API SHALL respond with HTTP status 401 and an ApiResponse whose success field is false.
3. THE Authorization_Layer SHALL grant users holding the ADMIN role access to every procurement endpoint of the Actor's Organization and to the audit log endpoints.
4. THE Authorization_Layer SHALL grant users holding the PROCUREMENT_OFFICER role access to vendor registration, vendor update, vendor contact management, vendor category management, vendor document management, purchase request creation, RFQ creation, RFQ item management, vendor invitation, quotation comparison, purchase order generation, purchase order issuance and delivery recording.
5. THE Authorization_Layer SHALL grant users holding the PROCUREMENT_MANAGER role access to purchase request approval and rejection, vendor status change, quotation comparison, criteria weight configuration, vendor selection, vendor performance reports and analytics reports.
6. THE Authorization_Layer SHALL grant users holding the REQUESTER role access to purchase request creation, purchase request item management for purchase requests the Actor created, purchase request attachment upload and purchase request status retrieval for purchase requests the Actor created.
7. THE Authorization_Layer SHALL grant users holding the FINANCE role access to invoice retrieval, invoice review, match result retrieval, match finding override, payment recording and outstanding payables reporting.
8. THE Authorization_Layer SHALL grant users holding the VENDOR role access to the linked vendor profile, linked vendor documents, RFQ invitations of the linked Vendor, quotation submission and revision for the linked Vendor, purchase orders of the linked Vendor, purchase order acknowledgement for the linked Vendor, delivery history of the linked Vendor and invoice submission for the linked Vendor.
9. IF an authenticated user requests an endpoint outside the grants listed in acceptance criteria 30.3 through 30.8, THEN THE VendorSphere_API SHALL respond with HTTP status 403 and an ApiResponse whose message is `Access denied`.
10. WHEN any service loads a business record, THE VendorSphere_API SHALL restrict the query to the Organization identifier returned by `SecurityUtils.getCurrentOrganizationId()`.
11. THE Authorization_Layer SHALL enforce every grant listed in acceptance criteria 30.3 through 30.8 on the server side.

### Requirement 31: List API Pagination, Sorting and Filtering

**User Story:** As a frontend developer, I want consistent paginated list responses, so that screens behave predictably as data grows.

#### Acceptance Criteria

1. WHEN a request lists vendors, purchase requests, RFQs, quotations, purchase orders, deliveries, invoices, payments, notifications or audit log entries, THE VendorSphere_API SHALL return an ApiResponse whose data field holds a PageResponse.
2. THE VendorSphere_API SHALL accept the query parameters `page`, `size`, `sort` and `direction` on every list endpoint named in acceptance criterion 31.1.
3. WHERE the `page` parameter is absent, THE VendorSphere_API SHALL apply page 0, and WHERE the `size` parameter is absent, THE VendorSphere_API SHALL apply size 20.
4. IF a request supplies a `size` parameter greater than 100, THEN THE VendorSphere_API SHALL apply size 100.
5. IF a request supplies a `sort` parameter naming a field outside the sortable fields of the endpoint, THEN THE VendorSphere_API SHALL reject the request with HTTP status 400 and a message listing the sortable fields.
6. THE VendorSphere_API SHALL create database indexes on the columns used by the status, vendor, department, organization and timestamp filters of every list endpoint named in acceptance criterion 31.1.

### Requirement 32: Transactional Integrity, Concurrency and Numeric Precision

**User Story:** As an engineer, I want award, purchase order generation, delivery posting, match evaluation and payment posting to be atomic and concurrency-safe, so that the ledger stays consistent.

#### Acceptance Criteria

1. THE VendorSphere_API SHALL execute vendor selection, purchase order generation, delivery recording, invoice submission with match evaluation, and payment recording each within a single database transaction annotated `@Transactional`.
2. IF an operation named in acceptance criterion 32.1 raises an exception, THEN THE VendorSphere_API SHALL roll back every change made by that operation.
3. THE VendorSphere_API SHALL carry a JPA `@Version` optimistic lock attribute on the Vendor, Purchase_Request, RFQ, Quotation, Purchase_Order and Invoice entities.
4. IF two concurrent requests update the same Vendor, Purchase_Request, RFQ, Quotation, Purchase_Order or Invoice record, THEN THE VendorSphere_API SHALL apply the first commit and SHALL respond to the second commit with HTTP status 409 and the message `Record was modified by another user, reload and retry`.
5. THE VendorSphere_API SHALL represent every monetary field as `java.math.BigDecimal` and SHALL round every computed monetary value at Money_Scale.
6. THE VendorSphere_API SHALL represent every quantity field as `java.math.BigDecimal` and SHALL round every computed quantity value at Quantity_Scale.
7. WHEN the VendorSphere_API serializes a monetary field, THE VendorSphere_API SHALL emit exactly two decimal places.

### Requirement 33: File Attachment Handling

**User Story:** As a platform user, I want to attach supporting files to procurement records, so that specifications, certificates and proofs travel with the transaction.

#### Acceptance Criteria

1. WHEN a user uploads a file for a vendor document, purchase request attachment, RFQ document, quotation document, delivery proof or invoice document, THE Attachment_Service SHALL store the file, persist the original file name, the content type, the byte size, the storage reference and the uploading user, and return the stored metadata.
2. THE Attachment_Service SHALL accept the content types `application/pdf`, `image/png`, `image/jpeg`, `application/vnd.openxmlformats-officedocument.wordprocessingml.document` and `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`.
3. IF an upload supplies a content type outside the list in acceptance criterion 33.2, THEN THE Attachment_Service SHALL reject the request with HTTP status 415 and a message listing the accepted content types.
4. IF an upload supplies a file whose byte size is greater than 10485760, THEN THE Attachment_Service SHALL reject the request with HTTP status 413 and the message `File exceeds the 10 MB limit`.
5. WHEN the Attachment_Service stores a file, THE Attachment_Service SHALL generate a storage reference that excludes the original file name and includes a randomly generated identifier.
6. WHEN a user requests a stored file, THE Attachment_Service SHALL return the file only when the Actor holds access to the owning record under Requirement 30.
7. THE Attachment_Service SHALL store files on the local filesystem under a configured base directory for the MVP scope.

### Requirement 34: Schema Migrations and API Documentation

**User Story:** As an engineer, I want schema changes versioned and endpoints documented, so that the project stays deployable and discoverable.

#### Acceptance Criteria

1. THE VendorSphere_API SHALL apply every schema change through a Flyway migration file under `backend/src/main/resources/db/migration` named `V{n}__{description}.sql` where `{n}` is greater than 1.
2. THE VendorSphere_API SHALL leave the contents of `V1__init_schema.sql` unchanged.
3. THE VendorSphere_API SHALL add through migration the tables for evaluation criteria weights, invoice match findings, purchase request attachments, RFQ documents and quotation documents, and the columns for optimistic lock versions, purchase order acknowledgement instant, purchase order delivery overdue flag, RFQ cancellation reason, vendor status change reason, quotation warranty duration in months and invoice review comments.
4. WHEN the VendorSphere_API starts, THE VendorSphere_API SHALL apply pending Flyway migrations before serving requests.
5. THE VendorSphere_API SHALL expose every new endpoint in the OpenAPI document served at `/v3/api-docs` with an operation summary, request schema and response schema.
6. THE VendorSphere_API SHALL return every response body wrapped in an ApiResponse.
7. THE VendorSphere_API SHALL expose DTO records at every controller boundary and SHALL expose no JPA entity type in a request or response body.
8. WHEN a service raises a domain rule violation, THE VendorSphere_API SHALL raise a `BusinessException` carrying the intended HTTP status and SHALL let `GlobalExceptionHandler` map the exception to the response.

### Requirement 35: Frontend Procurement Screens

**User Story:** As a platform user, I want screens for each stage of the lifecycle, so that I can run procurement without calling the API directly.

#### Acceptance Criteria

1. THE VendorSphere_Web SHALL provide a dashboard screen presenting the figures listed in acceptance criterion 27.1 and the monthly spend, spend by department and spend by vendor reports.
2. THE VendorSphere_Web SHALL provide a vendor list screen with company name search, category filter, status filter, rating filter, pagination and a link to each vendor detail screen.
3. THE VendorSphere_Web SHALL provide a vendor detail screen presenting the vendor profile, contacts, documents with expiry state, Performance_Score and status change control.
4. THE VendorSphere_Web SHALL provide a purchase request list screen and a purchase request detail screen presenting items, attachments, status, review notes and derived RFQ links.
5. THE VendorSphere_Web SHALL provide an RFQ list screen and an RFQ detail screen presenting items, invited vendors, documents, opening date, closing date and status.
6. THE VendorSphere_Web SHALL provide a quotation comparison screen presenting one column per Quotation with total amount, delivery period, warranty duration, Performance_Score, the four component scores, the Evaluation_Score and the recommended marker.
7. THE VendorSphere_Web SHALL present a confirmation step carrying a justification input before submitting a vendor selection.
8. THE VendorSphere_Web SHALL provide a purchase order list screen and a purchase order detail screen presenting items, delivery progress per item, status and delivery history.
9. THE VendorSphere_Web SHALL provide an invoice list screen and an invoice detail screen presenting invoice items, the match status, every Match_Finding, the paid amount and the payment history.
10. THE VendorSphere_Web SHALL provide a vendor portal area presenting RFQ invitations of the linked Vendor, a quotation submission form, awarded purchase orders, a purchase order acknowledgement control and an invoice submission form.
11. THE VendorSphere_Web SHALL present a notification indicator carrying the unread notification count and a notification list with a mark-as-read control.
12. WHEN the VendorSphere_Web renders a screen for a user whose roles exclude the roles granted access to that screen under Requirement 30, THE VendorSphere_Web SHALL present an access denied message in place of the screen content.
13. THE VendorSphere_Web SHALL retrieve every backend figure through the existing `apiClient` in `src/lib/api.ts` and SHALL manage server state through TanStack Query.
14. THE VendorSphere_Web SHALL render every interactive control with an accessible name and SHALL associate every form input with a visible label.

### Requirement 36: Automated Verification

**User Story:** As an engineer, I want the procurement lifecycle covered by automated tests that run in continuous integration, so that regressions surface before merge.

#### Acceptance Criteria

1. THE VendorSphere_API SHALL hold JUnit 5 unit tests covering the vendor status transition rules of Requirement 3, the purchase request transition rules of Requirement 8, the RFQ transition rules of Requirement 11, the purchase order transition rules of Requirement 19 and the invoice transition rules of Requirement 24.
2. THE VendorSphere_API SHALL hold JUnit 5 unit tests covering the quotation total computation of Requirement 13, the evaluation scoring of Requirement 16, the delivery progress derivation of Requirement 21, the three-way matching outcomes of Requirement 23, the payment aggregation of Requirement 25 and the performance metric computation of Requirement 26.
3. THE VendorSphere_API SHALL isolate collaborating components in unit tests using Mockito test doubles.
4. THE VendorSphere_API SHALL hold Spring Boot integration tests that execute against a PostgreSQL 16 container provisioned by Testcontainers and that apply the Flyway migrations before each test class.
5. THE VendorSphere_API SHALL hold one integration test that executes the end-to-end scenario purchase request creation, approval, RFQ creation, vendor invitation, three quotation submissions, comparison, vendor selection, purchase order generation, purchase order issuance, delivery recording, invoice submission with a passing three-way match, payment recording and vendor performance recalculation.
6. THE VendorSphere_API SHALL hold integration tests asserting that a Vendor_User receives HTTP status 404 when requesting a Quotation belonging to another Vendor and HTTP status 403 when requesting a quotation comparison.
7. THE VendorSphere_Web SHALL hold Vitest tests with React Testing Library covering the quotation comparison screen, the vendor selection confirmation step, the purchase order delivery progress presentation and the invoice match finding presentation.
8. WHEN the continuous integration workflow runs on a pull request, THE continuous integration workflow SHALL execute the backend test suite and the frontend test suite and SHALL fail the run when any test fails.
