package com.ebremer.halcyon.keycloak;

import com.ebremer.halcyon.datum.HalcyonPrincipal;
import java.io.IOException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import java.security.Principal;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.cycle.RequestCycle;

/**
 *
 * @author erich
 */

public class KeycloakTokenFilter implements ClientRequestFilter {

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        
        ServletWebRequest request = (ServletWebRequest) RequestCycle.get().getRequest();
      //  ServletWebResponse response = (ServletWebResponse) ResponseCycle.get().getResponse();
        HttpServletRequest containerRequest = request.getContainerRequest();
        //HttpServletResponse containerResponse = response.getContainerResponse();
     //   JEEContext context = new JEEContext(containerRequest, containerResponse);
      //  ProfileManager profileManager = new ProfileManager(context, new JEESessionStore());
       // Optional<UserProfile> profile = profileManager.getProfile();
        Principal ppp =  containerRequest.getUserPrincipal();
        HalcyonPrincipal hp = (HalcyonPrincipal) ppp;
        System.out.println("CLIENT ACCESS SECURITY ===> "+hp.getToken());
        requestContext.getHeaders().add("Authorization", "Bearer "+ hp.getToken());
    }
}


