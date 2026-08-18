package com.melodyHub.dto.response;

import com.melodyHub.entity.PaymentOrder;
import com.melodyHub.entity.PaymentStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentOrderResponse {
    private Long id;
    private String planCode;
    private int amount;
    private String currency;
    private int premiumDays;
    private String transferNote;
    private PaymentStatus status;
    private LocalDateTime confirmedAt;
    private LocalDateTime createdAt;
    private String qrImageUrl;

    public static PaymentOrderResponse fromEntity(PaymentOrder order, String qrImageUrl) {
        return new PaymentOrderResponse(
                order.getId(), order.getPlanCode(), order.getAmount(), order.getCurrency(),
                order.getPremiumDays(), order.getTransferNote(), order.getStatus(),
                order.getConfirmedAt(), order.getCreatedAt(), qrImageUrl
        );
    }
}
