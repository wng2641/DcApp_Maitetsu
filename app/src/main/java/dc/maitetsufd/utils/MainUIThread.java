package dc.maitetsufd.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.github.chrisbanes.photoview.PhotoViewAttacher;
import dc.maitetsufd.R;
import dc.maitetsufd.data.CurrentData;
import dc.maitetsufd.data.CurrentDataManager;
import dc.maitetsufd.enums.RequestCodes;
import dc.maitetsufd.enums.ResultCodes;
import dc.maitetsufd.models.*;
import dc.maitetsufd.service.MaruServiceProvider;
import dc.maitetsufd.service.ServiceProvider;
import dc.maitetsufd.ui.ArticleDetailActivity;
import dc.maitetsufd.ui.MaruViewerDetailActivity;
import dc.maitetsufd.ui.SplashActivity;
import dc.maitetsufd.ui.fragment.GalleryListFragment;
import dc.maitetsufd.ui.fragment.MangaViewerFragment;
import dc.maitetsufd.ui.listener.ImageViewerListener;
import dc.maitetsufd.ui.viewmodel.*;

import java.io.*;
import java.util.List;

/**
 * @since 2017-04-22
 * UI 쓰레드에서 해야하는 작업을 정의한 클래스.
 */
public class MainUIThread {
  private static Toast toast;  // 이전에 띄운 토스트가 있다면 제거하고 출력한다.
  private static Snackbar snackbar;
  private static int IMAGE_LOAD_COUNT = 12;

  /**
   * 이미 있는 게시물에 새로운 게시물들을 추가하는 메소드
   *
   * @param fragment          the fragment
   * @param simpleArticleList the simple article list
   */
  public static void setArticleListView(final HasViewModelFragment fragment,
                                        final List<SimpleArticle> simpleArticleList,
                                        final boolean showSnackBar) {
    fragment.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        fragment.getHasAdapterViewModel().addItems(simpleArticleList);
        if(showSnackBar) {
          showSnackBar(fragment.getView(), "게시물을 가져왔습니다.");
          fragment.getHasAdapterViewModel().getListView().requestFocus();
        }
      }
    });
  }

  /**
   * 로드된 게시물들을 삭제하고 새로운 게시물들을 읽어온다.
   * 항상 1페이지를 로드함.
   */
  public static void refreshArticleListView(final HasViewModelFragment fragment,
                                            final boolean resetSearchKeyword) {

    final HasAdapterViewModel viewModel = fragment.getHasAdapterViewModel();
    final CurrentData currentData = CurrentDataManager.getInstance(((Fragment) fragment).getContext());
    currentData.setPage(1);
    currentData.setSerPos(0);
    currentData.setNextSerPos(0);
    if (resetSearchKeyword) currentData.setSearchWord("");

    if(fragment.getActivity() != null)
      fragment.getActivity().runOnUiThread(new Runnable() {
        @Override
        public void run() {
          viewModel.clearItem();
          viewModel.startRefreshing();
          ServiceProvider.getInstance().getSimpleArticles(fragment);
        }
      });
  }


  /**
   * 로드된 게시물들을 삭제하고 새로운 게시물들을 읽어온다.
   * 항상 1페이지를 로드함.
   */
  public static void refreshMaruListView(final MangaViewerFragment fragment,
                                         final boolean resetSearchKeyword) {
    if(fragment == null || fragment.getActivity() == null || fragment.getPresenter() == null) return;

    final CurrentData currentData = CurrentDataManager.getInstance((fragment).getContext());
    currentData.setPage(1);
    currentData.setSerPos(0);
    currentData.setNextSerPos(0);
    if (resetSearchKeyword) currentData.setSearchWord("");
//    CurrentDataManager.save(currentData, fragment.getContext());
    fragment.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        MaruViewerViewModel presenter = fragment.getPresenter();
        presenter.clearItem();
        presenter.startRefreshing();
      }
    });
    MaruServiceProvider.getInstance().getMaruSimpleModels(fragment,
            currentData.getPage(), currentData.getSearchWord(), true);
  }

  /**
   * 갤러리 검색 결과 UI 적용
   *
   * @param gallerySearchResult the gallery search result
   * @param galleryListFragment the gallery list fragment
   */
  public static void setGallerySearchResult(final List<GalleryInfo> gallerySearchResult,
                                            final GalleryListFragment galleryListFragment) {
    galleryListFragment.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        GalleryListViewModel galleryListViewModel = galleryListFragment.galleryListViewModel;
        if (galleryListViewModel != null) galleryListViewModel.addGallerySearchResult(gallerySearchResult);
      }
    });
  }

  /**
   * 마루 결과 UI적용
   */
  public static void setMaruSearchResult(final List<MangaSimpleModel> maruSearchResult,
                                         final MangaViewerFragment fragment,
                                         final boolean doClear) {
    fragment.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        MaruViewerViewModel presenter = fragment.getPresenter();
        if (doClear) {
          presenter.clearItem();
          fragment.clearSearchKeyword();
        }

        if (presenter != null) presenter.addItems(maruSearchResult);
        presenter.stopRefreshing();
      }
    });
  }


  /**
   * 스낵바로 메시지를 출력하는 메소드
   *
   * @param view    the view
   * @param message the message
   */
  public static void showSnackBar(final View view, final String message) {

    final ForegroundColorSpan whiteSpan = new ForegroundColorSpan(ContextCompat.getColor(view.getContext(), android.R.color.white));
    final SpannableStringBuilder snackbarText = new SpannableStringBuilder(message);
    final Context context = view.getContext();

    view.post(new Runnable() {
      @Override
      public void run() {
		  try {
			snackbarText.setSpan(whiteSpan, 0, snackbarText.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
			snackbar = Snackbar.make(view, snackbarText, Snackbar.LENGTH_SHORT);
			setSnackBarAction(context);
			snackbar.show();
			hideKeyboard(view);
		  } catch (Exception e) {}
      }
    });
  }

  // 스낵바 액션 이벤트 처리
  private static void setSnackBarAction(Context context) {
    snackbar.setActionTextColor(context.getResources().getColor(R.color.colorGray));
    snackbar.setAction(context.getString(R.string.close), new View.OnClickListener() { // 터치 시 스낵바 제거함
      @Override
      public void onClick(View view) {
       snackbar.dismiss();
      }
    });
  }

  /**
   * 키보드를 숨기는 메소드
   *
   * @param view the view
   */
  public static void hideKeyboard(View view) {
    InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
  }

  /**
   * 키보드를 여는 메소드
   *
   * @param view the view
   */
  public static void showKeyboard(View view) {
    InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
  }


  /**
   * 입력된 문자열을 토스트 메시지로 보여주는 메소드
   *
   * @param activity the activity
   * @param message  the message
   */
  public static void showToast(final Activity activity, final String message) {
    activity.runOnUiThread(new Runnable() {

      @Override
      public void run() {
        if (toast != null) toast.cancel();
        toast = Toast.makeText(activity, message, Toast.LENGTH_SHORT);
        toast.show();
      }
    });
  }

  /**
   * 현재 토스트 메시지가 있다면 제거하는 메소드
   *
   * @param activity the activity
   */
  static void clearToast(final Activity activity) {
    activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (toast != null) toast.cancel();
      }
    });
  }


  /**
   * 스플래시 액티비티에 메시지를 띄우는 메소드.
   *
   * @param splashActivity the splash activity
   * @param msg            the msg
   */
  public static void setSplashText(final SplashActivity splashActivity, final String msg) {
    splashActivity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        splashActivity.setSplashText(msg);
      }
    });
  }

  /**
   * 해당 뷰를 선택할 수 있게 하거나 선택 하지 못하게 하는 메소드
   *
   * @param activity the activity
   * @param view     the view
   * @param value    the value
   */
  public static void setViewState(final Activity activity, final View view, final boolean value) {
    activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        view.setEnabled(value);
      }
    });
  }


  /**
   * 게시물 내용을 여는 메소드
   *
   * @param fragment      the fragment
   * @param articleDetail the article detail
   * @param articleUrl    the article url
   */
  public static void openArticleDetail(final HasViewModelFragment fragment,
                                       final ArticleDetail articleDetail,
                                       final String articleUrl) {
    fragment.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        fragment.getHasAdapterViewModel().stopRefreshing();
        Intent intent = new Intent(fragment.getActivity(), ArticleDetailActivity.class);
        intent.putExtra("articleDetail", articleDetail);
        intent.putExtra("articleUrl", articleUrl);
        fragment.getActivity().startActivityForResult(intent, RequestCodes.ARTICLE.ordinal());
      }
    });
  }

  /**
   * 게시물을 다시 여는 메소드
   *
   * @param activity      the activity
   * @param articleDetail the article detail
   * @param articleUrl    the article url
   */
  public static void openArticleDetail(final Activity activity,
                                       final ArticleDetail articleDetail,
                                       final String articleUrl) {
    activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Intent intent = new Intent(activity, ArticleDetailActivity.class);
        intent.putExtra("articleDetail", articleDetail);
        intent.putExtra("articleUrl", articleUrl);
        activity.startActivityForResult(intent, RequestCodes.ARTICLE.ordinal());
      }
    });
  }






  public static void refreshComment(final Activity activity,
                                    final ArticleDetailViewModel presenter,
                                    final List<Comment> newComments) {
    // 쓰레드 처리 순서 조정
    final int prevCount = presenter.getCommentLayoutCount();
    presenter.addComments(activity, presenter, newComments);

    ThreadPoolManager.getContentEc().submit(new Runnable() {
      @Override
      public void run() {
        activity.runOnUiThread(new Runnable() {
          @Override
          public void run() {
            for (int i = 0; i < prevCount; i++) presenter.removeCommentLayoutFirst();

          }
        });
      }
    });
  }


  /**
   * 이미지뷰에 이미지를 추가하는 메소드
   *
   * @param activity  the activity
   * @param imageView the image view
   * @param bytes     the bytes
   */
  static void setImageView(final Activity activity,
                           final ImageView imageView,
                           final byte[] bytes) {

    final int screenWidth = activity.getWindow().getDecorView().getMeasuredWidth();

    activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Glide.with(activity.getApplicationContext())
                .load(bytes)
                .override(Target.SIZE_ORIGINAL, 16000)
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .listener(new RequestListener<byte[], GlideDrawable>() {
                  @Override
                  public boolean onException(Exception e, byte[] model, Target<GlideDrawable> target, boolean isFirstResource) {
                    return false;
                  }

                  @Override
                  public boolean onResourceReady(GlideDrawable resource, byte[] model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                    int width = resource.getIntrinsicWidth();
                    int height = resource.getIntrinsicHeight();
                    int th = screenWidth / 10 * 6;

                    if (th <= width || th <= height) { // 이미지가 화면 너비의 70%를 차지하는 경우
                      imageView.setAdjustViewBounds(true); // 크기를 맞춘다
                    }

                    return false;
                  }
                })
                .into(imageView);
      }
    });
  }


  /**
   * 이미지뷰에 이미지를 추가하는 메소드.
   * 원본 크기로 가져온다.
   *
   * @param activity  the activity
   * @param imageView the image view
   * @param bytes     the bytes
   */
  static void setImageViewWithAttacher(final Activity activity,
                                       final ImageView imageView,
                                       final PhotoViewAttacher photoViewAttacher,
                                       final ImageView.ScaleType scaleType,
                                       final byte[] bytes){
    activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Glide.with(activity.getApplicationContext())
                .load(bytes)
                .override(Target.SIZE_ORIGINAL, 12000)
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .listener(new RequestListener<byte[], GlideDrawable>() {
                  @Override
                  public boolean onException(Exception e, byte[] model, Target<GlideDrawable> target, boolean isFirstResource) {
                    return false;
                  }
                  @Override
                  public boolean onResourceReady(GlideDrawable resource, byte[] model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                    photoViewAttacher.setScaleType(scaleType);
                    return false;
                  }
                })
                .into(imageView);
      }
    });
  }

  /**
   * activity를 종료하는 메소드
   *
   * @param activity the activity
   */
  public static void finishActivity(final Activity activity, final ResultCodes resultCodes) {
    activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (activity != null) {
          activity.setResult(resultCodes.ordinal());
          activity.finish();
        }
      }
    });
  }


  public static void addMaruImage(final MaruViewerDetailActivity activity,
                                  final CurrentData currentData,
                                  final ScrollView scrollView,
                                  final LinearLayout imageLayout,
                                  final List<ImageView> imageViews,
                                  final MangaContentModel mangaContentModel,
                                  final int start) {

    final SparseArray<byte[]> imageBytes = new SparseArray<>();

    activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        final Resources res = activity.getResources();
        LinearLayout.LayoutParams il
                = new LinearLayout.LayoutParams
                (LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);

        il.setMargins(0, DipUtils.getDp(res, 5),
                0, DipUtils.getDp(res, 5));

        LinearLayout.LayoutParams btnLayout = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        btnLayout.setMargins(DipUtils.getDp(res, 40), DipUtils.getDp(res, 20),
                DipUtils.getDp(res, 40), DipUtils.getDp(res, 20));


        // 브라우저로 열기
        Button openButton = (Button)
                              activity.findViewById(R.id.maru_detail_open);
        openButton.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse(mangaContentModel.getUrl()));
            activity.startActivity(intent);
          }
        });

        // 마지막 닫기 버튼
        Button closeButton = new Button(activity);
        closeButton.setLayoutParams(btnLayout);
        closeButton.setGravity(Gravity.CENTER);
        closeButton.setText(res.getString(R.string.close));
        ButtonUtils.setBtnTheme(activity, currentData, closeButton);
        closeButton.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            activity.finish();
          }
        });

        imageLayout.removeAllViews();

        for(int i = 0; i + start < mangaContentModel.getImagesUrls().size(); i++ ) {
          if(i > IMAGE_LOAD_COUNT) {
            final int nextStart = i + start;
            Button continueBtn = new Button(activity);
            continueBtn.setLayoutParams(btnLayout);
            continueBtn.setGravity(Gravity.CENTER);
            continueBtn.setText(res.getString(R.string.next_page_load));
            ButtonUtils.setBtnTheme(activity, currentData, continueBtn);
            continueBtn.setOnClickListener(new View.OnClickListener() {
              @Override
              public void onClick(View view) {
                for (ImageView imageView : imageViews) {
                  imageView.setImageBitmap(null);
                }
                imageBytes.clear();
                imageViews.clear();
                scrollView.setScrollY(0);

                addMaruImage(activity, currentData, scrollView,
                        imageLayout, imageViews, mangaContentModel, nextStart);
              }
            });
            imageLayout.addView(continueBtn);
            break;
          }
          imageBytes.put(i, null); // initialize
          String imageUrl = mangaContentModel.getImagesUrls().get(i + start);

          ImageView imageView = new ImageView(activity);
          imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
          imageView.setAdjustViewBounds(true);
          imageView.setLayoutParams(il);
          imageView.setImageResource(R.drawable.image_loading);
          imageViews.add(imageView);

          ContentUtils.loadBitmapFromUrl(activity, i, imageBytes, imageUrl, mangaContentModel.getOrigin(), imageView, currentData);
          imageView.setOnClickListener(ImageViewerListener.get(activity, mangaContentModel.getNo(), i, imageBytes, true));
          imageLayout.addView(imageView);
        }
          if (mangaContentModel.getNo().equals("CAPTCHA")) { // 캡차 입력이 필요한 경우 닫기 버튼 추가하지 않음

            // 캡차 입력
            final EditText inputCaptcha = new EditText(activity);
            inputCaptcha.setSingleLine(true);
            inputCaptcha.setLayoutParams(btnLayout);
            inputCaptcha.setGravity(Gravity.CENTER);
            inputCaptcha.setBackground(activity.getResources().getDrawable(R.drawable.border_gray));
            imageLayout.addView(inputCaptcha);

            // 캡차 전송 버튼
            final Button postButton = new Button(activity);
            postButton.setLayoutParams(btnLayout);
            postButton.setGravity(Gravity.CENTER);
            postButton.setText("Submit");
            ButtonUtils.setBtnTheme(activity, currentData, postButton);
            postButton.setOnClickListener(new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                try {
                  MaruServiceProvider.getInstance().postCaptch(mangaContentModel.getUrl(), inputCaptcha.getText().toString());
                  activity.recreate();
                } catch (IOException e) {}
              }
            });
            imageLayout.addView(postButton);

            // 엔터키로 확인처리
            inputCaptcha.setOnKeyListener(new View.OnKeyListener() {
              @Override
              public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.KEYCODE_ENTER) {
                  postButton.performClick();
                }
                return false;
              }
            });


          } else {
            imageLayout.addView(closeButton);
          }
      }

    });

  }

}
