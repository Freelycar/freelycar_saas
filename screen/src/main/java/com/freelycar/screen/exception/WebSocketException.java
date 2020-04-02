package com.freelycar.screen.exception;

import com.freelycar.screen.enums.ResultEnum;

/**
 * @author pyt
 * @date 2020/3/31 15:14
 * @email 2630451673@qq.com
 * @desc
 */
public class WebSocketException extends RuntimeException {
    private Integer code;

    public WebSocketException(ResultEnum resultEnum) {
        super(resultEnum.getMsg());
        this.code = resultEnum.getCode();
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }
}
