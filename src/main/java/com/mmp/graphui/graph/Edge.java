package com.mmp.graphui.graph;

import java.util.List;
import java.util.Map;

public interface Edge {

    String getFrom();

    String getTo();

    Map<String, List<String>> getProperties();

}
