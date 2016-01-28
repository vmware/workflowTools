package com.vmware.http.request;

import com.vmware.util.exception.RuntimeReflectiveOperationException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class PostDeserializeHandler {

    public void invokePostDeserializeMethods(Object createdObject) {
        for (Method method : createdObject.getClass().getMethods()) {
            if (method.getAnnotation(PostDeserialize.class) == null) {
                continue;
            }

            try {
                method.invoke(createdObject);
            } catch (InvocationTargetException | IllegalAccessException e) {
                throw new RuntimeReflectiveOperationException(e);
            }
        }
    }
}
