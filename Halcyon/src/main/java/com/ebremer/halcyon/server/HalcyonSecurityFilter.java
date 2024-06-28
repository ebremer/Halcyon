package com.ebremer.halcyon.server;

import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.Setter;
import lombok.val;
import org.pac4j.core.adapter.FrameworkAdapter;
import org.pac4j.core.config.Config;
import org.pac4j.core.util.Pac4jConstants;
import org.pac4j.core.util.security.SecurityEndpoint;
import org.pac4j.core.util.security.SecurityEndpointBuilder;
import org.pac4j.jee.config.AbstractConfigFilter;
import org.pac4j.jee.context.JEEFrameworkParameters;
import java.io.IOException;
import org.pac4j.core.client.config.BaseClientConfiguration;
import org.pac4j.core.resource.SpringResourceHelper;
import org.pac4j.core.resource.SpringResourceLoader;
import org.pac4j.jee.context.session.JEESessionStore;
import org.pac4j.oidc.client.KeycloakOidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import org.pac4j.oidc.profile.creator.TokenValidator;
import org.pac4j.oidc.redirect.OidcRedirectionActionBuilder;

/**
 * <p>This filter protects an URL.</p>
 *
 * @author Jerome Leleu, Michael Remond
 * @since 1.0.0
 */
@Getter
@Setter
public class HalcyonSecurityFilter extends AbstractConfigFilter implements SecurityEndpoint {
    private String clients;
    private String authorizers;
    private String matchers;

    public HalcyonSecurityFilter() {}

    public HalcyonSecurityFilter(final Config config) {
        setConfig(config);
    }

    public HalcyonSecurityFilter(final Config config, final String clients) {
        this(config);
        this.clients = clients;
    }

    public HalcyonSecurityFilter(final Config config, final String clients, final String authorizers) {
        this(config, clients);
        this.authorizers = authorizers;
    }

    public HalcyonSecurityFilter(final Config config, final String clients, final String authorizers, final String matchers) {
        this(config, clients, authorizers);
        this.matchers = matchers;
    }

    public static HalcyonSecurityFilter build(final Object... parameters) {
        final HalcyonSecurityFilter securityFilter = new HalcyonSecurityFilter();
        SecurityEndpointBuilder.buildConfig(securityFilter, parameters);
        return securityFilter;
    }

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
        super.init(filterConfig);
        this.clients = getStringParam(filterConfig, Pac4jConstants.CLIENTS, this.clients);
        this.authorizers = getStringParam(filterConfig, Pac4jConstants.AUTHORIZERS, this.authorizers);
        this.matchers = getStringParam(filterConfig, Pac4jConstants.MATCHERS, this.matchers);
    }

    @Override
    protected final void internalFilter( final HttpServletRequest request, final HttpServletResponse response, final FilterChain filterChain ) throws IOException, ServletException {
        System.out.println("START internalFilter..........................................................");
        TokenValidator ha;
        //OidcRedirectionActionBuilder blah;
        //SpringResourceLoader loaderxxx;
        KeycloakOidcClient sss;
        OidcConfiguration a333;
        BaseClientConfiguration sddd;
        OIDCProviderMetadata aaa;
        JEESessionStore jjj;
        SpringResourceHelper llll;
        System.out.println("AAA");
        val config = getSharedConfig();
        System.out.println("BBB : "+config.getSecurityLogic());
        FrameworkAdapter.INSTANCE.applyDefaultSettingsIfUndefined(config);
        System.out.println("CCC : "+config.getSecurityLogic());
        config.getSecurityLogic().perform(config, (ctx, session, profiles) -> {
            System.out.println("LOOP : ");
            // if no profiles are loaded, pac4j is not concerned with this request
            filterChain.doFilter(profiles.isEmpty() ? request : new HalcyonPac4JHttpServletRequestWrapper(request, profiles), response);
            return null;
        }, clients, authorizers, matchers, new JEEFrameworkParameters(request, response));
        System.out.println("END internalFilter..........................................................");
    }
}
