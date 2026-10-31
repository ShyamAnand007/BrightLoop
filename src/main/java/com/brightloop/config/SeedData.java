package com.brightloop.config;
import com.brightloop.model.*;import com.brightloop.repository.*;import org.springframework.boot.CommandLineRunner;import org.springframework.context.annotation.Bean;import org.springframework.context.annotation.Configuration;import org.springframework.security.crypto.password.PasswordEncoder;import java.math.BigDecimal;import java.time.LocalDate;
@Configuration public class SeedData {
 @Bean CommandLineRunner seed(AppUserRepository users,ProductRepository products,PlanRepository plans,CustomerRepository customers,SubscriptionRepository subs,PasswordEncoder encoder){return args->{
  if(!users.existsByEmail("admin@brightloop.demo")){AppUser u=new AppUser();u.email="admin@brightloop.demo";u.fullName="Brightloop Admin";u.passwordHash=encoder.encode("Admin@12345");u.role=AppUser.Role.ADMIN;users.save(u);}
  if(!users.existsByEmail("sales@brightloop.demo")){AppUser u=new AppUser();u.email="sales@brightloop.demo";u.fullName="Sales Executive";u.passwordHash=encoder.encode("Sales@12345");u.role=AppUser.Role.SALES;users.save(u);}
  if(!users.existsByEmail("customer@brightloop.demo")){AppUser u=new AppUser();u.email="customer@brightloop.demo";u.fullName="Demo Customer";u.passwordHash=encoder.encode("Customer@12345");u.role=AppUser.Role.CUSTOMER;users.save(u);}

  if(products.count()==0){Product p=new Product();p.name="Brightloop Workspace";p.type="Software";p.salesPrice=new BigDecimal("49.00");p.costPrice=new BigDecimal("12.00");p.notes="Core workspace subscription";products.save(p);}
  if(plans.count()==0){Plan p=new Plan();p.name="Professional Monthly";p.interval=Plan.Interval.MONTHLY;p.price=new BigDecimal("49.00");p.minimumQuantity=1;p.availableFrom=LocalDate.now().minusMonths(1);plans.save(p);}
  if(!customers.existsByEmail("billing@acme.demo")){Customer c=new Customer();c.name="Acme Small Business";c.email="billing@acme.demo";c.company="Acme Small Business";customers.save(c);}
  if(!customers.existsByEmail("customer@brightloop.demo")){Customer c=new Customer();c.name="Demo Customer";c.email="customer@brightloop.demo";c.company="Personal account";customers.save(c);}
 }; }
}
