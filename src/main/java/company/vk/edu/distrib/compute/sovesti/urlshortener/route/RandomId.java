package company.vk.edu.distrib.compute.sovesti.urlshortener.route;

import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Stream;

import company.vk.edu.distrib.compute.sovesti.urlshortener.handler.BadRequestException;

final class RandomId implements Supplier<String> {

    private final Random random = new Random();
    private static final String ALPHANUM = "qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM1234567890";
    private static final int SIZE = 10;

    @Override
    public String get() {
        return Stream.generate(this::randomChar)
            .limit(SIZE)
            .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
            .toString();
    }

    String throwIfInvalid(String id) {
        if (id.isEmpty()) {
            throw new BadRequestException(error(id));
        }
        if (!id.chars().allMatch(this::valid)) {
            throw new IllegalArgumentException(error(id));
        }
        return id;
    }

    private String error(String id) {
        return "Invalid id: %s".formatted(id);
    }

    private boolean valid(int character) {
        return ALPHANUM.chars().anyMatch(valid -> character == valid);
    }

    private char randomChar() {
        return ALPHANUM.charAt(random.nextInt(ALPHANUM.length()));
    }
}
