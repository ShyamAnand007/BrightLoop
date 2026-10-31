# Brightloop code review and fixes

## Fixed
- New subscription creation now resolves customer and plan IDs to real database records, checks plan availability/activity, applies the plan price when no override is supplied, validates quantity/tax/discount values, and supplies safe defaults for dates and status.
- The browser form now sends blank optional fields as `null` instead of empty strings (which broke date parsing) and blank optional numbers as `null` (which previously turned an unlimited discount into a zero-use discount).
- Customer, plan, discount, and payment requests receive additional server-side validation.
- Discount creation rejects invalid percentage values, invalid date ranges, negative minimum purchases, and invalid usage limits. Applying a discount checks eligibility and prevents applying a second discount to the same subscription.
- Subscription rows now provide a discount selector and Apply action.
- Payment creation validates the invoice, blocks cancelled invoices, checks positive amounts and outstanding balance, and updates invoice paid amount transactionally.
- Invoice discount/tax calculations are bounded and rounded to two decimal places.
- Dashboard MRR is now calculated from active subscriptions and billing intervals rather than showing a hard-coded figure.
- API exceptions now return consistent JSON error responses and appropriate 400/404/409 statuses.
- Staff creation is blocked until real admin authorization exists. README now warns that the remaining business APIs are demo-open and must not be publicly deployed as-is.
- Frontend JavaScript syntax and referenced static DOM IDs were checked; Maven project XML was parsed successfully.

## Verification limitation
The environment did not have Maven installed. An attempt to install it timed out, so a full Spring Boot compile/test run could not be completed here. Run `mvn test` in an environment with Maven 3.9+ and Java 21 before deployment.

## Security note
This remains a hackathon/demo starter, not a production-ready payment or subscription platform. All business APIs are still publicly accessible by design in the demo configuration. Implement authentication, role-based authorization, CSRF/session strategy, and concurrency-safe billing before exposing it to the internet.
