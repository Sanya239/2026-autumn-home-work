package company.vk.edu.distrib.compute.nixxx11.urlshortener.http;

import java.io.IOException;

import com.sun.net.httpserver.Request;

@FunctionalInterface
public interface SimpleHandler {
  Response handle(Request request, String content) throws IOException;
}
