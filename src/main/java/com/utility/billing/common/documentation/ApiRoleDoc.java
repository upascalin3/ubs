package com.utility.billing.common.documentation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Documents which roles can access an endpoint in Swagger/OpenAPI.
 * Use on public endpoints or to supplement {@code @PreAuthorize}.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiRoleDoc {

    String[] value();

    String description() default "";
}
