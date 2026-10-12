package company.vk.edu.distrib.compute.allpandasarecute.urlshortener;

final class HttpConstants {
    static final String GET_METHOD = "GET";
    static final String POST_METHOD = "POST";
    static final String PUT_METHOD = "PUT";
    static final String DELETE_METHOD = "DELETE";

    static final int OK = 200;
    static final int CREATED = 201;
    static final int ACCEPTED = 202;
    static final int MOVED_PERMANENTLY = 301;
    static final int UNAUTHORIZED = 401;
    static final int NOT_FOUND = 404;
    static final int METHOD_NOT_ALLOWED = 405;
    static final int UNPROCESSABLE_CONTENT = 422;
    static final int INTERNAL_SERVER_ERROR = 500;

    private HttpConstants() {
    }
}
