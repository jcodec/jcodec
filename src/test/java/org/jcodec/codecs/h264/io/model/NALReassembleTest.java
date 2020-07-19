package org.jcodec.codecs.h264.io.model;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;


public class NALReassembleTest 
{
	
	@Test
	public void testCorrectlyValidtesPointlessInput()
	{
		byte[][] data = new byte[][] {  };
				
		// Prepare & reassemble - null
		byte[] out = NALReasemble.defragmentFUANals(null);

		// Prepare & reassemble - empty
		final List<ByteBuffer> in = assemble(data);
		out = NALReasemble.defragmentFUANals(in);
		
		assertTrue("broken logic", out != null && out.length == 0);
	}
	
	
	@Test
	public void testCorrectlyValidtesIncorrecyFirstPacketInput()
	{
		byte[][] data1 = new byte[][] { { (byte)0x7D, (byte)0x81 } };
		byte[][] data2 = new byte[][] { { (byte)0x7C, (byte)0x01 } };
		byte[][] data3 = new byte[][] { { (byte)0x7C, (byte)0xC1 } };
				
		// Prepare & reassemble - bad type - FU-B
		List<ByteBuffer> in = assemble(data1);
		byte[] out = NALReasemble.defragmentFUANals(in);
		
		assertTrue("broken logic", out != null && out.length == 0);
		
		// Prepare & reassemble - bad data, first is NOT start FU
		in = assemble(data2);
		out = NALReasemble.defragmentFUANals(in);
		
		assertTrue("broken logic", out != null && out.length == 0);

		// Prepare & reassemble - bad type - FU-A START & END
		in = assemble(data3);
		out = NALReasemble.defragmentFUANals(in);
		
		assertTrue("broken logic", out != null && out.length == 0);
	}
	
	
	@Test
	public void testCorrectlyValidtesMultipleStartFUs()
	{
		byte[][] data = new byte[][] 
		{ 
			new byte[] { (byte)0x7C, (byte)0x81  }, 
		    new byte[] { (byte)0x7C, (byte)0x81  }
		};
				
		byte[] result = new byte[] 
		{ 
			(byte)0xE1 	
		};
		
		// Prepare & reassemble
		final List<ByteBuffer> in = assemble(data);
		final byte[] out = NALReasemble.defragmentFUANals(in);
		
		assertTrue("broken logic", out != null && out.length > 0);
		assertArrayEquals("bad reassembly", out, result); 
	}
	
	
	@Test
	public void testCorrectlyValidtesMissingEndFU()
	{
		byte[][] data = new byte[][] 
		{ 
			new byte[] { (byte)0x7C, (byte)0x81  }, 
		    new byte[] { (byte)0x7C, (byte)0x01  }
		};
				
		byte[] result = new byte[] 
		{ 
			(byte)0xE1 	
		};
		
		// Prepare & reassemble
		final List<ByteBuffer> in = assemble(data);
		final byte[] out = NALReasemble.defragmentFUANals(in);
		
		assertTrue("broken logic", out != null && out.length > 0);
		assertArrayEquals("bad reassembly", out, result); 
	}
	
	
	@Test
	public void testCorrectlyValidtesEndFUBeforeEnd()
	{
		byte[][] data = new byte[][] 
		{ 
			new byte[] { (byte)0x7C, (byte)0x81, 0x01, 0x02  }, 
			new byte[] { (byte)0x7C, (byte)0x01, 0x03, 0x04  }, 
			new byte[] { (byte)0x7C, (byte)0x41, 0x05, 0x06  }, 
		    new byte[] { (byte)0x7C, (byte)0x41, 0x07, 0x08  }
		};
				
		byte[] result = new byte[] 
		{ 
			(byte)0xE1, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06 	
		};
		
		// Prepare & reassemble
		final List<ByteBuffer> in = assemble(data);
		final byte[] out = NALReasemble.defragmentFUANals(in);
		
		assertTrue("broken logic", out != null && out.length > 0);
		assertArrayEquals("bad reassembly", out, result); 
	}
	
	
	@Test
	public void testEmptyFUReassembleWorksStartAndEndOnly()
	{
		byte[][] data = new byte[][] 
		{ 
			new byte[] { (byte)0x7C, (byte)0x81  }, 
		    new byte[] { (byte)0x7C, (byte)0x41  }
		};
				
		byte[] result = new byte[] 
		{ 
			(byte)0x61 	
		};
		
		// Prepare & reassemble
		final List<ByteBuffer> in = assemble(data);
		final byte[] out = NALReasemble.defragmentFUANals(in);
		
		assertTrue("broken logic", out != null && out.length > 0);
		assertArrayEquals("bad reassembly", out, result); 
	}
	
	
	@Test
	public void testSimpleReassembleWorksStartAndEndOnly()
	{
		byte[][] data = new byte[][] 
		{ 
			new byte[] { (byte)0x7C, (byte)0x81, 0x01, 0x02  }, 
		    new byte[] { (byte)0x7C, (byte)0x41, 0x03, 0x04  }
		};
				
		byte[] result = new byte[] 
		{ 
			(byte)0x61, 0x01, 0x02, 0x03, 0x04	
		};
		
		// Prepare & reassemble
		final List<ByteBuffer> in = assemble(data);
		final byte[] out = NALReasemble.defragmentFUANals(in);
		
		assertTrue("broken logic", out != null && out.length > 0);
		assertArrayEquals("bad reassembly", out, result); 
	}
	
	
	@Test
	public void testMultiPacketReassembleWorksStartAndEndOnly()
	{
		byte[][] data = new byte[][] 
		{ 
			new byte[] { (byte)0x7C, (byte)0x81, 0x01, 0x02  }, 
			new byte[] { (byte)0x7C, (byte)0x01, 0x03, 0x04  }, 
			new byte[] { (byte)0x7C, (byte)0x01, 0x05, 0x06  }, 
		    new byte[] { (byte)0x7C, (byte)0x41, 0x07, 0x08  }
		};
				
		byte[] result = new byte[] 
		{ 
			(byte)0x61, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08	
		};
		
		// Prepare & reassemble
		final List<ByteBuffer> in = assemble(data);
		final byte[] out = NALReasemble.defragmentFUANals(in);
		
		assertTrue("broken logic", out != null && out.length > 0);
		assertArrayEquals("bad reassembly", out, result); 
	}
	
	

	private List<ByteBuffer> assemble(final byte[][] data)
	{
		// JAVA8: return  Arrays.asList(data).stream().map(b -> ByteBuffer.wrap(b)).collect(Collectors.toList());
		
		final List<ByteBuffer> out = new ArrayList<ByteBuffer>(data.length);
		for (int i = 0 ; i < data.length ; i++)
		{
			out.add(ByteBuffer.wrap(data[i]));
		}
		
		return out;
	}

	
}
