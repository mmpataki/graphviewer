package com.mmp.graphui.graph;

import java.util.List;
import java.util.Map;

public interface Graph {

    /* return a reference to a graph object */
    String getId();

    /* open the graph */
    String open(Map<String, String> props) throws Exception;

    /* get the vertices with ids */
    List<Vertex> v(String ...ids) throws Exception;

    /* get the edges with ids */
    List<Edge> e(String ...edges) throws Exception;

    /* search the vertices */
    List<Vertex> v(Map<String, List<Object>> props) throws Exception;

    List<Edge> e(String[] ids, TraverseDirection dir) throws Exception;
}
