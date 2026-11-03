package company.vk.edu.distrib.compute.mrglaster.urlshortener.controller.enums;

public enum StatusCode {
    HTTP_OK(200),
    HTTP_BAD_REQUEST(400),
    HTTP_INTERNAL_SERVER_ERROR(500),
    HTTP_UNPROCESSABLE_ENTITY(422),
    HTTP_NOT_FOUND(404),
    HTTP_ACCEPTED(202),
    HTTP_UNAUTHORIZED(401),
    HTTP_CREATED(201),
    HTTP_MOVED_PERMANENTLY(301);

    private final int statusCode;

    StatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
