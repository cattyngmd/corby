/*                          GNU GENERAL PUBLIC LICENSE
 *                            Version 3, 29 June 2007
 *
 *        Copyright (C) 2007 Free Software Foundation, Inc. <https://fsf.org/>
 *            Everyone is permitted to copy and distribute verbatim copies
 *             of this license document, but changing it is not allowed.
 */

package xyz.d1snin.corby;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.d1snin.corby.commands.Command;
import xyz.d1snin.corby.commands.admin.RestartCommand;
import xyz.d1snin.corby.commands.admin.ShutdownCommand;
import xyz.d1snin.corby.commands.fun.BottomCommand;
import xyz.d1snin.corby.commands.fun.CatCommand;
import xyz.d1snin.corby.commands.misc.HelpCommand;
import xyz.d1snin.corby.commands.misc.PingCommand;
import xyz.d1snin.corby.commands.misc.StealCommand;
import xyz.d1snin.corby.commands.settings.PrefixCommand;
import xyz.d1snin.corby.commands.settings.StarboardCommand;
import xyz.d1snin.corby.database.DatabaseManager;
import xyz.d1snin.corby.event.ReactionUpdateEvent;
import xyz.d1snin.corby.event.ServerJoinEvent;
import xyz.d1snin.corby.manager.config.Config;
import xyz.d1snin.corby.manager.config.ConfigFileManager;
import xyz.d1snin.corby.manager.config.ConfigManager;
import xyz.d1snin.corby.utils.ExceptionUtils;

import javax.security.auth.login.LoginException;
import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Corby {

  private static JDA api;

  private static final ScheduledExecutorService schedulerPresence =
      Executors.newSingleThreadScheduledExecutor();

  private static final List<String> presences = new ArrayList<>();
  public static final Set<Permission> permissions = new TreeSet<>();
  public static final List<Permission> defaultPermissions =
      Arrays.asList(
          Permission.MESSAGE_HISTORY,
          Permission.MESSAGE_READ,
          Permission.MESSAGE_WRITE,
          Permission.VIEW_CHANNEL,
          Permission.MANAGE_CHANNEL);
  private static final Random random = new Random();

  public static Config config;

  public static Logger logger = LoggerFactory.getLogger("loader");

  public static void main(String[] args) {
    try {
      ConfigFileManager.initConfigFile();

      logger.info("Starting...");

      permissions.addAll(defaultPermissions);

      start();

      startUpdatePresence();
    } catch (Exception e) {
      ExceptionUtils.processException(e);
    }
  }

  public static void start() throws LoginException, InterruptedException, IOException {

    config = ConfigManager.init();

    logger.info("Trying to connect to the database...");
    DatabaseManager.createConnection();

    JDABuilder jdaBuilder = JDABuilder.createDefault(config.token);

    jdaBuilder.enableIntents(GatewayIntent.GUILD_PRESENCES, GatewayIntent.GUILD_MESSAGE_REACTIONS);
    jdaBuilder.enableCache(
        CacheFlag.CLIENT_STATUS, CacheFlag.VOICE_STATE, CacheFlag.ACTIVITY, CacheFlag.ROLE_TAGS);
    jdaBuilder.setEnableShutdownHook(true);
    jdaBuilder.setStatus(OnlineStatus.IDLE);

    jdaBuilder.addEventListeners(
        new ReactionUpdateEvent(),
        new ServerJoinEvent(),
        Command.add(new PingCommand()),
        Command.add(new PrefixCommand()),
        Command.add(new ShutdownCommand()),
        Command.add(new RestartCommand()),
        Command.add(new StarboardCommand()),
        Command.add(new HelpCommand()),
        Command.add(new BottomCommand()),
        Command.add(new CatCommand()),
        Command.add(new StealCommand()));

    api = jdaBuilder.build();
    api.awaitReady();
    System.out.println(
        "\n"
            + "   ██████╗ ██████╗ ██████╗ ██████╗ ██╗   ██╗  \n"
            + "  ██╔════╝██╔═══██╗██╔══██╗██╔══██╗╚██╗ ██╔╝  \n"
            + "  ██║     ██║   ██║██████╔╝██████╔╝ ╚████╔╝   \n"
            + "  ██║     ██║   ██║██╔══██╗██╔══██╗  ╚██╔╝    \n"
            + "  ╚██████╗╚██████╔╝██║  ██║██████╔╝   ██║     \n"
            + "   ╚═════╝ ╚═════╝ ╚═╝  ╚═╝╚═════╝    ╚═╝       "
            + "\n");

    config.initOther(
        new Color(98, 79, 255),
        new Color(255, 0, 0),
        new Color(70, 255, 0),
        new Color(255, 215, 0),
        api.getSelfUser().getName(),
        api.getSelfUser().getEffectiveAvatarUrl(),
        api.getInviteUrl(permissions),
        api.getSelfUser().getId(),
        api.getSelfUser().getAsTag(),
        "⭐");

    logger = LoggerFactory.getLogger(config.botName);

    logger.info(
        "Bot has started up!\n    "
            + "~ PFP:         "
            + config.botPfpUrl
            + "\n    "
            + "~ Name:        "
            + config.nameAsTag
            + "\n    "
            + "~ ID:          "
            + config.id
            + "\n    "
            + "~ Invite URL:  "
            + config.inviteUrl
            + "\n    "
            + "~ Ping:        "
            + api.getGatewayPing()
            + "\n    ");
  }

  private static void startUpdatePresence() {
    schedulerPresence.scheduleWithFixedDelay(
        () -> api.getPresence().setActivity(Activity.watching(";help | " + getPresence())),
        0,
        7,
        TimeUnit.SECONDS);
  }

  private static String getPresence() {
    presences.clear();
    presences.add("Ping: " + api.getGatewayPing());
    presences.add(api.getGuilds().size() + " Servers!");
    return presences.get(random.nextInt(presences.size()));
  }

  public static void shutdown(int exitCode) {
    logger.warn("Terminating... Bye!");
    api.shutdown();
    schedulerPresence.shutdown();
    System.exit(exitCode);
  }

  public static void restart() throws IOException {
    logger.warn("Restarting...");
    Runtime.getRuntime().exec("zsh scripts/start.sh");
    shutdown(Config.ExitCodes.NORMAL_SHUTDOWN_EXIT_CODE);
  }

  public static JDA getApi() {
    return api;
  }
}
