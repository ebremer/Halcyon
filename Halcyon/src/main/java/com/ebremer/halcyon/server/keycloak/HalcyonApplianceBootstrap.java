package com.ebremer.halcyon.server.keycloak;

import com.ebremer.halcyon.server.utils.HalcyonSettings;
import java.util.HashMap;
import java.util.Map;
//import org.keycloak.models.AdminRoles;
//import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
//import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.ApplianceBootstrap;

/**
 *
 * @author erich
 */
public class HalcyonApplianceBootstrap extends ApplianceBootstrap {
    private final KeycloakSession session;
    
    public HalcyonApplianceBootstrap(KeycloakSession session) {
        super(session);
        this.session = session;
    }
    
    public void createRealmUser(String username, String password) {
        RealmModel realm = session.realms().getRealmByName(HalcyonSettings.realm);
        Map<String, String> map = new HashMap<>();
        session.users().searchForUserStream(realm, map).forEach(um->{
            System.out.println(um.getUsername());
            um.getGroupsStream().forEach(gm->{
                System.out.println("memberOf : "+gm.getName());
            });
        });
        session.groups().getGroupsStream(realm).forEach(gm->{
            System.out.println(gm.getId()+"  "+gm.getName());
            gm.getRealmRoleMappingsStream().forEach(rm->{
                System.out.println("roleOf : "+rm.getName());
            });
        });
        if (session.users().getUsersCount(realm) > 0) {
            throw new IllegalStateException("Can't create initial user as users already exists");
        }
        UserModel user = session.users().addUser(realm, username);
        user.setEnabled(true);
        UserCredentialModel usrCredModel = UserCredentialModel.password(password);
        user.credentialManager().updateCredential(usrCredModel);
        session.groups().getTopLevelGroupsStream(realm).forEach(yay->{
            System.out.println(yay.getName()+" "+yay.getId());
            if ("admin".equals(yay.getName())) {
                user.joinGroup(yay);
            }
        });
    }    
}

        //GroupModel group = realm.
        //user.joinGroup(group);
        //RoleModel adminRole = realm.getRole(AdminRoles.MANAGE_REALM);
        //user.grantRole(adminRole);