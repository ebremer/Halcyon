package com.ebremer.halcyon.datum;

import com.ebremer.halcyon.datum.DataCore.Level;
import static com.ebremer.halcyon.datum.DataCore.Level.OPEN;
import com.ebremer.halcyon.fuseki.shiro.JwtToken;
import com.ebremer.halcyon.gui.HalcyonSession;
import com.ebremer.ns.HAL;
import java.security.Principal;
import java.util.HashMap;
import java.util.Set;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.permissions.SecurityEvaluator;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.shared.AuthenticationRequiredException;
import org.apache.jena.vocabulary.SchemaDO;
import org.apache.jena.vocabulary.WAC;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;

/**
 * 
 * @author erich
 */
public class WACSecurityEvaluator implements SecurityEvaluator {
    private final Model secm;
    private final HashMap<Node,HashMap> cache;
    private final Level level;
    
    public WACSecurityEvaluator(Level level) {
        this.level = level;
        System.out.println("==================================== WACSecurityEvaluator ======================================================");
        cache = new HashMap<>();
        secm = ModelFactory.createDefaultModel();
        Dataset ds = DataCore.getInstance().getDataset();
        ds.begin(ReadWrite.READ);
        secm.add(ds.getNamedModel(HAL.SecurityGraph.getURI()));
        secm.add(ds.getNamedModel(HAL.CollectionsAndResources.getURI()));
        secm.add(ds.getNamedModel(HAL.GroupsAndUsers.getURI()));
        ds.end();
    }
    
    public Model getSecurityModel() {
        return secm;
    }

    @Override
    public boolean evaluate(Object principal, Action action, Node graphIRI) {
        HalcyonPrincipal hp = (HalcyonPrincipal) principal;
        if (level == OPEN) {
            if (graphIRI.matches(HAL.CollectionsAndResources.asNode())) {
                return true;
            }
        }
         /*
        if (cache.containsKey(graphIRI)) {
            if (cache.get(graphIRI).containsKey(action)) {
                return true;
            }
        }
        HashMap<Action,Boolean> set = new HashMap<>();
        cache.put(graphIRI, set);
*/
        ParameterizedSparqlString pss = new ParameterizedSparqlString("""
            ASK {?rule acl:accessTo/so:hasPart* ?target;
                       acl:mode ?mode;
                       acl:agent ?group .
                 ?group so:member ?member
            }
        """);
        pss.setNsPrefix("acl", WAC.NS);
        pss.setNsPrefix("so", SchemaDO.NS);
        pss.setNsPrefix("hal", HAL.NS);
        pss.setIri("target", graphIRI.toString());
        pss.setIri("mode", WACUtil.WAC(action));
        pss.setIri("member", hp.getURNUUID());
        boolean ha = QueryExecutionFactory.create(pss.toString(), secm).execAsk();
      //  set.put(action, ha);
      //System.out.println(graphIRI+" ---> "+ha);
        return ha;
    }
    
    private boolean evaluate( Object principal, Triple triple ) {
        return evaluate( principal, triple.getSubject()) && evaluate( principal, triple.getObject()) && evaluate( principal, triple.getPredicate());
    }
    
    private boolean evaluate( Object principal, Node node ) {
        return node.equals( Node.ANY );
    }

    @Override
    public boolean evaluate(Object principal, Action action, Node graphIRI, Triple triple) {
        return !evaluate( principal, triple );
    }
    
    @Override
    public boolean evaluate(Object principal, Set<Action> actions, Node graphIRI) throws AuthenticationRequiredException {
        return SecurityEvaluator.super.evaluate(principal, actions, graphIRI);
    }

    @Override
    public boolean evaluate(Object principal, Set<Action> actions, Node graphIRI, Triple triple) throws AuthenticationRequiredException {
        return SecurityEvaluator.super.evaluate(principal, actions, graphIRI, triple);
    }

    @Override
    public boolean evaluateAny(Object principal, Set<Action> actions, Node graphIRI) throws AuthenticationRequiredException {
        return SecurityEvaluator.super.evaluateAny(principal, actions, graphIRI);
    }

    @Override
    public boolean evaluateAny(Object principal, Set<Action> actions, Node graphIRI, Triple triple) throws AuthenticationRequiredException {
        return SecurityEvaluator.super.evaluateAny(principal, actions, graphIRI, triple);
    }

    @Override
    public boolean evaluateUpdate(Object principal, Node graphIRI, Triple from, Triple to) throws AuthenticationRequiredException {
        return SecurityEvaluator.super.evaluateUpdate(principal, graphIRI, from, to);
    }

    @Override
    public Principal getPrincipal() {
        try {
            Subject subject = SecurityUtils.getSubject();
            JwtToken jwttoken = (JwtToken) subject.getPrincipal();
            //Claims dc = (Claims) SecurityUtils.getSubject().getPrincipal();
            return new HalcyonPrincipal(jwttoken,false);
        } catch (org.apache.shiro.UnavailableSecurityManagerException ex) {
            // assume and try for a Keycloak Servlet Filter Auth
        }
        HalcyonPrincipal p = HalcyonSession.get().getHalcyonPrincipal();
        if (p.isAnon()) {
            Resource au = secm.createResource(p.getURNUUID());
            Resource as = secm.createResource(HAL.Anonymous.toString()).addProperty(SchemaDO.member,au);
            au.addProperty(SchemaDO.memberOf, as);
        }
        //System.out.println("PRINCIPAL --> "+p.getURNUUID());
        return p;
    }

    @Override
    public boolean isPrincipalAuthenticated(Object o) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isHardReadError() {
        return SecurityEvaluator.super.isHardReadError();
    }
}
