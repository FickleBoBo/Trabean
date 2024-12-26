package com.trabean.account.dto.response.account;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RecentTransactionListResponseDTO {

    @JsonProperty("accountList")
    private List<Info> accountList;

    @Builder
    @Getter
    @EqualsAndHashCode
    public static class Info {
        private Long accountId;
        private String accountNo;
        private String adminName;
        private String bankName;
    }
}
