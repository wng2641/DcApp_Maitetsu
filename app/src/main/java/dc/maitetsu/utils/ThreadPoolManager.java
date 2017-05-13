package dc.maitetsu.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * @since 2017-04-28
 *
 * 앱에서 사용되는 백그라운드 쓰레드.
 */
public class ThreadPoolManager {
  private static ExecutorService serviceEc;
  private static ExecutorService contentEc;
  private static ExecutorService imageViewEc;

  private static void setServiceEc() {
    serviceEc = Executors.newFixedThreadPool(2, new ThreadFactory() {
      @Override
      public Thread newThread(Runnable runnable) {
        Thread t = Executors.defaultThreadFactory().newThread(runnable);
        t.setDaemon(true);
        return t;
      }
    });
  }

  private static void setContentEc() {
    contentEc = Executors.newSingleThreadExecutor(new ThreadFactory() {
      @Override
      public Thread newThread(Runnable runnable) {
        Thread t = Executors.defaultThreadFactory().newThread(runnable);
        t.setDaemon(true);
        return t;
      }
    });
  }


  private static void setImageViewEc() {
    imageViewEc = Executors.newSingleThreadExecutor(new ThreadFactory() {
      @Override
      public Thread newThread(Runnable runnable) {
        Thread t = Executors.defaultThreadFactory().newThread(runnable);
        t.setDaemon(true);
        return t;
      }
    });
  }



  /**
   * 글 목록 / 글 내용을 부르는 ServiceEc
   * @return the service ec
   */
  public static ExecutorService getServiceEc() {
    if(serviceEc == null || serviceEc.isShutdown()) {
      setServiceEc();
    }
    return serviceEc;
  }

  /**
   * 글 내용을 UI에 그리기 전 처리하는 쓰레드
   * @return the content ec
   */
  public static ExecutorService getContentEc() {
    if(contentEc == null || contentEc.isShutdown()) {
      setContentEc();
    }
    return contentEc;
  }


  /**
   * 이미지뷰에서 사용하는 쓰레드
   * @return the image view ec ec
   */
  public static ExecutorService getImageViewEc() {
    if(imageViewEc == null || imageViewEc.isShutdown()) {
      setImageViewEc();
    }
    return imageViewEc;
  }


  /**
   * 글 목록을 읽거나 새로고침하는 쓰레드를 shutdown한다
   */
  public static void shutdownServiceEc() {
    try{
      serviceEc.shutdownNow();
    } catch(Exception e) {}
  }

  /**
   * 글 내용을 처리하는 쓰레드를 shutdown한다
   */
  public static void shutdownContentEc() {
    try {
      contentEc.shutdownNow();
    } catch(Exception e) {}
  }


  /**
   * 이미지뷰 쓰레드 종료
   */
  public static void shutdownImageViewEc() {
    try {
      imageViewEc.shutdownNow();
    } catch(Exception e) {}
  }




}
