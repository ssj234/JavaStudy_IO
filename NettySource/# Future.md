# ChannelFuture

## 并发包的Future

```
boolean	cancel(boolean mayInterruptIfRunning) 试图取消对此任务的执行。
V	get() 如有必要，等待计算完成，然后获取其结果。
V	get(long timeout, TimeUnit unit) 如有必要，最多等待为使计算完成所给定的时间之后，获取其结果（如果结果可用）。
boolean	isCancelled() 如果在任务正常完成前将其取消，则返回 true。
boolean	isDone() 如果任务已完成，则返回 true。
```

## Netty的Future
```
public interface Future<V> extends java.util.concurrent.Future<V> 

  boolean isSuccess(); 只有当IO操作成功完成的时候回返回true
  boolean isCancellable(); 只有当cancel(boolean)成功取消时c 
  Throwable cause();  返回导致IO操作以此的原因，如果没有异常，返回null
  Future<V> addListener(GenericFutureListener<? extends Future<? super V>> listener);
  Future<V> removeListener(GenericFutureListener<? extends Future<? super V>> listener);
  Future<V> sync() throws InterruptedException;  等待future done
  Future<V> syncUninterruptibly();  等待future done，不可打断
  Future<V> await() throws InterruptedException;等待future 完成
  Future<V> awaitUninterruptibly();  等待future 完成，不可打断
  V getNow(); 立刻获得结果，如果没有完成，返回null
  boolean cancel(boolean mayInterruptIfRunning); 如果成功取消，future会失败，导致CancellationException
```

## Netty的Promise
Promise继承了Future接口，并添加了如下几个方法：
```
public interface Promise<V> extends Future<V> 

Promise<V> setSuccess(V result); 使future成功
boolean trySuccess(V result); 尝试使future成功
Promise<V> setFailure(Throwable cause);
boolean tryFailure(Throwable cause);
boolean setUncancellable(); 使得future不可取消
```

## Netty的ChannelFuture
ChannelFuture继承了Future，新增了两个方法：
```
Channel channel();
boolean isVoid();
```

我们一般使用Future的有:
```
DefaultChannelPromise
DefaultChannelProgressivePromise
```

## 例子

下面的例子中，read会立刻返回一个ChannelFuture ，ChannelFuture 可以添加监听事件，在ChannelFuture 执行完成后调用监听器的operationComplete方法。operationComplete的参数为ChannelFuture，可以通过isDone、isSuccess、isCancelled和cause()四个方法判断任务成功完成还是失败、是否取消等。
```
	

	public ChannelFuture read(){
		NioEventLoopGroup group=new NioEventLoopGroup();
		final DefaultChannelPromise promise=new DefaultChannelPromise(null,group.next());
		group.execute(new Runnable() {
			@Override
			public void run() {

				try {
					System.out.println("Read thread begin!");
					Thread.sleep(3000);
					System.out.println("Read thread end!");
					promise.setSuccess();
//					throw new InterruptedException();
				} catch (InterruptedException e) {
					promise.setFailure(e);
				}
			
			}
		});
		return promise;
	}
	
	public static void main(String[] args) throws InterruptedException {
		FutureTest ft=new FutureTest();
		System.out.println("Main thread begin!");
		ChannelFuture future=ft.read();
		future.addListener(new GenericFutureListener<Future<? super Void>>() {
			public void operationComplete(Future<? super Void> future) throws Exception {
				System.out.println("operationComplete");
				if(future.isDone() || future.cause() != null){
					System.out.println("Failed!");
				}else if(future.isDone() || future.isSuccess()){
					System.out.println("Success!");
				}
			};
		});
		System.out.println("Main thread end!");
	}

```