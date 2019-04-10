package ru.byprogminer.Lab5_Programming.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CommandHandler {

    String NULL = "\0";

    String description() default NULL;
    String usage() default NULL;
}
