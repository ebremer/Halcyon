package com.ebremer.vandegraaph.shacl;

import org.apache.jena.graph.Node;

/**
 *
 * @author erich
 */
public record Predicate(String name, Node node, Node subshape, Node viewer, Node editor) {};