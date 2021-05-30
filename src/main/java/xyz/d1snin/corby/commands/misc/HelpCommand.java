/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2021, Corby
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package xyz.d1snin.corby.commands.misc;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import xyz.d1snin.corby.Corby;
import xyz.d1snin.corby.commands.Command;
import xyz.d1snin.corby.database.managers.MongoPrefixManager;
import xyz.d1snin.corby.enums.Category;
import xyz.d1snin.corby.enums.EmbedTemplate;
import xyz.d1snin.corby.event.ReactionUpdateEvent;
import xyz.d1snin.corby.utils.Embeds;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class HelpCommand extends Command {

  private Emote next;
  private Emote back;
  private boolean extEmojisAllowed;

  public HelpCommand() {
    this.alias = "help";
    this.description = "Gives you information about commands.";
    this.category = Category.MISC;
    this.usages = new String[] {"alias", "<Command Name>"};
  }

  @Override
  protected void execute(MessageReceivedEvent e, String[] args) {
    back = Corby.getShards().getEmoteById(Corby.getConfig().getEmoteBack());
    next = Corby.getShards().getEmoteById(Corby.getConfig().getEmoteNext());

    extEmojisAllowed =
        Objects.requireNonNull(e.getGuild().getBotRole())
            .getPermissions()
            .contains(Permission.MESSAGE_EXT_EMOJI);

    final String couldNotFindMessage = "Could not find this command: `%s`";

    if (args.length < 2) {
      AtomicInteger page = new AtomicInteger(1);
      e.getTextChannel()
          .sendMessage(Objects.requireNonNull(getEmbedByPage(page.get(), e)))
          .queue(
              message -> {
                addReactionsSafe(message, e, back, next);

                ReactionUpdateEvent.registerReaction(
                    message.getId(),
                    extEmojisAllowed ? back.getId() : Corby.getConfig().getEmoteDefaultBack(),
                    getRun(page, e, message, back));

                ReactionUpdateEvent.registerReaction(
                    message.getId(),
                    extEmojisAllowed ? next.getId() : Corby.getConfig().getEmoteDefaultNext(),
                    getRun(page, e, message, next));
              });
      return;
    }

    Command command = getCommandByAlias(args[1]);

    if (command == null) {
      e.getTextChannel()
          .sendMessage(
              Embeds.create(
                  EmbedTemplate.ERROR,
                  e.getAuthor(),
                  String.format(couldNotFindMessage, args[1]),
                  e.getGuild()))
          .queue();
      return;
    }

    String msg =
        "**Category:**"
            + "\n"
            + command.getCategory().getName()
            + "\n\n"
            + "**Description**"
            + "\n"
            + command.getDescription()
            + (command.getLongDescription() == null ? "" : "\n\n" + command.getLongDescription())
            + "\n\n"
            + "**Usage:**"
            + "\n"
            + command.getUsagesString();
    e.getTextChannel()
        .sendMessage(
            Embeds.create(EmbedTemplate.DEFAULT, e.getAuthor(), msg, e.getGuild(), null, null))
        .queue();
  }

  @Override
  protected boolean isValidSyntax(MessageReceivedEvent e, String[] args) {
    return args.length <= 2;
  }

  private MessageEmbed getEmbedByPage(int page, MessageReceivedEvent e) {

    int categories = Category.values().length;

    if (page > categories || page < 1) {
      return null;
    }

    Category category = null;
    for (Category c : Category.values()) {
      if ((c.ordinal() + 1) == page) {
        category = c;
      }
    }

    StringBuilder sb = new StringBuilder();

    String prefix = MongoPrefixManager.getPrefix(e.getGuild()).getPrefix();

    for (Command c : getCommandsByCategory(category)) {
      sb.append("`")
          .append(prefix)
          .append(c.getAlias())
          .append("`")
          .append(" - *")
          .append(c.getDescription())
          .append("*\n");
    }

    assert category != null;
    return Embeds.create(
        EmbedTemplate.DEFAULT,
        e.getAuthor(),
        "**" + category.getName() + " Commands. Page " + page + "/" + categories + ".**\n\n" + sb,
        e.getGuild());
  }

  private Runnable getRun(
      AtomicInteger page, MessageReceivedEvent e, Message message, Emote emote) {
    return () -> {
      if (getEmbedByPage(emote == next ? page.get() + 1 : page.get() - 1, e) == null) {
        if (extEmojisAllowed) {
          message.removeReaction(emote, e.getAuthor()).queue();
        } else {
          message
              .removeReaction(
                  emote == next
                      ? Corby.getConfig().getEmoteDefaultNext()
                      : Corby.getConfig().getEmoteDefaultBack(),
                  e.getAuthor())
              .queue();
        }
        return;
      }
      message
          .editMessage(
              Objects.requireNonNull(getEmbedByPage(page.addAndGet(emote == next ? +1 : -1), e)))
          .queue();
      if (extEmojisAllowed) {
        message.removeReaction(emote, e.getAuthor()).queue();
      } else {
        message
            .removeReaction(
                emote == next
                    ? Corby.getConfig().getEmoteDefaultNext()
                    : Corby.getConfig().getEmoteDefaultBack(),
                e.getAuthor())
            .queue();
      }
    };
  }

  private void addReactionsSafe(Message message, MessageReceivedEvent e, Emote... emotes) {
    if (Objects.requireNonNull(e.getGuild().getBotRole())
        .getPermissions()
        .contains(Permission.MESSAGE_EXT_EMOJI)) {
      for (Emote emote : emotes) {
        message.addReaction(emote).queue();
      }
    } else {
      for (Emote emote : emotes) {
        message
            .addReaction(
                emote == next
                    ? Corby.getConfig().getEmoteDefaultNext()
                    : Corby.getConfig().getEmoteDefaultBack())
            .queue();
      }
    }
  }
}
