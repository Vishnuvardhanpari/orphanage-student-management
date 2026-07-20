# Production Operator Runbook (Milestone 15)

> First-time greenfield provisioning for OMS production.
> Prerequisites you must supply (not created by this repo): a GCP billing account and a custom domain you control.
>
> Related: [09_Deployment.md](./09_Deployment.md), CD workflow [cd-deploy.yml](../.github/workflows/cd-deploy.yml).

---

## 0. Prerequisites

| Item | Notes |
|------|--------|
| GCP billing account | Linked to the Google account that will own the project |
| Custom domain | e.g. `app.example.com` — DNS editable at your registrar |
| GitHub repo admin | To configure Actions variables and environments |
| Supabase account | For managed PostgreSQL |
| `gcloud` CLI | Authenticated as a user with Project Creator / Owner |

Set shell variables (adjust values):

```bash
export PROJECT_ID="oms-prod-YOURORG"
export REGION="asia-south1"          # change if needed
export DOMAIN="app.example.com"
export AR_REPO="oms"
export SPA_BUCKET="oms-spa-prod-${PROJECT_ID}"
export DOCS_BUCKET="oms-docs-prod-${PROJECT_ID}"
export CLOUD_RUN_SERVICE="oms-api"
export DEPLOY_SA="oms-deploy"
export RUNTIME_SA="oms-cloudrun"
export GITHUB_ORG="YOUR_GITHUB_ORG"
export GITHUB_REPO="orphanage-student-management"
```

Billing and domain purchase happen in Google Cloud / your registrar — not in this runbook’s automation.

---

## 1. GCP project and APIs

```bash
gcloud projects create "${PROJECT_ID}" --name="OMS Production"
# Link billing in Console: Billing → Account management → link PROJECT_ID
gcloud config set project "${PROJECT_ID}"

gcloud services enable \
  run.googleapis.com \
  artifactregistry.googleapis.com \
  secretmanager.googleapis.com \
  storage.googleapis.com \
  compute.googleapis.com \
  iam.googleapis.com \
  iamcredentials.googleapis.com \
  sts.googleapis.com \
  cloudresourcemanager.googleapis.com
```

---

## 2. Artifact Registry

```bash
gcloud artifacts repositories create "${AR_REPO}" \
  --repository-format=docker \
  --location="${REGION}" \
  --description="OMS container images"

export AR_HOST="${REGION}-docker.pkg.dev/${PROJECT_ID}/${AR_REPO}"
```

---

## 3. Cloud Storage buckets

### SPA bucket (public via Load Balancer only)

```bash
gcloud storage buckets create "gs://${SPA_BUCKET}" \
  --location="${REGION}" \
  --uniform-bucket-level-access

# Website-style index for LB backend bucket
gcloud storage buckets update "gs://${SPA_BUCKET}" \
  --web-main-page-suffix=index.html \
  --web-error-page=index.html
```

Do **not** grant `allUsers` objectViewer if traffic is only via the HTTPS Load Balancer with a backend bucket (LB uses a Google-managed identity). If Console requires public read for classic website hosting experiments, prefer LB backend-bucket access instead of making objects world-readable.

### Documents bucket (private — student photos/files)

```bash
gcloud storage buckets create "gs://${DOCS_BUCKET}" \
  --location="${REGION}" \
  --uniform-bucket-level-access

# No public IAM. Only the Cloud Run runtime SA may read/write objects.
```

Object layout (application-managed):

```text
student-documents/{student-id}/profile-photo.jpg
student-documents/{student-id}/...
```

---

## 4. Service accounts and IAM

```bash
gcloud iam service-accounts create "${RUNTIME_SA}" \
  --display-name="OMS Cloud Run runtime"

gcloud iam service-accounts create "${DEPLOY_SA}" \
  --display-name="OMS GitHub Actions deploy"

export RUNTIME_SA_EMAIL="${RUNTIME_SA}@${PROJECT_ID}.iam.gserviceaccount.com"
export DEPLOY_SA_EMAIL="${DEPLOY_SA}@${PROJECT_ID}.iam.gserviceaccount.com"

# Runtime: secrets + private docs bucket
gcloud projects add-iam-policy-binding "${PROJECT_ID}" \
  --member="serviceAccount:${RUNTIME_SA_EMAIL}" \
  --role="roles/secretmanager.secretAccessor"

gcloud storage buckets add-iam-policy-binding "gs://${DOCS_BUCKET}" \
  --member="serviceAccount:${RUNTIME_SA_EMAIL}" \
  --role="roles/storage.objectAdmin"

# Deploy: push images, deploy Cloud Run, write SPA objects
gcloud projects add-iam-policy-binding "${PROJECT_ID}" \
  --member="serviceAccount:${DEPLOY_SA_EMAIL}" \
  --role="roles/run.admin"

gcloud projects add-iam-policy-binding "${PROJECT_ID}" \
  --member="serviceAccount:${DEPLOY_SA_EMAIL}" \
  --role="roles/artifactregistry.writer"

gcloud projects add-iam-policy-binding "${PROJECT_ID}" \
  --member="serviceAccount:${DEPLOY_SA_EMAIL}" \
  --role="roles/iam.serviceAccountUser"

gcloud storage buckets add-iam-policy-binding "gs://${SPA_BUCKET}" \
  --member="serviceAccount:${DEPLOY_SA_EMAIL}" \
  --role="roles/storage.objectAdmin"
```

Grant the deploy SA permission to act as the runtime SA when deploying Cloud Run:

```bash
gcloud iam service-accounts add-iam-policy-binding "${RUNTIME_SA_EMAIL}" \
  --member="serviceAccount:${DEPLOY_SA_EMAIL}" \
  --role="roles/iam.serviceAccountUser"
```

---

## 5. Secret Manager

Create secrets (use strong random values; never commit them):

```bash
# Examples — paste values via stdin in a secure terminal
echo -n 'YOUR_SUPABASE_DB_PASSWORD' | gcloud secrets create DB_PASSWORD --data-file=-
openssl rand -base64 48 | tr -d '\n' | gcloud secrets create JWT_SECRET --data-file=-
echo -n 'YOUR_BOOTSTRAP_ADMIN_PASSWORD' | gcloud secrets create OMS_BOOTSTRAP_ADMIN_PASSWORD --data-file=-
```

GIS login verifies ID tokens with `GOOGLE_CLIENT_ID` only; `GOOGLE_CLIENT_SECRET` is not required by the current Spring Boot verifier. Skip creating it unless you add a confidential OAuth flow later.

Grant runtime SA access (already has project-level `secretAccessor`; confirm per-secret if you use fine-grained IAM).

---

## 6. Supabase PostgreSQL

1. Create a Supabase project (region close to `${REGION}` when possible).
2. Database settings → connection string (prefer **Session** or direct Postgres host for Flyway on Cloud Run; avoid transaction-mode pooler if migrations fail).
3. Note: host, port (`5432` or pooler port), database name, user, password.
4. Network: allow connections from the public internet (Cloud Run egress IPs are dynamic) or use Supabase’s recommended SSL + strong password. OMS prod JDBC uses `sslmode=require` by default (`DB_SSL_MODE`).

Non-secret values for Cloud Run / CD:

| Variable | Example |
|----------|---------|
| `DB_HOST` | `db.xxxxx.supabase.co` |
| `DB_PORT` | `5432` |
| `DB_NAME` | `postgres` |
| `DB_USERNAME` | `postgres` |
| `DB_PASSWORD` | Secret Manager `DB_PASSWORD` |

Flyway and `AdminBootstrapRunner` run on first Cloud Run start when the schema/users table is empty.

---

## 7. Cloud Run service (initial revision)

Build and push once (or rely on CD after WIF is ready):

```bash
gcloud auth configure-docker "${REGION}-docker.pkg.dev"

docker build -t "${AR_HOST}/oms-api:bootstrap" backend/
docker push "${AR_HOST}/oms-api:bootstrap"

gcloud run deploy "${CLOUD_RUN_SERVICE}" \
  --image="${AR_HOST}/oms-api:bootstrap" \
  --region="${REGION}" \
  --platform=managed \
  --service-account="${RUNTIME_SA_EMAIL}" \
  --ingress=internal-and-cloud-load-balancing \
  --allow-unauthenticated \
  --port=8080 \
  --memory=1Gi \
  --cpu=1 \
  --min-instances=0 \
  --max-instances=5 \
  --timeout=300 \
  --set-env-vars="SPRING_PROFILES_ACTIVE=prod,DB_HOST=...,DB_PORT=5432,DB_NAME=postgres,DB_USERNAME=postgres,GCS_BUCKET_NAME=${DOCS_BUCKET},OMS_CORS_ALLOWED_ORIGINS=https://${DOMAIN},GOOGLE_CLIENT_ID=...,JWT_ISSUER=oms,JWT_EXPIRATION=3600000,JWT_REFRESH_EXPIRATION=604800000" \
  --set-secrets="DB_PASSWORD=DB_PASSWORD:latest,JWT_SECRET=JWT_SECRET:latest,OMS_BOOTSTRAP_ADMIN_PASSWORD=OMS_BOOTSTRAP_ADMIN_PASSWORD:latest"
```

Notes:

- `--allow-unauthenticated` lets the Load Balancer invoke the service; JWT still protects API routes.
- `--ingress=internal-and-cloud-load-balancing` blocks public `*.run.app` access.
- Health / startup probes should use `/actuator/health` (not published on the LB URL map).
- Do **not** add `/actuator` or `/actuator/prometheus` to the public Load Balancer path rules.

---

## 8. HTTPS Load Balancer (single origin)

Target URL map:

| Path | Backend |
|------|---------|
| `/api` and `/api/*` | Serverless NEG → Cloud Run (paths **preserved**; Spring expects `/api/v1/...`) |
| All other paths (default) | Backend bucket → SPA bucket |

### SPA deep-link fallback

Angular client routes (`/students/123`, etc.) must return `index.html`. Configure the backend bucket / URL map **custom error response** so object `404` returns `index.html` with HTTP `200` (or use the bucket web error page set in §3). Verify refresh on a deep link after cutover.

### Managed certificate

```bash
# After reserved IP + forwarding rules exist (Console or gcloud):
gcloud compute ssl-certificates create oms-prod-cert \
  --domains="${DOMAIN}" \
  --global
```

Wait until certificate status is **ACTIVE** before relying on HTTPS. Point DNS A/AAAA (or CNAME per Google’s LB docs) to the LB IP.

Enable HTTP → HTTPS redirect on the HTTP proxy.

Cloud CDN may be attached later to the backend bucket without changing path routing.

### Checklist

- [ ] Global external Application Load Balancer created
- [ ] Backend bucket → `${SPA_BUCKET}`
- [ ] Serverless NEG → `${CLOUD_RUN_SERVICE}` in `${REGION}`
- [ ] Path matcher `/api` and `/api/*` → Cloud Run (no path strip)
- [ ] Default route → SPA
- [ ] Google-managed cert for `${DOMAIN}` ACTIVE
- [ ] DNS pointed at LB
- [ ] Deep-link refresh serves the SPA

---

## 9. Google Identity Services (OAuth client)

1. Google Cloud Console → APIs & Services → Credentials → OAuth 2.0 Client ID (Web).
2. Authorized JavaScript origins: `https://${DOMAIN}`
3. Authorized redirect URIs: not required for GIS ID-token button flow used by OMS.
4. Put the client ID in:
   - Cloud Run env `GOOGLE_CLIENT_ID`
   - GitHub Actions variable `GOOGLE_CLIENT_ID` (frontend CD injects into `environment.prod.ts`)

---

## 10. GitHub Workload Identity Federation

```bash
gcloud iam workload-identity-pools create "github-pool" \
  --location="global" \
  --display-name="GitHub Actions pool"

gcloud iam workload-identity-pools providers create-oidc "github-provider" \
  --location="global" \
  --workload-identity-pool="github-pool" \
  --display-name="GitHub provider" \
  --issuer-uri="https://token.actions.githubusercontent.com" \
  --attribute-mapping="google.subject=assertion.sub,attribute.repository=assertion.repository,attribute.ref=assertion.ref" \
  --attribute-condition="assertion.repository=='${GITHUB_ORG}/${GITHUB_REPO}'"

export PROJECT_NUMBER="$(gcloud projects describe ${PROJECT_ID} --format='value(projectNumber)')"
export WIF_PROVIDER="projects/${PROJECT_NUMBER}/locations/global/workloadIdentityPools/github-pool/providers/github-provider"
export WIF_SA_MEMBER="principalSet://iam.googleapis.com/projects/${PROJECT_NUMBER}/locations/global/workloadIdentityPools/github-pool/attribute.repository/${GITHUB_ORG}/${GITHUB_REPO}"

gcloud iam service-accounts add-iam-policy-binding "${DEPLOY_SA_EMAIL}" \
  --role="roles/iam.workloadIdentityUser" \
  --member="${WIF_SA_MEMBER}"
```

### GitHub repository variables (Settings → Secrets and variables → Actions)

Create a GitHub **Environment** named `production` (referenced by `cd-deploy.yml`). Optionally restrict it to the `main` branch.

| Name | Value |
|------|--------|
| `GCP_PROJECT_ID` | `${PROJECT_ID}` |
| `GCP_REGION` | `${REGION}` |
| `GCP_WIF_PROVIDER` | `${WIF_PROVIDER}` |
| `GCP_DEPLOY_SA` | `${DEPLOY_SA_EMAIL}` |
| `GCP_RUNTIME_SA` | `${RUNTIME_SA_EMAIL}` |
| `AR_REPO` | `${AR_REPO}` |
| `CLOUD_RUN_SERVICE` | `${CLOUD_RUN_SERVICE}` |
| `SPA_BUCKET` | `${SPA_BUCKET}` |
| `DOCS_BUCKET` | `${DOCS_BUCKET}` |
| `OMS_DOMAIN` | `${DOMAIN}` |
| `GOOGLE_CLIENT_ID` | GIS client ID |
| `DB_HOST` | Supabase host |
| `DB_PORT` | `5432` |
| `DB_NAME` | `postgres` |
| `DB_USERNAME` | Supabase user |

Do not store `DB_PASSWORD` / `JWT_SECRET` / bootstrap password in GitHub — they live in Secret Manager and are referenced by Cloud Run.

Recommend branch protection on `main`: require `CI Tests` to pass before merge; CD runs after merge / via `workflow_dispatch`.

---

## 11. Deploy via GitHub Actions

1. Merge to `main` or run **CD Deploy** → `workflow_dispatch`.
2. Confirm workflow green.
3. Open `https://${DOMAIN}` and run the smoke checklist below.

---

## 12. Operator smoke checklist (pre–Milestone 16)

| Check | Expected |
|-------|----------|
| `https://${DOMAIN}/` | Login page loads over HTTPS |
| Password login | Bootstrap or provisioned admin succeeds |
| `GET` via UI to dashboard | Data loads (same-origin `/api/v1/...`) |
| Create student + photo | Upload succeeds (private docs bucket) |
| Download document | Stream succeeds |
| Export one PDF | Download succeeds |
| Deep link refresh `/students/{id}` | SPA loads (not GCS XML 404) |
| Direct `*.run.app` URL | Should not be publicly usable (ingress restricted) |
| Cloud Logging | Cloud Run request / app logs visible |

Full production acceptance remains **Milestone 16**.

---

## 13. Rollback

| Layer | Action |
|-------|--------|
| API | `gcloud run deploy ... --image=${AR_HOST}/oms-api:PREVIOUS_SHA` or Console → Revisions → route traffic |
| SPA | Re-run CD on previous git SHA, or restore objects from bucket versioning (enable versioning if desired) |
| DB | Supabase PITR / backups per your plan (enable backups in Supabase dashboard) |

---

## 14. Cost notes

- Cloud Run scale-to-zero (`min-instances=0`)
- Supabase free/paid tier as chosen
- SPA + docs GCS: storage + egress
- Global LB + managed cert: fixed forwarding-rule / cert costs apply even at low traffic — review current Google Cloud pricing

---

## 15. What this runbook does not do

- Create a GCP billing account or payment method
- Purchase or register a domain
- Replace Milestone 16 validation
- Enable Cloud CDN (optional later)
