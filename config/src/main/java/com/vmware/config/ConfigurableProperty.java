package com.vmware.config;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotates workflow configuration properties in WorkflowConfig
 */
@Target(FIELD)
@Retention(RUNTIME)
public @interface ConfigurableProperty {
    static final String NO_COMMAND_LINE_OVERRIDES = "No command line overrides";

    String commandLine() default NO_COMMAND_LINE_OVERRIDES;
    String help();
    String gitConfigProperty() default "";

}
