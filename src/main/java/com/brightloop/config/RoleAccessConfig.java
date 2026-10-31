package com.brightloop.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class RoleAccessConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RoleAccessInterceptor()).addPathPatterns("/api/**");
    }

    private static class RoleAccessInterceptor implements HandlerInterceptor {
        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
                throws Exception {
            String path = request.getRequestURI().substring(request.getContextPath().length());
            String method = request.getMethod();

            if (isPublicAuthRoute(path, method)) {
                return true;
            }

            Object role = request.getSession(false) == null
                    ? null
                    : request.getSession(false).getAttribute("USER_ROLE");
            if (role == null) {
                return reject(response, HttpServletResponse.SC_UNAUTHORIZED, "Please sign in");
            }

            String roleName = role.toString();
            if ("ADMIN".equals(roleName)) {
                return true;
            }
            if ("SALES".equals(roleName) && isSalesRoute(path, method)) {
                return true;
            }
            if ("CUSTOMER".equals(roleName) && isCustomerRoute(path, method)) {
                return true;
            }

            return reject(response, HttpServletResponse.SC_FORBIDDEN, "This area is not available for your account role");
        }

        private boolean isPublicAuthRoute(String path, String method) {
            return ("/api/auth/login".equals(path) && "POST".equals(method))
                    || ("/api/auth/signup".equals(path) && "POST".equals(method));
        }

        private boolean isSalesRoute(String path, String method) {
            if (("/api/auth/me".equals(path) && "GET".equals(method))
                    || ("/api/auth/logout".equals(path) && "POST".equals(method))
                    || ("/api/profile".equals(path) && ("GET".equals(method) || "PUT".equals(method)))
                    || ("/api/customers".equals(path) && ("GET".equals(method) || "POST".equals(method)))
                    || ("/api/subscriptions".equals(path) && ("GET".equals(method) || "POST".equals(method)))
                    || ("/api/invoices".equals(path) && "GET".equals(method))
                    || ("/api/payments".equals(path) && ("GET".equals(method) || "POST".equals(method)))
                    || ("/api/discounts".equals(path) && "GET".equals(method))
                    || ("/api/discounts".equals(path) && "GET".equals(method))
                    || ("/api/sales/dashboard".equals(path) && "GET".equals(method))
                    || ("/api/sales/products".equals(path) && "GET".equals(method))
                    || ("/api/sales/plans".equals(path) && "GET".equals(method))
                    || ("/api/sales/discounts".equals(path) && "GET".equals(method))
                    || (path.matches("/api/sales/products/\\d+/variants") && "GET".equals(method))) {
                return true;
            }

            return ("PUT".equals(method) && path.matches("/api/subscriptions/\\d+/status"))
                    || ("POST".equals(method) && (path.matches("/api/subscriptions/\\d+/renew")
                    || path.matches("/api/subscriptions/\\d+/apply-discount/\\d+")
                    || path.matches("/api/invoices/generate/\\d+")
                    || path.matches("/api/invoices/\\d+/(confirm|cancel)")));
        }

        private boolean isCustomerRoute(String path, String method) {
            return ("/api/auth/me".equals(path) && "GET".equals(method))
                    || ("/api/auth/logout".equals(path) && "POST".equals(method))
                    || ("/api/profile".equals(path) && ("GET".equals(method) || "PUT".equals(method)))
                    || ("/api/customer/subscriptions".equals(path) && "GET".equals(method))
                    || (path.startsWith("/api/customer/") && ("GET".equals(method) || "PUT".equals(method)));
        }

        private boolean reject(HttpServletResponse response, int status, String message) throws Exception {
            response.setStatus(status);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"error\":\"" + message + "\"}");
            return false;
        }
    }
}
