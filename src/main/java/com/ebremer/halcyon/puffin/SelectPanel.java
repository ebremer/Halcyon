package com.ebremer.halcyon.puffin;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;

/**
 *
 * @author erich
 */
public class SelectPanel extends Panel implements IMarkupResourceStreamProvider {

    public SelectPanel(String id, Component c) {
        super(id);
        add(c);
    }
    
    @Override
    public IResourceStream getMarkupResourceStream(MarkupContainer mc, Class<?> type) {
        return new StringResourceStream(
            """
            <wicket:panel>
                <select wicket:id="comp"/>
            </wicket:panel>"
            """);
    }
}
