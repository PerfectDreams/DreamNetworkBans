package net.perfectdreams.dreamnetworkbans.commands

import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.perfectdreams.commands.annotation.Subcommand
import net.perfectdreams.dreamcorebungee.commands.SparklyBungeeCommand
import net.perfectdreams.dreamcorebungee.utils.Databases
import net.perfectdreams.dreamcorebungee.utils.discord.DiscordMessage
import net.perfectdreams.dreamcorebungee.utils.extensions.toBaseComponent
import net.perfectdreams.dreamcorebungee.utils.extensions.toTextComponent
import net.perfectdreams.dreamnetworkbans.DreamNetworkBans
import net.perfectdreams.dreamnetworkbans.dao.DiscordAccount
import net.perfectdreams.dreamnetworkbans.tables.DiscordAccounts
import org.jetbrains.exposed.sql.transactions.transaction


class DiscordCommand(val m: DreamNetworkBans) : SparklyBungeeCommand(arrayOf("discord")) {
	@Subcommand
	fun root(sender: CommandSender) {
		sender.sendMessage("§dNosso Discord! https://discord.gg/JYN6g2s".toTextComponent())
	}
	
	@Subcommand(["registrar", "register"])
	fun register(sender: ProxiedPlayer) {
		val account = transaction(Databases.databaseServer) {
			DiscordAccount.find { DiscordAccounts.minecraftId eq sender.uniqueId }
					.firstOrNull()
		}

		if (account == null) {
			sender.sendMessage("§cVocê não tem nenhum registro pendente! Use \"-registrar ${sender.name}\" no nosso servidor no Discord para registrar a sua conta!".toTextComponent())
			return
		}

		transaction(Databases.databaseNetwork) {
			account.isConnected = true
		}

		sender.sendMessage("§aConta registrada com sucesso, yay!".toTextComponent())
	}
}