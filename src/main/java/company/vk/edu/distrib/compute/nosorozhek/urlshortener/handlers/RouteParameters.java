package company.vk.edu.distrib.compute.nosorozhek.urlshortener.handlers;

import java.util.regex.Matcher;

public record RouteParameters(Matcher values) {
    public static final String ID = "ID";

    public String required(String name) {
        String value = values.group(name);
        if (value == null) {
            throw new IllegalArgumentException("Missing route parameter: " + name);
        }
        return value;
    }
}
