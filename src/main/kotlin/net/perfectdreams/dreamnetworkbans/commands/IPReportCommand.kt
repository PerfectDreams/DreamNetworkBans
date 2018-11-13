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
		sender.sendMessage("§cResumo dos players que estão online (${m.proxy.players.size}):".toTextComponent())
		sender.sendMessage(m.proxy.players.joinToString("\n") {
			val geoLocalization = transaction(Databases.databaseNetwork) {
				GeoLocalization.find { GeoLocalizations.player eq it.uniqueId }.firstOrNull()
			}
			
			"§c${it.name} (${it.address.hostString} - ${geoLocalization?.country ?: "???"}, ${geoLocalization?.region ?: "???"})"
		}.toTextComponent())
		
	}
}