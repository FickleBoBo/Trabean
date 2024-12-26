package com.trabean.external.ssafy.common;

import com.trabean.external.ssafy.util.ResponseHeader;

/**
 * SsafyApiResponseDTO를 생성하는 팩터리 클래스
 */
public class SsafyApiResponseDTOFactory {

    public static SsafyApiResponseDTO create(ResponseHeader responseHeader) {
        return SsafyApiResponseDTO.builder()
                .responseCode(responseHeader.getResponseCode())
                .responseMessage(responseHeader.getResponseMessage())
                .build();
    }
}
