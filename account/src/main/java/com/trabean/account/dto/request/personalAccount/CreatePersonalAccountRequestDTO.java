package com.trabean.account.dto.request.personalAccount;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class CreatePersonalAccountRequestDTO {

    @JsonProperty("password")
    private String password;
}
