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

The browser UI requires a sign-in. API requests use a server-side session and role-based authorization; do not use the seeded demo credentials in a public deployment.

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

## Production considerations
This is a runnable hackathon MVP, not a production payment processor. Before deployment, add verified password-reset flows, rate limiting, invoice PDF/email delivery, jurisdiction-aware tax rules, transaction locking for concurrent billing, pagination and indexes, and a payment gateway in sandbox mode. Use HTTPS and replace all seeded passwords.


## Login and role access

The app opens on an email/password sign-in screen. The server checks the session role for API access; navigation is role-specific, and customer invoices, payments, quotations and subscriptions are scoped to the signed-in customer.

Demo accounts (change these passwords before any real deployment):

| Role | Email | Password |
|---|---|---|
| Admin | admin@brightloop.demo | Admin@12345 |
| Sales | sales@brightloop.demo | Sales@12345 |
| Customer | customer@brightloop.demo | Customer@12345 |

Admin has access to the full workspace and staff accounts. Sales can work with customers, quotations, subscriptions, invoices and payments, and can select active products/plans and apply eligible discounts. Sales cannot manage staff or global product, plan or discount configuration. Customers can access only their own quotations, subscriptions, invoices, payments and profile. Customer records are linked to login email addresses.

Role workspaces are available at `/admin/dashboard`, `/sales/dashboard` and `/customer/dashboard`; accessing a route from another role redirects to that role's dashboard. The existing model treats quotations as subscriptions with `QUOTATION` status. Centralized tax-rule editing and deployment settings are not implemented in this starter; tax is currently stored on each subscription.

Run from the folder containing `pom.xml` with Java 21 and Maven 3.9+: `mvn spring-boot:run`. Open http://localhost:8080. The demo customer will initially have no subscription until an Admin or Sales user creates one for the same customer email. Run `mvn test` to verify role authorization and customer invoice isolation.

Security note: this is a development/demo starter. Use HTTPS, change seeded passwords, add password reset/rate limiting, and review deployment security before exposing it publicly.
