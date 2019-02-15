package net.perfectdreams.dreamnetworkbans.commands
	
import net.perfectdreams.dreamcorebungee.commands.SparklyBungeeCommand
import net.perfectdreams.dreamnetworkbans.DreamNetworkBans
import net.perfectdreams.commands.annotation.Subcommand
import net.md_5.bungee.api.CommandSender
import java.lang.IllegalArgumentException
import net.perfectdreams.dreamcorebungee.utils.extensions.toTextComponent
import java.util.UUID
import net.perfectdreams.dreamnetworkbans.PunishmentManager
import org.jetbrains.exposed.sql.transactions.transaction
import net.perfectdreams.dreamcorebungee.utils.Databases
import net.perfectdreams.dreamnetworkbans.dao.GeoLocalization
import net.perfectdreams.dreamnetworkbans.tables.GeoLocalizations
import net.perfectdreams.dreamcorebungee.dao.User
import net.perfectdreams.dreamnetworkbans.dao.Ban
import net.perfectdreams.dreamnetworkbans.tables.Bans
import org.jetbrains.exposed.sql.or

class DupeIpCommand(val m: DreamNetworkBans) : SparklyBungeeCommand(arrayOf("dupeip"), permission = "dreamnetworkbans.dupeip") {
	
	@Subcommand
	fun dupeIp(sender: CommandSender, playerName: String) {
		// Primeiramente vamos pegar o UUID para achar o IP
		val playerUniqueId = try { UUID.fromString(playerName) } catch (e: IllegalArgumentException) { PunishmentManager.getUniqueId(playerName) }
	
		// Vamos pegar o player
		val geoLoc = transaction(Databases.databaseNetwork) {
			GeoLocalization.find { (GeoLocalizations.player eq playerUniqueId) or (GeoLocalizations.ip eq playerName) }.lastOrNull()
		}

		// Caso achar...
		if (geoLoc != null) {
			sender.sendMessage("Escaneando ${geoLoc.ip}".toTextComponent())
			
			// Agora vamos achar todos os players que tem o mesmo IP
			val geolocalizations = transaction(Databases.databaseNetwork) {
				GeoLocalization.find { GeoLocalizations.ip eq geoLoc.ip }.toList()
			}

			val uids = geolocalizations.distinctBy { it.player }.map { it.player }
			val accounts = uids.joinToString(", ", transform = {
				// Está banido?
				val ban = transaction(Databases.databaseNetwork) {
					Ban.find { Bans.player eq it }.firstOrNull()
				}
				// Se ele estiver banido...
				if (ban != null) {
					val punishedName = transaction(Databases.databaseNetwork) { User.findById(ban.player) }

					return@joinToString "§c${punishedName?.username}"
				}

				// Está online?
				val isOnline = m.proxy.getPlayer(it)
				if (isOnline != null && isOnline.isConnected) {
					// Sim ele está online
					val onlineName = transaction(Databases.databaseNetwork) { User.findById(it) }

					return@joinToString "§a${onlineName?.username}"
				} else {
					// Ele não está online
					val offlineName = transaction(Databases.databaseNetwork) { User.findById(it) }

					return@joinToString "§7${offlineName?.username}"
				}
			})

			// Mandar o resultado final
			sender.sendMessage("[§cBanidos§f] [§aOnline§f] [§7Offline§f] \n${accounts}".toTextComponent())
		} else {
			sender.sendMessage("§cNão achei nenhum Player com esse nome!".toTextComponent())
		}
	}
}
