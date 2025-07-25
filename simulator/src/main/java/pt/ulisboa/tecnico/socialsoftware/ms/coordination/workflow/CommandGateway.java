package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public abstract class CommandGateway {

    private final ApplicationContext applicationContext;

    @Autowired
    public CommandGateway(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public Object send(Command command) {

        try {
            String beanName = Character.toLowerCase(command.getServiceName().charAt(0))
                    + command.getServiceName().substring(1);

            Object serviceBean = applicationContext.getBean(beanName);

            String methodName = command.getClass().getSimpleName().replace("Command", "");
            methodName = Character.toLowerCase(methodName.charAt(0)) + methodName.substring(1);

            Method method = findMethod(serviceBean.getClass(), methodName, command.getClass());
            if (method == null) {
                throw new NoSuchMethodException("No method " + methodName + " found on " + serviceBean.getClass());
            }

            List<Object> args = new ArrayList<>();
            for (java.lang.reflect.Field field : command.getClass().getDeclaredFields()) {
                if (!field.getName().equals("rootAggregateId") &&
                        !field.getName().equals("forbiddenStates") &&
                        !field.getName().equals("semanticLock") &&
                        !field.getName().equals("unitOfWork") &&
                        !field.getName().equals("serviceName")) {
                    field.setAccessible(true);
                    Object fieldValue = field.get(command);
                    if (fieldValue != null) {
                        args.add(fieldValue);
                    }
                }
            }
            args.add(command.getUnitOfWork());

            return method.invoke(serviceBean, args.toArray());

        } catch (java.lang.reflect.InvocationTargetException e) {
            // Unwrap the target exception
            Throwable cause = e.getCause();
            if (cause instanceof SimulatorException) {
                // Pass through SimulatorException
                throw (SimulatorException) cause;
            } else {
                // Wrap other exceptions
                throw new RuntimeException("Failed to invoke service method: " + cause.getMessage(), cause);
            }
        } catch (SimulatorException e) {
            // Pass through SimulatorException
            throw e;
        } catch (Exception e) {
            // Wrap other exceptions
            throw new RuntimeException("Failed to invoke service method: " + e.getMessage(), e);
        }
    }

    private Method findMethod(Class<?> clazz, String methodName, Class<?> paramType) {
        try {
            return clazz.getMethod(methodName, paramType);
        } catch (NoSuchMethodException e) {
            for (Method m : clazz.getMethods()) {
                if (m.getName().equals(methodName)) {
                    return m;
                }
            }
        }
        return null;
    }
}
