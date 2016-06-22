package com.huang.nio;

import java.nio.ByteBuffer;
//capacicty:作为一个内存块，buffer有一个固定的大小值，也叫“capacity”
//你只能往里写capacity个byte、long、char等类型

//position
//当你写数据到buffer中时，position表示当前的位置。初始的position值为0.
//当一个byte、long 等数据写到buffer后，position会向前移动到下一个可插入的buffer单元
//position最大可为capacity-1
//当读取数据时，也是从某个特定位置读。当将buffer从写模式切换到读模式，position会被重置为0
//当从buffer的position处读取数据时，position向前移动到下一个可读的位置

//limit
//在写模式下，buffer的limit表示你最多能往buffer里写多少数据.写模式下，limit等于buffer的capacity
//当切换buffer到读模式时
//limit表示你最多能读到多少数据。因此，当切换buffer到读模式时，limit会被设置成写模式下的position值
//换句话说，你能读到之前写入的所有数据

//flip
//flip方法将buffer从写模式切换到读模式。调用flip()方法会将position设回0，并将limit设置成之前的position值


//rewind
//将position设置为0，所以你可以重读buffer中的所有数据。limit保持不变。

//一旦读完buffer中的数据，需要让buffer准备好再次被写入。可以通过clear()或compact()方法来完成。

//clear
//如果调用的是clear()方法，position将被设置为0，limit被设置成capacity的值

//compact
//如果buffer中仍有未读的数据，且后续还需要这些数据，但是此时想要先写点数据，那么使用compact()方法
//compact()方法将所有未读的数据拷贝到buffer起始处。然后将position设到最后一个未读元素正后面。
//limit属性依然向clear()方法一样，设置成capacity。现在buffer准备好写数据了，但是不会覆盖未读的数据


//mark
//标记当前position

//reset
//回退到之前标记的position

//hasRemaining()
//还能继续读下去么？
public class ByteBufferTest {
	
	public static void main(String[] args) {
		
		//分配256字节的bytebuffer
		ByteBuffer buffer = ByteBuffer.allocate(256);
		System.out.println("初始化："+buffer.position());    
        System.out.println("初始化："+buffer.limit());    
        System.out.println("初始化："+buffer.capacity());
		
		buffer.put("hello".getBytes());
		System.out.println("放入5个字节："+buffer.position());    
        System.out.println("放入5个字节："+buffer.limit());    
        System.out.println("放入5个字节："+buffer.capacity());    
		//写改读
		buffer.flip();
		System.out.println("flip之后："+buffer.position());    
        System.out.println("flip之后："+buffer.limit());    
        System.out.println("flip之后："+buffer.capacity());   
		
		byte[] data = new byte[buffer.limit()];
		buffer.get(data);
		System.out.println(new String(data));
		
		System.out.println("拿出5个字节："+buffer.position());    
        System.out.println("拿出5个字节："+buffer.limit());    
        System.out.println("拿出5个字节："+buffer.capacity());    
		
        buffer.rewind();  
        System.out.println("执行rewind，重新读取数据"); 
        System.out.println("rewind之后："+buffer.position());    
        System.out.println("rewind之后："+buffer.limit());    
        System.out.println("rewind之后："+buffer.capacity());   
        
        buffer.rewind();
        buffer.get();
        buffer.mark();
        buffer.get();
        System.out.println("拿出1个字节："+buffer.position());    
        System.out.println("拿出1个字节："+buffer.limit());    
        System.out.println("拿出个字节："+buffer.capacity());    
        buffer.reset();
        System.out.println("reset之后："+buffer.position());    
        System.out.println("reset之后："+buffer.limit());    
        System.out.println("reset之后："+buffer.capacity()); 
        
        buffer.compact();
        System.out.println("compact之后："+buffer.position());    
        System.out.println("compact之后："+buffer.limit());    
        System.out.println("compact之后："+buffer.capacity()); 
        
        System.out.println("Integer.BYTES,"+Integer.BYTES);
	}

}
