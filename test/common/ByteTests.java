package common;

import java.nio.ByteBuffer;

import org.junit.Assert;
import org.junit.Test;

public class ByteTests {

	@Test
	public void testInt() {
		ByteBuffer bb = ByteBuffer.allocate(4);
		int val = 225999236;
		bb.putInt(val);
		ByteBuffer bb2 = ByteBuffer.wrap(bb.array());
		Assert.assertEquals(val, bb2.getInt());
	}

	@Test
	public void testLong() {
		ByteBuffer bb = ByteBuffer.allocate(8);
		long val = -262369225l;
		bb.putLong(val);
		ByteBuffer bb2 = ByteBuffer.wrap(bb.array());
		Assert.assertEquals(val, bb2.getLong());
	}
}
