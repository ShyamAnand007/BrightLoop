package com.brightloop.controller;

import com.brightloop.model.AppUser;
import com.brightloop.model.Customer;
import com.brightloop.model.DiscountRule;
import com.brightloop.model.DiscountRule;
import com.brightloop.model.Invoice;
import com.brightloop.model.Payment;
import com.brightloop.model.Plan;
import com.brightloop.model.Product;
import com.brightloop.model.ProductVariant;
import com.brightloop.model.Subscription;
import com.brightloop.repository.AppUserRepository;
import com.brightloop.repository.CustomerRepository;
import com.brightloop.repository.DiscountRuleRepository;
import com.brightloop.repository.DiscountRuleRepository;
import com.brightloop.repository.InvoiceRepository;
import com.brightloop.repository.PaymentRepository;
import com.brightloop.repository.PlanRepository;
import com.brightloop.repository.ProductRepository;
import com.brightloop.repository.ProductVariantRepository;
import com.brightloop.repository.SubscriptionRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api")
public class RoleDataController {
    private final AppUserRepository users;
    private final CustomerRepository customers;
    private final ProductRepository products;
    private final ProductVariantRepository variants;
    private final DiscountRuleRepository discounts;
    private final PlanRepository plans;
    private final SubscriptionRepository subscriptions;
    private final InvoiceRepository invoices;
    private final PaymentRepository payments;

    public RoleDataController(
            AppUserRepository users,
            CustomerRepository customers,
            ProductRepository products,
            ProductVariantRepository variants,
            DiscountRuleRepository discounts,
            PlanRepository plans,
            SubscriptionRepository subscriptions,
            InvoiceRepository invoices,
            PaymentRepository payments) {
        this.users = users;
        this.customers = customers;
        this.products = products;
        this.variants = variants;
        this.discounts = discounts;
        this.plans = plans;
        this.subscriptions = subscriptions;
        this.invoices = invoices;
        this.payments = payments;
    }

    @GetMapping("/sales/dashboard")
    public Map<String, Object> salesDashboard() {
        LocalDate today = LocalDate.now();
        List<Invoice> allInvoices = invoices.findAll();
        List<Subscription> allSubscriptions = subscriptions.findAll();
        List<Payment> allPayments = payments.findAll();
        BigDecimal outstanding = allInvoices.stream()
                .filter(invoice -> invoice.status != Invoice.Status.CANCELLED)
                .map(invoice -> invoice.total.subtract(valueOrZero(invoice.paidAmount)).max(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long overdue = allInvoices.stream()
                .filter(invoice -> invoice.status != Invoice.Status.PAID
                        && invoice.status != Invoice.Status.CANCELLED
                        && invoice.dueDate != null
                        && invoice.dueDate.isBefore(today))
                .count();
        long upcoming = allSubscriptions.stream()
                .filter(subscription -> subscription.status == Subscription.Status.ACTIVE
                        && subscription.nextBillingDate != null
                        && !subscription.nextBillingDate.isBefore(today)
                        && !subscription.nextBillingDate.isAfter(today.plusDays(7)))
                .count();
        List<Subscription> quotations = allSubscriptions.stream()
                .filter(subscription -> subscription.status == Subscription.Status.QUOTATION)
                .toList();
        List<Subscription> recentQuotations = quotations.stream()
                .sorted(Comparator.comparing((Subscription subscription) -> subscription.startDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .toList();

        return Map.of(
                "totalCustomers", customers.count(),
                "activeSubscriptions", allSubscriptions.stream()
                        .filter(subscription -> subscription.status == Subscription.Status.ACTIVE).count(),
                "pendingQuotations", quotations.size(),
                "upcomingBilling", upcoming,
                "outstandingInvoices", outstanding,
                "overdueInvoices", overdue,
                "recentPayments", allPayments.stream().limit(5).toList(),
                "recentQuotations", recentQuotations);
    }

    @GetMapping("/sales/products")
    public List<Map<String, Object>> salesProducts() {
        return products.findAll().stream()
                .filter(product -> product.active)
                .map(product -> Map.<String, Object>of(
                        "id", product.id,
                        "name", product.name,
                        "type", product.type == null ? "" : product.type,
                        "salesPrice", valueOrZero(product.salesPrice),
                        "active", product.active))
                .toList();
    }

    @GetMapping("/sales/products/{id}/variants")
    public List<Map<String, Object>> salesProductVariants(@PathVariable Long id) {
        Product product = products.findById(id).orElseThrow(() -> new NoSuchElementException("Product not found"));
        return variants.findByProductIdAndProductActiveTrue(product.id).stream()
                .map(variant -> Map.<String, Object>of(
                        "id", variant.id,
                        "attributeName", variant.attributeName,
                        "attributeValue", variant.attributeValue,
                        "priceAdjustment", valueOrZero(variant.priceAdjustment)))
                .toList();
    }

    @GetMapping("/sales/plans")
    public List<Plan> salesPlans() {
        return plans.findAll().stream().filter(this::isAvailable).toList();
    }

    @GetMapping("/sales/discounts")
    public List<Map<String, Object>> salesDiscounts() {
        LocalDate today = LocalDate.now();
        return discounts.findAll().stream()
                .filter(discount -> discount.active
                        && (discount.startDate == null || !today.isBefore(discount.startDate))
                        && (discount.endDate == null || !today.isAfter(discount.endDate))
                        && (discount.usageLimit == null || discount.usedCount < discount.usageLimit))
                .map(discount -> Map.<String, Object>of(
                        "id", discount.id,
                        "name", discount.name == null ? "" : discount.name,
                        "kind", discount.kind.name(),
                        "value", valueOrZero(discount.value),
                        "endDate", discount.endDate == null ? "" : discount.endDate.toString(),
                        "usedCount", discount.usedCount,
                        "usageLimit", discount.usageLimit == null ? 0 : discount.usageLimit,
                        "active", discount.active))
                .toList();
    }

    @GetMapping("/customer/dashboard")
    public Map<String, Object> customerDashboard(HttpSession session) {
        String email = sessionEmail(session);
        List<Subscription> ownSubscriptions = subscriptions.findByCustomerEmail(email);
        List<Invoice> ownInvoices = invoices.findAll().stream()
                .filter(invoice -> owns(invoice, email))
                .toList();
        List<Payment> ownPayments = payments.findAll().stream()
                .filter(payment -> owns(payment.invoice, email))
                .toList();
        List<Subscription> ownQuotations = ownSubscriptions.stream()
                .filter(subscription -> subscription.status == Subscription.Status.QUOTATION)
                .toList();
        BigDecimal outstanding = ownInvoices.stream()
                .filter(invoice -> invoice.status != Invoice.Status.CANCELLED)
                .map(invoice -> invoice.total.subtract(valueOrZero(invoice.paidAmount)).max(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return Map.of(
                "activeSubscriptions", ownSubscriptions.stream()
                        .filter(subscription -> subscription.status == Subscription.Status.ACTIVE).toList(),
                "subscriptions", ownSubscriptions,
                "quotations", ownQuotations,
                "invoices", ownInvoices.stream().limit(5).toList(),
                "payments", ownPayments.stream().limit(5).toList(),
                "outstanding", outstanding);
    }

    @GetMapping("/customer/quotations")
    public List<Subscription> customerQuotations(HttpSession session) {
        return subscriptions.findByCustomerEmail(sessionEmail(session)).stream()
                .filter(subscription -> subscription.status == Subscription.Status.QUOTATION)
                .toList();
    }

    @GetMapping("/customer/invoices")
    public List<Invoice> customerInvoices(HttpSession session) {
        String email = sessionEmail(session);
        return invoices.findAll().stream().filter(invoice -> owns(invoice, email)).toList();
    }

    @GetMapping("/customer/invoices/{id}")
    public Invoice customerInvoice(@PathVariable Long id, HttpSession session) {
        String email = sessionEmail(session);
        Invoice invoice = invoices.findById(id).orElseThrow(() -> new NoSuchElementException("Invoice not found"));
        if (!owns(invoice, email)) {
            throw new NoSuchElementException("Invoice not found");
        }
        return invoice;
    }

    @GetMapping("/customer/payments")
    public List<Payment> customerPayments(HttpSession session) {
        String email = sessionEmail(session);
        return payments.findAll().stream()
                .filter(payment -> owns(payment.invoice, email))
                .toList();
    }

    @GetMapping("/customer/plans")
    public List<Plan> customerPlans() {
        return plans.findAll().stream().filter(this::isAvailable).toList();
    }

    @GetMapping("/customer/products")
    public List<Map<String, Object>> customerProducts() {
        return products.findAll().stream()
                .filter(product -> product.active)
                .map(product -> Map.<String, Object>of(
                        "id", product.id,
                        "name", product.name,
                        "type", product.type == null ? "" : product.type,
                        "salesPrice", valueOrZero(product.salesPrice),
                        "active", product.active))
                .toList();
    }

    @GetMapping("/customer/profile")
    public Map<String, Object> customerProfile(HttpSession session) {
        AppUser user = currentUser(session);
        Customer customer = customers.findByEmail(user.email).orElse(null);
        return Map.of(
                "id", user.id,
                "email", user.email,
                "fullName", user.fullName,
                "phone", customer == null || customer.phone == null ? "" : customer.phone,
                "billingAddress", customer == null || customer.billingAddress == null ? "" : customer.billingAddress);
    }

    @GetMapping("/profile")
    public Map<String, Object> profile(HttpSession session) {
        AppUser user = currentUser(session);
        return Map.of("id", user.id, "email", user.email, "fullName", user.fullName, "role", user.role.name());
    }

    @PutMapping("/profile")
    public Map<String, Object> updateProfile(@RequestBody Map<String, String> body, HttpSession session) {
        AppUser user = currentUser(session);
        String fullName = body.getOrDefault("fullName", user.fullName).trim();
        if (fullName.isBlank()) {
            throw new IllegalArgumentException("Full name is required");
        }
        user.fullName = fullName;
        users.save(user);
        session.setAttribute("USER_NAME", fullName);
        session.setAttribute("USER_NAME", fullName);
        return profile(session);
    }

    @PutMapping("/customer/profile")
    public Map<String, Object> updateCustomerProfile(@RequestBody Map<String, String> body, HttpSession session) {
        AppUser user = currentUser(session);
        String fullName = body.getOrDefault("fullName", user.fullName).trim();
        if (fullName.isBlank()) {
            throw new IllegalArgumentException("Full name is required");
        }
        user.fullName = fullName;
        users.save(user);

        Customer customer = customers.findByEmail(user.email).orElseGet(() -> {
            Customer created = new Customer();
            created.email = user.email;
            return created;
        });
        customer.name = fullName;
        if (body.containsKey("phone")) {
            customer.phone = body.get("phone");
        }
        if (body.containsKey("billingAddress")) {
            customer.billingAddress = body.get("billingAddress");
        }
        customers.save(customer);
        return customerProfile(session);
    }

    private AppUser currentUser(HttpSession session) {
        Object id = session.getAttribute("USER_ID");
        if (id == null) {
            throw new IllegalStateException("Please sign in");
        }
        return users.findById(Long.valueOf(id.toString()))
                .orElseThrow(() -> new NoSuchElementException("Account not found"));
    }

    private String sessionEmail(HttpSession session) {
        return currentUser(session).email.toLowerCase(Locale.ROOT);
    }

    private boolean owns(Invoice invoice, String email) {
        return invoice != null && invoice.customer != null && invoice.customer.email != null
                && invoice.customer.email.equalsIgnoreCase(email);
    }

    private boolean isAvailable(Plan plan) {
        LocalDate today = LocalDate.now();
        return plan.active
                && (plan.availableFrom == null || !today.isBefore(plan.availableFrom))
                && (plan.availableUntil == null || !today.isAfter(plan.availableUntil));
    }

    private BigDecimal valueOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
