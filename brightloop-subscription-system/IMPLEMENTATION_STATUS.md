# Brightloop role-based access update

This update applies the supplied Admin / Staff (shown in the UI as Sales Staff) / Customer requirements to the existing Spring Boot application.

## Implemented in this update

- Server-side API authorization in `RoleAccessConfig`; frontend hiding is not the security boundary.
- Unauthenticated API requests are rejected except customer signup, login, and CSRF-token bootstrap.
- Session-backed browser mutations use Spring Security CSRF tokens; session cookies are HttpOnly, SameSite=Lax, and have a 30-minute timeout.
- Admin retains full API access.
- Sales Staff can operate customers, subscriptions, invoices and payments; can read active products, available plans and eligible discounts; can apply permitted discounts.
- Sales Staff cannot manage users, edit product/plan configuration, create discount rules, or access Admin reports.
- Customer access is limited to own subscriptions, invoices and payments, plus the public active product/plan catalogue.
- Customer invoice/payment/subscription queries are scoped by the authenticated session email, not a customer ID supplied by the browser.
- Product cost price is omitted from non-admin product API responses.
- Non-admin users only see active products and plans within their availability dates. Staff only see currently usable discounts.
- Admin Users page can create Sales Staff accounts, edit their names, activate/deactivate them, and reset their passwords.
- Deactivated users are denied login and are also rechecked against the database on each protected API request.
- Customer dashboard now shows own subscription count, outstanding invoice balance, next billing date and payment count.
- Customer invoice and payment pages use dedicated customer-scoped endpoints.
- User API responses do not return password hashes.

## Role names

The application enum uses `ADMIN`, `SALES`, and `CUSTOMER`. `SALES` is the Staff / Sales Staff role in the supplied requirements and in the user interface.

## Existing demo accounts

- Admin: `admin@brightloop.demo` / `Admin@12345`
- Sales Staff: `sales@brightloop.demo` / `Sales@12345`
- Customer: `customer@brightloop.demo` / `Customer@12345`

Change demo passwords before any real deployment.

## Requirements still needing implementation

The supplied specification is broader than the existing MVP. These modules are not represented by complete backend models and workflows yet, so this update does not claim they are finished:

- Quotations and customer accept/reject workflow.
- Tax-rule management and assignment.
- Full product-variant lifecycle management and UI.
- Staff/customer profile editing screens.
- Invoice PDF/download and email delivery.
- Online customer payment gateway.
- Audit logs.
- Advanced date/product/plan/customer/financial report filters and yearly recurring revenue.
- Full recurring billing ledger with billing-period idempotency and retry/failure tracking.
- Complete lifecycle history and approval workflows.

The existing recurring billing scheduler and invoice/payment features remain part of the MVP, but should be tested with realistic data before production.

## Validation

- Frontend JavaScript syntax checked with Node.js `--check`.
- Maven build/tests were not run in this environment because Maven is not installed here. Run `mvn clean test` and `mvn spring-boot:run` locally.
