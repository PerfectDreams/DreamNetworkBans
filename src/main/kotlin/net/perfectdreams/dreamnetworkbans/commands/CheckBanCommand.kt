package net.perfectdreams.dreamnetworkbans.commands

import net.md_5.bungee.api.CommandSender
import net.perfectdreams.commands.annotation.Subcommand
import net.perfectdreams.dreamcorebungee.commands.SparklyBungeeCommand
import net.perfectdreams.dreamcorebungee.utils.Databases
import net.perfectdreams.dreamcorebungee.utils.extensions.toTextComponent
import net.perfectdreams.dreamnetworkbans.DreamNetworkBans
import net.perfectdreams.dreamnetworkbans.PunishmentManager
import net.perfectdreams.dreamnetworkbans.dao.Ban
import net.perfectdreams.dreamnetworkbans.dao.Warn
import net.perfectdreams.dreamnetworkbans.tables.Bans
import net.perfectdreams.dreamnetworkbans.tables.Warns
import net.perfectdreams.dreamnetworkbans.utils.prettyBoolean
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class CheckBanCommand(val m: DreamNetworkBans) : SparklyBungeeCommand(arrayOf("checkban"), permission = "dreamnetworkbans.checkban") {

	@Subcommand
	fun checkBan(sender: CommandSender, playerName: String) {
		val punishedUniqueId = try { UUID.fromString(playerName) } catch (e: IllegalArgumentException) { PunishmentManager.getUniqueId(playerName) }

		sender.sendMessage("§eSobre §b$playerName§e...".toTextComponent())

		transaction(Databases.databaseNetwork) {
			val ban = Ban.find { Bans.player eq punishedUniqueId }.firstOrNull()

			// Estamos fazendo isto dentro de uma transaction!!
			// É bom? Não... mas fazer o que né
			sender.sendMessage("§eBanido? ${(ban != null).prettyBoolean()}".toTextComponent())
			if (ban != null) {
				sender.sendMessage("§eMotivo do Ban: ${ban.reason}".toTextComponent())
				sender.sendMessage("§eQuem baniu? §b${PunishmentManager.getPunisherName(ban.punishedBy)}".toTextComponent())
				sender.sendMessage("§eTemporário? §b${(ban.temporary).prettyBoolean()}".toTextComponent())
			}

			val warns = Warn.find { Warns.player eq punishedUniqueId }.toMutableList()
			val validWarns = warns.filter { System.currentTimeMillis() <= PunishmentManager.WARN_EXPIRATION + it.punishedAt }.sortedBy { it.punishedAt }
			val invalidWarns = warns.filter { PunishmentManager.WARN_EXPIRATION + it.punishedAt <= System.currentTimeMillis() }.sortedBy { it.punishedAt }
			sender.sendMessage("§eNúmero de avisos (${validWarns.size} avisos válidos):".toTextComponent())
			for (invalidWarn in invalidWarns) {
				sender.sendMessage("§7${invalidWarn.reason} por ${PunishmentManager.getPunisherName(invalidWarn.punishedBy)}".toTextComponent())
			}
			for (validWarn in validWarns) {
				sender.sendMessage("§a${validWarn.reason} por ${PunishmentManager.getPunisherName(validWarn.punishedBy)}".toTextComponent())
			}
		}
	}
}