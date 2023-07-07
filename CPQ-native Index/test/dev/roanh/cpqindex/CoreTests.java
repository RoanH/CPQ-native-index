package dev.roanh.cpqindex;

import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import dev.roanh.gmark.conjunct.cpq.CPQ;
import dev.roanh.gmark.conjunct.cpq.QueryGraphCPQ;
import dev.roanh.gmark.util.Util;

public class CoreTests{

	public static void main(String[] args) throws InterruptedException, ExecutionException{
		Util.setRandomSeed(1234L);
		
		//warmup runs
		for(int i = 0; i < 100; i++){
			CPQ.generateRandomCPQ(4, 2).computeCore();
		}
		
		ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		
		//actual runs
		for(int rules = 4; rules <= 1024; rules *= 2){
			List<Integer> sizeE = new ArrayList<Integer>(1024);
			List<Integer> sizeV = new ArrayList<Integer>(1024);
			List<Callable<Long>> tasks = new ArrayList<Callable<Long>>();
			
			
			for(int i = 0; i < 1024; i++){
				QueryGraphCPQ q = CPQ.generateRandomCPQ(rules, 4).toQueryGraph();
				sizeE.add(q.getEdgeCount());
				sizeV.add(q.getVertexCount());
				tasks.add(()->{
					long start = System.nanoTime();
					q.computeCore();
					long end = System.nanoTime();
					return end - start;
				});
			}
			
			List<Long> times = new ArrayList<Long>(1024);
			for(Future<Long> f : executor.invokeAll(tasks)){
				times.add(f.get());
			}
			
			System.out.print(rules + " DataE: ");
			printStats(sizeE);
			System.out.print("DataV: ");
			printStats(sizeV);
			System.out.print("Time: ");
			printStats(times);
		}
	}
	
	private static void printStats(List<? extends Number> data){
		DoubleSummaryStatistics stats = new DoubleSummaryStatistics();
		data.forEach(num->stats.accept(num.doubleValue()));
		double diff = data.stream().mapToDouble(d->Math.pow(d.doubleValue() - stats.getAverage(), 2.0D)).sum();
		System.out.println("min= " + stats.getMin() + " max= " + stats.getMax() + " avg= " + stats.getAverage() + " stddev= " + Math.sqrt(diff / stats.getCount()));
	}
}
