package com.trabean.account.dto.request.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.trabean.account.domain.UserAccountRelation.UserRole;
import lombok.Getter;

import java.util.List;

@Getter
public class UpdateUserRoleRequestDTO {

    @JsonProperty("userId")
    private Long userId;

    @JsonProperty("domesticAccountId")
    private Long domesticAccountId;

    @JsonProperty("foreignAccountIdList")
    private List<Long> foreignAccountIdList;

    @JsonProperty("userRole")
    private UserRole userRole;
}
