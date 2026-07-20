# OMS Frontend

Angular application for the Orphanage Management System (OMS).

## Stack

- Angular 22 (standalone, SCSS, strict mode, Zone.js)
- Tailwind CSS 4
- Lucide Angular (icons)
- AG Grid Community (tables — wired in later milestones)
- Apache ECharts + ngx-echarts (charts — wired in Milestone 11)
- Angular CDK
- ngx-toastr
- Jasmine + Karma (unit tests)

## Prerequisites

- Node.js 20+ LTS (Node 24 verified)
- npm 10+

## Setup

```powershell
cd frontend
npm install
```

`.npmrc` sets `legacy-peer-deps=true` because `lucide-angular` and `ngx-toastr` peer ranges currently declare Angular ≤21 while this app uses Angular 22.

## Run (development)

```powershell
npm start
```

Opens the dev server with `proxy.conf.json` forwarding `/api` → `http://localhost:8080` (Spring Boot).

App URL: [http://localhost:4200](http://localhost:4200)

## Build

```powershell
npm run build
```

Production output: `dist/oms-frontend/`

## Test

```powershell
npm run test:ci
```

## Environments

| File | Purpose |
|------|---------|
| `src/environments/environment.ts` | Local/dev — `apiBaseUrl: '/api/v1'` (proxy) |
| `src/environments/environment.prod.ts` | Production — same-origin `apiBaseUrl: '/api/v1'`; CD injects `googleClientId` |

Production hosting uses a Global HTTPS Load Balancer (custom domain). See [docs/14_Production_Runbook.md](../docs/14_Production_Runbook.md).

## Folder structure

```text
src/app/
├── core/          # guards, interceptors, services, models, enums, constants
├── shared/        # reusable UI (button, empty-state, page-header)
├── layout/        # sidebar, topbar, footer, main shell
└── features/      # auth, dashboard, student, report, user (lazy routes)
```

## Notes

- Authentication UI and JWT are stubs for Milestone 2.
- Feature pages are placeholders only (Milestone 1 foundation).
- Dark mode is CSS-ready (`ThemeService` + `dark` class); polish in Milestone 13.
