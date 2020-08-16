package com.mmp.graphui.janusplugin;

import com.mmp.graphui.graph.Edge;
import com.mmp.graphui.graph.Graph;
import com.mmp.graphui.graph.TraverseDirection;
import com.mmp.graphui.graph.Vertex;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.janusgraph.core.JanusGraphFactory;
import org.janusgraph.diskstorage.configuration.backend.CommonsConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.inV;

public class JanusGraph implements Graph {

    static final Logger LOG = LoggerFactory.getLogger(JanusGraph.class);

    String id;
    private static final AtomicLong gcntr = new AtomicLong(0);

    private static final Map<String, String> shortKey2LongMap = new HashMap<String, String>(){{
        put("table", "storage.hbase.table");
        put("zkQuorum", "storage.hbase.ext.hbase.zookeeper.quorum");
        put("zkZnode", "storage.hbase.ext.zookeeper.znode.parent");
    }};

    private static final Map<String, String> defaultConf = new HashMap<String, String>() {{
        put("storage.transactions", "false");
        put("query.force-index","false");
        put("storage.backend","hbase");
    }};
    private static org.janusgraph.core.JanusGraph G = null;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String open(Map<String, String> props) {

        CommonsConfiguration conf = new CommonsConfiguration();

        /* apply default config */
        defaultConf.forEach((k, v) -> conf.set(k, v));

        /* expand and apply user provided short config */
        for (Map.Entry<String, String> s2l : shortKey2LongMap.entrySet()) {
            if(props.containsKey(s2l.getKey()))
                conf.set(s2l.getValue(), props.get(s2l.getKey()));
        }

        /* put all user provided config */
        props.forEach((k, v) -> conf.set(k, v));

        LOG.info("User connection params: {}", props);
        LOG.info("Effective config: {}", conf);

        G = JanusGraphFactory.open(conf);
        return id = "janus-graph-" + gcntr.incrementAndGet();
    }

    @Override
    public List<Vertex> v(String... ids) {
        return deserializeVerts(G.traversal().V(ids == null ? new String[0] : ids));
    }

    private List<Vertex> deserializeVerts(GraphTraversal<org.apache.tinkerpop.gremlin.structure.Vertex, org.apache.tinkerpop.gremlin.structure.Vertex> T) {
        return T.valueMap(true).toList().stream().map(map -> new Vertex() {

            Map<String, List<String>> props = deserialize(map);

            @Override
            public String getId() {
                return props.get("id").get(0);
            }

            @Override
            public Map<String, List<String>> getProperties() {
                return props;
            }
        }).collect(Collectors.toList());
    }


    private Map<String, List<String>> deserialize(Map<String, Object> map) {
        Map<String, List<String>> retMap = new HashMap<>();
        for (Map.Entry kvp : map.entrySet()) {
            if (kvp.getValue() instanceof List) {
                retMap.put(kvp.getKey().toString(), ((List<Object>) kvp.getValue()).stream().map(i -> i.toString()).collect(Collectors.toList()));
            } else {
                List<String> valList = new LinkedList<>();
                valList.add(kvp.getValue().toString());
                retMap.put(kvp.getKey().toString(), valList);
            }
        }
        return retMap;
    }

    @Override
    public List<Edge> e(String... edges) {
        return null;
    }

    @Override
    public List<Vertex> v(Map<String, List<Object>> props) {

        /* ids are not supported in janus */
        String ids[] = new String[0];
        if(props.containsKey("id")) {
            ids = props.get("id").toArray(new String[0]);
            props.remove("id");
        }
        GraphTraversal<org.apache.tinkerpop.gremlin.structure.Vertex, org.apache.tinkerpop.gremlin.structure.Vertex> T = G.traversal().V(ids);
        for (Map.Entry<String, List<Object>> kvp : props.entrySet()) {
            for (Object val : kvp.getValue()) {
                T = T.has(kvp.getKey(), val);
            }
        }
        return deserializeVerts(T);
    }

    @Override
    public List<Edge> e(String[] ids, TraverseDirection dir) throws Exception {
        ids = ids == null ? new String[0] : ids;
        List<Edge> retList = new LinkedList<>();
        if (dir == TraverseDirection.BOTH || dir == TraverseDirection.IN) {
            retList.addAll(deserializeEdges(G.traversal().V(ids).inE()));
        }
        if (dir == TraverseDirection.BOTH || dir == TraverseDirection.OUT) {
            retList.addAll(deserializeEdges(G.traversal().V(ids).outE()));
        }
        return retList;
    }

    private List<Edge> deserializeEdges(GraphTraversal<org.apache.tinkerpop.gremlin.structure.Vertex, org.apache.tinkerpop.gremlin.structure.Edge> ET) {
        return ET.project("e").by().toList().stream()
                .map(map -> map.get("e"))
                .map(x -> {
                    Map<String, Object> map = new HashMap<>();
                    org.apache.tinkerpop.gremlin.structure.Edge e = (org.apache.tinkerpop.gremlin.structure.Edge) x;
                    Iterator<Property<Object>> propIterator = e.properties();
                    while (propIterator.hasNext()) {
                        Property<Object> prop = propIterator.next();
                        map.put(prop.key(), prop.value());
                    }
                    return new Edge() {
                        @Override
                        public String getFrom() {
                            return e.outVertex().id().toString();
                        }

                        @Override
                        public String getTo() {
                            return e.inVertex().id().toString();
                        }

                        @Override
                        public Map<String, List<String>> getProperties() {
                            return deserialize(map);
                        }
                    };
                }).collect(Collectors.toList());
    }

}
