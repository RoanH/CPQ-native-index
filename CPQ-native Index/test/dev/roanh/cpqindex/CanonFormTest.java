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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void canon0(){
		Predicate l1 = new Predicate(0, "1");
		Predicate l2 = new Predicate(1, "2");
		Predicate l3 = new Predicate(2, "3");
		
		CanonForm canon = new CanonForm(CPQ.intersect(CPQ.labels(l1, l3), CPQ.labels(l1, l2)));
		assertEquals("s=4,t=7,v={4,5,6,7},l0={0,1},l1={2},l2={3},e0={5},e1={6},e2={7},e3={7},e4={0,1},e5={2},e6={3},e7={}", canon.toStringCanon());
		assertArrayEquals(new byte[]{2, 39, -110, -18, 48, 32, 66, 80, -117, 52, -29, -49, 64, -108, 88}, canon.toBinaryCanon());
		assertEquals("AieS7jAgQlCLNOPPQJRY", canon.toBase64Canon());
		//00000010 00100111 10010010 11101110 00110000 00100000 01000010 01010000 10001011 00110100 11100011 11001111 01000000 10010100 01011000
	}
}