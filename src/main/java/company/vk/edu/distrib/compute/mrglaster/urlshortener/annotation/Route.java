package company.vk.edu.distrib.compute.mrglaster.urlshortener.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Route {
    String method();

    String path();

    boolean requiresAuthorization();
}
