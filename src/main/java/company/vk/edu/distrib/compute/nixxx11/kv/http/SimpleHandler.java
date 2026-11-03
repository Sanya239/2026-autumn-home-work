package company.vk.edu.distrib.compute.nixxx11.kv.http;

import java.io.IOException;

import com.sun.net.httpserver.Request;

@FunctionalInterface
public interface SimpleHandler {
  Response handle(Request request, byte[] content) throws IOException;
}
