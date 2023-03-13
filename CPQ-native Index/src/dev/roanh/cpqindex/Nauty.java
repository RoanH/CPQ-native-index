package dev.roanh.cpqindex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import dev.roanh.gmark.core.graph.Predicate;
import dev.roanh.gmark.util.DataProxy;
import dev.roanh.gmark.util.UniqueGraph;
import dev.roanh.gmark.util.UniqueGraph.GraphNode;

/**
 * This class provides and interface to the native binding for nauty.
 * @author Roan
 *
 */
public class Nauty{
	
	/**
	 * Computes a canonical labelling of the given coloured graph. The labelling
	 * is returned as an array of integers showing how to relabel the vertices
	 * in the graph. Each index of this array contains the ID of the vertex that
	 * previously had the ID of that index in the array.
	 * @param graph The graph to compute a canonical labelling of.
	 * @return The computed relabelling mapping.
	 */
	public static int[] computeCanonicalLabelling(ColoredGraph graph){
		int[] colors = prepareColors(graph);
		return computeCanonSparse(graph.getAdjacencyList(), colors);
	}
	
	/**
	 * Performs a canonical labelling of the given input graph.
	 * @param adj The input graph in adjacency list format, <code>n</code>
	 *        arrays with each the indices of the neighbours of the <code>
	 *        n</code>-th vertex.
	 * @param colors The array containing raw color information data. Contains vertex
	 *        indices in blocks of the same color with the start of a block of the same
	 *        color being indicated by a negated value. All vertex indices are also always
	 *        one higher than their actual index in the graph.
	 * @return A canonical relabelling of the graph returned as an array of integers showing
	 *         how to relabel the vertices in the graph. Each index of this array contains
	 *         the ID of the vertex that previously had the ID of that index in the array.
	 */
	protected static native int[] computeCanonSparse(int[][] adj, int[] colors);
	
	/**
	 * Computes a nauty and traces compatible array of color data. The
	 * returned array will have consecutive sections of nodes with the
	 * same color. The node is indicated with a number one higher than
	 * the ID of the actual it corresponds to. Negated number indicate
	 * the end of a range of nodes with the same color.
	 * @param graph The coloured graph to compute color data from.
	 * @return The constructed colour data.
	 */
	protected static int[] prepareColors(ColoredGraph graph){
		int[] colors = new int[graph.getNodeCount()];
		int idx = 0;
		for(List<Integer> group : graph.getColorLists()){
			for(int i = 0; i < group.size() - 1; i++){
				colors[idx++] = group.get(i) + 1;
			}
			colors[idx++] = -group.get(group.size() - 1) - 1;
		}
		return colors;
	}
	
	/**
	 * Converts the given input graph to a coloured graph instance
	 * by grouping vertices with their colour determined by the
	 * content of their {@link DataProxy} instance. All vertex data
	 * objects that are not DataProxy instances are given the same colour.
	 * @param <V> The vertex data type.
	 * @param <E> The edge label data type.
	 * @param graph The input graph to transform.
	 * @return The constructed coloured graph.
	 * @see ColoredGraph
	 */
	@SuppressWarnings("unchecked")
	public static <V, E> ColoredGraph toColoredGraph(UniqueGraph<V, E> graph){
		Map<Predicate, List<Integer>> colorMap = new HashMap<Predicate, List<Integer>>();
		int[][] adj = graph.toAdjacencyList();
		List<Integer> nolabel = new ArrayList<Integer>();
		
		for(GraphNode<V, E> node : graph.getNodes()){
			V data = node.getData();
			if(data instanceof DataProxy){
				colorMap.computeIfAbsent(((DataProxy<Predicate>)data).getData(), k->new ArrayList<Integer>()).add(node.getID());
			}else{
				nolabel.add(node.getID());
			}
		}
		
		return new ColoredGraph(
			adj,
			colorMap.entrySet().stream().sorted(Entry.comparingByKey()).collect(Collectors.toCollection(ArrayList::new)),
			nolabel
		);
	}
	
	/**
	 * Represents a coloured graph.
	 * @author Roan
	 * @see #toColoredGraph(UniqueGraph)
	 */
	public static class ColoredGraph{
		/**
		 * The adjacency list representing the graph.
		 */
		private int[][] graph;
		/**
		 * A collection of lists where each list has the
		 * IDs of nodes with the same colour. The label
		 * for the colour is also present in each entry.
		 * Excludes the special collection of nodes without
		 * label. The list items are sorted on predicate ID.
		 */
		private List<Entry<Predicate, List<Integer>>> labels;
		/**
		 * List of node IDs that have no label.
		 */
		private List<Integer> noLabel;
		
		/**
		 * Constructs a new coloured graph with the given
		 * adjacency list and colour information.
		 * @param adj The adjacency list of the graph.
		 * @param labels A list of node IDs for each label.
		 * @param nolabel A list of node IDs without any label.
		 */
		private ColoredGraph(int[][] adj, List<Entry<Predicate, List<Integer>>> labels, List<Integer> nolabel){
			graph = adj;
			this.labels = labels;
			noLabel = nolabel;
		}
		
		/**
		 * Gets the total number of nodes in this graph.
		 * @return The total number of nodes in this graph.
		 */
		public int getNodeCount(){
			return graph.length;
		}
		
		/**
		 * Gets the IDs of nodes without a label/colour.
		 * @return The IDs of nodes without a label/colour.
		 */
		public List<Integer> getNoLabels(){
			return noLabel;
		}
		
		/**
		 * Gets a list of colour information in the form of a list of
		 * entries where each entry has the colour label and IDs of
		 * vertices with that colour.
		 * @return Gets the IDs of the coloured vertices and labels.
		 */
		public List<Entry<Predicate, List<Integer>>> getLabels(){
			return labels;
		}
		
		/**
		 * Gets the colour information of this graph as a list of
		 * lists where each list has the IDs of vertices of the same colour.
		 * @return The colour information as a list of lists.
		 */
		public List<List<Integer>> getColorLists(){
			List<List<Integer>> colors = new ArrayList<List<Integer>>(labels.size() + 1);
			labels.forEach(e->colors.add(e.getValue()));
			colors.add(noLabel);
			return colors;
		}
		
		/**
		 * Gets the adjacency list representation of this graph.
		 * @return The adjacency list representation of this graph.
		 */
		public int[][] getAdjacencyList(){
			return graph;
		}
	}
}
