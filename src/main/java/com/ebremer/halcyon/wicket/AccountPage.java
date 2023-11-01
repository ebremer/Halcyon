package com.ebremer.halcyon.wicket;

import com.ebremer.halcyon.lib.HalcyonSettings;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;

/**
 *
 * @author erich
 */
public class AccountPage extends BasePage {
    
    public AccountPage() {
        add(new WebMarkupContainer("embedme") {
            @Override
            public void onComponentTag(ComponentTag tag) {
                super.onComponentTag(tag);
                tag.put("src", "/auth/realms/"+HalcyonSettings.realm+"/account");
            }
        }); 
        
        
    }
    
}