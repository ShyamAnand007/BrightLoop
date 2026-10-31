package com.brightloop.controller;
import com.brightloop.model.*;import com.brightloop.repository.*;import com.brightloop.service.BillingService;
import jakarta.servlet.http.HttpSession;import org.springframework.security.crypto.password.PasswordEncoder;import org.springframework.transaction.annotation.Transactional;import org.springframework.web.bind.annotation.*;
import java.math.*;import java.time.*;import java.util.*;
@RestController @RequestMapping("/api")
public class ExtrasController {
  private final TaxRuleRepository taxes; private final QuotationTemplateRepository templates; private final ResetTokenRepository tokens; private final BillingFailureRepository failures; private final ProductRepository products; private final PlanRepository plans;
  private final DiscountRuleRepository discounts; private final CustomerRepository customers; private final SubscriptionRepository subs; private final InvoiceRepository invoices; private final AppUserRepository users; private final PasswordEncoder encoder; private final BillingService billing;
  public ExtrasController(TaxRuleRepository t, QuotationTemplateRepository q, ResetTokenRepository r, BillingFailureRepository f, ProductRepository pr, PlanRepository pl, DiscountRuleRepository d, CustomerRepository c, SubscriptionRepository s, InvoiceRepository i, AppUserRepository u, PasswordEncoder e, BillingService b) {
    taxes = t; templates = q; tokens = r; failures = f; products = pr; plans = pl; discounts = d; customers = c; subs = s; invoices = i; users = u; encoder = e; billing = b; }
  static BigDecimal dec(Object o) { return o == null || o.toString().isBlank() ? null : new BigDecimal(o.toString()); }
  static LocalDate day(Object o) { return o == null || o.toString().isBlank() ? null : LocalDate.parse(o.toString()); }
  static Long lng(Object o) { return o == null || o.toString().isBlank() ? null : Long.valueOf(o.toString()); }
  static boolean bool(Object o) { return Boolean.parseBoolean(String.valueOf(o)); }
  static String str(Object o) { return o == null ? "" : o.toString().trim(); }
  static String pwRule(String pw) { return pw == null || pw.length() <= 8 || !pw.matches(".*[A-Z].*") || !pw.matches(".*[a-z].*") || !pw.matches(".*[^A-Za-z0-9].*") ? "Password must be more than 8 characters with an uppercase letter, a lowercase letter and a special character" : null; }

  // ---- tax rules (admin manages; staff can view) ----
  @GetMapping("/taxes") public List<TaxRule> listTaxes() { return taxes.findAll(); }
  @PostMapping("/taxes") public TaxRule addTax(@RequestBody Map<String, Object> b) { TaxRule t = new TaxRule(); t.name = str(b.get("name")); BigDecimal r = dec(b.get("ratePercent"));
    if (t.name.isBlank() || r == null || r.signum() < 0 || r.compareTo(new BigDecimal("100")) > 0) throw new IllegalArgumentException("Tax name and a rate between 0 and 100 are required"); t.ratePercent = r; return taxes.save(t); }
  @PutMapping("/taxes/{id}") public TaxRule editTax(@PathVariable Long id, @RequestBody Map<String, Object> b) { TaxRule t = taxes.findById(id).orElseThrow(); if (!str(b.get("name")).isBlank()) t.name = str(b.get("name")); BigDecimal r = dec(b.get("ratePercent"));
    if (r != null) { if (r.signum() < 0 || r.compareTo(new BigDecimal("100")) > 0) throw new IllegalArgumentException("Rate must be between 0 and 100"); t.ratePercent = r; } if (b.get("active") != null) t.active = bool(b.get("active")); return taxes.save(t); }

  // ---- admin edit / activate / deactivate (staff are blocked by the access interceptor) ----
  @PutMapping("/products/{id}") public Product editProduct(@PathVariable Long id, @RequestBody Map<String, Object> b) { Product p = products.findById(id).orElseThrow();
    if (!str(b.get("name")).isBlank()) p.name = str(b.get("name")); if (b.get("type") != null) p.type = str(b.get("type")); if (b.get("notes") != null) p.notes = str(b.get("notes"));
    BigDecimal sp = dec(b.get("salesPrice")); if (sp != null) { if (sp.signum() < 0) throw new IllegalArgumentException("Price cannot be negative"); p.salesPrice = sp; } BigDecimal cp = dec(b.get("costPrice")); if (cp != null) p.costPrice = cp;
    if (b.get("active") != null) p.active = bool(b.get("active")); return products.save(p); }
  @PutMapping("/plans/{id}") public Plan editPlan(@PathVariable Long id, @RequestBody Map<String, Object> b) { Plan p = plans.findById(id).orElseThrow(); if (!str(b.get("name")).isBlank()) p.name = str(b.get("name"));
    BigDecimal pr = dec(b.get("price")); if (pr != null) { if (pr.signum() < 0) throw new IllegalArgumentException("Price cannot be negative"); p.price = pr; }
    if (b.get("minimumQuantity") != null) p.minimumQuantity = Math.max(1, lng(b.get("minimumQuantity")).intValue()); if (b.containsKey("availableFrom")) p.availableFrom = day(b.get("availableFrom")); if (b.containsKey("availableUntil")) p.availableUntil = day(b.get("availableUntil"));
    if (b.get("active") != null) p.active = bool(b.get("active")); if (b.get("autoClose") != null) p.autoClose = bool(b.get("autoClose")); if (b.get("allowEarlyClose") != null) p.allowEarlyClose = bool(b.get("allowEarlyClose"));
    if (b.get("allowPause") != null) p.allowPause = bool(b.get("allowPause")); if (b.get("allowRenew") != null) p.allowRenew = bool(b.get("allowRenew")); return plans.save(p); }
  @PutMapping("/discounts/{id}") public DiscountRule editDiscount(@PathVariable Long id, @RequestBody Map<String, Object> b) { DiscountRule d = discounts.findById(id).orElseThrow(); if (!str(b.get("name")).isBlank()) d.name = str(b.get("name"));
    if (b.get("active") != null) d.active = bool(b.get("active")); if (b.containsKey("startDate")) d.startDate = day(b.get("startDate")); if (b.containsKey("endDate")) d.endDate = day(b.get("endDate"));
    if (b.containsKey("usageLimit")) d.usageLimit = lng(b.get("usageLimit")) == null ? null : lng(b.get("usageLimit")).intValue(); BigDecimal mp = dec(b.get("minimumPurchase")); if (mp != null) d.minimumPurchase = mp;
    if (d.startDate != null && d.endDate != null && d.endDate.isBefore(d.startDate)) throw new IllegalArgumentException("End date cannot be before start date"); return discounts.save(d); }

  // ---- quotation templates and the quotation workflow ----
  @GetMapping("/templates") public List<QuotationTemplate> listTemplates() { return templates.findAll(); }
  @PostMapping("/templates") public QuotationTemplate addTemplate(@RequestBody Map<String, Object> b) { QuotationTemplate t = new QuotationTemplate(); t.name = str(b.get("name")); if (t.name.isBlank()) throw new IllegalArgumentException("Template name is required");
    if (lng(b.get("planId")) == null) throw new IllegalArgumentException("Choose a plan for the template"); t.plan = plans.findById(lng(b.get("planId"))).orElseThrow(() -> new IllegalArgumentException("Plan not found")); t.quantity = lng(b.get("quantity")) == null ? 1 : Math.max(1, lng(b.get("quantity")).intValue());
    t.validDays = lng(b.get("validDays")) == null ? 30 : Math.max(1, lng(b.get("validDays")).intValue()); if (lng(b.get("taxRuleId")) != null) t.taxRule = taxes.findById(lng(b.get("taxRuleId"))).orElse(null); return templates.save(t); }
  @PostMapping("/quotations/from-template") @Transactional public Subscription fromTemplate(@RequestBody Map<String, Object> b) {
    if (lng(b.get("templateId")) == null) throw new IllegalArgumentException("Choose a quotation template"); if (lng(b.get("customerId")) == null) throw new IllegalArgumentException("Choose a customer");
    QuotationTemplate t = templates.findById(lng(b.get("templateId"))).orElseThrow(() -> new IllegalArgumentException("Template not found")); Customer c = customers.findById(lng(b.get("customerId"))).orElseThrow(() -> new IllegalArgumentException("Customer not found"));
    Plan p = t.plan; if (!p.active) throw new IllegalArgumentException("Plan is inactive"); if (t.quantity < p.minimumQuantity) throw new IllegalArgumentException("Template quantity is below the plan minimum");
    LocalDate start = day(b.get("startDate")) == null ? LocalDate.now() : day(b.get("startDate")); Subscription s = new Subscription(); s.number = "QUO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(); s.customer = c; s.plan = p; s.quantity = t.quantity; s.unitPrice = p.price;
    s.taxRate = t.taxRule == null ? new BigDecimal("0.18") : t.taxRule.ratePercent.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP); s.startDate = start; s.nextBillingDate = start; s.paymentTerms = "Net 14"; s.status = Subscription.Status.DRAFT; return subs.save(s); }
  @PostMapping("/quotations/{id}/send") public Subscription sendQuote(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> b) { Subscription s = subs.findById(id).orElseThrow();
    if (s.status != Subscription.Status.DRAFT) throw new IllegalStateException("Only draft quotations can be sent"); Long days = b == null ? null : lng(b.get("validDays")); s.validUntil = LocalDate.now().plusDays(days == null ? 30 : days); s.status = Subscription.Status.QUOTATION; return subs.save(s); }
  private Subscription mine(Long id, HttpSession session) { Subscription s = subs.findById(id).orElseThrow(); if (!Objects.equals(s.customer.email, session.getAttribute("USER_EMAIL"))) throw new IllegalStateException("Quotation not found"); return s; }
  @PostMapping("/customer/quotations/{id}/accept") public Subscription accept(@PathVariable Long id, HttpSession session) { Subscription s = mine(id, session); if (s.status != Subscription.Status.QUOTATION) throw new IllegalStateException("This quotation can no longer be accepted");
    if (s.validUntil != null && s.validUntil.isBefore(LocalDate.now())) throw new IllegalStateException("This quotation has expired"); s.status = Subscription.Status.CONFIRMED; return subs.save(s); }
  @PostMapping("/customer/quotations/{id}/reject") public Subscription reject(@PathVariable Long id, HttpSession session) { Subscription s = mine(id, session); if (s.status != Subscription.Status.QUOTATION) throw new IllegalStateException("This quotation can no longer be rejected"); s.status = Subscription.Status.CLOSED; return subs.save(s); }

  // ---- billing run, failures, invoice send ----
  @PostMapping("/billing/run") public Map<String, Object> runBilling(@RequestBody(required = false) Map<String, Object> b) { LocalDate d = b == null ? null : day(b.get("asOf")); return billing.runAll(d == null ? LocalDate.now() : d); }
  @GetMapping("/billing/failures") public List<BillingFailure> billingFailures() { return failures.findTop50ByOrderByIdDesc(); }
  @PostMapping("/invoices/{id}/send") public Map<String, String> sendInvoice(@PathVariable Long id) { invoices.findById(id).orElseThrow(); return Map.of("message", "Invoice queued. Email delivery is not configured on this server."); }

  // ---- password reset (link is printed to the server console until SMTP is configured) ----
  @PostMapping("/auth/forgot") public Map<String, String> forgot(@RequestBody Map<String, String> b) { String email = b.getOrDefault("email", "").trim().toLowerCase(Locale.ROOT);
    users.findByEmail(email).ifPresent(u -> { ResetToken t = new ResetToken(); t.token = UUID.randomUUID().toString().replace("-", ""); t.email = email; t.expiresAt = LocalDateTime.now().plusMinutes(30); tokens.save(t); System.out.println("PASSWORD RESET LINK: http://localhost:8080/#reset=" + t.token); });
    return Map.of("message", "If that email is registered, a reset link was sent"); }
  @PostMapping("/auth/reset") @Transactional public Map<String, String> reset(@RequestBody Map<String, String> b) { String err = pwRule(b.get("password")); if (err != null) throw new IllegalArgumentException(err);
    ResetToken t = tokens.findByToken(b.getOrDefault("token", "")).orElseThrow(() -> new IllegalArgumentException("Reset link is invalid or expired")); if (t.expiresAt.isBefore(LocalDateTime.now())) throw new IllegalArgumentException("Reset link is invalid or expired");
    AppUser u = users.findByEmail(t.email).orElseThrow(); u.passwordHash = encoder.encode(b.get("password")); users.save(u); tokens.delete(t); return Map.of("message", "Password updated"); }
}
