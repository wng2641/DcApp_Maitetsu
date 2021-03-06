package dc.maitetsufd.ui.listener;

import android.app.Activity;
import android.content.Intent;
import android.util.SparseArray;
import android.view.View;
import dc.maitetsufd.data.ImageData;
import dc.maitetsufd.ui.ImageViewActivity;

/**
 * @since 2017-05-12
 */
public class ImageViewerListener {

  public static View.OnClickListener get(final Activity activity,
                                         final String name,
                                         final int imagePosition,
                                         final SparseArray<byte[]> imageBytes,
                                         final boolean hideStatusBar) {
    return new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Intent intent = new Intent(activity, ImageViewActivity.class);
        intent.putExtra("position", imagePosition);
        intent.putExtra("name", name);
        intent.putExtra("hideStatusBar", hideStatusBar);
        ImageData.setImageBytes(imageBytes);
        activity.startActivity(intent);
      }
    };
  }


}
