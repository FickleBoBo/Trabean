package com.trabean.external.msa.user.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class UpdateMainAccountIdRequestDTO {

    @JsonProperty("userId")
    private Long userId;

    @JsonProperty("mainAccountId")
    private Long mainAccountId;
}
