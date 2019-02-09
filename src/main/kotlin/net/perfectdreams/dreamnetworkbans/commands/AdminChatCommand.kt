package net.perfectdreams.dreamnetworkbans.commands

import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.perfectdreams.commands.ArgumentType
import net.perfectdreams.commands.annotation.InjectArgument
import net.perfectdreams.commands.annotation.Subcommand
import net.perfectdreams.dreamcorebungee.commands.SparklyBungeeCommand
import net.perfectdreams.dreamcorebungee.network.DreamNetwork
import net.perfectdreams.dreamcorebungee.utils.Databases
import net.perfectdreams.dreamcorebungee.utils.extensions.toTextComponent
import net.perfectdreams.dreamnetworkbans.DreamNetworkBans
import net.perfectdreams.dreamnetworkbans.PunishmentManager
import net.perfectdreams.dreamnetworkbans.dao.Ban
import net.perfectdreams.dreamnetworkbans.dao.GeoLocalization
import net.perfectdreams.dreamnetworkbans.dao.IpBan
import net.perfectdreams.dreamnetworkbans.tables.GeoLocalizations
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import net.md_5.bungee.api.chat.HoverEvent
import com.sun.tools.javac.util.StringUtils
import net.perfectdreams.dreamcorebungee.utils.discord.DiscordMessage
import net.perfectdreams.dreamcorebungee.utils.extensions.toBaseComponent


class AdminChatCommand(val m: DreamNetworkBans) : SparklyBungeeCommand(arrayOf("adminchat", "a"), permission = "dreamnetworkbans.adminchat") {
	@Subcommand
	fun root(sender: CommandSender) {
		sender.sendMessage("§6/adminchat blah blah blah".toTextComponent())
	}
	
	@Subcommand
	fun adminChat(sender: CommandSender, args: Array<String>) {
		val message = args.joinToString(" ")

		val staff = m.proxy.players.filter { it.hasPermission("dreamnetworkbans.adminchat") }

		var senderName = sender.name
		val server = "???"
		var mensagem = "§eServidor: §6$server"

		if (sender is ProxiedPlayer) {
			senderName = sender.displayName
			mensagem = "§eServidor: §6" + sender.server.info.name
		}

		var color = "§7"
		if (sender.hasPermission("dreamnetworkbans.owner")) {
			color = "§a"
		} else if (sender.hasPermission("dreamnetworkbans.admin")) {
			color = "§4"
		} else if (sender.hasPermission("dreamnetworkbans.moderator")) {
			color = "§3"
		} else if (sender.hasPermission("dreamnetworkbans.builder")) {
			color = "§5"
		}
		val tc = "§3[$color§l${senderName}§3] §b$message".toTextComponent()
		tc.hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, mensagem.toBaseComponent())

		staff.forEach { it.sendMessage(tc) }

		if (staff.size == 1) {
			sender.sendMessage("§cHey... Não sei se você sabe... Mas você está falando sozinho!".toTextComponent())
		}

		m.adminChatWebhook.send(
				DiscordMessage(
						sender.name,
						message,
						"https://sparklypower.net/api/v1/render/avatar?name=${sender.name}&scale=16"
				)
		)
	}
}