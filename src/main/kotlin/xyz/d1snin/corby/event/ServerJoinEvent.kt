package xyz.d1snin.corby.event

import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.guild.GuildJoinEvent
import xyz.d1snin.corby.Corby
import xyz.d1snin.corby.database.managers.PrefixManager
import xyz.d1snin.corby.model.EmbedType
import xyz.d1snin.corby.util.createEmbed
import xyz.d1snin.corby.util.sendDmSafe

object ServerJoinEvent : Listener<GuildJoinEvent>() {
    init {
        execute {
            val channels = guild.channels

            if (channels.isEmpty()) {
                return@execute
            }

            val channel = if (guild.systemChannel == null) {
                channels.last() as TextChannel
            } else {
                guild.systemChannel!!
            }

            if (guild.botRole == null
                || guild.botRole!!.hasPermission(Corby.permissions)
            ) {
                guild.owner?.let {
                    sendDmSafe(
                        it.user,
                        createEmbed(
                            "Hi, you invited me without required permissions, please invite me using [this link](${Corby.config.inviteUrl}), thanks.",
                            guild,
                            type = EmbedType.ERROR
                        )
                    )
                } ?: guild.leave().queue()
            } else {
                channel.sendMessage(
                    createEmbed(
                        "Thank you for inviting me to your server!" +
                                "\nI can help you with moderation and administration of your server and much more." +
                                "\nYou can find out the full list of commands by simply writing to any chat `${PrefixManager[guild]}help`.",
                        guild
                    )
                ).queue()
            }
        }
    }
}