package com.mmp.graphui.inmem;

import com.mmp.graphui.graph.Edge;
import com.mmp.graphui.graph.Graph;
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
    Map<String, List<String>> edges = new HashMap<>();
    Map<String, Map<String, List<String>>> edgeProps = new HashMap<>();

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
            if (line.contains("#vertices"))
                reading = "verts";
            else if (line.contains("#edges"))
                reading = "edges";
            else {
                switch (reading) {
                    case "verts":
                        processVert(line);
                    case "edges":
                        processEdge(line);
                }
            }
        }
        id = "graph-" + System.currentTimeMillis();
        return id;
    }

    private void processEdge(String line) {
        String from = null, to = null;
        Map<String, List<String>> eprops = new HashMap<>();
        for (String chunk : line.split(",")) {
            if (chunk.contains("=")) {
                String kvp[] = chunk.split("=");
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
        if (!edges.containsKey(from)) {
            edges.put(from, new ArrayList<>());
        }
        edges.get(from).add(to);
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
    }

    @Override
    public List<Vertex> v(String... ids) {

        Stream<Map<String, List<String>>> mapStream =
                ids != null ?
                        Arrays.stream(ids).map(id -> vertices.get(id)).filter(vprops -> vprops != null) :
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
    public List<Vertex> v(Map<String, Object> props) {
        return new ArrayList<>();
    }

    @Override
    public Map<String, List<Edge>> outE(String... ids) throws Exception {
        Map<String, List<Edge>> retMap = new HashMap<>();
        for (String v : ids) {
            if (!edges.containsKey(v)) {
                retMap.put(v, Collections.EMPTY_LIST);
                continue;
            }
            retMap.put(v, edges.get(v).stream().map(id -> new Edge() {
                @Override
                public String getFrom() {
                    return v;
                }

                @Override
                public String getTo() {
                    return id;
                }

                @Override
                public Map<String, List<String>> getProperties() {
                    return edgeProps.get(v + "=>" + id);
                }
            }).collect(Collectors.toList()));
        }
        return retMap;
    }

    @Override
    public Map<String, List<Vertex>> out(String... ids) throws Exception {
        Map<String, List<Vertex>> retVerts = new HashMap<>();
        for (String id : ids) {
            List<Vertex> adjVerts = new LinkedList<>();
            retVerts.put(id, adjVerts);
            edges.get(id).forEach(adjVert -> adjVerts.add(new Vertex() {
                @Override
                public String getId() {
                    return adjVert;
                }

                @Override
                public Map<String, List<String>> getProperties() {
                    return vertices.get(adjVert);
                }
            }));
        }
        return retVerts;
    }
}
