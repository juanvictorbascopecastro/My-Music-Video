package app.list.mymusic.utils.idurl;

import java.util.Arrays;
import java.util.Collection;

public class YouTubeHelperTest {
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { "youtube.com/v/vidid" },
                { "youtube.com/vi/vidid" },
                { "youtube.com/?v=vidid" },
                { "youtube.com/?vi=vidid" },
                { "youtube.com/watch?v=vidid" },
                { "youtube.com/watch?vi=vidid" },
                { "youtu.be/vidid" },
                { "youtube.com/embed/vidid" },
                { "youtube.com/embed/vidid" },
                { "www.youtube.com/v/vidid" },
                { "http://www.youtube.com/v/vidid" },
                { "https://www.youtube.com/v/vidid" },
                { "youtube.com/watch?v=vidid&wtv=wtv" },
                { "http://www.youtube.com/watch?dev=inprogress&v=vidid&feature=related" },
                { "https://m.youtube.com/watch?v=vidid" }
        });
    }

    private String url;

    public YouTubeHelperTest(String url) {
        this.url= url;
    }

    private YouTubeHelper youTubeHelper = new YouTubeHelper();
    public String getUrl(){
        return youTubeHelper.extractVideoIdFromUrl(url);
    }


}
