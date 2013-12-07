package dfs;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import common.Constants;
import common.DFileID;
import dblockcache.DBuffer;

/**
 * This class is used to hold metadata of a file. It represents the top level of 
 * indirection as it points to indirect blocks that will contain the id of blocks to a file
 * The amount of space required is calculate dynamically, so if the basic size parameters
 * are altered, inode will still be able to map a file properly
 * 
 * @author henriquemoraes
 *
 */
public class Inode {
    private DFileID _FID;
    private int _fileSize;
    private byte[] _buffer;
    private boolean _isMapped;
    
    private int _numOfIndirectBlocks;
    
    Inode(DFileID fileID, int size) {
        _isMapped = false;
        _FID = fileID;
        _fileSize = size;
        _buffer = new byte[Constants.INODE_SIZE];

        byte[] someMetadata = writeInts(_FID.getDFileID(), _fileSize);
        System.arraycopy(someMetadata.length, 0, _buffer, 0, someMetadata.length);
        
        //Assuming size is given in bytes instead of blocks
        int numOfBlocks = (int) Math.ceil(_fileSize/Constants.BLOCK_SIZE);
        _numOfIndirectBlocks = (int) Math.ceil(numOfBlocks/Constants.INTS_IN_BLOCK);
    }
    
    /**
     * Populates indirect blocks with block ids that constitute a file
     * Note, this method assumes the whole file is mapped in one call
     * It also automatically indexes indirect blocks
     * @param indirectBlocks the buffers corresponding to this inode's that point to 
     * indirect blocks
     * @param blocksInFile list of blocks that contain file data that should be mapped
     * by this inode
     * @return true upon a successful mapping
     */
    public boolean mapFile(List<DBuffer> indirectBlocks, List<Integer> blocksInFile) {
        if (indirectBlocks.size() != _numOfIndirectBlocks) {
            System.out.println("List of indirect blocks does not match the size from this inode");
            return false;
        }
        else if ((double) blocksInFile.size() / Constants.INTS_IN_BLOCK > indirectBlocks.size()) {
            System.out.println("Blocks to write is greater than what indirect blocks can map");
            return false;
        }
        
        List<Integer> indirectBlocksIDs = new ArrayList<>(); 
        int subListStart = 0;
        int listSize = blocksInFile.size();
        
        for (DBuffer buf : indirectBlocks) {      
            indirectBlocksIDs.add(buf.getBlockID());
            byte[] result = null;
            
            if (listSize - subListStart > Constants.INTS_IN_BLOCK){
                result = writeInts(blocksInFile.subList(subListStart, subListStart + Constants.INTS_IN_BLOCK));
            }
            else {
                result = writeInts(blocksInFile.subList(subListStart, listSize - subListStart));
            }
                 
            buf.write(result, 0, result.length);
            subListStart += result.length;
        }
        
        byte[] moreMetadata = writeInts(indirectBlocksIDs);
        System.arraycopy(moreMetadata, 0, _buffer, Constants.POSITION_INDIRECT_BLOCK_REGION, moreMetadata.length);
        
        _isMapped = true; 
        return true;
    }
    
    /**
     * Creates a byte array with the given integers
     * @param ints
     * @return
     */
    private byte[] writeInts(int...ints) {
        List<Integer> list = new ArrayList<>();
        for(Integer i : ints) {
            list.add(i);
        }
        return writeInts(list);
    }
    
    /**
     * Creates a byte array with the given list of integers
     * @param ints
     * @return
     */
    private byte[] writeInts(List<Integer> ints) {
        byte[] result = new byte[Constants.BLOCK_SIZE];
        try{
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            for (int i : ints) {
                dos.writeInt(i);
            }
            dos.close();
            // Create a new byte array of the correct size
            ByteBuffer bb = ByteBuffer.wrap(result);
            bb.put(bos.toByteArray());
            return bb.array();
        } 
        catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * 
     * @return _buffer containing information of this inode
     */
    public byte[] getMetadata() {
        return _buffer;
    }
    
    /**
     * 
     * @return the number of indirect blocks this inode maps to
     */
    public int getNumberOfIndirectBlocks() {
        return _numOfIndirectBlocks;
    }
    
    /**
     * Determines whether this inode has already mapped a file
     */
    public boolean isMapped() {
       return _isMapped;
    }
}
