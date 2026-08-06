-- VendorSphere core schema
-- Version 1: Foundation entities for procurement lifecycle

-- ============================================================
-- ORGANIZATION & USERS
-- ============================================================

CREATE TABLE organizations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    slug            VARCHAR(100) NOT NULL UNIQUE,
    address         TEXT,
    tax_identifier  VARCHAR(100),
    currency        VARCHAR(3) NOT NULL DEFAULT 'INR',
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE roles (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE departments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    name            VARCHAR(255) NOT NULL,
    code            VARCHAR(50),
    manager_id      UUID,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (organization_id, name)
);

CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    department_id   UUID REFERENCES departments(id),
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    phone           VARCHAR(30),
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    email_verified  BOOLEAN NOT NULL DEFAULT FALSE,
    last_login_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE departments
    ADD CONSTRAINT fk_departments_manager
    FOREIGN KEY (manager_id) REFERENCES users(id);

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(255) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================
-- VENDOR MANAGEMENT
-- ============================================================

CREATE TABLE vendor_categories (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (organization_id, name)
);

CREATE TABLE vendors (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    category_id     UUID REFERENCES vendor_categories(id),
    vendor_code     VARCHAR(50) NOT NULL,
    company_name    VARCHAR(255) NOT NULL,
    contact_person  VARCHAR(255),
    email           VARCHAR(255) NOT NULL,
    phone           VARCHAR(30),
    address         TEXT,
    tax_identifier  VARCHAR(100),
    status          VARCHAR(30) NOT NULL DEFAULT 'PROSPECTIVE',
    rating          DECIMAL(3, 2) DEFAULT 0.00,
    user_id         UUID REFERENCES users(id),
    registered_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (organization_id, vendor_code),
    CONSTRAINT chk_vendor_status CHECK (status IN (
        'PROSPECTIVE', 'ACTIVE', 'SUSPENDED', 'BLACKLISTED', 'INACTIVE'
    ))
);

CREATE TABLE vendor_contacts (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vendor_id   UUID NOT NULL REFERENCES vendors(id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL,
    email       VARCHAR(255),
    phone       VARCHAR(30),
    designation VARCHAR(100),
    primary_contact BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE vendor_documents (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vendor_id       UUID NOT NULL REFERENCES vendors(id) ON DELETE CASCADE,
    document_type   VARCHAR(100) NOT NULL,
    file_name       VARCHAR(255) NOT NULL,
    file_url        TEXT NOT NULL,
    expiry_date     DATE,
    uploaded_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================
-- PURCHASE REQUESTS
-- ============================================================

CREATE TABLE purchase_requests (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id     UUID NOT NULL REFERENCES organizations(id),
    request_number      VARCHAR(50) NOT NULL,
    requester_id        UUID NOT NULL REFERENCES users(id),
    department_id       UUID NOT NULL REFERENCES departments(id),
    title               VARCHAR(255) NOT NULL,
    business_justification TEXT,
    required_date       DATE,
    priority            VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    estimated_budget    DECIMAL(15, 2),
    status              VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    reviewed_by         UUID REFERENCES users(id),
    reviewed_at         TIMESTAMPTZ,
    review_notes        TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (organization_id, request_number),
    CONSTRAINT chk_pr_status CHECK (status IN (
        'DRAFT', 'SUBMITTED', 'UNDER_REVIEW', 'APPROVED', 'REJECTED',
        'PROCUREMENT_STARTED', 'COMPLETED'
    )),
    CONSTRAINT chk_pr_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT'))
);

CREATE TABLE purchase_request_items (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    purchase_request_id UUID NOT NULL REFERENCES purchase_requests(id) ON DELETE CASCADE,
    item_name           VARCHAR(255) NOT NULL,
    quantity            DECIMAL(12, 3) NOT NULL,
    unit                VARCHAR(50) NOT NULL DEFAULT 'UNIT',
    specification       TEXT,
    estimated_unit_price DECIMAL(15, 2),
    sort_order          INT NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================
-- RFQ
-- ============================================================

CREATE TABLE rfqs (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id     UUID NOT NULL REFERENCES organizations(id),
    purchase_request_id UUID REFERENCES purchase_requests(id),
    rfq_number          VARCHAR(50) NOT NULL,
    title               VARCHAR(255) NOT NULL,
    description         TEXT,
    opening_date        TIMESTAMPTZ NOT NULL,
    closing_date        TIMESTAMPTZ NOT NULL,
    currency            VARCHAR(3) NOT NULL DEFAULT 'INR',
    delivery_location   TEXT,
    terms               TEXT,
    status              VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    created_by          UUID NOT NULL REFERENCES users(id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (organization_id, rfq_number),
    CONSTRAINT chk_rfq_status CHECK (status IN (
        'DRAFT', 'OPEN', 'CLOSED', 'EVALUATION', 'AWARDED', 'CANCELLED'
    ))
);

CREATE TABLE rfq_items (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rfq_id              UUID NOT NULL REFERENCES rfqs(id) ON DELETE CASCADE,
    purchase_request_item_id UUID REFERENCES purchase_request_items(id),
    item_name           VARCHAR(255) NOT NULL,
    quantity            DECIMAL(12, 3) NOT NULL,
    unit                VARCHAR(50) NOT NULL DEFAULT 'UNIT',
    specification       TEXT,
    sort_order          INT NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE rfq_vendors (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rfq_id      UUID NOT NULL REFERENCES rfqs(id) ON DELETE CASCADE,
    vendor_id   UUID NOT NULL REFERENCES vendors(id),
    invited_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    invited_by  UUID NOT NULL REFERENCES users(id),
    status      VARCHAR(30) NOT NULL DEFAULT 'INVITED',
    UNIQUE (rfq_id, vendor_id),
    CONSTRAINT chk_rfq_vendor_status CHECK (status IN (
        'INVITED', 'VIEWED', 'RESPONDED', 'DECLINED', 'AWARDED'
    ))
);

-- ============================================================
-- QUOTATIONS
-- ============================================================

CREATE TABLE quotations (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rfq_id              UUID NOT NULL REFERENCES rfqs(id),
    vendor_id           UUID NOT NULL REFERENCES vendors(id),
    quotation_number    VARCHAR(50),
    subtotal            DECIMAL(15, 2) NOT NULL DEFAULT 0,
    tax_amount          DECIMAL(15, 2) NOT NULL DEFAULT 0,
    discount_amount     DECIMAL(15, 2) NOT NULL DEFAULT 0,
    shipping_amount     DECIMAL(15, 2) NOT NULL DEFAULT 0,
    total_amount        DECIMAL(15, 2) NOT NULL DEFAULT 0,
    delivery_period_days INT,
    payment_terms       TEXT,
    warranty            TEXT,
    validity_date       DATE,
    notes               TEXT,
    status              VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED',
    submitted_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (rfq_id, vendor_id),
    CONSTRAINT chk_quotation_status CHECK (status IN (
        'DRAFT', 'SUBMITTED', 'UNDER_REVIEW', 'SELECTED', 'REJECTED', 'WITHDRAWN'
    ))
);

CREATE TABLE quotation_items (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    quotation_id    UUID NOT NULL REFERENCES quotations(id) ON DELETE CASCADE,
    rfq_item_id     UUID REFERENCES rfq_items(id),
    item_name       VARCHAR(255) NOT NULL,
    quantity        DECIMAL(12, 3) NOT NULL,
    unit_price      DECIMAL(15, 2) NOT NULL,
    tax_rate        DECIMAL(5, 2) NOT NULL DEFAULT 0,
    tax_amount      DECIMAL(15, 2) NOT NULL DEFAULT 0,
    discount_amount DECIMAL(15, 2) NOT NULL DEFAULT 0,
    line_total      DECIMAL(15, 2) NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE vendor_evaluations (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rfq_id              UUID NOT NULL REFERENCES rfqs(id),
    quotation_id        UUID NOT NULL REFERENCES quotations(id),
    vendor_id           UUID NOT NULL REFERENCES vendors(id),
    price_score         DECIMAL(5, 2),
    delivery_score      DECIMAL(5, 2),
    warranty_score      DECIMAL(5, 2),
    performance_score   DECIMAL(5, 2),
    total_score         DECIMAL(5, 2),
    recommended         BOOLEAN NOT NULL DEFAULT FALSE,
    evaluated_by        UUID REFERENCES users(id),
    evaluated_at        TIMESTAMPTZ,
    comments            TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (rfq_id, quotation_id)
);

CREATE TABLE vendor_selections (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rfq_id              UUID NOT NULL REFERENCES rfqs(id) UNIQUE,
    quotation_id        UUID NOT NULL REFERENCES quotations(id),
    vendor_id           UUID NOT NULL REFERENCES vendors(id),
    selected_by         UUID NOT NULL REFERENCES users(id),
    selection_justification TEXT,
    selected_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================
-- PURCHASE ORDERS
-- ============================================================

CREATE TABLE purchase_orders (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id     UUID NOT NULL REFERENCES organizations(id),
    rfq_id              UUID REFERENCES rfqs(id),
    quotation_id        UUID REFERENCES quotations(id),
    vendor_id           UUID NOT NULL REFERENCES vendors(id),
    po_number           VARCHAR(50) NOT NULL,
    delivery_address    TEXT,
    expected_delivery   DATE,
    payment_terms       TEXT,
    terms_conditions    TEXT,
    subtotal            DECIMAL(15, 2) NOT NULL DEFAULT 0,
    tax_amount          DECIMAL(15, 2) NOT NULL DEFAULT 0,
    total_amount        DECIMAL(15, 2) NOT NULL DEFAULT 0,
    status              VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    issued_at           TIMESTAMPTZ,
    issued_by           UUID REFERENCES users(id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (organization_id, po_number),
    CONSTRAINT chk_po_status CHECK (status IN (
        'DRAFT', 'ISSUED', 'ACKNOWLEDGED', 'PARTIALLY_DELIVERED',
        'DELIVERED', 'CLOSED', 'CANCELLED'
    ))
);

CREATE TABLE purchase_order_items (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    purchase_order_id   UUID NOT NULL REFERENCES purchase_orders(id) ON DELETE CASCADE,
    item_name           VARCHAR(255) NOT NULL,
    quantity            DECIMAL(12, 3) NOT NULL,
    unit_price          DECIMAL(15, 2) NOT NULL,
    tax_rate            DECIMAL(5, 2) NOT NULL DEFAULT 0,
    tax_amount          DECIMAL(15, 2) NOT NULL DEFAULT 0,
    line_total          DECIMAL(15, 2) NOT NULL DEFAULT 0,
    delivered_quantity  DECIMAL(12, 3) NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================
-- DELIVERIES
-- ============================================================

CREATE TABLE deliveries (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    purchase_order_id   UUID NOT NULL REFERENCES purchase_orders(id),
    delivery_number     VARCHAR(50) NOT NULL,
    delivery_date       DATE NOT NULL,
    received_by         UUID REFERENCES users(id),
    notes               TEXT,
    proof_document_url  TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE delivery_items (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    delivery_id             UUID NOT NULL REFERENCES deliveries(id) ON DELETE CASCADE,
    purchase_order_item_id  UUID NOT NULL REFERENCES purchase_order_items(id),
    received_quantity       DECIMAL(12, 3) NOT NULL,
    damaged_quantity        DECIMAL(12, 3) NOT NULL DEFAULT 0,
    rejected_quantity       DECIMAL(12, 3) NOT NULL DEFAULT 0,
    notes                   TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================
-- INVOICES & PAYMENTS
-- ============================================================

CREATE TABLE invoices (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id     UUID NOT NULL REFERENCES organizations(id),
    purchase_order_id   UUID NOT NULL REFERENCES purchase_orders(id),
    vendor_id           UUID NOT NULL REFERENCES vendors(id),
    invoice_number      VARCHAR(100) NOT NULL,
    invoice_date        DATE NOT NULL,
    due_date            DATE,
    subtotal            DECIMAL(15, 2) NOT NULL DEFAULT 0,
    tax_amount          DECIMAL(15, 2) NOT NULL DEFAULT 0,
    discount_amount     DECIMAL(15, 2) NOT NULL DEFAULT 0,
    total_amount        DECIMAL(15, 2) NOT NULL DEFAULT 0,
    paid_amount         DECIMAL(15, 2) NOT NULL DEFAULT 0,
    document_url        TEXT,
    status              VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED',
    match_status        VARCHAR(30),
    match_notes         TEXT,
    reviewed_by         UUID REFERENCES users(id),
    reviewed_at         TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (organization_id, vendor_id, invoice_number),
    CONSTRAINT chk_invoice_status CHECK (status IN (
        'SUBMITTED', 'UNDER_REVIEW', 'APPROVED', 'REJECTED',
        'PARTIALLY_PAID', 'PAID', 'OVERDUE'
    )),
    CONSTRAINT chk_match_status CHECK (match_status IS NULL OR match_status IN (
        'MATCHED', 'QUANTITY_MISMATCH', 'PRICE_MISMATCH',
        'MISSING_DELIVERY', 'DUPLICATE_INVOICE', 'PENDING'
    ))
);

CREATE TABLE invoice_items (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id              UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    purchase_order_item_id  UUID REFERENCES purchase_order_items(id),
    item_name               VARCHAR(255) NOT NULL,
    quantity                DECIMAL(12, 3) NOT NULL,
    unit_price              DECIMAL(15, 2) NOT NULL,
    tax_amount              DECIMAL(15, 2) NOT NULL DEFAULT 0,
    line_total              DECIMAL(15, 2) NOT NULL DEFAULT 0,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE payments (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id          UUID NOT NULL REFERENCES invoices(id),
    amount              DECIMAL(15, 2) NOT NULL,
    payment_date        DATE NOT NULL,
    payment_reference   VARCHAR(100),
    payment_method      VARCHAR(50),
    notes               TEXT,
    status              VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    recorded_by         UUID REFERENCES users(id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_payment_status CHECK (status IN (
        'PENDING', 'PARTIALLY_PAID', 'PAID', 'FAILED'
    ))
);

-- ============================================================
-- VENDOR PERFORMANCE
-- ============================================================

CREATE TABLE vendor_performance_snapshots (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vendor_id               UUID NOT NULL REFERENCES vendors(id),
    organization_id         UUID NOT NULL REFERENCES organizations(id),
    period_start            DATE NOT NULL,
    period_end              DATE NOT NULL,
    delivery_score          DECIMAL(5, 2) NOT NULL DEFAULT 0,
    quality_score           DECIMAL(5, 2) NOT NULL DEFAULT 0,
    pricing_score           DECIMAL(5, 2) NOT NULL DEFAULT 0,
    responsiveness_score    DECIMAL(5, 2) NOT NULL DEFAULT 0,
    fulfilment_score        DECIMAL(5, 2) NOT NULL DEFAULT 0,
    overall_score           DECIMAL(5, 2) NOT NULL DEFAULT 0,
    calculated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (vendor_id, period_start, period_end)
);

-- ============================================================
-- NOTIFICATIONS & AUDIT
-- ============================================================

CREATE TABLE notifications (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title           VARCHAR(255) NOT NULL,
    message         TEXT NOT NULL,
    entity_type     VARCHAR(100),
    entity_id       UUID,
    read            BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE audit_logs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID REFERENCES organizations(id),
    actor_id        UUID REFERENCES users(id),
    action          VARCHAR(100) NOT NULL,
    entity_type     VARCHAR(100) NOT NULL,
    entity_id       UUID,
    previous_value  JSONB,
    new_value       JSONB,
    ip_address      VARCHAR(45),
    user_agent      TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================
-- INDEXES
-- ============================================================

CREATE INDEX idx_users_org ON users(organization_id);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_departments_org ON departments(organization_id);
CREATE INDEX idx_vendors_org ON vendors(organization_id);
CREATE INDEX idx_vendors_status ON vendors(status);
CREATE INDEX idx_purchase_requests_org ON purchase_requests(organization_id);
CREATE INDEX idx_purchase_requests_status ON purchase_requests(status);
CREATE INDEX idx_purchase_requests_requester ON purchase_requests(requester_id);
CREATE INDEX idx_rfqs_org ON rfqs(organization_id);
CREATE INDEX idx_rfqs_status ON rfqs(status);
CREATE INDEX idx_rfqs_closing ON rfqs(closing_date);
CREATE INDEX idx_quotations_rfq ON quotations(rfq_id);
CREATE INDEX idx_quotations_vendor ON quotations(vendor_id);
CREATE INDEX idx_purchase_orders_org ON purchase_orders(organization_id);
CREATE INDEX idx_purchase_orders_status ON purchase_orders(status);
CREATE INDEX idx_purchase_orders_vendor ON purchase_orders(vendor_id);
CREATE INDEX idx_invoices_org ON invoices(organization_id);
CREATE INDEX idx_invoices_status ON invoices(status);
CREATE INDEX idx_invoices_po ON invoices(purchase_order_id);
CREATE INDEX idx_notifications_user ON notifications(user_id, read);
CREATE INDEX idx_audit_logs_org ON audit_logs(organization_id);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_created ON audit_logs(created_at);

-- ============================================================
-- SEED DATA: Roles
-- ============================================================

INSERT INTO roles (name, description) VALUES
    ('ADMIN', 'Organization administrator'),
    ('PROCUREMENT_MANAGER', 'Procurement manager with approval authority'),
    ('PROCUREMENT_OFFICER', 'Procurement officer managing RFQs and POs'),
    ('REQUESTER', 'Department requester creating purchase requirements'),
    ('FINANCE', 'Finance user managing invoices and payments'),
    ('VENDOR', 'External vendor user');
