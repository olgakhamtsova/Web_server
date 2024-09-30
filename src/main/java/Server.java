import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.IOException;
import java.net.Socket;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


class Server {

    private Map<String, Map<String, Handler1>> handlers = new HashMap<>();
    private ExecutorService executorService;


    public Server() {
        this.executorService = Executors.newFixedThreadPool(64);
        this.handlers = new HashMap<>();
    }


    public void addHandler(String method, String path, Handler1 handler) {
        if (!handlers.containsKey(method)) {
            handlers.put(method, new HashMap<>());
        }
        handlers.get(method).put(path, handler);
    }

    public void start(int port) {
        try (var serverSocket = new ServerSocket(port)) {
            System.out.println("Server started. Port: " + port);
            while (true) {
                var socket = serverSocket.accept();
                executorService.submit(() -> run(socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            executorService.shutdown();
        }
    }

    public HashMap<String, Map<String, Handler1>> getHandlers() {
        return (HashMap<String, Map<String, Handler1>>) handlers;
    }

    public void run(Socket socket) {
        try (var in = new BufferedInputStream(socket.getInputStream());
             var out = new BufferedOutputStream(socket.getOutputStream())) {

            // лимит на request line + заголовки
            final var limit = 4096;

            in.mark(limit);
            final var buffer = new byte[limit];
            final var read = in.read(buffer);

            // ищем request line
            final var requestLineDelimiter = new byte[]{'\r', '\n'};
            final var requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
            if (requestLineEnd == -1) {
                badRequest(out);
            }

            var requestLine = readLine(in);
            var parts = requestLine.split(" ");
            if (parts.length != 3) {
                badRequest(out);
                return;
            }


            var method = parts[0];
            var path = parts[1];
            var headers = new HashMap<String, String>();

            if (!path.startsWith("/")) {
                badRequest(out);
            }
            System.out.println(path);

            // ищем заголовки
            final var headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
            final var headersStart = requestLineEnd + requestLineDelimiter.length;
            final var headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
            if (headersEnd == -1) {
                badRequest(out);
                //continue;
            }
            // отматываем на начало буфера
            in.reset();
            // пропускаем requestLine
            in.skip(headersStart);

            var path1 = path.contains("?") ? path.substring(0, path.indexOf("?")) : path;

            List<NameValuePair> params = URLEncodedUtils.parse(new URI(path1), StandardCharsets.UTF_8);
            //var headers = new HashMap<String, String>();
            String header = null;
            while (!(header = readLine(in)).isEmpty()) {
                var headerParts = header.split(": ", 2);
                if (headerParts.length == 2) {
                    headers.put(headerParts[0], headerParts[1]);
                }
            }
            int contentLength = Integer.parseInt(headers.getOrDefault("Content-Length", "0"));

            InputStream body = new ByteArrayInputStream(in.readNBytes(contentLength));


            Request request = new Request(method, path, headers, body);

            Handler1 handler = handlers.getOrDefault(method, new HashMap<>()).get(path1);
            if (handler != null) {
                handler.handle(request, out);
            } else {
                out.write(("HTTP/1.1 404 Not Found\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n\r\n").getBytes());
                out.flush();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }


    }

    private void badRequest(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 400 Bad Request\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }

    private int indexOf(byte[] array, byte[] target, int start, int max) {
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

    private String readLine(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        int ch;
        while ((ch = in.read()) != -1 && ch != '\n') {
            if (ch != '\r') {
                sb.append((char) ch);
            }
        }
        return sb.toString();
    }


}


