package net.perfectdreams.dreamnetworkbans.commands

import net.md_5.bungee.api.CommandSender
import net.perfectdreams.commands.annotation.Subcommand
import net.perfectdreams.dreamcorebungee.commands.SparklyBungeeCommand
import net.perfectdreams.dreamcorebungee.utils.Databases
import net.perfectdreams.dreamcorebungee.utils.extensions.toTextComponent
import net.perfectdreams.dreamnetworkbans.DreamNetworkBans
import net.perfectdreams.dreamnetworkbans.PunishmentManager
import net.perfectdreams.dreamnetworkbans.dao.GeoLocalization
import net.perfectdreams.dreamnetworkbans.tables.Bans
import net.perfectdreams.dreamnetworkbans.tables.GeoLocalizations
import net.perfectdreams.dreamnetworkbans.tables.IpBans
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class UnbanCommand(val m: DreamNetworkBans) : SparklyBungeeCommand(arrayOf("unban"), permission = "dreamnetworkbans.unban") {

	@Subcommand
    fun unban(sender: CommandSender, playerName: String) {
		val punishedUniqueId = try { UUID.fromString(playerName) } catch (e: IllegalArgumentException) { PunishmentManager.getUniqueId(playerName) }
		
		val geoLocalization = transaction(Databases.databaseNetwork) {
			GeoLocalization.find { GeoLocalizations.player eq punishedUniqueId!! }.firstOrNull()
		}
		
		val ip = geoLocalization?.ip
		
		transaction(Databases.databaseNetwork) {
			Bans.deleteWhere { Bans.player eq punishedUniqueId }
			
			if (ip != null)
				IpBans.deleteWhere { IpBans.ip eq ip }
		}

		sender.sendMessage("§b$punishedUniqueId§a desbanido com sucesso!".toTextComponent())
	}
}