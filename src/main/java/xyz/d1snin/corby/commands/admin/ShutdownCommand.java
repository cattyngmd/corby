package xyz.d1snin.corby.commands.admin;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import xyz.d1snin.corby.Corby;
import xyz.d1snin.corby.commands.Command;
import xyz.d1snin.corby.enums.Category;
import xyz.d1snin.corby.enums.EmbedTemplate;
import xyz.d1snin.corby.model.Config;
import xyz.d1snin.corby.utils.Embeds;

public class ShutdownCommand extends Command {

  public ShutdownCommand() {
    this.alias = "terminate";
    this.description = "Turns off the bot";
    this.category = Category.ADMIN;
    this.usages = new String[] {"%sterminate"};
  }

  @Override
  protected void execute(MessageReceivedEvent e, String[] args) {
    e.getTextChannel()
        .sendMessage(
            Embeds.create(
                EmbedTemplate.SUCCESS, e.getAuthor(), "Terminating... Bye!", e.getGuild(), null))
        .complete();
    Corby.shutdown(Config.ExitCodes.NORMAL_SHUTDOWN_EXIT_CODE);
  }

  @Override
  protected boolean isValidSyntax(MessageReceivedEvent e, String[] args) {
    return args.length <= 1;
  }
}
