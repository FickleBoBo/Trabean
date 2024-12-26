package com.trabean.account.dto.response.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserKeyResponseDTO {

    @JsonProperty("userKey")
    private String userKey;
}
