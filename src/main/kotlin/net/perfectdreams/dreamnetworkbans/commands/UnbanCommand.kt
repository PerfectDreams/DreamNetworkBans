package net.perfectdreams.dreamnetworkbans.commands

import net.md_5.bungee.api.CommandSender
import net.perfectdreams.dreamcorebungee.utils.Databases
import net.perfectdreams.dreamcorebungee.utils.commands.AbstractCommand
import net.perfectdreams.dreamcorebungee.utils.commands.annotation.Subcommand
import net.perfectdreams.dreamcorebungee.utils.extensions.toTextComponent
import net.perfectdreams.dreamnetworkbans.DreamNetworkBans
import net.perfectdreams.dreamnetworkbans.PunishmentManager
import net.perfectdreams.dreamnetworkbans.dao.Ban
import net.perfectdreams.dreamnetworkbans.tables.Bans
import net.perfectdreams.libs.com.mongodb.client.model.Filters
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction
import java.lang.IllegalArgumentException
import java.util.*

class UnbanCommand(val m: DreamNetworkBans) : AbstractCommand("unban", permission = "dreamnetworkbans.unban") {
	@Subcommand
    fun unban(sender: CommandSender, playerName: String) {
		val punishedUniqueId = try { UUID.fromString(playerName) } catch (e: IllegalArgumentException) { PunishmentManager.getUniqueId(playerName) }

		transaction(Databases.databaseNetwork) {
			Ban.find { Bans.player eq punishedUniqueId }.firstOrNull()?.delete()
		}

		sender.sendMessage("§b$punishedUniqueId§a desbanido com sucesso!".toTextComponent())
	}
}