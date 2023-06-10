package dev.roanh.cpqindex;

import java.io.PrintStream;

/**
 * Interface for index construction progress listeners.
 * @author Roan
 * @see ProgressListener#NONE
 * @see ProgressListener#LOG
 */
public abstract interface ProgressListener{
	/**
	 * Default listener that logs all events to standard out.
	 */
	public static final ProgressListener LOG = stream(System.out);
	/**
	 * Default listener that ignores all events.
	 */
	public static final ProgressListener NONE = new ProgressListener(){
		
		@Override
		public void partitionStart(int k){
		}
		
		@Override
		public void partitionEnd(int k){
		}
		
		@Override
		public void partitionCombinationStart(int k1, int k2){
		}
		
		@Override
		public void partitionCombinationEnd(int k1, int k2){
		}
		
		@Override
		public void computeBlocksStart(int k){
		}
		
		@Override
		public void computeBlocksEnd(int k){
		}

		@Override
		public void coresBlocksDone(int done, int total){
		}

		@Override
		public void coresStart(int k){
		}

		@Override
		public void coresEnd(int k){
		}

		@Override
		public void mapStart(){
		}

		@Override
		public void mapEnd(){
		}
	};
	
	/**
	 * Called when graph partitioning for a new layer starts.
	 * @param k The diameter for the layer being partitioned.
	 */
	public abstract void partitionStart(int k);
	
	/**
	 * Called when partitions are constructed from two previous
	 * blocks from a different layer (start).
	 * @param k1 The diameter of the first block.
	 * @param k2 The diameter of the second block.
	 */
	public abstract void partitionCombinationStart(int k1, int k2);
	
	/**
	 * Called when partitions are constructed from two previous
	 * blocks from a different layer (end).
	 * @param k1 The diameter of the first block.
	 * @param k2 The diameter of the second block.
	 */
	public abstract void partitionCombinationEnd(int k1, int k2);
	
	/**
	 * Called when graph partitioning for a new layer ends.
	 * @param k The diameter for the layer that was partitioned.
	 */
	public abstract void partitionEnd(int k);
	
	/**
	 * Called when blocks are being computed for a new index layer.
	 * @param k The diameter for the layer that blocks are being computed for.
	 */
	public abstract void computeBlocksStart(int k);
	
	/**
	 * Called when blocks are done being computed for a new index layer.
	 * @param k The diameter for the layer that blocks were computed for.
	 */
	public abstract void computeBlocksEnd(int k);
	
	/**
	 * Called when cores for a new layer start being computed.
	 * @param k The diameter for the layer cores are computed for.
	 */
	public abstract void coresStart(int k);
	
	/**
	 * Intermediate core computation progress update.
	 * @param done Total number of computed blocks.
	 * @param total The number of blocks to compute in total.
	 */
	public abstract void coresBlocksDone(int done, int total);

	/**
	 * Called when cores for a new layer are done being computed.
	 * @param k The diameter for the layer cores were computed for.
	 */
	public abstract void coresEnd(int k);
	
	/**
	 * Called when mapping cores to blocks starts.
	 */
	public abstract void mapStart();
	
	/**
	 * Called when mapping cores to blocks is done.
	 */
	public abstract void mapEnd();
	
	/**
	 * Constructs a new progress listener that logs all
	 * event to the given print stream.
	 * @param out The print stream to log to.
	 * @return The constructed progress listener.
	 */
	public static ProgressListener stream(PrintStream out){
		return new ProgressListener(){
			
			@Override
			public void partitionStart(int k){
				out.println(System.currentTimeMillis() + " Partition start k=" + k);
			}
			
			@Override
			public void partitionEnd(int k){
				out.println(System.currentTimeMillis() + " Partition end k=" + k);
			}
			
			@Override
			public void partitionCombinationStart(int k1, int k2){
				out.println(System.currentTimeMillis() + " Partition combination start " + k1 + "x" + k2);
			}
			
			@Override
			public void partitionCombinationEnd(int k1, int k2){
				out.println(System.currentTimeMillis() + " Partition combination end " + k1 + "x" + k2);
			}
			
			@Override
			public void computeBlocksStart(int k){
				out.println(System.currentTimeMillis() + " Block start k=" + k);
			}
			
			@Override
			public void computeBlocksEnd(int k){
				out.println(System.currentTimeMillis() + " Block end k=" + k);
			}

			@Override
			public void coresStart(int k){
				out.println(System.currentTimeMillis() + " Cores start k=" + k);
			}
			
			@Override
			public void coresBlocksDone(int done, int total){
				out.println(System.currentTimeMillis() + " Blocks " + done + "/" + total);
			}

			@Override
			public void coresEnd(int k){
				out.println(System.currentTimeMillis() + " Cores end k=" + k);
			}

			@Override
			public void mapStart(){
				out.println(System.currentTimeMillis() + " Map start");
			}

			@Override
			public void mapEnd(){
				out.println(System.currentTimeMillis() + " Map end");
			}
		};
	}
}