# Deployment Guide

# Overview

The Orphanage Management System (OMS) is designed to be deployed using a cloud-native, cost-optimized architecture.

The deployment strategy prioritizes:

- Low operational cost
- High availability
- Easy maintenance
- Scalability
- Security
- Production readiness

**Operator runbook (greenfield):** [14_Production_Runbook.md](./14_Production_Runbook.md)  
**CD workflow:** `.github/workflows/cd-deploy.yml`  
**CI tests:** `.github/workflows/ci-tests.yml`

---

# Deployment Architecture

```text
                Internet
                     │
                     ▼
        Google Cloud HTTPS Load Balancer
              (custom domain + managed SSL)
                     │
        ┌────────────┴────────────┐
        │ path /api/*             │ default /
        ▼                         ▼
Spring Boot API              Angular SPA
(Cloud Run)                  (GCS bucket)
        │
        ├──────────────► Supabase PostgreSQL
        └──────────────► GCS (private docs bucket)
```

**Single-origin production (Milestone 15):**

| Path | Backend |
|------|---------|
| `/api` and `/api/*` | Cloud Run (paths preserved; Spring serves `/api/v1/**`) |
| All other paths | GCS SPA bucket |

- Browser `apiBaseUrl` is **`/api/v1`** (same origin) — see `frontend/src/environments/environment.prod.ts`.
- Cloud Run **ingress** = `internal-and-cloud-load-balancing` (no public `*.run.app` API).
- **SPA deep links:** backend-bucket / website error page returns `index.html` for missing objects so Angular routes work on refresh.
- **Cloud CDN:** optional later; attach to the SPA backend without changing path routing.
- **Actuator** (`/actuator/health`, Prometheus): used for Cloud Run probes only — **not** exposed on the public Load Balancer URL map.

Secrets: Google Secret Manager. Deploy identity: GitHub Actions → Workload Identity Federation → `oms-deploy` SA.

---

# Technology Stack

## Frontend

Angular

Hosting

Google Cloud Storage (behind Global HTTPS Load Balancer)

Reason

- Very low cost
- CDN ready (Cloud CDN optional)
- HTTPS via Load Balancer + Google-managed certificate
- Easy deployment via CD (`gcloud storage rsync`)

---

## Backend

Java 21

Spring Boot 3

Docker

Hosting

Google Cloud Run

Reason

- Auto Scaling
- Scale to Zero
- HTTPS Included (via LB)
- Managed Infrastructure
- Pay only for usage

---

## Database

PostgreSQL

Provider

Supabase

Reason

- PostgreSQL
- Free Tier
- Reliable
- Easy Integration
- Low Cost

Prod JDBC uses `sslmode=require` by default (`DB_SSL_MODE` in `application-prod.yml`).

---

## Document Storage

Google Cloud Storage (private bucket)

Documents include

- Student Photo
- Aadhaar
- Birth Certificate
- Medical Reports
- Mark Sheets
- Other Documents

Database stores only metadata.

The SPA bucket and the documents bucket are **separate**.

---

# Environment Architecture

Development

Angular Local

↓

Spring Boot Local

↓

Local PostgreSQL

↓

Local File Storage

---

Testing

Angular

↓

Spring Boot

↓

Supabase Dev Database (optional)

↓

Google Cloud Storage Test Bucket (optional)

---

Production

Custom domain → Global HTTPS Load Balancer

↓

GCS SPA + Cloud Run API

↓

Supabase PostgreSQL + private GCS docs

---

# Environments

Development

Purpose

Local development

---

Testing

Purpose

Testing new features (clone the production runbook with different project/buckets)

---

Production

Purpose

Live system

Milestone 15 provisions a single production environment. Staging is optional later.

---

# Environment Variables

Backend (Cloud Run / `.env` local)

```
SPRING_PROFILES_ACTIVE

DB_HOST
DB_PORT
DB_NAME
DB_USERNAME
DB_PASSWORD          # Secret Manager in prod
DB_SSL_MODE          # default require in prod

JWT_SECRET           # Secret Manager in prod
JWT_EXPIRATION
JWT_REFRESH_EXPIRATION
JWT_ISSUER

GOOGLE_CLIENT_ID

GCS_BUCKET_NAME      # private docs bucket

OMS_CORS_ALLOWED_ORIGINS   # https://your.domain

OMS_BOOTSTRAP_ADMIN_USERNAME
OMS_BOOTSTRAP_ADMIN_EMAIL
OMS_BOOTSTRAP_ADMIN_PASSWORD   # Secret Manager in prod
```

Frontend (build-time)

```
apiBaseUrl=/api/v1
googleClientId       # injected by CD from GitHub variable GOOGLE_CLIENT_ID
```

Never commit secrets to Git.

---

# Secret Management

Use

Google Secret Manager

Store

- Database Password (`DB_PASSWORD`)
- JWT Secret (`JWT_SECRET`)
- Bootstrap admin password (`OMS_BOOTSTRAP_ADMIN_PASSWORD`)

`GOOGLE_CLIENT_SECRET` is not required for the current GIS ID-token verification flow.

Never store secrets inside

application.yml (as literal values)

application.properties

Git Repository

GitHub Actions secrets for DB/JWT (use Secret Manager + Cloud Run `--set-secrets`)

---

# Build Pipeline

## Continuous Integration

GitHub Actions → `.github/workflows/ci-tests.yml`

- Backend `mvn verify` (JaCoCo ≥80% lines)
- Frontend Karma coverage
- Cypress critical-path E2E

## Continuous Deployment

GitHub Actions → `.github/workflows/cd-deploy.yml`

```text
GitHub (main or workflow_dispatch)
        ↓
Workload Identity Federation
        ↓
┌───────┴────────┐
▼                ▼
Docker build     ng build (prod)
Artifact Registry
        ↓                ↓
Cloud Run deploy    GCS SPA sync
```

First-time infrastructure (project, LB, WIF, Supabase, secrets) is **manual** via [14_Production_Runbook.md](./14_Production_Runbook.md). CD assumes that work is done.

---

# Angular Deployment

Generate Production Build

```
ng build --configuration production
```

CD injects `googleClientId`, then uploads `dist/oms-frontend/browser` to the SPA bucket.

Configure

- Cache: `index.html` no-cache; hashed assets long-lived
- HTTPS via Load Balancer
- SPA fallback for client-side routes

---

# Spring Boot Deployment

Package / image

```
docker build -t oms-api backend/
```

Image includes `SPRING_PROFILES_ACTIVE=prod` by default ([backend/Dockerfile](../backend/Dockerfile)).

Cloud Run listens on `PORT` (mapped to Spring `server.port`).

Deploy to

Cloud Run (via CD or runbook bootstrap)

---

# Database Deployment

Create PostgreSQL Database (Supabase)

Apply Flyway Migrations (on Cloud Run startup)

Seed Initial Data (Flyway roles + AdminBootstrapRunner when users empty)

Verify Indexes

---

# Initial Seed Data

Roles

ADMIN

STAFF

Create

Initial Administrator Account (`OMS_BOOTSTRAP_*` secrets/env)

---

# File Storage Structure

student-documents/

    student-id/

        profile-photo.jpg

        aadhaar.pdf

        birth-certificate.pdf

        medical-report.pdf

---

# Backup Strategy

Database

Daily Backup (enable in Supabase)

Document Storage

Weekly Backup / bucket versioning (optional)

Audit Logs

Never Delete (append-only trigger)

---

# Monitoring

Google Cloud Logging (Cloud Run default)

Google Cloud Monitoring

Spring Boot Actuator

Health Endpoint (Cloud Run probes only)

```
/actuator/health
```

Metrics (optional internal scrape — not on public LB)

```
/actuator/prometheus
```

Future

Grafana

Prometheus

---

# Security

HTTPS Only (LB + managed cert + HTTP redirect)

JWT Authentication

Google OAuth (GIS ID token)

BCrypt Passwords

Private documents bucket (ADC on Cloud Run runtime SA)

API not publicly exposed on `*.run.app` (LB-only ingress)

Input Validation

Output Validation

File Type Validation

Virus Scan (Future)

---

# Cost Optimization

Angular

Google Cloud Storage

Nearly Free (plus LB baseline cost)

Backend

Cloud Run

Scale to Zero

Database

Supabase

Free Tier (or paid as needed)

Storage

Google Cloud Storage

Pay Per Usage

Note: a Global HTTPS Load Balancer has ongoing base cost even at low traffic — review current pricing.

---

# Rollback

- API: redeploy previous Artifact Registry image digest / Cloud Run revision
- SPA: redeploy previous git SHA via CD, or restore bucket versions
- Details: [14_Production_Runbook.md](./14_Production_Runbook.md) §13

---

# Future Improvements

Cloud CDN

Redis Cache

Multi-region Deployment

Disaster Recovery

Automated Backups

Performance Monitoring

Application Insights

Terraform / Pulumi (IaC) for the runbook steps
