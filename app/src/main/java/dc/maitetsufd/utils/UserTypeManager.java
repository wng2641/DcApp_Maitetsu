package dc.maitetsufd.utils;

import android.content.res.Resources;
import android.view.View;
import android.widget.ImageView;
import dc.maitetsufd.R;
import dc.maitetsufd.models.UserInfo;

/**
 * @since 2017-04-24
 *
 * 유저 타입을 설정해줌
 */
public class UserTypeManager {

  public static void set(Resources res, UserInfo userInfo, ImageView imageView){
    if(userInfo.getUserType().ordinal() <= UserInfo.UserType.FIX_GALLOG.ordinal()) {
      imageView.setImageDrawable(res.getDrawable(R.drawable.fix_gallog));
    }else if(userInfo.getUserType() == UserInfo.UserType.FLOW_GALLOG) {
      imageView.setImageDrawable(res.getDrawable(R.drawable.flow_gallog));
    }else if (userInfo.getUserType() == UserInfo.UserType.FLOW) {
      imageView.setImageDrawable(res.getDrawable(R.drawable.flow));
    } else {
      imageView.setVisibility(View.GONE);
    }
  }
}
