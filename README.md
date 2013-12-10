DFiler

Henrique Moraes (hrm8)
Elder Yoshida (emy2)

Time spent on project: 18 hours

======

Virtual file system implementation

*******Class breakdown******

Constants
Contains a list of universal constants

===========================================

DFileID
Represents the id of a DFile

===========================================

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

===========================================

DFile
This class holds the basic information of a file. It wraps an inode corresponding to it
and holds its general information. The DFS does not make calls directly to an inode, instead
it calls methods from the DFile and the DFile does the appropriate manipulation on the inode

===========================================

DBuffer
This class essentially represents a block of memory in the cache. 
It keeps track of the state of the block and makes reads and writes on the disk
It can also be read from and written to

===========================================

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

===========================================

DFSImpl

This class is the entry point of the program and the main component that manages the 
files. It can be initiated with a format parameter that specifies if the disk should be 
formatted or not. 
If so, the disk attempts to zero the data from every block, if not, it will scan the 
inode region and load the existent data.

This class also structures the general structure of the disk.
Each file has one iNode. Each iNode contains in the following order:

|- Inode -|
|fileId
|fileSize
|indirectBlock 1
|indirectBlock 2 
...

An indirectBlock is a block that contains other blockIds as its data. They serve as a table for mapping a file.
Thus, we have a two-level structure. This allows for mapping files that are large. Although having a lot of space in an inode, we just need to use two indirect blocks to map a file of maximmum size. More metadata could be stored in the inode.


In order to use the DFS, the following methods are going to be called:
1. init()
2. createDFile()
3. read()
4. write()
5. sizeDFile()
6. listAllDFiles()
7. sync()

The details and implementation of the methods above are going to be discussed below.

1.init()
When initializing the DFS, the program uses a singleton pattern and creates the cache if it exists. 
Depending on the boolean variable, it will create a formatted or load a previously created file.
Then it proceeds to check the consistency of the file.
In order to do that, it 1-Reads Inode region, 2-creates a file map cache, 3-checks consistency of each file.
-> This DFS checks consistency for uniquely referenced blocks (No data block is part of two file).
-> Checks if the fileId has a valid numeber
-> Checks if the blockIds in the indirect blocks are a valid number
-> Checks what are the used blocks in the file.

The create file method simply creates a file and assings an id to it.
Destroy file erases the data in the file blocks and in its inode. It overwrites
the metadata and frees the indirect and direct blocks of the file.
 
readFile retrieves the file with the correct id and locks it for reading,
gets the mapped block ids from the file, and then loops for the appropriate amount
of iterations to read from the debuff requested to the cache. Finally, it releases the lock
  
write file gets the file and the lock, and first calculates the discrepancy between write size and
file size. It allocates or deallocates the required number of blocks, and initializes and maps 
the metadata in case it is the first write to the file. Finally, it feteches the appropriate blocks 
from the cache and overwrite them as necessary

getMappedBlockIDs is an interesting method to retrieve the ids of the data blocks in the file. 
Interestingly, we could have used a list contained in the object file to return the ids of the data
If it was implemented this way, we would be able to quickly access our data with a minimum number of 
lines of code. 
But we chose to access the data in the indirect blocks mapped by the inode, to scan
each of them, to painfully devise an algorithm to extract bytes and convert them to ints, and to 
retrieve the list of mapped block ids. Now, several lines of code are iterated multiple times hindering
the performance that could let the file manager using our code to have a good night of sleep.
But we chose it this way to not defy the purposes of the assignment

Synchronization

The synchronization that protects the files is done using a convinient lock provided in the concurrent java package.
It is a ReentrantReadWriteLock, allowing for better control of starvation scenario.


===========================================

VirtualDisk
This class represents the lowest level in the dfs hierarchy. it is accessed by
the dbuffers to fetch or pull data from or to memory. Once a buffer starts a request
to the disk, the disk places the buffer in a queue which is processed on another thread
Once this thread has an empty queue, it will wait until a new dbuffer comes. Also, it contains
a wrapper class called request, that contains the DBuffer and the operation type 
required for processing. Once a DBuffer is popped out of the queue, it makes a read from 
or write to a random access file depending on the operation type that should be done.

The virtual disk synchronization uses a queue of operations and the disk is itself a thread.
While there is an object inside the queue, it will execute the operations.

===========================================
Testing

Testing was done focusing on firstly getting every core method right.
The first test creates multiple files then deletes all them then we check if there are any remaining files.
The sencond test tests if text can be saved to the disk.
Then we porceeded to stress test the program, creating multiple concurrent reads and writes.
In order to test concurrency, we modified the code so that all writes woudl never relese the lock. That being done, we figured that all reads were stopped, confiming the success of the multithreading.

Thank you!

