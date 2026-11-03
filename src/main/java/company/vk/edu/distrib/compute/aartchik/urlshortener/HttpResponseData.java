package company.vk.edu.distrib.compute.aartchik.urlshortener;

public record HttpResponseData(int status, String body) {
    public HttpResponseData(int status) {
        this(status, "");
    }
}
