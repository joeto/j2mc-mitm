package org.kitteh.antimitm;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.HashSet;
import java.util.logging.Filter;
import java.util.logging.LogRecord;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class Anti extends JavaPlugin implements Listener {
    private static final Pattern ipattern = Pattern.compile("(?<=(^|[(\\p{Space}|\\p{Punct})]))((1?[0-9]{1,2}|2[0-4][0-9]|25[0-5])\\.){3}(1?[0-9]{1,2}|2[0-4][0-9]|25[0-5])(?=([(\\p{Space}|\\p{Punct})]|$))");
    //Thank you internet for terrible regex
    private final HashSet<String> ips = new HashSet<String>();

    @EventHandler
    public void onChat(PlayerChatEvent event) {
        this.screen(event.getMessage());
    }

    @EventHandler
    public void onCmd(PlayerCommandPreprocessEvent event) {
        this.screen(event.getMessage());
    }

    private void screen(String string) {
        final Matcher matcher = Anti.ipattern.matcher(string);
        if (matcher.find()) {
            final String ip = matcher.group();
            if (!this.ips.contains(ip)) {
                this.ips.add(ip);
                this.getServer().getScheduler().scheduleAsyncDelayedTask(this, new attempt(ip));
            }
        }
    }

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);
        this.getServer().getLogger().setFilter(new Filter() {

            @Override
            public boolean isLoggable(LogRecord record) {
                final String message = record.getMessage();
                if (message.contains("Failed to verify username!") && message.contains("notch") && !message.contains("<")) {
                    final Matcher matcher = Anti.ipattern.matcher(message);
                    if (matcher.find()) {
                        final String ip = matcher.group();
                        Anti.this.getServer().banIP(ip);
                    }
                }
                return true;
            }

        });
    }

    private class attempt implements Runnable {
        private final String ip;

        public attempt(String ip) {
            this.ip = ip;
        }

        @Override
        public void run() {
            try {
                final Socket sock = new Socket(this.ip, 25565);
                final DataOutputStream out = new DataOutputStream(sock.getOutputStream());
                out.writeByte((byte) 2);
                this.send(out, "notch;" + this.ip + ";25565");
                out.writeByte((byte) 1);
                out.writeInt(29);
                this.send(out, "notch");
                this.send(out, "");
                out.writeInt(0);
                out.writeInt(0);
                out.writeByte(0);
                out.writeByte(0);
                out.writeByte(0);
                out.close();
                sock.close();
            } catch (final Exception e) {
            }
        }

        private void send(DataOutputStream out, String x) throws IOException {
            final short len = (short) x.length();
            final char[] cs = new char[len];
            x.getChars(0, x.length(), cs, 0);
            out.writeShort(len);
            for (int i = 0; i < x.length(); i++) {
                out.writeShort((short) cs[i]);
            }
        }

    }

}
