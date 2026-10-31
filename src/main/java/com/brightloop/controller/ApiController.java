package com.brightloop.controller;

import com.brightloop.model.*;
import com.brightloop.repository.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class ApiController {
    private final AppUserRepository users;
    private final ProductRepository products;
    private final ProductVariantRepository variants;
    private final PlanRepository plans;
    private final CustomerRepository customers;
    private final SubscriptionRepository subscriptions;
    private final InvoiceRepository invoices;
    private final PaymentRepository payments;
    private final DiscountRuleRepository discounts;
    private final PasswordResetTokenRepository passwordResetTokens;
    private final PasswordEncoder encoder;

    public ApiController(AppUserRepository u, ProductRepository p, ProductVariantRepository v, PlanRepository pl,
                         CustomerRepository c, SubscriptionRepository s, InvoiceRepository i, PaymentRepository pay,
                         DiscountRuleRepository d, PasswordResetTokenRepository passwordResetTokens, PasswordEncoder e) {
        users = u;
        products = p;
        variants = v;
        plans = pl;
        customers = c;
        subscriptions = s;
        invoices = i;
        payments = pay;
        discounts = d;
        this.passwordResetTokens = passwordResetTokens;
        encoder = e;
    }

    @PostMapping("/auth/signup")
    public ResponseEntity<?> signup(@RequestBody Map<String, String> body) {
        String email = body.getOrDefault("email", "").trim().toLowerCase(Locale.ROOT);
        String password = body.getOrDefault("password", "");
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Valid email required"));
        }
        if (password.length() <= 8 || !password.matches(".*[A-Z].*") || !password.matches(".*[a-z].*") || !password.matches(".*[^A-Za-z0-9].*")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password must be >8 chars and include uppercase, lowercase and special character"));
        }
        if (users.existsByEmail(email)) {
            return ResponseEntity.status(409).body(Map.of("error", "Email already registered"));
        }
        AppUser user = new AppUser();
        user.email = email;
        user.fullName = body.getOrDefault("fullName", "Customer");
        user.passwordHash = encoder.encode(password);
        user.role = AppUser.Role.CUSTOMER;
        users.save(user);
        if (!customers.existsByEmail(email)) {
            Customer customer = new Customer();
            customer.name = user.fullName;
            customer.email = email;
            customers.save(customer);
        }
        return ResponseEntity.ok(Map.of("id", user.id, "message", "Account created"));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body, HttpSession session) {
        String email = body.getOrDefault("email", " ").trim().toLowerCase(Locale.ROOT);
        String password = body.getOrDefault("password", "");
        return users.findByEmail(email)
                .filter(user -> encoder.matches(password, user.passwordHash))
                .map(user -> {
                    session.setAttribute("USER_ID", user.id);
                    session.setAttribute("USER_EMAIL", user.email);
                    session.setAttribute("USER_NAME", user.fullName);
                    session.setAttribute("USER_ROLE", user.role.name());
                    return ResponseEntity.ok(Map.of("id", user.id, "email", user.email, "name", user.fullName, "role", user.role));
                })
                .orElseGet(() -> ResponseEntity.status(401).body(Map.of("error", "Invalid email or password")));
    }

    @GetMapping("/auth/me")
    public ResponseEntity<?> me(HttpSession session) {
        Object id = session.getAttribute("USER_ID");
        if (id == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not signed in"));
        }
        return ResponseEntity.ok(Map.of(
                "id", id,
                "email", session.getAttribute("USER_EMAIL"),
                "name", session.getAttribute("USER_NAME"),
                "role", session.getAttribute("USER_ROLE")
        ));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("message", "Signed out"));
    }

    @PostMapping("/auth/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.getOrDefault("email", "").trim().toLowerCase(Locale.ROOT);
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Valid email required"));
        }

        Optional<AppUser> userOpt = users.findByEmail(email);
        if (userOpt.isPresent()) {
            String token = UUID.randomUUID().toString().replace("-", "");
            AppUser user = userOpt.get();
            PasswordResetToken reset = new PasswordResetToken();
            reset.user = user;
            reset.token = token;
            reset.expiresAt = LocalDateTime.now().plusHours(1);
            passwordResetTokens.save(reset);
            return ResponseEntity.ok(Map.of(
                    "message", "If the account exists, a reset link has been sent.",
                    "resetToken", token,
                    "resetUrl", "/reset-password?token=" + token
            ));
        }

        return ResponseEntity.ok(Map.of("message", "If the account exists, a reset link has been sent."));
    }

    @PostMapping("/auth/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        String token = body.getOrDefault("token", "").trim();
        String password = body.getOrDefault("password", "");
        if (token.isBlank() || password.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Token and new password are required"));
        }
        PasswordResetToken reset = passwordResetTokens.findByToken(token).orElse(null);
        if (reset == null || reset.isExpired() || !reset.isUsable()) {
            return ResponseEntity.status(400).body(Map.of("error", "Invalid or expired reset token"));
        }
        if (password.length() <= 8 || !password.matches(".*[A-Z].*") || !password.matches(".*[a-z].*") || !password.matches(".*[^A-Za-z0-9].*")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password must be >8 chars and include uppercase, lowercase and special character"));
        }
        reset.user.passwordHash = encoder.encode(password);
        reset.usedAt = LocalDateTime.now();
        users.save(reset.user);
        passwordResetTokens.save(reset);
        return ResponseEntity.ok(Map.of("message", "Password reset successful"));
    }

    @GetMapping("/customer/subscriptions")
    public List<Subscription> mySubscriptions(HttpSession session) {
        String email = (String) session.getAttribute("USER_EMAIL");
        return subscriptions.findByCustomerEmail(email);
    }

    @GetMapping("/users")
    public List<Map<String, Object>> listUsers(HttpSession session) {
        requireRole(session, AppUser.Role.ADMIN);
        return users.findAll().stream().map(u -> Map.<String, Object>of("id", u.id, "email", u.email, "name", u.fullName, "role", u.role)).toList();
    }

    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody Map<String, String> body, HttpSession session) {
        requireRole(session, AppUser.Role.ADMIN);
        String email = body.getOrDefault("email", "").trim().toLowerCase(Locale.ROOT);
        String password = body.getOrDefault("password", "");
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Valid email required"));
        }
        if (password.length() < 9) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 9 characters"));
        }
        if (users.existsByEmail(email)) {
            return ResponseEntity.status(409).body(Map.of("error", "Email already registered"));
        }
        AppUser user = new AppUser();
        user.email = email;
        user.fullName = body.getOrDefault("fullName", "New user");
        user.passwordHash = encoder.encode(password);
        try {
            user.role = AppUser.Role.valueOf(body.getOrDefault("role", "CUSTOMER").toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", "Role must be ADMIN, SALES or CUSTOMER"));
        }
        users.save(user);
        if (user.role == AppUser.Role.CUSTOMER && !customers.existsByEmail(email)) {
            Customer customer = new Customer();
            customer.name = user.fullName;
            customer.email = email;
            customers.save(customer);
        }
        return ResponseEntity.ok(Map.of("id", user.id, "email", user.email, "name", user.fullName, "role", user.role));
    }

    @PostMapping("/auth/staff")
    public ResponseEntity<?> addStaff(@RequestBody Map<String, String> body, HttpSession session) {
        requireRole(session, AppUser.Role.ADMIN);
        String email = body.getOrDefault("email", "").trim().toLowerCase(Locale.ROOT);
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Valid email required"));
        }
        if (users.existsByEmail(email)) {
            return ResponseEntity.status(409).body(Map.of("error", "Email exists"));
        }
        String password = body.getOrDefault("password", "Staff@12345");
        if (password.length() < 9) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 9 characters"));
        }
        AppUser user = new AppUser();
        user.email = email;
        user.fullName = body.getOrDefault("fullName", "Staff");
        user.passwordHash = encoder.encode(password);
        user.role = AppUser.Role.SALES;
        return ResponseEntity.ok(users.save(user));
    }

    @GetMapping("/products")
    public List<Product> products(HttpSession session) {
        requireAnyRole(session, AppUser.Role.ADMIN, AppUser.Role.SALES, AppUser.Role.CUSTOMER);
        return products.findAll();
    }

    @PostMapping("/products")
    public Product createProduct(@RequestBody Product product, HttpSession session) {
        requireRole(session, AppUser.Role.ADMIN);
        return products.save(product);
    }

    @PostMapping("/products/{id}/variants")
    public ProductVariant variant(@PathVariable Long id, @RequestBody ProductVariant variant, HttpSession session) {
        requireAnyRole(session, AppUser.Role.ADMIN, AppUser.Role.SALES);
        variant.product = products.findById(id).orElseThrow();
        return variants.save(variant);
    }

    @GetMapping("/plans")
    public List<Plan> plans(HttpSession session) {
        requireAnyRole(session, AppUser.Role.ADMIN, AppUser.Role.SALES, AppUser.Role.CUSTOMER);
        return plans.findAll();
    }

    @PostMapping("/plans")
    public Plan createPlan(@RequestBody Plan plan, HttpSession session) {
        requireRole(session, AppUser.Role.ADMIN);
        if (plan.name == null || plan.name.isBlank()) {
            throw new IllegalArgumentException("Plan name is required");
        }
        if (plan.interval == null) {
            plan.interval = Plan.Interval.MONTHLY;
        }
        if (plan.price == null || plan.price.signum() < 0) {
            throw new IllegalArgumentException("Plan price cannot be negative");
        }
        if (plan.minimumQuantity < 1) {
            throw new IllegalArgumentException("Minimum quantity must be at least 1");
        }
        if (plan.availableFrom != null && plan.availableUntil != null && plan.availableUntil.isBefore(plan.availableFrom)) {
            throw new IllegalArgumentException("Plan end date cannot be before start date");
        }
        return plans.save(plan);
    }

    @GetMapping("/customers")
    public List<Customer> customers(HttpSession session) {
        requireAnyRole(session, AppUser.Role.ADMIN, AppUser.Role.SALES);
        return customers.findAll();
    }

    @PostMapping("/customers")
    public Customer createCustomer(@RequestBody Customer customer, HttpSession session) {
        requireAnyRole(session, AppUser.Role.ADMIN, AppUser.Role.SALES);
        if (customer.name == null || customer.name.isBlank()) {
            throw new IllegalArgumentException("Customer name is required");
        }
        if (customer.email == null || !customer.email.trim().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("Valid customer email is required");
        }
        customer.email = customer.email.trim().toLowerCase(Locale.ROOT);
        return customers.save(customer);
    }

    @GetMapping("/subscriptions")
    public List<Subscription> subscriptions(HttpSession session) {
        requireAnyRole(session, AppUser.Role.ADMIN, AppUser.Role.SALES);
        return subscriptions.findAll();
    }

    @PostMapping("/subscriptions")
    @Transactional
    public Subscription createSubscription(@RequestBody Subscription subscription, HttpSession session) {
        requireAnyRole(session, AppUser.Role.ADMIN, AppUser.Role.SALES);
        if (subscription.customer == null || subscription.customer.id == null || subscription.plan == null || subscription.plan.id == null) {
            throw new IllegalArgumentException("Select a valid customer and plan");
        }
        subscription.customer = customers.findById(subscription.customer.id).orElseThrow(() -> new IllegalArgumentException("Customer not found"));
        subscription.plan = plans.findById(subscription.plan.id).orElseThrow(() -> new IllegalArgumentException("Plan not found"));
        if (!subscription.plan.active) {
            throw new IllegalArgumentException("Selected plan is inactive");
        }
        LocalDate today = LocalDate.now();
        if (subscription.plan.availableFrom != null && today.isBefore(subscription.plan.availableFrom)) {
            throw new IllegalArgumentException("Plan is not available yet");
        }
        if (subscription.plan.availableUntil != null && today.isAfter(subscription.plan.availableUntil)) {
            throw new IllegalArgumentException("Plan is no longer available");
        }
        if (subscription.number == null || subscription.number.isBlank()) {
            subscription.number = "SUB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        }
        if (subscription.quantity < Math.max(1, subscription.plan.minimumQuantity)) {
            throw new IllegalArgumentException("Quantity is below the plan minimum of " + subscription.plan.minimumQuantity);
        }
        if ("SALES".equals(session.getAttribute("USER_ROLE"))) {
            subscription.unitPrice = subscription.plan.price;
            subscription.taxRate = new BigDecimal("0.18");
            subscription.discountAmount = BigDecimal.ZERO;
        }
        if (subscription.unitPrice == null || subscription.unitPrice.signum() == 0) {
            subscription.unitPrice = subscription.plan.price;
        }
        if (subscription.unitPrice == null || subscription.unitPrice.signum() < 0) {
            throw new IllegalArgumentException("Unit price cannot be negative");
        }
        if (subscription.taxRate == null) {
            subscription.taxRate = new BigDecimal("0.18");
        }
        if (subscription.taxRate.signum() < 0 || subscription.taxRate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Tax rate must be between 0 and 1 (for example, 0.18 for 18%)");
        }
        if (subscription.discountAmount == null) {
            subscription.discountAmount = BigDecimal.ZERO;
        }
        if (subscription.discountAmount.signum() < 0) {
            throw new IllegalArgumentException("Discount cannot be negative");
        }
        if (subscription.startDate == null) {
            subscription.startDate = today;
        }
        if (subscription.nextBillingDate == null) {
            subscription.nextBillingDate = subscription.startDate;
        }
        if (subscription.status == null) {
            subscription.status = Subscription.Status.ACTIVE;
        }
        return subscriptions.save(subscription);
    }

    @PutMapping("/subscriptions/{id}/status")
    public Subscription status(@PathVariable Long id, @RequestBody Map<String, String> body, HttpSession session) {
        requireAnyRole(session, AppUser.Role.ADMIN, AppUser.Role.SALES);
        Subscription subscription = subscriptions.findById(id).orElseThrow();
        String requested = body.get("status");
        if (requested == null || requested.isBlank()) {
            throw new IllegalArgumentException("Subscription status is required");
        }
        try {
            subscription.status = Subscription.Status.valueOf(requested.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid subscription status");
        }
        if (subscription.status == Subscription.Status.PAUSED && !subscription.plan.allowPause) {
            throw new IllegalStateException("Pausing is disabled for this plan");
        }
        if (subscription.status == Subscription.Status.CLOSED && !subscription.plan.allowEarlyClose) {
            throw new IllegalStateException("Early closure is disabled for this plan");
        }
        if (subscription.status == Subscription.Status.CLOSED) {
            subscription.expiryDate = LocalDate.now();
        }
        return subscriptions.save(subscription);
    }

    @PostMapping("/subscriptions/{id}/renew")
    public Subscription renew(@PathVariable Long id, HttpSession session) {
        requireAnyRole(session, AppUser.Role.ADMIN, AppUser.Role.SALES);
        Subscription subscription = subscriptions.findById(id).orElseThrow();
        if (!subscription.plan.allowRenew) {
            throw new IllegalStateException("Renewal is disabled for this plan");
        }
        if (subscription.status != Subscription.Status.CLOSED) {
            throw new IllegalStateException("Only closed subscriptions can be renewed");
        }
        subscription.status = Subscription.Status.ACTIVE;
        subscription.startDate = subscription.expiryDate == null ? LocalDate.now() : subscription.expiryDate.plusDays(1);
        subscription.nextBillingDate = subscription.startDate;
        subscription.expiryDate = null;
        return subscriptions.save(subscription);
    }

    @GetMapping("/invoices")
    public List<Invoice> invoices(HttpSession session) {
        requireAnyRole(session, AppUser.Role.ADMIN, AppUser.Role.SALES);
        return invoices.findAll();
    }

    @PostMapping("/invoices/generate/{subscriptionId}")
    @Transactional
    public Invoice generate(@PathVariable Long subscriptionId, HttpSession session) {
        requireAnyRole(session, AppUser.Role.ADMIN, AppUser.Role.SALES);
        Subscription subscription = subscriptions.findById(subscriptionId).orElseThrow();
        LocalDate date = LocalDate.now();
        if (invoices.existsBySubscriptionIdAndInvoiceDate(subscription.id, date)) {
            throw new IllegalStateException("Invoice already generated for this subscription today");
        }
        return makeInvoice(subscription, date);
    }

    public Invoice generate(Long subscriptionId) {
        return generate(subscriptionId, null);
    }

    private Invoice makeInvoice(Subscription subscription, LocalDate date) {
        Invoice invoice = new Invoice();
        invoice.number = "INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        invoice.subscription = subscription;
        invoice.customer = subscription.customer;
        invoice.invoiceDate = date;
        invoice.dueDate = date.plusDays(14);
        invoice.subtotal = subscription.unitPrice.multiply(BigDecimal.valueOf(subscription.quantity));
        invoice.discount = (subscription.discountAmount == null ? BigDecimal.ZERO : subscription.discountAmount).min(invoice.subtotal).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxable = invoice.subtotal.subtract(invoice.discount).max(BigDecimal.ZERO);
        invoice.tax = taxable.multiply(subscription.taxRate == null ? BigDecimal.ZERO : subscription.taxRate).setScale(2, RoundingMode.HALF_UP);
        invoice.total = taxable.add(invoice.tax);
        invoice.status = Invoice.Status.CONFIRMED;
        invoice.description = subscription.plan.name + " subscription billing";
        Invoice saved = invoices.save(invoice);
        subscription.nextBillingDate = nextDate(date, subscription.plan.interval);
        subscriptions.save(subscription);
        return saved;
    }

    private LocalDate nextDate(LocalDate date, Plan.Interval interval) {
        return switch (interval) {
            case DAILY -> date.plusDays(1);
            case WEEKLY -> date.plusWeeks(1);
            case MONTHLY -> date.plusMonths(1);
            case YEARLY -> date.plusYears(1);
        };
    }

    @PostMapping("/invoices/{id}/confirm")
    public Invoice confirm(@PathVariable Long id) {
        Invoice invoice = invoices.findById(id).orElseThrow();
        invoice.status = Invoice.Status.CONFIRMED;
        return invoices.save(invoice);
    }

    @PostMapping("/invoices/{id}/cancel")
    public Invoice cancel(@PathVariable Long id) {
        Invoice invoice = invoices.findById(id).orElseThrow();
        invoice.status = Invoice.Status.CANCELLED;
        return invoices.save(invoice);
    }

    @GetMapping("/payments")
    public List<Payment> payments(HttpSession session) {
        requireAnyRole(session, AppUser.Role.ADMIN, AppUser.Role.SALES);
        return payments.findAll();
    }

    @PostMapping("/payments")
    @Transactional
    public Payment payment(@RequestBody Payment payment) {
        if (payment.invoice == null || payment.invoice.id == null) {
            throw new IllegalArgumentException("Invoice is required");
        }
        Invoice invoice = invoices.findById(payment.invoice.id).orElseThrow(() -> new IllegalArgumentException("Invoice not found"));
        if (invoice.status == Invoice.Status.CANCELLED) {
            throw new IllegalArgumentException("Cannot pay a cancelled invoice");
        }
        if (payment.amount == null || payment.amount.signum() <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }
        if ((invoice.paidAmount == null ? BigDecimal.ZERO : invoice.paidAmount).add(payment.amount).compareTo(invoice.total) > 0) {
            throw new IllegalArgumentException("Payment exceeds invoice balance");
        }
        if (payment.paymentDate == null) {
            payment.paymentDate = LocalDate.now();
        }
        payment.invoice = invoice;
        Payment saved = payments.save(payment);
        invoice.paidAmount = (invoice.paidAmount == null ? BigDecimal.ZERO : invoice.paidAmount).add(payment.amount);
        if (invoice.paidAmount.compareTo(invoice.total) >= 0) {
            invoice.status = Invoice.Status.PAID;
        }
        invoices.save(invoice);
        return saved;
    }

    @GetMapping("/discounts")
    public List<?> discounts(HttpSession session) {
        requireAnyRole(session, AppUser.Role.ADMIN, AppUser.Role.SALES);
        List<DiscountRule> available = discounts.findAll();
        if (!"SALES".equals(session.getAttribute("USER_ROLE"))) {
            return available;
        }
        LocalDate today = LocalDate.now();
        return available.stream()
                .filter(d -> d.active && (d.startDate == null || !today.isBefore(d.startDate)) && (d.endDate == null || !today.isAfter(d.endDate)) && (d.usageLimit == null || d.usedCount < d.usageLimit))
                .map(d -> Map.of(
                        "id", d.id,
                        "name", d.name,
                        "kind", d.kind.name(),
                        "value", d.value,
                        "endDate", d.endDate == null ? "No expiry" : d.endDate.toString(),
                        "usedCount", d.usedCount,
                        "usageLimit", d.usageLimit == null ? "∞" : d.usageLimit,
                        "active", d.active))
                .toList();
    }

    @PostMapping("/discounts")
    public DiscountRule discount(@RequestBody DiscountRule discount, HttpSession session) {
        requireRole(session, AppUser.Role.ADMIN);
        if (discount.name == null || discount.name.isBlank()) {
            throw new IllegalArgumentException("Discount name is required");
        }
        if (discount.kind == null || discount.value == null || discount.value.signum() <= 0) {
            throw new IllegalArgumentException("A positive discount value and type are required");
        }
        if (discount.kind == DiscountRule.Kind.PERCENTAGE && discount.value.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Percentage discount cannot exceed 100%");
        }
        if (discount.minimumPurchase != null && discount.minimumPurchase.signum() < 0) {
            throw new IllegalArgumentException("Minimum purchase cannot be negative");
        }
        if (discount.minimumQuantity != null && discount.minimumQuantity < 1) {
            throw new IllegalArgumentException("Minimum quantity must be at least 1");
        }
        if (discount.usageLimit != null && discount.usageLimit < 1) {
            throw new IllegalArgumentException("Usage limit must be at least 1");
        }
        if (discount.startDate != null && discount.endDate != null && discount.endDate.isBefore(discount.startDate)) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }
        if (discount.usedCount < 0) {
            discount.usedCount = 0;
        }
        return discounts.save(discount);
    }

    @PostMapping("/subscriptions/{id}/apply-discount/{discountId}")
    @Transactional
    public Subscription applyDiscount(@PathVariable Long id, @PathVariable Long discountId) {
        Subscription subscription = subscriptions.findById(id).orElseThrow();
        DiscountRule discount = discounts.findById(discountId).orElseThrow();
        LocalDate today = LocalDate.now();
        if (!discount.active || !discount.appliesToSubscription || (discount.product != null) || (discount.startDate != null && today.isBefore(discount.startDate)) || (discount.endDate != null && today.isAfter(discount.endDate)) || (discount.usageLimit != null && discount.usedCount >= discount.usageLimit)) {
            throw new IllegalStateException("Discount is not currently eligible");
        }
        if (subscription.discountAmount != null && subscription.discountAmount.signum() > 0) {
            throw new IllegalStateException("A discount has already been applied to this subscription");
        }
        BigDecimal base = subscription.unitPrice.multiply(BigDecimal.valueOf(subscription.quantity));
        if (discount.minimumPurchase != null && base.compareTo(discount.minimumPurchase) < 0) {
            throw new IllegalStateException("Minimum purchase not met");
        }
        if (discount.minimumQuantity != null && subscription.quantity < discount.minimumQuantity) {
            throw new IllegalStateException("Minimum quantity not met");
        }
        if (discount.value == null || discount.value.signum() <= 0) {
            throw new IllegalStateException("Discount value must be positive");
        }
        if (discount.kind == DiscountRule.Kind.PERCENTAGE && discount.value.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalStateException("Percentage discount cannot exceed 100%");
        }
        BigDecimal amount = discount.kind == DiscountRule.Kind.PERCENTAGE ? base.multiply(discount.value).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP) : discount.value;
        subscription.discountAmount = amount.min(base).setScale(2, RoundingMode.HALF_UP);
        discount.usedCount++;
        discounts.save(discount);
        return subscriptions.save(subscription);
    }

    @GetMapping("/dashboard/summary")
    public Map<String, Object> dashboard(HttpSession session) {
        requireAnyRole(session, AppUser.Role.ADMIN, AppUser.Role.SALES);
        List<Invoice> all = invoices.findAll();
        BigDecimal revenue = all.stream().map(invoice -> invoice.paidAmount == null ? BigDecimal.ZERO : invoice.paidAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal owed = all.stream().filter(invoice -> invoice.status != Invoice.Status.CANCELLED).map(invoice -> invoice.total.subtract(invoice.paidAmount == null ? BigDecimal.ZERO : invoice.paidAmount).max(BigDecimal.ZERO)).reduce(BigDecimal.ZERO, BigDecimal::add);
        long overdue = all.stream().filter(invoice -> invoice.status != Invoice.Status.PAID && invoice.status != Invoice.Status.CANCELLED && invoice.dueDate != null && invoice.dueDate.isBefore(LocalDate.now())).count();
        List<Subscription> activeSubs = subscriptions.findByStatus(Subscription.Status.ACTIVE);
        long active = activeSubs.size();
        BigDecimal mrr = activeSubs.stream().map(subscription -> {
            BigDecimal monthly = subscription.unitPrice.multiply(BigDecimal.valueOf(subscription.quantity));
            return switch (subscription.plan.interval == null ? Plan.Interval.MONTHLY : subscription.plan.interval) {
                case DAILY -> monthly.multiply(new BigDecimal("30.4375"));
                case WEEKLY -> monthly.multiply(new BigDecimal("52")).divide(new BigDecimal("12"), 4, RoundingMode.HALF_UP);
                case MONTHLY -> monthly;
                case YEARLY -> monthly.divide(new BigDecimal("12"), 4, RoundingMode.HALF_UP);
            };
        }).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        return Map.of("monthlyRecurringRevenue", mrr, "activeSubscriptions", active, "revenueCollected", revenue, "outstanding", owed, "overdueInvoices", overdue, "totalSubscriptions", subscriptions.count(), "totalInvoices", invoices.count());
    }

    @GetMapping("/dashboard/overdue")
    public List<Invoice> overdue(HttpSession session) {
        requireAnyRole(session, AppUser.Role.ADMIN, AppUser.Role.SALES);
        return invoices.findAll().stream().filter(invoice -> invoice.status != Invoice.Status.PAID && invoice.status != Invoice.Status.CANCELLED && invoice.dueDate != null && invoice.dueDate.isBefore(LocalDate.now())).toList();
    }

    private void requireRole(HttpSession session, AppUser.Role expected) {
        String role = session == null ? null : (String) session.getAttribute("USER_ROLE");
        if (role != null) {
            try {
                if (AppUser.Role.valueOf(role.trim().toUpperCase(Locale.ROOT)) == expected) {
                    return;
                }
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed role values and fall back to user lookup.
            }
        }
        AppUser user = null;
        if (session != null && session.getAttribute("USER_ID") != null) {
            user = currentUser(session);
        }
        if (user != null && user.role == expected) {
            return;
        }
        throw new AccessDeniedException("Access denied");
    }

    private void requireAnyRole(HttpSession session, AppUser.Role... allowed) {
        final AppUser.Role[] allowedRoles = allowed;
        String role = session == null ? null : (String) session.getAttribute("USER_ROLE");
        if (role != null) {
            try {
                AppUser.Role parsed = AppUser.Role.valueOf(role.trim().toUpperCase(Locale.ROOT));
                if (Arrays.stream(allowedRoles).anyMatch(candidate -> candidate == parsed)) {
                    return;
                }
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed role values and fall back to user lookup.
            }
        }
        AppUser user = null;
        if (session != null && session.getAttribute("USER_ID") != null) {
            user = currentUser(session);
        }
        if (user != null) {
            final AppUser resolvedUser = user;
            if (Arrays.stream(allowedRoles).anyMatch(candidate -> candidate == resolvedUser.role)) {
                return;
            }
        }
        throw new AccessDeniedException("Access denied");
    }

    private AppUser currentUser(HttpSession session) {
        if (session == null) {
            throw new IllegalStateException("Please sign in");
        }
        Object id = session.getAttribute("USER_ID");
        if (id == null) {
            String email = (String) session.getAttribute("USER_EMAIL");
            if (email != null) {
                return users.findByEmail(email.trim().toLowerCase(Locale.ROOT)).orElseThrow(() -> new NoSuchElementException("Account not found"));
            }
            throw new IllegalStateException("Please sign in");
        }
        return users.findById(Long.valueOf(id.toString())).orElseThrow(() -> new NoSuchElementException("Account not found"));
    }
}

@Configuration
@EnableScheduling
class BillingScheduler {
    private final SubscriptionRepository subscriptions;
    private final InvoiceRepository invoices;
    private final ApiController api;

    BillingScheduler(SubscriptionRepository subscriptions, InvoiceRepository invoices, ApiController api) {
        this.subscriptions = subscriptions;
        this.invoices = invoices;
        this.api = api;
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void recurringBilling() {
        LocalDate today = LocalDate.now();
        for (Subscription subscription : subscriptions.findByStatus(Subscription.Status.ACTIVE)) {
            if (subscription.nextBillingDate != null && !subscription.nextBillingDate.isAfter(today) && !invoices.existsBySubscriptionIdAndInvoiceDate(subscription.id, today)) {
                try {
                    api.generate(subscription.id);
                } catch (Exception ignored) {
                    // log in a real deployment, but keep the scheduler resilient for the demo.
                }
            }
        }
    }
}
