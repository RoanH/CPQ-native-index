package dev.roanh.cpqindex;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;

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

		@Override
		public void intermediateProgress(long cores, int blockDone, int totalBlocks){
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
	 * Logs and intermediate progress update.
	 * @param cores The total number of cores computed so far.
	 * @param blockDone The total number of blocks done.
	 * @param totalBlocks THe total number of blocks.
	 */
	public abstract void intermediateProgress(long cores, int blockDone, int totalBlocks);
	
	/**
	 * Constructs a new progress listener that logs all
	 * event to the given print stream. Intermediate updates
	 * are still printed to standard out.
	 * @param out The print stream to log to.
	 * @return The constructed progress listener.
	 */
	public static ProgressListener stream(PrintStream out){
		return new StreamListener(out);
	}
	
	/**
	 * Constructs a new progress listener that logs all
	 * event to the given file.
	 * @param logFile The name of the file to log to.
	 * @return The constructed progress listener.
	 * @throws IOException When an IOException occurs.
	 */
	public static ProgressListener file(String logFile) throws IOException{
		return stream(new PrintStream(logFile, StandardCharsets.UTF_8));
	}
	
	/**
	 * Constructs a new progress listener that logs all
	 * events to the given file and to Discord. Block updates
	 * are not logged to Discord and intermediate updates
	 * are not logged to the file and also printed to standard out.
	 * @param logFile The name of the file to log to.
	 * @param webhookUrl The Discord webhook to log to.
	 * @return The constructed progress listener.
	 * @throws IOException When an IOException occurs.
	 */
	public static ProgressListener discord(String logFile, String webhookUrl) throws IOException{
		final WebhookClient webhook = new WebhookClientBuilder(webhookUrl).setWait(false).setDaemon(false).build();
		webhook.send("==================================================\nLogging to: " + logFile + "\n==================================================");
		return new StreamListener(new PrintStream(logFile, StandardCharsets.UTF_8)){
			
			@Override
			protected void write(String msg){
				if(msg.startsWith("Blocks ")){
					super.write(msg);
				}else{
					super.write(msg);
					webhook.send(msg);
				}
			}
			
			@Override
			public void intermediateProgress(long total, int blockDone, int totalBlocks){
				super.intermediateProgress(total, blockDone, totalBlocks);
				webhook.send("Cores: " + total + " (Block: " + blockDone + "/" + totalBlocks + ")");
			}
		};
	}
	
	/**
	 * Progress listener that logs to a print stream all events
	 * except for intermediate updates, which are printed to standard out.
	 * @author Roan
	 */
	public static class StreamListener implements ProgressListener{
		/**
		 * The stream to log to.
		 */
		private final PrintStream out;
		
		/**
		 * Constructs a new progress listener that log to the given stream.
		 * @param out The stream to log to.
		 */
		private StreamListener(PrintStream out){
			this.out = out;
		}
		
		/**
		 * Writes a new message to the stream for this listener.
		 * @param msg The message to write.
		 */
		protected void write(String msg){
			out.println(System.currentTimeMillis() + " " + msg);
		}
		
		@Override
		public void partitionStart(int k){
			write("Partition start k=" + k);
		}
		
		@Override
		public void partitionEnd(int k){
			write("Partition end k=" + k);
		}
		
		@Override
		public void partitionCombinationStart(int k1, int k2){
			write("Partition combination start " + k1 + "x" + k2);
		}
		
		@Override
		public void partitionCombinationEnd(int k1, int k2){
			write("Partition combination end " + k1 + "x" + k2);
		}
		
		@Override
		public void computeBlocksStart(int k){
			write("Block start k=" + k);
		}
		
		@Override
		public void computeBlocksEnd(int k){
			write("Block end k=" + k);
		}

		@Override
		public void coresStart(int k){
			write("Cores start k=" + k);
		}
		
		@Override
		public void coresBlocksDone(int done, int total){
			write("Blocks " + done + "/" + total);
		}

		@Override
		public void coresEnd(int k){
			write("Cores end k=" + k);
		}

		@Override
		public void mapStart(){
			write("Map start");
		}

		@Override
		public void mapEnd(){
			write("Map end");
		}

		@Override
		public void intermediateProgress(long total, int blockDone, int totalBlocks){
			System.out.println("Cores: " + total + " (Block: " + blockDone + "/" + totalBlocks + ")");
		}
	}
}