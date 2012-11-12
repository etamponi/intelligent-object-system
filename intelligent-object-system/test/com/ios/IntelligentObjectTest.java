package com.ios;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.junit.Test;

import com.esotericsoftware.kryo.Kryo;
import com.ios.listeners.PropertyBinding;

public class IntelligentObjectTest {
	
	public static class Node extends IntelligentObject {
		public Node right, down;
		
		public String content;
		
		public String contentCopy;
		
		public String nonConnected;
		
		public Node() {
			addListener(new PropertyBinding(this, "content", "right.content", "contentCopy"));
		}
		
		public Node(boolean downConnected) {
			addListener(new PropertyBinding(this, "content", "right.content", "down.content", "contentCopy"));
		}
		
	}

	@Test
	public void testCopy() {
		final int DIM = 32;
		final int TIMES = 100;
		
		Node[][] grid = new Node[DIM][DIM];
		
		long startingTime = System.currentTimeMillis();
		
		for(int i = DIM-1; i >= 0; i--) {
			for(int j = DIM-1; j >= 0; j--) {
				Node node = j > 0 ? new Node() : new Node(true);
				grid[i][j] = node;
				if (i+1 < DIM)
					node.set("down",  grid[i+1][j]);
				if (j+1 < DIM)
					node.set("right", grid[i][j+1]);
			}
		}
		
		long elapsed = System.currentTimeMillis() - startingTime;
		System.out.println("Grid prepared in: " + elapsed);
		
		assertEquals(2, grid[4][4].getParentsLinksToThis().size());
		
		Node copy = grid[4][4].copy();
		
		assertEquals(0, copy.getParentsLinksToThis().size());
		assertEquals(1, copy.right.getParentsLinksToThis().size());
		
		startingTime = System.currentTimeMillis();
		
		copy.set("content", "Hello");
		
		elapsed = System.currentTimeMillis() - startingTime;
		System.out.println("Change propagated in copy in: " + elapsed);
		
		startingTime = System.currentTimeMillis();
		
		copy.set("nonConnected", "Hello");
		
		elapsed = System.currentTimeMillis() - startingTime;
		System.out.println("Not listened change propagated in copy in: " + elapsed);
		
		assertEquals("Hello", copy.right.content);
		assertEquals("Hello", copy.contentCopy);
		assertEquals("Hello", copy.right.contentCopy);
		assertEquals(null, grid[4][4].content);
		
		startingTime = System.currentTimeMillis();
		
		grid[0][0].set("content", "Hello 2");
		
		elapsed = System.currentTimeMillis() - startingTime;
		System.out.println("Change propagated in original in: " + elapsed);
		
		assertEquals("Hello 2", grid[0][4].content);
		assertEquals("Hello 2", grid[4][0].content);
		assertEquals("Hello 2", grid[DIM-1][DIM-1].content);
		assertEquals("Hello 2", grid[DIM-1][DIM-1].contentCopy);
		
		assertEquals(1, copy.getBoundProperties().size());
		assertEquals(2, grid[4][4].getBoundProperties().size());
		
		startingTime = System.currentTimeMillis();
		
		for(int i = 0; i < TIMES; i++) {
			grid[4][4].copy();
		}
		
		elapsed = System.currentTimeMillis() - startingTime;
		System.out.println(TIMES + " copies in: " + elapsed);
		
		startingTime = System.currentTimeMillis();
		
		Kryo kryo = new Kryo();
		for(int i = 0; i < TIMES; i++)
			kryo.copy(grid[4][4]);
		
		elapsed = System.currentTimeMillis() - startingTime;
		System.out.println(TIMES + " fast copies in: " + elapsed);
	}
	
	@Test
	public void testSerialization() {
		final int DIM = 32;
		
		Node[][] grid = new Node[DIM][DIM];
		
		long startingTime = System.currentTimeMillis();
		
		for(int i = DIM-1; i >= 0; i--) {
			for(int j = DIM-1; j >= 0; j--) {
				Node node = j > 0 ? new Node() : new Node(true);
				grid[i][j] = node;
				if (i+1 < DIM)
					node.set("down",  grid[i+1][j]);
				if (j+1 < DIM)
					node.set("right", grid[i][j+1]);
			}
		}
		
		long elapsed = System.currentTimeMillis() - startingTime;
		System.out.println("Grid prepared in: " + elapsed);
		
		startingTime = System.currentTimeMillis();
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		grid[0][0].write(out);
		
		elapsed = System.currentTimeMillis() - startingTime;
		System.out.println("Serialized in: " + elapsed);
		
		startingTime = System.currentTimeMillis();
		
		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		Node copy = IntelligentObject.load(in);
		
		elapsed = System.currentTimeMillis() - startingTime;
		System.out.println("Deserialized in: " + elapsed);
		
		copy.set("content", "Hello");
		
		assertEquals("Hello", copy.right.content);
		assertEquals("Hello", copy.contentCopy);
		assertEquals("Hello", copy.right.contentCopy);
		assertEquals("Hello", copy.down.content);		
	}

}
