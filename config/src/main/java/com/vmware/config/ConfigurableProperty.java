package com.vmware.config;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.lang.annotation.ElementType.FIELD;

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
    String methodNameForValueCalculation() default "";

}
