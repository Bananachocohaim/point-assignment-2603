package io.github.bananachocohaim.pointassignment2603.common.error;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class PointApiException extends RuntimeException {

    private ErrorCode errorCode;
}
