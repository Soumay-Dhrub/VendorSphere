# VendorSphere

**End-to-end B2B procurement platform — from purchase request to vendor performance.**

VendorSphere digitizes the complete sourcing lifecycle for small and mid-sized organizations: raise purchase requests, run RFQs, compare scored vendor quotations, award with justification, track deliveries, verify invoices with three-way matching, record payments and continuously score supplier performance — all in one auditable workspace.

[![Java](https://img.shields.io/badge/Java-21-orange)](https://adoptium.net)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-brightgreen)](https://spring.io/projects/spring-boot)
[![Next.js](https://img.shields.io/badge/Next.js-16-black)](https://nextjs.org)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)](https://www.postgresql.org)
[![Tests](https://img.shields.io/badge/tests-293%20backend%20%7C%2027%20frontend-success)](#testing)

---

## Why VendorSphere

Procurement teams usually coordinate through spreadsheets, email threads and PDF quotations. That creates real, daily problems:

| Without VendorSphere | With VendorSphere |
| --- | --- |
| Quotes compared by hand in Excel | Normalized side-by-side comparison with weighted scores |
| No idea where a request/PO/invoice stands | Status-driven lifecycle for every document |
| Invoice over-billing slips through | Automated **three-way matching** (PO ↔ GRN ↔ Invoice) |
| Supplier choices based on gut feel | Monthly **vendor performance scorecards** |
| Decisions with no paper trail | Append-only audit log on every critical action |

## Core Capabilities

- **Vendor Management** — profiles, contacts, compliance documents with expiry tracking, categories, status lifecycle (Prospective → Active → Suspended → Blacklisted)
- **Purchase Requests** — department requirements with line items, attachments, DRAFT → SUBMITTED → APPROVED workflow
- **RFQ Management** — create from approved requests, invite vendors (all-or-nothing validation), auto-close past deadline, cancellation with reason
- **Quotation Engine** — server-computed totals (clients can't manipulate figures), confidentiality between competing vendors, weighted evaluation scoring (price / delivery / warranty / history) with a recommended bid
- **Purchase Orders** — generated straight from the awarded quotation; issue → acknowledge → partial/full delivery → close
- **Delivery Tracking** — partial receipts, damaged/rejected quantities, automatic overdue flagging at 00:30/01:00 UTC jobs
- **Three-Way Matching** — quantity, unit-price, missing-delivery and duplicate-invoice findings in precedence order; approval blocked until exceptions are resolved or explicitly overridden with justification
- **Payments** — over-payment protection, partial payments, outstanding payables by vendor
- **Vendor Performance** — five-metric scoring engine (delivery, quality, pricing, responsiveness, fulfilment) persisted as monthly snapshots
- **Analytics** — spend trend, active RFQs, open POs, outstanding/overdue invoices dashboard
- **Platform** — JWT auth with refresh rotation, six-role RBAC enforced server-side, tenant isolation (cross-tenant IDs return 404), append-only audit trail, OpenAPI docs

## Architecture

Modular monolith — clean module boundaries without microservice overhead.

```
VendorSphere/
├── backend/                        # Spring Boot 3.4 · Java 21 · Maven
│   └── src/main/java/com/vendorsphere/
│       ├── auth/                   #   JWT authentication & refresh tokens
│       ├── user/ organization/     #   Users, roles, departments
│       ├── vendor/                 #   Vendors, contacts, documents, categories
│       ├── procurement/            #   Purchase requests & review
│       ├── rfq/                    #   RFQs, invitations, closing job
│       ├── quotation/              #   Submissions, comparison, evaluation, award
│       ├── purchaseorder/          #   PO generation & lifecycle
│       ├── delivery/               #   Goods receipts, progress derivation
│       ├── invoice/                #   Invoices + three-way matching engine
│       ├── payment/                #   Payments & outstanding payables
│       ├── analytics/              #   Performance engine & dashboard
│       ├── notification/ audit/    #   Cross-cutting notification + audit
│       └── common/                 #   Money, state machines, pagination,
│                                   #   reference numbers, attachments
├── frontend/                       # Next.js App Router · TypeScript · Tailwind
│   └── src/
│       ├── app/                    #   Landing, auth, (app) shell + screens
│       ├── components/             #   Shell, guards, shadcn/ui primitives
│       └── lib/                    #   Typed API clients, TanStack Query hooks
├── docker-compose.yml              # PostgreSQL + API + Web
└── .github/workflows/              # CI: build + test on push
```

### Design decisions worth reading the code for

- **Pure engines** (`ComparisonEngine`, `EvaluationEngine`, `ThreeWayMatcher`, `PerformanceCalculator`, `QuotationCalculator`) are framework-free functions — property-tested arithmetic, no Spring/JPA inside
- **Declarative state machines** encode every lifecycle (vendor, PR, RFQ, PO, invoice); invalid transitions are a single pinned 409
- **Money is never floats** — one `Money` utility owns scale-2 HALF_UP rounding and quantity scale-3
- **Reference numbers** come from a sequence table with `UPDATE … RETURNING`, safe under concurrency
- **Optimistic locking** (`@Version`) on every mutable business entity → concurrent edits surface as 409
- **Tenant isolation** is structural: every finder is keyed on `organization_id`; foreign identifiers answer 404, never 403

## Getting Started

**Prerequisites:** Docker Desktop, Java 21 (or use the container), Node 20+.

```bash
# 1. Clone
git clone https://github.com/Soumay-Dhrub/VendorSphere.git
cd VendorSphere

# 2. Start everything (Postgres + API + Web)
docker compose up -d --build

# 3. Open the app
#    Web  → http://localhost:3000
#    API  → http://localhost:8080/api/v1
#    Docs → http://localhost:8080/swagger-ui.html
```

Prefer running services natively?

```bash
docker compose up -d postgres        # database only

cd backend  && mvn spring-boot:run   # API on :8080
cd frontend && npm install && npm run dev   # Web on :3000
```

> Local port conflicts? See `docker-compose.override.yml` pattern — this repo maps DB `5434`, API `8081`, web `3001`.

**First run:** register an organization on `/register` — that account becomes its ADMIN. Seed further users via Admin → Users.

## The Signature Demo Flow

Run the whole product in ~10 minutes:

1. **Requester** raises *PR-… "20 development laptops"* → submits
2. **Manager** approves it
3. **Officer** creates an RFQ from the request, invites 3 active vendors, opens it
4. **Vendors** (separate accounts) submit priced quotations — totals computed server-side
5. Officer runs **Evaluate** → platform scores each bid and recommends one
6. **Comparison screen** shows every factor side-by-side; manager awards with a written justification
7. **PO** generated from the winning quote → issued → vendor acknowledges → deliveries recorded (partial supported)
8. Vendor submits an **invoice** → three-way match runs automatically → finance approves and records payment
9. Watch the **vendor scorecard** update and the dashboard spend climb

## Testing

```bash
cd backend  && mvn test     # 293 tests: unit + Testcontainers integration + jqwik properties
cd frontend && npm test     # 27 tests: Vitest + React Testing Library
```

Coverage highlights: state-machine transition tables, quotation total consistency (property-based), evaluation bounds & single-recommendation, three-way match precedence, payment over-flow guards, pagination contracts, tenant isolation against real Postgres.

## Security Notes

- Passwords hashed with BCrypt; short-lived access + rotating refresh tokens
- Every authorization rule enforced server-side (`@PreAuthorize` + service-level guards); vendor users are fail-closed to their own data
- Critical actions (awards, cancellations, overrides) persist previous/new snapshots to an append-only audit table

## Roadmap

Email notifications · GST-ready tax handling · approval delegations · contract management · SSO (OIDC) · Kubernetes Helm chart.

---

Built as a production-style portfolio project demonstrating complex relational modeling, transactional business logic, RBAC and procurement domain depth.
