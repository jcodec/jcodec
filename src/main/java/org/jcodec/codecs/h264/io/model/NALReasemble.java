package org.jcodec.codecs.h264.io.model;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

public class NALReasemble 
{

	/** The mask for NAL header type. */
	private static final int FU_TYPE_MASK = 0x1F;

	/** The mask for FU start & end bits. */
	private static final int FU_STARTEND_MASK = 0xC0;
	
	/** The mask for FU start bit. */
	private static final int FU_START_MASK = 0x80;
	
	/** The mask for FU end bit. */
	private static final int FU_END_MASK = 0x40;

	/** The mask for NAL NRI. */
	private static final int NRI_MASK = 0x60;
    
	/** The mask for NAL forbidden bit. */
	private static final int FORBIDDEN_MASK = 0x80;
    
	
	// Important Notes for FU-A/B fragments:
	//
	//  - A FU payload MAY have any number of octets and MAY be empty.  [RFC3984-p30] 
	//
	//  - A fragmented NAL unit MUST NOT be transmitted in one FU; i.e., the
	//       Start bit and End bit MUST NOT both be set to one in the same FU
	//       header.   [RFC3984-p30]
	//
	//  - If a fragmentation unit is lost, the receiver SHOULD discard all
	//       following fragmentation units in transmission order corresponding to
	//       the same fragmented NAL unit.   [RFC3984-p31]
	//
	//  - A receiver in an endpoint or in a MANE MAY aggregate the first n-1
	//       fragments of a NAL unit to an (incomplete) NAL unit, even if fragment
	//       n of that NAL unit is not received.  In this case, the
	//       forbidden_zero_bit of the NAL unit MUST be set to one to indicate a
	//       syntax violation.   [RFC3984-p31]
	
	
	
	
	
	/**
	 * Defragment FU-A NALs into a single NAL.
	 * 
	 * This method assumes the following are true:
	 * 
	 *   1. The NALs presented in the list are CONTIGUOUS - i.e.
	 *      any FU packets received have been reordered based on
	 *      RTP sequence number AND any dropped packets been detected
	 *      and the rest of the fragment NALs have NOT been sent on this
	 *      list - i.e. there is no END packet.
	 *      e.g.
	 *       RTP:seq1  RTP:seq3  RTP:seq2 RTP:seq4  RTP:seq5  ---> { [FU-A-S], [FU-A:1], [FU-A:2] } 
	 *       [FU-A-S]  [FU-A:2]  [FU-A:1]    X      [FU-A-E]
	 *      
	 *      NB: If an issue is detected, the F bit will be set to 1 to indicate
	 *          to the decoder that this NAL is suspicious. 
	 *          
	 *   2. The list of buffers passed into this method are ONLY related to the
	 *      same Fragmentation Unit, i.e. ALL buffers will be used to build
	 *      the resulting NAL.       
	 *      
	 *   3. Each buffer in the list is at position 0, with limit set the the 
	 *      length of the data that should be extracted - i.e. payload length.   
	 * 
	 * For FU-A, the following 2 bytes are seen in the payload of each NAL:
	 * 
	 *   |  FU indicator |   FU header   |
	 *   +---------------+---------------+
	 *   |0|1|2|3|4|5|6|7|0|1|2|3|4|5|6|7|
	 *   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *   |F|NRI|  TypeX  |S|E|R|  TypeY  |
	 *   +---------------+---------------+
	 *   
	 * The defragmented NAL takes it's NAL header data from the first FU-A payload 
	 * as follows:  
	 * 
	 *   |  NAL header   |
	 *   +---------------+
	 *   |0|1|2|3|4|5|6|7|
	 *   +-+-+-+-+-+-+-+-+
	 *   |F|NRI|  TypeY  |   NB: F bit is SET if no end marker OR 
	 *   +---------------+       start and end marker seen in a packet.  
	 *   
	 *  NB: If NO start element is seen, this method will return byte[0]
	 *      THE RTP interface layer should have stripped missing data as per note 1
	 *      already. In this case, the data would never be sent to this method, as
	 *      a missing FU-A-START would mean dropping all fragments, same as passing
	 *      empty list.     
	 * 
	 * @param nals The list of ByteBuffer-based NALs that meet the requirements above.
	 * @return The (optional) byte[] represented the defragmented NAL - NB: data will be empty
	 *         if the defragmentation failed due to bad data, BUT will be present if some data
	 *         could be decoded but was not 'perfect' - in this instance the forbidden bit is set.
	 */
	public static byte[] defragmentFUANals(final List<ByteBuffer> nals)
	{   // JAVA 8 - Optional<byte[]>
		
		// Require a valid list of NALs to work on.
		if (nals == null || nals.isEmpty())
		{
			return new byte[0];
		}
		
		// Peek at the first two bytes of the first NAL.
		final ByteBuffer headNAL = nals.get(0);
		final NALUnit nalUnit = NALUnit.read(headNAL);
		headNAL.rewind();
		
		final byte headFuIndicator = headNAL.get();
		final byte headFuHeader = headNAL.get();
		headNAL.rewind();
		
		// Ensure that the first NAL is an FU-A-START ONLY (i.e. not STARt, or START & END are not allowed)
		if (NALUnitType.FU_A != nalUnit.type || 
		    (headFuHeader & FU_START_MASK) == 0 || (headFuHeader & FU_STARTEND_MASK) == FU_STARTEND_MASK)
		{
			return new byte[0];
		}
		
		// The required data byte[] is the sum of (1 + (each buffer remaining - 2))
		// i.e. add 1 byte for NAL header, then add each FU length - FU Indicator and FU Header.
		// JAVA 8 -> byte[] data = new byte[1 + nals.stream().flatMapToInt(n -> IntStream.of(n.remaining() - 2)).sum()];
		int datasize = 1;
		for (int i = 0 ; i < nals.size() ; i++) 
		{
			datasize += nals.get(i).remaining() - 2;
		}
		
		byte[] data = new byte[datasize];
		final ByteBuffer out = ByteBuffer.wrap(data);

		// Set the NAL header based on parts from the first FU indicator and header.
		out.put((byte)((headFuIndicator & NRI_MASK) | (headFuHeader & FU_TYPE_MASK)));
		
		int validUntilPosition = -1;
		boolean shouldSetFbit = false;
		for (int i = 0 ; i < nals.size() ; i++)
		{
			final ByteBuffer nal = nals.get(i);
			nal.get(); // Skip first byte.
			final byte fuHeader = nal.get();
			
			// Detect any errors in the ordering.
			if ( (i > 0 && ((fuHeader & FU_START_MASK) == FU_START_MASK)) ||
			     (i == nals.size() - 1 && ((fuHeader & FU_END_MASK) == 0)) )
			{
				// Found a start indicator AFTER first entry OR
				// DID NOT find end indicator at last entry.
				// NOT IDEAL, but carry on with defragment.
				shouldSetFbit = true;
			}
			
			if (nal.hasRemaining())
			{
				final byte[] buffer = new byte[nal.remaining()];
				nal.get(buffer);
				out.put(buffer);
			}
			
			if ( (i != nals.size() - 1 && ((fuHeader & FU_END_MASK) == FU_END_MASK)) )
			{
				// Found end indicator NOT at last entry.
				// Need to STOP defragmenting.
				shouldSetFbit = true;
				validUntilPosition = out.position();
				break;
			}
		}
		
		// If there was an issue with the data, set the F bit - this packet is probably bad.
		//   i.e. no FU-END, or FU-END seen but more packets in the list.
		if (shouldSetFbit) 
		{
			if (validUntilPosition > -1)
			{
				// If this is a special case for truncating the data, do that first.
				data = Arrays.copyOf(data, data.length - (data.length - validUntilPosition));
			}
			
			data[0] = (byte)(FORBIDDEN_MASK | data[0]);
		}
		
		return data;
	}
	
	
	/**
	 * Private constructor to prevent general instance creation.
	 */
	private NALReasemble()
	{
		// Do nothing.
	}
	
}
