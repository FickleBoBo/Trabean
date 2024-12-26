package com.trabean.external.msa.user.client;

import com.trabean.config.FeignClientConfig;
import com.trabean.external.msa.user.dto.request.UpdateMainAccountIdRequestDTO;
import com.trabean.external.msa.user.dto.request.UserKeyRequestDTO;
import com.trabean.external.msa.user.dto.response.MainAccountIdResponseDTO;
import com.trabean.external.msa.user.dto.response.UserKeyResponseDTO;
import com.trabean.external.msa.user.dto.response.UserNameResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "user", configuration = FeignClientConfig.class)
public interface UserClient {

    /**
     * userId로 userKey 조회
     */
    @PostMapping("/api/user/getuserkey")
    UserKeyResponseDTO getUserKey(@RequestBody UserKeyRequestDTO requestDTO);

    /**
     * userId로 userName 조회
     */
    @GetMapping("/api/user/name/{userId}")
    UserNameResponseDTO getUserName(@PathVariable Long userId);

    /**
     * userId로 mainAccountId 조회
     */
    @GetMapping("/api/user/mainAccountId/{userId}")
    MainAccountIdResponseDTO getMainAccountId(@PathVariable Long userId);

    /**
     * mainAccountId 저장
     */
    @PostMapping("api/user/mainAccountId")
    void updateMainAccountId(@RequestBody UpdateMainAccountIdRequestDTO requestDTO);
}
