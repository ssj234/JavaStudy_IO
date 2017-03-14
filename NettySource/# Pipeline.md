# ChannelPipeline

## 概述
Netty的ChannelPipeline和ChannelHandler机制类似于Servlet和Filter过滤器，是责任链的一种。
消息在ChannelPipeline中流动和传递，ChannelPipeline内部维护了一个过滤器的双向链表，有Head和Tail指向表头和表尾；该链表对I/O事件进行拦截和处理，可以方便的通过新增和删除链表中的ChannelHandler来实现不同的业务逻辑，不需要对已有的ChannelHandler进行修改。

## ChannelPipeline

**ChannelInboundInvoker接口**
该接口定义了触发过滤器的方法
```
    ChannelInboundInvoker fireChannelRegistered();
    ChannelInboundInvoker fireChannelUnregistered();
    ChannelInboundInvoker fireChannelActive();
    ChannelInboundInvoker fireChannelInactive();
    ChannelInboundInvoker fireExceptionCaught(Throwable cause);
    ChannelInboundInvoker fireUserEventTriggered(Object event);
    ChannelInboundInvoker fireChannelRead(Object msg);
    ChannelInboundInvoker fireChannelReadComplete();
    ChannelInboundInvoker fireChannelWritabilityChanged();
```
**ChannelOutboundInvoker**
内部定义了Channel的绑定、连接、读写等方法
```
ChannelFuture bind(SocketAddress localAddress);
ChannelFuture connect(SocketAddress remoteAddress);
...
```
**ChannelPipeline接口**
该接口继承了ChannelInboundInvoker和ChannelOutboundInvoker、Iterable接口，其中Iterable用来遍历过滤器ChannelHandler；该接口自己定义了一些操作过滤器链表的一些方法，如addLast等
```
ChannelInboundInvoker, ChannelOutboundInvoker, Iterable<Entry<String, ChannelHandler>>


ChannelPipeline addFirst(String name, ChannelHandler handler);
ChannelPipeline addLast(EventExecutorGroup group, String name, ChannelHandler handler);
ChannelPipeline addBefore(EventExecutorGroup group, String baseName, String name, ChannelHandler handler);
ChannelPipeline addAfter(EventExecutorGroup group, String baseName, String name, ChannelHandler handler);
ChannelPipeline remove(ChannelHandler handler);
```

**DefaultChannelPipeline**
DefaultChannelPipeline是Netty默认使用的Pipeline，内部使用下面两个遍历维护链表，注意链表使用的是AbstractChannelHandlerContext抽象类，
```
final AbstractChannelHandlerContext head;
final AbstractChannelHandlerContext tail;
```
初始化链表
```
protected DefaultChannelPipeline(Channel channel) {
        this.channel = ObjectUtil.checkNotNull(channel, "channel");
        succeededFuture = new SucceededChannelFuture(channel, null);
        voidPromise =  new VoidChannelPromise(channel, true);

        tail = new TailContext(this);
        head = new HeadContext(this);

        head.next = tail;
        tail.prev = head;
}
```
上面的TailContext和HeadContext是DefaultChannelPipeline的内部类,其中TailContext 实现了ChannelInboundHandler 接口，HeadContext 还实现了ChannelOutboundHandler，这是因为read时，从HeadContext->TailContext，而write操作时从TailContext->HeadContext，读操作的时候需要首先由HeadContext的javachannel读取数据，再传递给其他过滤器；而写时需要HeadContext最终将数据通过javachannel写到Channel。
```
final class HeadContext extends AbstractChannelHandlerContext
            implements ChannelOutboundHandler, ChannelInboundHandler {
            
final class TailContext extends AbstractChannelHandlerContext 
			implements ChannelInboundHandler {
//读
public final ChannelPipeline fireChannelRead(Object msg) {
    AbstractChannelHandlerContext.invokeChannelRead(head, msg);
    return this;
}
//写
public final ChannelFuture writeAndFlush(Object msg, ChannelPromise promise) {
    return tail.writeAndFlush(msg, promise);
}
```

在上面的分析中，我们可以看到过滤器链并不是直接将ChannelHandler连接起来，而是通过Context连接起来的。下面的addLast()的内部代码可以看到，添加之前，先将其包装为一个DefaultChannelHandlerContext。
```
addLast()
newCtx = newContext(group, filterName(name, handler), handler);
addLast0(newCtx);

private AbstractChannelHandlerContext newContext(EventExecutorGroup group, String name, ChannelHandler handler) {
        return new DefaultChannelHandlerContext(this, childExecutor(group), name, handler);
    }
```

## 常用的过滤器

**ByteToMessageDecoder**
**ByteToMessageEncode**
**MessageToMessageEncoder**
**MessageToMessageDecoder**