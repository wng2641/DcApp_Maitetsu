package dc.maitetsufd.service;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import dc.maitetsufd.R;
import dc.maitetsufd.data.CurrentData;
import dc.maitetsufd.data.CurrentDataManager;
import dc.maitetsufd.enums.RequestCodes;
import dc.maitetsufd.enums.ResultCodes;
import dc.maitetsufd.models.*;
import dc.maitetsufd.ui.ArticleWriteActivity;
import dc.maitetsufd.ui.MainActivity;
import dc.maitetsufd.ui.SplashActivity;
import dc.maitetsufd.ui.fragment.GalleryListFragment;
import dc.maitetsufd.ui.fragment.RecommendArticleListFragment;
import dc.maitetsufd.ui.viewmodel.ArticleDetailViewModel;
import dc.maitetsufd.ui.viewmodel.HasViewModelFragment;
import dc.maitetsufd.utils.MainUIThread;
import dc.maitetsufd.utils.ThreadPoolManager;
import dc.maitetsufd.utils.ContentFilter;

import java.io.File;
import java.util.List;

/**
 * @since 2017-04-21
 */
public class ServiceProvider {

  // 갤럭시 S6 Useragent
  public static String USER_AGENT = "Mozilla/5.0 (Linux; Android 7.0; PLUS Build/NRD90M) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.98 Mobile Safari/537.36";

  private static ServiceProvider serviceProvider = null;


  private ServiceProvider() {
  }

  /**
   * service provider 객체를 리턴하는 메소드
   *
   * @return the service provider
   */
  public static ServiceProvider getInstance() {

    if (serviceProvider == null) {
      serviceProvider = new ServiceProvider();
    }
    return serviceProvider;
  }


  /**
   * 로그인하고 정보를 얻어오는 메소드
   *
   * @param activity 결과를 띄울 액티비티
   */
  public void login(final SplashActivity activity, final boolean resetMode) {

    ThreadPoolManager.getServiceEc().submit(new Runnable() {
      @Override
      public void run() {
        CurrentData currentData = CurrentDataManager.load(activity);

        // 빠른 로그인 사용시 이전 정보를 그대로 사용함
        if (!resetMode && currentData.isFastLogin()
                && currentData.getLoginCookies().get("sso_token_dcinside") != null) {

          if (isLoginCookieUseable(currentData)) {
            MainUIThread.setSplashText(activity, activity.getString(R.string.splash_login_keep));
            MainUIThread.finishActivity(activity, ResultCodes.LOGIN_SUCCESS);
            return;
          }
        }


        try {
          MainUIThread.setSplashText(activity, activity.getString(R.string.splash_login_try));
          currentData.setLoginCookies(LoginService.getInstance.login(currentData.getId(), currentData.getPw(), USER_AGENT));
          currentData.setLastLogin(System.currentTimeMillis());
          CurrentDataManager.save(activity);
          MainUIThread.setSplashText(activity, activity.getString(R.string.splash_login_success));

          getDcConList(activity, currentData);
          CurrentData.resetMode = false;

        } catch (Exception e) {
          String msg = activity.getString(R.string.login_failure) + "\n";
          if (e instanceof IllegalAccessException) msg = e.getMessage();
          else msg += "다시 시도해 보세요";

          MainUIThread.showToast(activity, msg);
          MainUIThread.finishActivity(activity, ResultCodes.LOGIN_FAIL);
        }
      }
    });

  }


  public void openArticleDetail(final HasViewModelFragment fragment,
                                final String articleUrl) {
    ThreadPoolManager.getServiceEc().submit(new Runnable() {
      @Override
      public void run() {
        CurrentData currentData = getCurrentData(fragment.getActivity().getApplicationContext());
        try {
          ArticleDetail articleDetail = ArticleDetailService.getInstance
                                                .getArticleDetail(currentData.getLoginCookies()
                                                                , USER_AGENT, articleUrl);
          articleDetail.setUrl(articleUrl);
          ContentFilter.setComments(currentData, articleDetail.getComments());
          MainUIThread.openArticleDetail(fragment, articleDetail, articleUrl);
        } catch (Exception e) {
          fragment.getHasAdapterViewModel().stopRefreshing();
          Log.e("err", e.getMessage());
          MainUIThread.showToast(fragment.getActivity(),
                  fragment.getActivity().getString(R.string.article_load_failure));
        }
      }
    });
  }

  public void refreshArticleDetail(final Activity activity,
                                final String articleUrl) {
    ThreadPoolManager.getServiceEc().submit(new Runnable() {
      @Override
      public void run() {
        CurrentData currentData = getCurrentData(activity.getApplicationContext());
        try {
          ArticleDetail articleDetail = ArticleDetailService.getInstance
															.getArticleDetail(currentData.getLoginCookies() , USER_AGENT, articleUrl);
          articleDetail.setUrl(articleUrl);
          ContentFilter.setComments(currentData, articleDetail.getComments());
          MainUIThread.finishActivity(activity, ResultCodes.NONE);
          MainUIThread.openArticleDetail(activity, articleDetail, articleUrl);
        } catch (Exception e) {
          MainUIThread.showToast(activity, activity.getString(R.string.article_load_failure));
        }
      }
    });
  }



  public void refreshComment(final Activity activity,
                             final ArticleDetailViewModel viewModel,
                             final String articleUrl) {
    ThreadPoolManager.getServiceEc().submit(new Runnable() {
      @Override
      public void run() {
        CurrentData currentData = getCurrentData(activity.getApplicationContext());
        try {
          ArticleDetail articleDetail = ArticleDetailService.getInstance
                  .getArticleDetail(currentData.getLoginCookies()
                                  , USER_AGENT, articleUrl);
          ContentFilter.setComments(currentData, articleDetail.getComments());
          MainUIThread.refreshComment(activity, viewModel, articleDetail.getComments());
        } catch (Exception e) {
          MainUIThread.showToast(activity, activity.getString(R.string.comment_reload_failure));
        }
      }
    });
  }


  public void searchGalleryName(final GalleryListFragment fragment, final String galleryName) {
    ThreadPoolManager.getServiceEc().submit(new Runnable() {
      @Override
      public void run() {
        try {
          List<GalleryInfo> galleryInfos = GalleryListService.getInstance.searchGallery(USER_AGENT, galleryName);
          MainUIThread.setGallerySearchResult(galleryInfos, fragment);
        } catch (Exception e) {
          MainUIThread.showToast(fragment.getActivity(), fragment.getString(R.string.gallery_search_failure));
        }
      }
    });
  }

  /**
   * SimpleArticle을 얻어오는 메소드.
   * Fragment의 종류에 따라 일반글/개념글 구분한다.
   *
   * @param fragment the fragment
   */
  public void getSimpleArticles(final HasViewModelFragment fragment) {
    ThreadPoolManager.getServiceEc().submit(new Runnable() {
      @Override
      public void run() {


        try {
          CurrentData currentData = CurrentDataManager.getInstance(fragment.getActivity());
          boolean isRecommend = fragment instanceof RecommendArticleListFragment;
          boolean showSnackBar = true;
          List<SimpleArticle> simpleArticles = SimpleArticleService.getInstance
                                                .getSimpleArticles(currentData,
                                                            USER_AGENT, isRecommend);
          ContentFilter.setSimpleArticles(currentData, simpleArticles);

          if (simpleArticles.size() == 0) {
            if (currentData.getLoginCookies().get("sso_token_dcinside") == null ||
                    !isLoginCookieUseable(currentData)) {
              restartApplication(fragment);
              return;
            }
            MainUIThread.showToast(fragment.getActivity(), fragment.getActivity().getString(R.string.article_list_zero));
            currentData.setPage(currentData.getPage() - 1);
            showSnackBar = false;
          }

          currentData.setLastLogin(System.currentTimeMillis());
          MainUIThread.setArticleListView(fragment, simpleArticles, showSnackBar);

        } catch (Exception e1) {
          MainUIThread.showToast(fragment.getActivity(), fragment.getActivity().getString(R.string.article_list_failure));
          fragment.getHasAdapterViewModel().stopRefreshing();
        }
      }
    });
  }

  private void restartApplication(HasViewModelFragment fragment) {
    MainUIThread.showToast(fragment.getActivity(), fragment.getActivity().getString(R.string.splash_login_expire));
    MainActivity activity = (MainActivity) fragment.getActivity();
    activity.callSplashActivity();
  }

  public void writeArticle(final ArticleWriteActivity activity,
                           final Button writeButton,
                           final String title,
                           final String content,
                           final List<File> files,
                           final ArticleModify articleModify) {
    ThreadPoolManager.getServiceEc().submit(new Runnable() {
      @Override
      public void run() {
        MainUIThread.setViewState(activity, writeButton, false);
        CurrentData currentData = getCurrentData(activity.getApplicationContext());
        try {
          String result = ArticleWriteService.getInstance.write(currentData.getLoginCookies(),
                                                                currentData.getGalleryInfo().getGalleryCode(),
                                                                files, USER_AGENT, title, content, articleModify);
          if (result != null
              && !result.contains("Warning")
              && !result.trim().isEmpty()) throw new Exception(result);
          else {
            MainUIThread.showToast(activity, activity.getString(R.string.article_write_complete));
            MainUIThread.finishActivity(activity, ResultCodes.ARTICLE_REFRESH);
          }

        } catch (IllegalAccessException ie) {
          MainUIThread.showToast(activity, activity.getString(R.string.article_write_image_upload_failure) + ie.getMessage());

        } catch (Exception e) {
          MainUIThread.showToast(activity, activity.getString(R.string.article_write_failure) + e.getMessage());
        }
        MainUIThread.setViewState(activity, writeButton, true);

      }
    });
  }

  public void recommendArticle(final ArticleDetail articleDetail,
                               final Activity activity) {
    ThreadPoolManager.getServiceEc().submit(new Runnable() {
      @Override
      public void run() {
        CurrentData currentData = getCurrentData(activity.getApplicationContext());

        try {
          if (RecommendService.getInstance.recommend(currentData.getLoginCookies(), USER_AGENT, articleDetail)) {
            MainUIThread.showToast(activity, activity.getString(R.string.article_read_recommend_success));
            currentData.getRecommendList().put(articleDetail.getArticleDeleteData().getNo(), System.currentTimeMillis());
            CurrentDataManager.save(activity);
          } else throw new Exception();
        } catch (Exception e) {
          MainUIThread.showToast(activity, activity.getString(R.string.article_read_recommend_failure));
        }
      }
    });
  }

  public void noRecommendArticle(final ArticleDetail articleDetail,
                               final Activity activity) {
    ThreadPoolManager.getServiceEc().submit(new Runnable() {
      @Override
      public void run() {
        CurrentData currentData = getCurrentData(activity.getApplicationContext());

        try {
          if (RecommendService.getInstance.norecommend(currentData.getLoginCookies(), USER_AGENT, articleDetail)) {
            MainUIThread.showToast(activity, activity.getString(R.string.article_read_norecommend_success));
            currentData.getNoRecommendList().put(articleDetail.getArticleDeleteData().getNo(), System.currentTimeMillis());
            CurrentDataManager.save(activity);
          } else throw new Exception();
        } catch (Exception e) {
          MainUIThread.showToast(activity, activity.getString(R.string.article_read_norecommend_failure));
        }
      }
    });
  }


  public void deleteArticle(final View deleteButton,
                            final ArticleDetail articleDetail,
                            final Activity activity) {
    ThreadPoolManager.getServiceEc().submit(new Runnable() {
      @Override
      public void run() {
        try {
          MainUIThread.setViewState(activity, deleteButton, false);
          CurrentData currentData = CurrentDataManager.getInstance(activity.getApplicationContext());
          if (ArticleDeleteService.getInstance
                  .delete(currentData.getLoginCookies(), USER_AGENT, articleDetail)) {
            MainUIThread.finishActivity(activity, ResultCodes.ARTICLE_REFRESH);
            MainUIThread.showToast(activity, activity.getString(R.string.article_read_delete_success));
          } else throw new Exception();
        } catch (Exception e) {
          MainUIThread.showToast(activity, activity.getString(R.string.article_read_delete_failure));
        }
        MainUIThread.setViewState(activity, deleteButton, true);
      }
    });
  }


  public void deleteComment(final ArticleDetail articleDetail,
                            final ArticleDetailViewModel viewModel,
                            final String articleUrl,
                            final String deleteNo,
                            final Activity activity) {
    ThreadPoolManager.getServiceEc().submit(new Runnable() {
      @Override
      public void run() {
        CurrentData currentData = getCurrentData(activity.getApplicationContext());
        try {
          if (CommentDeleteService.getInstance
                  .delete(currentData.getLoginCookies(), USER_AGENT, articleDetail, deleteNo)) {
            MainUIThread.showToast(activity, activity.getString(R.string.comment_delete_success));
            refreshComment(activity, viewModel, articleUrl);
          } else throw new Exception();

        } catch (Exception e) {
          MainUIThread.showToast(activity, activity.getString(R.string.comment_delete_failure));
        }
      }
    });
  }


  /**
   * 댓글 추가 요청
   *
   * @param articleDetail the article detail
   * @param comment       the comment
   * @param button        the button
   * @param activity      the activity
   */
  public void writeComment(final ArticleDetail articleDetail,
                           final ArticleDetailViewModel viewModel,
                           final String articleUrl,
                           final String comment,
                           final EditText commentEditText,
                           final View button,
                           final Activity activity) {
    ThreadPoolManager.getServiceEc().submit(new Runnable() {
      @Override
      public void run() {
        MainUIThread.setViewState(activity, button, false);
        CurrentData currentData = getCurrentData(activity.getApplicationContext());
        try {
          if (CommentWriteService.getInstance
                  .write(currentData.getLoginCookies(), USER_AGENT, articleDetail, comment, "")) {

            MainUIThread.showToast(activity, activity.getString(R.string.comment_submit_success));
            refreshComment(activity, viewModel, articleUrl);

          } else throw new Exception();
        } catch (IllegalAccessException ie) {
          MainUIThread.showToast(activity, activity.getString(R.string.comment_submit_failure) + " " + ie.getMessage());
          commentEditText.setText(comment);

        } catch (Exception e) {
          MainUIThread.showToast(activity, activity.getString(R.string.comment_submit_failure));
          activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
              commentEditText.setText(comment);

            }
          });

        }
        MainUIThread.setViewState(activity, button, true);
      }
    });
  }

  public void writeDcconComment(final Activity activity,
                                final ArticleDetailViewModel viewModel,
                                final View blockedView,
                                final ArticleDetail articleDetail,
                                final String articleUrl,
                                final DcConPackage.DcCon dcCon) {
    ThreadPoolManager.getServiceEc().submit(new Runnable() {
      @Override
      public void run() {
        MainUIThread.setViewState(activity, blockedView, false);
        CurrentData currentData = getCurrentData(activity.getApplicationContext());
        try {
          if (CommentWriteService.getInstance
                  .writeDcCon(currentData.getLoginCookies(), USER_AGENT, articleDetail, dcCon)) {
            MainUIThread.showToast(activity, activity.getString(R.string.comment_dccon_submit_success));
            refreshComment(activity, viewModel, articleUrl);
          } else throw new Exception();
        } catch (IllegalAccessException ie) {
          MainUIThread.showToast(activity, ie.getMessage());
        } catch (Exception e) {
          MainUIThread.showToast(activity, activity.getString(R.string.comment_dccon_submit_failure));
        }
        MainUIThread.setViewState(activity, blockedView, true);
      }
    });
  }

  // 디시콘 리스트를 읽어오는 메소드.
  private void getDcConList(final SplashActivity activity, final CurrentData currentData) {
    ThreadPoolManager.getServiceEc().submit(new Runnable() {
      @Override
      public void run() {
        try {
          MainUIThread.setSplashText(activity, activity.getString(R.string.dccon_list_load_try));
          currentData.setDcConPackages(DcConService.getInstance
                     .getDcConList(currentData.getLoginCookies(), USER_AGENT));
          currentData.setLastLogin(System.currentTimeMillis());
          CurrentDataManager.save(activity.getApplicationContext());

          MainUIThread.setSplashText(activity, activity.getString(R.string.dccon_list_load_success));
          MainUIThread.finishActivity(activity, ResultCodes.LOGIN_SUCCESS);
        } catch (Exception e) {
          MainUIThread.showToast(activity, activity.getString(R.string.dccon_list_load_failure));
          MainUIThread.finishActivity(activity, ResultCodes.LOGIN_FAIL);
        }

      }
    });
  }

  public void openArticleModify(final Activity activity,
                                final ArticleDetail articleDetail,
                                final CurrentData currentData) {
    ThreadPoolManager.getServiceEc().submit(new Runnable() {
      @Override
      public void run() {
        try {
          ArticleModify articleModify = ArticleModifyService.getInstance
                                          .getArticleModifyData(USER_AGENT,
                                                                articleDetail,
                                                                currentData.getLoginCookies());

          Intent intent = new Intent(activity, ArticleWriteActivity.class);
          intent.putExtra("articleModify", articleModify);
          activity.startActivityForResult(intent, RequestCodes.ARTICLE.ordinal());
        }catch(Exception e) {
          e.printStackTrace();
          MainUIThread.showToast(activity, activity.getString(R.string.article_modify_try_failure));
        }
      }
    });
  }





  // 마지막 로그인 시간이 2시간 이내라면 true를 반환함.
  private static boolean isLoginCookieUseable(CurrentData currentData) {
    long lastTime = currentData.getLastLogin();
    long loginKeepTime = lastTime + (1000 * 60) * 60 * 6; // 6시간
    long currentTime = System.currentTimeMillis();
    return currentTime < loginKeepTime;
  }

  private static CurrentData getCurrentData(Context context) {
    return CurrentDataManager.getInstance(context);
  }


}
