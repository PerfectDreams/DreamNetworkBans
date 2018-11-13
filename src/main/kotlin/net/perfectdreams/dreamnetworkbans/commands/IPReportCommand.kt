package net.perfectdreams.dreamnetworkbans.commands

import net.md_5.bungee.api.CommandSender
import net.perfectdreams.dreamcorebungee.utils.Databases
import net.perfectdreams.dreamcorebungee.utils.commands.AbstractCommand
import net.perfectdreams.dreamcorebungee.utils.commands.annotation.Subcommand
import net.perfectdreams.dreamcorebungee.utils.extensions.toTextComponent
import net.perfectdreams.dreamnetworkbans.DreamNetworkBans
import net.perfectdreams.dreamnetworkbans.dao.GeoLocalization
import net.perfectdreams.dreamnetworkbans.tables.GeoLocalizations
import org.jetbrains.exposed.sql.transactions.transaction

class IPReportCommand(val m: DreamNetworkBans) : AbstractCommand("ipreport", permission = "dreamnetworkbans.ipreport") {
	
	@Subcommand
	fun ipreport(sender: CommandSender) {
		var message = ""
		
		for (player in m.proxy.players) {
			val geoLocalization = transaction(Databases.databaseNetwork) {
				GeoLocalization.find { GeoLocalizations.player eq player.uniqueId }.firstOrNull()
			}
			
			message += "§c${player.name} => ${player.address.hostString} - (${geoLocalization?.country ?: "???"}, ${geoLocalization?.region ?: "???"})\n"
		}
		
		sender.sendMessage("§cResumo dos players que estão online (${m.proxy.players.size}):".toTextComponent())
		sender.sendMessage(message.toTextComponent())
	}
}