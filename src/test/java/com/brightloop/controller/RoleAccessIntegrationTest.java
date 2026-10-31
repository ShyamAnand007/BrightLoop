package com.brightloop.controller;

import com.brightloop.model.AppUser;
import com.brightloop.model.Customer;
import com.brightloop.model.Invoice;
import com.brightloop.model.Plan;
import com.brightloop.model.Subscription;
import com.brightloop.repository.AppUserRepository;
import com.brightloop.repository.CustomerRepository;
import com.brightloop.repository.InvoiceRepository;
import com.brightloop.repository.PlanRepository;
import com.brightloop.repository.SubscriptionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:role-access-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class RoleAccessIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private AppUserRepository users;
    @Autowired private CustomerRepository customers;
    @Autowired private PlanRepository plans;
    @Autowired private SubscriptionRepository subscriptions;
    @Autowired private InvoiceRepository invoices;

    @Test
    void unauthenticatedRequestsCannotReadAdminUsers() throws Exception {
        mvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void salesCannotReadAdminUsersButAdminCan() throws Exception {
        mvc.perform(get("/api/users").session(session("SALES")))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/users").session(session("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void customerCannotReadAnotherCustomersInvoice() throws Exception {
        String suffix = UUID.randomUUID().toString();
        AppUser user = new AppUser();
        user.email = "customer-" + suffix + "@example.test";
        user.fullName = "Test Customer";
        user.passwordHash = "not-used";
        user.role = AppUser.Role.CUSTOMER;
        user = users.save(user);

        Customer otherCustomer = new Customer();
        otherCustomer.name = "Other Customer";
        otherCustomer.email = "other-" + suffix + "@example.test";
        otherCustomer = customers.save(otherCustomer);

        Plan plan = new Plan();
        plan.name = "Test Plan " + suffix;
        plan.price = BigDecimal.TEN;
        plan = plans.save(plan);

        Subscription subscription = new Subscription();
        subscription.number = "SUB-" + suffix;
        subscription.customer = otherCustomer;
        subscription.plan = plan;
        subscription.unitPrice = BigDecimal.TEN;
        subscription.startDate = LocalDate.now();
        subscription = subscriptions.save(subscription);

        Invoice invoice = new Invoice();
        invoice.number = "INV-" + suffix;
        invoice.subscription = subscription;
        invoice.customer = otherCustomer;
        invoice.total = BigDecimal.TEN;
        invoice.invoiceDate = LocalDate.now();
        invoice = invoices.save(invoice);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("USER_ID", user.id);
        session.setAttribute("USER_EMAIL", user.email);
        session.setAttribute("USER_ROLE", "CUSTOMER");

        mvc.perform(get("/api/customer/invoices/" + invoice.id).session(session))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/customer/invoices").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(0)));
    }

    @Test
    void salesCannotOverrideConfiguredSubscriptionPriceTaxOrDiscount() throws Exception {
        String suffix = UUID.randomUUID().toString();
        Customer customer = new Customer();
        customer.name = "Sales Test Customer";
        customer.email = "sales-" + suffix + "@example.test";
        customer = customers.save(customer);

        Plan plan = new Plan();
        plan.name = "Sales Test Plan " + suffix;
        plan.price = new BigDecimal("49.00");
        plan.minimumQuantity = 1;
        plan = plans.save(plan);

        String body = """
                {"customer":{"id":%d},"plan":{"id":%d},"quantity":1,
                 "unitPrice":1,"taxRate":0.9,"discountAmount":10,"status":"ACTIVE"}
                """.formatted(customer.id, plan.id);

        mvc.perform(post("/api/subscriptions")
                        .session(session("SALES"))
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unitPrice", is(49.0)))
                .andExpect(jsonPath("$.taxRate", is(0.18)))
                .andExpect(jsonPath("$.discountAmount", is(0)));
    }

    private MockHttpSession session(String role) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("USER_ROLE", role);
        session.setAttribute("USER_EMAIL", "test-" + role.toLowerCase() + "@example.test");
        return session;
    }
}
