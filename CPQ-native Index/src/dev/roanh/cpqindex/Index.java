package dev.roanh.cpqindex;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Objects;
import java.util.Set;

import dev.roanh.gmark.conjunct.cpq.CPQ;
import dev.roanh.gmark.core.graph.Predicate;
import dev.roanh.gmark.util.RangeList;
import dev.roanh.gmark.util.UniqueGraph;
import dev.roanh.gmark.util.UniqueGraph.GraphEdge;
import dev.roanh.gmark.util.Util;

/**
 * Implementation of a graph database index based on k-path-bisimulation
 * and with support for indexing by CPQ cores. This index is based on the
 * path index proposed by Yuya Sasaki, George Fletcher and Makoto Onizuka.
 * @author Roan
 * @param <V> The graph vertex data type.
 * @see <a href="https://doi.org/10.1109/ICDE53745.2022.00054">Yuya Sasaki, George Fletcher and Makoto Onizuka,
 *      "Language-aware indexing for conjunctive path queries", in IEEE 38th ICDE, 2022</a>
 * @see <a href="https://github.com/yuya-s/CPQ-aware-index">yuya-s/CPQ-aware-index</a>
 */
public class Index<V extends Comparable<V>>{
	private final boolean computeCores;
	private final int k;
	private List<Block> blocks = new ArrayList<Block>();
	private Map<String, List<Block>> coreToBlock = new HashMap<String, List<Block>>();
	
	public static void main(String[] args){
		try{
			Main.loadNatives();
		}catch(UnsatisfiedLinkError | IOException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Predicate l0 = new Predicate(0, "0");
		Predicate l1 = new Predicate(1, "1");
		Predicate l2 = new Predicate(2, "2");
		Predicate l3 = new Predicate(3, "3");
		
		UniqueGraph<Integer, Predicate> g = new UniqueGraph<Integer, Predicate>();
		g.addUniqueNode(0);
		g.addUniqueNode(1);
		g.addUniqueNode(2);
		g.addUniqueNode(3);
		g.addUniqueNode(4);
		g.addUniqueNode(5);
		g.addUniqueNode(6);

		g.addUniqueEdge(0, 1, l0);
		g.addUniqueEdge(0, 2, l1);
		g.addUniqueEdge(1, 3, l0);
		g.addUniqueEdge(2, 3, l1);
		g.addUniqueEdge(3, 4, l2);
		g.addUniqueEdge(3, 5, l3);
		g.addUniqueEdge(4, 6, l2);
		g.addUniqueEdge(5, 6, l3);
		
		Index<Integer> eq = new Index<Integer>(g, 4, false);
		
//		Predicate a = new Predicate(0, "0");
//		Predicate b = new Predicate(1, "1");
//		Predicate c = new Predicate(2, "2");
//		
//		UniqueGraph<Integer, Predicate> g = new UniqueGraph<Integer, Predicate>();
//		g.addUniqueNode(0);
//		g.addUniqueNode(1);
//		g.addUniqueNode(2);
//		g.addUniqueNode(3);
//		g.addUniqueNode(4);
//
//		g.addUniqueEdge(0, 1, c);
//		g.addUniqueEdge(1, 2, b);
//		g.addUniqueEdge(1, 3, a);
//		g.addUniqueEdge(2, 4, b);
//		g.addUniqueEdge(3, 4, a);
//		
//		EquivalenceClass<Integer> eq = new EquivalenceClass<Integer>(2);
		
//		UniqueGraph<Integer, Predicate> g = new UniqueGraph<Integer, Predicate>();
//		g.addUniqueNode(0);
//		g.addUniqueNode(1);
//
//		g.addUniqueEdge(0, 1, a);
//		g.addUniqueEdge(1, 1, b);
//		
//		EquivalenceClass<Integer> eq = new EquivalenceClass<Integer>(2);
		
		System.out.println("Final blocks for CPQ" + eq.k + " | " + eq.blocks.size());
		eq.sort();
		eq.blocks.forEach(System.out::println);
		
		eq.print(9);
	}
	
	public Index(UniqueGraph<V, Predicate> g, int k) throws IllegalArgumentException{
		this(g, k, true);
	}
	
	public Index(UniqueGraph<V, Predicate> g, int k, boolean computeCores) throws IllegalArgumentException{
		this.computeCores = computeCores;
		this.k = k;
		computeBlocks(partition(g));
		mapCoresToBlocks();
	}
	
	public List<Pair> query(CPQ cpq) throws IllegalArgumentException{
		if(cpq.getDiameter() > k){
			throw new IllegalArgumentException("");
		}
		
		return coreToBlock.getOrDefault(new CanonForm(cpq).toBase64Canon(), Collections.emptyList()).stream().flatMap(b->b.getPaths().stream()).collect(Collectors.toList());
	}
	
	public List<Block> getBlocks(){
		return blocks;
	}
	
	/**
	 * Sorts all the list of blocks of this index. There's is no real
	 * reason to do this other than to make the output of {@link #print(int)}
	 * more organised.
	 * @see #print(int)
	 */
	public void sort(){
		for(Block block : blocks){
			block.paths.sort(this::sortPairs);
			block.labels.sort((a, b)->{
				int c = Integer.compare(a.size(), b.size());
				if(c == 0){
					for(int i = 0; i < a.size(); i++){
						c = a.get(i).compareTo(b.get(i));
						if(c != 0){
							return c;
						}
					}
				}
				
				return 0;
			});
		}
		
		blocks.sort(Comparator.comparing(b->b.paths.get(0), this::sortPairs));
	}
	
	/**
	 * Prints the index to standard output.
	 * @param blockWidth The width to allocate for each block column.
	 * @see #sort()
	 */
	public void print(int blockWidth){
		StringBuilder[] out = new StringBuilder[3 + blocks.stream().mapToInt(b->b.paths.size() + b.labels.size()).max().orElse(0)];
		int labStart = 3 + blocks.stream().mapToInt(b->b.paths.size()).max().orElse(0);
		for(int i = 0; i < out.length; i++){
			out[i] = new StringBuilder();
		}
		
		int col = 0;
		for(Block block : blocks){
			out[0].append(block.id);
			for(int i = 0; i < block.paths.size(); i++){
				out[i + 2].append(block.paths.get(i));
			}
			
			out[labStart - 1].append("-----");
			for(int i = 0; i < block.labels.size(); i++){
				for(Predicate p : block.labels.get(i)){
					out[labStart + i].append(p.getAlias());
				}
			}
			
			col++;
			for(int i = 0; i < out.length; i++){
				while(out[i].length() < blockWidth * col){
					out[i].append(' ');
				}
			}
		}
		
		for(StringBuilder buf : out){
			System.out.println(buf.toString());
		}
	}
	
	private void mapCoresToBlocks(){
		for(Block block : blocks){
			for(String core : block.canonCores){//TODO again, should really use the byte form
				coreToBlock.computeIfAbsent(core, k->new ArrayList<Block>()).add(block);
			}
		}
	}
	
	private void computeBlocks(RangeList<List<LabelledPath>> segments){
		Map<Pair, Block> pairMap = new HashMap<Pair, Block>();
		
		for(int j = 0; j < k; j++){
			List<LabelledPath> segs = segments.get(j);
			int start = 0;
			int lastId = segs.get(0).segId;
			for(int i = 0; i <= segs.size(); i++){
				if(i == segs.size() || segs.get(i).segId != lastId){
					List<LabelledPath> slice = segs.subList(start, i);
					
					List<Block> inherited = new ArrayList<Block>();
					if(j > 0){
						for(LabelledPath path : slice){
							Block b = pairMap.remove(path.pair);
							if(b != null){
								inherited.add(b);
							}
						}
					}
					
					Block block = new Block(slice, inherited);
					if(j != k - 1){
						for(Pair pair : block.paths){
							pairMap.put(pair, block);
						}
					}else{
						blocks.add(block);
					}
					
					if(i != segs.size()){
						lastId = segs.get(i).segId;
						start = i;
					}
				}
			}
		}
		
		//any remaining pairs denote unused blocks
		pairMap.values().stream().distinct().forEach(blocks::add);
	}
	
	//partition according to k-path-bisimulation
	private RangeList<List<LabelledPath>> partition(UniqueGraph<V, Predicate> g) throws IllegalArgumentException{
		if(k <= 0){
			throw new IllegalArgumentException("Invalid value of k for bisimulation, has to be 1 or greater.");
		}
		
		RangeList<List<LabelledPath>> segments = new RangeList<List<LabelledPath>>(k, ArrayList::new);
		
		//classes for 1-path-bisimulation
		Map<Pair, LabelledPath> pathMap = new HashMap<Pair, LabelledPath>();
		for(GraphEdge<V, Predicate> edge : g.getEdges()){
			//forward and backward edges are just the labels on those edges
			pathMap.computeIfAbsent(new Pair(edge.getSource(), edge.getTarget()), LabelledPath::new).addLabel(edge.getData());
			pathMap.computeIfAbsent(new Pair(edge.getTarget(), edge.getSource()), LabelledPath::new).addLabel(edge.getData().getInverse());
		}
		
		//sort 1-path
		List<LabelledPath> segOne = segments.get(0);
		pathMap.values().stream().sorted(this::sortOnePath).forEachOrdered(segOne::add);
		
		//assign block IDs
		LabelledPath prev = null;
		int id = 1;
		for(LabelledPath seg : segOne){
			if(prev != null && (!seg.labels.equals(prev.labels) || seg.isLoop() ^ prev.isLoop())){
				//if labels and cyclic patterns (loop) are not the same a new ID is started
				id++;
			}

			seg.segId = id;
			prev = seg;
		}
		
		//classes for 2-path-bisimulation to k-path-bisimulation
		for(int i = 1; i < k; i++){
			pathMap.clear();

			id++;
			for(int k1 = i - 1; k1 >= 0; k1--){//all combinations to make CPQi
				int k2 = i - k1 - 1;
				
				for(LabelledPath seg : segments.get(k1)){
					for(LabelledPath end : segments.get(k2)){
						if(!seg.pair.trg.equals(end.pair.src)){
							continue;
						}
						
						Pair key = new Pair(seg.pair.src, end.pair.trg);
						LabelledPath path = pathMap.computeIfAbsent(key, LabelledPath::new);
						
						path.addSegment(seg, end);
						if(k2 == 0){//slight optimisation, since we only need one combination to find all paths
							for(List<Predicate> labels : seg.labels){
								for(List<Predicate> label : end.labels){
									path.addLabel(labels, label);
								}
							}
						}
					}
				}
			}

			//sort
			List<LabelledPath> segs = segments.get(i);
			pathMap.values().stream().sorted(this::sortPaths).forEachOrdered(segs::add);

			//assign IDs
			prev = null;
			for(LabelledPath path : segs){
				if(prev != null && (!path.equalSegments(prev) || prev.isLoop() ^ path.isLoop())){
					//increase id if loop status or segments differs
					id++;
				}

				path.segId = id;
				prev = path;
			}
		}
		
		return segments;
	}
	
	private int sortPairs(Pair a, Pair b){
		int c = a.getSource().compareTo(b.getSource());
		if(c == 0){
			c = a.getTarget().compareTo(b.getTarget());
		}

		return c;
	}
	
	private int sortPaths(LabelledPath a, LabelledPath b){
		if(a.equalSegments(b)){
			if(a.isLoop() && b.isLoop()){
				return a.hashCode() - b.hashCode();
			}else if(a.isLoop()){
				return 1;
			}else if(b.isLoop()){
				return -1;
			}else if(a.pair.src.compareTo(b.pair.src) < 0){
				return 1;
			}else if(a.pair.src.equals(b.pair.src) && a.pair.trg.compareTo(b.pair.trg) < 0){
				return 1;
			}else{
				return -1;
			}
		}else{
			return a.segs.hashCode() - b.segs.hashCode();
		}
	}
	
	private int sortOnePath(LabelledPath a, LabelledPath b){
		if(a.labels.equals(b.labels)){
			if(a.isLoop()){
				return 1;
			}else if(b.isLoop()){
				return -1;
			}else if(a.pair.src.compareTo(b.pair.src) < 0){
				return 1;
			}else if(a.pair.src.equals(b.pair.src) && a.pair.trg.compareTo(b.pair.trg) < 0){
				return 1;
			}else{
				return -1;
			}
		}else{
			return a.labels.hashCode() - b.labels.hashCode();
		}
	}
	
	public final class Block{
		private final int id;
		private List<Pair> paths;
		private List<List<Predicate>> labels;//TODO technically no need to store this for a core based index, probably turn it off for real use
		private List<CPQ> cores = new ArrayList<CPQ>();//TODO technically only need #canonCores, but this is good for debugging
		private Set<String> canonCores = new HashSet<String>();//TODO should use bytes rather than strings, but strings are easier to debug
		
		private Block(List<LabelledPath> slice, List<Block> inherited){
			id = slice.get(0).segId;
			labels = slice.get(0).labels.stream().collect(Collectors.toList());
			paths = slice.stream().map(p->p.pair).collect(Collectors.toList());
			slice.forEach(s->s.block = this);
			
			//labels inherited from previous layer blocks
			for(Block block : inherited){
				labels.addAll(block.labels);
			}
			
			if(computeCores){
				computeCores(slice.get(0).segs, inherited);
			}
		}
		
		public List<Pair> getPaths(){
			return paths;
		}
		
		public List<List<Predicate>> getLabels(){
			return labels;
		}
		
		public boolean isLoop(){
			return paths.get(0).isLoop();
		}
		
		private void computeCores(Set<PathPair> segs, List<Block> inherited){
			if(segs.isEmpty()){
				labels.stream().map(CPQ::labels).forEach(q->{
					String canon = new CanonForm(q).toBase64Canon();
					if(canonCores.add(canon)){
						cores.add(q);
					}
				});
			}else{
				//cores from previous layers
				for(Block block : inherited){//TODO technically we have a number of uniqueness guarantees here (unique within a block)
					for(CPQ q : block.cores){
						String canon = new CanonForm(q).toBase64Canon();
						if(canonCores.add(canon)){
							cores.add(q);
						}
					}
				}
				
				//all combinations of cores from previous layers
				for(PathPair pair : segs){
					for(CPQ core1 : pair.first.block.cores){
						for(CPQ core2 : pair.second.block.cores){
							CPQ q = CPQ.concat(core1, core2);
							String canon = new CanonForm(q).toBase64Canon();
							if(canonCores.add(canon)){
								cores.add(q);
							}
						}
					}
				}
			}
			
			//all intersections of cores
			Util.computeAllSubsets(cores, set->{
				if(set.size() >= 2){
					CPQ q = CPQ.intersect(set);
					String canon = new CanonForm(q).toBase64Canon();
					if(canonCores.add(canon)){
						cores.add(q);
					}
				}
			});
			
			//intersect with identity if possible
			if(isLoop()){
				final int max = cores.size();
				for(int i = 0; i < max; i++){
					cores.add(CPQ.intersect(cores.get(i), CPQ.id()));
				}
			}
		}
		
		@Override
		public String toString(){
			StringBuilder builder = new StringBuilder();
			builder.append("Block[id=");
			builder.append(id);
			builder.append(",paths=");
			builder.append(paths);
			builder.append(",labels={");
			for(List<Predicate> seq : labels){
				for(Predicate p : seq){
					builder.append(p.getAlias());
				}
				builder.append(",");
			}
			builder.delete(builder.length() - 1, builder.length());
			builder.append("},cores={");
			for(CPQ core : cores){
				builder.append(core.toString());
				builder.append(",");
			}
			if(!cores.isEmpty()){
				builder.delete(builder.length() - 1, builder.length());
			}
			builder.append("}]");
			return builder.toString();
		}
	}
	
	/**
	 * Represents a pair of two labelled paths
	 * that were joined to form a new path.
	 * @author Roan
	 */
	private final class PathPair{
		/**
		 * The first path of this pair, also the start of the joined path.
		 */
		private final LabelledPath first;
		/**
		 * The second path of this pair, also the end of the joined path.
		 */
		private final LabelledPath second;
		
		/**
		 * Constructs a new path pair with the given paths.
		 * @param first The first and start path.
		 * @param second The second and end path.
		 */
		private PathPair(LabelledPath first, LabelledPath second){
			this.first = first;
			this.second = second;
		}
		
		/**
		 * Tests of this path pair is equal to another path pair
		 * based on the segment IDs assigned to the stored paths.
		 * @param other The other path pair to test against.
		 * @return True if this path pair has segment IDs equal
		 *         to the given path pair.
		 * @see LabelledPath#segId
		 */
		private boolean equalSegId(PathPair other){
			return first.segId == other.first.segId && second.segId == other.second.segId;
		}
		
		@Override
		public boolean equals(Object obj){
			if(obj instanceof Index<?>.PathPair){
				Index<?>.PathPair other = (Index<?>.PathPair)obj;
				return first.equals(other.first) && second.equals(other.second);
			}else{
				return false;
			}
		}
		
		@Override
		public int hashCode(){
			return Objects.hash(first, second);
		}
	}
	
	/**
	 * Represents a path through the graph identified by
	 * a source target node pair and a number of label
	 * sequences that exist between that node pair.
	 * @author Roan
	 */
	private final class LabelledPath{
		/**
		 * The node pair for this labelled path. All stored label
		 * sequences are between the vertices of this pair.
		 */
		private final Pair pair;
		/**
		 * The label sequences that were found that exist between
		 * the vertices of the node pair for this path.
		 */
		private Set<List<Predicate>> labels = new HashSet<List<Predicate>>();
		
		//id stuff
		private int segId;
		private Block block;
		
		private Set<PathPair> segs = new HashSet<PathPair>();//effectively a history of blocks that were combined to form this path
		
		private LabelledPath(Pair pair){
			this.pair = pair;
		}
		
		public boolean equalSegments(LabelledPath other){
			main: for(Index<V>.PathPair seg : segs){
				for(Index<V>.PathPair test : other.segs){
					if(seg.equalSegId(test)){
						continue main;
					}
				}
				
				return false;
			}
			
			return true;
		}
		
		public void addSegment(LabelledPath first, LabelledPath last){
			segs.add(new PathPair(first, last));
		}
		
		public void addLabel(Predicate label){
			labels.add(Arrays.asList(label));
		}
		
		public void addLabel(List<Predicate> first, List<Predicate> last){
			List<Predicate> path = new ArrayList<Predicate>(first.size() + last.size());
			path.addAll(first);
			path.addAll(last);
			labels.add(path);
		}
		
		public boolean isLoop(){
			return pair.isLoop();
		}
		
		@Override
		public String toString(){
			StringBuilder builder = new StringBuilder();
			builder.append("LabelledPath[id=");
			builder.append(segId);
			builder.append(",path=");
			builder.append(pair);
			builder.append(",labels={");
			for(List<Predicate> seq : labels){
				for(Predicate p : seq){
					builder.append(p.getAlias());
				}
				builder.append(",");
			}
			builder.delete(builder.length() - 1, builder.length());builder.append("},segs={");
			for(PathPair seq : segs){
				builder.append(seq.first.segId);
				builder.append(seq.second.segId);
				builder.append(",");
			}
			builder.delete(builder.length() - 1, builder.length());
			builder.append("}]");
			builder.append("}]");
			return builder.toString();
		}
	}
	
	/**
	 * Represents a pair of two vertices, also referred to as a
	 * path or an st-pair.
	 * @author Roan
	 */
	public final class Pair{
		/**
		 * The source vertex.
		 */
		private final V src;
		/**
		 * The target vertex.
		 */
		private final V trg;
		
		/**
		 * Constructs a new pair with the given source and target vertex.
		 * @param src The source vertex.
		 * @param trg The target vertex.
		 */
		private Pair(V src, V trg){
			this.src = src;
			this.trg = trg;
		}
		
		/**
		 * Get the source vertex of this pair.
		 * @return The source vertex of this pair.
		 */
		public V getSource(){
			return src;
		}
		
		/**
		 * Gets the target vertex of this pair.
		 * @return The target vertex of thsi pair.
		 */
		public V getTarget(){
			return trg;
		}
		
		/**
		 * Checks if this pair represents a loop, that is,
		 * if the source and target vertex are equivalent.
		 * @return True if this pair represents a loop.
		 */
		public boolean isLoop(){
			return src.equals(trg);
		}
		
		@Override
		public boolean equals(Object obj){
			if(obj instanceof Index.Pair){
				Index<?>.Pair other = (Index<?>.Pair)obj;
				return src.equals(other.src) && trg.equals(other.trg);
			}else{
				return false;
			}
		}

		@Override
		public int hashCode(){
			return Objects.hash(src, trg);
		}
		
		@Override
		public String toString(){
			return "(" + src + "," + trg + ")";
		}
	}
}
