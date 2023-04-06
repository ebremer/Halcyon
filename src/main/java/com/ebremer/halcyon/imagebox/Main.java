package com.ebremer.halcyon.imagebox;

import com.ebremer.halcyon.keycloak.HALKeycloakOIDCFilter;
import com.ebremer.halcyon.HalcyonSettings;
import com.ebremer.halcyon.INIT;
import com.ebremer.halcyon.fuseki.HalcyonProxyServlet;
import com.ebremer.halcyon.datum.SessionsManager;
import com.ebremer.halcyon.gui.HalcyonApplication;
import io.undertow.UndertowOptions;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.servlet.DispatcherType;
import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.protocol.http.ContextParamWebApplicationFactory;
import org.apache.wicket.protocol.http.WicketFilter;
import org.keycloak.adapters.servlet.KeycloakOIDCFilter;
import org.keycloak.adapters.spi.SessionIdMapper;
import org.mitre.dsmiley.httpproxy.ProxyServlet;
import org.springframework.boot.Banner.Mode;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication(exclude = LiquibaseAutoConfiguration.class)
public class Main {
    private final HalcyonSettings settings = HalcyonSettings.getSettings();
    private static final SessionIdMapper sessionidmapper = SessionsManager.getSessionsManager().getSessionIdMapper();
    
    public static SessionIdMapper getSessionIdMapper() {
        return sessionidmapper;
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public ServletRegistrationBean proxyServletRegistrationBean() {
        ServletRegistrationBean bean = new ServletRegistrationBean(new HalcyonProxyServlet(), "/sparql/*");
        bean.addInitParameter("targetUri", "http://localhost:8887/rdf");
        bean.addInitParameter(ProxyServlet.P_PRESERVECOOKIES, "true");
        bean.addInitParameter(ProxyServlet.P_HANDLEREDIRECTS, "true");
        return bean;
    }
    
    @Bean
    public UndertowServletWebServerFactory embeddedServletContainerFactory() {
        System.out.println("Configuring Undertow Web Engine...");
        UndertowServletWebServerFactory factory = new UndertowServletWebServerFactory();
        int cores = Runtime.getRuntime().availableProcessors();
        int ioThreads = cores;
        int taskThreads = 16*cores;
        System.out.println("ioThreads  :"+ioThreads+"\ntaskThreads :"+taskThreads);
        factory.setIoThreads(ioThreads);   
        if (settings.isHTTPSenabled()) {
            factory.addBuilderCustomizers(builder -> {
                SSLContext ssl;
                try {
                    ssl = getSSLContext();
                    System.out.println("HTTPS PORT : "+settings.GetHTTPSPort());
                    builder
                        .addHttpsListener(settings.GetHTTPSPort(), "0.0.0.0", ssl)
                        .setServerOption(UndertowOptions.ENABLE_HTTP2, true)
                        .setServerOption(UndertowOptions.MAX_PARAMETERS, 100000)
                        .setServerOption(UndertowOptions.MAX_CONCURRENT_REQUESTS_PER_CONNECTION, 100);
                } catch (Exception ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }
            });
        } else {
            System.out.println("HTTPS not enabled.");
        }
        return factory;
    }  
    
    @Bean
    ServletRegistrationBean iboxServletRegistration () {
        System.out.println("iboxServletRegistration order: "+Ordered.LOWEST_PRECEDENCE);
        ServletRegistrationBean srb = new ServletRegistrationBean();
        srb.setServlet(new iboxServlet());
        srb.setUrlMappings(Arrays.asList("/iiif/*"));
        return srb;
    }
    
    @Bean
    public FilterRegistrationBean<HALKeycloakOIDCFilter> KeycloakOIDCFilterFilterRegistration(){
        System.out.println("KeycloakOIDCFilterFilterRegistration order: "+Ordered.HIGHEST_PRECEDENCE);
            HALKeycloakOIDCFilter filter = new HALKeycloakOIDCFilter();
            //filter.setSessionIdMapper(SessionsManager.getSessionsManager().getSessionIdMapper());
            filter.setSessionIdMapper(getSessionIdMapper());
	    FilterRegistrationBean<HALKeycloakOIDCFilter> registration = new FilterRegistrationBean<>();
	    registration.setFilter(filter);
            registration.setName("keycloak");
	    registration.addUrlPatterns("/*");
            registration.setOrder(0);
            registration.addInitParameter(KeycloakOIDCFilter.CONFIG_FILE_PARAM, "keycloak.json");
            registration.addInitParameter(KeycloakOIDCFilter.SKIP_PATTERN_PARAM, "(^/multi-viewer.*|^/iiif.*|^/gui/viewer.*|^/gui|^/gui/about|^/gui/ListImages.*|^/sparql.*|^/wicket/resource/com.*\\.css|^/gui/public|^/gui/vendor/openseadragon/.*|^/auth/.*|^/favicon.ico|^/auth/.*$)");
            registration.setEnabled(true);
        return registration;
    }
    
    @Bean
    public FilterRegistrationBean<JwtInterceptor> headerAddingFilter() {
        FilterRegistrationBean<JwtInterceptor> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new JwtInterceptor());
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(1);
        return registrationBean;
    }
    
    @Bean
    public FilterRegistrationBean<WicketFilter> wicketFilterRegistration(){
        HalcyonApplication hal = new HalcyonApplication();
        hal.setConfigurationType(RuntimeConfigurationType.DEPLOYMENT);
        WicketFilter filter = new WicketFilter(hal);
        filter.setFilterPath("/");
        FilterRegistrationBean<WicketFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.setOrder(2);
        registration.addInitParameter(ContextParamWebApplicationFactory.APP_CLASS_PARAM, HalcyonApplication.class.getName());
        registration.addInitParameter(WicketFilter.FILTER_MAPPING_PARAM, "/");
        registration.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.FORWARD);
        return registration;
    }
    
    @Bean
    ServletRegistrationBean HalcyonServletRegistration () {
        ServletRegistrationBean srb = new ServletRegistrationBean();
        srb.setServlet(new HalcyonServlet());
        srb.setOrder(20);
        srb.setUrlMappings(Arrays.asList("/halcyon/*"));
        return srb;
    }
    
    @Configuration
    public class HalcyonResourceConfiguration implements WebMvcConfigurer {
        
        @Override
        public void addResourceHandlers(ResourceHandlerRegistry registry) {
            String mv = settings.getMultiewerLocation();
            if (mv!=null) {
                if (mv.startsWith("file:///")) {
                    mv = mv.replace("file:///", "file:/");
                }
                registry.addResourceHandler("/multi-viewer/**").addResourceLocations(mv);
            } else {
                registry.addResourceHandler("/multi-viewer/**").addResourceLocations("classpath:/META-INF/public-web-resources/multi-viewer/");
                registry.addResourceHandler("/three.js/**").addResourceLocations("classpath:/META-INF/public-web-resources/three.js/");
            }   
        }
    }
    
    private final String keyStorePassword = "changeit";
    private final String serverKeystore = "cacerts.p12";
    private final String serverTruststore = "cacerts.p12";
    private final String trustStorePassword = "changeit";
    
    public SSLContext getSSLContext() throws Exception {
        return createSSLContext(loadKeyStore(serverKeystore,keyStorePassword), loadKeyStore(serverTruststore,trustStorePassword));
    }

    private SSLContext createSSLContext(final KeyStore keyStore, final KeyStore trustStore) throws Exception {
        KeyManager[] keyManagers;
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, keyStorePassword.toCharArray());
        keyManagers = keyManagerFactory.getKeyManagers();

        TrustManager[] trustManagers;
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);
        trustManagers = trustManagerFactory.getTrustManagers();

        SSLContext sslContext;
        sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagers, trustManagers, null);

        return sslContext;
    }
    
    private static KeyStore loadKeyStore(final String storeLoc, final String storePw) throws Exception {
        InputStream stream = Files.newInputStream(Paths.get(storeLoc));
        if(stream == null) {
            throw new IllegalArgumentException("Could not load keystore");
        }
        try (InputStream is = stream) {
            KeyStore loadedKeystore = KeyStore.getInstance("JKS");
            loadedKeystore.load(is, storePw.toCharArray());
            return loadedKeystore;
        }
    }

    public static void main(String[] args) throws NoSuchAlgorithmException {
        INIT i = new INIT();
        i.init();
        SpringApplication app = new SpringApplication(Main.class);
        app.setBannerMode(Mode.CONSOLE);
        ApplicationContext yay = app.run(args);
        System.out.println("===================== " +app.getWebApplicationType());
        //ApplicationContext ha = (ApplicationContext) yay;
        //WebServerApplicationContext webContext = (WebServerApplicationContext) ha;
        //System.out.println(webContext.getWebServer());
        //UndertowServletWebServer ut = (UndertowServletWebServer) webContext.getWebServer();
        //SessionManager sm = ut.getDeploymentManager().getDeployment().getSessionManager();
    }
}
