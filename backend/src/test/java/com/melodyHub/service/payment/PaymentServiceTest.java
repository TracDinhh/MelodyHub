package com.melodyHub.service.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.melodyHub.dto.response.PaymentOrderResponse;
import com.melodyHub.entity.PaymentOrder;
import com.melodyHub.entity.PaymentStatus;
import com.melodyHub.entity.User;
import com.melodyHub.exception.PremiumAlreadyActiveException;
import com.melodyHub.repository.PaymentRepository;
import com.melodyHub.repository.UserRepository;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PaymentServiceTest {
    @Test
    void createOrderReturnsTheExistingPendingOrderForTheUser() throws SQLException {
        StubPaymentRepository repository = new StubPaymentRepository();
        repository.pendingOrder = Optional.of(order(15L, 12, "MONTHLY"));
        PaymentService service = new PaymentService(repository, new StubUserRepository(user(12, null)));

        PaymentOrderResponse response = service.createOrder(12, "QUARTERLY");

        assertEquals(15L, response.getId());
        assertEquals("MONTHLY", response.getPlanCode());
        assertEquals(0, repository.createCalls);
    }

    @Test
    void createOrderCreatesAnOrderWhenTheUserHasNoPendingOrder() throws SQLException {
        StubPaymentRepository repository = new StubPaymentRepository();
        repository.createdOrder = order(16L, 12, "MONTHLY");
        repository.createdOrder.setTransferNote("temporary");
        PaymentService service = new PaymentService(repository, new StubUserRepository(user(12, null)));

        PaymentOrderResponse response = service.createOrder(12, "MONTHLY");

        assertEquals(1, repository.createCalls);
        assertEquals("MHUB-000016", repository.updatedNote);
        assertEquals("MHUB-000016", response.getTransferNote());
    }

    @Test
    void createOrderRejectsAUserWhosePremiumIsStillActive() {
        StubPaymentRepository repository = new StubPaymentRepository();
        PaymentService service = new PaymentService(
                repository,
                new StubUserRepository(user(12, LocalDateTime.now().plusDays(10)))
        );

        PremiumAlreadyActiveException exception = assertThrows(
                PremiumAlreadyActiveException.class,
                () -> service.createOrder(12, "MONTHLY")
        );

        assertEquals("Premium is already active for this account", exception.getMessage());
        assertEquals(0, repository.createCalls);
    }

    @Test
    void createOrderAllowsAUserWhosePremiumHasExpired() throws SQLException {
        StubPaymentRepository repository = new StubPaymentRepository();
        repository.createdOrder = order(17L, 12, "MONTHLY");
        PaymentService service = new PaymentService(
                repository,
                new StubUserRepository(user(12, LocalDateTime.now().minusDays(1)))
        );

        PaymentOrderResponse response = service.createOrder(12, "MONTHLY");

        assertEquals(17L, response.getId());
        assertEquals(1, repository.createCalls);
    }

    @Test
    void markPaidImmediatelyActivatesPremiumForTheOrderOwner() throws SQLException {
        StubPaymentRepository paymentRepository = new StubPaymentRepository();
        paymentRepository.orderById = Optional.of(order(18L, 12, "MONTHLY"));
        StubUserRepository userRepository = new StubUserRepository(user(12, null));
        PaymentService service = new PaymentService(paymentRepository, userRepository);

        PaymentOrderResponse response = service.markPaid(12, 18L);

        assertEquals(PaymentStatus.CONFIRMED, response.getStatus());
        assertNotNull(response.getConfirmedAt());
        assertEquals(12, paymentRepository.activatedUserId);
        assertTrue(paymentRepository.activatedPremiumUntil.isAfter(LocalDateTime.now().plusDays(29)));
    }

    @Test
    void markPaidDoesNotExtendPremiumTwiceForAConfirmedOrder() throws SQLException {
        StubPaymentRepository paymentRepository = new StubPaymentRepository();
        PaymentOrder confirmedOrder = order(19L, 12, "MONTHLY");
        confirmedOrder.setStatus(PaymentStatus.CONFIRMED);
        paymentRepository.orderById = Optional.of(confirmedOrder);
        StubUserRepository userRepository = new StubUserRepository(user(12, null));
        PaymentService service = new PaymentService(paymentRepository, userRepository);

        PaymentOrderResponse response = service.markPaid(12, 19L);

        assertEquals(PaymentStatus.CONFIRMED, response.getStatus());
        assertEquals(0, paymentRepository.activatedUserId);
    }

    private static User user(int id, LocalDateTime premiumUntil) {
        User user = new User();
        user.setId(id);
        user.setPremiumUntil(premiumUntil);
        return user;
    }

    private static PaymentOrder order(long id, int userId, String planCode) {
        return new PaymentOrder(
                id,
                userId,
                planCode,
                29_000,
                "VND",
                30,
                "MHUB-%06d".formatted(id),
                PaymentStatus.PENDING,
                null,
                null,
                LocalDateTime.of(2026, 8, 17, 10, 0)
        );
    }

    private static final class StubPaymentRepository extends PaymentRepository {
        private Optional<PaymentOrder> pendingOrder = Optional.empty();
        private Optional<PaymentOrder> orderById = Optional.empty();
        private PaymentOrder createdOrder;
        private int createCalls;
        private String updatedNote;
        private int activatedUserId;
        private LocalDateTime activatedPremiumUntil;

        @Override
        public Optional<PaymentOrder> findPendingByUser(int userId) {
            return pendingOrder;
        }

        @Override
        public PaymentOrder create(PaymentOrder order) {
            createCalls++;
            return createdOrder;
        }

        @Override
        public boolean updateTransferNote(long id, String note) {
            updatedNote = note;
            return true;
        }

        @Override
        public Optional<PaymentOrder> findById(long id) {
            return orderById;
        }

        @Override
        public boolean confirmAndActivate(long orderId, int userId, LocalDateTime premiumUntil, LocalDateTime confirmedAt) {
            activatedUserId = userId;
            activatedPremiumUntil = premiumUntil;
            return true;
        }
    }

    private static final class StubUserRepository extends UserRepository {
        private final Optional<User> user;

        private StubUserRepository(User user) {
            this.user = Optional.ofNullable(user);
        }

        @Override
        public Optional<User> findById(int id) {
            return user;
        }
    }
}
