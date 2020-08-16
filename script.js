var nodeSet, edgeSet;
var nw;
var qObj;
var baseUrl = location.protocol == "file:" ? "http://localhost:8080" : "";
var graphProps = {};
var selectedVert;

/* graph descriptor, TODO: cache this */
var GD;

function ajax(method, url, data, callBack) {
	var xhttp = new XMLHttpRequest();
	xhttp.onreadystatechange = function () {
		if (this.readyState == 4 && this.status == 200) {
			callBack(this.responseText);
		}
	};
	xhttp.open(method, url, true);
	xhttp.setRequestHeader("Content-type", "application/json; charset=utf-8");
	xhttp.send(JSON.stringify(data));
}
function post(url, data, callBack) {
	ajax("POST", url, data, callBack);
}
function get(url, data, callBack) {
	ajax("GET", url, data, callBack);
}
window.onload = function () {
	queryChanged();
	nodeSet = new vis.DataSet([]);
	edgeSet = new vis.DataSet([]);
	options = {
		physics: {
			enabled: true,
			hierarchicalRepulsion: {
				nodeDistance: 400
			}
		}
	};
	nw = new vis.Network(g("graphPanel"), { nodes: nodeSet, edges: edgeSet }, options);
	nw.on("select", vertSelected);
	nw.on("doubleClick", console.log);
}
function enableEdgeFetcher() {
	g("fetchOutEdgeBtn").disabled = false;
	g("fetchInEdgeBtn").disabled = false;
}
function disableEdgeFetcher() {
	g("fetchOutEdgeBtn").disabled = true;
	g("fetchInEdgeBtn").disabled = true;
}
function fetchOutEdges() {
	fetchEdgesOf([selectedVert["id"]], "OUT");
}
function fetchInEdges() {
	fetchEdgesOf([selectedVert["id"]], "IN");
}
function vertSelected(params) {
	var props;
	var node;
	/* dont' revert this if condition, vs.js will return*/
	if (params["nodes"].length > 0) {
		node = nodeSet.get(params["nodes"][0])
		props = node.props;
		selectedVert = node;
		enableEdgeFetcher(node);
	} else {
		props = edgeSet.get(params["edges"][0]).props;
		disableEdgeFetcher();
	}
	if (props && props.length == 0)
		return;
	if (!props) {
		/* fetch all the properties of the vertex */
		searchInternal({ id: [params["nodes"][0]] }, function (verts) {
			var vert = verts[0];
			node.color = getVColor(vert)
			nodeSet.updateOnly(node)
			showProps(vert["properties"]);
			processVertSearchResponse(verts);
		})
		//showProps(props);
	} else {
		showProps(props);
	}
}
function showProps(props) {
	var text = "";
	for (var k in props) {
		text += `<tr><td>${k}</td><td>${JSON.stringify(props[k])}</td></tr>`;
	}
	g("propsPanel").innerHTML = `<h3>${props["id"]}</h3><table class="propsTable"><tr><th class="propHeader">Property</th><th class="propHeader">Value</th></tr>${text}</table>`
}
function g(id) { return document.getElementById(id); }
function t(id) { return g(id).value; }
function clearNw() {
	nodeSet.clear();
	edgeSet.clear();
}
function connect() {
	t("connString").split(";").forEach(kvp => {
		kvp = kvp.split("=");
		graphProps[kvp[0]] = kvp[1]
	});
	post(baseUrl + "/graph/open", graphProps, function (data) {
		GD = data;
		g("isConnected").style.color = "lawngreen";
		g("isConnected").title = "Connected";
		console.log(`graph descriptor (GD) = ${GD}`)
	});
}
function search() {
	searchInternal(qObj, processVertSearchResponse)
}
function searchInternal(q, callBack) {
	post(baseUrl + `/graph/searchv?gId=${GD}`, q, function (data) {
		console.log(data)
		callBack(JSON.parse(data));
	})
}
function fetchEdgesOf(vids, dir) {
	var query = `${baseUrl}/graph/edges?gId=${GD}&dir=${dir}`;
	vids.forEach(v => { query += `&id=${encodeURIComponent(v)}` });
	get(`${query}`, "", function (data) {
		console.log(data)
		processEdgeResponse(JSON.parse(data))
	})
}
function processEdgeResponse(edges) {
	edges.forEach(e => {
		var toNonNull;
		if (!(toNonNull = nodeSet.get(e["to"])) || !nodeSet.get(e["from"])) {
			var propName = toNonNull ? "from" : "to";
			addV({
				id: e[propName],
				label: e[propName],
				color: "lightgray"
			})
		}
		addE({
			id: getEdgeId(e),
			from: e["from"],
			to: e["to"],
			props: e["properties"],
			label: getELabel(e),
			arrows: {
				to: {
					enabled: true,
					type: 'arrow'
				}
			}
		})
	})
}
function getEdgeId(e) {
	return `${e['from']}=>${e['to']}`;
}
function processVertSearchResponse(verts) {
	var fetchEdge = [];
	verts.forEach(v => {
		fetchEdge.push(v["id"])
		addV({
			id: v["id"],
			label: getVLabel(v),
			props: v["properties"],
			color: getVColor(v)
		});
	})
	if (!graphProps.saveNet || graphProps.saveNet == "false")
		fetchEdgesOf(fetchEdge, "BOTH");
}
function getVColor(v) {
	var colors = {
		"India": "orange",
		"Lanka": "green"
	}
	return colors[v["properties"]["country"]]
}
function getVLabel(v) {
	return v["id"];
}
function getELabel(e) {
	return e["properties"]["rel"][0];
}
function addV(v) {
	try {
		nodeSet.add([v]);
	} catch (err) {
		console.error(err.message)
	}
}
function addE(e) {
	try {
		if (!edgeSet.get(getEdgeId(e)))
			edgeSet.add([e]);
	} catch (err) {
		console.error(err.message)
	}
}
function addQueryItemForm() {
	var e = document.createElement("div");
	e.classList.add("queryItem");
	e.innerHTML = `
				<input oninput="queryChanged()" class="queryKey" placeholder="key" />
				<input oninput="queryChanged()" class="queryVal" placeholder="value" />
				<button onclick="deleteQueryItemForm(this.parentNode)">-</button>
			`;
	g("queryBox").appendChild(e);
}
function deleteQueryItemForm(z) {
	if (z.parentNode.children.length > 1) {
		z.remove();
		queryChanged();
	}
}
function queryChanged() {
	var q = {};
	var queryItems = document.getElementsByClassName("queryItem");
	for (var i = 0; i < queryItems.length; i++) {
		var x = queryItems[i];
		var key = x.getElementsByClassName("queryKey")[0].value;
		var val = x.getElementsByClassName("queryVal")[0].value;
		if (!key || key == "" || !val || val == "")
			continue;
		if (!(key in q)) {
			q[key] = [];
		}
		q[key].push(val);
	}

	var Q = "";
	for (var k in q) {
		Q += `${Q != "" ? " AND " : ""}(${k} IN ${JSON.stringify(q[k])})`;
	}
	g("searchQuery").value = Q;
	qObj = q;
}

function expandNode(params) {

}