package dev.roanh.cpqindex;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import dev.roanh.gmark.conjunct.cpq.CPQ;
import dev.roanh.gmark.core.graph.Predicate;

public class CanonFormTest{
	
	static{
		try{
			Main.loadNatives();
		}catch(UnsatisfiedLinkError | IOException e){
			e.printStackTrace();
		}
	}

	@Test
	public void canon0(){
		Predicate l1 = new Predicate(0, "a");
		Predicate l2 = new Predicate(1, "b");
		Predicate l3 = new Predicate(2, "c");
		
		CanonForm canon = new CanonForm(CPQ.intersect(CPQ.labels(l1, l3), CPQ.labels(l1, l2)));
		assertEquals("s=0,t=1,l0=2,l1=1,l2=1,e0={2,3},e1={},e2={6},e3={7},e4={1},e5={1},e6={5},e7={4}", canon.toStringCanon());
		assertArrayEquals(new byte[]{2, 1, 24, 16, 72, -118, 76, 28, 121, 36, -102, 96}, canon.toBinaryCanon());
		assertEquals("AgEYEEiKTBx5JJpg", canon.toBase64Canon());
		
		canon = new CanonForm(CPQ.intersect(CPQ.labels(l1, l2), CPQ.labels(l1, l3)));
		assertEquals("s=0,t=1,l0=2,l1=1,l2=1,e0={2,3},e1={},e2={6},e3={7},e4={1},e5={1},e6={5},e7={4}", canon.toStringCanon());
		assertArrayEquals(new byte[]{2, 1, 24, 16, 72, -118, 76, 28, 121, 36, -102, 96}, canon.toBinaryCanon());
		assertEquals("AgEYEEiKTBx5JJpg", canon.toBase64Canon());
	}

	@Test
	public void canon1(){
		Predicate l1 = new Predicate(0, "a");
		Predicate l2 = new Predicate(1, "b");
		Predicate l3 = new Predicate(2, "c");
		
		assertEquals(
			new CanonForm(CPQ.intersect(CPQ.labels(l1, l3), CPQ.labels(l1, l2))),
			new CanonForm(CPQ.intersect(CPQ.labels(l1, l2), CPQ.labels(l1, l3)))
		);
	}

	@Test
	public void canon2(){
		Predicate l1 = new Predicate(0, "a");
		Predicate l2 = new Predicate(1, "b");
		Predicate l3 = new Predicate(2, "c");
		
		assertEquals(
			new CanonForm(CPQ.intersect(CPQ.labels(l1, l3), CPQ.intersect(l1, l2))),
			new CanonForm(CPQ.intersect(CPQ.intersect(l1, l2), CPQ.labels(l1, l3)))
		);
	}
	
	@Test
	public void canon3(){
		Predicate l1 = new Predicate(0, "a");
		Predicate l2 = new Predicate(1, "b");
		Predicate l3 = new Predicate(2, "c");
		
		assertEquals(
			new CanonForm(CPQ.intersect(CPQ.labels(l1, l3, l1), CPQ.id(), CPQ.intersect(l1, l2))),
			new CanonForm(CPQ.intersect(CPQ.intersect(l1, l2), CPQ.labels(l1, l3, l1), CPQ.id()))
		);
	}
}
