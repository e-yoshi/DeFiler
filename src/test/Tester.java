package test;

import java.util.Random;
import common.Constants;
import common.DFileID;
import dfs.DFS;
import dfs.DFSImpl;

public class Tester implements Runnable {

	private static final String text = "DeFiler clients can create, destroy, read, and write dfiles.";
	private static DFS dfs = new DFSImpl(false);

	public static void main(String args[]) {
		dfs.init();
		// Testing Create DFile
		testCreateDFile();
		// TestDeleteFile
		testDeleteDFiles();
		// Testing max byte array

		// Testing Read and Write
		testReadWriteText();

		// Test concurrent read write.

	}

	private static void testMaxByteArray() {
		byte[] max = new byte[Constants.MAX_FILE_SIZE];
		Random rand = new Random();
		rand.nextBytes(max);
		DFileID fileID = dfs.createDFile();
		dfs.write(fileID, max, 0, max.length);
		byte[] result = new byte[max.length];
		dfs.read(fileID, result, 0, max.length);
		if (max == result) {
			System.out.println("Test case passed!");
		}
	}

	private static void testReadWriteText() {
		byte[] buffer = text.getBytes();
		DFileID fileID = dfs.createDFile();
		dfs.write(fileID, buffer, 0, buffer.length);
		byte[] result = new byte[buffer.length];
		dfs.read(fileID, result, 0, buffer.length);
		String resultString = new String(result);
		System.out.println(resultString);
	}

	private static void testDeleteDFiles() {
		for (int i = 0; i < 512; i++) {
			dfs.destroyDFile(new DFileID(i));
		}
	}

	private static void testCreateDFile() {
		for (int i = 0; i < 514; i++) {
			DFileID fileId = dfs.createDFile();
			if (fileId == null) {
				System.out.println("Should fail at 512. FileId = " + i);
			} else
				System.out.println("fileId = " + fileId.getDFileID());
		}
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub

	}
}
