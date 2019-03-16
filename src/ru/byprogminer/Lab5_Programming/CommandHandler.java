package ru.byprogminer.Lab5_Programming;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CommandHandler {

    String NULL = "\0";

    String description() default NULL;
    String usage() default NULL;
}
