package com.mmp.graphui.controllers;

import com.mmp.graphui.graph.Edge;
import com.mmp.graphui.graph.Vertex;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin
public class GraphController {

    static GraphService DAO = new GraphService();

    @RequestMapping(value = "/graph/open", method = RequestMethod.POST)
    public String open(@RequestBody Map<String, String> props) throws Exception {
        return DAO.open(props);
    }

    @RequestMapping(value = "/graph/vertices", method = RequestMethod.GET)
    public List<Vertex> v(String gId, String ...ids) throws Exception {
        return DAO.get(gId).v(ids);
    }

    @RequestMapping(value = "/graph/outE", method = RequestMethod.GET)
    public List<Edge> outE(String gId, String ...id) throws Exception {
        return DAO.get(gId).outE(id);
    }

    @RequestMapping(value = "/graph/out", method = RequestMethod.GET)
    public Map<String, List<Vertex>> out(String gId, String ...id) throws Exception {
        return DAO.get(gId).out(id);
    }

    @RequestMapping(value = "/graph/searchv", method = RequestMethod.POST)
    public List<Vertex> searchV(String gId, @RequestBody Map<String, List<Object>> searchFilter) throws Exception {
        return DAO.get(gId).v(searchFilter);
    }

}
