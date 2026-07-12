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

---

# Deployment Architecture

                Internet
                     │
                     ▼
        Google Cloud HTTPS Load Balancer
                     │
        ┌────────────┴────────────┐
        ▼                         ▼
Angular Application          Spring Boot API
(Google Cloud Storage)       (Cloud Run)
                                      │
                ┌─────────────────────┴─────────────────────┐
                ▼                                           ▼
      Supabase PostgreSQL                     Google Cloud Storage
       (Application Data)                     (Photos & Documents)

---

# Technology Stack

## Frontend

Angular

Hosting

Google Cloud Storage

Reason

- Very low cost
- CDN ready
- HTTPS support
- Easy deployment

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
- HTTPS Included
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

---

## Document Storage

Google Cloud Storage

Documents include

- Student Photo
- Aadhaar
- Birth Certificate
- Medical Reports
- Mark Sheets
- Other Documents

Database stores only metadata.

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

Supabase Dev Database

↓

Google Cloud Storage Test Bucket

---

Production

Angular

↓

Google Cloud Storage

↓

Cloud Run

↓

Supabase PostgreSQL

↓

Google Cloud Storage

---

# Environments

Development

Purpose

Local development

---

Testing

Purpose

Testing new features

---

Production

Purpose

Live system

---

# Environment Variables

Backend

```
SPRING_PROFILES_ACTIVE

DB_HOST

DB_PORT

DB_NAME

DB_USERNAME

DB_PASSWORD

JWT_SECRET

JWT_EXPIRATION

GOOGLE_CLIENT_ID

GOOGLE_CLIENT_SECRET

GCS_BUCKET_NAME
```

Never commit secrets to Git.

---

# Secret Management

Use

Google Secret Manager

Store

- Database Password
- JWT Secret
- Google OAuth Secret
- Storage Credentials

Never store secrets inside

application.yml

application.properties

Git Repository

---

# Build Pipeline

GitHub

↓

GitHub Actions

↓

Run Tests

↓

Build Angular

↓

Build Spring Boot

↓

Create Docker Image

↓

Push to Artifact Registry

↓

Deploy Cloud Run

---

# Angular Deployment

Generate Production Build

```
ng build --configuration production
```

Upload build artifacts to

Google Cloud Storage

Enable

Static Website Hosting

Configure

Cache Headers

HTTPS

---

# Spring Boot Deployment

Package

```
mvn clean package
```

Create Docker Image

```
docker build -t orphanage-student-management .
```

Deploy to

Cloud Run

---

# Database Deployment

Create PostgreSQL Database

Apply Flyway Migrations

Seed Initial Data

Verify Indexes

---

# Initial Seed Data

Roles

ADMIN

STAFF

Create

Initial Administrator Account

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

Daily Backup

Document Storage

Weekly Backup

Audit Logs

Never Delete

---

# Monitoring

Google Cloud Logging

Google Cloud Monitoring

Spring Boot Actuator

Health Endpoint

```
/actuator/health
```

Metrics

```
/actuator/prometheus
```

Future

Grafana

Prometheus

---

# Security

HTTPS Only

JWT Authentication

Google OAuth

BCrypt Passwords

Private Cloud Storage Bucket

Signed URLs

Input Validation

Output Validation

File Type Validation

Virus Scan (Future)

---

# Cost Optimization

Angular

Google Cloud Storage

Nearly Free

Backend

Cloud Run

Scale to Zero

Database

Supabase

Free Tier

Storage

Google Cloud Storage

Pay Per Usage

---

# Future Improvements

Custom Domain

Cloud CDN

Redis Cache

Multi-region Deployment

Disaster Recovery

Automated Backups

Performance Monitoring

Application Insights