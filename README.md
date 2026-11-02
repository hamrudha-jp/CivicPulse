# CivicPulse — Smart Public Complaint Prioritization Platform

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-blue.svg)](https://www.postgresql.org/)
[![React](https://img.shields.io/badge/React-18.3-61dafb.svg)](https://react.dev/)
[![Vite](https://img.shields.io/badge/Vite-5.4-purple.svg)](https://vitejs.dev/)
[![Tests](https://img.shields.io/badge/Tests-27%20Passed-success.svg)](https://github.com/)

CivicPulse is an industry-grade municipal grievance orchestration and transparent algorithmic triage system. It replaces arbitrary first-come-first-served municipal queues with dynamic multi-factor priority scoring, heuristic spatial and lexical duplicate detection, scheduled SLA escalation, and role-based department authorization.

---

## Architecture Overview

```
                                      ┌────────────────────────────────┐
                                      │  React 18 + Vite Frontend SPA  │
                                      │  (Tailored Dark Glassmorphism) │
                                      └──────────────┬─────────────────┘
                                                     │ HTTP / REST / JSON & Multipart
                                                     ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                               Spring Boot 3.3.5 Backend                                │
│                                                                                        │
│  ┌───────────────────────┐   ┌───────────────────────┐   ┌──────────────────────────┐  │
│  │ Security & Auth (JWT) │   │ Department RBAC Guard │   │ Global Exception Handler │  │
│  │ Stateless HMAC-SHA256 │   │ Dept Isolation / Admin│   │ RFC 7807 Problem Details │  │
│  └───────────┬───────────┘   └───────────┬───────────┘   └─────────────┬────────────┘  │
│              │                           │                             │               │
│  ┌───────────▼───────────────────────────▼─────────────────────────────▼────────────┐  │
│  │                                Controllers                                       │  │
│  │   AuthController | ComplaintController | OfficialComplaintController             │  │
│  │   DuplicateController | AnalyticsController | AdminController                     │  │
│  └───────────────────────────────────────┬──────────────────────────────────────────┘  │
│                                          │                                             │
│  ┌───────────────────────────────────────▼──────────────────────────────────────────┐  │
│  │                             Core Service & Engine Layer                          │  │
│  │  ┌────────────────────────┐ ┌───────────────────────────┐ ┌────────────────────┐ │  │
│  │  │ Priority Engine        │ │ Duplicate Engine          │ │ SLA Escalation     │ │  │
│  │  │ S * 0.45 + I*0.3 + A*0.25│ │ Haversine + Trigram Sim  │ │ Automated Breaches │ │  │
│  │  └────────────────────────┘ └───────────────────────────┘ └────────────────────┘ │  │
│  │  ┌────────────────────────┐ ┌───────────────────────────┐                        │  │
│  │  │ FileStorageService     │ │ OfficialComplaintService  │                        │  │
│  │  │ Magic-Bytes & MIME Val │ │ Department Authorization  │                        │  │
│  │  └────────────────────────┘ └───────────────────────────┘                        │  │
│  └───────────────────────────────────────┬──────────────────────────────────────────┘  │
│                                          │ JPA / Hibernate 6.5                         │
│                                          ▼                                             │
│  ┌──────────────────────────────────────────────────────────────────────────────────┐  │
│  │                      PostgreSQL 17 Database + pg_trgm                            │  │
│  │     Complaints | Users | Departments | Categories | Duplicates | History         │  │
│  └──────────────────────────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

### Algorithmic Triaging Formulas
1. **Priority Calculation Formula**:
   $$\text{Score} = (S \times 0.45) + (I \times 0.30) + (A \times 0.25)$$
   - **Severity ($S \in [1, 10]$)**: `CRITICAL` = 10.0, `HIGH` = 7.5, `MEDIUM` = 5.0, `LOW` = 2.5.
   - **Public Impact ($I \in [0, 10]$)**:
     $$I = \min(10.0, \, \ln(1 + \text{upvotes}) \times 1.5 + (\text{mergedDuplicates} \times 2.0))$$
   - **Aging Factor ($A \in [0, 10]$)**:
     $$A = \min\left(10.0, \, \frac{\text{elapsedHours}}{\text{slaHours}} \times 5.0\right) + (\text{isEscalated} \,?\, 3.0 : 0.0)$$
2. **Spatial Distance Calculation**:
   - Haversine Great-Circle distance formula calculates exact geographic distance in meters between GPS coordinates `(lat1, lon1)` and `(lat2, lon2)` using Earth radius $R = 6,371,000\text{ m}$.
   - Optimized with geographic bounding box SQL pre-filtering ($d \le 100\text{ m}$).
3. **Lexical Similarity Dual Metric**:
   - Character Trigram Dice Coefficient combined with token Jaccard overlap on sanitized complaints.
   - Composite Confidence threshold: $\ge 0.50$ triggers duplicate flag for official review.

---

## Prerequisites

- **Java**: Java 21 (Eclipse Adoptium Temurin 21 recommended)
- **Build Tool**: Apache Maven 3.9+
- **Database**: PostgreSQL 17 (installed natively or via Docker)
- **Node.js**: Node 18+ and npm (for frontend build and development)
- **Docker**: Docker Desktop (optional, for containerized PostgreSQL)

---

## Configuration & Environment Variables

Configuration parameters in `backend/src/main/resources/application.yml` support environment variable overrides with safe local fallbacks:

| Environment Variable | Description | Safe Default Value |
|---|---|---|
| `SERVER_PORT` | Backend HTTP port | `8080` |
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC connection URL | `jdbc:postgresql://localhost:5432/civicpulse_db` |
| `SPRING_DATASOURCE_USERNAME` | Database username | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | Database password | `postgres123` |
| `APP_JWT_SECRET` | 256-bit Hex secret for HMAC-SHA256 | `404E63526655...566B5970` |
| `APP_JWT_EXPIRATION_MS` | JWT expiration duration in milliseconds | `86400000` (24 hours) |
| `APP_UPLOAD_DIR` | Physical disk storage directory for photos | `./uploads` |
| `APP_CORS_ALLOWED_ORIGINS` | Permitted cross-origin endpoints | `http://localhost:5173,http://localhost:3000` |
| `APP_SLA_ESCALATION_CRON` | Scheduled cron expression for SLA sweeps | `0 */15 * * * *` (every 15 min) |

To customize variables for your local machine, copy the template:
```bash
cp .env.example .env
```

---

## Database Setup

CivicPulse requires a PostgreSQL 17 database named `civicpulse_db` with the `pg_trgm` extension enabled.

### Option A: Local Native PostgreSQL (Port 5432)
1. Ensure your local PostgreSQL service is running on port 5432.
2. Create the database and activate trigram indexing:
   ```sql
   CREATE DATABASE civicpulse_db;
   \c civicpulse_db
   CREATE EXTENSION IF NOT EXISTS pg_trgm;
   ```

### Option B: Docker Compose
If you prefer running PostgreSQL in Docker:
```bash
docker compose up -d
```
The included `docker-compose.yml` configures:
- Image: `postgres:17-alpine`
- Container name: `civicpulse_postgres`
- Persistent named volume: `civicpulse_pgdata`
- Automated health check via `pg_isready`

#### Port Conflict Guidance (Port 5432 vs 5433)
> [!IMPORTANT]
> If port 5432 is already bound by a local native PostgreSQL instance, avoid collisions by running Docker on port 5433:
> 1. In `.env`, set `POSTGRES_PORT=5433`.
> 2. Set `SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/civicpulse_db`.
> 3. Run `docker compose up -d`.

### Database Schema Notes & Migrations
- **Fresh Databases**: Initializing on a new database automatically configures all tables and check constraints with all enum constants (including `CANCELLED`).
- **Existing Databases Upgrade**: If upgrading an existing database initialized before `CANCELLED` was introduced, run the idempotent SQL migration script:
  ```bash
  psql -U postgres -d civicpulse_db -f backend/src/main/resources/db/migration_cancelled_status.sql
  ```
  This safely updates `complaints_status_check`, `complaint_status_history_to_status_check`, and `complaint_status_history_from_status_check` without data disruption.

---

## Startup Commands

### 1. Start Backend Service
```bash
cd backend
mvn spring-boot:run
```
The backend initializes demo seed data automatically on first launch and listens on `http://localhost:8080`.

### 2. Start Frontend Dev Server
```bash
cd frontend
npm install
npm run dev
```
Open your browser at `http://localhost:5173`.

### 3. Build Frontend for Production
```bash
cd frontend
npm run build
```
Generates production-optimized assets in `frontend/dist/`.

---

## Test Execution & Verification

Run the complete Maven test suite:
```bash
cd backend
mvn test
```

### Verified Test Suites (27 Tests Total)
- **`FileStorageServiceTest` (8 tests)**:
  - Valid JPEG magic-byte header inspection (`FF D8 FF`)
  - Valid PNG magic-byte header inspection (`89 50 4E 47 0D 0A 1A 0A`)
  - Valid WEBP magic-byte header inspection (`RIFF....WEBP`)
  - Rejection of spoofed files (executables or plain text disguised with image extensions)
  - MIME type vs extension mismatch rejection
  - Disallowed file format rejection (`.exe`, `.sh`, `.pdf`)
  - Empty file and truncated header rejection
  - 10MB payload size enforcement
- **`ComplaintSecurityAndWorkflowIntegrationTest` (8 tests)**:
  - Unauthenticated access to official endpoints returns `401 Unauthorized`
  - Citizen role attempting official queue returns `403 Forbidden`
  - Cross-department access denial: Water official attempting action on Roads complaint returns `403 Forbidden`
  - Admin cross-department governance: System Admin successfully performs cross-department action (`200 OK`)
  - Citizen cancellation ownership restriction: Citizen cannot cancel another citizen's complaint (`403 Forbidden`)
  - Citizen cancellation status restriction: Citizen cannot cancel an `IN_PROGRESS` or `ASSIGNED` complaint (`400 Bad Request`)
  - Citizen cancellation happy path: Citizen cancels own `SUBMITTED` complaint (`200 OK`, status updated to `CANCELLED`, timeline recorded)
  - Duplicate upvote prevention: Upvoting twice toggles and prevents multiple votes from the same citizen
- **`PriorityCalculationEngineTest` (4 tests)**:
  - High severity baseline scoring
  - Logarithmic public impact amplification
  - SLA breach aging acceleration
  - Score clamping within bounds $[1.0, 10.0]$
- **`SpatialDistanceServiceTest` (3 tests)**:
  - Zero-distance co-located coordinates
  - High-precision Haversine distance verification (~35m)
  - Bounding box filtering
- **`TextSimilarityServiceTest` (3 tests)**:
  - Identical title text evaluation
  - Trigram Dice overlap for paraphrased civic complaints
  - Complete lexical dissimilarity rejection
- **`CivicPulseApplicationTests` (1 test)**:
  - Full Spring Application Context and Hikari Connection Pool bootstrap validation

---

## API Endpoints Reference

| HTTP Method | Endpoint | Allowed Role | Description | Response Status |
|---|---|---|---|---|
| `POST` | `/api/v1/auth/login` | Public | Authenticate and obtain JWT token | `200 OK`, `401 Unauthorized` |
| `POST` | `/api/v1/auth/register` | Public | Register new citizen account | `201 Created`, `400 Bad Request` |
| `GET` | `/api/v1/auth/me` | Authenticated | Retrieve authenticated user profile | `200 OK`, `401 Unauthorized` |
| `GET` | `/api/v1/complaints/public-feed` | Public | View prioritized complaints feed | `200 OK` |
| `GET` | `/api/v1/complaints/track/{trackingNumber}` | Public | Real-time status, timeline, & score | `200 OK`, `404 Not Found` |
| `GET` | `/api/v1/departments` | Public | List all municipal departments | `200 OK` |
| `GET` | `/api/v1/departments/{id}/categories`| Public | List grievance categories for department | `200 OK`, `404 Not Found` |
| `POST` | `/api/v1/complaints/json` | `ROLE_CITIZEN` | Submit complaint with JSON payload | `201 Created`, `400 Bad Request` |
| `POST` | `/api/v1/complaints` | `ROLE_CITIZEN` | Submit complaint with photo attachment | `201 Created`, `400 Bad Request` |
| `GET` | `/api/v1/complaints/my` | `ROLE_CITIZEN` | Paginated complaints submitted by caller | `200 OK`, `401 Unauthorized` |
| `POST` | `/api/v1/complaints/{id}/upvote` | `ROLE_CITIZEN` | Toggle upvote (amplifies community score)| `200 OK`, `400 Bad Request` |
| `PATCH` | `/api/v1/complaints/{id}/cancel` | `ROLE_CITIZEN` | Cancel own complaint (SUBMITTED only) | `200 OK`, `400 Bad Request`, `403 Forbidden` |
| `GET` | `/api/v1/official/complaints` | `ROLE_OFFICIAL`, `ROLE_ADMIN` | Prioritized official triage queue | `200 OK`, `401/403` |
| `POST` | `/api/v1/official/complaints/{id}/assign` | `ROLE_OFFICIAL`, `ROLE_ADMIN` | Assign complaint to department official | `200 OK`, `403 Forbidden` |
| `PATCH` | `/api/v1/official/complaints/{id}/status` | `ROLE_OFFICIAL`, `ROLE_ADMIN` | Update operational status | `200 OK`, `403 Forbidden` |
| `POST` | `/api/v1/official/complaints/{id}/resolve/json` | `ROLE_OFFICIAL`, `ROLE_ADMIN` | Resolve complaint with statement | `200 OK`, `403 Forbidden` |
| `POST` | `/api/v1/official/complaints/{id}/resolve` | `ROLE_OFFICIAL`, `ROLE_ADMIN` | Resolve complaint with photo proof | `200 OK`, `403 Forbidden` |
| `GET` | `/api/v1/duplicates/pending` | `ROLE_OFFICIAL`, `ROLE_ADMIN` | View flagged candidate duplicate pairs | `200 OK`, `403 Forbidden` |
| `POST` | `/api/v1/duplicates/{matchId}/review` | `ROLE_OFFICIAL`, `ROLE_ADMIN` | Review & merge or dismiss duplicate | `200 OK`, `403 Forbidden` |
| `GET` | `/api/v1/analytics/dashboard` | `ROLE_OFFICIAL`, `ROLE_ADMIN` | Department resolution rates & SLA stats | `200 OK`, `403 Forbidden` |
| `POST` | `/api/v1/admin/escalation/trigger`| `ROLE_ADMIN` | Manually run automated SLA escalation | `200 OK`, `403 Forbidden` |

---

## Pre-Seeded Demo Accounts

The database is pre-seeded with realistic municipal accounts across departments:

| Role | Name | Email | Password | Department Scope |
|---|---|---|---|---|
| **System Admin** | Chief Administrative Officer | `admin@civicpulse.gov` | `admin123` | System-wide (Cross-Department) |
| **Official (Roads)** | Rajesh Sharma (Exec. Engineer) | `roads.officer@civicpulse.gov` | `official123` | Roads & Infrastructure |
| **Official (Water)** | Pooja Nair (Zonal Engineer) | `water.officer@civicpulse.gov` | `official123` | Water Supply & Sewerage |
| **Citizen** | Arun Kumar | `arun.kumar@gmail.com` | `citizen123` | Citizen Submissions & Upvotes |
| **Citizen** | Deepa Sharma | `deepa.s@gmail.com` | `citizen123` | Citizen Submissions & Upvotes |

---

## Postman Collection Usage

1. Open Postman and click **Import**.
2. Select:
   - `CivicPulse.postman_collection.json`
   - `CivicPulse.postman_environment.json`
3. In the top-right environment selector, select **CivicPulse Local Environment**.
4. Open folder `1. Authentication` and execute `Login - Citizen (Arun Kumar)`.
   - The test script extracts the JWT token and automatically saves it to the `{{authToken}}` environment variable.
5. All subsequent requests in the collection automatically inherit and use `{{authToken}}` in their `Authorization: Bearer {{authToken}}` headers.
