package com.trabean.account.dto.request.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
public class UpdateTravelAccountPasswordRequestDTO {

    @JsonProperty("password")
    private String password;

    @JsonProperty("domesticAccountId")
    private Long domesticAccountId;

    @JsonProperty("foreignAccountIdList")
    private List<Long> foreignAccountIdList;
}
