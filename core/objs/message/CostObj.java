package com.coloryr.allmusic.server.core.objs.message;

public class CostObj {
    public String search;
    public String addMusic;
    public String noMoney;
    public String costFail;

    public boolean check() {
        if (search == null)
            return true;
        if (addMusic == null)
            return true;
        if (noMoney == null)
            return true;
        return costFail == null;
    }

    public void init() {
        search = "§d[AllMusic3]§e你搜歌花费了%Cost%";
        addMusic = "§d[AllMusic3]§e你点歌花费了%Cost%";
        noMoney = "§d[AllMusic3]§c你没有足够的钱";
        costFail = "§d[AllMusic3]§c扣钱过程中错误";
    }

    public static CostObj make() {
        CostObj obj = new CostObj();
        obj.init();

        return obj;
    }
}
