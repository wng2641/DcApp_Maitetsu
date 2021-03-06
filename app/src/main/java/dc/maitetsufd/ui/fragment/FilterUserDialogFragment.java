package dc.maitetsufd.ui.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import dc.maitetsufd.R;
import dc.maitetsufd.data.CurrentData;
import dc.maitetsufd.data.CurrentDataManager;
import dc.maitetsufd.models.UserInfo;
import dc.maitetsufd.utils.MainUIThread;

/**
 * @since 2017-04-25
 *
 * 유저 차단 확인 메시지 다이얼로그.
 *
 */
public class FilterUserDialogFragment extends DialogFragment {
  private UserInfo userInfo;
  private CurrentData currentData;

  public static FilterUserDialogFragment newInstance(UserInfo userInfo) {
    FilterUserDialogFragment fragment = new FilterUserDialogFragment();
    fragment.userInfo = userInfo;
    return fragment;
  }


  @Override
  public void onCreate(Bundle savedInstanceState) {
    currentData = CurrentDataManager.getInstance(getActivity());
    super.onCreate(savedInstanceState);
    if(currentData.isDarkTheme()) {
      setStyle(DialogFragment.STYLE_NORMAL, R.style.DarkTheme);
    }
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {

    AlertDialog.Builder builder;
    if( currentData.isDarkTheme()) builder = new AlertDialog.Builder(getActivity(), R.style.DarkTheme_DialogCustom);
    else builder= new AlertDialog.Builder(getActivity());

    builder.setMessage(R.string.filter_user_list_dialog)
            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int id) {

                if(userInfo.getUserType() == UserInfo.UserType.FLOW) {
                  userInfo.setNickname(userInfo.getIpAdd()
                                        .replaceAll("[()]", ""));
                }
                currentData.getFilterUserList().remove(userInfo);
                currentData.getFilterUserList().add(userInfo);
                CurrentDataManager.save(getActivity().getApplicationContext());

                String msg = getString(R.string.filter_user_list_dialog_success);
                MainUIThread.showToast(getActivity(), String.format(msg, userInfo.getNickname()));
              }
            })
            .setNegativeButton(R.string.cancle, new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int id) {
              }
            });

    return builder.create();
  }
}
