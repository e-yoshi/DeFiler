package dblockcache;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import virtualdisk.IVirtualDisk;
import virtualdisk.VirtualDisk;
import common.Constants;

public class DBufferCache {
	
	private int _cacheSize;
	private List<DBuffer> _bufferList;
	private VirtualDisk _disk;
	
	/**
	 * Constructor: allocates a cacheSize number of cache blocks, each
	 * containing BLOCK-size bytes data, in memory
	 */
	public DBufferCache(int cacheSize, VirtualDisk disk) {
		_cacheSize = cacheSize * Constants.BLOCK_SIZE;
		_bufferList = new ArrayList<>();
		
		_disk = disk;
		Thread diskThread = new Thread(_disk);
		diskThread.start();
		
	}
	
	/**
	 * Get buffer for block specified by blockID. The buffer is "held" until the
	 * caller releases it. A "held" buffer cannot be evicted: its block ID
	 * cannot change.
	 */
	public DBuffer getBlock(int blockID) {
	    
	}

	/**
	 *  Release the buffer so that others waiting on it can use it 
	 */
	public void releaseBlock(DBuffer buf) {
	    buf.setBusy(false);
	}
	
	/**
	 * sync() writes back all dirty blocks to the volume and wait for completion.
	 * The sync() method should maintain clean block copies in DBufferCache.
	 */
	public void sync() {
	    for (DBuffer d : _bufferList)
	        if(!d.checkClean()) { 
	            try {
	                _disk.startRequest(d, Constants.DiskOperationType.WRITE);
	            }
	            catch (IllegalArgumentException | IOException e) {
	                e.printStackTrace();
	            }
	        }
	}
	
	public void terminate() {
	    
	}
}
