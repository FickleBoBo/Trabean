package com.trabean.external.ssafy.domestic.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.trabean.util.RequestHeader;
import lombok.Builder;
import lombok.Getter;

/**
 * SSAFY 금융 API p.66 - 계좌 거래 내역 조회 (단건) requestDTO
 */
@Builder
@Getter
public class InquireTransactionHistoryRequestDTO {

    @JsonProperty("Header")
    private RequestHeader header;

    @JsonProperty("accountNo")
    private String accountNo;

    @JsonProperty("transactionUniqueNo")
    private Long transactionUniqueNo;
}