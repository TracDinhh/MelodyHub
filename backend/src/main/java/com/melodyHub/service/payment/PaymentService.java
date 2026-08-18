package com.melodyHub.service.payment;

import com.melodyHub.config.AppConfig;
import com.melodyHub.dto.response.PagedResponse;
import com.melodyHub.dto.response.PaymentOrderResponse;
import com.melodyHub.entity.PaymentOrder;
import com.melodyHub.entity.PaymentStatus;
import com.melodyHub.entity.User;
import com.melodyHub.exception.PremiumAlreadyActiveException;
import com.melodyHub.repository.PaymentRepository;
import com.melodyHub.repository.UserRepository;
import com.melodyHub.util.Pagination;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class PaymentService {
    private static final int DEFAULT_PAGE_SIZE = 20;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    public PaymentService() { this(new PaymentRepository(), new UserRepository()); }

    public PaymentService(PaymentRepository paymentRepository, UserRepository userRepository) {
        this.paymentRepository = Objects.requireNonNull(paymentRepository);
        this.userRepository = Objects.requireNonNull(userRepository);
    }

    public synchronized PaymentOrderResponse createOrder(int userId, String planCode) throws SQLException {
        Plan plan = plans().get(normalizePlan(planCode));
        if (plan == null) throw new IllegalArgumentException("Unknown premium plan");
        // Already-premium users must not open a fresh payment: block re-purchase
        // while their subscription is still active.
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (user.isPremium()) throw new PremiumAlreadyActiveException();
        // Reuse the outstanding order instead of spawning a duplicate.
        PaymentOrder pendingOrder = paymentRepository.findPendingByUser(userId).orElse(null);
        if (pendingOrder != null) return response(pendingOrder);

        PaymentOrder saved = paymentRepository.create(new PaymentOrder(null, userId, plan.code(), plan.amount(),
                AppConfig.get("payment.currency"), plan.days(), "MHUB-TMP-" + System.nanoTime(),
                PaymentStatus.PENDING, null, null, null));
        String note = "MHUB-%06d".formatted(saved.getId());
        if (!paymentRepository.updateTransferNote(saved.getId(), note)) throw new SQLException("Could not assign payment transfer note");
        saved.setTransferNote(note);
        return response(saved);
    }

    public PaymentOrderResponse getOrder(int userId, long orderId) throws SQLException {
        PaymentOrder order = paymentRepository.findById(orderId).orElseThrow(() -> new IllegalArgumentException("Payment order not found"));
        if (order.getUserId() != userId) throw new SecurityException("Payment order does not belong to this user");
        return response(order);
    }

    public PagedResponse<PaymentOrderResponse> listMyOrders(int userId, int page, int size) throws SQLException {
        int offset = Pagination.offset(page, size);
        List<PaymentOrderResponse> items = paymentRepository.findByUser(userId, size, offset).stream().map(this::response).toList();
        return new PagedResponse<>(items, paymentRepository.countByUser(userId), page, size);
    }

    public PaymentOrderResponse markPaid(int userId, long orderId) throws SQLException {
        PaymentOrder order = ownedOrder(userId, orderId);
        if (order.getStatus() == PaymentStatus.CONFIRMED) return response(order);
        if (order.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalArgumentException("Only pending payment orders can be activated");
        }
        User user = userRepository.findById(order.getUserId()).orElseThrow(() -> new IllegalArgumentException("Payment user not found"));
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = user.getPremiumUntil() != null && user.getPremiumUntil().isAfter(now) ? user.getPremiumUntil() : now;
        LocalDateTime premiumUntil = start.plusDays(order.getPremiumDays());
        if (!paymentRepository.confirmAndActivate(orderId, user.getId(), premiumUntil, now)) {
            throw new IllegalArgumentException("Payment order was already processed");
        }
        order.setStatus(PaymentStatus.CONFIRMED); order.setConfirmedBy(null); order.setConfirmedAt(now);
        return response(order);
    }

    private PaymentOrder ownedOrder(int userId, long orderId) throws SQLException {
        PaymentOrder order = paymentRepository.findById(orderId).orElseThrow(() -> new IllegalArgumentException("Payment order not found"));
        if (order.getUserId() != userId) throw new SecurityException("Payment order does not belong to this user");
        return order;
    }

    private PaymentOrderResponse response(PaymentOrder order) { return PaymentOrderResponse.fromEntity(order, buildQr(order)); }

    private String buildQr(PaymentOrder order) {
        String staticImage = AppConfig.get("payment.qr.static-image-url");
        if (staticImage != null && !staticImage.isBlank()) return staticImage.trim();
        String bank = AppConfig.get("payment.qr.vietqr.bank");
        String account = AppConfig.get("payment.qr.vietqr.account");
        if (bank == null || bank.isBlank() || account == null || account.isBlank()) return null;
        String name = AppConfig.get("payment.qr.vietqr.account-name");
        return "https://img.vietqr.io/image/%s-%s-compact2.png?amount=%d&addInfo=%s&accountName=%s".formatted(
                encode(bank), encode(account), order.getAmount(), encode(order.getTransferNote()), encode(name == null ? "" : name));
    }

    private Map<String, Plan> plans() {
        Map<String, Plan> result = new LinkedHashMap<>();
        String configured = AppConfig.get("payment.plans");
        if (configured == null) return result;
        for (String item : configured.split(",")) {
            String[] parts = item.trim().split(":");
            if (parts.length != 3) continue;
            try { Plan plan = new Plan(normalizePlan(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2])); result.put(plan.code(), plan); }
            catch (NumberFormatException ignored) { }
        }
        return result;
    }
    private String normalizePlan(String code) { return code == null ? "" : code.trim().toUpperCase(Locale.ROOT); }
    private String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
    private record Plan(String code, int amount, int days) { }
}
