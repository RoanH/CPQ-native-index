package dev.roanh.cpqindex;

public abstract interface ProgressListener{
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
	
	public static final ProgressListener LOG = new ProgressListener(){
		
		@Override
		public void partitionStart(int k){
			System.out.println(System.currentTimeMillis() + " Partition start k=" + k);
		}
		
		@Override
		public void partitionEnd(int k){
			System.out.println(System.currentTimeMillis() + " Partition end k=" + k);
		}
		
		@Override
		public void partitionCombinationStart(int k1, int k2){
			System.out.println(System.currentTimeMillis() + " Partition combination start " + k1 + "x" + k2);
		}
		
		@Override
		public void partitionCombinationEnd(int k1, int k2){
			System.out.println(System.currentTimeMillis() + " Partition combination end " + k1 + "x" + k2);
		}
		
		@Override
		public void computeBlocksStart(int k){
			System.out.println(System.currentTimeMillis() + " Block start k=" + k);
		}
		
		@Override
		public void computeBlocksEnd(int k){
			System.out.println(System.currentTimeMillis() + " Block end k=" + k);
		}

		@Override
		public void coresStart(int k){
			System.out.println(System.currentTimeMillis() + " Cores start k=" + k);
		}
		
		@Override
		public void coresBlocksDone(int done, int total){
			System.out.println(System.currentTimeMillis() + " Blocks " + done + "/" + total);
		}

		@Override
		public void coresEnd(int k){
			System.out.println(System.currentTimeMillis() + " Cores end k=" + k);
		}

		@Override
		public void mapStart(){
			System.out.println(System.currentTimeMillis() + " Map start");
		}

		@Override
		public void mapEnd(){
			System.out.println(System.currentTimeMillis() + " Map end");
		}
	};

	public abstract void partitionStart(int k);
	
	public abstract void partitionCombinationStart(int k1, int k2);
	
	public abstract void partitionCombinationEnd(int k1, int k2);
	
	public abstract void partitionEnd(int k);
	
	public abstract void computeBlocksStart(int k);
	
	public abstract void computeBlocksEnd(int k);
	
	public abstract void coresStart(int k);
	
	public abstract void coresBlocksDone(int done, int total);

	public abstract void coresEnd(int k);
	
	public abstract void mapStart();
	
	public abstract void mapEnd();
}