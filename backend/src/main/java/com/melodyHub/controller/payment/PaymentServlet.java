package com.melodyHub.controller.payment;

import com.melodyHub.controller.JsonServlet;
import com.melodyHub.dto.request.CreateOrderRequest;
import com.melodyHub.entity.User;
import com.melodyHub.entity.UserRole;
import com.melodyHub.exception.AuthException;
import com.melodyHub.exception.PremiumAlreadyActiveException;
import com.melodyHub.service.auth.AuthorizationService;
import com.melodyHub.service.payment.PaymentService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

public class PaymentServlet extends JsonServlet {
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;
    private PaymentService paymentService;
    private AuthorizationService authorizationService;

    @Override public void init() throws ServletException {
        paymentService = new PaymentService();
        authorizationService = new AuthorizationService();
    }

    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String path = getPath(request);
            User user = requireUser(request, false);
            if (user == null) { writeUnauthorized(response); return; }
            if ("/orders".equals(path) || "/orders/".equals(path)) { writeJson(response, 200, paymentService.listMyOrders(user.getId(), page(request), size(request))); return; }
            Long orderId = orderId(path, "/orders/");
            if (orderId != null) { writeJson(response, 200, paymentService.getOrder(user.getId(), orderId)); return; }
            if ("/admin/pending".equals(path)) {
                if (user.getRole() != UserRole.ADMIN) { writeForbidden(response); return; }
                writeJson(response, 200, paymentService.listPending(page(request), size(request))); return;
            }
            writeError(response, 404, "NOT_FOUND", "Payment endpoint was not found");
        } catch (AuthException exception) { writeError(response, status(exception), exception.getCode(), exception.getMessage());
        } catch (InvalidQueryParamException | IllegalArgumentException exception) { writeError(response, 400, "INVALID_REQUEST", exception.getMessage());
        } catch (SecurityException exception) { writeForbidden(response);
        } catch (SQLException exception) { writeError(response, 500, "DATABASE_ERROR", "Database error occurred"); }
    }

    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String path = getPath(request);
            User user = requireUser(request, false);
            if (user == null) { writeUnauthorized(response); return; }
            if ("/orders".equals(path) || "/orders/".equals(path)) {
                CreateOrderRequest body = objectMapper.readValue(request.getInputStream(), CreateOrderRequest.class);
                writeJson(response, 201, paymentService.createOrder(user.getId(), body == null ? null : body.getPlanCode())); return;
            }
            Long paidId = suffixId(path, "/orders/", "/paid");
            if (paidId != null) { writeJson(response, 200, paymentService.markPaid(user.getId(), paidId)); return; }
            Long confirmId = suffixId(path, "/admin/orders/", "/confirm");
            if (confirmId != null) { requireAdmin(user); writeJson(response, 200, paymentService.confirm(user.getId(), confirmId)); return; }
            Long rejectId = suffixId(path, "/admin/orders/", "/reject");
            if (rejectId != null) { requireAdmin(user); writeJson(response, 200, paymentService.reject(user.getId(), rejectId)); return; }
            writeError(response, 404, "NOT_FOUND", "Payment endpoint was not found");
        } catch (AuthException exception) { writeError(response, status(exception), exception.getCode(), exception.getMessage());
        } catch (PremiumAlreadyActiveException exception) { writeError(response, 409, "PREMIUM_ALREADY_ACTIVE", exception.getMessage());
        } catch (IllegalArgumentException exception) { writeError(response, 400, "INVALID_REQUEST", exception.getMessage());
        } catch (SecurityException exception) { writeForbidden(response);
        } catch (SQLException exception) { writeError(response, 500, "DATABASE_ERROR", "Database error occurred");
        } catch (IOException exception) { writeError(response, 400, "INVALID_JSON", "Request body is not valid JSON"); }
    }

    private User requireUser(HttpServletRequest request, boolean premium) throws AuthException, SQLException {
        String token = getBearerToken(request);
        return premium ? authorizationService.requirePremium(token) : authorizationService.requireAuthenticated(token);
    }
    private void requireAdmin(User user) { if (user.getRole() != UserRole.ADMIN) throw new SecurityException("Admin access required"); }
    private int page(HttpServletRequest request) throws InvalidQueryParamException { return parsePositiveInt(request.getParameter("page"), "page", DEFAULT_PAGE); }
    private int size(HttpServletRequest request) throws InvalidQueryParamException { int value = parsePositiveInt(request.getParameter("size"), "size", DEFAULT_SIZE); if (value > MAX_SIZE) throw new InvalidQueryParamException("size must not exceed " + MAX_SIZE); return value; }
    private Long orderId(String path, String prefix) { return suffixId(path, prefix, ""); }
    private Long suffixId(String path, String prefix, String suffix) {
        if (!path.startsWith(prefix) || !path.endsWith(suffix)) return null;
        String id = path.substring(prefix.length(), path.length() - suffix.length());
        if (id.isBlank() || id.contains("/")) return null;
        try { return Long.parseLong(id); } catch (NumberFormatException exception) { return null; }
    }
    private void writeForbidden(HttpServletResponse response) throws IOException { writeError(response, 403, "FORBIDDEN", "You do not have permission to access this resource"); }
    private int status(AuthException exception) { return "PREMIUM_REQUIRED".equals(exception.getCode()) ? 402 : 401; }
}
