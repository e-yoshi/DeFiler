package dblockcache;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.TreeMap;
import virtualdisk.VirtualDisk;
import common.Constants;

public class DBufferCache {
	
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
	private Queue<Integer> _replacementBlocks;
	
	/**
	 * Constructor: allocates a cacheSize number of cache blocks, each
	 * containing BLOCK-size bytes data, in memory
	 */
	public DBufferCache(int cacheSize, VirtualDisk disk) {
		_replacementBlocks = new ArrayDeque<>();
		_freeBlocksInDisk = new PriorityQueue<>();
		_blocksInCache = new TreeMap<>();
		
		_disk = disk;
		initializeCache();
		Thread diskThread = new Thread(_disk);
		diskThread.start();		
	}
	
	private void initializeCache() {
	    
	    int inodesInBlock = (int) Math.ceil(Constants.BLOCK_SIZE/Constants.INODE_SIZE);
	    inodeRegionSize = (int) Math.ceil(Constants.MAX_DFILES/inodesInBlock);
	    
	    //Skip block zero and inode region
	    for (int i = inodeRegionSize + 1; i < Constants.NUM_OF_BLOCKS; i++) {
	        _freeBlocksInDisk.add(i);
	    }
	    
	    //Initialize inode region blocks and put them in cache, skip block 0
	    for (int i = 1; i <= inodeRegionSize; i++) {
	        _blocksInCache.put(i, new DBuffer(_disk, i));
	    }
	}
	
	/**
	 * Get buffer for block specified by blockID. The buffer is "held" until the
	 * caller releases it. A "held" buffer cannot be evicted: its block ID
	 * cannot change.
	 *
	 */
	public DBuffer getBlock(int blockID) {
	    if(_blocksInCache.containsKey(blockID)) {
	        updateLRUBlock(blockID);
	        return _blocksInCache.get(blockID);
	    }
	    
	    checkLRULatency();
	    DBuffer buffer = null;
	    buffer = new DBuffer(_disk, blockID);
	    
	    if (!_freeBlocksInDisk.contains(blockID))
	        buffer.startFetch();
	    else
	        _freeBlocksInDisk.remove(blockID);
	    
	    _blocksInCache.put(blockID, buffer);
            _replacementBlocks.add(blockID);
            
            return buffer;
	}
	
	/**
	 * Creates space in cache according to LRU policy in case cache is full
	 */
	public void checkLRULatency() {
	    sync();
	    while (_blocksInCache.size() >= Constants.NUM_OF_CACHE_BLOCKS) {
	        Integer blockID = _replacementBlocks.poll();
	        _blocksInCache.remove(blockID);
	    }
	}
	
	/**
	 * If the block is in the cache, move it to the end of the queue
	 * @param blockID
	 */
	public void updateLRUBlock(int blockID) {
	    if(blockID <= inodeRegionSize) return;
	    if (_replacementBlocks.contains(blockID)) {
	        _replacementBlocks.remove(blockID);
	        _replacementBlocks.add(blockID);
	    }
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
	    for (Integer i : _blocksInCache.keySet()) {
	        DBuffer buffer = _blocksInCache.get(i);
	        buffer.startPush();
	    }
	}
	
	/**
	 * Ceases execution of the disk
	 */
	public void terminate() {
	    _disk.terminate();
	}
	
	/**
	 * 
	 * @return number of Blocks from the inode region
	 */
	public int getInodeRegionSize() {
	    return inodeRegionSize;
	}
	
	public 
}
