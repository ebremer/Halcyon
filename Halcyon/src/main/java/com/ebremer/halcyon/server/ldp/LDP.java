package com.ebremer.halcyon.server.ldp;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.eclipse.jetty.ee10.servlet.DefaultServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LDP extends DefaultServlet {
    private static final Logger logger = LoggerFactory.getLogger(LDP.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        super.doGet(request, response);
        System.out.println(request.getRequestURI()+" ---->  "+request.getContentType());
    }
       
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        logger.debug("{} ----> {}",request.getRequestURI(),request.getContentType());
        switch (request.getContentType()) {
            case "text/turtle":
                break;
            case "application/json":
                System.out.println(request.getContentLengthLong());
                System.out.println(Utils.getBody(request));
                break;
            case "application/octet-stream":
                Utils.UploadFile(request);
                break;
            default:
        }
    }
}

/*
                Optional<URI> xparent = PathMapper.getPathMapper().http2file(HalcyonSettings.getSettings().getHostName()+request.getRequestURI());
                if (xparent.isPresent()) {
                    URI parent = xparent.get();
                    String fileName = request.getHeader("File-Name");
                    long offset = Long.parseLong(request.getHeader("Chunk-Offset"));
                    File outputFile = Path.of(parent.getPath().substring(1), fileName).toFile();
                    outputFile.getParentFile().mkdirs();
                    try (InputStream inputStream = request.getInputStream()) {
                        if (offset == 0 && outputFile.exists()) {
                            outputFile.delete();
                        }
                        try (RandomAccessFile raf = new RandomAccessFile(outputFile, "rw")) {
                            raf.seek(offset);
                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            while ((bytesRead = inputStream.read(buffer)) != -1) {
                                raf.write(buffer, 0, bytesRead);
                            }
                        }
                    }
                } else {
                    System.out.println("NOT FOUND!!!");
                }
*/