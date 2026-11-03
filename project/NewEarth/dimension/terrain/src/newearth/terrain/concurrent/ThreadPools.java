package newearth.terrain.concurrent;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
public final class ThreadPools {
 private static final AtomicInteger IDS=new AtomicInteger();
 public static final ExecutorService WORLD_GEN=Executors.newFixedThreadPool(availableProcessors(), task->{
  Thread thread=new Thread(task,"NewEarth terrain-"+IDS.incrementAndGet());thread.setDaemon(true);return thread;
 });
 public static int availableProcessors(){return Math.max(1,Math.min(4,Runtime.getRuntime().availableProcessors()-1));}
}
