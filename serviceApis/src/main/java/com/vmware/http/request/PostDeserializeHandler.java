package com.vmware.http.request;

import java.lang.reflect.Method;

import com.vmware.util.ReflectionUtils;

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
