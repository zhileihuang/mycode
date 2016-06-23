package com.huang.rpc.datasource.impl;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.huang.rpc.datasource.DataSource;
import com.huang.rpc.datasource.Row;
import com.huang.rpc.util.Utils;

public class PageDataSource implements DataSource {

	private static final Logger log = LoggerFactory.getLogger(PageDataSource.class);
	private final File dataFile;

	// 空行，避免过多的对象分配
	private final static Row EMPTY_ROW = new Row(-1, new byte[0]);

	// 缓存页大小，要求是4k的倍数
	private final static int BUFFER_SIZE = 4;

	// 页行大小
	// 一行数据构成：有效字节数(4B)+数据段(210B)+填充段(42B) = 256B
	private final int PAGE_ROW_SIZE = 256;

	// 页行数，一页中总共有几行
	private final int PAGE_ROWS_NUM = 10;

	// 页码表大小
	// 一共有几页
	private final int PAGE_TABLE_SIZE = 4;

	// 页码表
	private Page[] pageTables = new Page[PAGE_TABLE_SIZE];

	// 标记是否曾经有其他线程到达过EOF状态
	private volatile boolean isEOF = false;

	// 页面切换者锁
	private final ReentrantLock pageSwitchLock = new ReentrantLock();
	private final Condition pageSwitchWakeUpCondition = pageSwitchLock.newCondition();

	private volatile Page currentPage = null;

	public PageDataSource(File dataFile) {
		this.dataFile = dataFile;
	}

	@Override
	public Row getRow(Row row) throws IOException {

		if (isEOF) {
			if (null == row) {
				return EMPTY_ROW;
			} else {
				row.setLineNum(EMPTY_ROW.getLineNum());
				row.setData(EMPTY_ROW.getData());
				return row;
			}
		}

		while (true) {
			final Page page = currentPage == null ? pageTables[0] : currentPage;
			final int readCount = page.readCount.get();
			final int rowCount = page.rowCount;

			if (page.isLast && readCount == rowCount) {
				if (null == row) {
					return EMPTY_ROW;
				} else {
					row.setLineNum(EMPTY_ROW.getLineNum());
					row.setData(EMPTY_ROW.getData());
					return row;
				}
			}

			if (readCount == rowCount) {
				continue;
			}

			if (!page.readCount.compareAndSet(readCount, readCount + 1)) {
				continue;
			}

			final int offsetOfRow = readCount * PAGE_ROW_SIZE;

			final ByteBuffer byteBuffer = ByteBuffer.wrap(page.data, offsetOfRow, PAGE_ROW_SIZE);
			final int lineNum = byteBuffer.getInt();
			final int validByteCount = byteBuffer.getInt();
			final byte[] data = new byte[validByteCount];
			byteBuffer.get(data);

			if (page.readCount.get() == rowCount) {
				if (page.isLast) {
					isEOF = true;
				} else {
					pageSwitchLock.lock();
					try {
						pageSwitchWakeUpCondition.signal();
					} finally {
						pageSwitchLock.unlock();
					}

					final int pageNum = page.pageNum;
					final int nextPageIdx = (pageNum + 1) % PAGE_TABLE_SIZE;
					while (pageTables[nextPageIdx].pageNum != pageNum + 1) {
						// spin for switch
					}
					currentPage = pageTables[nextPageIdx];
				}
			}

			if (null == row) {
				row = new Row();
			} else {
				row.setLineNum(lineNum);
				row.setData(data);
			}
			return row;
		}
	}

	@Override
	public Row getRow() throws IOException {
		return getRow(null);
	}

	@Override
	public void init() throws IOException {
		final Thread pageSwitcher = new Thread(() -> {
			for (int i = 0; i < pageTables.length; i++) {
				final Page page = new Page();
				page.pageNum = i;
				pageTables[i] = page;
			}

		    // 下一次要替换掉的页码表编号
			int nextSwitchPageTableIndex = 0;

			// 文件读取偏移量
			long fileOffset = 0;

			try (final FileChannel fileChannel = new RandomAccessFile(dataFile, "r").getChannel()) {
				final long fileSize = fileChannel.size();
				int lineCounter = 0;
				MappedByteBuffer mappedBufer = null;

				DecodeLineState state = DecodeLineState.READ_D;

				while (fileOffset < fileSize) {

					// 遍历页码表，主要做两件事
					// 1.顺序的更换页码
					// 2.将文件缓存刷入页码
					final Page page = pageTables[nextSwitchPageTableIndex];

					// 如果已经被初始化后的当前页还没被读完,休眠等待被唤醒
					if (page.isInit && page.readCount.get() < page.rowCount) {
						pageSwitchLock.lock();
						try {
							pageSwitchWakeUpCondition.await();
							continue;
						} catch (Exception e) {
							Thread.currentThread().interrupt();
						} finally {
							pageSwitchLock.unlock();
						}
					}

					if (!page.isInit || page.readCount.get() == page.rowCount) {

						final ByteBuffer dataBuffer = ByteBuffer.wrap(page.data);

						int rowIdx = 0;

						final ByteBuffer tempBuffer = ByteBuffer.allocate(PAGE_ROW_SIZE);

						FILL_PAGE_LOOP: 
						while (true) {

							if (null == mappedBufer || !mappedBufer.hasRemaining()) {
								final long fixLength = (fileOffset + BUFFER_SIZE >= fileSize) ? fileSize - fileOffset : BUFFER_SIZE;
								if (null != mappedBufer) {
									Utils.unmap(mappedBufer);
								}
								if (fixLength > 0) {
									mappedBufer = fileChannel.map(MapMode.READ_ONLY, fileOffset, fixLength).load();
								}
							}
							if (!mappedBufer.hasRemaining()) {
								page.isLast = true;
								break;
							}

							while (mappedBufer.hasRemaining()) {
								switch (state) {
								case READ_D: {
									final byte b = mappedBufer.get();
									fileOffset++;
									if (b == '\r') {
										state = DecodeLineState.READ_R;
									} else {
										tempBuffer.put(b);
										break;
									}
								}
								case READ_R: {
									final byte b = mappedBufer.get();
									fileOffset++;
									if (b != '\n') {
										throw new IOException("illegal format,\\n did not behind \\r, b=" + b);
									}
									state = DecodeLineState.READ_N;
								}
								case READ_N: {
									state = DecodeLineState.READ_D;
									tempBuffer.flip();
									final int dateLength = tempBuffer.limit();
									dataBuffer.putInt(lineCounter++);
									final byte[] _data = new byte[dateLength];
									tempBuffer.get(_data);
									final byte[] __data = Utils.process(_data);
									dataBuffer.putInt(__data.length);
									dataBuffer.put(__data);
									tempBuffer.clear();

									if (++rowIdx == PAGE_ROWS_NUM) {
										break FILL_PAGE_LOOP;
									}

									int offsetOfRow = rowIdx * PAGE_ROW_SIZE;
									dataBuffer.position(offsetOfRow);
									break;

								}
								default:
									throw new IOException("init failed, illegal state=" + state);
								}
							}

						}

						page.rowCount = rowIdx;
						page.readCount.set(0);
						log.info("page.pagenum={} was switched.fileOffset={},fileSize={},page.rowCount={};", new Object[] { page.pageNum, fileOffset, fileSize, page.rowCount });

						if (fileOffset == fileSize) {
							page.isLast = true;
							log.info("page.pagenum={} is last,page.readcount={}", page.pageNum, page.readCount.get());
						}

						if (page.isInit) {
							page.pageNum += PAGE_TABLE_SIZE;
						} else {
							page.isInit = true;
						}
						nextSwitchPageTableIndex = (nextSwitchPageTableIndex + 1) % PAGE_TABLE_SIZE;
					}

				}

			} catch (Exception e) {
				log.warn("mapping file={} failed.", dataFile, e);
			}

			log.info("PageDataSource(file:{}) was arrive EOF.", dataFile);

		}, "PageDataSource");
		pageSwitcher.setDaemon(true);
		pageSwitcher.start();
		log.info("PageDataSource(file:{}) was inited.", dataFile);
	}

	@Override
	public void destroy() throws IOException {
		log.info("PageDataSource(file:{}) was destroyed.", dataFile);
	}

	class Page {
		volatile int pageNum;
		volatile int rowCount = 0; //总行数
		// 已被读取行数
		AtomicInteger readCount = new AtomicInteger(0);
		volatile boolean isLast = false;
		volatile boolean isInit = false;
		byte[] data = new byte[PAGE_ROW_SIZE * PAGE_ROWS_NUM];
	}

	private enum DecodeLineState {
		READ_D, READ_R, READ_N

	}

}
