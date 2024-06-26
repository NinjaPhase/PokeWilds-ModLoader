package com.pkmngen.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Mod {
    String name();
    String author();
    int[] version();
    String[] requirements() default {};
    String[] optional() default {};
}
