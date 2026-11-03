package company.vk.edu.distrib.compute.mperikov.urlshortener;

enum HttpStatus {
    OK(200),
    CREATED(201),
    ACCEPTED(202),
    MOVED(301),
    UNAUTHORIZED(401),
    NOT_FOUND(404),
    UNPROCESSABLE(422),
    SERVER_ERROR(500);

    private final int value;

    HttpStatus(int value) {
        this.value = value;
    }

    int value() {
        return value;
    }
}
