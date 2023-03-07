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
	
	
	public CanonForm(CPQ cpq){
		this(cpq.toQueryGraph());
	}
	
	
	public static void main(String[] args) throws UnsatisfiedLinkError, IOException{
		Main.loadNatives();
		CanonForm cf = new CanonForm(CPQ.parse("((1 ◦ 2) ∩ (1 ◦ 3))").toQueryGraph());
		System.out.println(cf.toStringCanon());
		System.out.println(Arrays.toString(cf.toBinaryCanon()));
	}
	
	public CanonForm(QueryGraphCPQ graph){
		UniqueGraph<Vertex, Predicate> core = graph.computeCore();
		canonise(Util.edgeLabelsToNodes(core), graph.getSourceVertex(), graph.getTargetVertex());
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
 			System.out.println(pair.getKey());
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
		int vb = (int)Math.ceil(Math.log(graph.length) / Math.log(2));
		
		int bits = MAX_VERTEX_BITS + vb * 3 + nolabel.length * vb + labels.size() * MAX_LABEL_BITS + MAX_LABEL_BITS;
		for(int[] lab : labels.values()){
			bits += lab.length * vb + vb;
		}
		
		bits += graph.length * vb;
		for(int[] edges : graph){
			bits += edges.length * vb;
		}
		
		System.out.println("bits: " + bits);
		
		BitWriter out = new BitWriter(bits);
		out.writeInt(graph.length, MAX_VERTEX_BITS);
		out.writeInt(source, vb);
		out.writeInt(target, vb);
		out.writeInt(nolabel.length, vb);
		for(int v : nolabel){
			out.writeInt(v, vb);
		}
		out.writeInt(labels.size(), MAX_LABEL_BITS);
		for(Entry<Predicate, int[]> entry : labels.entrySet()){
			out.writeInt(entry.getKey().getID(), MAX_LABEL_BITS);
			out.writeInt(entry.getValue().length, vb);
			for(int v : entry.getValue()){
				out.writeInt(v, vb);
			}
		}
		for(int[] edges : graph){
			out.writeInt(edges.length, vb);
			for(int v : edges){
				out.writeInt(v, vb);
			}
		}
		
		System.out.println(out.toBinaryString());
		return out.getData();
	}
}
