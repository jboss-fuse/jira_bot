package io.syndesis.tools;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

import javax.activation.MimetypesFileTypeMap;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class DirectoryHandler implements HttpHandler
{
    private String currentDir;
    public static String WEBCONTEXT = "/data";

    public DirectoryHandler(String path)
    {
        super();
        currentDir = path;
    }

    @Override
    public void handle(HttpExchange request) throws IOException
    {
        Headers headers = request.getRequestHeaders();
        URI uri = request.getRequestURI();
        String path = uri.getPath().substring(WEBCONTEXT.length());

        File requestedFile = new File(currentDir, path);

        if (!requestedFile.exists()) {
            String response = "Not Found!";
            request.sendResponseHeaders(404, response.length());

            try (OutputStream os = request.getResponseBody()) {
                os.write(response.getBytes());
            }

            return;
        }

        if (requestedFile.isFile()) {
            byte[] bytearray  = new byte [(int)requestedFile.length()];
            FileInputStream fis = new FileInputStream(requestedFile);
            try (BufferedInputStream bis = new BufferedInputStream(fis)) {
                bis.read(bytearray, 0, bytearray.length);
            }

            Headers responseHeader = request.getResponseHeaders();
            responseHeader.add("Content-Type",
                    new MimetypesFileTypeMap().getContentType(requestedFile));
            request.sendResponseHeaders(200, requestedFile.length());

            try (OutputStream os = request.getResponseBody()) {
                os.write(bytearray, 0, bytearray.length);
            }
        }

        else if (requestedFile.isDirectory()) {
            String[] fileArray = requestedFile.list();
            StringBuilder response = new StringBuilder();
            response.append("<html><head><title>ls</title>" +
                    "<style type=\"text/css\">" +
                    " body {font-family: Monospace; font-size: 14px;}" +
                    "</style>" +
                    "</head><body>");

            for (String f : fileArray) {
                File currentFile = new File(requestedFile, f);

                if (currentFile.isDirectory())
                    response.append("D");
                else
                    response.append("&nbsp;");

                response.append("&nbsp;&nbsp;&nbsp");
                response.append("<a href=\""+WEBCONTEXT+"/" + new File(path, f) + "\">");
                response.append(f + "</a>");
                response.append("<br>");
            }

            response.append("</body></html>");

            request.sendResponseHeaders(200, response.length());

            try (OutputStream os = request.getResponseBody()) {
                os.write(response.toString().getBytes());
            }
        }
    }
}