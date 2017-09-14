package dc.maitetsu.service;

import android.util.Log;
import dc.maitetsu.models.MangaContentModel;
import dc.maitetsu.models.MangaSimpleModel;
import dc.maitetsu.ui.fragment.MangaViewerFragment;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @since 2017-04-29
 */
enum MaruService implements IMangaService {
  getInstance;

  private static final String MARU_URL = "http://marumaru.in/";
  private static final String MARU_LINK_URL = "http://marumaru.in/b/mangaup/";
  private static final String PASS = "?pass=qndxkr";
  private static final String ORIGIN = "http://wasabisyrup.com/";
  private static Map<String, String> cookies = new HashMap<>();

  public List<MangaSimpleModel> getSimpleModels(String userAgent, int page, String keyword) throws IOException {
    Document rawData = getTitleRawData(userAgent, page, keyword);

    List<MangaSimpleModel> mangaSimpleModels = new ArrayList<>();
    Elements elements = rawData.select(".list");
    for (Element element : elements) {
      String thumbStyle = element.select(".image-thumb").attr("style");
      if (thumbStyle.trim().isEmpty()) continue;

      String no = element.previousElementSibling().attr("name");
      String thumbUrl = MARU_URL + thumbStyle.split("\\(")[1].split("\\)")[0];
      String title = element.select(".subject").text();
      String date = element.select(".info").text().split(" \\|")[0];

      mangaSimpleModels.add(new MangaSimpleModel(no, thumbUrl, title, date, false));
    }

    return mangaSimpleModels;
  }


  // 업데이트 목록
  private Document getTitleRawData(String userAgent, int page, String keyword) throws IOException {
    String category = "?c=26";
    String searchKeyword = "&where=subject&keyword=" + keyword;
    String pageKeyword = "&sort=gid&p=" + page;

    Connection.Response res = Jsoup.connect(MARU_URL + category + searchKeyword + pageKeyword)
            .userAgent(userAgent)
            .timeout(5000)
            .header("Origin", MARU_URL)
            .header("Referer", MARU_URL)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
            .header("Accept-Encoding", "gzip, deflate, sdch")
            .header("Accept-Language", "ko-KR,ko;q=0.8,en-US;q=0.6,en;q=0.4")
            .cookies(cookies)
            .method(Connection.Method.GET)
            .followRedirects(true)
            .execute();

//    sendIncapsula(res.body(), userAgent);
    return res.parse();
  }


  private String getMaruUrl(String userAgent, String no) throws IOException {
    Elements els = Jsoup.connect(MARU_LINK_URL + no.substring(1))
            .userAgent(userAgent)
            .timeout(5000)
            .header("Origin", "http://marumaru.in/")
            .header("Referer", MARU_URL)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
            .header("Accept-Encoding", "gzip, deflate, sdch")
            .header("Accept-Language", "ko-KR,ko;q=0.8,en-US;q=0.6,en;q=0.4")
            .cookies(cookies)
            .followRedirects(true)
            .get()
            .select("#vContent a");

    for (Element e : els) {
      String url = e.attr("abs:href");
      if (url.contains("archives")) return url;
    }
    return "";
  }


  public MangaContentModel getContentModel(String userAgent, String no, boolean isViewerModel, int count) throws IOException {
    if (count > 3) throw new IOException();

    String url;
    if (isViewerModel)
      url = ORIGIN + "archives/" + no;
    else
      url = getMaruUrl(userAgent, no);


    Document rawData = Jsoup.connect(url + PASS)
            .userAgent(userAgent)
            .timeout(5000)
            .header("Origin", ORIGIN)
            .header("Upgrade-Insecure-Requests", "1")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
            .header("Accept-Encoding", "gzip, deflate")
            .header("Accept-Language", "ko-KR,ko;q=0.8,en-US;q=0.6,en;q=0.4")
            .followRedirects(true)
            .get();

    if (rawData.text().trim().isEmpty()) {
      return getContentModel(userAgent, no, isViewerModel, count + 1);
    }

    int episodeNum = 1;
    String nowEpisodeName = rawData.select(".title-no").first().text().trim();
    String title = rawData.select(".title-subject").first().text()
            + " " + nowEpisodeName;


    // Images
    List<String> urls = new ArrayList<>();
    Elements elements = rawData.select(".lz-lazyload");
    for (Element element : elements) {
      String imageUrl = element.attr("abs:data-src");
      urls.add(imageUrl);
    }


    // Episodes
    List<MangaContentModel.MaruEpisode> episodes = new ArrayList<>();
    Elements episodeEls = rawData.select("select.list-articles").first().children();
    for (Element episodeEl : episodeEls) {
      String episodeName = episodeEl.text().trim();
      MangaContentModel.MaruEpisode maruEpisode = new MangaContentModel.MaruEpisode(
              episodeName,
              episodeEl.attr("value")
      );
      if (nowEpisodeName.contains(episodeName)) {
        episodeNum = episodes.size();
      }
      episodes.add(maruEpisode);
    }


    MangaContentModel mangaContentModel = new MangaContentModel();
    mangaContentModel.setNo(no);
    mangaContentModel.setTitle(title);
    mangaContentModel.setUrl(url);
    mangaContentModel.setOrigin(ORIGIN);
    mangaContentModel.setTitle(title);
    mangaContentModel.setEpisodeNum(episodeNum);
    mangaContentModel.setEpisodes(episodes);
    mangaContentModel.setImagesUrls(urls);
    return mangaContentModel;
  }

}
