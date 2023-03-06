package dev.roanh.cpqindex;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import dev.roanh.cpqindex.Nauty.ColoredGraph;
import dev.roanh.gmark.conjunct.cpq.CPQ;
import dev.roanh.gmark.conjunct.cpq.QueryGraphCPQ;
import dev.roanh.gmark.conjunct.cpq.QueryGraphCPQ.Vertex;
import dev.roanh.gmark.core.graph.Predicate;
import dev.roanh.gmark.util.UniqueGraph;
import dev.roanh.gmark.util.Util;

public class CanonForm{
	private static final int MAX_LABEL_BITS = 5;
	private static final int MAX_VERTEX_BITS = 10;
	private int source;
	private int target;
	private int[] nolabel;
	private Map<Predicate, int[]> labels = new LinkedHashMap<Predicate, int[]>();
	private int[][] graph;
	
	
	private CanonForm(){
		
	}
	
	
	public static void main(String[] args) throws UnsatisfiedLinkError, IOException{
		Main.loadNatives();
		CanonForm cf = canonise(CPQ.parse("((1 ◦ 2) ∩ (1 ◦ 3))").toQueryGraph());
		System.out.println(cf.toStringCanon());
	}
	
	public static final CanonForm canonise(QueryGraphCPQ graph){
		UniqueGraph<Vertex, Predicate> core = graph.computeCore();
		return new CanonForm().canonise(Util.edgeLabelsToNodes(core), graph.getSourceVertex(), graph.getTargetVertex());
	}//TODO this constructor abuse is horrible
	
	
	private final CanonForm canonise(UniqueGraph<Object, Void> core, Vertex src, Vertex trg){
		ColoredGraph input = Nauty.toColoredGraph(core);
		
		int[] relabel = Nauty.computeCanonicalLabelling(input);
		
		System.out.println("Mapping");
		int[] inv = new int[relabel.length];
 		for(int i = 0; i < relabel.length; i++){
			System.out.println(i + " -> " + relabel[i]);
			inv[relabel[i]] = i;
		}
 		
 		source = inv[core.getNode(src).getID()];
 		target = inv[core.getNode(trg).getID()];
 		
 		nolabel = new int[input.getNoLabels().size()];
 		for(int i = 0; i < nolabel.length; i++){
 			nolabel[i] = inv[input.getNoLabels().get(i)];
 		}
 		Arrays.sort(nolabel);
 		
 		for(Entry<Predicate, List<Integer>> pair : input.getLabels()){
 			int[] row = new int[pair.getValue().size()];
 			int i = 0;
 			for(int id : pair.getValue()){
 				row[i++] = inv[id];
 			}
 			Arrays.sort(row);
 			labels.put(pair.getKey(), row);
 		}
 		
 		System.out.println("Relabelled with: " + Arrays.toString(relabel));	
 		graph = new int[relabel.length][];
		for(int i = 0; i < relabel.length; i++){
			System.out.print(i + " -> ");
			int[] row = input.getAdjacencyList()[relabel[i]];
			graph[i] = new int[row.length];
			int j = 0;
			for(int v : row){
				graph[i][j++] = inv[v];
				System.out.print(inv[v]);//TODO probably need to sort each list
				System.out.print(' ');
			}
			
			Arrays.sort(graph[i]);
			
			System.out.println();
			
			
		}
		
		
		
		
		
		
		
		
		
		return this;
	}
	
	public String toStringCanon(){
		StringBuilder buf = new StringBuilder();
		buf.append("s=");
		buf.append(source);
		buf.append(",t=");
		buf.append(target);
		buf.append(",v={");
		for(int i : nolabel){
			buf.append(i);
			buf.append(',');
		}
		buf.deleteCharAt(buf.length() - 1);
		buf.append("},");
		for(Entry<Predicate, int[]> pair : labels.entrySet()){
			buf.append('l');
			buf.append(pair.getKey().getID());
			buf.append("={");
			for(int i : pair.getValue()){
				buf.append(i);
				buf.append(',');
			}
			buf.deleteCharAt(buf.length() - 1);
			buf.append("},");
		}
		for(int i = 0; i < graph.length; i++){
			buf.append('e');
			buf.append(i);
			buf.append("={");
			for(int v : graph[i]){
				buf.append(v);
				buf.append(',');
			}
			if(graph[i].length != 0){
				buf.deleteCharAt(buf.length() - 1);
			}
			buf.append("},");
		}
		buf.deleteCharAt(buf.length() - 1);
		return buf.toString();
	}
	
	public byte[] toBinaryCanon(){
		//TODO
		
		return null;
	}
}
