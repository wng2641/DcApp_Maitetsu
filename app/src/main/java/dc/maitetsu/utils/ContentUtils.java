package dc.maitetsu.utils;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import dc.maitetsu.R;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.URL;
import java.util.Map;


/**
 * @since 2017-04-24
 * ㅋ
 * 이미지 로드를 처리하는 클래스.
 */
public class ContentUtils {


  public static void clearContext(final Context context) {
    ThreadPoolManager.getServiceEc().submit(new Runnable() {
      @Override
      public void run() {
        Glide.get(context).clearDiskCache();
        Glide.get(context).clearMemory();
      }
    });
  }

  /**
   * File 객체를 입력받아 액티비티의 이미지뷰에
   * 이미지를 보여주는 메소드
   *
   * @param activity  the activity
   * @param file      the file
   * @param imageView the image view
   */
  public static void loadBitmapFromLocal(final Activity activity,
                                         final File file,
                                         final ImageView imageView) {
    ThreadPoolManager.getServiceEc().submit(new Runnable() {
      @Override
      public void run() {
        try {
          MainUIThread.setImageView(activity, imageView, IOUtils.toByteArray(new FileInputStream(file)));
        } catch (Exception e) {
          MainUIThread.showToast(activity, activity.getString(R.string.image_load_failure));
        }
      }
    });

  }


  /**
   * URL로부터 이미지를 얻어 액티비티의 이미지뷰에 보여주는 메소드
   *
   * @param activity  the activity
   * @param url       the url
   * @param imageView the image view
   */
  public static void loadBitmapFromUrl(final Activity activity,
                                       final int position,
                                       final Map<Integer, byte[]> imageBytes,
                                       final String url,
                                       final ImageView imageView) {

    ThreadPoolManager.getServiceEc().submit(new Runnable() {
      @Override
      public void run() {
        try {
          InputStream in = (InputStream) new URL(url).getContent();
          byte[] bytes = IOUtils.toByteArray(in);
          if (imageBytes != null) imageBytes.put(position, bytes);
          MainUIThread.setImageView(activity, imageView, bytes);
          in.close();
        } catch (Exception e) {
          if (!e.getMessage().equals("thread interrupted")) // 쓰레드 인터럽트 예외는 무시
            MainUIThread.showToast(activity, activity.getString(R.string.image_load_failure));
        }
      }
    });
  }


  /**
   * 이미지뷰에서 사용된다.
   * 바이트 배열을 이미지처리한다.
   * 개별 쓰레드 사용됨.
   *
   * @param activity  the activity
   * @param bytes     the bytes
   * @param imageView the image view
   */
  public static void loadBitMap(final Activity activity,
                                final byte[] bytes,
                                final ImageView imageView) {

    ThreadPoolManager.getImageViewEc().submit(new Runnable() {
      @Override
      public void run() {
        try {
          MainUIThread.setImageViewOriginalSize(activity, imageView, bytes);
        } catch (Exception e) {
          if (!e.getMessage().equals("thread interrupted")) // 쓰레드 인터럽트 예외는 무시
            MainUIThread.showToast(activity, activity.getString(R.string.image_load_failure));
        }
      }
    });
  }


  /**
   * 먼저 저장된 이미지인지 체크하고 없으면 URL에서 받아 저장한 후
   * 액티비티의 이미지뷰에 보여주는 메소드
   *
   * @param activity  the activity
   * @param url       the url
   * @param imageView the image view
   */
  public static void loadBitmapFromUrlWithLocalCheck(final Activity activity,
                                                     final String url,
                                                     final ImageView imageView) {
    ThreadPoolManager.getServiceEc().submit(new Runnable() {
      @Override
      public void run() {
        try {
          byte[] bytes = getOrSave(activity, url);
          MainUIThread.setImageView(activity, imageView, bytes);
        } catch (Exception e) {
          if (!e.getMessage().equals("thread interrupted")) // 쓰레드 인터럽트 예외는 무시
            MainUIThread.showToast(activity, activity.getString(R.string.image_load_failure));
        }
      }
    });

  }

  // 로컬에 있으면 그 파일을, 없으면 저장하고 파일을 가져오는 메소드
  private static byte[] getOrSave(Activity activity, String url) throws IOException {
    String name = String.valueOf(url.split("=")[1]);
    File file = new File(activity.getApplicationContext().getFilesDir(), name);
    if (file.exists()) {
      return FileUtils.readFileToByteArray(new File(activity.getApplicationContext().getFilesDir(), name));
    } else {
      try {
        byte buffer[] = new byte[1024];
        int length;
        InputStream in = (InputStream) new URL(url).getContent();
        DataInputStream dis = new DataInputStream(in);
        FileOutputStream fos = new FileOutputStream(file);
        while ((length = dis.read(buffer)) > 0) {
          fos.write(buffer, 0, length);
        }
        fos.flush();
        fos.close();
        dis.close();
        in.close();
        return FileUtils.readFileToByteArray(file);
      } catch (Exception e) {
        Log.e("err", e.getMessage() + "f");
        throw e;
      }
    }
  }

}