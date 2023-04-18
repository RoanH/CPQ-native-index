package dev.roanh.cpqindex;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

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
	
	//TODO double check private/public of everything
	
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
		
		Index<Integer> eq = new Index<Integer>(g, 2, true);
		
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
		
		eq.print();
		
		CPQ q = CPQ.labels(l3, l3.getInverse());
		System.out.println("run: " + q);
		System.out.println(eq.query(q));
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
			throw new IllegalArgumentException("Query diameter large than index diameter.");
		}
		
		String key = new CanonForm(cpq).toBase64Canon();
		System.out.println("key: " + key);
		System.out.println("ret: " + coreToBlock.get(key));
		
		return coreToBlock.getOrDefault(key, Collections.emptyList()).stream().flatMap(b->b.getPaths().stream()).collect(Collectors.toList());
	}
	
	/**
	 * Gets all the blocks in this index.
	 * @return All the blocks in this index.
	 */
	public List<Block> getBlocks(){
		return blocks;
	}
	
	/**
	 * Sorts all the list of blocks of this index. There's is no real
	 * reason to do this other than to make the output of {@link #print()}
	 * more organised.
	 * @see #print()
	 */
	public void sort(){
		for(Block block : blocks){
			block.paths.sort(null);
			block.labels.sort(null);
		}
		
		blocks.sort(Comparator.comparing(b->b.paths.get(0)));
	}
	
	/**
	 * Prints the index to standard output.
	 * @see #sort()
	 */
	public void print(){
		int maxPath = blocks.stream().mapToInt(b->b.paths.size()).max().orElse(0);
		int maxLab = blocks.stream().mapToInt(b->b.labels.size()).max().orElse(0);
		StringBuilder[] out = new StringBuilder[4 + maxPath + maxLab + blocks.stream().mapToInt(b->b.cores.size()).max().orElse(0)];
		int labStart = 3 + blocks.stream().mapToInt(b->b.paths.size()).max().orElse(0);
		int coreStart = labStart + 1 + maxLab;
		for(int i = 0; i < out.length; i++){
			out[i] = new StringBuilder();
		}
		
		int currentWidth = 0;
		for(Block block : blocks){
			int blockWidth = 5;
			if(currentWidth > 0){
				for(int i = 0; i < out.length; i++){
					out[i].append('|');
				}
			}
			
			out[0].append(block.id);
			for(int i = 0; i < block.paths.size(); i++){
				String str = block.paths.get(i).toString();
				out[i + 2].append(str);
				blockWidth = Math.max(blockWidth, str.length());
			}
			
			out[labStart - 1].append("-----");
			for(int i = 0; i < block.labels.size(); i++){
				int w = 0;
				for(Predicate p : block.labels.get(i).getLabels()){
					String str = p.getAlias();
					out[labStart + i].append(p.getAlias());
					w += str.length();
				}
				blockWidth = Math.max(blockWidth, w);
			}
			
			out[coreStart - 1].append("-----");
			for(int i = 0; i < block.cores.size(); i++){
				String str = block.cores.get(i).toString();
				out[coreStart + i].append(str);
				blockWidth = Math.max(blockWidth, str.length());
			}
			
			currentWidth += blockWidth + 2;
			for(int i = 0; i < out.length; i++){
				while(out[i].length() < currentWidth){
					out[i].append((out[i].length() == 0 || out[i].charAt(out[i].length() - 1) != '-') ? ' ' : '-');
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
		Map<Pair, LabelledPath> unused = new HashMap<Pair, LabelledPath>();
		
		for(int j = 0; j < k; j++){
			List<LabelledPath> segs = segments.get(j);
			int start = 0;
			int lastId = segs.get(0).segId;
			for(int i = 0; i <= segs.size(); i++){
				if(i == segs.size() || segs.get(i).segId != lastId){
					List<LabelledPath> slice = segs.subList(start, i);
					
					List<Block> inherited = new ArrayList<Block>();
//					if(j > 0){//TODO
//						for(LabelledPath path : slice){
//							Block b = pairMap.remove(path.pair);
//							if(b != null){
//								inherited.add(b);
//							}
//						}
//					}
					
					Block block = new Block(slice, inherited);
					if(j != k - 1){
						for(LabelledPath path : slice){
							unused.put(path.pair, path);
						}
					}else{
						for(Pair pair : block.paths){
							unused.remove(pair);
						}
						blocks.add(block);
					}
					
					if(i != segs.size()){
						lastId = segs.get(i).segId;
						start = i;
					}
				}
			}
		}
		
		//any remaining pairs denote blocks from previous layers
		List<LabelledPath> remaining = unused.values().stream().sorted(Comparator.comparing(l->l.segId)).collect(Collectors.toList());
		if(!remaining.isEmpty()){
			int start = 0;
			int lastId = remaining.get(0).segId;
			for(int i = 0; i <= remaining.size(); i++){
				if(i == remaining.size() || remaining.get(i).segId != lastId){
					blocks.add(new Block(remaining.subList(start, i), Collections.EMPTY_LIST));
					if(i != remaining.size()){
						lastId = remaining.get(i).segId;
						start = i;
					}
				}
			}
		}
	}
	
	//partition according to k-path-bisimulation
	private RangeList<List<LabelledPath>> partition(UniqueGraph<V, Predicate> g) throws IllegalArgumentException{
		if(k <= 0){
			throw new IllegalArgumentException("Invalid value of k for bisimulation, has to be 1 or greater.");
		}
		
		RangeList<List<LabelledPath>> segments = new RangeList<List<LabelledPath>>(k, ArrayList::new);
		Map<Pair, LabelledPath> history = new HashMap<Pair, LabelledPath>();
		
		//classes for 1-path-bisimulation
		Map<Pair, LabelledPath> pathMap = new HashMap<Pair, LabelledPath>();
		for(GraphEdge<V, Predicate> edge : g.getEdges()){
			//forward and backward edges are just the labels on those edges
			LabelledPath path = pathMap.computeIfAbsent(new Pair(edge.getSource(), edge.getTarget()), p->new LabelledPath(p, null));
			path.addLabel(edge.getData());
			history.put(path.pair, path);
			
			path = pathMap.computeIfAbsent(new Pair(edge.getTarget(), edge.getSource()), p->new LabelledPath(p, null));
			path.addLabel(edge.getData().getInverse());
			history.put(path.pair, path);
		}
		
		//sort 1-path
		System.out.println("Start sort: 1");
		List<LabelledPath> segOne = segments.get(0);
		pathMap.values().stream().sorted(this::sortOnePath).forEachOrdered(segOne::add);
		System.out.println("end sort");
		
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
						LabelledPath path = pathMap.computeIfAbsent(key, p->{
							LabelledPath newPath = new LabelledPath(p, history.get(p));
							history.put(p, newPath);
							return newPath;
						});
						
						path.addSegment(seg, end);
						if(k2 == 0){//slight optimisation, since we only need one combination to find all paths
							for(LabelSequence labels : seg.labels){//TODO, do we even need to do this?
								for(LabelSequence label : end.labels){
									path.addLabel(labels, label);
								}
							}
						}
					}
				}
			}
			
			//sort
			System.out.println("Start sort: " + (i + 1));
			List<LabelledPath> segs = segments.get(i);
			pathMap.values().stream().sorted(this::sortPaths).forEachOrdered(segs::add);
			System.out.println("end sort");

			//assign IDs
			prev = null;
			for(LabelledPath path : segs){
				if(prev != null && (path.compareSegmentsTo(prev) != 0 || prev.isLoop() ^ path.isLoop())){
					//increase id if loop status or segments differs
					id++;
				}

				path.segId = id;
				prev = path;
			}
		}
		
		return segments;
	}
	
	private int sortPaths(LabelledPath a, LabelledPath b){
		int cmp = a.compareSegmentsTo(b);
		if(cmp != 0){
			return cmp;
		}

		cmp = Boolean.compare(a.isLoop(), b.isLoop());
		if(cmp != 0){
			return cmp;
		}

		return a.pair.compareTo(b.pair);
	}
	
	private int sortOnePath(LabelledPath a, LabelledPath b){
		int cmp = a.compareLabelsTo(b);
		if(cmp != 0){
			return cmp;
		}
		
		cmp = Boolean.compare(a.isLoop(), b.isLoop());
		if(cmp != 0){
			return cmp;
		}

		return a.pair.compareTo(b.pair);
	}
	
	public final class Block{
		private final int id;
		private List<Pair> paths;
		private List<LabelSequence> labels;//TODO technically no need to store this for a core based index, probably turn it off for real use
		private List<CPQ> cores = new ArrayList<CPQ>();//TODO technically only need #canonCores, but this is good for debugging
		private Set<String> canonCores = new HashSet<String>();//TODO should use bytes rather than strings, but strings are easier to debug
		
		private Block(List<LabelledPath> slice, List<Block> inherited){
			id = slice.get(0).segId;
			labels = slice.get(0).labels.stream().collect(Collectors.toList());
			paths = slice.stream().map(p->p.pair).collect(Collectors.toList());
			slice.forEach(s->s.block = this);
			
			//labels inherited from previous layer blocks
			LabelledPath parent = slice.get(0);
			while(parent.ancestor != null){
				parent = parent.ancestor;
				labels.addAll(parent.labels);//TODO no need for labels when only computing cores (unless layer 1 but layer 1 has no ancestors anyway
			}
			
			if(computeCores){
				computeCores(slice.get(0).segs, inherited);
			}
		}
		
		public List<Pair> getPaths(){
			return paths;
		}
		
		public List<LabelSequence> getLabels(){
			return labels;
		}
		
		public boolean isLoop(){
			return paths.get(0).isLoop();
		}
		
		private void computeCores(Set<PathPair> segs, List<Block> inherited){
			if(segs.isEmpty()){
				labels.stream().map(LabelSequence::getLabels).map(CPQ::labels).forEach(q->{
					String canon = new CanonForm(q).toBase64Canon();
					if(canonCores.add(canon)){
						cores.add(q);
					}
				});
			}else{
				//cores from previous layers
				for(Block block : inherited){//TODO technically we have a number of uniqueness guarantees here (unique within a block)
					for(CPQ q : block.cores){
						String canon = new CanonForm(q).toBase64Canon();//TODO technically already computed and stored in the block
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
			for(LabelSequence seq : labels){
				for(Predicate p : seq.getLabels()){
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
	private final class PathPair implements Comparable<PathPair>{
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

		@Override
		public int compareTo(PathPair o){
			int cmp = Integer.compare(first.segId, o.first.segId);
			return cmp == 0 ? Integer.compare(second.segId, o.second.segId) : cmp;
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
		private SortedSet<LabelSequence> labels = new TreeSet<LabelSequence>();
		
		//id stuff
		private int segId;
		private Block block;
		
		private SortedSet<PathPair> segs = new TreeSet<PathPair>();//effectively a history of blocks that were combined to form this path
		private LabelledPath ancestor;
		
		private LabelledPath(Pair pair, LabelledPath ancestor){
			this.pair = pair;
			this.ancestor = ancestor;
		}
		
		public int compareLabelsTo(LabelledPath other){
			return compare(labels, other.labels);
		}

		public int compareSegmentsTo(LabelledPath other){
			int cmp = Boolean.compare(ancestor == null, other.ancestor == null);
			if(cmp != 0){
				return cmp;
			}
			
			if(ancestor != null){
				cmp = Integer.compare(ancestor.segId, other.ancestor.segId);
				if(cmp != 0){
					return cmp;
				}
			}
			
			return compare(segs, other.segs);
		}
		
		public <T extends Comparable<T>> int compare(SortedSet<T> a, SortedSet<T> b){
			int cmp = Integer.compare(a.size(), b.size());
			if(cmp == 0){
				Iterator<T> iter = b.iterator();
				for(T seg : a){
					cmp = seg.compareTo(iter.next());
					if(cmp != 0){
						return cmp;
					}
				}
				
				return 0;
			}else{
				return cmp;
			}
		}
		
		public void addSegment(LabelledPath first, LabelledPath last){
			segs.add(new PathPair(first, last));
		}
		
		public void addLabel(Predicate label){
			labels.add(new LabelSequence(label));
		}
		
		public void addLabel(LabelSequence first, LabelSequence last){
			labels.add(new LabelSequence(first, last));
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
			for(LabelSequence seq : labels){
				for(Predicate p : seq.getLabels()){
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
	
	public final class LabelSequence implements Comparable<LabelSequence>{
		private Predicate[] data;
		
		public LabelSequence(LabelSequence first, LabelSequence last){
			data = new Predicate[first.data.length + last.data.length];
			System.arraycopy(first.data, 0, data, 0, first.data.length);
			System.arraycopy(last.data, 0, data, first.data.length, last.data.length);
		}

		public LabelSequence(Predicate label){
			data = new Predicate[]{label};
		}
		
		public Predicate[] getLabels(){
			return data;
		}

		@Override
		public int compareTo(LabelSequence o){
			int cmp = Integer.compare(data.length, o.data.length);
			if(cmp == 0){
				for(int i = 0; i < data.length; i++){
					cmp = data[i].compareTo(o.data[i]);
					if(cmp != 0){
						return cmp;
					}
				}
				
				return 0;
			}else{
				return cmp;
			}
		}
		
		@Override
		public int hashCode(){
			return Arrays.hashCode(data);
		}
		
		@Override
		public boolean equals(Object obj){
			return obj instanceof Index<?>.LabelSequence ? Arrays.equals(data, ((Index<?>.LabelSequence)obj).data) : false;
		}
	}
	
	/**
	 * Represents a pair of two vertices, also referred to as a
	 * path or an st-pair.
	 * @author Roan
	 */
	public final class Pair implements Comparable<Pair>{
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

		@Override
		public int compareTo(Pair o){
			int cmp = src.compareTo(o.src);
			return cmp == 0 ? trg.compareTo(o.trg) : cmp;
		}
	}
}
