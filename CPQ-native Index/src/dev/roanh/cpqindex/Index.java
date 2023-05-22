package dev.roanh.cpqindex;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import dev.roanh.cpqindex.CanonForm.CanonFuture;
import dev.roanh.cpqindex.CanonForm.CoreHash;
import dev.roanh.gmark.conjunct.cpq.CPQ;
import dev.roanh.gmark.conjunct.cpq.QueryGraphCPQ;
import dev.roanh.gmark.core.graph.Predicate;
import dev.roanh.gmark.util.RangeList;
import dev.roanh.gmark.util.UniqueGraph;
import dev.roanh.gmark.util.UniqueGraph.GraphEdge;

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
	private final int maxIntersections;
	private final boolean computeLabels;
	private final boolean computeCores;
	private final int k;
	private final boolean verbose;//storage of labels & cpqs
	
	private List<Block> blocks = new ArrayList<Block>();
	private Map<CoreHash, List<Block>> coreToBlock = new HashMap<CoreHash, List<Block>>();
	private RangeList<Predicate> predicates;
	@Deprecated
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
		
		UniqueGraph<Integer, Predicate> g = new UniqueGraph<Integer, Predicate>();
		g.addUniqueNode(0);
		g.addUniqueNode(1);
		g.addUniqueNode(2);

		g.addUniqueEdge(0, 1, l0);
		g.addUniqueEdge(0, 2, l0);
		g.addUniqueEdge(1, 2, l1);
//		g.addUniqueEdge(2, 1, l1);
		Index eq = new Index(g, 1, true, true, 1, 2, true);
		eq.sort();
		eq.print();
		
		eq = new Index(g, 2, true, true, 1, 2, true);
		eq.sort();
		eq.print();
		
	}
	
	public Index(UniqueGraph<Integer, Predicate> g, int k, int threads) throws IllegalArgumentException, InterruptedException, ExecutionException{
		this(g, k, threads, Integer.MAX_VALUE);
	}
	
	public Index(UniqueGraph<Integer, Predicate> g, int k, int threads, int maxIntersections) throws IllegalArgumentException, InterruptedException, ExecutionException{
		this(g, k, true, false, threads, maxIntersections, false);
	}
	
	public Index(UniqueGraph<Integer, Predicate> g, int k, boolean computeCores, boolean computeLabels, int threads) throws IllegalArgumentException, InterruptedException, ExecutionException{
		this(g, k, computeCores, computeLabels, threads, Integer.MAX_VALUE, false);
	}
	
	public Index(UniqueGraph<Integer, Predicate> g, int k, boolean computeCores, boolean computeLabels, int threads, int maxIntersections, boolean verbose) throws IllegalArgumentException, InterruptedException, ExecutionException{
		this.computeCores = computeCores;
		this.computeLabels = computeLabels;
		this.maxIntersections = maxIntersections;
		this.k = k;
		this.verbose = verbose;
		graph = g;
		computeBlocks(partition(g), threads);
		mapCoresToBlocks();
	}
	
	public Index(InputStream source) throws IOException{
		DataInputStream in = new DataInputStream(source);
		computeCores = in.readBoolean();
		computeLabels = in.readBoolean();
		maxIntersections = in.readInt();
		k = in.readInt();
		verbose = in.readBoolean();
		
		predicates = new RangeList<Predicate>(in.readInt());
		for(int i = 0; i < predicates.size(); i++){
			byte[] data = new byte[in.readInt()];
			in.readFully(data);
			predicates.set(i, new Predicate(i, new String(data, StandardCharsets.UTF_8)));
		}
		
		int len = in.readInt();
		for(int i = 0; i < len; i++){
			blocks.add(new Block(in));
		}
	}
	
	public void write(OutputStream target) throws IOException{
		DataOutputStream out = new DataOutputStream(target);
		out.writeBoolean(computeCores);
		out.writeBoolean(computeLabels);
		out.writeInt(maxIntersections);
		out.writeInt(k);
		out.writeBoolean(verbose);
		
		out.writeInt(predicates.size());
		for(Predicate p : predicates){
			out.writeInt(p.getID());
			byte[] str = p.getAlias().getBytes(StandardCharsets.UTF_8);
			out.writeInt(str.length);
			out.write(str);
		}
		
		out.writeInt(blocks.size());
		for(Block block : blocks){
			block.write(out);
		}
	}
	
	public List<Pair> query(CPQ cpq) throws IllegalArgumentException, InterruptedException, ExecutionException{//TODO how to handle exceptions
		if(cpq.getDiameter() > k){
			throw new IllegalArgumentException("Query diameter larger than index diameter.");
		}
		
		CoreHash key = CanonForm.computeCanon(cpq, false).get().toHashCanon();
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
				String str = block.labels.get(i).getLabels().toString();
				out[labStart + i].append(str);
				blockWidth = Math.max(blockWidth, str.length());
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
			for(CoreHash core : block.canonCores){
				coreToBlock.computeIfAbsent(core, k->new ArrayList<Block>()).add(block);
			}
		}
	}
	
	private void computeBlocks(RangeList<List<LabelledPath>> segments, int threads) throws InterruptedException, ExecutionException{
		Map<Pair, LabelledPath> unused = new HashMap<Pair, LabelledPath>();
		ExecutorService executor = Executors.newFixedThreadPool(threads);
		
		for(int j = 0; j < k; j++){
			List<Callable<Block>> tasks = new ArrayList<Callable<Block>>();
			final int lk = j + 1;
			
			List<LabelledPath> segs = segments.get(j);
			int start = 0;
			int lastId = segs.get(0).getSegmentId();
			for(int i = 0; i <= segs.size(); i++){
				if(i == segs.size() || segs.get(i).getSegmentId() != lastId){
					List<LabelledPath> slice = segs.subList(start, i);
					
					tasks.add(()->{
						Block block = new Block(lk, slice);
						System.out.println("id=" + block.id + " s=" + slice.size() + " c=" + block.cores.size() + " r=" + block.reject + String.format(" (%1$.3f)", block.cores.size() / (double)(block.cores.size() + block.reject)));
						return block;
					});
					
					if(lk != k){
						for(LabelledPath path : slice){
							unused.put(path.getPair(), path);
						}
					}else{
						for(LabelledPath path : slice){
							unused.remove(path.getPair());
						}
					}
					
					if(i != segs.size()){
						lastId = segs.get(i).getSegmentId();
						start = i;
					}
				}
			}
			
			if(lk != k){
				for(Future<Block> future : executor.invokeAll(tasks)){
					//only CPQk blocks get added directly
					future.get();
				}
			}else{
				for(Future<Block> future : executor.invokeAll(tasks)){
					blocks.add(future.get());
				}
			}
		}
		
		//any remaining pairs denote blocks from previous layers
		List<LabelledPath> remaining = unused.values().stream().sorted(Comparator.comparing(LabelledPath::getSegmentId)).collect(Collectors.toList());
		if(!remaining.isEmpty()){
			List<Callable<Block>> tasks = new ArrayList<Callable<Block>>();
			
			int start = 0;
			int lastId = remaining.get(0).getSegmentId();
			for(int i = 0; i <= remaining.size(); i++){
				if(i == remaining.size() || remaining.get(i).getSegmentId() != lastId){
					List<LabelledPath> slice = remaining.subList(start, i);
					
					tasks.add(()->{
						Block block = new Block(k, slice);
						System.out.println("id=" + block.id + " s=" + slice.size() + " c=" + block.cores.size() + " r=" + block.reject + String.format(" (%1$.3f)", block.cores.size() / (double)(block.cores.size() + block.reject)));
						return block;
					});
					
					if(i != remaining.size()){
						lastId = remaining.get(i).getSegmentId();
						start = i;
					}
				}
			}
			
			for(Future<Block> future : executor.invokeAll(tasks)){
				blocks.add(future.get());
			}
		}
		
		executor.shutdown();
		
		System.out.println("Total cores: " + blocks.parallelStream().mapToLong(b->b.cores.size()).sum());
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
		predicates = new RangeList<Predicate>(1 + g.getEdges().stream().mapToInt(e->e.getData().getID()).max().orElse(0));
		for(GraphEdge<Integer, Predicate> edge : g.getEdges()){
			//forward and backward edges are just the labels on those edges
			LabelledPath path = pathMap.computeIfAbsent(new Pair(edge.getSource(), edge.getTarget()), p->new LabelledPath(p, null));
			path.addLabel(edge.getData());
			history.put(path.getPair(), path);
			
			path = pathMap.computeIfAbsent(new Pair(edge.getTarget(), edge.getSource()), p->new LabelledPath(p, null));
			path.addLabel(edge.getData().getInverse());
			history.put(path.getPair(), path);
			
			predicates.set(edge.getData(), edge.getData());
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
			if(prev != null && (!seg.equalLabels(prev) || seg.isLoop() ^ prev.isLoop())){
				//if labels and cyclic patterns (loop) are not the same a new ID is started
				id++;
			}

			seg.setSegmentId(id);
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
						if(seg.getTarget() != end.getSource()){
							continue;
						}
						
						Pair key = new Pair(seg.getSource(), end.getTarget());
						LabelledPath path = pathMap.computeIfAbsent(key, p->{
							LabelledPath newPath = new LabelledPath(p, history.get(p));
							history.put(p, newPath);
							return newPath;
						});
						
						path.addSegment(seg, end);
						if(k2 == 0 && computeLabels){//slight optimisation, since we only need one combination to find all paths
							for(LabelSequence labels : seg.getLabels()){
								for(LabelSequence label : end.getLabels()){
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
			pathMap.values().forEach(LabelledPath::cacheHashCode);
			pathMap.values().stream().sorted(this::sortPaths).forEachOrdered(segs::add);
			System.out.println("end sort");

			//assign IDs
			prev = null;
			for(LabelledPath path : segs){
				if(prev != null && (path.compareSegmentsTo(prev) != 0 || prev.isLoop() ^ path.isLoop())){
					//increase id if loop status or segments differs
					id++;
				}

				path.setSegmentId(id);
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

		return a.comparePathTo(b);
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

		return a.comparePathTo(b);
	}
	
	public final class Block{
		private final int id;
		private final int k;
		private List<Pair> paths;
		private List<LabelSequence> labels = new ArrayList<LabelSequence>();//TODO technically no need to store this for a core based index, probably turn it off for real use -- except dia 1
		private List<CPQ> cores = new ArrayList<CPQ>();//TODO technically not needed for the top level -- not restored after write read
		private Set<CoreHash> canonCores = new HashSet<CoreHash>();
		
		/**
		 * The block from the previous layer that the paths in this block were stored at.
		 * @see #paths
		 */
		private Block ancestor;
		private List<BlockPair> combinations;
		
		private Block(int k, List<LabelledPath> slice){
			this.k = k;
			
			LabelledPath range = slice.get(0);
			id = range.getSegmentId();
			paths = slice.stream().map(LabelledPath::getPair).collect(Collectors.toList());
			slice.forEach(s->s.setBlock(this));
			
			if(computeLabels || k == 1){
				//we need labels to compute cores for k = 1
				labels.addAll(range.getLabels());
			}
			
			if(range.hasAncestor()){
				ancestor = range.getAncestor().getBlock();
				if(computeLabels){
					labels.addAll(ancestor.labels);
				}
			}
			
			combinations = range.getSegments().stream().map(BlockPair::new).toList();
			
			if(computeCores){
				computeCores();//TODO remove
			}
		}
		
		private Block(DataInputStream in) throws IOException{
			id = in.readInt();
			k = in.readInt();
			
			int len = in.readInt();
			paths = new ArrayList<Pair>(len);
			for(int i = 0; i < len; i++){
				paths.add(new Pair(in));
			}
			
			len = in.readInt();
			labels = new ArrayList<LabelSequence>(len);
			for(int i = 0; i < len; i++){
				labels.add(new LabelSequence(in, predicates));
			}
			
			len = in.readInt();
			for(int i = 0; i < len; i++){
				canonCores.add(CoreHash.read(in));
			}
		}
		
		private void write(DataOutputStream out) throws IOException{
			out.writeInt(id);
			out.writeInt(k);
			
			out.writeInt(paths.size());
			for(Pair pair : paths){
				pair.write(out);
			}
			
			out.writeInt(labels.size());
			for(LabelSequence seq : labels){
				seq.write(out);
			}
			
			out.writeInt(canonCores.size());
			for(CoreHash core : canonCores){
				core.write(out);
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
		
		private int reject;//TODO remove?
		private void addCore(CanonForm canon){
			if(canonCores.add(canon.toHashCanon())){
				cores.add(canon.getCPQ());//TODO don't store this on the last layer?
			}else{
				reject++;
				rejected.add(canon.getCPQ());
			}
		}
		private List<CPQ> rejected = new ArrayList<CPQ>();
		
		private void addCores(List<CanonFuture> candidates){
			try{
				for(CanonFuture future : candidates){
					addCore(future.get());
				}
			}catch(InterruptedException | ExecutionException e){
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		private void computeCores(){
			//inherited from previous layer blocks
			if(ancestor != null){//only need to go back one level since the previous level already collected the level before that
				
				
				//these are by definition of a different diameter
				//if(computeCores){//TODO probably want to differentiate between prepping and computing maybe?
					
					canonCores.addAll(ancestor.canonCores);
					if(verbose || k != Index.this.k){
						//we do not need to store core structure for the last layer
						cores.addAll(ancestor.cores);
					}
				//}
			}
			
			
//			if(computeCores){
//			}
				
			final int skip = cores.size();//TODO skip is inherited range
			List<CanonFuture> candidates = new ArrayList<CanonFuture>();
			
			if(combinations.isEmpty()){
				//for layer 1 the cores are the label sequences (which are distinct cores)
				labels.stream().map(LabelSequence::getLabels).map(CPQ::labels).map(q->CanonForm.computeCanon(q, true)).forEach(candidates::add);
			}else{
				//all combinations of cores from previous layers (this can generate duplicates, but all are cores)
				for(BlockPair pair : combinations){
					for(CPQ core1 : pair.first.cores){
						for(CPQ core2 : pair.second.cores){
							candidates.add(CanonForm.computeCanon(CPQ.concat(core1, core2), true));
						}
					}
				}
			}
			
			addCores(candidates);
			candidates.clear();
			
			//all intersections of cores (exactly, these are all cores)
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
			
			computeIntersectionCores(cores, 0, skip, cores.size(), new ArrayList<CPQ>(), new boolean[cores.size()], conflicts, candidates);
			addCores(candidates);
			
			//intersect with identity if possible (this can generate duplicates or non-cores)
			if(isLoop()){
				candidates.clear();
				final int max = cores.size();
				for(int i = skip; i < max; i++){
					candidates.add(CanonForm.computeCanon(CPQ.intersect(cores.get(i), CPQ.id()), false));
				}
				addCores(candidates);
			}
		}
		
		private void computeIntersectionCores(List<CPQ> items, int offset, final int restricted, final int max, List<CPQ> set, boolean[] selected, boolean[][] conflicts, List<CanonFuture> candidates){
			if(offset >= max || set.size() == maxIntersections){
				if(set.size() >= 2){
					candidates.add(CanonForm.computeCanon(CPQ.intersect(set), true));
				}
			}else{
				//don't pick the element
				computeIntersectionCores(items, offset + 1, restricted, max, set, selected, conflicts, candidates);
				
				//pick the element
				for(int i = 0; i < conflicts[offset].length; i++){
					if(selected[i] && conflicts[offset][i]){
						//can't pick a conflicting item
						return;
					}
				}
				
				selected[offset] = true;
				set.add(items.get(offset));
				computeIntersectionCores(items, offset < restricted ? restricted : (offset + 1), restricted, max, set, selected, conflicts, candidates);
				set.remove(set.size() - 1);
				selected[offset] = false;
			}
		}

		public Set<CoreHash> getCanonCores(){
			return canonCores;
		}
		
		public List<CPQ> getCores(){
			return cores;
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
				builder.append(seq.toString());
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
	static final class PathPair implements Comparable<PathPair>{
		/**
		 * The first path of this pair, also the start of the joined path.
		 */
		final LabelledPath first;
		/**
		 * The second path of this pair, also the end of the joined path.
		 */
		final LabelledPath second;
		
		/**
		 * Constructs a new path pair with the given paths.
		 * @param first The first and start path.
		 * @param second The second and end path.
		 */
		PathPair(LabelledPath first, LabelledPath second){
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
			int cmp = Integer.compare(first.getSegmentId(), o.first.getSegmentId());
			return cmp == 0 ? Integer.compare(second.getSegmentId(), o.second.getSegmentId()) : cmp;
		}
	}
	
	private static final record BlockPair(Block first, Block second){
		
		private BlockPair(PathPair pair){
			this(pair.first.getBlock(), pair.second.getBlock());
		}
	}
	
	public static abstract interface ProgressListener{
		
		public abstract void partitionStart(int k);
		
		public abstract void partitioningDone(int k);
	}
}
