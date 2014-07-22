package com.vmware.config;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotation used to describe workflow actions.
 * Allows workflow help to print meaningful information.
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface ActionDescription {
    String value();
}
