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
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;

import dev.roanh.gmark.conjunct.cpq.CPQ;
import dev.roanh.gmark.conjunct.cpq.QueryGraphCPQ;
import dev.roanh.gmark.conjunct.cpq.QueryGraphCPQ.Vertex;
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
 * @see <a href="https://doi.org/10.1109/ICDE53745.2022.00054">Yuya Sasaki, George Fletcher and Makoto Onizuka,
 *      "Language-aware indexing for conjunctive path queries", in IEEE 38th ICDE, 2022</a>
 * @see <a href="https://github.com/yuya-s/CPQ-aware-index">yuya-s/CPQ-aware-index</a>
 */
public class Index{
	private final boolean computeLabels;
	private final boolean computeCores;
	private final int k;
	private List<Block> blocks = new ArrayList<Block>();
	private Map<String, List<Block>> coreToBlock = new HashMap<String, List<Block>>();
	private UniqueGraph<Integer, Predicate> graph;
	
	//TODO double check private/public of everything
	
	public static void main(String[] args) throws IllegalArgumentException, InterruptedException, ExecutionException{
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
		
		Index eq = new Index(g, 2, true, true, 1);
		
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
	
	public Index(UniqueGraph<Integer, Predicate> g, int k, int threads) throws IllegalArgumentException, InterruptedException, ExecutionException{
		this(g, k, true, false, threads);
	}
	
	public Index(UniqueGraph<Integer, Predicate> g, int k, boolean computeCores, boolean computeLabels, int threads) throws IllegalArgumentException, InterruptedException, ExecutionException{
		this.computeCores = computeCores;
		this.computeLabels = computeLabels;
		this.k = k;
		graph = g;
		computeBlocks(partition(g), threads);
		mapCoresToBlocks();
	}
	
	public List<Pair> query(CPQ cpq) throws IllegalArgumentException{
		if(cpq.getDiameter() > k){
			throw new IllegalArgumentException("Query diameter larger than index diameter.");
		}
		
		String key = new CanonForm(cpq).toBase64Canon();//TODO bytes
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
	
	private void computeBlocks(RangeList<List<LabelledPath>> segments, int threads) throws InterruptedException, ExecutionException{
		Map<Pair, LabelledPath> unused = new HashMap<Pair, LabelledPath>();
		ExecutorService executor = Executors.newFixedThreadPool(threads);
		
		for(int j = 0; j < k; j++){
			List<Callable<Block>> tasks = new ArrayList<Callable<Block>>();
			
			List<LabelledPath> segs = segments.get(j);
			int start = 0;
			int lastId = segs.get(0).segId;
			for(int i = 0; i <= segs.size(); i++){
				if(i == segs.size() || segs.get(i).segId != lastId){
					List<LabelledPath> slice = segs.subList(start, i);
					
					tasks.add(()->{
						Block block = new Block(slice, computeLabels, computeCores);
						System.out.println("id=" + block.id + " s=" + slice.size() + " c=" + block.cores.size() + " r=" + block.reject + String.format(" (%1$.3f)", block.cores.size() / (double)(block.cores.size() + block.reject)));
						return block;
					});
					
					if(j != k - 1){
						for(LabelledPath path : slice){
							unused.put(path.pair, path);
						}
					}else{
						for(LabelledPath path : slice){
							unused.remove(path.pair);
						}
					}
					
					if(i != segs.size()){
						lastId = segs.get(i).segId;
						start = i;
					}
				}
			}

			for(Future<Block> future : executor.invokeAll(tasks)){
				blocks.add(future.get());
			}
		}
		
		//any remaining pairs denote blocks from previous layers
		List<LabelledPath> remaining = unused.values().stream().sorted(Comparator.comparing(l->l.segId)).collect(Collectors.toList());
		if(!remaining.isEmpty()){
			List<Callable<Block>> tasks = new ArrayList<Callable<Block>>();
			
			int start = 0;
			int lastId = remaining.get(0).segId;
			for(int i = 0; i <= remaining.size(); i++){
				if(i == remaining.size() || remaining.get(i).segId != lastId){
					List<LabelledPath> slice = remaining.subList(start, i);
					
					tasks.add(()->{
						Block block = new Block(slice, computeLabels, computeCores);
						System.out.println("id=" + block.id + " s=" + slice.size() + " c=" + block.cores.size() + " r=" + block.reject + String.format(" (%1$.3f)", block.cores.size() / (double)(block.cores.size() + block.reject)));
						return block;
					});
					
					if(i != remaining.size()){
						lastId = remaining.get(i).segId;
						start = i;
					}
				}
			}
			
			for(Future<Block> future : executor.invokeAll(tasks)){
				blocks.add(future.get());
			}
		}
		
		executor.shutdown();
	}
	
	//partition according to k-path-bisimulation
	private RangeList<List<LabelledPath>> partition(UniqueGraph<Integer, Predicate> g) throws IllegalArgumentException{
		if(k <= 0){
			throw new IllegalArgumentException("Invalid value of k for bisimulation, has to be 1 or greater.");
		}
		
		RangeList<List<LabelledPath>> segments = new RangeList<List<LabelledPath>>(k, ArrayList::new);
		Map<Pair, LabelledPath> history = new HashMap<Pair, LabelledPath>();
		
		//classes for 1-path-bisimulation
		Map<Pair, LabelledPath> pathMap = new HashMap<Pair, LabelledPath>();
		for(GraphEdge<Integer, Predicate> edge : g.getEdges()){
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
						if(seg.pair.trg != end.pair.src){
							continue;
						}
						
						Pair key = new Pair(seg.pair.src, end.pair.trg);
						LabelledPath path = pathMap.computeIfAbsent(key, p->{
							LabelledPath newPath = new LabelledPath(p, history.get(p));
							history.put(p, newPath);
							return newPath;
						});
						
						path.addSegment(seg, end);
						if(k2 == 0 && computeLabels){//slight optimisation, since we only need one combination to find all paths
							for(LabelSequence labels : seg.labels){
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
			pathMap.values().forEach(s->s.segHash = s.segs.hashCode());
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
	
	/**
	 * Compares the given paths based on their segments,
	 * cyclic properties, source and target.
	 * @param a The first path.
	 * @param b The second path.
	 * @return A value less than 0 if {@code a < b}, a value equal
	 *         to 0 if {@code a == b} and a value grater than 0 if
	 *         {@code a > b}.
	 */
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
	
	public static final class Block{
		private final int id;
		private List<Pair> paths;
		private List<LabelSequence> labels;//TODO technically no need to store this for a core based index, probably turn it off for real use -- except dia 1
		private List<CPQ> cores = new ArrayList<CPQ>();//TODO technically only need #canonCores, but this is good for debugging
		private Set<String> canonCores = new HashSet<String>();//TODO should use bytes rather than strings, but strings are easier to debug -- string cache hash code so the only reason to switch is if base64 is expensive
		
		private Block(List<LabelledPath> slice, boolean computeLabels, boolean computeCores){
			id = slice.get(0).segId;
			labels = slice.get(0).labels.stream().collect(Collectors.toList());
			paths = slice.stream().map(p->p.pair).collect(Collectors.toList());
			slice.forEach(s->s.block = this);
			
			//inherited from previous layer blocks
			LabelledPath parent = slice.get(0);
			while(parent.ancestor != null){
				parent = parent.ancestor;
				if(computeLabels){
					labels.addAll(parent.labels);
				}
				
				//these are by definition of a different diameter
				if(computeCores){//TODO probably want to differentiate between prepping and computing maybe?
					cores.addAll(parent.block.cores);
					canonCores.addAll(parent.block.canonCores);
				}
			}
			
			if(computeCores){
				computeCores(slice.get(0).segs);//TODO could be multithreaded by layer
			}
		}
		
		public int getId(){
			return id;
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
		
		//TODO static inner classes
		
		private int reject;
		private void addCore(CPQ q){
			if(canonCores.add(new CanonForm(q).toBase64Canon())){//TODO use byte[]
				cores.add(q);
//				if(id == 1813){
//					System.out.println("qa: " + q);
//				}
			}else{
				reject++;
//				if(id == 1813){
////					System.out.println("qr: " + q);
//				}
			}
		}
		
		private void computeCores(Set<PathPair> segs){
//			if(id == 1813){
//				System.out.println("--- joins");
//			}
			
			if(segs.isEmpty()){
				//TODO technically these were already computed -- here or elsewhere? -- may not be a performance issue though
				labels.stream().map(LabelSequence::getLabels).map(CPQ::labels).forEach(this::addCore);
			}else{
				//all combinations of cores from previous layers
				for(PathPair pair : segs){
					for(CPQ core1 : pair.first.block.cores){
						for(CPQ core2 : pair.second.block.cores){
							addCore(CPQ.concat(core1, core2));
						}
					}
				}
			}
			
//			if(id == 1813){
//				System.out.println("--- subs");
//			}
			
			//all intersections of cores
			QueryGraphCPQ[] graphs = new QueryGraphCPQ[cores.size()];
			for(int i = 0; i < graphs.length; i++){
				graphs[i] = cores.get(i).toQueryGraph();
			}
			
			boolean[][] conflicts = new boolean[cores.size()][];
			conflicts[0] = new boolean[0];
			for(int i = 1; i < conflicts.length; i++){
				QueryGraphCPQ a = graphs[i];
				conflicts[i] = new boolean[i];
				for(int j = 0; j < i; j++){
					QueryGraphCPQ b = graphs[j];
					conflicts[i][j] = a.isHomomorphicTo(b) || b.isHomomorphicTo(a);
				}
			}
			
			computeIntersectionCores(cores, 0, cores.size(), new ArrayList<CPQ>(), new boolean[cores.size()], conflicts);
			
			
//			Util.computeAllSubsets(cores, set->{//TODO could limit max set size
//				if(set.size() >= 2){
//					addCore(CPQ.intersect(set));
//				}
//			});
			
//			if(id == 1813){
//				System.out.println("--- identity");
//			}
			
			//intersect with identity if possible
			if(isLoop()){
				final int max = cores.size();
				for(int i = 0; i < max; i++){
					addCore(CPQ.intersect(cores.get(i), CPQ.id()));
				}
			}
		}
		
		private void computeIntersectionCores(List<CPQ> items, int offset, final int max, List<CPQ> set, boolean[] selected, boolean[][] conflicts){
			if(offset >= max){
				if(set.size() >= 2){//TODO could limit max set size
					addCore(CPQ.intersect(set));
				}
			}else{
				//don't pick the element
				computeIntersectionCores(items, offset + 1, max, set, selected, conflicts);
				
				//pick the element
				for(int i = 0; i < conflicts[offset].length; i++){
					if(selected[i] && conflicts[offset][i]){
						//can't pick a conflicting item
						return;
					}
				}
				
				selected[offset] = true;
				set.add(items.get(offset));
				computeIntersectionCores(items, offset + 1, max, set, selected, conflicts);
				set.remove(set.size() - 1);
				selected[offset] = false;
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
	private static final class PathPair implements Comparable<PathPair>{
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
			Index.PathPair other = (Index.PathPair)obj;
			return first.equals(other.first) && second.equals(other.second);
		}
		
		@Override
		public int hashCode(){
			return 31 * first.hashCode() + second.hashCode();
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
	private static final class LabelledPath{
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
		private int segHash;//TODO set from a different place kinda eh
		private LabelledPath ancestor;
		
		private LabelledPath(Pair pair, LabelledPath ancestor){
			this.pair = pair;
			this.ancestor = ancestor;
		}
		
		public int compareLabelsTo(LabelledPath other){
			return compare(labels, other.labels);
		}

		public int compareSegmentsTo(LabelledPath other){
			int cmp = Integer.compare(segHash, other.segHash);
			if(cmp != 0){
				return cmp;
			}
			
			cmp = Boolean.compare(ancestor == null, other.ancestor == null);
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
		
		@Override
		public int hashCode(){
			return segId;
		}
	}
	
	public static final class LabelSequence implements Comparable<LabelSequence>{
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
			return Arrays.equals(data, ((Index.LabelSequence)obj).data);
		}
	}
	
	/**
	 * Represents a pair of two vertices, also referred to as a
	 * path or an st-pair.
	 * @author Roan
	 */
	public static final class Pair implements Comparable<Pair>{
		/**
		 * The source vertex.
		 */
		private final int src;
		/**
		 * The target vertex.
		 */
		private final int trg;
		
		/**
		 * Constructs a new pair with the given source and target vertex.
		 * @param src The source vertex.
		 * @param trg The target vertex.
		 */
		private Pair(int src, int trg){
			this.src = src;
			this.trg = trg;
		}
		
		/**
		 * Get the source vertex of this pair.
		 * @return The source vertex of this pair.
		 */
		public int getSource(){
			return src;
		}
		
		/**
		 * Gets the target vertex of this pair.
		 * @return The target vertex of thsi pair.
		 */
		public int getTarget(){
			return trg;
		}
		
		/**
		 * Checks if this pair represents a loop, that is,
		 * if the source and target vertex are equivalent.
		 * @return True if this pair represents a loop.
		 */
		public boolean isLoop(){
			return src == trg;
		}
		
		@Override
		public boolean equals(Object obj){
			Index.Pair other = (Index.Pair)obj;
			return src == other.src && trg == other.trg;
		}

		@Override
		public int hashCode(){
			return 31 * src + trg;
		}
		
		@Override
		public String toString(){
			return "(" + src + "," + trg + ")";
		}

		@Override
		public int compareTo(Pair o){
			int cmp = Integer.compare(src, o.src);
			return cmp == 0 ? Integer.compare(trg, o.trg) : cmp;
		}
	}
}
