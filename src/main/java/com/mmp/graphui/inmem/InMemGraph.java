package com.mmp.graphui.inmem;

import com.mmp.graphui.graph.Edge;
import com.mmp.graphui.graph.Graph;
import com.mmp.graphui.graph.TraverseDirection;
import com.mmp.graphui.graph.Vertex;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Inmemory D-A-G.
 */
public class InMemGraph implements Graph {

    String id;
    Map<String, Map<String, List<String>>> vertices = new HashMap<>();
    Map<String, List<String>> outEdges = new HashMap<>();
    Map<String, List<String>> inEdges = new HashMap<>();
    Map<String, Map<String, List<String>>> edgeProps = new HashMap<>();
    /* vertex index on properties */
    Map<String, Map<Object, Set<String>>> vPropIndex = new HashMap<>();

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String open(Map<String, String> props) throws Exception {
        BufferedReader br = new BufferedReader(new FileReader(props.get("graphFile")));
        String line;
        String reading = "verts";
        while ((line = br.readLine()) != null) {
            if (line.startsWith("*"))
                processVert(line.substring(2));
            else if (line.startsWith("-"))
                processEdge(line.substring(2));
        }
        id = "graph-" + System.currentTimeMillis();
        br.close();
        return id;
    }

    private void processEdge(String line) {
        String from = null, to = null;
        Map<String, List<String>> eprops = new HashMap<>();
        for (String chunk : line.split(",")) {
            if (chunk.contains("=")) {
                String[] kvp = chunk.split("=");
                String key = kvp[0];
                String val = kvp[1];
                if (key.equals("from")) {
                    from = val;
                } else if (key.equals("to")) {
                    to = val;
                } else {
                    if (!eprops.containsKey(key)) {
                        eprops.put(key, new ArrayList<>());
                    }
                    eprops.get(key).add(val);
                }
            }
        }
        if (!outEdges.containsKey(from)) {
            outEdges.put(from, new ArrayList<>());
        }
        if(!inEdges.containsKey(to)) {
            inEdges.put(to, new ArrayList<>());
        }
        outEdges.get(from).add(to);
        inEdges.get(to).add(from);
        edgeProps.put(from + "=>" + to, eprops);
    }

    private void processVert(String line) {
        String id = null;
        Map<String, List<String>> vprops = new HashMap<>();
        for (String chunk : line.split(",")) {
            if (!chunk.contains("=")) {
                id = chunk;
                chunk = "id=" + chunk;
            }
            String[] kvp = chunk.split("=");
            String key = kvp[0];
            String val = kvp[1];
            if (!vprops.containsKey(key)) {
                vprops.put(key, new ArrayList<>());
            }
            vprops.get(key).add(val);
        }
        vertices.put(id, vprops);
        for (Map.Entry<String, List<String>> kvp : vprops.entrySet()) {
            if (!vPropIndex.containsKey(kvp.getKey())) {
                vPropIndex.put(kvp.getKey(), new HashMap<>());
            }
            Map<Object, Set<String>> vals2VertexIds = vPropIndex.get(kvp.getKey());

            for (String value : kvp.getValue()) {
                if (!vals2VertexIds.containsKey(value)) {
                    vals2VertexIds.put(value, new HashSet<>());
                }
                vals2VertexIds.get(value).add(id);
            }
        }
    }

    @Override
    public List<Vertex> v(String... ids) {

        Stream<Map<String, List<String>>> mapStream =
                ids != null ?
                        Arrays.stream(ids).map(id -> vertices.get(id)).filter(Objects::nonNull) :
                        vertices.values().stream();
        return mapStream.map(vprops -> new Vertex() {
            @Override
            public String getId() {
                return vprops.get("id").get(0);
            }

            @Override
            public Map<String, List<String>> getProperties() {
                return vprops;
            }
        }).collect(Collectors.toList());
    }

    @Override
    public List<Edge> e(String... edges) {
        List<Edge> edgeList = new ArrayList<>();
        for (String e : edges) {
            if (!edgeProps.containsKey(e))
                continue;
            edgeList.add(new Edge() {
                @Override
                public String getFrom() {
                    return e.split("=>")[0];
                }

                @Override
                public String getTo() {
                    return e.split("=>")[1];
                }

                @Override
                public Map<String, List<String>> getProperties() {
                    return edgeProps.get(e);
                }
            });
        }
        return edgeList;
    }

    @Override
    public List<Vertex> v(Map<String, List<Object>> props) {
        Set<String> rSet = null;
        for (Map.Entry<String, List<Object>> prop : props.entrySet()) {
            if (!vPropIndex.containsKey(prop.getKey()))
                continue;
            Map<Object, Set<String>> val2VidMap = vPropIndex.get(prop.getKey());
            Set<String> vSet = new HashSet<>();
            for (Object val : prop.getValue()) {
                if (!val2VidMap.containsKey(val))
                    continue;
                Set<String> cur = val2VidMap.get(val);
                vSet.addAll(cur);
            }
            if (rSet == null) {
                rSet = vSet;
            } else {
                rSet.retainAll(vSet);
            }
        }
        return rSet.stream().map(id -> new Vertex() {
            @Override
            public String getId() {
                return id;
            }

            @Override
            public Map<String, List<String>> getProperties() {
                return vertices.get(id);
            }
        }).collect(Collectors.toList());
    }

    @Override
    public List<Edge> e(String[] ids, TraverseDirection dir) throws Exception {
        List<Edge> retVal = new LinkedList<>();
        for (String v : ids) {
            if (!outEdges.containsKey(v))
                continue;
            if(dir == TraverseDirection.BOTH || dir == TraverseDirection.OUT)
                retVal.addAll(getEdges(v, outEdges, TraverseDirection.OUT));
            if(dir == TraverseDirection.BOTH || dir == TraverseDirection.IN)
                retVal.addAll(getEdges(v, inEdges, TraverseDirection.IN));
        }
        return retVal;
    }

    private List<Edge> getEdges(String v, Map<String, List<String>> _edges, TraverseDirection hint) {
        if(!_edges.containsKey(v))
            return Collections.EMPTY_LIST;
        return _edges.get(v).stream().map(id -> new Edge() {
            @Override
            public String getFrom() {
                return hint == TraverseDirection.OUT ? v : id;
            }

            @Override
            public String getTo() {
                return hint == TraverseDirection.OUT ? id : v;
            }

            @Override
            public Map<String, List<String>> getProperties() {
                String edgeID = hint == TraverseDirection.OUT ? v + "=>" + id : id + "=>" + v;
                return edgeProps.get(edgeID);
            }
        }).collect(Collectors.toList());
    }
}
