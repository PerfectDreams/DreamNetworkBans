package net.perfectdreams.dreamnetworkbans.commands

import net.md_5.bungee.api.CommandSender
import net.perfectdreams.dreamcorebungee.utils.Databases
import net.perfectdreams.dreamcorebungee.utils.commands.AbstractCommand
import net.perfectdreams.dreamcorebungee.utils.commands.annotation.Subcommand
import net.perfectdreams.dreamcorebungee.utils.extensions.toTextComponent
import net.perfectdreams.dreamnetworkbans.PunishmentManager
import net.perfectdreams.dreamnetworkbans.tables.Warns
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class UnmuteCommand : AbstractCommand("unmute", permission = "dreamnetworkbans.mute") {
	
	@Subcommand
	fun unmute(sender: CommandSender, playerName: String) {
		val punishedUniqueId = try { UUID.fromString(playerName) } catch (e: IllegalArgumentException) { PunishmentManager.getUniqueId(playerName) }
		
		transaction(Databases.databaseNetwork) {
			Warns.deleteWhere { Warns.player eq punishedUniqueId }
		}
		
		sender.sendMessage("§aJogador desmutado com sucesso!!!".toTextComponent())
	}
}