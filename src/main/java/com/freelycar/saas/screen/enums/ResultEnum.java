package com.freelycar.saas.screen.enums;

/**
 * @author pyt
 * @date 2020/3/19 17:16
 * @email 2630451673@qq.com
 * @desc
 */
public enum ResultEnum {
    UNKONWN_ERROR(-1,"未知错误"),
    SUCCESS(0,"成功"),
    WEBSOCKET_ERROR(101,"websocket出错");
    private Integer code;
    private String msg;

    ResultEnum(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Integer getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
