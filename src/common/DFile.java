package common;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DFile {
	private ReadWriteLock lock = new ReentrantReadWriteLock();
	public DFile(DFileID file) {
		
	}
}
