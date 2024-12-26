package com.trabean.account.dto.response.domesticTravelAccount;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.trabean.account.domain.UserAccountRelation.UserRole;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DomesticTravelAccountMemberListResponseDTO {

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
