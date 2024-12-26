package com.trabean.account.dto.response.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.trabean.account.domain.UserAccountRelation.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TravelAccountMembersResponseDTO {

    @JsonProperty("userId")
    private Long userId;

    @JsonProperty("memberCount")
    private Long memberCount;

    @JsonProperty("members")
    private List<Member> members;

    @Builder
    @Getter
    public static class Member {
        private Long userId;
        private String userName;
        private UserRole role;
    }
}
