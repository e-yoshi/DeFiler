package virtualdisk;

/**
 * VirtualDisk.java
 *
 * A virtual asynchronous disk.
 *
 */

import java.io.RandomAccessFile;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Queue;

import common.Constants;
import common.Constants.DiskOperationType;
import dblockcache.DBuffer;

/**
 * This class represents the lowest level in the dfs hierarchy. it is accessed by
 * the dbuffers to fetch or pull data from or to memory. Once a buffer starts a request
 * to the disk, the disk places the buffer in a queue which is processed on another thread
 * Once this thread has an empty queue, it will wait until a new dbuffer comes. Also, it contains
 * a wrapper class called request, that contains the DBuffer and the operation type 
 * required for processing. Once a DBuffer is popped out of the queue, it makes a read from 
 * or write to a random access file depending on the operation type that should be done
 * 
 * @author elderyoshida
 *
 */
public class VirtualDisk implements IVirtualDisk, Runnable {

	private String _volName;
	private RandomAccessFile _file;
	private int _maxVolSize;
	private Queue<Request> _queue;
	private boolean _running;

	/**
	 * VirtualDisk Constructors
	 */
	public VirtualDisk(String volName, boolean format) throws FileNotFoundException, IOException {

		_volName = volName;
		_maxVolSize = Constants.BLOCK_SIZE * Constants.NUM_OF_BLOCKS;

		/*
		 * mode: rws => Open for reading and writing, as with "rw", and also
		 * require that every update to the file's content or metadata be
		 * written synchronously to the underlying storage device.
		 */
		_file = new RandomAccessFile(_volName, "rws");

		/*
		 * Set the length of the file to be NUM_OF_BLOCKS with each block of
		 * size BLOCK_SIZE. setLength internally invokes ftruncate(2) syscall to
		 * set the length.
		 */
		_file.setLength(Constants.BLOCK_SIZE * Constants.NUM_OF_BLOCKS);
		if (format) {
			formatStore();
		}
		/* Other methods as required */

		/* Initialize the request queue */
		_queue = new ArrayDeque<Request>();
		_running = true;
	}

	public VirtualDisk(boolean format) throws FileNotFoundException, IOException {
		this(Constants.vdiskName, format);
	}

	public VirtualDisk() throws FileNotFoundException, IOException {
		this(Constants.vdiskName, false);
	}

	/**
	 * Start an asynchronous request to the underlying device/disk/volume. --
	 * buf is an DBuffer object that needs to be read/write from/to the volume.
	 * -- operation is either READ or WRITE
	 */
	public void startRequest(DBuffer buf, DiskOperationType operation) throws IllegalArgumentException, IOException {
		synchronized (_queue) {
			while (!_queue.offer(new Request(buf, operation))) {
				continue;
			}
			_queue.notifyAll();
		}
	}

	/**
	 * Clear the contents of the disk by writing 0s to it
	 */
	private void formatStore() {
		byte b[] = new byte[Constants.BLOCK_SIZE];
		setBuffer((byte) 0, b, Constants.BLOCK_SIZE);
		for (int i = 0; i < Constants.NUM_OF_BLOCKS; i++) {
			try {
				int seekLen = i * Constants.BLOCK_SIZE;
				_file.seek(seekLen);
				_file.write(b, 0, Constants.BLOCK_SIZE);
			} catch (Exception e) {
				System.out.println("Error in format: WRITE operation failed at the device block " + i);
			}
		}
	}

	/***
	 * helper function: setBuffer
	 */
	private static void setBuffer(byte value, byte b[], int bufSize) {
		for (int i = 0; i < bufSize; i++) {
			b[i] = value;
		}
	}

	/***
	 * Reads the buffer associated with DBuffer to the underlying
	 * device/disk/volume
	 */
	private int readBlock(DBuffer buf) throws IOException {
		int seekLen = buf.getBlockID() * Constants.BLOCK_SIZE;
		/** Boundary check */
		if (_maxVolSize < seekLen + Constants.BLOCK_SIZE) {
			return -1;
		}
		_file.seek(seekLen);
		return _file.read(buf.getBuffer(), 0, Constants.BLOCK_SIZE);
	}

	/***
	 * Writes the buffer associated with DBuffer to the underlying
	 * device/disk/volume
	 */
	private void writeBlock(DBuffer buf) throws IOException {
		int seekLen = buf.getBlockID() * Constants.BLOCK_SIZE;
		_file.seek(seekLen);
		_file.write(buf.getBuffer(), 0, Constants.BLOCK_SIZE);
	}

	public void terminate() {
		_running = false;
	}

	@Override
	public void run() {
		while (_running) {
			synchronized (_queue) {
				while (_queue.isEmpty()) {
					try {
						_queue.wait();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				Request request = _queue.poll();
				if (request == null)
					return;
				try {
					if (request.getOperation() == DiskOperationType.READ) {
						readBlock(request.getDBuffer());

					} else {
						writeBlock(request.getDBuffer());
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} finally {
					request.getDBuffer().ioComplete();
				}

			}
		}
	}

	public class Request {

		private DBuffer _buf = null;
		private DiskOperationType _op = null;

		public Request(DBuffer buf, DiskOperationType operation) {
			_buf = buf;
			_op = operation;
		}

		public DBuffer getDBuffer() {
			return _buf;
		}

		public DiskOperationType getOperation() {
			return _op;
		}
	}
}
