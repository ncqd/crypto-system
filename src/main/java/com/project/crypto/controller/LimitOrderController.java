package com.project.crypto.controller;

import com.project.crypto.domain.entity.User;
import com.project.crypto.domain.enums.OrderStatus;
import com.project.crypto.dto.LimitOrderRequest;
import com.project.crypto.dto.LimitOrderResponse;
import com.project.crypto.security.AuthUserPrincipal;
import com.project.crypto.security.SecurityUtils;
import com.project.crypto.service.LimitOrderService;
import com.project.crypto.service.UserContextService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class LimitOrderController {

    private final UserContextService userContextService;
    private final LimitOrderService limitOrderService;

    public LimitOrderController(UserContextService userContextService, LimitOrderService limitOrderService) {
        this.userContextService = userContextService;
        this.limitOrderService = limitOrderService;
    }

    @PostMapping("/limit")
    @ResponseStatus(HttpStatus.CREATED)
    public LimitOrderResponse placeLimitOrder(@Valid @RequestBody LimitOrderRequest request) {
        return limitOrderService.placeLimitOrder(currentUser(), request);
    }

    @GetMapping("/limit")
    public List<LimitOrderResponse> listLimitOrders(@RequestParam(required = false) OrderStatus status) {
        return limitOrderService.listOrders(currentUser(), status);
    }

    @DeleteMapping("/limit/{orderId}")
    public LimitOrderResponse cancelLimitOrder(@PathVariable Long orderId) {
        return limitOrderService.cancelLimitOrder(currentUser(), orderId);
    }

    private User currentUser() {
        AuthUserPrincipal principal = SecurityUtils.requirePrincipal();
        return userContextService.requireUser(principal.userId());
    }
}
