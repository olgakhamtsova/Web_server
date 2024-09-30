import org.apache.http.NameValuePair;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    public static final String GET = "GET";
    public static final String POST = "POST";
    static final int PORT = 8080;
    static ExecutorService executeIt = Executors.newFixedThreadPool(64);

    public static void main(String[] args) {
        final var server = new Server();

        server.addHandler("GET", "/messages", (request, responseStream) -> {
            String responseBody = "{\"messages\": [\"Hello, World!\"]}";
            responseStream.write(("HTTP/1.1 200 OK\r\n" +
                    "Content-Type: application/json\r\n" +
                    "Content-Length: " + responseBody.length() + "\r\n" +
                    "Connection: close\r\n" +
                    "\r\n").getBytes());
            responseStream.write(responseBody.getBytes());
            responseStream.flush();
        });

        server.addHandler("GET", "/messages", (request, responseStream) -> {
            List<NameValuePair> lastValue = (List<NameValuePair>) request.getQueryParam("last");
            String responseBody = "You have sent a GET request!";
            responseStream.write(("HTTP/1.1 200 OK\r\n" +
                    "Content-Length: " + responseBody.length() + "\r\n" +
                    "Connection: close\r\n\r\n").getBytes());
            responseStream.write(responseBody.getBytes());
            responseStream.flush();
        });
        server.addHandler("POST", "/messages", (request, responseStream) -> {
            // Здесь вы можете читать из request.getBody() и обрабатывать данные
            responseStream.write(("HTTP/1.1 200 OK\r\n" +
                    "Connection: close\r\n" +
                    "\r\n").getBytes());
            responseStream.flush();
        });

        server.start(PORT);


    }

    private static Optional<String> extractHeader(List<String> headers, String header) {
        return headers.stream()
                .filter(o -> o.startsWith(header))
                .map(o -> o.substring(o.indexOf(" ")))
                .map(String::trim)
                .findFirst();
    }

    private static void badRequest(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 400 Bad Request\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }

    // from google guava with modifications
    private static int indexOf(byte[] array, byte[] target, int start, int max) {
        outer:
        for (int i = start; i < max - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }


}
