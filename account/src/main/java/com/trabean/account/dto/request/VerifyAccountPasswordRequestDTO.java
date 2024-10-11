package com.trabean.account.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class VerifyAccountPasswordRequestDTO {

    @JsonProperty("password")
    private String password;
}