package dev.roanh.cpqindex;

/**
 * Represents a pair of two labelled paths
 * that were joined to form a new path.
 * @author Roan
 */
public final class PathPair implements Comparable<PathPair>{
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
	public PathPair(LabelledPath first, LabelledPath second){
		this.first = first;
		this.second = second;
	}
	
	/**
	 * Gets the first path in this pair.
	 * @return The first path in this pair.
	 * @see #first
	 */
	public LabelledPath getFirst(){
		return first;
	}
	
	/**
	 * Gets the second path in this pair.
	 * @return The second path in this pair.
	 * @see #second
	 */
	public LabelledPath getSecond(){
		return second;
	}
	
	@Override
	public boolean equals(Object obj){
		PathPair other = (PathPair)obj;
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