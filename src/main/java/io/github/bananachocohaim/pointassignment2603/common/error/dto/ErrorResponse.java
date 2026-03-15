package io.github.bananachocohaim.pointassignment2603.common.error.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.BindingResult;

import io.github.bananachocohaim.pointassignment2603.common.error.ErrorCode;

public record ErrorResponse(
    LocalDateTime localDateTime,
    int status,                 //상태 값
    String errorCode,            //에러 코드
    String errorMessage,        //에러 메시지
    List<FieldError> paramErrors    //파라미터 오류시 상세 리스트
) {
    public static ErrorResponse of (ErrorCode code, BindingResult bindingResult){
        return new ErrorResponse(LocalDateTime.now(),  code.getStatus(), code.getErrorCode(), code.getErrorMessage(), FieldError.of(bindingResult));
    }

    //필드, 인입된 벨류, 사유
    public record FieldError(String param, String value, String message) {
        public static List<FieldError> of(BindingResult bindingResult) {
            return bindingResult.getFieldErrors().stream()
                .map(err -> new FieldError(
                    err.getField(),
                    err.getRejectedValue() == null ? "" : err.getRejectedValue().toString(),
                    StringUtils.defaultIfBlank(err.getDefaultMessage(), "Invalid Param")
                ))
                .collect(Collectors.toList());
        }
    }

}
