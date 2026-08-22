-- VendorSphere procurement lifecycle schema
-- Version 2: reference sequences, evaluation weights, match findings, attachments,
--            optimistic locking columns, additional business columns and list indexes.
-- V1__init_schema.sql is left untouched.

-- ============================================================
-- REFERENCE NUMBER SEQUENCES (Requirement 1)
-- ============================================================

CREATE TABLE reference_sequences (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    prefix          VARCHAR(10) NOT NULL,
    year            INT NOT NULL,
    next_value      INT NOT NULL DEFAULT 0,
    UNIQUE (organization_id, prefix, year)
);

-- ============================================================
-- EVALUATION CRITERIA WEIGHTS (Requirement 16)
-- ============================================================

CREATE TABLE evaluation_criteria_weights (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id     UUID NOT NULL UNIQUE REFERENCES organizations(id),
    price_weight        DECIMAL(3, 2) NOT NULL,
    delivery_weight     DECIMAL(3, 2) NOT NULL,
    performance_weight  DECIMAL(3, 2) NOT NULL,
    warranty_weight     DECIMAL(3, 2) NOT NULL,
    updated_by          UUID REFERENCES users(id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================
-- THREE-WAY MATCH FINDINGS (Requirements 23, 24)
-- ============================================================

CREATE TABLE invoice_match_findings (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id              UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    purchase_order_item_id  UUID REFERENCES purchase_order_items(id),
    finding_type            VARCHAR(30) NOT NULL,
    item_name               VARCHAR(255),
    expected_value          VARCHAR(255),
    actual_value            VARCHAR(255),
    detail                  TEXT,
    resolution_state        VARCHAR(20) NOT NULL DEFAULT 'UNRESOLVED',
    overridden_by           UUID REFERENCES users(id),
    overridden_at           TIMESTAMPTZ,
    override_justification  TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_finding_type CHECK (finding_type IN (
        'DUPLICATE_INVOICE', 'MISSING_DELIVERY', 'QUANTITY_MISMATCH', 'PRICE_MISMATCH'
    )),
    CONSTRAINT chk_finding_resolution CHECK (resolution_state IN (
        'UNRESOLVED', 'OVERRIDDEN'
    ))
);

-- ============================================================
-- POLYMORPHIC ATTACHMENTS (Requirement 33)
-- ============================================================

CREATE TABLE attachments (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id   UUID NOT NULL REFERENCES organizations(id),
    owner_type        VARCHAR(40) NOT NULL,
    owner_id          UUID NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type      VARCHAR(150) NOT NULL,
    byte_size         BIGINT NOT NULL,
    storage_reference VARCHAR(255) NOT NULL UNIQUE,
    uploaded_by       UUID NOT NULL REFERENCES users(id),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================
-- OPTIMISTIC LOCKING (Requirement 32.3)
-- ============================================================

ALTER TABLE vendors            ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE purchase_requests  ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE rfqs               ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE quotations         ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE purchase_orders    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE invoices           ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- ============================================================
-- ADDITIONAL BUSINESS COLUMNS
-- ============================================================

ALTER TABLE vendors         ADD COLUMN status_change_reason TEXT;
ALTER TABLE rfqs            ADD COLUMN cancellation_reason TEXT;
ALTER TABLE quotations      ADD COLUMN warranty_months INT;
ALTER TABLE purchase_orders ADD COLUMN acknowledged_at TIMESTAMPTZ,
                            ADD COLUMN delivery_overdue BOOLEAN NOT NULL DEFAULT FALSE,
                            ADD COLUMN cancellation_reason TEXT,
                            ADD COLUMN closed_at TIMESTAMPTZ;
ALTER TABLE invoices        ADD COLUMN review_comments TEXT;
ALTER TABLE notifications   ADD COLUMN event_type VARCHAR(60);

-- ============================================================
-- NOTIFICATION IDEMPOTENCE (Requirement 28.9)
-- ============================================================

CREATE UNIQUE INDEX uq_notifications_event
    ON notifications(user_id, event_type, entity_type, entity_id)
    WHERE event_type IS NOT NULL AND entity_id IS NOT NULL;

-- ============================================================
-- LIST AND FILTER INDEXES (Requirement 31.6)
-- ============================================================

CREATE INDEX idx_vendors_category        ON vendors(category_id);
CREATE INDEX idx_vendors_user            ON vendors(user_id);
CREATE INDEX idx_vendor_documents_expiry ON vendor_documents(expiry_date);
CREATE INDEX idx_pr_department           ON purchase_requests(department_id);
CREATE INDEX idx_pr_created              ON purchase_requests(created_at);
CREATE INDEX idx_rfqs_pr                 ON rfqs(purchase_request_id);
CREATE INDEX idx_rfq_vendors_vendor      ON rfq_vendors(vendor_id, status);
CREATE INDEX idx_quotations_status       ON quotations(status);
CREATE INDEX idx_po_expected_delivery    ON purchase_orders(expected_delivery);
CREATE INDEX idx_po_rfq                  ON purchase_orders(rfq_id);
CREATE INDEX idx_deliveries_po           ON deliveries(purchase_order_id, delivery_date);
CREATE INDEX idx_delivery_items_po_item  ON delivery_items(purchase_order_item_id);
CREATE INDEX idx_invoices_vendor         ON invoices(vendor_id);
CREATE INDEX idx_invoices_due            ON invoices(due_date);
CREATE INDEX idx_invoice_items_po_item   ON invoice_items(purchase_order_item_id);
CREATE INDEX idx_payments_invoice        ON payments(invoice_id, status);
CREATE INDEX idx_attachments_owner       ON attachments(owner_type, owner_id);
CREATE INDEX idx_perf_snapshots_vendor   ON vendor_performance_snapshots(vendor_id, period_start);
CREATE INDEX idx_audit_logs_actor        ON audit_logs(actor_id);
