/*                          GNU GENERAL PUBLIC LICENSE
 *                            Version 3, 29 June 2007
 *
 *        Copyright (C) 2007 Free Software Foundation, Inc. <https://fsf.org/>
 *            Everyone is permitted to copy and distribute verbatim copies
 *             of this license document, but changing it is not allowed.
 */

package xyz.d1snin.corby.utils;

import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;

import java.util.concurrent.TimeUnit;

public class OtherUtils {
  public static void sendPrivateMessageSafe(User user, MessageEmbed success, Runnable onFailure) {
    user.openPrivateChannel()
        .complete()
        .sendMessage(success)
        .queue(
            response -> {
              /* ok */
            },
            fail -> onFailure.run());
  }

  public static boolean isNumeric(String s) {
    try {
      Integer.parseInt(s);
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  public static String formatMillis(long ms) {
    return String.format(
        "%02dh %02dm %02ds",
        TimeUnit.MILLISECONDS.toHours(ms),
        TimeUnit.MILLISECONDS.toMinutes(ms) % TimeUnit.HOURS.toMinutes(1),
        TimeUnit.MILLISECONDS.toSeconds(ms) % TimeUnit.MINUTES.toSeconds(1));
  }

  public static String formatMessageKeyText(String key, String text) {
    return String.format("**%s:** *%s*", key, text);
  }

  public static boolean isImage(String url) {
    String lowered = url.toLowerCase();
    return (lowered.endsWith(".jpg") || lowered.endsWith(".png") || lowered.endsWith(".jpeg"));
  }
}
