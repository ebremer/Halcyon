package com.ebremer.halcyon.server.ldp;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import org.apache.commons.fileupload2.jakarta.servlet6.JakartaServletFileUpload;
import org.eclipse.jetty.ee10.servlet.DefaultServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@MultipartConfig
public class LDP extends DefaultServlet {
    private static final Logger logger = LoggerFactory.getLogger(LDP.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        super.doGet(request, response);
        logger.debug("{} ----> {}",request.getRequestURI(),request.getContentType());
    }
       
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        logger.debug("doPost {} ----> {}",request.getRequestURI(),request.getContentType());        
        if (JakartaServletFileUpload.isMultipartContent(request)) {
            System.out.println("HERE "+JakartaServletFileUpload.isMultipartContent(request));
            Collection<Part> parts = request.getParts();
            System.out.println("BBB");
            parts.forEach(c->{
                System.out.println(c);
            });
        } else {
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
                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType("text/html;charset=UTF-8");
                try (PrintWriter out = response.getWriter()) {
                    out.println("<html>");
                    out.println("<head>");
                    out.println("<title>Status OK</title>");
                    out.println("</head>");
                    out.println("<body>");
                    out.println("<h1>All is well!</h1>");
                    out.println("</body>");
                    out.println("</html>");
                }
        }
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