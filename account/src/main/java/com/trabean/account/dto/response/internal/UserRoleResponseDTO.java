package com.trabean.account.dto.response.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.trabean.account.domain.UserAccountRelation.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserRoleResponseDTO {

    @JsonProperty("message")
    private String message;

    @JsonProperty("userRole")
    private UserRole userRole;
}
