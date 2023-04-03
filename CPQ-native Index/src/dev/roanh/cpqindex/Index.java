package dev.roanh.cpqindex;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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

public class Index<V extends Comparable<V>>{
	private static final boolean COMPUTE_CORES = false;
	private RangeList<List<LabelledPath>> segments;
	private final int k;
	private List<Block> blocks = new ArrayList<Block>();
	
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
		
		Index<Integer> eq = new Index<Integer>(4);
		
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
		
		eq.partition(g);//TODO improve API
		eq.computeBlocks();
		System.out.println("Final blocks for CPQ" + eq.k + " | " + eq.blocks.size());
		eq.blocks.stream().sorted((a, b)->{
			int c = Integer.compare(a.paths.get(0).src, b.paths.get(0).src);
			if(c == 0){
				c = Integer.compare(a.paths.get(0).trg, b.paths.get(0).trg);
			}
			
			return c;
		}).forEach(System.out::println);
	}
	
	public Index(int k){
		this.k = k;
		segments = new RangeList<List<LabelledPath>>(k, ArrayList::new);
	}
	
	public void computeBlocks(){
		Map<Pair, Block> pairMap = new HashMap<Pair, Block>();
//		Map<Pair, Block> prevMap = null;
		
		for(int j = 0; j < k; j++){
//			prevMap = nextMap;
//			nextMap = new HashMap<Pair, Block>();
			
			List<LabelledPath> segs = segments.get(j);
			System.out.println("===== " + (j + 1));

			int start = 0;
			int lastId = segs.get(0).segId;
			for(int i = 0; i < segs.size(); i++){
//				System.out.println("p: " + segs.get(i));
				if(segs.get(i).segId != lastId){
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
					System.out.println(block);
					
					if(j != k - 1){
						for(Pair pair : block.paths){
							pairMap.put(pair, block);
						}
					}else{
						blocks.add(block);
					}
					
					lastId = segs.get(i).segId;
					start = i;
				}
			}
			
			List<LabelledPath> slice = segs.subList(start, segs.size());
			
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
			System.out.println(block);
			
			if(j != k - 1){
				for(Pair pair : block.paths){
					pairMap.put(pair, block);
				}
			}else{
				blocks.add(block);
			}
			
			System.out.println("Block count: " + blocks.size());
		}
		
		//any remaining pairs denote unused blocks
		System.out.println("remain: " + pairMap.keySet());
		pairMap.values().stream().distinct().forEach(blocks::add);
	}
	
	//partition according to k-path-bisimulation
	private void partition(UniqueGraph<V, Predicate> g) throws IllegalArgumentException{
		if(k <= 0){
			throw new IllegalArgumentException("Invalid value of k for bisimulation, has to be 1 or greater.");
		}
		
		//we first compute classes for 1-path-bisimulation
		Map<Pair, LabelledPath> pathMap = new HashMap<Pair, LabelledPath>();
		for(GraphEdge<V, Predicate> edge : g.getEdges()){
			//forward edges S1(u,v) <- all labels on edges between u and v
			pathMap.computeIfAbsent(new Pair(edge.getSource(), edge.getTarget()), LabelledPath::new).addLabel(edge.getData());
			
			//inverse edges S1(v,u) <- all labels on inverse edges between v and u
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
		
		
//		System.out.println("================ k-path");
		//=================================================================================
		
		
		for(int i = 1; i < k; i++){
			pathMap.clear();

			id++;
			System.out.println("----- " + (i + 1));
			for(int k1 = i - 1; k1 >= 0; k1--){
				int k2 = i - k1 - 1;
				
//				System.out.println((k1 + 1) + " + " + (k2 + 1));
				//all k's are one lower than their actual meaning
				
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

			//assign ids
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
	
	private final class Block{
		private final int id;
		private List<Pair> paths;
		private List<List<Predicate>> labels;
		private List<CPQ> cores = new ArrayList<CPQ>();
		private Set<String> canonCores = new HashSet<String>();
		
		private Block(List<LabelledPath> slice, List<Block> inherited){
			id = slice.get(0).segId;
			labels = slice.get(0).labels.stream().collect(Collectors.toList());
			paths = slice.stream().map(p->p.pair).collect(Collectors.toList());
			slice.forEach(s->s.block = this);
			
			//if any of the segments was
			for(Block block : inherited){
				labels.addAll(block.labels);
			}
			
//			System.out.println("block from: " + slice.size());
			
			if(COMPUTE_CORES){
				computeCores(slice.get(0).segs, inherited);
			}
		}
		
		private void computeCores(Set<List<LabelledPath>> segs, List<Block> inherited){
			if(segs.isEmpty()){
				labels.stream().map(CPQ::labels).forEach(q->{
					String canon = new CanonForm(q).toBase64Canon();
					if(canonCores.add(canon)){
						cores.add(q);
					}
				});
			}else{
				
				for(Block block : inherited){//TODO technically we have a number of uniqueness guarantees here (unique within a block)
					for(CPQ q : block.cores){
						String canon = new CanonForm(q).toBase64Canon();
						if(canonCores.add(canon)){
							cores.add(q);
						}
					}
				}
				
				//TODO inherited cores are still a thing
				
				for(List<LabelledPath> pair : segs){
//					System.out.println("concat: " + pair.get(0).block.cores.size() + " | " + pair.get(1).block.cores.size() + " paths: " + pair.get(0).pair + " | " + pair.get(1).pair);
					for(CPQ core1 : pair.get(0).block.cores){
						for(CPQ core2 : pair.get(1).block.cores){
							CPQ q = CPQ.concat(core1, core2);
							String canon = new CanonForm(q).toBase64Canon();
							if(canonCores.add(canon)){
								cores.add(q);
							}
						}
					}
				}
			}
			
//			System.out.println("init set for " + paths + " | " + labels.size());
//			cores.forEach(System.out::println);
			
			Util.computeAllSubsets(cores, set->{
				if(set.size() >= 2){
					CPQ q = CPQ.intersect(set);
					String canon = new CanonForm(q).toBase64Canon();
					if(canonCores.add(canon)){
						cores.add(q);
					}
				}
			});
			
			if(paths.get(0).isLoop()){//TODO does it suffice to only check one?
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
					builder.append(p.isInverse() ? (p.getID() + 4) : p.getID());//TODO
//					builder.append(p.getAlias());
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
	
	private final class LabelledPath{
		private final Pair pair;
		private Set<List<Predicate>> labels = new HashSet<List<Predicate>>();//label sequences for this path
		
		//id stuff
		private int segId;
		private Block block;
		
		private Set<List<LabelledPath>> segs = new HashSet<List<LabelledPath>>();//effectively a history of blocks that were combined to form this path
		
		private LabelledPath(Pair pair){
			this.pair = pair;
		}
		
		public boolean equalSegments(LabelledPath other){
			main: for(List<Index<V>.LabelledPath> seg : segs){
				for(List<Index<V>.LabelledPath> test : other.segs){
					if(seg.get(0).segId == test.get(0).segId && seg.get(1).segId == test.get(1).segId){
						continue main;
					}
				}
				
				return false;
			}
			
			return true;
		}
		
		public void addSegment(LabelledPath first, LabelledPath last){
			segs.add(Arrays.asList(first, last));
		}
		
		public void addLabel(Predicate label){
			labels.add(Arrays.asList(label));
		}
		
		public void addLabel(List<Predicate> first, List<Predicate> last){
			//addLabel((1 + first) * labelCount * 2 + last);//TODO formula up for discussion I guess
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
			for(List<Index<V>.LabelledPath> seq : segs){
				for(Index<V>.LabelledPath p : seq){
					builder.append(p.segId);
				}
				builder.append(",");
			}
			builder.delete(builder.length() - 1, builder.length());
			builder.append("}]");
			builder.append("}]");
			return builder.toString();
		}
	}
	
	private final class Pair{//aka path, aka st-pair, aka pathkey
		private V src;//src,u
		private V trg;//trg,v
		
		private Pair(V src, V trg){
			this.src = src;
			this.trg = trg;
		}
		
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
