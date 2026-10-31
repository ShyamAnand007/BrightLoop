package com.brightloop.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import com.brightloop.repository.AppUserRepository;
import com.brightloop.model.AppUser;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class RoleAccessConfig implements WebMvcConfigurer {
  private final AppUserRepository users;
  public RoleAccessConfig(AppUserRepository users){this.users=users;}
  @Override public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(new HandlerInterceptor() {
      @Override public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws Exception {
        String path=req.getRequestURI(), method=req.getMethod();
        if (!path.startsWith("/api/") || path.equals("/api/auth/login") || path.equals("/api/auth/signup") || path.equals("/api/auth/csrf") || path.equals("/api/auth/forgot") || path.equals("/api/auth/reset")) return true;
        var session=req.getSession(false);
        Object userId=session==null?null:session.getAttribute("USER_ID");
        if(userId==null){deny(res,401,"Please sign in");return false;}
        AppUser account=users.findById(Long.valueOf(userId.toString())).orElse(null);
        if(account==null||Boolean.FALSE.equals(account.active)){if(session!=null)session.invalidate();deny(res,401,"Your account is inactive or no longer exists");return false;}
        String role=account.role.name();
        if("ADMIN".equals(role)) return true;

        if("SALES".equals(role)) {
          boolean read=method.equals("GET");
          boolean allowed =
            path.equals("/api/auth/me") || path.equals("/api/auth/logout") ||
            (path.equals("/api/products") || path.matches("/api/products/\\d+/variants")) && read ||
            path.equals("/api/plans") && read ||
            path.equals("/api/customers") ||
            path.equals("/api/subscriptions") ||
            path.matches("/api/subscriptions/\\d+/(status|renew)") ||
            path.matches("/api/invoices") ||
            path.matches("/api/invoices/generate/\\d+") ||
            path.matches("/api/invoices/\\d+/(confirm|cancel)") ||
            path.equals("/api/payments") ||
            path.equals("/api/discounts") && read ||
            path.matches("/api/subscriptions/\\d+/apply-discount/\\d+") ||
            path.equals("/api/taxes") && read || path.equals("/api/templates") && read || path.matches("/api/quotations/(from-template|\\d+/send)") || path.equals("/api/billing/run") || path.equals("/api/billing/failures") || path.matches("/api/invoices/\\d+/send") ||
            path.equals("/api/dashboard/summary") ||
            path.equals("/api/dashboard/overdue");
          if(allowed) return true;
          deny(res,403,"This action requires Admin access");return false;
        }

        // Customers may access only their own self-service resources and public catalogue.
        boolean allowed =
          path.equals("/api/auth/me") || path.equals("/api/auth/logout") ||
          path.equals("/api/plans") && method.equals("GET") ||
          (path.equals("/api/products") || path.matches("/api/products/\\d+/variants")) && method.equals("GET") ||
          path.matches("/api/customer/quotations/\\d+/(accept|reject)") && method.equals("POST") ||
          path.equals("/api/customer/profile") && (method.equals("GET")||method.equals("PUT")) ||
          path.equals("/api/customer/subscriptions") && method.equals("GET") ||
          path.equals("/api/customer/invoices") && method.equals("GET") ||
          path.equals("/api/customer/payments") && method.equals("GET");
        if(allowed) return true;
        deny(res,403,"This area is not available for your account role");return false;
      }
    }).addPathPatterns("/api/**");
  }
  private static void deny(HttpServletResponse res,int status,String message)throws Exception {
    res.setStatus(status);res.setContentType("application/json");res.setCharacterEncoding("UTF-8");
    res.getWriter().write("{\"error\":\""+message+"\"}");
  }
}
