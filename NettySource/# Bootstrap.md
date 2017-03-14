# Bootstrap



## 1.启动器准备

### 概述
Bootstrap启动准备阶段的主要目的是初始化启动器，为启动器设置相关属性。
我们先来看一下Bootstrap的类结构，
其有一个AbstractBootstrap基类，有两个实现类Bootstrap和ServerBootstrap，分别用于客户端和服务器的启动。

### AbstractBootstrap

**属性**
```

EventLoopGroup group; //线程组,对于ServerBootstrap来说，为ServerSocketChannel服务
ChannelFactory<? extends C> channelFactory; //用于获取channel的工厂类
SocketAddress localAddress;//绑定时候的地址
Map<ChannelOption<?>, Object> options;//channel可设置的选项，包含java-channel和netty-channel
Map<AttributeKey<?>, Object> attrs;//channel属性
ChannelHandler handler;//处理器，
```
**方法**
```
group() 设置线程组
channelFactory()及channel() 设置channel工厂和channel类型
localAddress() 设置地址
option() 添加选项
attr() 添加属性
handler() 设置channelHander
```
上面这些方法主要用于设置启动器的相关参数，除此之外，还有一些启动时调用的方法
```
register() 内部调用initAndRegister() 用来初始化channel并注册到线程组
bind() 首先会调用initAndRegister()，之后绑定IP地址，使用Promise保证先initAndRegister()在bind()
initAndRegister()，主要是创建netty的channel，设置options和attrs，注册到线程组
```

### Bootstrap

Bootstrap用于Client启动，继承了AbstractBootstrap类，新增了如下属性

```
//启动器配置类，主要用来获取启动器的相关配置，基本上就AbstractBootstrap的一些属性
private final BootstrapConfig config = new BootstrapConfig(this);
//远程地址
private volatile SocketAddress remoteAddress;
// SocketAddress解析器集合，主机到IP的解析；
// AddressResolverGroup内部有一个Map-resolvers，映射了EventExecutor -> AddressResolver<T>
private volatile AddressResolverGroup<SocketAddress> resolver =
            (AddressResolverGroup<SocketAddress>) DEFAULT_RESOLVER;
```

新增的方法
```
resolver() 设置AddressResolverGroup
remoteAddress() 设置remoteAddress
connect() 连接到远程地址，
```
由于client的连接主要是connect方法，其调用了
```
initAndRegister();  AbstractBootstrap的方法
doResolveAndConnect0();  获取EventLoop的AddressResolver，查看指定的地址是否支持，是否已经解析并doConnect
doConnect() 将channel.connect加入线程池
```

### ServerBootstrap

ServerBootstrap用于Server端的启动，也继承了AbstractBootstrap类，由于需要处理处理客户端连接时的channel(SocketChannel)，因此又一些子属性，如
```
//设置连接时打开的channel的选项
private final Map<ChannelOption<?>, Object> childOptions = new LinkedHashMap<ChannelOption<?>, Object>();
//设置连接时打开的channel的属性
private final Map<AttributeKey<?>, Object> childAttrs = new LinkedHashMap<AttributeKey<?>, Object>();
//用于获取ServerBootstrap的配置
private final ServerBootstrapConfig config = new ServerBootstrapConfig(this);
//处理SocketChannel的子线程组
private volatile EventLoopGroup childGroup;
//处理SocketChannel的子处理器
private volatile ChannelHandler childHandler;
```
新增的设置子属性的方法
```
childOption() 设置NioSocketChannel的选项
childAttr() 设置NioSocketChannel的属性
childHandler() 设置NioSocketChannel的处理器
```
除此之外，还包括一个内部类ServerBootstrapAcceptor，这是一个ChannelHandler，会在register时加入Pipeline中。主要用来接收客户端的请求，后续章节会进行详细说明。

### 准备代码

**ServerBootstrap**
```
//创建两个线程组
EventLoopGroup bossGroup=new NioEventLoopGroup();
EventLoopGroup workGroup=new NioEventLoopGroup();
    
try{
    //创建启动类
    ServerBootstrap b=new ServerBootstrap();
    ////创建的channel为NioServerSocketChannel【nio-ServerSocketChannel】
    b.group(bossGroup,workGroup).channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .childOption(ChannelOption.SO_KEEPALIVE, true) //配置accepted的channel
                .childHandler(new ChildChannelHandler());//处理IO事件的处理类，处理网络事件
```
上述代码中，创建启动器，设置了两个线程组，设置了channel类型，选项，子channel选项和子channnel处理器

**Bootstrap**
```

EventLoopGroup group=new NioEventLoopGroup();
try{
    Bootstrap b=new Bootstrap();
    b.group(group)
        .channel(NioSocketChannel.class)
        .option(ChannelOption.TCP_NODELAY, true)
        .handler(new ChannelInitializer<SocketChannel>(){
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(new TimeClientHandler());
            }           
        });
```
上述代理，与ServerBootstrap不同，没有为childchannel，因为其channel就是服务器的子channel

### 总结

启动器的准备阶段，主要是设置
* channel的线程组
* channel的类型、选项、属性、handler等


## 2.启动阶段

### ServerBootstrap

**>bind方法**
首先，来看看服务器启动器的启动代码 ，首先调用initAndRegister，返回一个注册promise，之后如果mise成功则调用doBind0否则加入promise的监听事件等待完成后调用doBind方法

```
private ChannelFuture doBind(final SocketAddress localAddress) {
        //[initAndRegister] 初始化channel并注册到线程池组
        final ChannelFuture regFuture = initAndRegister();
        //若出错，返回注册的future
        final Channel channel = regFuture.channel();
        if (regFuture.cause() != null) {
            return regFuture;
        }

        if (regFuture.isDone()) {//注册已经结束了
            //创建一个channel的promise
            ChannelPromise promise = channel.newPromise();
            //绑定
            doBind0(regFuture, channel, localAddress, promise);
            return promise;
        } else {  //注册没有结束
            // Registration future通常已经结束了，但也可能没，没有的话加个监听器，执行完后再执行内部的方法
            final PendingRegistrationPromise promise = new PendingRegistrationPromise(channel);
            regFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    Throwable cause = future.cause();
                    if (cause != null) {
                        promise.setFailure(cause);
                    } else {
                        promise.registered();
                        doBind0(regFuture, channel, localAddress, promise);
                    }
                }
            });
            return promise;
        }
    }
```
可以看到，绑定方法主要会
* 调用initAndRegister()初始化NioServerSocketChannel并注册到线程池组上
* 通过future机制，保证注册完成后，调用doBind0()方法
下面主要分析这两个方法：

**bind > initAndRegister**
1. 先根据指定Class，创建channel的实例，
2. 调用init方法,init是抽象方法，Bootstrap和ServerBootstrap分别实现了。
3. 调用完成后，调用group().register(channel) 为线程组注册channel
```
final ChannelFuture initAndRegister() {
        Channel channel = null;
        try {
            //根据指定的channelFactory创建channel，
            //这里使用的是ReflectiveChannelFactory，根据指定Class，创建channel的实例
            channel = channelFactory.newChannel();
            init(channel);
        } catch (Throwable t) {
            if (channel != null) {
                // channel can be null if newChannel crashed (eg SocketException("too many open files"))
                channel.unsafe().closeForcibly();
            }
            // as the Channel is not registered yet we need to force the usage of the GlobalEventExecutor
            return new DefaultChannelPromise(channel, GlobalEventExecutor.INSTANCE).setFailure(t);
        }

        ChannelFuture regFuture = config().group().register(channel);
        if (regFuture.cause() != null) {
            if (channel.isRegistered()) {
                channel.close();
            } else {
                channel.unsafe().closeForcibly();
            }
        }

        return regFuture;
    }
```


***bind > initAndRegister > init |  初始化channel***
1. 为channel设置选项和属性
2. 为channel的pipeline添加handler，该handler会在initChannel后添加一个ServerBootstrapAcceptor(ChannelHandler)
```
void init(Channel channel) throws Exception {
        //1.为channel设置option和attr
        final Map<ChannelOption<?>, Object> options = options0();
        synchronized (options) {
            channel.config().setOptions(options);
        }
        final Map<AttributeKey<?>, Object> attrs = attrs0();
        synchronized (attrs) {
            for (Entry<AttributeKey<?>, Object> e: attrs.entrySet()) {
                @SuppressWarnings("unchecked")
                AttributeKey<Object> key = (AttributeKey<Object>) e.getKey();
                channel.attr(key).set(e.getValue());
            }
        }
        
        ChannelPipeline p = channel.pipeline();

        final EventLoopGroup currentChildGroup = childGroup;
        final ChannelHandler currentChildHandler = childHandler;
        final Entry<ChannelOption<?>, Object>[] currentChildOptions;
        final Entry<AttributeKey<?>, Object>[] currentChildAttrs;
        synchronized (childOptions) {
            currentChildOptions = childOptions.entrySet().toArray(newOptionArray(childOptions.size()));
        }
        synchronized (childAttrs) {
            currentChildAttrs = childAttrs.entrySet().toArray(newAttrArray(childAttrs.size()));
        }
        //2.为channel的pipeline添加一个handler,
        p.addLast(new ChannelInitializer<Channel>() {
            @Override
            public void initChannel(Channel ch) throws Exception {
                final ChannelPipeline pipeline = ch.pipeline();
                ChannelHandler handler = config.handler();
                if (handler != null) {
                    pipeline.addLast(handler);
                }
                ch.eventLoop().execute(new Runnable() {
                    @Override
                    public void run() {
                        pipeline.addLast(new ServerBootstrapAcceptor(
                                currentChildGroup, currentChildHandler, currentChildOptions, currentChildAttrs));
                    }
                });
            }
        });
    }
```

***bind > initAndRegister > register | 线程组注册channel***

1.在线程池组中选择一个线程池，调用其register方法
2.线程池会创建一个DefaultChannelPromise
3.调用channel的unsafe的register，unsafe时在NioServerSocketChannel初始化时设置的NioMessageUnsafe，其是AbstractChannel的内部类AbstractUnsafe的子类；
4.unsafe的主要目的是判断当前线程是否在指定的线程池中，如果在直接执行register0(),否则提交到线程池中执行register0
5.register0方法
```
//EventLoopGroup中
public ChannelFuture register(Channel channel) {
    return next().register(channel);
}
//EventLoop中
public ChannelFuture register(Channel channel) {
    return register(new DefaultChannelPromise(channel, this));
}
public ChannelFuture register(final ChannelPromise promise) {
    ObjectUtil.checkNotNull(promise, "promise");
    promise.channel().unsafe().register(this, promise);//NioMessageUnsafe
    return promise;
}

//AbstractUnsafe中
public final void register(EventLoop eventLoop, final ChannelPromise promise) {
            if (eventLoop == null) {
                throw new NullPointerException("eventLoop");
            }
            if (isRegistered()) { //如果已经注册过了
                promise.setFailure(new IllegalStateException("registered to an event loop already"));
                return;
            }
            if (!isCompatible(eventLoop)) { //eventLoop 不是NioEventLoop
                promise.setFailure(
                        new IllegalStateException("incompatible event loop type: " + eventLoop.getClass().getName()));
                return;
            }

            AbstractChannel.this.eventLoop = eventLoop;
            if (eventLoop.inEventLoop()) { //如果当前线程在eventLoop中，则执行
                register0(promise);
            } else { //不在eventLoop中，提交一个任务，调用register0
                try {
                    eventLoop.execute(new Runnable() {
                        @Override
                        public void run() {
                            register0(promise);
                        }
                    });
                } catch (Throwable t) {
                    logger.warn(
                            "Force-closing a channel whose registration task was not accepted by an event loop: {}",
                            AbstractChannel.this, t);
                    closeForcibly();
                    closeFuture.setClosed();
                    safeSetFailure(promise, t);
                }
            }
        }    
```
线程组注册channel的核心方法：最后的register0
1. 调用doRegister()，将java的channel注册到线程池的selector上面,修改标志位
2. 调用pipeline的invokeHandlerAddedIfNeeded();其会调用callHandlerAddedForAllHandlers，遍历执行pipeline的任务链表（链表内容是在register之前加入到Pipeline的任务，如ServerBootstrap的init方法会addLast一个Handler）
3. 调用注册promise的promise.trySuccess()，触发其成功事件，在doBind中定义，其会调用doBind()
4. 调用channelHandler的channelRegistered
5. 如果channel已经打开，触发channelHandler的channelActive
```
private void register0(ChannelPromise promise) {
            try {
                if (!promise.setUncancellable() || !ensureOpen(promise)) {
                    return;
                }
                boolean firstRegistration = neverRegistered;
                doRegister();//【1】抽象方法，在AbstractNioChannel中，由于不一定是NIO的需要自己实现
                neverRegistered = false;//已经注册过了
                registered = true;//已经注册
                //【2】执行pipeline的链表任务，保存了注册之前addLast的ChanndleHandler任务
                pipeline.invokeHandlerAddedIfNeeded();
                //【3】注册成功，触发注册的promise的成功事件promise.trySuccess()
                safeSetSuccess(promise);
                //【4】触发pipeline的注册事件
                pipeline.fireChannelRegistered();
                // Only fire a channelActive if the channel has never been registered. This prevents firing
                // multiple channel actives if the channel is deregistered and re-registered.
                if (isActive()) {
                    if (firstRegistration) {
                        pipeline.fireChannelActive();
                    } else if (config().isAutoRead()) {
                        // This channel was registered before and autoRead() is set. This means we need to begin read
                        // again so that we process inbound data.
                        //
                        // See https://github.com/netty/netty/issues/4805
                        beginRead();
                    }
                }
            } catch (Throwable t) {
                // Close the channel directly to avoid FD leak.
                closeForcibly();
                closeFuture.setClosed();
                safeSetFailure(promise, t);
            }
        }
```
AbstractNioChannel的doRegister()
逻辑为：将java的channel注册到线程池的selector上面
```
protected void doRegister() throws Exception {
        boolean selected = false;
        for (;;) {
            try {
                //NioServerSocketChannel对应的javaChannel时在初始化时
                //通过NioServerSocketChannel.DEFAULT_SELECTOR_PROVIDER生成的ServerSocketChannel
                //注册到线程池的selector上面
                selectionKey = javaChannel().register(eventLoop().selector, 0, this);
                return;
            } catch (CancelledKeyException e) {
                if (!selected) {
                    eventLoop().selectNow();
                    selected = true;
                } else {
                    throw e;
                }
            }
        }
    }
```

callHandlerAddedForAllHandlers
逻辑为：遍历Pipeline的pendingHandlerCallbackHead链表，执行其execute方法；pendingHandlerCallbackHead保存的是在register之前addLast()添加到pipeline上的任务(ServerBootstrap的init方法，pipeline.addLast加入了一个Handler)，而addLast方中，如果还没有register，会先加入链表，注册完成后执行任务。
```
private void callHandlerAddedForAllHandlers() {
        final PendingHandlerCallback pendingHandlerCallbackHead;
        synchronized (this) {
            assert !registered;//这是Pipeline的标志位，记录是否将channel注册到了Pipeline上面
            registered = true;
            pendingHandlerCallbackHead = this.pendingHandlerCallbackHead;
            this.pendingHandlerCallbackHead = null;
        }

        //register之前的任务链表，init方法的时候，调用了p.addLast()
        PendingHandlerCallback task = pendingHandlerCallbackHead;
        while (task != null) {
            task.execute();
            task = task.next;
        }
    }
```
在channel注册到线程池组和selector之前，addList到pipleline时会加入链表，注册后调用执行initChannel方法
```
// newCtx返回的是AbstractChannelHandlerContext
newCtx = newContext(group, name, handler);
if (!registered) {
    newCtx.setAddPending();
    callHandlerCallbackLater(newCtx, true);//加入链表
    return this;
}
//添加到链表
private void callHandlerCallbackLater(AbstractChannelHandlerContext ctx, boolean added) {
        assert !registered;

        PendingHandlerCallback task = added ? new PendingHandlerAddedTask(ctx) : new PendingHandlerRemovedTask(ctx);
        PendingHandlerCallback pending = pendingHandlerCallbackHead;
        if (pending == null) {
            pendingHandlerCallbackHead = task;
        } else {
            // Find the tail of the linked-list.
            while (pending.next != null) {
                pending = pending.next;
            }
            pending.next = task;
        }
    }
PendingHandlerAddedTask的execute
callHandlerAdded0(ctx);
    callHandlerAdded0(ctx);
        initChannel(ctx);
```

fireChannelRegistered
```
public final ChannelPipeline fireChannelRegistered() {
    AbstractChannelHandlerContext.invokeChannelRegistered(head);
    return this;
}
调用channelHandler的channelRegistered事件
```



**[>>]doBind0方法**
逻辑比较简单，向线程池提交一个任务，如果注册成功，调用channel的bind方法，绑定指定地址。
```
 private static void doBind0(
            final ChannelFuture regFuture, final Channel channel,
            final SocketAddress localAddress, final ChannelPromise promise) {
        channel.eventLoop().execute(new Runnable() {
            @Override
            public void run() {
                if (regFuture.isSuccess()) {
                    channel.bind(localAddress, promise).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
                } else {
                    promise.setFailure(regFuture.cause());
                }
            }
        });
    }
```


### Bootstrap

Bootstrap以connect开始，connect有多个方法，最终会调用如下方法：
```
public ChannelFuture connect(SocketAddress remoteAddress) {
        if (remoteAddress == null) {
            throw new NullPointerException("remoteAddress");
        }
        validate();
        return doResolveAndConnect(remoteAddress, config.localAddress());
    }
```
**connect > doResolveAndConnect**
1.调用initAndRegister()，与ServerBootstrap的initAndRegister方法一样，主要是为了初始化channel，并注册到线程池和pipeline中。不同的是init方不同，Bootstrap的init方法只需要设置option和attr。
2.调用doResolveAndConnect0方法

```
private ChannelFuture doResolveAndConnect(final SocketAddress remoteAddress, final SocketAddress localAddress) {
        final ChannelFuture regFuture = initAndRegister();
        final Channel channel = regFuture.channel();

        if (regFuture.isDone()) {//如果注册成功
            if (!regFuture.isSuccess()) {//注册失败
                return regFuture;
            }
            return doResolveAndConnect0(channel, remoteAddress, localAddress, channel.newPromise());
        } else { //还未完成
            // Registration future is almost always fulfilled already, but just in case it's not.
            final PendingRegistrationPromise promise = new PendingRegistrationPromise(channel);
            regFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    
                    Throwable cause = future.cause();
                    if (cause != null) {
                        promise.setFailure(cause);
                    } else {
                        promise.registered();
                        doResolveAndConnect0(channel, remoteAddress, localAddress, promise);
                    }
                }
            });
            return promise;
        }
    }
```

**connect > doResolveAndConnect > doResolveAndConnect0**
1.获取线程池的地址转换器，如果支持远程地址或意见解析调用doConnect
2.解析未完成，则加入监听器，解析完成后调用doConnect
```
 private ChannelFuture doResolveAndConnect0(final Channel channel, SocketAddress remoteAddress,
       final SocketAddress localAddress, final ChannelPromise promise) {
        try {
            final EventLoop eventLoop = channel.eventLoop();
            //resolver 用来将eventloop转为地址
            final AddressResolver<SocketAddress> resolver = this.resolver.getResolver(eventLoop);
            //如果
            if (!resolver.isSupported(remoteAddress) || resolver.isResolved(remoteAddress)) {
                doConnect(remoteAddress, localAddress, promise);
                return promise;
            }
            //reoslver的
            final Future<SocketAddress> resolveFuture = resolver.resolve(remoteAddress);
            if (resolveFuture.isDone()) {
                final Throwable resolveFailureCause = resolveFuture.cause();

                if (resolveFailureCause != null) {
                    // Failed to resolve immediately
                    channel.close();
                    promise.setFailure(resolveFailureCause);
                } else {
                    // Succeeded to resolve immediately; cached? (or did a blocking lookup)
                    doConnect(resolveFuture.getNow(), localAddress, promise);
                }
                return promise;
            }

            // Wait until the name resolution is finished.
            resolveFuture.addListener(new FutureListener<SocketAddress>() {
                @Override
                public void operationComplete(Future<SocketAddress> future) throws Exception {
                    if (future.cause() != null) {
                        channel.close();
                        promise.setFailure(future.cause());
                    } else {
                        doConnect(future.getNow(), localAddress, promise);
                    }
                }
            });
        } catch (Throwable cause) {
            promise.tryFailure(cause);
        }
        return promise;
    }
```
**channel > doConnect**
将连接任务提交到线程池，内部调用channel的connect方法。
```
private static void doConnect(
            final SocketAddress remoteAddress, final SocketAddress localAddress, final ChannelPromise connectPromise) {
        final Channel channel = connectPromise.channel();
        channel.eventLoop().execute(new Runnable() {
            @Override
            public void run() {
                if (localAddress == null) {
                    channel.connect(remoteAddress, connectPromise);
                } else {
                    //channel的connect
                    channel.connect(remoteAddress, localAddress, connectPromise);
                }
                connectPromise.addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            }
        });
    }
```
channel的connection方法会调用pipeline的connect方法，会从tail出发，查找outbound，，最终达到HeadContext，调用HeadContext的connect

```
//调用1.AbstractChannel中
public ChannelFuture connect(SocketAddress remoteAddress, ChannelPromise promise) {
        return pipeline.connect(remoteAddress, promise);
}
//调用2.pipeline的connect
public final ChannelFuture connect(SocketAddress remoteAddress, ChannelPromise promise) {
        return tail.connect(remoteAddress, promise);
    }
//调用3.channelHandler都被包装在AbstractChannelHandlerContext中，tail时内部类
public ChannelFuture connect(
            final SocketAddress remoteAddress, final SocketAddress localAddress, final ChannelPromise promise) {
        if (remoteAddress == null) {
            throw new NullPointerException("remoteAddress");
        }
        if (!validatePromise(promise, false)) {
            // cancelled
            return promise;
        }
        //1.从tail开始查找outbound，会调用其invokeConnect
        final AbstractChannelHandlerContext next = findContextOutbound();
        EventExecutor executor = next.executor();
        if (executor.inEventLoop()) {
            next.invokeConnect(remoteAddress, localAddress, promise);
        } else {
            safeExecute(executor, new Runnable() {
                @Override
                public void run() {
                    next.invokeConnect(remoteAddress, localAddress, promise);
                }
            }, promise, null);
        }
        return promise;
    }
//调用4.outbound的ChannelHandler的invokeConnect,最终会调用HeadContext的，内部会调用connect方法
private void invokeConnect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
        if (invokeHandler()) {
            try {
                ((ChannelOutboundHandler) handler()).connect(this, remoteAddress, localAddress, promise);
            } catch (Throwable t) {
                notifyOutboundHandlerException(t, promise);
            }
        } else {
            connect(remoteAddress, localAddress, promise);
        }
    }
//调用5.HeadContext的connect
public void connect(
                ChannelHandlerContext ctx,
                SocketAddress remoteAddress, SocketAddress localAddress,
                ChannelPromise promise) throws Exception {
            unsafe.connect(remoteAddress, localAddress, promise);
        }
```

***AbstractNioUnsafe***
 unsafe的connect是连接的主要实现：
 调用doConnect，
```
public final void connect(
final SocketAddress remoteAddress, final SocketAddress localAddress, final ChannelPromise promise) {
            if (!promise.setUncancellable() || !ensureOpen(promise)) {
                return;
            }
            try {
                if (connectPromise != null) {
                    // Already a connect in process.
                    throw new ConnectionPendingException();
                }

                boolean wasActive = isActive();
                //1.doConnect是抽象方法，
                if (doConnect(remoteAddress, localAddress)) {
                    fulfillConnectPromise(promise, wasActive);
                } else {
                    connectPromise = promise;
                    requestedRemoteAddress = remoteAddress;

                    // Schedule connect timeout.
                    int connectTimeoutMillis = config().getConnectTimeoutMillis();
                    if (connectTimeoutMillis > 0) {
                        connectTimeoutFuture = eventLoop().schedule(new Runnable() {
                            @Override
                            public void run() {
                                ChannelPromise connectPromise = AbstractNioChannel.this.connectPromise;
                                ConnectTimeoutException cause =
                                        new ConnectTimeoutException("connection timed out: " + remoteAddress);
                                if (connectPromise != null && connectPromise.tryFailure(cause)) {
                                    close(voidPromise());
                                }
                            }
                        }, connectTimeoutMillis, TimeUnit.MILLISECONDS);
                    }

                    promise.addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
                            if (future.isCancelled()) {
                                if (connectTimeoutFuture != null) {
                                    connectTimeoutFuture.cancel(false);
                                }
                                connectPromise = null;
                                close(voidPromise());
                            }
                        }
                    });
                }
            } catch (Throwable t) {
                promise.tryFailure(annotateConnectException(t, remoteAddress));
                closeIfClosed();
            }
        }
```

***NioSocketChannel > doConnect的实现***
如果设置了本地地址，则绑定；
调用javachannel的connect方，这是一个非阻塞方法，连接失败会返回false，然后使用注册是调用的doRegister返回的selectKey，为其设置SelectionKey.OP_CONNECT事件。
后续EventLoop会不断select，如果连接完毕，会对连接事件进行处理。
根据nio的api，如果此通道处于非阻塞模式，则调用此方法会发起一个非阻塞连接操作。如果立即建立连接（使用本地连接时就是如此），则此方法返回 true。否则此方法返回 false，并且必须在以后通过调用 finishConnect 方法来完成该连接操作。 

```
protected boolean doConnect(SocketAddress remoteAddress, SocketAddress localAddress) throws Exception {
        if (localAddress != null) {//1.如果设置了本地地址，则绑定
            doBind0(localAddress);
        }
        boolean success = false;
        try {
            //2.连接
            boolean connected = javaChannel().connect(remoteAddress);
            if (!connected) {//3.将此键的interest集合设置为给定值
                selectionKey().interestOps(SelectionKey.OP_CONNECT);
            }
            success = true;
            return connected;
        } finally {
            if (!success) {
                doClose();
            }
        }
    }
//doRegister时候注册的
selectionKey = javaChannel().register(eventLoop().selector, 0, this);
```

### NioEventLoop获取连接事件

NioEventLoop的细节不在此进行叙述，其会一直不断的select获取准备就绪的通道，由于上面我们已经注册了OP_CONNECTION，连接成功会调用processSelectedKeys();方法，由于netty默认会对select进行优化，会调用processSelectedKeysOptimized
```
if (a instanceof AbstractNioChannel) {
    processSelectedKey(k, (AbstractNioChannel) a);
} 
```

**processSelectedKey处理select的所有事件**
首先判断是否有效，键在创建时是有效的，并在被取消、其通道已关闭或者其选择器已关闭之前保持有效。 
清除key的OP_CONNECT事件，调用unsafe的finishConnect，会完成连接操作
```
private void processSelectedKey(SelectionKey k, AbstractNioChannel ch) {
        final AbstractNioChannel.NioUnsafe unsafe = ch.unsafe();
        //键在创建时是有效的，并在被取消、其通道已关闭或者其选择器已关闭之前保持有效。 
        if (!k.isValid()) {
            final EventLoop eventLoop;
            try {
                eventLoop = ch.eventLoop();
            } catch (Throwable ignored) {
                return;
            }
            if (eventLoop != this || eventLoop == null) {
                return;
            }
            unsafe.close(unsafe.voidPromise());
            return;
        }

        try {
            int readyOps = k.readyOps();//获取此键的 ready 操作集合
            //连接
            if ((readyOps & SelectionKey.OP_CONNECT) != 0) {
                //将key的OP_CONNECT清除
                int ops = k.interestOps();
                ops &= ~SelectionKey.OP_CONNECT;
                k.interestOps(ops);
                unsafe.finishConnect();
            }

            //写操作
            if ((readyOps & SelectionKey.OP_WRITE) != 0) {
                ch.unsafe().forceFlush();
            }
            //读或接受连接
            if ((readyOps & (SelectionKey.OP_READ | SelectionKey.OP_ACCEPT)) != 0 || readyOps == 0) {
                unsafe.read();
                if (!ch.isOpen()) {
                    return;
                }
            }
        } catch (CancelledKeyException ignored) {
            unsafe.close(unsafe.voidPromise());
        }
    }
```

**finishConnect 完成连接**
逻辑为：调用javaChannel().finishConnect()完成连接，通过promise连接完成。
```
public final void finishConnect() {
            assert eventLoop().inEventLoop();

            try {
                boolean wasActive = isActive();
                doFinishConnect();//调用!javaChannel().finishConnect()
                fulfillConnectPromise(connectPromise, wasActive);
            } catch (Throwable t) {
                fulfillConnectPromise(connectPromise, annotateConnectException(t, requestedRemoteAddress));
            } finally {
                // Check for null as the connectTimeoutFuture is only created if a connectTimeoutMillis > 0 is used
                // See https://github.com/netty/netty/issues/1770
                if (connectTimeoutFuture != null) {
                    connectTimeoutFuture.cancel(false);
                }
                connectPromise = null;
            }
        }
```
**通知promise连接完成**
会触发pipeline的fireChannelActive事件
```
private void fulfillConnectPromise(ChannelPromise promise, boolean wasActive) {
            if (promise == null) {
                return;
            }
            boolean active = isActive();
            boolean promiseSet = promise.trySuccess();
            if (!wasActive && active) {
                pipeline().fireChannelActive();
            }

            // If a user cancelled the connection attempt, close the channel, which is followed by channelInactive().
            if (!promiseSet) {
                close(voidPromise());
            }
        }
```

如此，client端连接完成。

### 服务器端连接
根据上面的分析，会执行unsafe.read()，对于NioServerSocketChannel来说，其unsafe为NioMessageUnsafe,其逻辑为：
* 获取RecvByteBufAllocator.Handle，时AdaptiveRecvByteBufAllocator.HandleImpl,并重置参数
* 调用doReadMessages，主要是调用javachannel的accept方法
* 调用接受缓冲分配器的incMessagesRead，将totalMessages加1，继续读，直到没有数据
* 触发pipeline的ChannelRead方法
* 清除缓存，触发readComplete()
* 
```
public void read() {
            assert eventLoop().inEventLoop();
            final ChannelConfig config = config();
            final ChannelPipeline pipeline = pipeline();
            //1. 获取分配处理器
            final RecvByteBufAllocator.Handle allocHandle = unsafe().recvBufAllocHandle();
            //1.1.重置相关参数
            allocHandle.reset(config);

            boolean closed = false;
            Throwable exception = null;
            try {
                try {
                    do {// 一直读，知道没有数据了
                        //2.将read的消息读取到readBuf
                        int localRead = doReadMessages(readBuf);
                        if (localRead == 0) {
                            break;
                        }
                        if (localRead < 0) {
                            closed = true;
                            break;
                        }
                        //3. 调用处理器的incMessagesRead
                        allocHandle.incMessagesRead(localRead);
                    } while (allocHandle.continueReading());
                } catch (Throwable t) {
                    exception = t;
                }

                int size = readBuf.size();
                for (int i = 0; i < size; i ++) {
                    readPending = false;
                    //4.触发pipeline的channelRead()
                    pipeline.fireChannelRead(readBuf.get(i));
                }
                readBuf.clear();//清除buf
                allocHandle.readComplete();
                //5.触发readComplete()
                pipeline.fireChannelReadComplete();

                if (exception != null) {
                    closed = closeOnReadError(exception);

                    pipeline.fireExceptionCaught(exception);
                }

                if (closed) {
                    inputShutdown = true;
                    if (isOpen()) {
                        close(voidPromise());
                    }
                }
            } finally {
                if (!readPending && !config.isAutoRead()) {
                    removeReadOp();
                }
            }
        }
```

***RecvByteBufAllocator.Handle***
unsafe的recvBufAllocHandle会获取启动器的config的请求缓存分配器，如果不知道的话，默认为AdaptiveRecvByteBufAllocator，newHandle()会返回一个AdaptiveRecvByteBufAllocator.HandleImpl
```
public RecvByteBufAllocator.Handle recvBufAllocHandle() {
            if (recvHandle == null) {
                recvHandle = config().getRecvByteBufAllocator().newHandle();
            }
            return recvHandle;
        }
//默认设置了AdaptiveRecvByteBufAllocator
public DefaultChannelConfig(Channel channel) {
        this(channel, new AdaptiveRecvByteBufAllocator());
    }
//创建处理器
public Handle newHandle() {
        return new HandleImpl(minIndex, maxIndex, initial);
    }
```

***doReadMessages***
调用javachannel的accept方法接收请求，加入buf
```
protected int doReadMessages(List<Object> buf) throws Exception {
        //接受请求，如果无连接，会返回null，有则阻塞返回channel
        SocketChannel ch = javaChannel().accept();

        try {
            if (ch != null) {//不为空，则创建NioSocketChannel，加入buf
                buf.add(new NioSocketChannel(this, ch));
                return 1;
            }
        } catch (Throwable t) {
            logger.warn("Failed to create a new channel from an accepted socket.", t);

            try {
                ch.close();
            } catch (Throwable t2) {
                logger.warn("Failed to close a socket.", t2);
            }
        }

        return 0;
    }
```




## 3. 总结

过程图1
过程图2

## 4. 补充

### AddressResolverGroup

### Pipeline及ChanndelHandler


