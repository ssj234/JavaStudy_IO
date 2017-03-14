# Read & Write

### 服务器读取数据
根据上面的分析，会执行unsafe.read()，服务器端的读事件是监听了连接时生成的对于NioSocketChannel来说，其unsafe为NioSocketChannelUnsafe,其逻辑为：
* 获取缓存分配器，服务接受到请求连接成功后，会调用public NioSocketChannel(Channel parent, SocketChannel socket)，其config中的分配器是默认的`ByteBufAllocator allocator = ByteBufAllocator.DEFAULT;` 为 PooledByteBufAllocator.DEFAULT
* 获取接收缓存分配处理器，AdaptiveRecvByteBufAllocator$HandleImpl，使用PooledByteBufAllocator的ioBuffer分配缓冲，大小为1024，这步生成了byteBuf
* 一直不断的读取，读取到bytebuf中，然后调用缓冲处理器，将其保存在直接内存DirectByteBuffer中，然后触发fireChannelRead方法
* 读完之后，触发pipeline的ReadComplete事件
```
public final void read() {
            final ChannelConfig config = config();
            final ChannelPipeline pipeline = pipeline();
            //1.缓冲分配器PooledByteBufAllocator
            final ByteBufAllocator allocator = config.getAllocator();
            //2.缓冲分配处理器
            final RecvByteBufAllocator.Handle allocHandle = recvBufAllocHandle();
            allocHandle.reset(config);

            ByteBuf byteBuf = null;
            boolean close = false;
            try {
                do {//一直读到没有数据
                    //调用PooledByteBufAllocator分配缓冲
                    byteBuf = allocHandle.allocate(allocator);
                    //将读取的数据保存在内部缓冲DirectByteBuffer中
                    allocHandle.lastBytesRead(doReadBytes(byteBuf));
                    if (allocHandle.lastBytesRead() <= 0) {
                        // nothing was read. release the buffer.
                        byteBuf.release();
                        byteBuf = null;
                        close = allocHandle.lastBytesRead() < 0;
                        break;
                    }

                    allocHandle.incMessagesRead(1);
                    readPending = false;
                    pipeline.fireChannelRead(byteBuf);
                    byteBuf = null;
                } while (allocHandle.continueReading());

                allocHandle.readComplete();
                pipeline.fireChannelReadComplete();

                if (close) {
                    closeOnRead(pipeline);
                }
            } catch (Throwable t) {
                handleReadException(pipeline, byteBuf, t, close, allocHandle);
            } finally {
                // Check if there is a readPending which was not processed yet.
                // This could be for two reasons:
                // * The user called Channel.read() or ChannelHandlerContext.read() in channelRead(...) method
                // * The user called Channel.read() or ChannelHandlerContext.read() in channelReadComplete(...) method
                //
                // See https://github.com/netty/netty/issues/2254
                if (!readPending && !config.isAutoRead()) {
                    removeReadOp();
                }
            }
        }
```

### 客户端写入数据
起始是我们调用的context的write方法
```
ChannelHandlerContext ctx;
ctx.writeAndFlush(firstMessage);
```
其会调用AbstractChannelHandlerContext的writeAndFlush,会创建一个DefaultChannelPromise
```
public ChannelFuture writeAndFlush(Object msg) {
        return writeAndFlush(msg, newPromise());
    }
```
校验promise，调用write
```
public ChannelFuture writeAndFlush(Object msg, ChannelPromise promise) {
        if (msg == null) {
            throw new NullPointerException("msg");
        }
        if (!validatePromise(promise, true)) {
            ReferenceCountUtil.release(msg);
            // cancelled
            return promise;
        }
        write(msg, true, promise);
        return promise;
    }
```
write方法会查找pipeline的outbound的ctx，调用其invokeWriteAndFlush
```
private void write(Object msg, boolean flush, ChannelPromise promise) {
        AbstractChannelHandlerContext next = findContextOutbound();
        final Object m = pipeline.touch(msg, next);
        EventExecutor executor = next.executor();
        if (executor.inEventLoop()) {
            if (flush) {
                next.invokeWriteAndFlush(m, promise);
            } else {
                next.invokeWrite(m, promise);
            }
        } else {
            AbstractWriteTask task;
            if (flush) {
                task = WriteAndFlushTask.newInstance(next, m, promise);
            }  else {
                task = WriteTask.newInstance(next, m, promise);
            }
            safeExecute(executor, task, promise, m);
        }
    }
```
invokeWriteAndFlush,会调用invokeWrite0写入，invokeFlush0刷新
```
private void invokeWriteAndFlush(Object msg, ChannelPromise promise) {
        if (invokeHandler()) {
            invokeWrite0(msg, promise);
            invokeFlush0();
        } else {
            writeAndFlush(msg, promise);
        }
    }
```

**invokeWrite0**
invokeWrite0, next最终是head，调用HeadContext的write
```
//handler()最终是HeadContext
((ChannelOutboundHandler) handler()).write(this, msg, promise);
//HeadContext的write
unsafe.write(msg, promise);
//客户端的unsafe是NioSocketChannelUnsafe
```
NioSocketChannelUnsafe的程序如下，
```
public final void write(Object msg, ChannelPromise promise) {
            assertEventLoop();
            //ChannelOutboundBuffer
            ChannelOutboundBuffer outboundBuffer = this.outboundBuffer;
            if (outboundBuffer == null) {
                ReferenceCountUtil.release(msg);
                return;
            }
            int size;
            try {
                //不是直接内存就创建直接内存
                msg = filterOutboundMessage(msg);
                //io.netty.channel.DefaultMessageSizeEstimator$HandleImpl
                //计算大小
                size = pipeline.estimatorHandle().size(msg);
                if (size < 0) {
                    size = 0;
                }
            } catch (Throwable t) {
                safeSetFailure(promise, t);
                ReferenceCountUtil.release(msg);
                return;
            }
            //加入outbound
            outboundBuffer.addMessage(msg, size, promise);
        }
```

**invokeFlush0**
```
//最终时HeadContext，调用unsafe的flush
((ChannelOutboundHandler) handler()).flush(this);
//NioSocketChannelUnsafe
unsafe.flush();
//unsafe的flush
flush0()
//NioSocketChannel的doWrite
doWrite(outboundBuffer);
```
doWrite, javachannel write出去
```
protected void doWrite(ChannelOutboundBuffer in) throws Exception {
        for (;;) {
            int size = in.size();
            if (size == 0) {
                // All written so clear OP_WRITE
                clearOpWrite();
                break;
            }
            long writtenBytes = 0;
            boolean done = false;
            boolean setOpWrite = false;

            // Ensure the pending writes are made of ByteBufs only.
            ByteBuffer[] nioBuffers = in.nioBuffers();
            int nioBufferCnt = in.nioBufferCount();
            long expectedWrittenBytes = in.nioBufferSize();
            SocketChannel ch = javaChannel();
            switch (nioBufferCnt) {
                case 0:
                    // We have something else beside ByteBuffers to write so fallback to normal writes.
                    super.doWrite(in);
                    return;
                case 1:
                    // Only one ByteBuf so use non-gathering write
                    ByteBuffer nioBuffer = nioBuffers[0];
                    for (int i = config().getWriteSpinCount() - 1; i >= 0; i --) {
                        final int localWrittenBytes = ch.write(nioBuffer);
                        if (localWrittenBytes == 0) {
                            setOpWrite = true;
                            break;
                        }
                        expectedWrittenBytes -= localWrittenBytes;
                        writtenBytes += localWrittenBytes;
                        if (expectedWrittenBytes == 0) {
                            done = true;
                            break;
                        }
                    }
                    break;
                default:
                    for (int i = config().getWriteSpinCount() - 1; i >= 0; i --) {
                        final long localWrittenBytes = ch.write(nioBuffers, 0, nioBufferCnt);
                        if (localWrittenBytes == 0) {
                            setOpWrite = true;
                            break;
                        }
                        expectedWrittenBytes -= localWrittenBytes;
                        writtenBytes += localWrittenBytes;
                        if (expectedWrittenBytes == 0) {
                            done = true;
                            break;
                        }
                    }
                    break;
            }

            // Release the fully written buffers, and update the indexes of the partially written buffer.
            in.removeBytes(writtenBytes);

            if (!done) {
                // Did not write all buffers completely.
                incompleteWrite(setOpWrite);
                break;
            }
        }
    }
```

### 服务端写入数据




## ResourceLeakDetector
记录ReferenceCounted的count

poolAreba
PoolChunk