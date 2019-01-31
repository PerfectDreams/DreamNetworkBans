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
import net.perfectdreams.dreamcorebungee.tables.Users
import net.perfectdreams.dreamcorebungee.dao.User
class DupeIpCommand(val m: DreamNetworkBans) : SparklyBungeeCommand(arrayOf("dupeip"), permission = "dreamnetworkbans.ban") {
	
	@Subcommand
	fun dupeIp(sender: CommandSender, playerName: String) {

		// Primeiramente vamos pegar o UUID para achar o IP
		val playerUniqueId = try { UUID.fromString(playerName) } catch (e: IllegalArgumentException) { PunishmentManager.getUniqueId(playerName) }
	
		// Vamos pegar o player
		val playerIP = transaction(Databases.databaseNetwork) {
			GeoLocalization.find { GeoLocalizations.player eq playerUniqueId}.firstOrNull()
		}
		// Caso achar...
		if (playerIP != null) {
			// Agora vamos achar todos os players que tem o mesmo IP
			val geolocalizations = transaction(Databases.databaseNetwork) {
				GeoLocalization.find { GeoLocalizations.ip eq playerIP.ip }.toList()
			}
			// Contas e os nomes (depois vai juntar tudo)
			var accounts : String = ""
			var displayName: String = ""
			
			// Vamos pegar apenas os uids e fazer um forEach
			val uids = geolocalizations.distinctBy { it.player }.map { it.player }
			uids.forEach {
				//:tobias_hat:
				transaction(Databases.databaseNetwork) {
					val user = User.findById(it)
					if (user != null) {
						displayName = user.username
					}
				}
				accounts += "§8Usuarios com o mesmo ip: §l${displayName} "
			}
			// Mandar o resultado final
			sender.sendMessage(accounts.toTextComponent())
		} else {
			sender.sendMessage("§cNão achei nenhum Player com esse nome!".toTextComponent())
		}
	}
}