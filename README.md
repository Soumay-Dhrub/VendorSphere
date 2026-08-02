# VendorSphere

**B2B Vendor, RFQ & Procurement Management Platform**

VendorSphere is a centralized procurement platform for organizations managing vendors, quotations, purchase orders, deliveries, invoices, and vendor performance.

## Tech Stack

| Layer | Technology |
|-------|------------|
| Backend | Java 21, Spring Boot 3.4, Spring Security, JPA, Flyway |
| Frontend | Next.js, TypeScript, Tailwind CSS, TanStack Query |
| Database | PostgreSQL 16 |
| Infrastructure | Docker, Docker Compose, GitHub Actions |

## Procurement Lifecycle

```
Purchase Requirement → RFQ → Quotations → Comparison → Approval
→ Purchase Order → Delivery → Invoice → Payment → Vendor Performance
```

## Project Structure

```
VendorSphere/
├── backend/                 # Spring Boot modular monolith
│   └── src/main/java/com/vendorsphere/
│       ├── auth/            # Authentication & JWT
│       ├── user/            # User management
│       ├── organization/    # Org & departments
│       ├── vendor/          # Vendor management
│       ├── procurement/     # Purchase requests
│       ├── rfq/             # RFQ management
│       ├── quotation/       # Quotation & comparison
│       ├── purchaseorder/   # Purchase orders
│       ├── delivery/        # Delivery tracking
│       ├── invoice/         # Invoice & three-way matching
│       ├── payment/         # Payment tracking
│       ├── analytics/       # Dashboard & vendor scoring
│       ├── notification/    # In-app notifications
│       ├── audit/           # Audit logging
│       └── common/          # Shared utilities
├── frontend/                # Next.js application
├── docker-compose.yml
└── .github/workflows/ci.yml
```

## Quick Start

### Prerequisites

- Java 21+
- Maven 3.9+
- Node.js 22+
- Docker & Docker Compose

### Run with Docker (recommended)

```bash
docker compose up --build
```

| Service | URL |
|---------|-----|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| PostgreSQL | localhost:5432 |

### Local Development

**1. Start PostgreSQL**

```bash
docker compose up postgres -d
```

**2. Run backend**

```bash
cd backend
mvn spring-boot:run
```

**3. Run frontend**

```bash
cd frontend
npm install
npm run dev
```

## API Documentation

OpenAPI docs are available at `/swagger-ui.html` when the backend is running.

Health check: `GET /api/v1/health`

## User Roles

| Role | Description |
|------|-------------|
| ADMIN | Organization settings, users, departments |
| PROCUREMENT_MANAGER | Approvals, vendor selection |
| PROCUREMENT_OFFICER | RFQs, POs, deliveries |
| REQUESTER | Purchase requirements |
| FINANCE | Invoices and payments |
| VENDOR | RFQ responses, deliveries, invoices |

## Development Phases

- [x] **Phase 1** — Foundation (Spring Boot, Next.js, PostgreSQL, Docker, Flyway, CI)
- [ ] **Phase 2** — Authentication (JWT, RBAC, users, departments)
- [ ] **Phase 3** — Vendor Management
- [ ] **Phase 4** — Purchase Requests
- [ ] **Phase 5** — RFQ
- [ ] **Phase 6** — Quotations & Comparison
- [ ] **Phase 7** — Award & PO
- [ ] **Phase 8** — Delivery
- [ ] **Phase 9** — Invoice & Three-Way Matching
- [ ] **Phase 10** — Analytics & Vendor Scoring
- [ ] **Phase 11** — Frontend modules
- [ ] **Phase 12** — Testing & Deployment

## Signature Demo Scenario

Engineering requests 20 laptops → RFQ-2026-001 → 3 vendor quotations compared → Vendor A selected → PO-2026-001 → delivery → invoice → three-way match → payment → vendor score updated.

## License

Private — placement project.
