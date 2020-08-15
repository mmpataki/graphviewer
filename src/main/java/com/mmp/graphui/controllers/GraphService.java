package com.mmp.graphui.controllers;

import com.mmp.graphui.graph.Graph;

import java.util.HashMap;
import java.util.Map;

public class GraphService {

    static String GTYPE = "gtype";

    static Map<String, String> gClasses;
    static {
        gClasses = new HashMap<>();
        gClasses.put("janus", "com.mmp.graphui.janusplugin.JanusGraph");
        gClasses.put("inmem", "com.mmp.graphui.inmem.InMemGraph");
    }

    static Map<String, Graph> graphs = new HashMap<>();

    String open(Map<String, String> props) throws Exception {
        Graph g = (Graph)Class.forName(gClasses.get(props.get(GTYPE))).newInstance();
        g.open(props);
        graphs.put(g.getId(), g);
        return g.getId();
    }

    Graph get(String id) {
        return graphs.get(id);
    }

}
