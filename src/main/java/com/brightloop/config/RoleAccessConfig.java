package com.brightloop.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class RoleAccessConfig implements WebMvcConfigurer {
  @Override public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(new HandlerInterceptor() {
      @Override public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws Exception {
        String path=req.getRequestURI(), method=req.getMethod();
        if (!path.startsWith("/api/") || path.startsWith("/api/auth/login") || path.startsWith("/api/auth/signup")) return true;
        Object roleObj=req.getSession(false)==null?null:req.getSession(false).getAttribute("USER_ROLE");
        if(roleObj==null){res.setStatus(401);res.setContentType("application/json");res.getWriter().write("{\"error\":\"Please sign in\"}");return false;}
        String role=roleObj.toString();
        if("ADMIN".equals(role)) return true;
        if("SALES".equals(role)) {
          if(path.startsWith("/api/users") || path.startsWith("/api/products") || (path.startsWith("/api/plans") && !method.equals("GET")) || path.startsWith("/api/auth/staff")) {res.setStatus(403);res.getWriter().write("{\"error\":\"Admin access required\"}");return false;}
          return true;
        }
        boolean allowed=(path.equals("/api/auth/me") || path.equals("/api/auth/logout") || (path.equals("/api/plans") && method.equals("GET")) || (path.equals("/api/customer/subscriptions") && method.equals("GET")));
        if(!allowed){res.setStatus(403);res.setContentType("application/json");res.getWriter().write("{\"error\":\"This area is not available for your account role\"}");return false;}
        return true;
      }
    }).addPathPatterns("/api/**");
  }
}
