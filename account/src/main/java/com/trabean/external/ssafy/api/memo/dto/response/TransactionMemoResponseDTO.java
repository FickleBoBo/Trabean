package com.trabean.external.ssafy.api.memo.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.trabean.external.ssafy.util.ResponseHeader;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * SSAFY 금융 API p.266 - 거래내역 메모 responseDTO
 */
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TransactionMemoResponseDTO {

    @JsonProperty("Header")
    private ResponseHeader header;

    @JsonProperty("REC")
    private REC rec;

    @Getter
    public static class REC {
        private Long memoUniqueNo;
        private String accountNo;
        private Long transactionUniqueNo;
        private String transactionMemo;
        private String created;
    }
}
