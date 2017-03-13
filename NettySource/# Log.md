# Log

使用
```
prvate static final InternalLogger logger =
 InternalLoggerFactory.getInstance(MultithreadEventLoopGroup.class);
```

**解析**

InternalLogger是一个接口，封装了trace、info、error、debug、warn等方法，用来记录日志    
AbstractInternalLogger是一个抽象日志类，实现了InternalLogger接口中的部分方法，内部包含name变量
AbstractInternalLogger的子类有：


***CommonsLogger***
    内部实现了InternalLogger的方法，使用了一个`org.apache.commons.logging.Log logger`，MessageFormatter是格式化消息的工具类，格式化后的结果是FormattingTuple 
```
public void trace(String format, Object arg) {
        if (logger.isTraceEnabled()) {
            FormattingTuple ft = MessageFormatter.format(format, arg);
            logger.trace(ft.getMessage(), ft.getThrowable());
        }
    }
```
***JdkLogger***
    内部使用`java.util.logging.Logger logger`作为实际的日志记录器
```
@Override
    public void trace(String format, Object... argArray) {
        if (logger.isLoggable(Level.FINEST)) {
            FormattingTuple ft = MessageFormatter.arrayFormat(format, argArray);
            log(SELF, Level.FINEST, ft.getMessage(), ft.getThrowable());
        }
    }
```

***Log4J2Logger***
    内部使用`org.apache.logging.log4j.Logger logger`
```
//很奇怪没有先调用logger.isTraceEnabled();
public void trace(String format, Object... arguments) {
        logger.trace(format, arguments);
    }
```
***Log4JLogger***
    内部使用`org.apache.log4j.Logger logger`
```
public void trace(String format, Object... arguments) {
        if (isTraceEnabled()) {
            FormattingTuple ft = MessageFormatter.arrayFormat(format, arguments);
            logger.log(FQCN, traceCapable ? Level.TRACE : Level.DEBUG, ft
                    .getMessage(), ft.getThrowable());
        }
    }
```
***Slf4JLogger***
    内部使用`org.slf4j.Logger logger`
```
public void trace(String format, Object... argArray) {
    logger.trace(format, argArray);
}
```
以上这些记录日志类只是内部封装了不同的日志处理的具体类
InternalLogLevel表示日志等级，是一个枚举，TRACE,DEBUG,INFO,WARN,ERROR


InternalLoggerFactory是一个抽象的类，其子类有 ：
CommonsLoggerFactory
JdkLoggerFactory
Log4J2LoggerFactory
Log4JLoggerFactory
Slf4JLoggerFactory
每个factory对应一个上面的Logger，
```
InternalLoggerFactory类：

public static InternalLogger getInstance(Class<?> clazz) {
    return getInstance(clazz.getName());
}

public static InternalLogger getInstance(String name) {
    return getDefaultFactory().newInstance(name);
}

//默认的defaultFactory 调用newDefaultFactory获取
private static volatile InternalLoggerFactory defaultFactory =
            newDefaultFactory(InternalLoggerFactory.class.getName());
//创建Factory，先找Slf4J，再找Log4J，最后使用JdkLog
private static InternalLoggerFactory newDefaultFactory(String name) {
        InternalLoggerFactory f;
        try {
            f = new Slf4JLoggerFactory(true);
            f.newInstance(name).debug("Using SLF4J as the default logging framework");
        } catch (Throwable t1) {
            try {
                f = Log4JLoggerFactory.INSTANCE;
                f.newInstance(name).debug("Using Log4J as the default logging framework");
            } catch (Throwable t2) {
                f = JdkLoggerFactory.INSTANCE;
                f.newInstance(name).debug("Using java.util.logging as the default logging framework");
            }
        }
        return f;
    }
```




# 总结

对于不同的log，netty封装成了一个InteralLogger，并对每种日志封装了不同的工厂类，最后使用工厂类来创建InteralLogger的实例，让InteralLogger内部不同的log来记录日志

