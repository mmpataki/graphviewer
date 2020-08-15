package com.mmp.graphui.controllers;

import com.mmp.graphui.graph.Edge;
import com.mmp.graphui.graph.Vertex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
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
    public Map<String, List<Edge>> outE(String gId, String ...ids) throws Exception {
        return DAO.get(gId).outE(ids);
    }

    @RequestMapping(value = "/graph/out", method = RequestMethod.GET)
    public Map<String, List<Vertex>> out(String gId, String ...ids) throws Exception {
        return DAO.get(gId).out(ids);
    }

}
