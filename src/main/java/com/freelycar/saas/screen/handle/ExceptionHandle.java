package com.freelycar.saas.screen.handle;

import com.freelycar.saas.screen.domain.Result;
import com.freelycar.saas.screen.exception.WebSocketException;
import com.freelycar.saas.screen.utils.ResultUtil;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author pyt
 * @date 2020/3/31 15:11
 * @email 2630451673@qq.com
 * @desc  全局异常处理
 */
@ControllerAdvice
public class ExceptionHandle {
    @ExceptionHandler(value = Exception.class)
    @ResponseBody
    public Result handle(Exception e) {
        if (e instanceof WebSocketException){
            WebSocketException webSocketException = (WebSocketException) e;
            return ResultUtil.error(webSocketException.getCode(), webSocketException.getMessage());
        }
        return ResultUtil.error(100, e.getMessage());
    }
}
