package common;

import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DFile {
	private ReadWriteLock _lock = new ReentrantReadWriteLock();
	private int _file = -1;
	private List<Integer> _indirectBlocks = null;
	private int _size = -1;
	private int _iNodeBlock = -1;
	private int _iNodePosition = -1;

	public DFile(int fileId, int size) {
		_file = fileId;
		setSize(size);
	}

	public DFile(int fileId, int size, List<Integer> indirectBlocks, int iNodeBlock, int iNodePosition) {
		_file = fileId;
		_size = size;
		_indirectBlocks = indirectBlocks;
		setINodeBlock(iNodeBlock);
		setINodePosition(iNodePosition);
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

	public int getINodeBlock() {
		return _iNodeBlock;
	}

	public void setINodeBlock(int _iNodeBlock) {
		this._iNodeBlock = _iNodeBlock;
	}

	public int getINodePosition() {
		return _iNodePosition;
	}

	public void setINodePosition(int _iNodePosition) {
		this._iNodePosition = _iNodePosition;
	}
}
