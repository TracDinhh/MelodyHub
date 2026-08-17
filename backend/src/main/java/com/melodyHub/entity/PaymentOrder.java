package com.melodyHub.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentOrder {
    private Long id;
    private Integer userId;
    private String planCode;
    private int amount;
    private String currency;
    private int premiumDays;
    private String transferNote;
    private PaymentStatus status;
    private Integer confirmedBy;
    private LocalDateTime confirmedAt;
    private LocalDateTime createdAt;
}
