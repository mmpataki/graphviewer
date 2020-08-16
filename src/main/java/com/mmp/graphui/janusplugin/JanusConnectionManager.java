package com.mmp.graphui.janusplugin;

import org.janusgraph.core.JanusGraph;
import org.janusgraph.diskstorage.configuration.backend.CommonsConfiguration;

import java.util.HashMap;
import java.util.Map;

public class JanusConnectionManager {
    private static final JanusConnectionManager INSTANCE = new JanusConnectionManager();

    private synchronized JanusConnectionManager getInstance() {
        return INSTANCE;
    }

    Map<String, JanusGraph> graphs = new HashMap<>();

    JanusGraph getOrCreate(CommonsConfiguration conf) {
        return null;
    }

}
