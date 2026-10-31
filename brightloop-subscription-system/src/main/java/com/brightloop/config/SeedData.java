package com.brightloop.config;

import com.brightloop.model.*;
import com.brightloop.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Configuration
public class SeedData {
    @Bean
    CommandLineRunner seed(AppUserRepository users,
                           ProductRepository products,
                           PlanRepository plans,
                           CustomerRepository customers,
                           SubscriptionRepository subscriptions,
                           ProductVariantRepository variants,
                           DiscountRuleRepository discounts,
                           InvoiceRepository invoices,
                           PaymentRepository payments,
                           PasswordEncoder encoder) {
        return args -> {
            seedUser(users, encoder, "admin@brightloop.demo", "Brightloop Admin", AppUser.Role.ADMIN, "Admin@12345");
            seedUser(users, encoder, "sales@brightloop.demo", "Sales Executive", AppUser.Role.SALES, "Sales@12345");

            // Customer demo users are linked to customer records by matching email.
            seedUser(users, encoder, "customer@brightloop.demo", "Aarav Mehta", AppUser.Role.CUSTOMER, "Customer@12345");
            seedUser(users, encoder, "priya@brightloop.demo", "Priya Nair", AppUser.Role.CUSTOMER, "Customer@12345");
            seedUser(users, encoder, "rohan@brightloop.demo", "Rohan Iyer", AppUser.Role.CUSTOMER, "Customer@12345");
            seedUser(users, encoder, "sana@brightloop.demo", "Sana Thomas", AppUser.Role.CUSTOMER, "Customer@12345");

            Customer acme = customer(customers, "billing@acme.demo", "Acme Small Business", "Acme Small Business", "+91 98765 10001", "Kochi, Kerala");
            Customer aarav = customer(customers, "customer@brightloop.demo", "Aarav Mehta", "Aarav Mehta", "+91 98765 10002", "Thrissur, Kerala");
            Customer priya = customer(customers, "priya@brightloop.demo", "Priya Nair", "Nair Design Studio", "+91 98765 10003", "Kochi, Kerala");
            Customer rohan = customer(customers, "rohan@brightloop.demo", "Rohan Iyer", "Iyer Technologies", "+91 98765 10004", "Bengaluru, Karnataka");
            Customer sana = customer(customers, "sana@brightloop.demo", "Sana Thomas", "Sana Consulting", "+91 98765 10005", "Alappuzha, Kerala");

            Product workspace = product(products, "Brightloop Workspace", "Software", "49.00", "Core team workspace, projects and collaboration tools", "12.00");
            Product crm = product(products, "Brightloop CRM", "Software", "79.00", "Customer relationship and sales pipeline management", "21.00");
            Product analytics = product(products, "Brightloop Analytics", "Software", "99.00", "Dashboards, reporting and business insights", "28.00");
            Product support = product(products, "Priority Support", "Service", "29.00", "Priority email and chat support add-on", "8.00");

            variant(variants, workspace, "Seats", "5 users", "0.00");
            variant(variants, workspace, "Seats", "10 users", "25.00");
            variant(variants, workspace, "Storage", "1 TB", "15.00");
            variant(variants, crm, "Contacts", "5,000 contacts", "0.00");
            variant(variants, crm, "Contacts", "20,000 contacts", "35.00");
            variant(variants, analytics, "Reports", "Standard", "0.00");
            variant(variants, analytics, "Reports", "Advanced", "40.00");

            plan(plans, "Starter Monthly", Plan.Interval.MONTHLY, "29.00");
            plan(plans, "Professional Monthly", Plan.Interval.MONTHLY, "49.00");
            plan(plans, "Business Monthly", Plan.Interval.MONTHLY, "149.00");
            plan(plans, "Professional Annual", Plan.Interval.YEARLY, "499.00");
            plan(plans, "Business Annual", Plan.Interval.YEARLY, "1499.00");
            plan(plans, "Weekly Trial Plan", Plan.Interval.WEEKLY, "9.00");

            discount(discounts, "WELCOME10", DiscountRule.Kind.PERCENTAGE, "10.00", 1, 100);
            discount(discounts, "ANNUAL50", DiscountRule.Kind.FIXED, "50.00", 1, 50);
            discount(discounts, "SPRING15", DiscountRule.Kind.PERCENTAGE, "15.00", 1, 200);

            Plan professional = findPlan(plans, "Professional Monthly");
            Plan business = findPlan(plans, "Business Monthly");
            Plan annual = findPlan(plans, "Professional Annual");
            LocalDate today = LocalDate.now();
            Subscription s1 = subscription(subscriptions, "SUB-2026-001", aarav, professional, today.minusMonths(2), today.plusMonths(10), Subscription.Status.ACTIVE, 1, "49.00");
            Subscription s2 = subscription(subscriptions, "SUB-2026-002", priya, business, today.minusMonths(1), today.plusMonths(11), Subscription.Status.ACTIVE, 1, "149.00");
            Subscription s3 = subscription(subscriptions, "SUB-2026-003", rohan, annual, today.minusMonths(3), today.plusMonths(9), Subscription.Status.ACTIVE, 1, "499.00");
            subscription(subscriptions, "SUB-2026-004", sana, professional, today.minusDays(10), today.plusMonths(11), Subscription.Status.CONFIRMED, 2, "49.00");
            subscription(subscriptions, "SUB-2026-005", acme, business, today.minusMonths(4), today.plusMonths(8), Subscription.Status.PAUSED, 1, "149.00");

            Invoice i1 = invoice(invoices, "INV-2026-001", s1, aarav, today.minusDays(20), "49.00", "8.82", "0.00", "57.82", "Paid - Professional Monthly");
            invoice(invoices, "INV-2026-002", s2, priya, today.minusDays(12), "149.00", "26.82", "15.00", "160.82", "Part payment - Business Monthly");
            invoice(invoices, "INV-2026-003", s3, rohan, today.minusDays(5), "499.00", "89.82", "50.00", "538.82", "Annual subscription");
            if (payments.count() == 0) {
                payment(payments, invoices, i1, "57.82", today.minusDays(18), "UPI", "DEMO-UPI-1001");
                Invoice i2 = invoices.findAll().stream().filter(x -> "INV-2026-002".equals(x.number)).findFirst().orElse(null);
                if (i2 != null) payment(payments, invoices, i2, "80.00", today.minusDays(10), "Bank Transfer", "DEMO-NEFT-1002");
                Invoice i3 = invoices.findAll().stream().filter(x -> "INV-2026-003".equals(x.number)).findFirst().orElse(null);
                if (i3 != null) payment(payments, invoices, i3, "200.00", today.minusDays(3), "Card", "DEMO-CARD-1003");
            }
        };
    }

    private static void seedUser(AppUserRepository repo, PasswordEncoder encoder, String email, String name, AppUser.Role role, String password) {
        if (!repo.existsByEmail(email)) {
            AppUser u = new AppUser(); u.email = email; u.fullName = name;
            u.passwordHash = encoder.encode(password); u.role = role; u.active = true; repo.save(u);
        }
    }

    private static Customer customer(CustomerRepository repo, String email, String name, String company, String phone, String address) {
        return repo.findByEmail(email).orElseGet(() -> {
            Customer c = new Customer(); c.email = email; c.name = name; c.company = company;
            c.phone = phone; c.billingAddress = address; return repo.save(c);
        });
    }

    private static Product product(ProductRepository repo, String name, String type, String price, String notes, String cost) {
        return repo.findAll().stream().filter(p -> name.equals(p.name)).findFirst().orElseGet(() -> {
            Product p = new Product(); p.name = name; p.type = type; p.salesPrice = money(price);
            p.costPrice = money(cost); p.notes = notes; p.active = true; return repo.save(p);
        });
    }

    private static void variant(ProductVariantRepository repo, Product product, String attribute, String value, String adjustment) {
        boolean exists = repo.findAll().stream().anyMatch(v -> v.product.id.equals(product.id) && attribute.equals(v.attributeName) && value.equals(v.attributeValue));
        if (!exists) { ProductVariant v = new ProductVariant(); v.product = product; v.attributeName = attribute; v.attributeValue = value; v.priceAdjustment = money(adjustment); repo.save(v); }
    }

    private static void plan(PlanRepository repo, String name, Plan.Interval interval, String price) {
        if (repo.findAll().stream().noneMatch(p -> name.equals(p.name))) {
            Plan p = new Plan(); p.name = name; p.interval = interval; p.price = money(price);
            p.minimumQuantity = 1; p.availableFrom = LocalDate.now().minusYears(1); p.active = true; repo.save(p);
        }
    }

    private static Plan findPlan(PlanRepository repo, String name) {
        return repo.findAll().stream().filter(p -> name.equals(p.name)).findFirst().orElseThrow();
    }

    private static void discount(DiscountRuleRepository repo, String name, DiscountRule.Kind kind, String value, int minQty, int limit) {
        if (repo.findAll().stream().noneMatch(d -> name.equals(d.name))) {
            DiscountRule d = new DiscountRule(); d.name = name; d.kind = kind; d.value = money(value);
            d.minimumPurchase = money("0.00"); d.minimumQuantity = minQty; d.startDate = LocalDate.now().minusMonths(1);
            d.endDate = LocalDate.now().plusMonths(6); d.usageLimit = limit; d.active = true; repo.save(d);
        }
    }

    private static Subscription subscription(SubscriptionRepository repo, String number, Customer customer, Plan plan,
                                             LocalDate start, LocalDate expiry, Subscription.Status status, int quantity, String unitPrice) {
        return repo.findAll().stream().filter(s -> number.equals(s.number)).findFirst().orElseGet(() -> {
            Subscription s = new Subscription(); s.number = number; s.customer = customer; s.plan = plan;
            s.startDate = start; s.expiryDate = expiry; s.status = status; s.quantity = quantity;
            s.unitPrice = money(unitPrice); s.taxRate = money("0.18"); s.discountAmount = money("0.00");
            s.paymentTerms = "Due within 15 days"; s.nextBillingDate = LocalDate.now().plusDays(10); return repo.save(s);
        });
    }

    private static Invoice invoice(InvoiceRepository repo, String number, Subscription subscription, Customer customer,
                                   LocalDate date, String subtotal, String tax, String discount, String total, String description) {
        return repo.findAll().stream().filter(i -> number.equals(i.number)).findFirst().orElseGet(() -> {
            Invoice i = new Invoice(); i.number = number; i.subscription = subscription; i.customer = customer;
            i.invoiceDate = date; i.dueDate = date.plusDays(15); i.subtotal = money(subtotal); i.tax = money(tax);
            i.discount = money(discount); i.total = money(total); i.paidAmount = money("0.00");
            i.status = Invoice.Status.CONFIRMED; i.description = description; return repo.save(i);
        });
    }

    private static void payment(PaymentRepository repo, InvoiceRepository invoiceRepo, Invoice invoice, String amount, LocalDate date, String method, String note) {
        Payment p = new Payment(); p.invoice = invoice; p.amount = money(amount); p.paymentDate = date; p.method = method; p.note = note;
        repo.save(p); invoice.paidAmount = invoice.paidAmount.add(p.amount);
        if (invoice.paidAmount.compareTo(invoice.total) >= 0) invoice.status = Invoice.Status.PAID;
        invoiceRepo.save(invoice);
    }

    private static BigDecimal money(String value) { return new BigDecimal(value); }
}
