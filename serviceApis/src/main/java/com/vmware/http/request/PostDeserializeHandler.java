package com.vmware.http.request;

import com.vmware.util.ReflectionUtils;

import java.lang.reflect.Method;

public class PostDeserializeHandler {

    public void invokePostDeserializeMethods(Object createdObject) {
        for (Method method : createdObject.getClass().getMethods()) {
            if (!method.isAnnotationPresent(PostDeserialize.class)) {
                continue;
            }

            ReflectionUtils.invokeMethod(method, createdObject);
        }
    }
}
