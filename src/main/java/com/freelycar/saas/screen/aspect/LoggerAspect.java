package com.freelycar.saas.screen.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * @author pyt
 * @date 2020/3/31 11:31
 * @email 2630451673@qq.com
 * @desc
 */
@Aspect
@Service
public class LoggerAspect {
    private final static Logger logger = LoggerFactory.getLogger(LoggerAspect.class);

    private String getMethodName(JoinPoint joinPoint){
        return  joinPoint.getSignature().getDeclaringTypeName() + '.' + joinPoint.getSignature().getName();
    }

    @Pointcut("execution(public * com.freelycar.saas.screen.controller.*.*(..))")
    public void logPointcut() {
    }

    @Before(value = "logPointcut()")
    public void doBefore(JoinPoint joinPoint) {
        logger.info("执行："+ getMethodName(joinPoint)+"开始");
        logger.info("args={}", joinPoint.getArgs());
    }


    @AfterReturning(returning = "object", pointcut = "logPointcut()")
    public void doAfterReturning(JoinPoint joinPoint,Object object) {
        logger.info("response={}", object);
        logger.info("执行:"+getMethodName(joinPoint)+"结束");
    }

    @AfterThrowing(pointcut = "logPointcut()", throwing = "ex")
    public void doAfterThrowing(JoinPoint joinPoint, Exception ex) {
        logger.error("执行："+getMethodName(joinPoint)+"发生异常：", ex);
    }
}
