package com.jkbff.ao.itemsextractor.rdb.index

import java.io.RandomAccessFile
import com.jkbff.ao.itemsextractor.rdb.RDBFunctions._
import scala.annotation.tailrec

class RDBIndexReader(in: RandomAccessFile) {
	class IndexBlock(val offset: Long, val nextBlock: Long, val previousBlock: Long, val records: Seq[IndexRecord]) {
		
		override def toString() = {
			"offset: " + offset +
			"\nnextBlock: " + nextBlock +
			"\npreviousBlock: " + previousBlock +
			"\nrecords:" + records.foldLeft("")((output, record) => output + "\n  " + record.toString)
		}
	}
	
	lazy val resourceTypeMap = {
		//println("last offset: " + readLittleEndianInt(in))
		//println("data end: " + readLittleEndianInt(in))
		//println("block size: " + readLittleEndianInt(in))
		in.seek(72)
		val dataStart = readLittleEndianInt(in)
		//println("data start: " + dataStart)
		//println
		
		val indexes = readIndexes(in, dataStart)
		val records = indexes.flatMap(index => index.records).groupBy(x => x.resourceType)
		records
	}
	
	@tailrec
	final def readIndexes(in: RandomAccessFile, dataStart: Long, list: List[IndexBlock] = Nil): List[IndexBlock] = {
		if (dataStart == 0) {
			list.reverse
		} else {
			in.seek(dataStart)
			val indexBlock = readIndexBlock(in)
			readIndexes(in, indexBlock.nextBlock, indexBlock :: list)
		}
	}
	
	def readIndexBlock(in: RandomAccessFile): IndexBlock = {
		val offset = in.getFilePointer()
		val nextBlock = readLittleEndianInt(in)
		val prevBlock = readLittleEndianInt(in)

		val count = readLittleEndianShort(in)
		
		// skip block header
		in.skipBytes(18)
		val records = (1 to count).foldLeft(List[IndexRecord]()) { (list, number) =>
			readRecord(in) :: list
		}
		
		new IndexBlock(offset, nextBlock, prevBlock, records.reverse)
	}
	
	def readRecord(in: RandomAccessFile): IndexRecord = {
		val offset = readMiddleEndianLong(in)
		val resourceType = in.readInt()
		val resourceId = in.readInt()
		
		new IndexRecord(resourceType, resourceId, offset)
	}
}