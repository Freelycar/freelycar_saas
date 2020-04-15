package com.freelycar.saas.screen.utils;


import com.freelycar.saas.screen.domain.Result;

/**
 * @author pyt
 * @date 2020/3/19 15:47
 * @email 2630451673@qq.com
 * @desc
 */
public class ResultUtil {
    public static Result success(Object object) {
        Result result = new Result();
        result.setCode(0);
        result.setMsg("成功");
        result.setData(object);
        return result;
    }

    public static Result success() {
        return success(null);
    }

    public static Result error(Integer code, String msg) {
        Result result = new Result();
        result.setCode(code);
        result.setMsg(msg);
        return result;
    }
}
