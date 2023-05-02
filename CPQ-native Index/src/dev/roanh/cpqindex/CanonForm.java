package dev.roanh.cpqindex;

import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import dev.roanh.cpqindex.Nauty.ColoredGraph;
import dev.roanh.gmark.conjunct.cpq.CPQ;
import dev.roanh.gmark.conjunct.cpq.QueryGraphCPQ;
import dev.roanh.gmark.conjunct.cpq.QueryGraphCPQ.Vertex;
import dev.roanh.gmark.core.graph.Predicate;
import dev.roanh.gmark.util.UniqueGraph;
import dev.roanh.gmark.util.Util;

/**
 * Utility class to compute and represent the canonical form of a CPQ.
 * @author Roan
 * @see Nauty
 */
public class CanonForm{
	/**
	 * Maximum number of bits that will ever be required to encode a vertex label ID.
	 */
	private static final int MAX_LABEL_BITS = 5;
	/**
	 * Maximum number of bits that will ever be required to encode a vertex ID.
	 */
	private static final int MAX_VERTEX_BITS = 10;
	/**
	 * The vertex ID of the source vertex of the CPQ.
	 */
	private final int source;
	/**
	 * The vertex ID of the target vertex of the CPQ.
	 */
	private final int target;
	/**
	 * A map containing the IDs of vertices with a specific label.
	 */
	private final Map<Predicate, Integer> labels;
	/**
	 * The adjacency list form of the canonically labelled transformed CPQ query graph.
	 */
	private final int[][] graph;
	/**
	 * The CPQ this canonical form was constructed from.
	 */
	private final CPQ cpq;
	
	/**
	 * Constructs a new canonical form with the given data.
	 * @param source The ID of the source vertex.
	 * @param target The ID of the target vertex.
	 * @param labels The IDs of labelled nodes by label.
	 * @param graph The canonically labelled graph.
	 * @param cpq The original CPQ.
	 */
	private CanonForm(int source, int target, Map<Predicate, Integer> labels, int[][] graph, CPQ cpq){
		this.source = source;
		this.target = target;
		this.labels = labels;
		this.graph = graph;
		this.cpq = cpq;
	}
	
	/**
	 * Gets the CPQ this canonical form was constructed from.
	 * @return The CPQ this canonical form was constructed from.
	 */
	public CPQ getCPQ(){
		return cpq;
	}
	
	/**
	 * Constructs a canonical form for the core of the given CPQ.
	 * @param cpq The CPQ to compute a canonical form for.
	 * @return A future representing the computed canonical form.
	 */
	public static CanonFuture computeCanon(CPQ cpq){
		QueryGraphCPQ core = cpq.toQueryGraph().computeCore();
		UniqueGraph<Object, Void> transformed = Util.edgeLabelsToNodes(core.toUniqueGraph());
		Vertex src = core.getSourceVertex();
		Vertex trg = core.getTargetVertex();
		
		//compute a coloured graph
		ColoredGraph input = Nauty.toColoredGraph(transformed, src, trg);
		
		//compute the canonical labelling with nauty
		Future<int[]> relabel = Nauty.computeCanonicalLabelling(input);

		//a future representing the final result
		return new CanonFuture(input, relabel, transformed.getNode(src).getID(), transformed.getNode(trg).getID(), cpq);
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
		buf.append(',');
		
		for(Entry<Predicate, Integer> pair : labels.entrySet()){
			buf.append('l');
			buf.append(pair.getKey().getID());
			buf.append("=");
			buf.append(pair.getValue());
			buf.append(",");
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
		int bits = MAX_VERTEX_BITS + vb * 2 + labels.size() * MAX_LABEL_BITS + MAX_LABEL_BITS + vb * labels.size();
		
		bits += graph.length * vb;
		for(int[] edges : graph){
			bits += edges.length * vb;
		}
				
		//write canonical form
		BitWriter out = new BitWriter(bits);
		out.writeInt(graph.length, MAX_VERTEX_BITS);
		out.writeInt(source, vb);
		out.writeInt(target, vb);
		
		out.writeInt(labels.size(), MAX_LABEL_BITS);
		for(Entry<Predicate, Integer> entry : labels.entrySet()){
			out.writeInt(entry.getKey().getID(), MAX_LABEL_BITS);
			out.writeInt(entry.getValue(), vb);
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
	
	@Override
	public boolean equals(Object obj){
		return obj instanceof CanonForm && Arrays.equals(toBinaryCanon(), ((CanonForm)obj).toBinaryCanon());
	}
	
	@Override
	public int hashCode(){
		return Objects.hashCode(toBinaryCanon());
	}
	
	/**
	 * Future representing an ongoing canonisation task.
	 * @author Roan
	 */
	public static final class CanonFuture implements Future<CanonForm>{
		/**
		 * The nauty future for the task computing the
		 * canonical graph relabelling.
		 */
		private Future<int[]> nautyFuture;
		/**
		 * The input colour graph.
		 */
		private ColoredGraph input;
		/**
		 * The ID of the input source vertex.
		 */
		private int src;
		/**
		 * The ID of the input target vertex.
		 */
		private int trg;
		/**
		 * The original input CPQ.
		 */
		private CPQ cpq;
		
		/**
		 * Constructs a new canonisation future.
		 * @param input The original input graph being canonised.
		 * @param nautyFuture The future for the relabelling task.
		 * @param src The original source vertex.
		 * @param trg The original target vertex.
		 * @param cpq The original input CPQ.
		 */
		private CanonFuture(ColoredGraph input, Future<int[]> nautyFuture, int src, int trg, CPQ cpq){
			this.nautyFuture = nautyFuture;
			this.input = input;
			this.src = src;
			this.trg = trg;
			this.cpq = cpq;
		}

		@Override
		public boolean cancel(boolean mayInterruptIfRunning){
			return nautyFuture.cancel(mayInterruptIfRunning);
		}

		@Override
		public boolean isCancelled(){
			return nautyFuture.isCancelled();
		}

		@Override
		public boolean isDone(){
			return nautyFuture.isDone();
		}

		@Override
		public CanonForm get() throws InterruptedException, ExecutionException{
			return computeResult(nautyFuture.get());
		}

		@Override
		public CanonForm get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException{
			return computeResult(nautyFuture.get(timeout, unit));
		}
		
		/**
		 * Computes the canonical form of the input CPQ
		 * giving the relabelling mapping from nauty.
		 * @param relabel The nauty graph relabelling mapping.
		 * @return The fully computed canonical form.
		 * @see Nauty#computeCanonicalLabelling(ColoredGraph)
		 */
		private CanonForm computeResult(int[] relabel){
			//compute the inverse of the relabelling function.
			int[] inv = new int[relabel.length];
	 		for(int i = 0; i < relabel.length; i++){
				inv[relabel[i]] = i;
			}
	 		
	 		//relabel the source and target node
	 		int source = inv[src];
	 		int target = inv[trg];
	 		
	 		//relabel labels
	 		Map<Predicate, Integer> labels = new LinkedHashMap<Predicate, Integer>();
	 		for(Entry<Predicate, List<Integer>> pair : input.getLabels()){
	 			labels.put(pair.getKey(), pair.getValue().size());
	 		}
	 		
	 		//relabel the graph itself
	 		int[][] graph = new int[relabel.length][];
			for(int i = 0; i < relabel.length; i++){
				int[] row = input.getAdjacencyList()[relabel[i]];
				graph[i] = new int[row.length];
				for(int j = 0; j < row.length; j++){
					graph[i][j] = inv[row[j]];
				}
				Arrays.sort(graph[i]);
			}
			
			return new CanonForm(source, target, labels, graph, cpq);
		}
	}
}
