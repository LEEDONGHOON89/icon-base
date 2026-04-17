package com.itmasters.icon.api.common.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResponseOk<T> {
    private String message = "OK";
    private T data;

    public ResponseOk() {}

    public ResponseOk(String message, T data) {
        this.message = message;
        this.data = data;
    }

    public static ResponseOk<Void> ok() {
        return new ResponseOk<>();
    }

    public static <T> ResponseOk<T> of(String message, T data) {
        return new ResponseOk<>(message, data);
    }
}
