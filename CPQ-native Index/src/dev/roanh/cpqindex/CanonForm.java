package dev.roanh.cpqindex;

import java.util.Arrays;
import java.util.Base64;
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
	/**
	 * Maximum number of bits that will ever be required to encode a vertex label ID.
	 */
	private static final int MAX_LABEL_BITS = 5;
	/**
	 * Maximum number of bits that will ever be required to encode a vertex ID.
	 */
	private static final int MAX_VERTEX_BITS = 10;
	private int source;
	private int target;
	private int[] nolabel;
	private Map<Predicate, int[]> labels = new LinkedHashMap<Predicate, int[]>();
	private int[][] graph;
	
	public CanonForm(CPQ cpq){
		this(cpq.toQueryGraph());
	}
	
	public CanonForm(QueryGraphCPQ graph){
		this(Util.edgeLabelsToNodes(graph.computeCore()), graph.getSourceVertex(), graph.getTargetVertex());
	}

	private CanonForm(UniqueGraph<Object, Void> core, Vertex src, Vertex trg){
		//compute a coloured graph
		ColoredGraph input = Nauty.toColoredGraph(core);
		
		//compute the canonical labelling with nauty
		int[] relabel = Nauty.computeCanonicalLabelling(input);

		//compute the inverse of the relabelling function.
		int[] inv = new int[relabel.length];
 		for(int i = 0; i < relabel.length; i++){
			inv[relabel[i]] = i;
		}
 		
 		//relabel the source and target node
 		source = inv[core.getNode(src).getID()];
 		target = inv[core.getNode(trg).getID()];
 		
 		//relabel nodes without label
 		nolabel = new int[input.getNoLabels().size()];
 		for(int i = 0; i < nolabel.length; i++){
 			nolabel[i] = inv[input.getNoLabels().get(i)];
 		}
 		Arrays.sort(nolabel);
 		
 		//relabel labels
 		for(Entry<Predicate, List<Integer>> pair : input.getLabels()){
 			List<Integer> old = pair.getValue();
 			int[] row = new int[old.size()];
 			for(int i = 0; i < old.size(); i++){
 				row[i] = inv[old.get(i)];
 			}
 			Arrays.sort(row);
 			labels.put(pair.getKey(), row);
 		}
 		
 		//relabel the graph itself
 		graph = new int[relabel.length][];
		for(int i = 0; i < relabel.length; i++){
			int[] row = input.getAdjacencyList()[relabel[i]];
			graph[i] = new int[row.length];
			for(int j = 0; j < row.length; j++){
				graph[i][j] = inv[row[j]];
			}
			Arrays.sort(graph[i]);
		}
	}
	
	/**
	 * Computes the string variant of this canonical form. This representation
	 * is human readable, but it is also larger than {@link #toBinaryCanon()}
	 * or {@link #toBase64Canon()};
	 * @return The string form of this canonical form.
	 */
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
	
	/**
	 * Computes the binary variant of this canonical form. This representation
	 * is smaller than the string variant produced by {@link #toStringCanon()},
	 * but can optionally be encoded in Base64 using {@link #toBase64Canon()}.
	 * @return The binary form of this canonical form.
	 */
	public byte[] toBinaryCanon(){
		//bits per vertex
		int vb = (int)Math.ceil(Math.log(graph.length) / Math.log(2));
		
		//total required bits
		int bits = MAX_VERTEX_BITS + vb * 3 + nolabel.length * vb + labels.size() * MAX_LABEL_BITS + MAX_LABEL_BITS;
		for(int[] lab : labels.values()){
			bits += lab.length * vb + vb;
		}
		
		bits += graph.length * vb;
		for(int[] edges : graph){
			bits += edges.length * vb;
		}
		
		//write canonical form
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
		
		return out.getData();
	}
	
	/**
	 * Computes the Base64 encoded string of {@link #toBinaryCanon()}.
	 * @return The Base64 encoded version of the binary canonical form.
	 */
	public String toBase64Canon(){
		return Base64.getEncoder().encodeToString(toBinaryCanon());
	}
}
