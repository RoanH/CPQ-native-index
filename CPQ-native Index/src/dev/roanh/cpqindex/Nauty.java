package dev.roanh.cpqindex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.roanh.gmark.conjunct.cpq.CPQ;
import dev.roanh.gmark.util.DataProxy;
import dev.roanh.gmark.util.UniqueGraph;
import dev.roanh.gmark.util.UniqueGraph.GraphNode;
import dev.roanh.gmark.util.Util;

public class Nauty{

	
	
	
	
	
	
	public static void runTest(){
		CPQ q = CPQ.parse("((1 ◦ 2) ∩ (1 ◦ 3))"); 
		ColoredGraph graph = toColoredGraph(Util.edgeLabelsToNodes(q.toQueryGraph().toUniqueGraph()));
		int[] colors = prepareColors(graph);
		
		System.out.println("Input:");
		for(int i = 0; i < graph.getNodeCount(); i++){
			System.out.println(i + " -> " + Arrays.toString(graph.getAdjacencyList()[i]));
		}
		
		//TODO, ensure colours are in label ID ascending order
		System.out.println("Colours: " + Arrays.toString(colors));
		int c = 0;
		for(List<Integer> group : graph.colorMap){
			System.out.println(c + " -> " + group);
			c++;
		}
		
		int[] relabel = computeCanonSparse(graph.getAdjacencyList(), colors);
		
		System.out.println("Mapping");
		int[] inv = new int[relabel.length];
 		for(int i = 0; i < relabel.length; i++){
			System.out.println(i + " -> " + relabel[i]);
			inv[relabel[i]] = i;
		}
		
		
		
		System.out.println("Relabelled with: " + Arrays.toString(relabel));	
		for(int i = 0; i < relabel.length; i++){
			System.out.print(i + " -> ");
			for(int v : graph.graph[relabel[i]]){
				System.out.print(inv[v]);//TODO probably need to sort each list
				System.out.print(' ');
			}
			
			System.out.println();
			
			
		}
		
	}
	
	
	
	
	
	
	protected static native int[] computeCanonSparse(int[][] adj, int[] colors);
	
	//TODO functions below could possibly be merged to simplify things
	
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
		for(List<Integer> group : graph.getColorMap()){
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
	public static <V, E> ColoredGraph toColoredGraph(UniqueGraph<V, E> graph){
		Map<Object, List<Integer>> colorMap = new HashMap<Object, List<Integer>>();
		int[][] adj = graph.toAdjacencyList();
		final Object nolabel = new Object();
		
		for(GraphNode<V, E> node : graph.getNodes()){
			V data = node.getData();
			if(data instanceof DataProxy){
				colorMap.computeIfAbsent(((DataProxy<?>)data).getData(), k->new ArrayList<Integer>()).add(node.getID());
			}else{
				colorMap.computeIfAbsent(nolabel, k->new ArrayList<Integer>()).add(node.getID());
			}
		}
		
		return new ColoredGraph(adj, colorMap.values());
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
		 * IDs of nodes with the same colour.
		 */
		private Collection<List<Integer>> colorMap;
		
		/**
		 * Constructs a new coloured graph with the given
		 * adjacency list and colour information.
		 * @param adj The adjacency list of the graph.
		 * @param colors The colour information.
		 */
		private ColoredGraph(int[][] adj, Collection<List<Integer>> colors){
			graph = adj;
			colorMap = colors;
		}
		
		/**
		 * Gets the total number of nodes in this graph.
		 * @return The total number of nodes in this graph.
		 */
		public int getNodeCount(){
			return graph.length;
		}
		
		/**
		 * Gets the colour map for this coloured graph, each
		 * list in the returned collection contains the IDs
		 * of nodes with the same colour.
		 * @return The colour map for this coloured graph.
		 */
		public Collection<List<Integer>> getColorMap(){
			return colorMap;
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
