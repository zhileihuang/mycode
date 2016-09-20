package com.huang.tinyhttp.nio;

import java.nio.ByteBuffer;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class ByteBufferFifo {

	private static final int BUFFER_SIZE = 4096;

	private final AtomicReference<ByteBuffer> currentBuffer = new AtomicReference<>();
	private final BlockingDeque<ByteBuffer> readBuffers = new LinkedBlockingDeque<>();

	private AtomicBoolean ignoreData = new AtomicBoolean(false);

	public ByteBufferFifo() {
		currentBuffer.set(ByteBuffer.allocate(BUFFER_SIZE));
	}

	public ByteBufferFifo(final ByteBuffer buffer) {
		readBuffers.add(buffer);
	}

	/**
	 * Tells to the ByteBufferFifo to flush the buffer and ignore all new write
	 * buffer requests.
	 * 
	 */
	public void stop() {
		if (ignoreData.compareAndSet(false, true)) {
			ByteBuffer buffer = currentBuffer.get();
			if (currentBuffer.compareAndSet(buffer, null) && buffer != null) {
				buffer.flip();
				readBuffers.add(buffer);
			}
		}
	}

	/**
	 * Request a new BytBuffer to be used to receive new data from the NIO.
	 * 
	 * @return A ByteBuffer with remaining space or null.
	 * 
	 */
	public ByteBuffer getWriteBuffer() {
		if (ignoreData.get()) {
			return null;
		}
		ByteBuffer buffer = null;
		do {
			buffer = currentBuffer.get();
			if (buffer == null || !buffer.hasRemaining()) {
				if (currentBuffer.compareAndSet(buffer, ByteBuffer.allocate(BUFFER_SIZE)) && buffer != null) {
					buffer.flip();
					readBuffers.add(buffer);
				}
				buffer = null;
			}
		} while (buffer == null);
		if (ignoreData.get()) {
			readBuffers.add(buffer);
		}
		return buffer;
	}

	/**
	 * Invalidate a write buffer, adding it to the read ready buffer.
	 * 
	 */
	public void invalidateWriteBuffer() {
		if (ignoreData.get()) {
			return;
		}

		ByteBuffer buffer = null;
		do {
			buffer = currentBuffer.get();
			if (currentBuffer.compareAndSet(buffer, ByteBuffer.allocate(BUFFER_SIZE)) && buffer != null) {
				buffer.flip();
				readBuffers.add(buffer);
			}
			buffer = null;
		} while (buffer == null);

	}

	/**
	 * Request a filled buffer. If there is no buffer to read, return null.
	 * 
	 * @return A filled buffer to be read or null.
	 * 
	 */
	public ByteBuffer getReadBuffer() {
		try {
			ByteBuffer ret = readBuffers.poll(100, TimeUnit.MILLISECONDS);

			if (ret == null) {
				ret = currentBuffer.getAndSet(null);
				if (ret != null)
					ret.flip();
			}

			return ret;
		} catch (InterruptedException e) {
			return null;
		}
	}

	/**
	 * Prepend a byte buffer to the ready buffer.
	 * 
	 * @param buffer
	 *            Buffer to be added to the read buffer.
	 * 
	 */
	public void prependByteBuffer(final ByteBuffer buffer) {
		readBuffers.push(buffer);
	}

	/**
	 * Clear all the information from this FIFO.
	 * 
	 */
	public void clear() {
		readBuffers.clear();
		currentBuffer.set(ByteBuffer.allocate(BUFFER_SIZE));
	}

}
