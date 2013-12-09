DFiler
======

Virtual file system implementation

*******Class breakdown******

Constants
Contains a list of universal constants


DFileID
Represents the id of a DFile


Inode
This class is used to hold metadata of a file. The metadata constitues the file id, file size
and ids of indirect blocks it maps. It represents the top level of 
indirection as it points to indirect blocks that will contain the id of blocks to a file

The amount of space required is calculated dynamically, so if the basic size parameters
are altered, inode will still be able to map a file properly
The inode has a method that maps its buffer and the indirect blocks it points to. 
It contains the ids of the debuffers in its metadata that act as indirect blocks.
To map the blocks, it takes the debuffers corresponding to the indirect blocks and an integer
list of the block ids that actually make the data of the file


DFile
This class holds the basic information of a file. It wraps an inode corresponding to it
and holds its general information. The DFS does not make calls directly to an inode, instead
it calls methods from the DFile and the DFile does the appropriate manipulation on the inode


DBuffer
This class essentially represents a block of memory in the cache. 
It keeps track of the state of the block and makes reads and writes on the disk
It can also be read from and written to


DBufferCache
This class represents the cache of the defiler. It keeps track of the free and used blocks
in disk with the aid of a sorted set to always select the first block available. 
It uses a map to easily find the debuffers in the cache and a queue to determine the LRU
policy.

It also contains methods of manipulation of the free blocks that are generally used by the DFS
Every time the DFS requests a block to the cache, the cache first checks if it has it. It updates
the position of the block in the LRU queue and returns it. If the block is not in the cache 
but is allocated, it tries to fetch it first and then returns it. If the block is free, it
simply updates the free list and returns a new dbuffer. Always making sure the maximum capacity
is not reached and discarding the front of the queue if necessary

DFSImpl

