package net.sf.l2j.gameserver.geoengine.geodata;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import net.sf.l2j.gameserver.enums.GeoType;

public class BlockFlat extends ABlock
{
	protected final short _height;
	protected byte _nswe;
	
	/**
	 * Creates FlatBlock.
	 * @param bb : Input byte buffer.
	 * @param type : The type of loaded geodata.
	 */
	public BlockFlat(ByteBuffer bb, GeoType type)
	{
		// Get height and nswe.
		_height = bb.getShort();
//		_nswe = GeoStructure.TYPE_COMPLEX_L2D;
		
		_nswe = type != GeoType.L2D ? 0x0F : (byte) (0xFF);
		
		// Read dummy data.
		if (type == GeoType.L2OFF)
		{
			bb.getShort();
		}
	}
	
	@Override
	public final boolean hasGeoPos()
	{
		return true;
	}
	
	@Override
	public final short getHeightNearest(int geoX, int geoY, int worldZ, IGeoObject ignore)
	{
		return _height;
	}
	
	@Override
	public final byte getNsweNearest(int geoX, int geoY, int worldZ, IGeoObject ignore)
	{
		return _nswe;
	}
	
	@Override
	public final int getIndexNearest(int geoX, int geoY, int worldZ, IGeoObject ignore)
	{
		return 0;
	}
	
	@Override
	public final int getIndexAbove(int geoX, int geoY, int worldZ, IGeoObject ignore)
	{
		// Check height and return index.
		return _height > worldZ ? 0 : -1;
	}
	
	@Override
	public final int getIndexBelow(int geoX, int geoY, int worldZ, IGeoObject ignore)
	{
		// Check height and return index.
		return _height < worldZ ? 0 : -1;
	}
	
	@Override
	public final short getHeight(int index, IGeoObject ignore)
	{
		return _height;
	}
	
	@Override
	public final byte getNswe(int index, IGeoObject ignore)
	{
		return _nswe;
	}
	
	@Override
	public final void saveBlock(BufferedOutputStream stream) throws IOException
	{
		// write block type
		stream.write(GeoStructure.TYPE_FLAT_L2D);
		
		// write height
		stream.write((byte) (_height & 0x00FF));
		stream.write((byte) (_height >> 8));
	}
}