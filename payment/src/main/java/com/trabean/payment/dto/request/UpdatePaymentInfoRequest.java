package com.trabean.payment.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePaymentInfoRequest {
    private String userKey;
    private Long accountId; // 돈 빠져나가는 계좌
    private Long merchantId;
    private Long krwAmount;
    private Double foreignAmount;
}
