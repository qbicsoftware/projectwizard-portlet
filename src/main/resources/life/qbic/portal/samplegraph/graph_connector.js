window.life_qbic_portal_samplegraph_ProjectGraph = function() {
	var diagramElement = this.getElement();
	var rpcProxy = this.getRpcProxy();

	this.onStateChange = function() {
		imagePath = this.getState().imagePath;
		init_graph_circles(this.getState().project);
	};

	var pi = Math.PI;
	var idToSample = {};
	
	function removeAllSpaces(text) {
		return text.replace(/\s/g, "");
	}

	function getTextWidth(text, font) {
		// re-use canvas object for better performance
		var canvas = getTextWidth.canvas
				|| (getTextWidth.canvas = document.createElement("canvas"));
		var context = canvas.getContext("2d");
		context.font = font;
		var metrics = context.measureText(text);
		return metrics.width;
	}

	var icons = {
		"dna" : "dna_filled.svg",
		"rna" : "rna_filled.svg",
		"peptides" : "peptide.svg",
		"proteins" : "protein.png",
		"smallmolecules" : "mol.png"
	};
	
	function addBGImages(canvas, data, prefix, size) {
		var base_dir = imagePath;
		canvas.append("defs").selectAll("patterns").data(data).enter().append(
				"pattern").attr('width', size).attr('height', size).attr("id",
				function(d) {
					return prefix + d;
				}).append("image").attr('width', size).attr('height', size)
				.attr("xlink:href", function(d) {
					return base_dir + icons[d];
				});
	}

	function init_graph_circles(samples) {
		var factor = 1;
		var rad = 20 * factor;
		var margin = 15 * factor; // distance from top and left
		var max_Y = 0, max_X = 0;// maximal node positions
		var longest_label = "x";

		var g = new dagre.graphlib.Graph();
		// Set an object for the graph label
		g.setGraph({marginx:margin,marginy:margin});

		// Default to assigning a new object as a label for each new edge.
		g.setDefaultEdgeLabel(function() {
			return {};
		});

		// if(d3.select(diagramElement).select("div")) {
		// d3.select(diagramElement).select("div").remove();
		// }

		var usedSymbols = new Set();
		var noSymbols = new Set();
		for ( var key in samples) {
			var sample = samples[key];
			idToSample[sample.id] = sample;
			// alert(sample.id);
			g.setNode(sample.id, {
				label : sample.name,
				width : rad * 2,
				height : rad * 2
			});
			for (var j = 0; j < sample.childIDs.length; j++) {
				g.setEdge(sample.id, sample.childIDs[j]);
			}
			// for(var j = 0; j < sample.children.length; j++) {
			// g.setEdge(sample.id, sample.children[j].id)
			// }
			label = sample.name;
			lowerLabel = removeAllSpaces(label.toLowerCase());
			res = Object.keys(icons).indexOf(lowerLabel);
			if (res === -1) {
				if (label !== "") {
					noSymbols.add(label);
				}
			} else {
				usedSymbols.add(label);
			}
		}

		// var color = d3.scaleOrdinal()
		// .domain(noSymbols)
		// .range(['#f7fbff','#c6dbef','#6baed6','#2171b5','#084594']);
		// var color = d3.scaleOrdinal(d3.schemeCategory20)
		// var color = d3.scaleOrdinal(d3.schemeCategory20b)
		var color = d3.scaleOrdinal(d3.schemeCategory10).domain(noSymbols);
		if (noSymbols.length > 10) {
			color = d3.scaleOrdinal(d3.schemeCategory20).domain(noSymbols);
		}

		dagre.layout(g);

		var nodes = g.nodes();
		// find maximum coordinates of nodes
		g.nodes().forEach(function(v) {
			var n = g.node(v);
			if (typeof n != 'undefined') {
				if (n.label != "") {
					var x = n["x"];
					var y = n["y"];
					max_X = Math.max(max_X, x);
					max_Y = Math.max(max_Y, y);
					if(n.label.length > longest_label.length) {
						longest_label = n.label;
					}
				}
			}
		});
		
		var legend_entry_height = rad + 5;
		// guess needed width of graph using 4-digit sample label
		var graph_width = factor * max_X + rad + getTextWidth("9999") + 20;
		var legend_width = factor * rad/2 + getTextWidth(longest_label)*2 + 20;			

		// minimum width needed to make the graph look nice
		var min_width = 300;
		
		var box_width = Math.max(min_width, Math.max(legend_width, graph_width));
		var box_height = factor * max_Y + rad + 20 + (noSymbols.size + usedSymbols.size)
					* (legend_entry_height+10);
		
		var svg = d3.select(diagramElement).append("div").classed(
				"svg-container", true) // container class to make it responsive
		.append("svg")
		// responsive SVG needs these 2 attributes and no width and height attr
		.attr("preserveAspectRatio", "xMinYMin meet").attr("viewBox",
				"0 0 " + box_width + " " + box_height + "")
		// class to make it responsive
		.classed("svg-content-responsive", true);

		addBGImages(svg, Object.keys(icons), "", rad * 2);
		addBGImages(svg, Object.keys(icons), "legend_", rad);

		g.edges().forEach(
				function(e) {
					var points = g.edge(e)["points"];
					var start = g.node(e.v);
					var end = g.node(e.w);
					var mid = points[1];
					var top_x = start["x"] * factor;
					var top_y = start["y"] * factor;
					var mid_x = mid["x"] * factor;
					var mid_y = mid["y"] * factor;
					var bot_x = end["x"] * factor;
					var bot_y = end["y"] * factor;
					d3.select("svg").append("line").attr("x1",
							top_x).attr("y1",
							top_y).attr("x2",
							mid_x).attr("y2",
							mid_y).attr("stroke-width", 2).attr("stroke", "black");
					d3.select("svg").append("line").attr("x1",
							mid_x).attr("y1",
							mid_y).attr("x2",
							bot_x).attr("y2",
							bot_y).attr("stroke-width", 2).attr("stroke", "black");
				});

		g.nodes().forEach(
				function(v) {
					var n = g.node(v);
					var data = idToSample[v];
					var label = n.label;
					if (label != "") {
						var lowerLabel = removeAllSpaces(label.toLowerCase());
						var x = n["x"] * factor;
						var y = n["y"] * factor;

						// main circles
						d3.select("svg").append("circle").attr("cx", x).attr(
								"cy", y).attr("r", rad).attr(
										"stroke", "black").style("fill",
								function() {
									if (usedSymbols.has(label)) {
										return "#3494F8";
									} else {
										return color(label);
									}
								})
							 .on('click', function() {
							     rpcProxy.onCircleClick(label, data.codes);
							 })
							 .on("mouseover", function(){
							 d3.select(this).attr("opacity",0.9);
							 })
							 .on("mouseout", function(){
							 d3.select(this).attr("opacity",1);
							 });
						// circles containing symbols
						if (usedSymbols.has(label)) {
							d3.select("svg").append("circle").attr("cx", x)
									.attr("cy", y).attr("r", rad).attr(
											"stroke", "black").attr("fill",
											"url(#" + lowerLabel + ")")
							 .on('click', function() {
							     rpcProxy.onCircleClick(label, data.codes);
							 })
							 .on("mouseover", function(){
							 d3.select(this).attr("opacity",0.3);
							 })
							 .on("mouseout", function(){
							 d3.select(this).attr("opacity",1);
							 })
							;
						}
						if(data.leaf) {
						// done and missing datasets (angles)
						var angle_done = 360 * data.measuredPercent / 100;
						if(angle_done != 0) {
						var arc_done = d3.arc().innerRadius(rad).outerRadius(
								rad + rad / 4).startAngle(0).endAngle(
								angle_done * (pi / 180)); // converting from
															// degrees to
															// radians
						d3.select("svg").append("path").attr("d", arc_done)
								.attr("fill", "green")
								.attr("stroke","black")
								.attr("transform",
									"translate(" + x + "," + y + ")")
								.on('click', function() {
									rpcProxy.onCircleClick(label, data.codes);
								})
								.on("mouseover", function(){
									d3.select(this).attr("opacity",0.6);
								})
								.on("mouseout", function(){
									d3.select(this).attr("opacity",1);
								});
						}
						if(angle_done != 360) {
						var arc_missing = d3.arc().innerRadius(rad)
								.outerRadius(rad + rad / 4).startAngle(
										angle_done * (pi / 180)).endAngle(
										360 * (pi / 180));
						d3.select("svg").append("path").attr("d", arc_missing)
								.attr("fill", "grey")
							    .attr("stroke","black")
								.attr("transform",
									"translate(" + x + "," + y + ")")
								.on('click', function() {
									rpcProxy.onCircleClick(label, data.codes);
										})
								.on("mouseover", function(){
									d3.select(this).attr("opacity",0.6);
								})
								.on("mouseout", function(){
									d3.select(this).attr("opacity",1);
								});
						}
						}
						// amount
						d3.select("svg").append("text").text(data.amount).attr(
								"font-family", "sans-serif").attr("font-size",
								"14px").attr("stroke", "black").attr(
								"text-anchor", "middle").attr("x", x + 22)
								.attr("y", y - 22);
					}
				});

		// legend
		var legend_x = margin;
		var legend_y = max_Y * factor + rad + 20;
        var used = [...usedSymbols];
        var nosym = [...noSymbols];
		d3.select("svg").selectAll("legends").data(nosym)
				.enter().append("circle").attr("cx", legend_x).attr("cy",
						function(d, i) {
							return legend_y + legend_entry_height * i + 10;
						}).attr("r", rad / 2)
				.attr("fill", function(d) {
					return color(d);
				});
		d3.select("svg").selectAll("legends").data(used)
				.enter().append("circle").attr("cx", legend_x).attr(
						"cy",
						function(d, i) {
							return legend_y + legend_entry_height
									* (noSymbols.size + i) + 10;
						}).attr("r", rad / 2)
				.attr("fill", "#3494F8");
		d3.select("svg").selectAll("legends").data(used)
				.enter().append("circle").attr("cx", legend_x).attr(
						"cy",
						function(d, i) {
							return legend_y + legend_entry_height
									* (noSymbols.size + i) + 10;
						}).attr("r", rad / 2)
				.attr("fill", function(d) {
					var type = removeAllSpaces(d.toLowerCase());
					return "url(#legend_" + type + ")";
				});
		// text
		d3.select("svg").selectAll("legends").data(used)
				.enter().append("text").text(function(d) {
					return d;
				}).attr("font-family", "sans-serif").attr("font-size", "14px")
				.attr("stroke", "black").attr("text-anchor", "left").attr("x",
						legend_x + margin).attr(
						"y",
						function(d, i) {
							return legend_y + legend_entry_height
									* (noSymbols.size + i) + 15;
						});
		d3.select("svg").selectAll("legends").data(nosym)
				.enter().append("text").text(function(d) {
					return d;
				}).attr("font-family", "sans-serif").attr("font-size", "14px")
				.attr("stroke", "black").attr("text-anchor", "left").attr("x",
						legend_x + margin).attr("y", function(d, i) {
					return legend_y + legend_entry_height * i + 15;
				});
	}
	;

};