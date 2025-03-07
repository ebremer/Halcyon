package com.ebremer.halcyon.fuseki;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.mitre.dsmiley.httpproxy.ProxyServlet;

/**
 *
 * @author erich
 */
public class HalcyonProxyServlet extends ProxyServlet {

    @Override
    protected void copyRequestHeaders(HttpServletRequest servletRequest, HttpRequest proxyRequest) {
        super.copyRequestHeaders(servletRequest, proxyRequest);
        
        proxyRequest.removeHeaders("X-Forwarded-For");
        proxyRequest.removeHeaders("X-Forwarded-Proto");
        proxyRequest.removeHeaders("X-Forwarded-Host");        
        proxyRequest.removeHeaders("Access-Control-Allow-Headers");
        
        proxyRequest.addHeader("X-Forwarded-For", servletRequest.getRemoteAddr());
        proxyRequest.addHeader("X-Forwarded-Proto", "https");        
        proxyRequest.addHeader("X-Forwarded-Host", "localhost");
        proxyRequest.addHeader("X-Forwarded-Port", "8888");
        proxyRequest.addHeader("Access-Control-Allow-Origin", "*");
        proxyRequest.addHeader("Access-Control-Allow-Headers", "Content-type, Authorization, X-Requested-With, DPop");
    }    
    
    @Override
    protected String rewritePathInfoFromRequest(HttpServletRequest servletRequest) {
        String wow = servletRequest.getPathInfo();
        return wow;
    }
    
    @Override
    protected HttpResponse doExecute(HttpServletRequest servletRequest, HttpServletResponse servletResponse, HttpRequest proxyRequest) throws IOException {
        System.out.println(servletRequest);
        /*
        servletRequest.getHeaderNames().asIterator().forEachRemaining(h->{
            System.out.println("SH : "+h+" --> "+servletRequest.getHeader(h));
        });
        Arrays.stream(proxyRequest.getAllHeaders()).forEach(h->{
            System.out.println("pH : "+h.getName()+" --> "+h.getValue());
        });    */ 
        System.out.println(
                "proxy   : " + servletRequest.getMethod()
            + "\nuri     : " + servletRequest.getRequestURI()
            + "\nRLINE   : " + proxyRequest.getRequestLine().getUri());
        HttpHost host = getTargetHost(servletRequest);
        HttpResponse rrr = this.getProxyClient().execute(host, proxyRequest);
        return rrr;
  }
}
