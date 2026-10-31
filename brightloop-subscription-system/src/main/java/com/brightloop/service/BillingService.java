package com.brightloop.service;
import com.brightloop.model.*;import com.brightloop.repository.*;
import org.springframework.beans.factory.annotation.Autowired;import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;
import java.math.*;import java.time.LocalDate;import java.util.*;
@Service
public class BillingService {
  @Autowired SubscriptionRepository subs; @Autowired InvoiceRepository invoices; @Autowired DiscountRuleRepository discounts; @Autowired BillingFailureRepository failures;
  @Autowired @Lazy BillingService self;
  static LocalDate next(LocalDate d, Plan.Interval x) { return switch (x == null ? Plan.Interval.MONTHLY : x) { case DAILY -> d.plusDays(1); case WEEKLY -> d.plusWeeks(1); case MONTHLY -> d.plusMonths(1); case YEARLY -> d.plusYears(1); }; }
  /** Bills every due subscription; one failure never blocks the rest and is recorded for review. */
  public Map<String, Object> runAll(LocalDate asOf) {
    int created = 0, failed = 0;
    for (Subscription s : subs.findByStatus(Subscription.Status.ACTIVE)) {
      if (s.nextBillingDate == null || s.nextBillingDate.isAfter(asOf)) continue;
      try { created += self.billSubscription(s.id, asOf).size(); }
      catch (Exception e) { failed++; BillingFailure f = new BillingFailure(); f.subscriptionId = s.id; f.billingDate = asOf; String m = String.valueOf(e.getMessage()); f.message = m.length() > 480 ? m.substring(0, 480) : m; failures.save(f); }
    }
    return Map.of("created", created, "failed", failed);
  }
  /** Bills every missed period up to asOf. UNIQUE(subscription, period_start) makes reruns safe. */
  @Transactional public List<Invoice> billSubscription(Long id, LocalDate asOf) {
    Subscription s = subs.findById(id).orElseThrow(); List<Invoice> made = new ArrayList<>();
    if (s.status != Subscription.Status.ACTIVE) return made;
    LocalDate period = s.nextBillingDate == null ? s.startDate : s.nextBillingDate; int guard = 0;
    while (period != null && !period.isAfter(asOf) && (s.expiryDate == null || !period.isAfter(s.expiryDate)) && guard++ < 400) {
      if (!invoices.existsBySubscriptionIdAndPeriodStart(s.id, period)) made.add(invoices.save(build(s, period)));
      period = next(period, s.plan.interval);
    }
    s.nextBillingDate = period;
    if (s.plan.autoClose && s.expiryDate != null && period != null && period.isAfter(s.expiryDate)) s.status = Subscription.Status.CLOSED;
    subs.save(s); return made;
  }
  private Invoice build(Subscription s, LocalDate period) {
    Invoice i = new Invoice(); i.number = "INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    i.subscription = s; i.customer = s.customer; i.periodStart = period; i.invoiceDate = period; i.dueDate = period.plusDays(14);
    i.subtotal = s.unitPrice.multiply(BigDecimal.valueOf(s.quantity)); BigDecimal disc = BigDecimal.ZERO;
    if (s.discountRuleId != null) {   // discount comes from the rule at billing time: stops at expiry or when the usage limit is reached
      DiscountRule d = discounts.findById(s.discountRuleId).orElse(null);
      boolean ok = d != null && d.active && (d.startDate == null || !period.isBefore(d.startDate)) && (d.endDate == null || !period.isAfter(d.endDate))
        && (d.minimumPurchase == null || i.subtotal.compareTo(d.minimumPurchase) >= 0) && (d.minimumQuantity == null || s.quantity >= d.minimumQuantity);
      if (ok && discounts.consume(d.id) == 1) disc = d.kind == DiscountRule.Kind.PERCENTAGE ? i.subtotal.multiply(d.value).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP) : d.value;
    }
    i.discount = disc.min(i.subtotal).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    BigDecimal taxable = i.subtotal.subtract(i.discount);
    i.tax = taxable.multiply(s.taxRate == null ? BigDecimal.ZERO : s.taxRate).setScale(2, RoundingMode.HALF_UP);
    i.total = taxable.add(i.tax); i.status = Invoice.Status.DRAFT; i.description = s.plan.name + " subscription billing"; return i;
  }
}
