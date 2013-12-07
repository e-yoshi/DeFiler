package dblockcache;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import virtualdisk.IVirtualDisk;
import virtualdisk.VirtualDisk;
import common.Constants;

public class DBufferCache {
	
	private int _cacheSize;
	
	private VirtualDisk _disk;
	
	private int inodeRegionSize;
	
	/**
	 * Use of a priority queue to mark the free blocks from the disk
	 */
	private PriorityQueue<Integer> _freeBlocksInDisk;
	
	/**
	 * Use a map to get fast acces to the dBuffers in the cache
	 */
	private Map<Integer, DBuffer> _blocksInCache;
	
	/**
	 * Use a queue to approximate LRU replacement algorithm
	 * If a block is reused, it is added back to the end of the queue
	 */
	private Queue<Integer> _replacementBuffers;
	
	/**
	 * Constructor: allocates a cacheSize number of cache blocks, each
	 * containing BLOCK-size bytes data, in memory
	 */
	public DBufferCache(int cacheSize, VirtualDisk disk) {
		_cacheSize = cacheSize * Constants.BLOCK_SIZE;
		_replacementBuffers = new ArrayDeque<Integer>();
		
		_disk = disk;
		Thread diskThread = new Thread(_disk);
		diskThread.start();		
	}
	
	private void initializeCache() {
	    _freeBlocksInDisk = new PriorityQueue<>();
	    int inodesInBlock = (int) Math.ceil(Constants.BLOCK_SIZE/Constants.INODE_SIZE);
	    inodeRegionSize = (int) Math.ceil(Constants.MAX_DFILES/inodesInBlock);
	    
	    //Skip block zero and inode region
	    for (int i = inodeRegionSize; i < Constants.NUM_OF_BLOCKS; i++) {
	        
	    }
	}
	
	/**
	 * Get buffer for block specified by blockID. The buffer is "held" until the
	 * caller releases it. A "held" buffer cannot be evicted: its block ID
	 * cannot change.
	 *
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
	
	/**
	 * Ceases execution of the disk
	 */
	public void terminate() {
	    _disk.terminate();
	}
}
