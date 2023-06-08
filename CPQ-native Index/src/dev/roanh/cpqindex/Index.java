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
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
	private final boolean computeLabels;//both labels and core labels
	private final int k;
	private boolean computeCores;
	private RangeList<Predicate> predicates;

	private RangeList<List<Block>> layers;
	private List<Block> blocks;
	private Map<CoreHash, List<Block>> coreToBlock = new HashMap<CoreHash, List<Block>>();
	
	private ProgressListener progress;
		
	public Index(UniqueGraph<Integer, Predicate> g, int k, int threads) throws IllegalArgumentException, InterruptedException{
		this(g, k, threads, Integer.MAX_VALUE);
	}
	
	public Index(UniqueGraph<Integer, Predicate> g, int k, int threads, int maxIntersections) throws IllegalArgumentException, InterruptedException{
		this(g, k, true, false, threads, maxIntersections, ProgressListener.NONE);
	}
	
	public Index(UniqueGraph<Integer, Predicate> g, int k, boolean computeCores, boolean computeLabels, int threads) throws IllegalArgumentException, InterruptedException{
		this(g, k, computeCores, computeLabels, threads, Integer.MAX_VALUE, ProgressListener.NONE);
	}
	
	public Index(UniqueGraph<Integer, Predicate> g, int k, boolean computeCores, boolean computeLabels, int threads, int maxIntersections, ProgressListener listener) throws IllegalArgumentException, InterruptedException{
		this.computeLabels = computeLabels;
		this.maxIntersections = maxIntersections;
		this.k = k;
		layers = new RangeList<List<Block>>(k, ArrayList::new);
		blocks = layers.get(k - 1);
		setProgressListener(listener == null ? ProgressListener.NONE : listener);
		
		computeBlocks(partition(g));
		if(computeCores){
			computeCores(threads);
			this.computeCores = true;
		}else{
			this.computeCores = false;
		}
	}
	
	public Index(InputStream source) throws IOException{
		DataInputStream in = new DataInputStream(source);
		boolean full = in.readBoolean();
		computeCores = in.readBoolean();
		computeLabels = in.readBoolean();
		maxIntersections = in.readInt();
		k = in.readInt();
		progress = ProgressListener.NONE;
		
		if(full){
			predicates = new RangeList<Predicate>(in.readInt());
			for(int i = 0; i < predicates.size(); i++){
				byte[] data = new byte[in.readInt()];
				in.readFully(data);
				predicates.set(i, new Predicate(i, new String(data, StandardCharsets.UTF_8)));
			}
		}
		
		RangeList<Block> blockMap = new RangeList<Block>(in.readInt());
		layers = new RangeList<List<Block>>(k, ArrayList::new);
		blocks = layers.get(k - 1);
		for(int i = full ? 0 : (k - 1); i < k; i++){
			List<Block> layer = layers.get(i);
			int len = in.readInt();
			for(int j = 0; j < len; j++){
				Block block = new Block(in, full, blockMap);
				layer.add(block);
				blockMap.set(block.getId(), block);
			}			
		}

		int len = in.readInt();
		for(int i = 0; i < len; i++){
			CoreHash key = CoreHash.read(in);

			int count = in.readInt();
			List<Block> blocks = new ArrayList<Block>(count);
			for(int c = 0; c < count; c++){
				blocks.add(blockMap.get(in.readInt()));
			}

			coreToBlock.put(key, blocks);
		}
	}
	
	public void write(OutputStream target, boolean full) throws IOException{
		DataOutputStream out = new DataOutputStream(target);
		out.writeBoolean(full);
		out.writeBoolean(computeCores);
		out.writeBoolean(computeLabels);
		out.writeInt(maxIntersections);
		out.writeInt(k);
		
		if(full){
			out.writeInt(predicates.size());
			for(Predicate p : predicates){
				byte[] str = p.getAlias().getBytes(StandardCharsets.UTF_8);
				out.writeInt(str.length);
				out.write(str);
			}
		}
		
		out.writeInt(layers.get(k - 1).stream().mapToInt(Block::getId).max().orElse(0) + 1);
		for(int i = full ? 0 : (k - 1); i < k; i++){
			List<Block> layer = layers.get(i);
			out.writeInt(layer.size());
			for(Block block : layer){
				block.write(out, full);
			}
		}
		
		out.writeInt(coreToBlock.size());
		for(Entry<CoreHash, List<Block>> entry : coreToBlock.entrySet()){
			entry.getKey().write(out);
			List<Block> blocks = entry.getValue();
			out.writeInt(blocks.size());
			for(Block block : blocks){
				out.writeInt(block.getId());
			}
		}
	}
	
	public List<Pair> query(CPQ cpq) throws IllegalArgumentException{
		if(cpq.getDiameter() > k){
			throw new IllegalArgumentException("Query diameter larger than index diameter.");
		}
		
		CoreHash key = CanonForm.computeCanon(cpq, false).toHashCanon();
		System.out.println("key: " + key);
		System.out.println("ret: " + coreToBlock.get(key));
		
		return coreToBlock.getOrDefault(key, Collections.emptyList()).stream().flatMap(b->b.getPaths().stream()).collect(Collectors.toList());
	}
	
	public void setProgressListener(ProgressListener listener){
		progress = listener;
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
			if(block.labels != null){
				block.labels.sort(null);
			}
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
				String str = block.labels.get(i).toString();
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
		progress.mapStart();
		for(Block block : blocks){
			for(CoreHash core : block.canonCores){
				coreToBlock.computeIfAbsent(core, k->new ArrayList<Block>()).add(block);
			}
		}
		progress.mapEnd();
	}
	
	public long getTotalCores(){
		return blocks.stream().mapToInt(b->b.canonCores.size()).summaryStatistics().getSum();
	}
	
	public int getUniqueCores(){
		return coreToBlock.size();
	}
	
	private void computeBlocks(RangeList<List<LabelledPath>> segments){
		Map<Pair, LabelledPath> unused = new HashMap<Pair, LabelledPath>();
		
		for(int j = 0; j < k; j++){
			final int lk = j + 1;
			progress.computeBlocksStart(lk);

			List<Block> layerBlocks = layers.get(j);
			List<LabelledPath> segs = segments.get(j);
			int start = 0;
			int lastId = segs.get(0).getSegmentId();
			for(int i = 0; i <= segs.size(); i++){
				if(i == segs.size() || segs.get(i).getSegmentId() != lastId){
					List<LabelledPath> slice = segs.subList(start, i);
					layerBlocks.add(new Block(lk, slice));
					
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
				progress.computeBlocksEnd(lk);
			}
		}
		
		//any remaining pairs denote blocks from previous layers
		List<LabelledPath> remaining = unused.values().stream().sorted(Comparator.comparing(LabelledPath::getSegmentId)).collect(Collectors.toList());
		if(!remaining.isEmpty()){
			int start = 0;
			int lastId = remaining.get(0).getSegmentId();
			for(int i = 0; i <= remaining.size(); i++){
				if(i == remaining.size() || remaining.get(i).getSegmentId() != lastId){
					List<LabelledPath> slice = remaining.subList(start, i);
					blocks.add(new Block(k, slice));
					
					if(i != remaining.size()){
						lastId = remaining.get(i).getSegmentId();
						start = i;
					}
				}
			}
		}
		
		progress.computeBlocksEnd(k);
	}
	
	public void computeCores(int threads) throws InterruptedException{
		if(computeCores){
			throw new IllegalStateException("Cores have already been computed.");
		}else if(predicates == null){
			throw new IllegalStateException("Cannot compute cores on an index that wasn't fully saved.");
		}
		
		ExecutorService executor = Executors.newFixedThreadPool(threads);

		//process cores layer by layer
		for(int i = 0; i < k; i++){
			progress.coresStart(i + 1);
			
			final int total = layers.get(i).size();
			Lock lock = new ReentrantLock();
			Condition cond = lock.newCondition();
			AtomicInteger done = new AtomicInteger(0);
			ListIterator<Block> iter = layers.get(i).listIterator(total);
			while(iter.hasPrevious()){
				Block block = iter.previous();
				executor.execute(()->{
					block.computeCores();

					if(done.incrementAndGet() == total){
						lock.lock();
					}else if(!lock.tryLock()){
						return;
					}

					try{
						cond.signal();
					}finally{
						lock.unlock();
					}
				});
			}
			
			while(true){
				try{
					lock.lock();
					if(cond.await(10, TimeUnit.MINUTES)){
						int val = done.get();
						progress.coresBlocksDone(val, total);
						if(val == total){
							break;
						}
					}else{
						System.out.println("Cores: " + getTotalCores());
					}
				}finally{
					lock.unlock();
				}
			}
			
			progress.coresEnd(i + 1);
		}
		
		executor.shutdown();
		computeCores = true;
		System.out.println("Total cores: " + blocks.stream().mapToLong(b->b.canonCores.size()).sum());
		
		mapCoresToBlocks();
	}
	
	//partition according to k-path-bisimulation
	private RangeList<List<LabelledPath>> partition(UniqueGraph<Integer, Predicate> g) throws IllegalArgumentException{
		if(k <= 0){
			throw new IllegalArgumentException("Invalid value of k for bisimulation, has to be 1 or greater.");
		}
		
		progress.partitionStart(1);
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
		List<LabelledPath> segOne = segments.get(0);
		pathMap.values().stream().sorted(this::sortOnePath).forEachOrdered(segOne::add);
		
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
		progress.partitionEnd(1);
		
		//classes for 2-path-bisimulation to k-path-bisimulation
		for(int i = 1; i < k; i++){
			progress.partitionStart(i + 1);
			pathMap.clear();

			id++;
			for(int k1 = i - 1; k1 >= 0; k1--){//all combinations to make CPQi
				int k2 = i - k1 - 1;
				
				progress.partitionCombinationStart(k1 + 1, k2 + 1);
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
				
				progress.partitionCombinationEnd(k1 + 1, k2 + 1);
			}
			
			//sort
			List<LabelledPath> segs = segments.get(i);
			pathMap.values().forEach(LabelledPath::cacheHashCode);
			pathMap.values().stream().sorted(this::sortPaths).forEachOrdered(segs::add);

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
			
			progress.partitionEnd(i + 1);
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
	
	/**
	 * Representation of a single block in the index containing
	 * the paths, labels and cores of the partition it represents.
	 * @author Roan
	 */
	public final class Block{
		/**
		 * The ID of this block.
		 */
		private final int id;
		/**
		 * The value of k for the index layer this core belongs to.
		 */
		private final int k;
		/**
		 * A list of all paths stored at this block.
		 */
		private final List<Pair> paths;
		/**
		 * A list of all label sequences that map to this block. This is
		 * the same set of label sequences as computed in the original
		 * paper on language aware indexing. This list may also be set
		 * to null if its computation is not explicitly requested by
		 * setting {@link Index#computeLabels} to true.
		 */
		private List<LabelSequence> labels;
		private List<CPQ> cores;//TODO never restored after write read - some cleared after compute
		private final Set<CoreHash> canonCores;
		private List<BlockPair> combinations;
		/**
		 * The block from the previous layer that the paths in this block were stored at.
		 * @see #paths
		 */
		private Block ancestor;
		
		private Block(int k, List<LabelledPath> slice){
			this.k = k;
			
			LabelledPath range = slice.get(0);
			id = range.getSegmentId();
			paths = slice.stream().map(LabelledPath::getPair).collect(Collectors.toList());
			slice.forEach(s->s.setBlock(this));
			combinations = range.getSegments().stream().map(BlockPair::new).toList();
			cores = new ArrayList<CPQ>();
			canonCores = new HashSet<CoreHash>();
			
			if(computeLabels || combinations.isEmpty()){
				//we need labels to compute cores for k = 1 and in rare cases higher k where a k = 1 block did not get any higher k paths added
				labels = new ArrayList<LabelSequence>();
				labels.addAll(range.getLabels());
			}else{
				labels = null;
			}
			
			//we inherit all labels from the previous layer block the paths in this block are a subset of
			if(range.hasAncestor()){
				ancestor = range.getAncestor().getBlock();
				if(computeLabels){
					labels.addAll(ancestor.labels);
				}
			}else{
				ancestor = null;
			}
		}
		
		private Block(DataInputStream in, boolean full, RangeList<Block> blockMap) throws IOException{
			id = in.readInt();
			
			int len = in.readInt();
			paths = new ArrayList<Pair>(len);
			for(int i = 0; i < len; i++){
				paths.add(new Pair(in));
			}
			
			if(full){
				k = in.readInt();
				
				len = in.readInt();
				labels = new ArrayList<LabelSequence>(len);
				for(int i = 0; i < len; i++){
					labels.add(new LabelSequence(in, predicates));
				}
				
				int anc = in.readInt();
				ancestor = anc == -1 ? null : blockMap.get(anc);
				
				len = in.readInt();
				combinations = new ArrayList<BlockPair>(len);
				for(int i = 0; i < len; i++){
					combinations.add(new BlockPair(in, blockMap));
				}
				
				len = in.readInt();
				canonCores = new HashSet<CoreHash>(len);
				for(int i = 0; i < len; i++){
					canonCores.add(CoreHash.read(in));
				}
				
				cores = new ArrayList<CPQ>();
			}else{
				k = -1;
				ancestor = null;
				labels = null;
				combinations = null;
				canonCores = null;
				cores = null;
			}
		}
		
		private void write(DataOutputStream out, boolean full) throws IOException{
			out.writeInt(id);
			
			out.writeInt(paths.size());
			for(Pair pair : paths){
				pair.write(out);
			}
			
			if(full){
				out.writeInt(k);
				
				out.writeInt(labels == null ? 0 : labels.size());
				if(labels != null){
					for(LabelSequence seq : labels){
						seq.write(out);
					}
				}
				
				out.writeInt(ancestor == null ? -1 : ancestor.getId());
				
				out.writeInt(combinations == null ? 0 : combinations.size());
				if(combinations != null){
					for(BlockPair pair : combinations){
						pair.write(out);
					}
				}
				
				out.writeInt(canonCores == null ? 0 : canonCores.size());
				if(canonCores != null){
					for(CoreHash core : canonCores){
						core.write(out);
					}
				}
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
		
		public final boolean isLoop(){
			return paths.get(0).isLoop();
		}
		
		private final void addCore(CanonForm canon, boolean noSave){
			if(canonCores.add(canon.toHashCanon())){
				if(!noSave){
					cores.add(canon.getCPQ());
				}
			}
		}
		
		private final void addCore(CPQ q, boolean noSave){
			addCore(CanonForm.computeCanon(q, false), noSave);
		}
		
		private void computeCores(){
			//inherited from previous layer blocks
			if(ancestor != null){//only need to go back one level since the previous level already collected the level before that
				//these are by definition of a different diameter
				canonCores.addAll(ancestor.canonCores);
				cores.addAll(ancestor.cores);
			}
			
			//all cores so far are inherited fully processed cores from the ancestor, we skip these for intersection with each other and identity
			final int skip = cores.size();
			boolean noSave = k == Index.this.k && !computeLabels;
			
			if(combinations.isEmpty()){
				//for layer 1 the cores are the label sequences (which are distinct cores)
				labels.stream().map(LabelSequence::getLabels).map(CPQ::labels).map(q->CanonForm.computeCanon(q, true)).forEach(c->this.addCore(c, false));
			}else{
				//all combinations of cores from previous layers (this can generate duplicates, but all are cores unless both cores are a loop)
				for(BlockPair pair : combinations){
					for(CPQ core1 : pair.first().cores){
						for(CPQ core2 : pair.second().cores){
							addCore(CPQ.concat(core1, core2), false);
						}
					}
				}
			}
			
			//all cores up to intersection
			final int end = cores.size();
			
			//all intersections of cores (these are all distinct cores unless blackflow happens, so we cannot assume them to be cores)
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
			
			computeIntersectionCores(cores, 0, skip, cores.size(), new ArrayList<CPQ>(), new boolean[cores.size()], conflicts, noSave, isLoop());
			
			//intersect with identity if possible, these are not always cores and not always unique (not that intersections were already handled so they are skipped)
			if(isLoop()){
				for(int i = skip; i < end; i++){
					addCore(CPQ.intersect(cores.get(i), CPQ.id()), noSave);
				}
			}
			
			if(noSave){
				cores = null;
				labels = null;
				ancestor = null;
				combinations = null;
			}
		}
		
		private final void computeIntersectionCores(List<CPQ> items, int offset, final int restricted, final int max, List<CPQ> set, boolean[] selected, boolean[][] conflicts, final boolean noSave, final boolean id){
			if(offset >= max || set.size() == maxIntersections){
				if(set.size() >= 2){
					CPQ q = CPQ.intersect(new ArrayList<CPQ>(set));
					addCore(q, noSave);
					if(id){
						addCore(CPQ.intersect(q, CPQ.id()), noSave);
					}
				}
			}else{
				//don't pick the element
				computeIntersectionCores(items, offset + 1, restricted, max, set, selected, conflicts, noSave, id);
				
				//pick the element
				for(int i = 0; i < conflicts[offset].length; i++){
					if(selected[i] && conflicts[offset][i]){
						//can't pick a conflicting item
						return;
					}
				}
				
				selected[offset] = true;
				CPQ q = items.get(offset);
				set.add(q);
				computeIntersectionCores(items, offset < restricted ? restricted : (offset + 1), restricted, max, set, selected, conflicts, noSave, id);
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
			if(labels != null){
				for(LabelSequence seq : labels){
					builder.append(seq.toString());
					builder.append(",");
				}
				builder.delete(builder.length() - 1, builder.length());
			}
			builder.append("},cores={");
			if(cores != null){
				for(CPQ core : cores){
					builder.append(core.toString());
					builder.append(",");
				}
				if(!cores.isEmpty()){
					builder.delete(builder.length() - 1, builder.length());
				}
			}
			builder.append("}]");
			return builder.toString();
		}
	}
}
