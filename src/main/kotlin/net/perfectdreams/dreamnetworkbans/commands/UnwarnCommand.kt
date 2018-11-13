package net.perfectdreams.dreamnetworkbans.commands

import net.md_5.bungee.api.CommandSender
import net.perfectdreams.dreamcorebungee.utils.Databases
import net.perfectdreams.dreamcorebungee.utils.commands.AbstractCommand
import net.perfectdreams.dreamcorebungee.utils.commands.annotation.Subcommand
import net.perfectdreams.dreamcorebungee.utils.extensions.toTextComponent
import net.perfectdreams.dreamnetworkbans.PunishmentManager
import net.perfectdreams.dreamnetworkbans.dao.Warn
import net.perfectdreams.dreamnetworkbans.tables.Warns
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class UnwarnCommand : AbstractCommand("unwarn", permission = "dreamnetworkbans.unwarn") {
	
	@Subcommand
	fun root(sender: CommandSender) {
		sender.sendMessage("§cUse /unwarn usuário".toTextComponent())
	}
	
	@Subcommand
	fun unwarn(sender: CommandSender, playerName: String) {
		val punishedUniqueId = try { UUID.fromString(playerName) } catch (e: IllegalArgumentException) { PunishmentManager.getUniqueId(playerName) }
		
		val warn = transaction(Databases.databaseNetwork) {
			Warn.find { Warns.player eq punishedUniqueId }.firstOrNull()
		}
		
		if (warn == null) {
			sender.sendMessage("§cEste jogador não tem nenhum aviso válido!".toTextComponent())
			return
		}
		
		warn.delete()
		sender.sendMessage("§aAviso removido com sucesso!!!".toTextComponent())
	}
}