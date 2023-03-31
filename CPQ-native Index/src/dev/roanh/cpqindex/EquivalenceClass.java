package dev.roanh.cpqindex;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.Objects;
import java.util.Set;

import dev.roanh.gmark.conjunct.cpq.CPQ;
import dev.roanh.gmark.core.graph.Predicate;
import dev.roanh.gmark.util.IDable;
import dev.roanh.gmark.util.RangeList;
import dev.roanh.gmark.util.UniqueGraph;
import dev.roanh.gmark.util.UniqueGraph.GraphEdge;
import dev.roanh.gmark.util.Util;

public class EquivalenceClass<V extends Comparable<V>>{//TODO possibly move generics to the top class and make the rest inner classes
	private RangeList<List<LabelledPath>> segments;
	private final int k;
	private final int labelCount;
	private List<Block> blocks = new ArrayList<Block>();
	
	public static void main(String[] args){
		try{
			Main.loadNatives();
		}catch(UnsatisfiedLinkError | IOException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Predicate a = new Predicate(0, "0");
		Predicate b = new Predicate(1, "1");
		Predicate c = new Predicate(2, "2");
		
		UniqueGraph<Integer, Predicate> g = new UniqueGraph<Integer, Predicate>();
		g.addUniqueNode(0);
		g.addUniqueNode(1);
		g.addUniqueNode(2);
		g.addUniqueNode(3);
		g.addUniqueNode(4);

		g.addUniqueEdge(0, 1, c);
		g.addUniqueEdge(1, 2, b);
		g.addUniqueEdge(1, 3, a);
		g.addUniqueEdge(2, 4, b);
		g.addUniqueEdge(3, 4, a);
		
		EquivalenceClass<Integer> eq = new EquivalenceClass<Integer>(3, 3);
		
//		UniqueGraph<Integer, Predicate> g = new UniqueGraph<Integer, Predicate>();
//		g.addUniqueNode(0);
//		g.addUniqueNode(1);
//
//		g.addUniqueEdge(0, 1, a);
//		g.addUniqueEdge(1, 1, b);
//		
//		EquivalenceClass<Integer> eq = new EquivalenceClass<Integer>(2, 2);
		
		eq.partition(g);
		eq.computeBlocks();
		System.out.println("Final blocks for CPQ" + eq.k + " | " + eq.blocks.size());
		for(EquivalenceClass<Integer>.Block block : eq.blocks){
			System.out.println(block);
		}
	}
	
	public EquivalenceClass(int k, int labels){
		this.k = k;
		labelCount = labels;
		segments = new RangeList<List<LabelledPath>>(k, ArrayList::new);
	}
	
	public void computeBlocks(){
		
		Map<Pair, Block> nextMap = new HashMap<Pair, Block>();
		Map<Pair, Block> prevMap = null;
		
		for(int j = 0; j < k; j++){
			prevMap = nextMap;
			nextMap = new HashMap<Pair, Block>();
			
			blocks.clear();
			
			System.out.println("===== " + (j + 1));
			List<LabelledPath> segs = segments.get(j);
			int start = 0;
			int lastId = segs.get(0).segId;
			for(int i = 0; i < segs.size(); i++){
//				System.out.println("p: " + segs.get(i));
				if(segs.get(i).segId != lastId){
					List<LabelledPath> slice = segs.subList(start, i);
					
					List<Block> inherited = new ArrayList<Block>();
					if(j > 0){
						for(LabelledPath path : slice){
							Block b = prevMap.remove(path.pair);
							if(b != null){
								inherited.add(b);
							}
						}
					}
					
					
					Block block = new Block(slice, inherited);
					blocks.add(block);
					System.out.println(block);
					
					
					for(Pair pair : block.paths){
						nextMap.put(pair, block);
					}
					
					lastId = segs.get(i).segId;
					start = i;
				}
				
				
				
			}
			
			List<LabelledPath> slice = segs.subList(start, segs.size());
			
			List<Block> inherited = new ArrayList<Block>();
			if(j > 0){
				for(LabelledPath path : slice){
					Block b = prevMap.remove(path.pair);
					if(b != null){
						inherited.add(b);
					}
				}
			}
			
			Block block = new Block(slice, inherited);
			blocks.add(block);
			System.out.println(block);
			
			for(Pair pair : block.paths){
				nextMap.put(pair, block);
			}
			
			
			
		}
		
		//any remaining pairs denote unused blocks
		
		System.out.println("remain: " + prevMap.keySet());
		prevMap.values().stream().distinct().forEach(blocks::add);
		
//		List<LabelledPath> segs = segments.get(k - 1);
//		int start = 0;
//		int lastId = segs.get(0).segId;
//		for(int i = 0; i < segs.size(); i++){
////			System.out.println("p: " + segs.get(i));
//			if(segs.get(i).segId != lastId){
//				blocks.add(new Block(segs.subList(start, i)));
//				
//				lastId = segs.get(i).segId;
//				start = i;
//			}
//			
//			
//			
//		}
//		
//		blocks.add(new Block(segs.subList(start, segs.size())));

//		Map<Pair, Block> blockMap = new HashMap<Pair, Block>();
		
		//TODO basically we want to merge blocks, but we need to keep the information to compute cores later intact, or do things in step with partitioning
		
		
		
		
	}
	
	//partition according to k-path-bisimulation
	public void partition(UniqueGraph<V, Predicate> g) throws IllegalArgumentException{
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
//		Map<V, List<LabelledPath>> pathsZero = new HashMap<V, List<LabelledPath>>();
		LabelledPath prev = null;
		int id = 1;
		for(LabelledPath seg : segOne){
			if(prev == null){
				seg.segId = id;
			}else{
				if(seg.labels.equals(prev.labels)){
					if(seg.isLoop() ^ prev.isLoop()){
						id++;
						seg.segId = id;
					}else{
						//is labels and cyclic patterns (loop) are the same the same segment ID is assigned
						seg.segId = id;
//						seg.bisimilar = true;
//						prev.bisimilar = true;
					}
				}else{
					id++;
					seg.segId = id;
				}
			}

			prev = seg;
//			pathsZero.computeIfAbsent(seg.pair.src, v->new ArrayList<LabelledPath>()).add(seg);
		}
		
		int[] maxSegId = new int[k];
		maxSegId[0] = id;
		
//		System.out.println(pathMap);
		
//		segOne.forEach(l->System.out.println(l.pair + " / " + l.labels + " / " + l.segId));
		
//		System.out.println("================ k-path");
		//=================================================================================
		
		pathMap.clear();
		
		for(int i = 1; i < k; i++){
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
						
						//TODO remove, we need all combinations for cores
//						if(k2 != 0){
//							continue;
//						}
						
						Pair key = new Pair(seg.pair.src, end.pair.trg);
						
						LabelledPath path = pathMap.computeIfAbsent(key, LabelledPath::new);
						
						path.addSegment(seg, end);
//						path.segs.add(seg.segId * maxSegId[i - 1] + end.segId);
//						path.bisimilar = (end.bisimilar || end.isLoop()) && (seg.bisimilar || seg.isLoop());
						if(k2 == 0){//slight optimisation, since we only need one combination to find all paths
							for(List<Predicate> labels : seg.labels){
								for(List<Predicate> label : end.labels){
									path.addLabel(labels, label);
								}
							}
						}
						
//						System.out.println("join: " + seg.pair + " with " + end.pair + " for " + key);
						
					}
					
					
					
				}
				
//				id = 1;
				
			}

			//sort

			List<LabelledPath> segs = segments.get(i);
			pathMap.values().stream().sorted(this::sortPaths).forEachOrdered(segs::add);
			System.out.println("AFTER SORT");
			segs.forEach(System.out::println);



			//assign ids


			prev = null;
			for(LabelledPath path : segs){
				if(prev == null){
					path.segId = id;

				}else{
					if(path.equalSegments(prev)){
						if(!(prev.isLoop() ^ path.isLoop())){//both are a loop or both are not a loop

							path.segId = id;

						}else{
							id++;
							path.segId = id;
						}



					}else{

						id++;
						path.segId = id;


					}




				}


				prev = path;
			}

//				System.out.println("AFTER ASSIGN");
//				segs.forEach(System.out::println);




			pathMap.clear();
		}
		
		
		
//		for(int i = 1; i < k; i++){
//			for(LabelledPath seg : segments.get(i - 1)){
//				for(LabelledPath end : pathsZero.get(seg.pair.trg)){
//					Pair key = new Pair(seg.pair.src, end.pair.trg);
//					
//					LabelledPath path = pathMap.computeIfAbsent(key, LabelledPath::new);
//					
//					path.addSegment(seg, end);
////					path.segs.add(seg.segId * maxSegId[i - 1] + end.segId);
////					path.bisimilar = (end.bisimilar || end.isLoop()) && (seg.bisimilar || seg.isLoop());
//					for(List<Predicate> labels : seg.labels){
//						for(List<Predicate> label : end.labels){
//							path.addLabel(labels, label);
//						}
//					}
//					
////					System.out.println("join: " + seg.pair + " with " + end.pair);
//					
//				}
//				
//				
//				
//			}
//			
//			id = 1;
//			
//			//sort
//			
//			//assign ids
//			
//			
//			
//		}
//		
//		
	}
	
	
	
	
	
//	inline bool cmpsegvaluepointer(const Segment *a, const Segment *b)
//	{
//	    if(a->possiblitilyofbisimilar&&!b->possiblitilyofbisimilar)return true;
//	    else if(!a->possiblitilyofbisimilar&&b->possiblitilyofbisimilar)return false;
//		else if(a->segvalue==b->segvalue){
//	        if(a->segset.size() == b->segset.size()) {
//	            for(int i=0;i<a->segset.size();i++){
//	                if((a->segset)[i] != b->segset[i])return a->segset[i]<b->segset[i];
//	            }
//	            if (a->path.src == a->path.dst&& b->path.src == b->path.dst)return a->path.src <= b->path.src; //a and b loop
//	            else if (a->path.src == a->path.dst)return true;                                               //a is a loop
//	            else if (b->path.src == b->path.dst)return false;                                              //b is a loop
//	            else if (a->path.src < b->path.src)return true;                                                //a.s < b.s
//	            else if (a->path.src == b->path.src && a->path.dst <= b->path.dst)return true;                 //a.s == b.s and a.t <= b.t
//	            return false;
//	        }
//	        return a->segset.size() < b->segset.size();
//		}
//	    return a->segvalue < b->segvalue;
//	}
	
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
//			if(b.pair.toString().endsWith("(2,2)") || a.pair.toString().endsWith("(2,2)")){
//				System.out.println("false: " + b + " / " + a);
//				System.out.println("b: " + b.segs);
//				System.out.println("a: " + a.segs);
//			}
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
	
	
//	inline bool cmpsegvaluepointer_0(const Segment *a, const Segment *b)
//	{
//	    if(a->segvalue==b->segvalue){
//			if(a->path.src==a->path.dst)return true;
//			else if(b->path.src==b->path.dst)return false;
//	        else if(a->path.src<b->path.src)return true;
//	        else if(a->path.src==b->path.src&&a->path.dst<b->path.dst)return true;
//	        else return false;
//		}
//	    else return a->segvalue < b->segvalue;
//	}
	
	
	
	private final class Block{
		private final int id;
		private List<Pair> paths;
		private List<List<Predicate>> labels;
		private List<CPQ> cores = new ArrayList<CPQ>();
		private Set<String> canonCores = new HashSet<String>();
		
		//TODO cores
		
		private Block(List<LabelledPath> slice, List<Block> inherited){
			id = slice.get(0).segId;
			labels = slice.get(0).labels.stream().collect(Collectors.toList());
			paths = slice.stream().map(p->p.pair).collect(Collectors.toList());
			slice.forEach(s->s.block = this);
			//TODO cores
			
			//if any of the segments was
			for(Block block : inherited){
				labels.addAll(block.labels);
			}
			
//			System.out.println("block from: " + slice.size());
			
			//TODO computeCores(slice.get(0).segs, inherited);
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
//				System.out.println("add: " + set);
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
//					builder.append(p.getAlias());
					builder.append(p.isInverse() ? (p.getID() + labelCount) : p.getID());
				}
				builder.append(",");
			}
			builder.delete(builder.length() - 1, builder.length());
			builder.append("},cores={");
			for(CPQ core : cores){
				builder.append(core.toString());
				builder.append(",");
			}
			builder.delete(builder.length() - 1, builder.length());
			builder.append("}]");
			return builder.toString();
		}
	}
	
	private final class LabelledPath{//TODO currently removed the optimisations that store seg/label sequences as integers
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
			main: for(List<EquivalenceClass<V>.LabelledPath> seg : segs){
				for(List<EquivalenceClass<V>.LabelledPath> test : other.segs){
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
			for(List<EquivalenceClass<V>.LabelledPath> seq : segs){
				for(EquivalenceClass<V>.LabelledPath p : seq){
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
			if(obj instanceof EquivalenceClass.Pair){
				EquivalenceClass<?>.Pair other = (EquivalenceClass<?>.Pair)obj;
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
