package dblockcache;

import java.io.IOException;
import common.Constants;
import common.Constants.DiskOperationType;
import virtualdisk.IVirtualDisk;

public class DBuffer {
	private boolean _isClean;
	private byte[] _dBuffer;
	private boolean _isValid;
	private boolean _isBusy;
	private int _blockID;
	
	private IVirtualDisk _disk;
    
    
	/**
	 *  Start an asynchronous fetch of associated block from the volume 
	 **/
	public void startFetch() {
	    _isValid = false;
	    _isBusy = true;

	    try {
	        _disk.startRequest(this, DiskOperationType.READ);
	    }
	    catch (IllegalArgumentException | IOException e) {
	        System.out.println("Culpa do Elder!");
	        e.printStackTrace();
	    }
	}
	
	/** 
	 * Start an asynchronous write of buffer contents to block on volume 
	 * 
	 **/
	public void startPush() {
	    if (_isClean) return;
	    _isBusy = true;
	    
	    try {
	        _disk.startRequest(this, DiskOperationType.WRITE);
	    }
	    catch (IllegalArgumentException | IOException e) {
	        e.printStackTrace();
	    } 
	}

	/** 
	 * Check whether the buffer has valid data 
	 **/ 
	public boolean checkValid() {
	    return _isValid;
	}
	
	/**
	 *  Wait until the buffer has valid data, i.e., wait for fetch to complete 
	 *  */
	public synchronized boolean waitValid() {
	    while (!_isValid) {
	        try {
	            wait();
	        }
	        catch (InterruptedException e) {
	            e.printStackTrace();
	        }
	    }
	    return true;
	}
	
	/**
	 *  Check whether the buffer is dirty, i.e., has modified data written back to disk? 
	 *  */
	public boolean checkClean() {
	    return _isClean;
	}
	
	/**
	 *  Wait until the buffer is clean, i.e., wait until a push operation completes 
	 *  */
	public synchronized boolean waitClean() {
	    while (!_isClean) {
	        try {
	            wait();
	        }
	        catch (InterruptedException e) {
	            e.printStackTrace();
	        }
	    }
	    
	    return true;
	}
	
	/**
	 *  Check if buffer is evictable: not evictable if I/O in progress, or buffer is held 
	 *  */
	public boolean isBusy() {
	    return _isBusy;
	}

	/**
	 * reads into the buffer[] array from the contents of the DBuffer. Check
	 * first that the DBuffer has a valid copy of the data! startOffset and
	 * count are for the buffer array, not the DBuffer. Upon an error, it should
	 * return -1, otherwise return number of bytes read.
	 * 
	 * If it hits the end of the file, it returns the number of bytes read up
	 * to that point
	 */
	public int read(byte[] buffer, int startOffset, int count) {
	    
	    if(startOffset + count > buffer.length || startOffset < 0)
                return Constants.DBUFFER_ERROR;
            
	    // Read from the whole dBuffer
	    if(count > _dBuffer.length)
	        count = _dBuffer.length;
            
            // read into dBuff
            for (int i = 0; i < count; i++) {
               
                if(_dBuffer[i] == Constants.EOF) 
                    return i;
                
                buffer[i + startOffset] = _dBuffer[i];
            }
              
            notifyAll();
            return count;
	}

	/**
	 * writes into the DBuffer from the contents of buffer[] array. startOffset
	 * and count are for the buffer array, not the DBuffer. Mark buffer dirty!
	 * Upon an error, it should return -1, otherwise return number of bytes
	 * written.
	 * 
	 * If count is greater than a block size, only BLOCK_SIZE bytes will be written 
	 */
	public synchronized int write(byte[] buffer, int startOffset, int count) {
	    
	    if(startOffset + count > buffer.length || startOffset < 0)
	        return Constants.DBUFFER_ERROR;
	    
	    // Write on the whole dBuffer
	    if(count > _dBuffer.length)
	        count = _dBuffer.length;
	    
	    // Passed tests, mark dBuff as dirty
            _isClean = false;
	    
	    // write into dBuff
	    for (int i = 0; i < count; i++) 
	        _dBuffer[i] = buffer[i + startOffset];
	    
	    // To prevent wrong readings, add EOF
	    if (count < _dBuffer.length)
	        _dBuffer[count] = Constants.EOF;
	   
	    
	    notifyAll();
	    return count;
	}
	
	/**
	 *  An upcall from VirtualDisk layer to inform the completion of an IO operation 
	 *  */
	public void ioComplete() {
	    _isBusy = false;
	    _isValid = true;
	    
	    //Wake threads waiting on this dBuffer's state
	    notifyAll();
	}
	
	/**
	 *  An upcall from VirtualDisk layer to fetch the blockID associated with a startRequest operation 
	 *  */
	public int getBlockID() {
	    return _blockID;
	}
	
	/**
	 *  An upcall from VirtualDisk layer to fetch the buffer associated with DBuffer object
	 *  */
	public byte[] getBuffer() {
	    return _dBuffer;
	}
}
