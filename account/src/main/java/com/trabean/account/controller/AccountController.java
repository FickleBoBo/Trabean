package com.trabean.account.controller;

import com.trabean.account.dto.request.AccountNoRequestDTO;
import com.trabean.account.dto.request.UserRoleRequestDTO;
import com.trabean.account.dto.response.AccountNoResponseDTO;
import com.trabean.account.dto.response.UserRoleResponseDTO;
import com.trabean.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/accounts")
public class AccountController {

    @Autowired
    private final AccountService accountService;

    @PostMapping("/get-account-number")
    public ResponseEntity<AccountNoResponseDTO> getAccountNo(@RequestBody AccountNoRequestDTO requestDTO){
        Long accountId = requestDTO.getAccountId();

        String accountNo = accountService.getAccountNoById(accountId);

        if(accountNo != null){
            AccountNoResponseDTO responseDTO = AccountNoResponseDTO.builder()
                    .accountNo(accountNo)
                    .message("성공")
                    .build();
            return ResponseEntity.ok(responseDTO);
        }
        else{
            AccountNoResponseDTO responseDTO = AccountNoResponseDTO.builder()
                    .message("해당 계좌를 찾을 수 없습니다.")
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
        }
    }

    @PostMapping("/get-user-role")
    public ResponseEntity<UserRoleResponseDTO> getUserRole(@RequestBody UserRoleRequestDTO requestDTO){
        Long userId = requestDTO.getUserId();
        Long accountId = requestDTO.getAccountId();

        String userRole = accountService.getUserRoleByUserIdAndAccountId(userId, accountId);

        if(userRole != null){
            UserRoleResponseDTO responseDTO = UserRoleResponseDTO.builder()
                    .userRole(userRole)
                    .message("성공")
                    .build();
            return ResponseEntity.ok(responseDTO);
        }
        else{
            UserRoleResponseDTO responseDTO = UserRoleResponseDTO.builder()
                    .message("잘못된 요청입니다.")
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
        }
    }
}
