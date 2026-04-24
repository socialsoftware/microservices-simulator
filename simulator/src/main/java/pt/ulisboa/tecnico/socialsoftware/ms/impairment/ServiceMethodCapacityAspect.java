package pt.ulisboa.tecnico.socialsoftware.ms.impairment;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Aspect
@Component
public class ServiceMethodCapacityAspect {
    // ! TODO - this class should depend on if we are using capacityManager or not
    @Around("execution(public * pt.ulisboa.tecnico.socialsoftware.quizzes.microservices..service.*.*(..))")
    public Object applyServiceCapacity(ProceedingJoinPoint joinPoint) throws Throwable {
        CapacityManager manager = CapacityManager.getInstance();

        String methodName = joinPoint.getSignature().getName().toLowerCase();
        String microserviceName = resolveMicroserviceName(joinPoint.getTarget().getClass());
        String requestId = UUID.randomUUID().toString().substring(0, 8);

        try {
            manager.acquire(microserviceName, methodName, requestId);
            return joinPoint.proceed();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Interrupted while waiting for capacity for service method " + microserviceName + "." + methodName,
                    e);
        } finally {
            manager.release(microserviceName, methodName, requestId);
        }
    }

    private String resolveMicroserviceName(Class<?> serviceClass) {
        String packageName = serviceClass.getPackageName();
        String[] packageTokens = packageName.split("\\.");
        for (int i = 0; i < packageTokens.length - 1; i++) {
            if ("microservices".equals(packageTokens[i])) {
                return packageTokens[i + 1].toLowerCase();
            }
        }
        return null;
    }
}
