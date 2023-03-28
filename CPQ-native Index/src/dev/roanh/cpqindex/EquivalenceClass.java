package dev.roanh.cpqindex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import dev.roanh.gmark.core.graph.Predicate;
import dev.roanh.gmark.util.IDable;
import dev.roanh.gmark.util.RangeList;
import dev.roanh.gmark.util.UniqueGraph;
import dev.roanh.gmark.util.UniqueGraph.GraphEdge;

public class EquivalenceClass<V extends Comparable<V>>{//TODO possibly move generics to the top class and make the rest inner classes
	private RangeList<List<LabelledPath>> segments;
	private final int k;
	private final int labelCount;
	
	public static void main(String[] args){
		UniqueGraph<Integer, Predicate> g = new UniqueGraph<Integer, Predicate>();
		g.addUniqueNode(0);
		g.addUniqueNode(1);
		g.addUniqueNode(2);
		g.addUniqueNode(3);
		g.addUniqueNode(4);

		Predicate a = new Predicate(0, "a");
		Predicate b = new Predicate(1, "b");
		Predicate c = new Predicate(2, "c");
		
		g.addUniqueEdge(0, 1, c);
		g.addUniqueEdge(1, 2, a);
		g.addUniqueEdge(1, 3, a);
		g.addUniqueEdge(2, 4, b);
		g.addUniqueEdge(3, 4, b);
		
		new EquivalenceClass<Integer>(3, 3).partition(g);
	}
	
	public EquivalenceClass(int k, int labels){
		this.k = k;
		labelCount = labels;
		segments = new RangeList<List<LabelledPath>>(k, ArrayList::new);
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
		
		System.out.println(pathMap);
		
		segOne.forEach(l->System.out.println(l.pair + " / " + l.labels + " / " + l.segId));
		
		System.out.println("================ k-path");
		//=================================================================================
		
		pathMap.clear();
		
		for(int i = 1; i < k; i++){
			System.out.println("----- " + (i + 1));
			for(int k1 = i - 1; k1 >= 0; k1--){
				int k2 = i - k1 - 1;
				
				System.out.println((k1 + 1) + " + " + (k2 + 1));
				//all k's are one lower than their actual meaning
				
				
				
				for(LabelledPath seg : segments.get(k1)){
					for(LabelledPath end : segments.get(k2)){
						if(!seg.pair.trg.equals(end.pair.src)){
							continue;
						}
						
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
						
//						System.out.println("join: " + seg.pair + " with " + end.pair);
						
					}
					
					
					
				}
				
				id = 1;
				
				//sort
				
				List<LabelledPath> segs = segments.get(i);
				pathMap.values().stream().sorted(this::sortPaths).forEachOrdered(segs::add);
				
				
				
				//assign ids
				
				
				prev = null;
				for(LabelledPath path : segs){
					if(prev == null){
						path.segId = id;
						
					}else{
						if(path.segs.equals(prev.segs)){
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
				
				
			}
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
		if(a.segs.equals(b.segs)){
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
	
	
	
	
	
	private final class LabelledPath{//TODO currently removed the optimisations that store seg/label sequences as integers
		private final Pair pair;
		private Set<List<Predicate>> labels = new HashSet<List<Predicate>>();//label sequences for this path
		
		//id stuff
		private int segId;
		
		private Set<List<LabelledPath>> segs = new HashSet<List<LabelledPath>>();//effectively a history of blocks that were combined to form this path
		
		private LabelledPath(Pair pair){
			this.pair = pair;
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
