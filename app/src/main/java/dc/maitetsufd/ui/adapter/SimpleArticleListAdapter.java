package dc.maitetsufd.ui.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.v4.content.ContextCompat;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import dc.maitetsufd.R;
import dc.maitetsufd.data.CurrentData;
import dc.maitetsufd.data.CurrentDataManager;
import dc.maitetsufd.models.SimpleArticle;
import dc.maitetsufd.ui.listener.FilterUserLongClickListener;
import dc.maitetsufd.ui.listener.SimpleArticleClickListener;
import dc.maitetsufd.ui.viewmodel.HasViewModelFragment;
import dc.maitetsufd.utils.DipUtils;
import dc.maitetsufd.utils.KeywordUtils;
import dc.maitetsufd.utils.NickNameHighLight;
import dc.maitetsufd.utils.UserTypeManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @since 2017-04-22
 *
 * 갤러리의 글 목록을 관리하는 어댑터
 *
 */
public class SimpleArticleListAdapter extends BaseAdapter {
  private HasViewModelFragment fragment;
  private CurrentData currentData;
  private List<SimpleArticle> simpleArticles = Collections.synchronizedList(new ArrayList<SimpleArticle>());

  public SimpleArticleListAdapter(HasViewModelFragment fragment) {
    this.fragment = fragment;
    this.currentData = CurrentDataManager.getInstance(fragment.getActivity().getApplicationContext());

  }

  public void loadCurrentData(){
    currentData = CurrentDataManager.getInstance(fragment.getActivity().getApplicationContext());
  }

  @Override
  public int getCount() {
    return simpleArticles.size();
  }

  @Override
  public Object getItem(int i) {
    return simpleArticles.get(i);
  }

  @Override
  public long getItemId(int i) {
    return i;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    final Context context = parent.getContext();

    if (convertView == null) {
      LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      convertView = inflater.inflate(R.layout.simple_article_list_item, parent, false);
    }

    Resources res = parent.getResources();
    ImageView itemImg = (ImageView) convertView.findViewById(R.id.simple_article_item_img);
    TextView title = (TextView) convertView.findViewById(R.id.simple_article_item_title);
    TextView date = (TextView) convertView.findViewById(R.id.simple_article_item_date);
    TextView viewCount = (TextView) convertView.findViewById(R.id.simple_article_item_viewCount);
    TextView recommendCount = (TextView) convertView.findViewById(R.id.simple_article_item_recommendCount);
    TextView nickname = (TextView) convertView.findViewById(R.id.simple_article_item_nickname);
    ImageView userType = (ImageView) convertView.findViewById(R.id.simple_article_item_user_type);

    SimpleArticle simpleArticle = simpleArticles.get(position);
    if (simpleArticle.getArticleType() == SimpleArticle.ArticleType.IMG)
      itemImg.setImageDrawable(res.getDrawable(R.drawable.all_image));
    else if(simpleArticle.getArticleType() == SimpleArticle.ArticleType.NO_IMG)
      itemImg.setImageDrawable(res.getDrawable(R.drawable.none_image));
    else if(simpleArticle.getArticleType() == SimpleArticle.ArticleType.MOV)
      itemImg.setImageDrawable(res.getDrawable(R.drawable.movie_article_icon));
    else
      itemImg.setImageDrawable(res.getDrawable(R.drawable.recommand_article_icon));

    itemImg.setColorFilter(
          ContextCompat.getColor(convertView.getContext(), R.color.colorDarkGray),
          PorterDuff.Mode.SRC_IN);

    SpannableStringBuilder builder = KeywordUtils
            .getBuilder(simpleArticle.getTitle(), currentData.getSearchWord(), null);

    if(simpleArticle.getCommentCount() > 0) { // 댓글 수가 0개 이상일 때
      builder.append(KeywordUtils.colorText("  " +
                      String.format(res.getString(R.string.comment), simpleArticle.getCommentCount()),
              res.getColor(R.color.colorAccent)));
    }

    title.setText(builder, TextView.BufferType.SPANNABLE);

    date.setText(simpleArticle.getDate());
    viewCount.setText(String.format(res.getString(R.string.view), simpleArticle.getViewCount()));
    recommendCount.setText(String.format(res.getString(R.string.recommend), simpleArticle.getRecommendCount()));
    nickname.setText(simpleArticle.getUserInfo().getNickname());
    UserTypeManager.set(res, simpleArticle.getUserInfo(), userType);


    // 글 제목을 클릭해서 글 내용을 보는 이벤트 핸들링
    convertView.setOnClickListener(SimpleArticleClickListener.get(fragment, simpleArticle));

    // 글 제목을 롱클릭해서 유저 차단 여부를 묻는 이벤트 핸들링
    convertView.setOnLongClickListener(FilterUserLongClickListener.get(fragment.getActivity(),
                                                                      simpleArticle.getUserInfo()));
    setThemeColor(convertView, currentData);

    // 닉네임 하이라이팅
    NickNameHighLight.set(simpleArticle.getUserInfo(), nickname, 250);

    return convertView;
  }

  // 테마에 따라 색 설정
  private void setThemeColor(View convertView, CurrentData currentData) {
    if(currentData.isDarkTheme()) {
      ((TextView) convertView.findViewById(R.id.simple_article_item_title))
              .setTextColor(ContextCompat.getColor(convertView.getContext(), R.color.colorWhite));

      ((TextView) convertView.findViewById(R.id.simple_article_item_nickname))
              .setTextColor(ContextCompat.getColor(convertView.getContext(), R.color.colorGray));
    }
  }

  /**
   * 어댑터 리스트에 추가하는 메소드
   *
   * @param simpleArticle the simple article
   */
  public void addItem(SimpleArticle simpleArticle) {
    simpleArticles.add(simpleArticle);
  }

  /**
   * 어댑터 리스트를 비우는 메소드
   */
  public void clearItem() {
    simpleArticles.clear();
  }

}
