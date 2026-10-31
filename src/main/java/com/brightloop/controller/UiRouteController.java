package com.brightloop.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class UiRouteController {
    @GetMapping({
            "/admin", "/admin/{page}",
            "/sales", "/sales/{page}",
            "/customer", "/customer/{page}"
    })
    public String roleWorkspace() {
        return "forward:/index.html";
    }
}
