package cn.edu.zzu.airweb.common;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * API请求日志切面
 * 记录每个API请求的调用方法、参数和响应时间
 */
@Slf4j
@Aspect
@Component
public class ApiLog {

    @Pointcut("execution(* cn.edu.zzu.airweb.controller..*(..))")
    public void controllerPointcut() {}

    @Around("controllerPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName();

        try {
            Object result = joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("[API] {} 耗时: {}ms", methodName, elapsed);
            return result;
        } catch (Throwable e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("[API] {} 异常, 耗时: {}ms, 错误: {}", methodName, elapsed, e.getMessage());
            throw e;
        }
    }
}
