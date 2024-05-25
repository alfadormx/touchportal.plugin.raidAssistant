package mx.alfador.touchportal;

import com.christophecvb.touchportal.TouchPortalPlugin;
import com.christophecvb.touchportal.annotations.Category;
import com.christophecvb.touchportal.annotations.Plugin;
import com.christophecvb.touchportal.annotations.Setting;
import com.christophecvb.touchportal.annotations.State;
import com.christophecvb.touchportal.helpers.PluginHelper;
import com.christophecvb.touchportal.model.*;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.helix.domain.Stream;
import com.github.twitch4j.helix.domain.StreamList;
import com.github.twitch4j.helix.domain.User;
import com.google.gson.JsonObject;
import com.netflix.hystrix.exception.HystrixBadRequestException;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import org.imgscalr.Scalr;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.text.MessageFormat;
import java.util.*;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Plugin(version = BuildConfig.VERSION_CODE, colorDark = "#203060", colorLight = "#4070F0", name = "RaidAssistant plugin - alFadorMX")
public class RaidAssistantPlugin extends TouchPortalPlugin implements TouchPortalPlugin.TouchPortalPluginListener {

    private final static Logger LOGGER = Logger.getLogger(TouchPortalPlugin.class.getName());

    private TwitchClient client;

    private String userId;

    private final String clientId = Config.CLIENT_ID;
    private final String clientSecret = Config.CLIENT_SECRET;
    private final String clientResponseType = "token";
    private final String clientRedirectUri = Config.REDIRECT_URL;
    private final String clientScope = "user%3Aread%3Afollows";
    private final String tokenAuthGeneration_url = "https://id.twitch.tv/oauth2/authorize?response_type={0}&client_id={1}&redirect_uri={2}&scope={3}";
    private final HashMap<String, User> users = new HashMap<>();
    private final HashMap<String, String> base64Images = new HashMap<>();

    private final int fetchHowMany = 20;
    private ScheduledExecutorService scheduledExecutorService;

    private enum Categories {
        @Category(name = "General", imagePath = "images/icon-24.png")
        General,
        @Category(name = "Raid #01", imagePath = "images/icon-24.png")
        Raid01,
        @Category(name = "Raid #02", imagePath = "images/icon-24.png")
        Raid02,
        @Category(name = "Raid #03", imagePath = "images/icon-24.png")
        Raid03,
        @Category(name = "Raid #04", imagePath = "images/icon-24.png")
        Raid04,
        @Category(name = "Raid #05", imagePath = "images/icon-24.png")
        Raid05,
        @Category(name = "Raid #06", imagePath = "images/icon-24.png")
        Raid06,
        @Category(name = "Raid #07", imagePath = "images/icon-24.png")
        Raid07,
        @Category(name = "Raid #08", imagePath = "images/icon-24.png")
        Raid08,
        @Category(name = "Raid #09", imagePath = "images/icon-24.png")
        Raid09,
        @Category(name = "Raid #10", imagePath = "images/icon-24.png")
        Raid10,
        @Category(name = "Raid #11", imagePath = "images/icon-24.png")
        Raid11,
        @Category(name = "Raid #12", imagePath = "images/icon-24.png")
        Raid12,
        @Category(name = "Raid #13", imagePath = "images/icon-24.png")
        Raid13,
        @Category(name = "Raid #14", imagePath = "images/icon-24.png")
        Raid14,
        @Category(name = "Raid #15", imagePath = "images/icon-24.png")
        Raid15,
        @Category(name = "Raid #16", imagePath = "images/icon-24.png")
        Raid16,
        @Category(name = "Raid #17", imagePath = "images/icon-24.png")
        Raid17,
        @Category(name = "Raid #18", imagePath = "images/icon-24.png")
        Raid18,
        @Category(name = "Raid #19", imagePath = "images/icon-24.png")
        Raid19,
        @Category(name = "Raid #20", imagePath = "images/icon-24.png")
        Raid20,
    }

    @Setting(name = "Twitch Username", defaultValue = "", maxLength = 68.0D)
    private String setting_userName;
    @Setting(name = "Twitch Auth Token", defaultValue = "", maxLength = 68.0D)
    private String setting_authToken;

    @State(desc = "Total Channels Online", defaultValue = "", categoryId = "General")
    private String state_totalChannelsOnline;

    @State(desc = "Request Has Error", defaultValue = "FALSE", categoryId = "General")
    private String state_requestHasError;
    @State(desc = "Request Error Message", defaultValue = "", categoryId = "General")
    private String state_requestErrorMessage;

    @State(desc = "Raid #01: User Id", defaultValue = "", categoryId = "Raid01")
    private String state_raidUserId01;
    @State(desc = "Raid #01: User Name", defaultValue = "", categoryId = "Raid01")
    private String state_raidUsername01;
    @State(desc = "Raid #01: User Thumbnail", defaultValue = "", categoryId = "Raid01")
    private String state_raidUserThumbnail01;
    @State(desc = "Raid #01: Game Name", defaultValue = "", categoryId = "Raid01")
    private String state_raidGameName01;
    @State(desc = "Raid #01: Title", defaultValue = "", categoryId = "Raid01")
    private String state_raidTitle01;
    @State(desc = "Raid #01: Short Title", defaultValue = "", categoryId = "Raid01")
    private String state_raidShortTitle01;
    @State(desc = "Raid #01: Is Mature", defaultValue = "", categoryId = "Raid01")
    private String state_raidIsMature01;
    @State(desc = "Raid #01: Thumbnail", defaultValue = "", categoryId = "Raid01")
    private String state_raidThumbnail01;
    @State(desc = "Raid #01: Viewer Count", defaultValue = "", categoryId = "Raid01")
    private String state_raidViewerCount01;
    @State(desc = "Raid #01: Url", defaultValue = "", categoryId = "Raid01")
    private String state_raidUrl01;

    @State(desc = "Raid #02: User Id", defaultValue = "", categoryId = "Raid02")
    private String state_raidUserId02;
    @State(desc = "Raid #02: User Name", defaultValue = "", categoryId = "Raid02")
    private String state_raidUsername02;
    @State(desc = "Raid #02: User Thumbnail", defaultValue = "", categoryId = "Raid02")
    private String state_raidUserThumbnail02;
    @State(desc = "Raid #02: Game Name", defaultValue = "", categoryId = "Raid02")
    private String state_raidGameName02;
    @State(desc = "Raid #02: Title", defaultValue = "", categoryId = "Raid02")
    private String state_raidTitle02;
    @State(desc = "Raid #02: Short Title", defaultValue = "", categoryId = "Raid02")
    private String state_raidShortTitle02;
    @State(desc = "Raid #02: Is Mature", defaultValue = "", categoryId = "Raid02")
    private String state_raidIsMature02;
    @State(desc = "Raid #02: Thumbnail", defaultValue = "", categoryId = "Raid02")
    private String state_raidThumbnail02;
    @State(desc = "Raid #02: Viewer Count", defaultValue = "", categoryId = "Raid02")
    private String state_raidViewerCount02;
    @State(desc = "Raid #02: Url", defaultValue = "", categoryId = "Raid02")
    private String state_raidUrl02;

    @State(desc = "Raid #03: User Id", defaultValue = "", categoryId = "Raid03")
    private String state_raidUserId03;
    @State(desc = "Raid #03: User Name", defaultValue = "", categoryId = "Raid03")
    private String state_raidUsername03;
    @State(desc = "Raid #03: User Thumbnail", defaultValue = "", categoryId = "Raid03")
    private String state_raidUserThumbnail03;
    @State(desc = "Raid #03: Game Name", defaultValue = "", categoryId = "Raid03")
    private String state_raidGameName03;
    @State(desc = "Raid #03: Title", defaultValue = "", categoryId = "Raid03")
    private String state_raidTitle03;
    @State(desc = "Raid #03: Short Title", defaultValue = "", categoryId = "Raid03")
    private String state_raidShortTitle03;
    @State(desc = "Raid #03: Is Mature", defaultValue = "", categoryId = "Raid03")
    private String state_raidIsMature03;
    @State(desc = "Raid #03: Thumbnail", defaultValue = "", categoryId = "Raid03")
    private String state_raidThumbnail03;
    @State(desc = "Raid #03: Viewer Count", defaultValue = "", categoryId = "Raid03")
    private String state_raidViewerCount03;
    @State(desc = "Raid #03: Url", defaultValue = "", categoryId = "Raid03")
    private String state_raidUrl03;

    @State(desc = "Raid #04: User Id", defaultValue = "", categoryId = "Raid04")
    private String state_raidUserId04;
    @State(desc = "Raid #04: User Name", defaultValue = "", categoryId = "Raid04")
    private String state_raidUsername04;
    @State(desc = "Raid #04: User Thumbnail", defaultValue = "", categoryId = "Raid04")
    private String state_raidUserThumbnail04;
    @State(desc = "Raid #04: Game Name", defaultValue = "", categoryId = "Raid04")
    private String state_raidGameName04;
    @State(desc = "Raid #04: Title", defaultValue = "", categoryId = "Raid04")
    private String state_raidTitle04;
    @State(desc = "Raid #04: Short Title", defaultValue = "", categoryId = "Raid04")
    private String state_raidShortTitle04;
    @State(desc = "Raid #04: Is Mature", defaultValue = "", categoryId = "Raid04")
    private String state_raidIsMature04;
    @State(desc = "Raid #04: Thumbnail", defaultValue = "", categoryId = "Raid04")
    private String state_raidThumbnail04;
    @State(desc = "Raid #04: Viewer Count", defaultValue = "", categoryId = "Raid04")
    private String state_raidViewerCount04;
    @State(desc = "Raid #04: Url", defaultValue = "", categoryId = "Raid04")
    private String state_raidUrl04;

    @State(desc = "Raid #05: User Id", defaultValue = "", categoryId = "Raid05")
    private String state_raidUserId05;
    @State(desc = "Raid #05: User Name", defaultValue = "", categoryId = "Raid05")
    private String state_raidUsername05;
    @State(desc = "Raid #05: User Thumbnail", defaultValue = "", categoryId = "Raid05")
    private String state_raidUserThumbnail05;
    @State(desc = "Raid #05: Game Name", defaultValue = "", categoryId = "Raid05")
    private String state_raidGameName05;
    @State(desc = "Raid #05: Title", defaultValue = "", categoryId = "Raid05")
    private String state_raidTitle05;
    @State(desc = "Raid #05: Short Title", defaultValue = "", categoryId = "Raid05")
    private String state_raidShortTitle05;
    @State(desc = "Raid #05: Is Mature", defaultValue = "", categoryId = "Raid05")
    private String state_raidIsMature05;
    @State(desc = "Raid #05: Thumbnail", defaultValue = "", categoryId = "Raid05")
    private String state_raidThumbnail05;
    @State(desc = "Raid #05: Viewer Count", defaultValue = "", categoryId = "Raid05")
    private String state_raidViewerCount05;
    @State(desc = "Raid #05: Url", defaultValue = "", categoryId = "Raid05")
    private String state_raidUrl05;

    @State(desc = "Raid #06: User Id", defaultValue = "", categoryId = "Raid06")
    private String state_raidUserId06;
    @State(desc = "Raid #06: User Name", defaultValue = "", categoryId = "Raid06")
    private String state_raidUsername06;
    @State(desc = "Raid #06: User Thumbnail", defaultValue = "", categoryId = "Raid06")
    private String state_raidUserThumbnail06;
    @State(desc = "Raid #06: Game Name", defaultValue = "", categoryId = "Raid06")
    private String state_raidGameName06;
    @State(desc = "Raid #06: Title", defaultValue = "", categoryId = "Raid06")
    private String state_raidTitle06;
    @State(desc = "Raid #06: Short Title", defaultValue = "", categoryId = "Raid06")
    private String state_raidShortTitle06;
    @State(desc = "Raid #06: Is Mature", defaultValue = "", categoryId = "Raid06")
    private String state_raidIsMature06;
    @State(desc = "Raid #06: Thumbnail", defaultValue = "", categoryId = "Raid06")
    private String state_raidThumbnail06;
    @State(desc = "Raid #06: Viewer Count", defaultValue = "", categoryId = "Raid06")
    private String state_raidViewerCount06;
    @State(desc = "Raid #06: Url", defaultValue = "", categoryId = "Raid06")
    private String state_raidUrl06;

    @State(desc = "Raid #07: User Id", defaultValue = "", categoryId = "Raid07")
    private String state_raidUserId07;
    @State(desc = "Raid #07: User Name", defaultValue = "", categoryId = "Raid07")
    private String state_raidUsername07;
    @State(desc = "Raid #07: User Thumbnail", defaultValue = "", categoryId = "Raid07")
    private String state_raidUserThumbnail07;
    @State(desc = "Raid #07: Game Name", defaultValue = "", categoryId = "Raid07")
    private String state_raidGameName07;
    @State(desc = "Raid #07: Title", defaultValue = "", categoryId = "Raid07")
    private String state_raidTitle07;
    @State(desc = "Raid #07: Short Title", defaultValue = "", categoryId = "Raid07")
    private String state_raidShortTitle07;
    @State(desc = "Raid #07: Is Mature", defaultValue = "", categoryId = "Raid07")
    private String state_raidIsMature07;
    @State(desc = "Raid #07: Thumbnail", defaultValue = "", categoryId = "Raid07")
    private String state_raidThumbnail07;
    @State(desc = "Raid #07: Viewer Count", defaultValue = "", categoryId = "Raid07")
    private String state_raidViewerCount07;
    @State(desc = "Raid #07: Url", defaultValue = "", categoryId = "Raid07")
    private String state_raidUrl07;

    @State(desc = "Raid #08: User Id", defaultValue = "", categoryId = "Raid08")
    private String state_raidUserId08;
    @State(desc = "Raid #08: User Name", defaultValue = "", categoryId = "Raid08")
    private String state_raidUsername08;
    @State(desc = "Raid #08: User Thumbnail", defaultValue = "", categoryId = "Raid08")
    private String state_raidUserThumbnail08;
    @State(desc = "Raid #08: Game Name", defaultValue = "", categoryId = "Raid08")
    private String state_raidGameName08;
    @State(desc = "Raid #08: Title", defaultValue = "", categoryId = "Raid08")
    private String state_raidTitle08;
    @State(desc = "Raid #08: Short Title", defaultValue = "", categoryId = "Raid08")
    private String state_raidShortTitle08;
    @State(desc = "Raid #08: Is Mature", defaultValue = "", categoryId = "Raid08")
    private String state_raidIsMature08;
    @State(desc = "Raid #08: Thumbnail", defaultValue = "", categoryId = "Raid08")
    private String state_raidThumbnail08;
    @State(desc = "Raid #08: Viewer Count", defaultValue = "", categoryId = "Raid08")
    private String state_raidViewerCount08;
    @State(desc = "Raid #08: Url", defaultValue = "", categoryId = "Raid08")
    private String state_raidUrl08;

    @State(desc = "Raid #09: User Id", defaultValue = "", categoryId = "Raid09")
    private String state_raidUserId09;
    @State(desc = "Raid #09: User Name", defaultValue = "", categoryId = "Raid09")
    private String state_raidUsername09;
    @State(desc = "Raid #09: User Thumbnail", defaultValue = "", categoryId = "Raid09")
    private String state_raidUserThumbnail09;
    @State(desc = "Raid #09: Game Name", defaultValue = "", categoryId = "Raid09")
    private String state_raidGameName09;
    @State(desc = "Raid #09: Title", defaultValue = "", categoryId = "Raid09")
    private String state_raidTitle09;
    @State(desc = "Raid #09: Short Title", defaultValue = "", categoryId = "Raid09")
    private String state_raidShortTitle09;
    @State(desc = "Raid #09: Is Mature", defaultValue = "", categoryId = "Raid09")
    private String state_raidIsMature09;
    @State(desc = "Raid #09: Thumbnail", defaultValue = "", categoryId = "Raid09")
    private String state_raidThumbnail09;
    @State(desc = "Raid #09: Viewer Count", defaultValue = "", categoryId = "Raid09")
    private String state_raidViewerCount09;
    @State(desc = "Raid #09: Url", defaultValue = "", categoryId = "Raid09")
    private String state_raidUrl09;

    @State(desc = "Raid #10: User Id", defaultValue = "", categoryId = "Raid10")
    private String state_raidUserId10;
    @State(desc = "Raid #10: User Name", defaultValue = "", categoryId = "Raid10")
    private String state_raidUsername10;
    @State(desc = "Raid #10: User Thumbnail", defaultValue = "", categoryId = "Raid10")
    private String state_raidUserThumbnail10;
    @State(desc = "Raid #10: Game Name", defaultValue = "", categoryId = "Raid10")
    private String state_raidGameName10;
    @State(desc = "Raid #10: Title", defaultValue = "", categoryId = "Raid10")
    private String state_raidTitle10;
    @State(desc = "Raid #10: Short Title", defaultValue = "", categoryId = "Raid10")
    private String state_raidShortTitle10;
    @State(desc = "Raid #10: Is Mature", defaultValue = "", categoryId = "Raid10")
    private String state_raidIsMature10;
    @State(desc = "Raid #10: Thumbnail", defaultValue = "", categoryId = "Raid10")
    private String state_raidThumbnail10;
    @State(desc = "Raid #10: Viewer Count", defaultValue = "", categoryId = "Raid10")
    private String state_raidViewerCount10;
    @State(desc = "Raid #10: Url", defaultValue = "", categoryId = "Raid10")
    private String state_raidUrl10;

    @State(desc = "Raid #11: User Id", defaultValue = "", categoryId = "Raid11")
    private String state_raidUserId11;
    @State(desc = "Raid #11: User Name", defaultValue = "", categoryId = "Raid11")
    private String state_raidUsername11;
    @State(desc = "Raid #11: User Thumbnail", defaultValue = "", categoryId = "Raid11")
    private String state_raidUserThumbnail11;
    @State(desc = "Raid #11: Game Name", defaultValue = "", categoryId = "Raid11")
    private String state_raidGameName11;
    @State(desc = "Raid #11: Title", defaultValue = "", categoryId = "Raid11")
    private String state_raidTitle11;
    @State(desc = "Raid #1: Short Title", defaultValue = "", categoryId = "Raid11")
    private String state_raidShortTitle11;
    @State(desc = "Raid #11: Is Mature", defaultValue = "", categoryId = "Raid11")
    private String state_raidIsMature11;
    @State(desc = "Raid #11: Thumbnail", defaultValue = "", categoryId = "Raid11")
    private String state_raidThumbnail11;
    @State(desc = "Raid #11: Viewer Count", defaultValue = "", categoryId = "Raid11")
    private String state_raidViewerCount11;
    @State(desc = "Raid #11: Url", defaultValue = "", categoryId = "Raid11")
    private String state_raidUrl11;

    @State(desc = "Raid #12: User Id", defaultValue = "", categoryId = "Raid12")
    private String state_raidUserId12;
    @State(desc = "Raid #12: User Name", defaultValue = "", categoryId = "Raid12")
    private String state_raidUsername12;
    @State(desc = "Raid #12: User Thumbnail", defaultValue = "", categoryId = "Raid12")
    private String state_raidUserThumbnail12;
    @State(desc = "Raid #12: Game Name", defaultValue = "", categoryId = "Raid12")
    private String state_raidGameName12;
    @State(desc = "Raid #12: Title", defaultValue = "", categoryId = "Raid12")
    private String state_raidTitle12;
    @State(desc = "Raid #12: Short Title", defaultValue = "", categoryId = "Raid12")
    private String state_raidShortTitle12;
    @State(desc = "Raid #12: Is Mature", defaultValue = "", categoryId = "Raid12")
    private String state_raidIsMature12;
    @State(desc = "Raid #12: Thumbnail", defaultValue = "", categoryId = "Raid12")
    private String state_raidThumbnail12;
    @State(desc = "Raid #12: Viewer Count", defaultValue = "", categoryId = "Raid12")
    private String state_raidViewerCount12;
    @State(desc = "Raid #12: Url", defaultValue = "", categoryId = "Raid12")
    private String state_raidUrl12;

    @State(desc = "Raid #13: User Id", defaultValue = "", categoryId = "Raid13")
    private String state_raidUserId13;
    @State(desc = "Raid #13: User Name", defaultValue = "", categoryId = "Raid13")
    private String state_raidUsername13;
    @State(desc = "Raid #13: User Thumbnail", defaultValue = "", categoryId = "Raid13")
    private String state_raidUserThumbnail13;
    @State(desc = "Raid #13: Game Name", defaultValue = "", categoryId = "Raid13")
    private String state_raidGameName13;
    @State(desc = "Raid #13: Title", defaultValue = "", categoryId = "Raid13")
    private String state_raidTitle13;
    @State(desc = "Raid #13: Short Title", defaultValue = "", categoryId = "Raid13")
    private String state_raidShortTitle13;
    @State(desc = "Raid #13: Is Mature", defaultValue = "", categoryId = "Raid13")
    private String state_raidIsMature13;
    @State(desc = "Raid #13: Thumbnail", defaultValue = "", categoryId = "Raid13")
    private String state_raidThumbnail13;
    @State(desc = "Raid #13: Viewer Count", defaultValue = "", categoryId = "Raid13")
    private String state_raidViewerCount13;
    @State(desc = "Raid #13: Url", defaultValue = "", categoryId = "Raid13")
    private String state_raidUrl13;

    @State(desc = "Raid #14: User Id", defaultValue = "", categoryId = "Raid14")
    private String state_raidUserId14;
    @State(desc = "Raid #14: User Name", defaultValue = "", categoryId = "Raid14")
    private String state_raidUsername14;
    @State(desc = "Raid #14: User Thumbnail", defaultValue = "", categoryId = "Raid14")
    private String state_raidUserThumbnail14;
    @State(desc = "Raid #14: Game Name", defaultValue = "", categoryId = "Raid14")
    private String state_raidGameName14;
    @State(desc = "Raid #14: Title", defaultValue = "", categoryId = "Raid14")
    private String state_raidTitle14;
    @State(desc = "Raid #14: Short Title", defaultValue = "", categoryId = "Raid14")
    private String state_raidShortTitle14;
    @State(desc = "Raid #14: Is Mature", defaultValue = "", categoryId = "Raid14")
    private String state_raidIsMature14;
    @State(desc = "Raid #14: Thumbnail", defaultValue = "", categoryId = "Raid14")
    private String state_raidThumbnail14;
    @State(desc = "Raid #14: Viewer Count", defaultValue = "", categoryId = "Raid14")
    private String state_raidViewerCount14;
    @State(desc = "Raid #14: Url", defaultValue = "", categoryId = "Raid14")
    private String state_raidUrl14;

    @State(desc = "Raid #15: User Id", defaultValue = "", categoryId = "Raid15")
    private String state_raidUserId15;
    @State(desc = "Raid #15: User Name", defaultValue = "", categoryId = "Raid15")
    private String state_raidUsername15;
    @State(desc = "Raid #15: User Thumbnail", defaultValue = "", categoryId = "Raid15")
    private String state_raidUserThumbnail15;
    @State(desc = "Raid #15: Game Name", defaultValue = "", categoryId = "Raid15")
    private String state_raidGameName15;
    @State(desc = "Raid #15: Title", defaultValue = "", categoryId = "Raid15")
    private String state_raidTitle15;
    @State(desc = "Raid #15: Short Title", defaultValue = "", categoryId = "Raid15")
    private String state_raidShortTitle15;
    @State(desc = "Raid #15: Is Mature", defaultValue = "", categoryId = "Raid15")
    private String state_raidIsMature15;
    @State(desc = "Raid #15: Thumbnail", defaultValue = "", categoryId = "Raid15")
    private String state_raidThumbnail15;
    @State(desc = "Raid #15: Viewer Count", defaultValue = "", categoryId = "Raid15")
    private String state_raidViewerCount15;
    @State(desc = "Raid #15: Url", defaultValue = "", categoryId = "Raid15")
    private String state_raidUrl15;

    @State(desc = "Raid #16: User Id", defaultValue = "", categoryId = "Raid16")
    private String state_raidUserId16;
    @State(desc = "Raid #16: User Name", defaultValue = "", categoryId = "Raid16")
    private String state_raidUsername16;
    @State(desc = "Raid #16: User Thumbnail", defaultValue = "", categoryId = "Raid16")
    private String state_raidUserThumbnail16;
    @State(desc = "Raid #16: Game Name", defaultValue = "", categoryId = "Raid16")
    private String state_raidGameName16;
    @State(desc = "Raid #16: Title", defaultValue = "", categoryId = "Raid16")
    private String state_raidTitle16;
    @State(desc = "Raid #16: Short Title", defaultValue = "", categoryId = "Raid16")
    private String state_raidShortTitle16;
    @State(desc = "Raid #16: Is Mature", defaultValue = "", categoryId = "Raid16")
    private String state_raidIsMature16;
    @State(desc = "Raid #16: Thumbnail", defaultValue = "", categoryId = "Raid16")
    private String state_raidThumbnail16;
    @State(desc = "Raid #16: Viewer Count", defaultValue = "", categoryId = "Raid16")
    private String state_raidViewerCount16;
    @State(desc = "Raid #16: Url", defaultValue = "", categoryId = "Raid16")
    private String state_raidUrl16;

    @State(desc = "Raid #17: User Id", defaultValue = "", categoryId = "Raid17")
    private String state_raidUserId17;
    @State(desc = "Raid #17: User Name", defaultValue = "", categoryId = "Raid17")
    private String state_raidUsername17;
    @State(desc = "Raid #17: User Thumbnail", defaultValue = "", categoryId = "Raid17")
    private String state_raidUserThumbnail17;
    @State(desc = "Raid #17: Game Name", defaultValue = "", categoryId = "Raid17")
    private String state_raidGameName17;
    @State(desc = "Raid #17: Title", defaultValue = "", categoryId = "Raid17")
    private String state_raidTitle17;
    @State(desc = "Raid #17: Short Title", defaultValue = "", categoryId = "Raid17")
    private String state_raidShortTitle17;
    @State(desc = "Raid #17: Is Mature", defaultValue = "", categoryId = "Raid17")
    private String state_raidIsMature17;
    @State(desc = "Raid #17: Thumbnail", defaultValue = "", categoryId = "Raid17")
    private String state_raidThumbnail17;
    @State(desc = "Raid #17: Viewer Count", defaultValue = "", categoryId = "Raid17")
    private String state_raidViewerCount17;
    @State(desc = "Raid #17: Url", defaultValue = "", categoryId = "Raid17")
    private String state_raidUrl17;

    @State(desc = "Raid #18: User Id", defaultValue = "", categoryId = "Raid18")
    private String state_raidUserId18;
    @State(desc = "Raid #18: User Name", defaultValue = "", categoryId = "Raid18")
    private String state_raidUsername18;
    @State(desc = "Raid #18: User Thumbnail", defaultValue = "", categoryId = "Raid18")
    private String state_raidUserThumbnail18;
    @State(desc = "Raid #18: Game Name", defaultValue = "", categoryId = "Raid18")
    private String state_raidGameName18;
    @State(desc = "Raid #18: Title", defaultValue = "", categoryId = "Raid18")
    private String state_raidTitle18;
    @State(desc = "Raid #18: Short Title", defaultValue = "", categoryId = "Raid18")
    private String state_raidShortTitle18;
    @State(desc = "Raid #18: Is Mature", defaultValue = "", categoryId = "Raid18")
    private String state_raidIsMature18;
    @State(desc = "Raid #18: Thumbnail", defaultValue = "", categoryId = "Raid18")
    private String state_raidThumbnail18;
    @State(desc = "Raid #18: Viewer Count", defaultValue = "", categoryId = "Raid18")
    private String state_raidViewerCount18;
    @State(desc = "Raid #18: Url", defaultValue = "", categoryId = "Raid18")
    private String state_raidUrl18;

    @State(desc = "Raid #19: User Id", defaultValue = "", categoryId = "Raid19")
    private String state_raidUserId19;
    @State(desc = "Raid #19: User Name", defaultValue = "", categoryId = "Raid19")
    private String state_raidUsername19;
    @State(desc = "Raid #19: User Thumbnail", defaultValue = "", categoryId = "Raid19")
    private String state_raidUserThumbnail19;
    @State(desc = "Raid #19: Game Name", defaultValue = "", categoryId = "Raid19")
    private String state_raidGameName19;
    @State(desc = "Raid #19: Title", defaultValue = "", categoryId = "Raid19")
    private String state_raidTitle19;
    @State(desc = "Raid #19: Short Title", defaultValue = "", categoryId = "Raid19")
    private String state_raidShortTitle19;
    @State(desc = "Raid #19: Is Mature", defaultValue = "", categoryId = "Raid19")
    private String state_raidIsMature19;
    @State(desc = "Raid #19: Thumbnail", defaultValue = "", categoryId = "Raid19")
    private String state_raidThumbnail19;
    @State(desc = "Raid #19: Viewer Count", defaultValue = "", categoryId = "Raid19")
    private String state_raidViewerCount19;
    @State(desc = "Raid #19: Url", defaultValue = "", categoryId = "Raid19")
    private String state_raidUrl19;

    @State(desc = "Raid #20: User Id", defaultValue = "", categoryId = "Raid20")
    private String state_raidUserId20;
    @State(desc = "Raid #20: User Name", defaultValue = "", categoryId = "Raid20")
    private String state_raidUsername20;
    @State(desc = "Raid #20: User Thumbnail", defaultValue = "", categoryId = "Raid20")
    private String state_raidUserThumbnail20;
    @State(desc = "Raid #20: Game Name", defaultValue = "", categoryId = "Raid20")
    private String state_raidGameName20;
    @State(desc = "Raid #20: Title", defaultValue = "", categoryId = "Raid20")
    private String state_raidTitle20;
    @State(desc = "Raid #20: Short Title", defaultValue = "", categoryId = "Raid20")
    private String state_raidShortTitle20;
    @State(desc = "Raid #20: Is Mature", defaultValue = "", categoryId = "Raid20")
    private String state_raidIsMature20;
    @State(desc = "Raid #20: Thumbnail", defaultValue = "", categoryId = "Raid20")
    private String state_raidThumbnail20;
    @State(desc = "Raid #20: Viewer Count", defaultValue = "", categoryId = "Raid20")
    private String state_raidViewerCount20;
    @State(desc = "Raid #20: Url", defaultValue = "", categoryId = "Raid20")
    private String state_raidUrl20;

    public RaidAssistantPlugin() {
        super(true);

        this.connectThenPairAndListen(this);
    }

    private void initializeClientAndStart() {
        if (!this.areSettingsFilled()) {
            return;
        }

        this.client = TwitchClientBuilder.builder()
                .withClientId(this.clientId)
                .withClientSecret(this.clientSecret)
                .withEnableHelix(true)
                .build();

        String userNameLowerCase =  this.setting_userName.trim().toLowerCase();
        this.getUsersInformation(userNamesNotInCache(Collections.singletonList(userNameLowerCase)));
        this.userId = users.get(userNameLowerCase).getId();

        this.startScheduledExecutor();
    }

    private boolean areSettingsFilled() {
        String message_MissingSetting = "Bad configuration > Missing setting: {0}";

        if (this.setting_userName == null || this.setting_userName.trim().isEmpty()) {
            this.hasError(MessageFormat.format(message_MissingSetting,
                    RaidAssistantPluginConstants.Settings.Setting_userName.NAME));
            return false;
        }

        this.hasNoError();
        return true;
    }

    private void hasError(String errorMessage) {
        this.state_requestHasError = "TRUE";
        this.state_requestErrorMessage = errorMessage;
        this.sendStateUpdate(RaidAssistantPluginConstants.General.States.State_requestHasError.ID,
                this.state_requestHasError, true, true);
        this.sendStateUpdate(RaidAssistantPluginConstants.General.States.State_requestErrorMessage.ID,
                this.state_requestErrorMessage, true, true);
        RaidAssistantPlugin.LOGGER.log(Level.WARNING, errorMessage);
    }

    private void hasNoError() {
        this.state_requestHasError = "FALSE";
        this.state_requestErrorMessage = "";
        this.sendStateUpdate(RaidAssistantPluginConstants.General.States.State_requestHasError.ID,
                this.state_requestHasError, true, true);
        this.sendStateUpdate(RaidAssistantPluginConstants.General.States.State_requestErrorMessage.ID,
                this.state_requestErrorMessage, true, true);
    }

    private @NotNull List<String> userNamesNotInCache(@NotNull List<String> usernames) {
        List<String> userNamesNotInCache = new ArrayList<>();
        for (String username : usernames) {
            if (!this.users.containsKey(username)) {
                userNamesNotInCache.add(username);
            }
        }
        return userNamesNotInCache;
    }

    private void getUsersInformation(List<String> usernames) {
        if (usernames != null && !usernames.isEmpty()) {
            List<User> queriedUsers = null;
            try {
                queriedUsers = this.client.getHelix()
                        .getUsers(null, null, usernames)
                        .execute()
                        .getUsers();
            } catch (HystrixRuntimeException | HystrixBadRequestException | IllegalStateException e) {
                this.hasError(MessageFormat.format("Twitch API Client Error > {0}", e.getMessage()));
            }
            if (queriedUsers != null) {
                for (User user : queriedUsers) {
                    this.imageUrlToBase64(user.getProfileImageUrl());
                    this.users.put(user.getDisplayName().toLowerCase(), user);
                }
                this.hasNoError();
            }
        }
    }

    private void startScheduledExecutor() {
        this.stopScheduledExecutor();

        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        this.scheduledExecutorService.scheduleAtFixedRate(this::refreshFollowersList,
                0, 20, TimeUnit.SECONDS);
    }

    private void stopScheduledExecutor() {
        if (this.scheduledExecutorService != null) {
            this.scheduledExecutorService.shutdownNow();
            this.scheduledExecutorService = null;
        }
    }

    private void refreshFollowersList() {
        RaidAssistantPlugin.LOGGER.log(Level.INFO, "RaidAssistantPlugin: refreshFollowersList");

        List<Stream> followedStreamsList = this.fetchFollowedStreamsAndUsersInformation();
        this.validateAuthKey(followedStreamsList);
        this.updatePluginStatesBasedOnFollowedStreams(followedStreamsList);
    }

    private @Nullable List<Stream> fetchFollowedStreamsAndUsersInformation() {
        StreamList followedStreams = null;
        try {
            followedStreams = this.client.getHelix()
                    .getFollowedStreams(this.setting_authToken, userId, null, this.fetchHowMany)
                    .execute();

            this.hasNoError();
            if (followedStreams == null) {
                return null;
            }

            List<Stream> followedStreamsList = followedStreams.getStreams();

            List<String> usernames = followedStreamsList.stream().map(Stream::getUserName).collect(Collectors.toList());
            this.getUsersInformation(userNamesNotInCache(usernames));

            this.state_totalChannelsOnline = Integer.toString(followedStreamsList.size());
            this.sendStateUpdate(RaidAssistantPluginConstants.General.States.State_totalChannelsOnline.ID,
                    this.state_totalChannelsOnline, true, true);


            return followedStreamsList;
        } catch(HystrixRuntimeException | HystrixBadRequestException | IllegalStateException e) {
            this.hasError(MessageFormat.format("Twitch API Client Error > {0}", e.getMessage()));
        }

        return null;
    }

    private void validateAuthKey(List<Stream> followedStreamsList) {
        if (followedStreamsList == null && this.state_requestHasError.equals("TRUE")) {
            sendShowNotification("mx.alfador.touchportal.RaidAssistantPlugin.AuthUpdate",
                    "RaidAssistant Plugin - Twitch Token Update",
                    "By clicking on the button you'll be directed to the Twitch Authorization Token page.",
                    new TPNotificationOption[] {
                            new TPNotificationOption("mx.alfador.touchportal.RaidAssistantPlugin.AuthUpdate.OpenWebsite", "Open")
                    });
        } else {
            this.updatePluginStatesBasedOnFollowedStreams(followedStreamsList);
        }
    }

    private void updatePluginStatesBasedOnFollowedStreams(List<Stream> followedStreamsList) {
        if (followedStreamsList != null) {
            for (int currentStreamIndex = 0; currentStreamIndex < this.fetchHowMany; currentStreamIndex++) {
                Stream currentStream = (currentStreamIndex < followedStreamsList.size())
                        ? followedStreamsList.get(currentStreamIndex) :
                        null;

                if (currentStream == null) {
                    this.emptyRaidByNumber(currentStreamIndex);
                } else {
                    this.setRaidByNumber(currentStreamIndex, currentStream);
                }
            }
        }
    }

    private void emptyRaidByNumber(int raidNumber) {
        updateField(raidNumber, "raidUserId", "");
        updateField(raidNumber, "raidUsername", "");
        updateField(raidNumber, "raidUserThumbnail", "");
        updateField(raidNumber, "raidGameName", "");
        updateField(raidNumber, "raidTitle", "");
        updateField(raidNumber, "raidShortTitle", "");
        updateField(raidNumber, "raidIsMature", "");
        updateField(raidNumber, "raidThumbnail", "");
        updateField(raidNumber, "raidViewerCount", "");
        updateField(raidNumber, "raidUrl", "");
    }

    private void setRaidByNumber(int raidNumber, @NotNull Stream currentStream) {
        User currentUser = users.get(currentStream.getUserName().toLowerCase());

        String raidUserId = currentUser.getId();
        String raidUsername = currentUser.getDisplayName();
        String raidUserThumbnail = this.imageUrlToBase64(currentUser.getProfileImageUrl());
        String raidGameName = currentStream.getGameName();
        String raidTitle = currentStream.getTitle();
        String raidShortTitle = shortenTitle(raidTitle);
        String raidIsMature = currentStream.isMature().toString();
        String raidThumbnail = this.imageUrlToBase64(currentStream.getThumbnailUrl()
                .replace("{width}","440")
                .replace("{height}","218"));
        String raidViewerCount = currentStream.getViewerCount().toString();
        String raidUrl = "https://twitch.tv/" + raidUsername;

        updateField(raidNumber, "raidUserId", raidUserId);
        updateField(raidNumber, "raidUsername", raidUsername);
        updateField(raidNumber, "raidUserThumbnail", raidUserThumbnail);
        updateField(raidNumber, "raidGameName", raidGameName);
        updateField(raidNumber, "raidTitle", raidTitle);
        updateField(raidNumber, "raidShortTitle", raidShortTitle);
        updateField(raidNumber, "raidIsMature", raidIsMature);
        updateField(raidNumber, "raidThumbnail", raidThumbnail);
        updateField(raidNumber, "raidViewerCount", raidViewerCount);
        updateField(raidNumber, "raidUrl", raidUrl);
    }

    private void updateField(int raidNumber, String fieldName, String value) {
        String formattedRaidNumber = String.format("%02d", raidNumber + 1);
        String formattedFieldName = MessageFormat.format("state_{0}{1}", fieldName, formattedRaidNumber);

        try {
            Field field = getClass().getDeclaredField(formattedFieldName);
            String fieldId =
                    MessageFormat.format("mx.alfador.touchportal.RaidAssistantPlugin.Raid{1}.state.state_{0}{1}",
                            fieldName, formattedRaidNumber);
            field.set(this, value);
            this.sendStateUpdate(fieldId, value, true, true);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            String errorMessage =
                    MessageFormat.format("Error for field: {0} > {1}", fieldName, e.getMessage());
            LOGGER.severe(errorMessage);
            this.hasError(errorMessage);
        }
    }

    private String imageUrlToBase64(String imageUrl) {
        if (this.base64Images.containsKey(imageUrl)) {
            return this.base64Images.get(imageUrl);
        }

        String base64 = "";
        ByteArrayOutputStream byteArrayOutputStream = null;
        try {
            BufferedImage bufferedImage = ImageIO.read(new URL(imageUrl));
            int imageSize = 128;
            BufferedImage resizedBufferedImage = Scalr.resize(bufferedImage, Scalr.Mode.FIT_TO_HEIGHT, imageSize);
            ImageIO.write(resizedBufferedImage, "png", byteArrayOutputStream = new ByteArrayOutputStream());
            base64 = Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray());
            this.base64Images.put(imageUrl, base64);
        } catch (Exception exception) {
            LOGGER.severe(exception.getMessage() + " for Image URL: " + imageUrl);
        } finally {
            if (byteArrayOutputStream != null) {
                try {
                    byteArrayOutputStream.close();
                } catch (IOException ignored) { }
            }
        }
        return base64;
    }

    private @NotNull String shortenTitle(@NotNull String title) {
        @NotNull String result;
        if (title.length() <= 20) {
            result = title;
        } else {
            result = title.substring(0, 17) + "...";
        }
        return result;
    }

    private void OpenBrowser(String openUrl) {
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            try {
                desktop.browse(URI.create(openUrl));
            } catch (IOException ioException) {
                RaidAssistantPlugin.LOGGER.log(Level.SEVERE,
                        Arrays.toString(ioException.getStackTrace()));
            }
        }
    }

    @Override
    public void onDisconnected(Exception exception) {
        stopScheduledExecutor();
        System.exit(0);
    }

    @Override
    public void onReceived(JsonObject jsonMessage) { }

    @Override
    public void onInfo(TPInfoMessage tpInfoMessage) {
        this.initializeClientAndStart();
    }

    @Override
    public void onListChanged(TPListChangedMessage tpListChangedMessage) { }

    @Override
    public void onBroadcast(TPBroadcastMessage tpBroadcastMessage) { }

    @Override
    public void onSettings(TPSettingsMessage tpSettingsMessage) {
        this.initializeClientAndStart();
    }

    @Override
    public void onNotificationOptionClicked(@NotNull TPNotificationOptionClickedMessage tpNotificationOptionClickedMessage) {
        if (tpNotificationOptionClickedMessage.notificationId
                .equals("mx.alfador.touchportal.RaidAssistantPlugin.AuthUpdate")) {
            if (tpNotificationOptionClickedMessage.optionId
                    .equals("mx.alfador.touchportal.RaidAssistantPlugin.AuthUpdate.OpenWebsite")) {
                    String openUrl = MessageFormat.format(this.tokenAuthGeneration_url, this.clientResponseType,
                        this.clientId, this.clientRedirectUri, this.clientScope);
                    OpenBrowser(openUrl);
            }
        }
    }

    public static void main(String[] args) {
        if (args != null && args.length == 1 && PluginHelper.COMMAND_START.equals(args[0])) {
            new RaidAssistantPlugin();

            RaidAssistantPlugin.LOGGER.log(Level.INFO, "RaidAssistantPlugin: Let the party begin!");
        }
    }
}
