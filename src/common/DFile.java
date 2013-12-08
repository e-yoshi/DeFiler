package common;

import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DFile {
	private ReadWriteLock _lock = new ReentrantReadWriteLock();
	private int _file = -1;
	private List<Integer> _indirectBlocks = null;
	private List<Integer> _dataBlocks = null;
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

	public void setIndirectBlocks(List<Integer> indirectBlocks) {
		this._indirectBlocks = indirectBlocks;
	}

	public int getSize() {
		return _size;
	}

	public void setSize(int size) {
		this._size = size;
	}

	public int getINodeBlock() {
		return _iNodeBlock;
	}

	public void setINodeBlock(int iNodeBlock) {
		this._iNodeBlock = iNodeBlock;
	}

	public int getINodePosition() {
		return _iNodePosition;
	}

	public void setINodePosition(int iNodePosition) {
		this._iNodePosition = iNodePosition;
	}

	public List<Integer> getDataBlocks() {
		return _dataBlocks;
	}

	public void setDataBlocks(List<Integer> dataBlocks) {
		this._dataBlocks = dataBlocks;
	}
}
