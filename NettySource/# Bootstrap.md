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
除此之外，还包括一个内部类ServerBootstrapAcceptor，这是一个ChannelHandler，会在register时加入Pipeline中。

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
上述代理，与ServerBootstrap不同，没有为childchannel，因为其channel就是于服务器的子channel

### 总结

启动器的准备阶段，主要是设置
* channel的线程组
* channel的类型、选项、属性、handler等


## 2.启动阶段

### ServerBootstrap

**>bind方法**
首先，来看看服务器启动器的启动代码，b.bind(port)，后续会调用doBind方法

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
register之前，加入链表，注册后调用执行initChannel方法
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




## 3. 补充

### AddressResolverGroup

### Pipeline及ChanndelHandler

### 


