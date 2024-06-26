package com.ebremer.halcyon.server;

import java.util.Optional;
import org.pac4j.core.context.CallContext;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.core.profile.creator.ProfileCreator;
import org.pac4j.oidc.credentials.OidcCredentials;
import org.pac4j.oidc.profile.OidcProfile;
import org.pac4j.oidc.profile.keycloak.KeycloakOidcProfile;

/**
 *
 * @author erich
 */
public class HalcyonProfileCreator implements ProfileCreator {

    @Override
    public Optional<UserProfile> create(CallContext ctx, Credentials credentials) {
        if (!(credentials instanceof OidcCredentials)) {
            throw new IllegalArgumentException("Invalid credentials type");
        }

        OidcCredentials oidcCredentials = (OidcCredentials) credentials;
        OidcProfile oidcProfile = (OidcProfile) oidcCredentials.getUserProfile();

        // Create a custom profile, which could be a subclass of CommonProfile or a completely new implementation
        KeycloakOidcProfile customProfile = new KeycloakOidcProfile();

        // Copy relevant information from the OIDC profile to your custom profile
        customProfile.setId(oidcProfile.getId());
        customProfile.addAttributes(oidcProfile.getAttributes());

        // Additional custom logic for the profile can be added here
        // e.g., customProfile.addAttribute("customAttribute", value);
        return Optional.of(customProfile);
    }
}

