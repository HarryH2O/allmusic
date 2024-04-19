package com.coloryr.allmusic.server.core;

import com.coloryr.allmusic.server.core.music.api.APIMain;
import com.coloryr.allmusic.server.core.music.play.MusicSearch;
import com.coloryr.allmusic.server.core.music.play.PlayGo;
import com.coloryr.allmusic.server.core.music.play.PlayMusic;
import com.coloryr.allmusic.server.core.objs.CookieObj;
import com.coloryr.allmusic.server.core.objs.config.ConfigObj;
import com.coloryr.allmusic.server.core.objs.message.MessageObj;
import com.coloryr.allmusic.server.core.objs.music.SearchPageObj;
import com.coloryr.allmusic.server.core.side.IMyLogger;
import com.coloryr.allmusic.server.core.side.ISide;
import com.coloryr.allmusic.server.core.sql.DataSql;
import com.coloryr.allmusic.server.core.sql.IEconomy;
import com.coloryr.allmusic.server.core.utils.Logs;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public class AllMusic {
    public static final Gson gson = new Gson();

    /**
     * 客户端插件信道名
     */
    public static final String channel = "allmusic:channel";
    /**
     * BC插件信道名
     */
    public static final String channelBC = "allmusic:channelbc";
    /**
     * 插件版本号
     */
    public static final String version = "3.0.0";
    /**
     * 配置文件版本号
     */
    public static final String configVersion = "200";
    /**
     * 语言文件配置版本号
     */
    public static final String messageVersion = "200";
    /**
     * 搜歌结果
     * 玩家名 结果
     */
    private static final Map<String, SearchPageObj> searchSave = new HashMap<>();
    /**
     * 切歌投票的玩家
     */
    private static final Set<String> votePlayer = new HashSet<>();
    /**
     * 插歌投票的玩家
     */
    private static final Set<String> pushPlayer = new HashSet<>();
    /**
     * 正在播放的玩家
     */
    private static final Set<String> nowPlayPlayer = new HashSet<>();
    /**
     * 暂时不往玩家发送数据包
     */
    private static final Set<String> pauseSendPlayer = new HashSet<>();
    /**
     * 日志
     */
    public static IMyLogger log;
    /**
     * 服务器端操作
     */
    public static ISide side;
    /**
     * 是否在运行
     */
    public static boolean isRun;
    /**
     * Cookie对象
     */
    public static CookieObj cookie;
    /**
     * 经济插件对象
     */
    public static IEconomy economy;
    /**
     * 网易API
     */
    private static APIMain apiMusic;
    /**
     * 配置对象
     */
    private static ConfigObj config;
    /**
     * 语言对象
     */
    private static MessageObj message;
    /**
     * 配置文件
     */
    private static File configFile;
    /**
     * Cookie文件
     */
    private static File cookieFile;
    /**
     * 语言文件
     */
    private static File messageFile;

    /**
     * 检查配置文件完整性
     */
    public static void configCheck() {
        if (config == null || config.check()) {
            config = ConfigObj.make();
            log.warning("§d[AllMusic3]§c配置文件config.json错误，已覆盖");
            saveConfig();
        }
    }

    /**
     * 检查语言文件完整性
     */
    private static void messageCheck() {
        if (message == null || message.check()) {
            message = MessageObj.make();
            log.warning("§d[AllMusic3]§c配置文件message.json错误，已覆盖");
            saveMessage();
        }
    }

    /**
     * 检查是否需要
     *
     * @param name      用户名
     * @param server    服务器名
     * @param checkList 是否检查正在播放的列表
     * @return 结果
     */
    public static boolean isOK(String name, String server, boolean checkList) {
        try {
            if (server != null && AllMusic.getConfig().muteServer.contains(server))
                return true;
            if (AllMusic.getConfig().mutePlayer.contains(name))
                return true;
            if (AllMusic.isPause(name))
                return true;
            if (!checkList)
                return false;
            return AllMusic.containNowPlay(name);
        } catch (NoSuchElementException e) {
            return true;
        }
    }

    /**
     * 是否存在正在播放的玩家
     *
     * @param player 用户名
     * @return 是否存在
     */
    public static boolean containNowPlay(String player) {
        return !nowPlayPlayer.contains(player);
    }

    /**
     * 获取配置文件
     *
     * @return 配置对象
     */
    public static ConfigObj getConfig() {
        if (config == null) {
            log.warning("§d[AllMusic3]§c配置文件config.json错误，已使用默认配置文件");
            config = ConfigObj.make();
        }
        return config;
    }

    /**
     * 获取语言文件
     *
     * @return 语言对象
     */
    public static MessageObj getMessage() {
        if (message == null) {
            log.warning("§d[AllMusic3]§c配置文件message.json错误，已使用默认配置文件");
            message = MessageObj.make();
        }
        return message;
    }

    /**
     * 添加搜歌结果
     *
     * @param player 用户名
     * @param page   结果
     */
    public static void addSearch(String player, SearchPageObj page) {
        searchSave.put(player, page);
    }

    /**
     * 获取搜歌结果
     *
     * @param player 用户名
     * @return 结果
     */
    public static SearchPageObj getSearch(String player) {
        return searchSave.get(player);
    }

    /**
     * 删除搜歌结果
     *
     * @param player 用户名
     */
    public static void removeSearch(String player) {
        searchSave.remove(player);
    }

    /**
     * 添加投票的玩家
     *
     * @param player 用户名
     */
    public static void addVote(String player) {
        votePlayer.add(player);
    }

    /**
     * 添加投票的玩家
     *
     * @param player 用户名
     */
    public static void addPush(String player) {
        pushPlayer.add(player);
    }

    /**
     * 获取投票数量
     *
     * @return 数量
     */
    public static int getVoteCount() {
        return votePlayer.size();
    }

    /**
     * 清空投票
     */
    public static void clearVote() {
        votePlayer.clear();
    }

    public static void clearPush() {
        pushPlayer.clear();
    }

    /**
     * 是否已经投票了
     *
     * @param player 用户名
     * @return 结果
     */
    public static boolean containVote(String player) {
        return votePlayer.contains(player);
    }

    public static boolean containPush(String player) {
        return pushPlayer.contains(player);
    }

    /**
     * 添加正在播放的玩家
     *
     * @param player 用户名
     */
    public static void addNowPlayPlayer(String player) {
        nowPlayPlayer.add(player);
    }

    /**
     * 删除正在播放的玩家
     *
     * @param player 用户名
     */
    public static void removeNowPlayPlayer(String player) {
        nowPlayPlayer.remove(player);
        pauseSendPlayer.remove(player);
    }

    /**
     * 清空正在播放玩家的列表
     */
    public static void clearNowPlayer() {
        nowPlayPlayer.clear();
    }

    /**
     * 保存配置文件
     */
    public static void saveConfig() {
        try {
            String data = new GsonBuilder().setPrettyPrinting().create().toJson(config);
            FileOutputStream out = new FileOutputStream(configFile);
            OutputStreamWriter write = new OutputStreamWriter(
                    out, StandardCharsets.UTF_8);
            write.write(data);
            write.close();
            out.close();
        } catch (Exception e) {
            log.warning("§d[AllMusic3]§c配置文件保存错误");
            e.printStackTrace();
        }
    }

    public static void saveMessage() {
        try {
            String data = new GsonBuilder().setPrettyPrinting().create().toJson(message);
            FileOutputStream out = new FileOutputStream(messageFile);
            OutputStreamWriter write = new OutputStreamWriter(
                    out, StandardCharsets.UTF_8);
            write.write(data);
            write.close();
            out.close();
        } catch (Exception e) {
            log.warning("§d[AllMusic3]§c配置文件保存错误");
            e.printStackTrace();
        }
    }

    /**
     * 保存Cookie
     */
    public static void saveCookie() {
        try {
            String data = new GsonBuilder().setPrettyPrinting().create().toJson(cookie);
            FileOutputStream out = new FileOutputStream(cookieFile);
            OutputStreamWriter write = new OutputStreamWriter(
                    out, StandardCharsets.UTF_8);
            write.write(data);
            write.close();
        } catch (Exception e) {
            log.warning("§d[AllMusic3]§c配置文件保存错误");
            e.printStackTrace();
        }
    }

    /**
     * 启动插件
     */
    public static void start() {
        AllMusic.apiMusic = new APIMain();
        PlayMusic.start();
        PlayGo.start();
        MusicSearch.start();
        DataSql.start();

        log.info("§d[AllMusic3]§e已启动-" + version);
    }

    /**
     * 停止插件
     */
    public static void stop() {
        try {
            clearVote();
            Logs.stop();
            side.sendStop();
            MusicSearch.stop();
            PlayMusic.stop();
            PlayGo.stop();
            DataSql.stop();
        } catch (IOException e) {
            e.printStackTrace();
        }
        log.info("§d[AllMusic3]§2§e已停止，感谢使用");
    }

    /**
     * 获取音乐API
     *
     * @return 音乐API
     */
    public static APIMain getMusicApi() {
        return apiMusic;
    }

    /**
     * 加载配置文件
     */
    private static void loadConfig() {
        try {
            InputStreamReader reader = new InputStreamReader(
                    Files.newInputStream(configFile.toPath()), StandardCharsets.UTF_8);
            BufferedReader bf = new BufferedReader(reader);
            config = new Gson().fromJson(bf, ConfigObj.class);
            bf.close();
            reader.close();
            configCheck();

            reader = new InputStreamReader(Files.newInputStream(messageFile.toPath()), StandardCharsets.UTF_8);
            bf = new BufferedReader(reader);
            message = new Gson().fromJson(bf, MessageObj.class);
            bf.close();
            reader.close();
            messageCheck();

            log.info("§d[AllMusic3]§e当前语言配置文件版本为：" + messageVersion
                    + "，你的语言文件版本为：" + message.version);

            if (!message.version.equalsIgnoreCase(messageVersion)) {
                log.warning("§d[AllMusic3]§c语言文件版本号错误，运行可能会发生问题，请删除后重载");
            }

            reader = new InputStreamReader(Files.newInputStream(cookieFile.toPath()), StandardCharsets.UTF_8);
            bf = new BufferedReader(reader);
            cookie = new Gson().fromJson(bf, CookieObj.class);
            bf.close();
            reader.close();
            if (cookie == null) {
                cookie = new CookieObj();
                saveCookie();
            }

            log.info("§d[AllMusic3]§e当前插件配置文件版本为：" + configVersion
                    + "，你的配置文件版本为：" + config.version);

            if (!AllMusic.configVersion.equalsIgnoreCase(config.version)) {
                log.warning("§d[AllMusic3]§c请及时更新配置文件");
            }
        } catch (Exception e) {
            log.warning("§d[AllMusic3]§c读取配置文件错误");
            e.printStackTrace();
        }
    }

    /**
     * 加入时播放
     *
     * @param player 用户名
     */
    public static void joinPlay(String player) {
        if (getConfig().mutePlayer.contains(player) || nowPlayPlayer.contains(player)) {
            pauseSendPlayer.remove(player);
            return;
        }

        AllMusic.side.runTask(() -> {
            if (PlayMusic.nowPlayMusic != null && PlayGo.url != null) {
                AllMusic.side.sendHudPos(player);
                AllMusic.side.sendMusic(player, PlayGo.url);
                if (!PlayMusic.nowPlayMusic.isUrl()) {
                    AllMusic.side.runTask(() ->
                            AllMusic.side.sendPic(player, PlayMusic.nowPlayMusic.getPicUrl()), 15);
                }
                AllMusic.side.runTask(() ->
                        AllMusic.side.sendPos(player, PlayMusic.musicNowTime + 1000), 40);
            }

            pauseSendPlayer.remove(player);
        }, 40);
    }

    /**
     * 将玩家添加到暂停发送列表
     *
     * @param player 玩家名
     */
    public static void pauseSend(String player) {
        pauseSendPlayer.add(player);
    }

    /**
     * 暂停发送列表中是否存在玩家
     *
     * @param player 玩家名
     * @return 是否存在
     */
    public static boolean isPause(String player) {
        return pauseSendPlayer.contains(player);
    }

    /**
     * 读取配置文件
     *
     * @param file 配置文件文件夹
     */
    public void init(File file) {
        log.info("§d[AllMusic3]§2§e正在启动，感谢使用，本插件交流群：571239090");
        try {
            if (!file.exists())
                file.mkdir();
            if (configFile == null)
                configFile = new File(file, "config.json");
            if (messageFile == null)
                messageFile = new File(file, "message.json");
            if (cookieFile == null)
                cookieFile = new File(file, "cookie.json");
            if (Logs.file == null)
                Logs.file = new File(file, "logs.log");
            if (DataSql.sqlFile == null)
                DataSql.sqlFile = new File(file, "data.db");
            if (!configFile.exists()) {
                configFile.createNewFile();
            }
            if (!messageFile.exists()) {
                messageFile.createNewFile();
            }
            if (!cookieFile.exists()) {
                cookieFile.createNewFile();
            }
            if (!Logs.file.exists()) {
                Logs.file.createNewFile();
            }
            loadConfig();
            isRun = true;
        } catch (IOException e) {
            isRun = false;
            log.warning("§d[AllMusic3]§c启动失败");
            e.printStackTrace();
        }
    }
}
