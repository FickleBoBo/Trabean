package com.trabean.account.dto.response.personalAccount;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PersonalAccountCreatedDateResponseDTO {

    @JsonProperty("accountCreatedDate")
    private String accountCreatedDate;
}
