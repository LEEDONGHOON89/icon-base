package com.itmasters.icon.api.common.response;

import lombok.Getter;

import java.util.List;

@Getter
public class ResponseList<T> {
    private Long total;
    private List<T> data;

    public ResponseList(Long total, List<T> data) {
        this.total = total;
        this.data = data;
    }

    public ResponseList(List<T> data) {
        this.total = (long) data.size();
        this.data = data;
    }
}
