package com.coloryr.allmusic.server.core.command;

import com.coloryr.allmusic.server.core.AllMusic;
import com.coloryr.allmusic.server.core.utils.Function;

public class CommandAddList extends ACommand {
    @Override
    public void ex(Object sender, String name, String[] args) {
        if (args.length != 2) {
            AllMusic.side.sendMessage(sender, AllMusic.getMessage().command.error);
            return;
        }
        if (Function.isInteger(args[1])) {
            AllMusic.getMusicApi().setList(args[1], sender);
            AllMusic.side.sendMessage(sender, "§d[AllMusic3]§2添加空闲音乐列表" + args[1]);
        } else {
            AllMusic.side.sendMessage(sender, "§d[AllMusic3]§2请输入有效的音乐列表ID");
        }
    }
}
