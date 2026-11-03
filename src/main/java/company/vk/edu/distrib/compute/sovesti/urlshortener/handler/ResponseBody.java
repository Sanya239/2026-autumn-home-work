package company.vk.edu.distrib.compute.sovesti.urlshortener.handler;

import java.io.IOException;
import java.io.OutputStream;

public interface ResponseBody {

    long length();

    void write(OutputStream out) throws IOException;

    final class Empty implements ResponseBody {

        @Override
        public void write(OutputStream out) throws IOException {
            // ignore
        }

        @Override
        public long length() {
            return 0;
        }

    }

    record Plain(byte[] bytes) implements ResponseBody {

        public Plain(String encoded) {
            this(encoded.getBytes());
        }

        @Override
        public void write(OutputStream out) throws IOException {
            out.write(bytes);
            out.flush();
        }

        @Override
        public long length() {
            return bytes.length;
        }

    }
}
