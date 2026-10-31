# Brightloop Subscription Management System

A full-stack Java/Spring Boot starter implementation of the Subscription Management System hackathon brief.

## Included
- Browser dashboard with overview, subscriptions, customers, products/plans, invoices, payments, discounts and reports
- Java 21 + Spring Boot REST backend
- H2 file database by default (data persists in `./data`); MySQL supported by environment configuration
- Signup password validation and BCrypt password hashing
- Admin/staff/customer role model; demo admin and staff-creation endpoint
- Product and variant model (variant price adjustments)
- Daily/weekly/monthly/yearly plans and subscription lifecycle statuses
- Invoice generation with tax and discount calculations, duplicate-per-day guard
- Scheduled recurring billing at 02:00 server time for active subscriptions due that day
- Payment recording and invoice balance reconciliation
- Discount eligibility checks (date, usage cap, minimum purchase and quantity), percentage caps and duplicate-application protection
- Server-side validation for subscription creation, payment recording, discounts and plan availability
- Consistent JSON error responses for invalid, missing and conflicting records
- Dashboard MRR calculated from active subscriptions and their billing intervals
- Dashboard summary and overdue invoice endpoints
- Demo seed data

## Run
Requirements: Java 21 and Maven 3.9+.

```bash
mvn spring-boot:run
```
Open http://localhost:8080

Demo admin (seeded):
- Email: `admin@brightloop.demo`
- Password: `Admin@12345`

The UI currently runs in demo mode without a login screen; authentication endpoints are available for integration. The staff-creation endpoint is disabled at the security layer because this starter does not yet implement authenticated admin authorization. Other business APIs are intentionally open for local demonstration only; do not expose this application to the public internet until authentication and role-based authorization are implemented for every business endpoint.

## MySQL
Set environment variables before starting:

```bash
DB_URL=jdbc:mysql://localhost:3306/brightloop DB_USER=brightloop DB_PASSWORD=yourpassword mvn spring-boot:run
```
Create the `brightloop` database first. JPA creates/updates tables automatically.

## Main API
- `POST /api/auth/signup` `{fullName,email,password}`
- `POST /api/auth/login` `{email,password}`
- `POST /api/auth/staff` `{fullName,email,password}` (intended for admin-only use)
- CRUD create/list: `/api/products`, `/api/plans`, `/api/customers`, `/api/subscriptions`, `/api/discounts`
- `POST /api/products/{id}/variants`
- `PUT /api/subscriptions/{id}/status` `{status}`
- `POST /api/subscriptions/{id}/renew`
- `POST /api/invoices/generate/{subscriptionId}`
- `POST /api/invoices/{id}/confirm` and `/cancel`
- `POST /api/payments` (invoice object `{id}`, amount, method, paymentDate, note)
- `POST /api/subscriptions/{id}/apply-discount/{discountId}`
- `GET /api/dashboard/summary`, `GET /api/dashboard/overdue`

## Important hackathon hardening before production
This is a runnable hackathon MVP, not a production payment processor. Before deployment, enforce authorization with authenticated sessions/JWT on every admin/staff/customer route, implement verified email reset tokens, add invoice PDF/email delivery and tax jurisdiction rules, use database transactions/locking for concurrent billing, add pagination/indexes and tests, and connect a payment gateway in sandbox mode. Staff/admin creation endpoint should be restricted to admin after auth is enabled.


## Login and role dashboards (updated)

The app now opens on an email/password sign-in screen. Authentication uses a server-side HTTP session; protected API routes check the session role.

Demo accounts (change these passwords before any real deployment):

| Role | Email | Password |
|---|---|---|
| Admin | admin@brightloop.demo | Admin@12345 |
| Sales | sales@brightloop.demo | Sales@12345 |
| Customer | customer@brightloop.demo | Customer@12345 |

Admin can access all workspace sections and the Users page (create users and assign roles). Sales can access the sales workspace (subscriptions, customers, invoices, payments and discounts). Customers can see their own subscriptions only. Customer records are linked to login email addresses.

Run from the folder containing `pom.xml` with Java 21 and Maven 3.9+: `mvn clean` then `mvn spring-boot:run`. Open http://localhost:8080. The demo customer will initially have no subscription until an Admin or Sales user creates one for the same customer email.

Security note: this is a development/demo starter. Use HTTPS, change seeded passwords, add password reset/rate limiting, and review deployment security before exposing it publicly.
