package com.vmware.config;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation used to describe workflow actions.
 * Allows workflow help to print meaningful information.
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface ActionDescription {
    String value();
    boolean ignoreConfigValuesInSuperclass() default false;
}
