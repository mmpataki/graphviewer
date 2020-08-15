package com.mmp.graphui.janusplugin;

import com.mmp.graphui.graph.Edge;
import com.mmp.graphui.graph.Graph;
import com.mmp.graphui.graph.Vertex;

import java.util.List;
import java.util.Map;

public class JanusGraph implements Graph {
    @Override
    public String getId() {
        return null;
    }

    @Override
    public String open(Map<String, String> props) {
        return null;
    }

    @Override
    public List<Vertex> v(String... ids) {
        return null;
    }

    @Override
    public List<Edge> e(String... edges) {
        return null;
    }

    @Override
    public List<Vertex> v(Map<String, Object> props) {
        return null;
    }

    @Override
    public Map<String, List<Edge>> outE(String ...ids) throws Exception {
        return null;
    }
}
