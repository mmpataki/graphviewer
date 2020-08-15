package com.mmp.graphui.graph;

import java.util.List;
import java.util.Map;

public interface Vertex {

    public String getId();

    public Map<String, List<String>> getProperties();
}
