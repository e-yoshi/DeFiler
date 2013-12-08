package common;

import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DFile {
	private ReadWriteLock _lock = new ReentrantReadWriteLock();
	private int _file = -1;
	private List<Integer> _indirectBlocks = null;
	private int _size = -1;

	public DFile(int fileId, int size) {
		_file = fileId;
		setSize(size);
	}

	public DFile(int fileId, int size, List<Integer> indirectBlocks) {
		_file = fileId;
		setSize(size);
		_indirectBlocks = indirectBlocks;
	}

	public ReadWriteLock getLock() {
		return _lock;
	}

	public Integer getFileId() {
		return _file;
	}

	public List<Integer> getIndirectBlocks() {
		return _indirectBlocks;
	}

	public void setIndirectBlocks(List<Integer> _indirectBlocks) {
		this._indirectBlocks = _indirectBlocks;
	}

	public int getSize() {
		return _size;
	}

	public void setSize(int _size) {
		this._size = _size;
	}
}
