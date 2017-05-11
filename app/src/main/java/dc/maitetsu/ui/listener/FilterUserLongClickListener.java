package dc.maitetsu.ui.listener;

import android.app.Activity;
import android.view.View;
import dc.maitetsu.models.UserInfo;
import dc.maitetsu.ui.fragment.FilterUserDialogFragment;

/**
 * @author Park Hyo Jun
 * @since 2017-04-25
 *
 * 유저이름 길게 눌렀을 때 다이얼로그 여는 리스너
 *
 */
public class FilterUserLongClickListener {
  public static View.OnLongClickListener get(final Activity activity, final UserInfo userInfo) {
    return new View.OnLongClickListener() {
      @Override
      public boolean onLongClick(View view) {
        FilterUserDialogFragment.newInstance(userInfo)
                .show(activity.getFragmentManager(), "filterUserDialog");
        return true;
      }
    };
  }
}
