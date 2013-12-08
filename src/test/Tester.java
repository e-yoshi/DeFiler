package test;

import common.DFileID;

import dfs.DFS;
import dfs.DFSImpl;

public class Tester {

	private static DFS dfs = new DFSImpl(true);

	public static void main(String args[]) {
		dfs.init();
		// Testing Create DFile
		

	}
	
	private static void testcreateDFile() {
		for (int i = 0; i < 514; i++) {
			DFileID fileId = dfs.createDFile();
			if (fileId == null) {
				System.out.println("Should fail at 512. FileId = " + i);
			} else
				System.out.println("fileId = " + fileId.getDFileID());
		}
	}
}
