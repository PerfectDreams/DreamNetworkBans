package net.perfectdreams.dreamnetworkbans.commands

import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.perfectdreams.commands.ArgumentType
import net.perfectdreams.commands.annotation.InjectArgument
import net.perfectdreams.commands.annotation.Subcommand
import net.perfectdreams.dreamcorebungee.commands.SparklyBungeeCommand
import net.perfectdreams.dreamcorebungee.network.DreamNetwork
import net.perfectdreams.dreamcorebungee.utils.Databases
import net.perfectdreams.dreamcorebungee.utils.extensions.toTextComponent
import net.perfectdreams.dreamnetworkbans.DreamNetworkBans
import net.perfectdreams.dreamnetworkbans.PunishmentManager
import net.perfectdreams.dreamnetworkbans.dao.Ban
import net.perfectdreams.dreamnetworkbans.dao.GeoLocalization
import net.perfectdreams.dreamnetworkbans.dao.IpBan
import net.perfectdreams.dreamnetworkbans.dao.Warn
import net.perfectdreams.dreamnetworkbans.tables.GeoLocalizations
import net.perfectdreams.dreamnetworkbans.tables.Warns
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class WarnCommand(val m: DreamNetworkBans) : SparklyBungeeCommand(arrayOf("warn", "avisar"), permission = "dreamnetworkbans.warn") {

	@Subcommand
	fun warn(sender: CommandSender, playerName: String, @InjectArgument(ArgumentType.ALL_ARGUMENTS) reason: String?) {
		var punishedUniqueId: UUID? = null
		var punishedDisplayName: String? = null

		// O nosso querido "player name" pode ser várias coisas...
		// Talvez seja um player online!
		val player = m.proxy.getPlayer(playerName)
		if (player != null) {
			punishedDisplayName = player.displayName
			punishedUniqueId = player.uniqueId
		} else {
			// Talvez seja o UUID de um player online!
			punishedUniqueId = try {
				UUID.fromString(playerName)
			} catch (e: IllegalArgumentException) {
				null
			}
			if (punishedUniqueId != null) {
				val playerByUuid = m.proxy.getPlayer(punishedUniqueId)
				if (playerByUuid != null) {
					punishedUniqueId = playerByUuid.uniqueId
					punishedDisplayName = playerByUuid.name
				} else {
					// ...tá, mas talvez seja o UUID de um player offline!
					// Caso o UUID seja != null, quer dizer que ele É UM UUID VÁLIDO!!
					punishedDisplayName = punishedUniqueId.toString()
				}
			} else {
				// Se não, vamos processar como se fosse um player mesmo
				punishedDisplayName = playerName
				punishedUniqueId = PunishmentManager.getUniqueId(playerName)
			}
		}

		// ...blah
		if (punishedUniqueId == null && punishedDisplayName == null) {
			sender.sendMessage("§cEu sei que você tá correndo para avisar aquele mlk meliante... mas eu não conheço ninguém chamado §b$playerName§c... respira um pouco... fica calmo e VEJA O NOME NOVAMENTE!".toTextComponent())
			return
		}

		var effectiveReason = reason ?: "Sem motivo definido"

		var silent = false
		if (effectiveReason.endsWith("-s")) {
			effectiveReason = effectiveReason.substring(0, effectiveReason.length - "-s".length)
			
			silent = true
		}
		
		val punisherDisplayName = if (sender is ProxiedPlayer) {
			sender.name
		} else { "Pantufa" }

		transaction(Databases.databaseNetwork) {
			Warn.new {
				this.player = punishedUniqueId!!
				this.punishedBy = punishedUniqueId
				this.punishedAt = System.currentTimeMillis()
				this.reason = effectiveReason
			}
		}
		
		val warns = transaction(Databases.databaseNetwork) {
			Warn.find { Warns.player eq punishedUniqueId!! }.toList()
		}
		val count = warns.size
		
		val geoLocalization = transaction(Databases.databaseNetwork) {
			GeoLocalization.find { GeoLocalizations.player eq punishedUniqueId!! }.firstOrNull()
		}
		
		// IP do usuário, caso seja encontrado
		val ip = if (player != null)
			player.address.hostString
		else
			geoLocalization?.ip
		
		when (count) {
			2 -> {
				val player = m.proxy.getPlayer(punishedUniqueId!!)
				
				if (player != null) {
					player.disconnect("§cVocê está chegando ao limite de avisos, cuidado!\n§cTotal de avisos: §e$count".toTextComponent())
					
					m.proxy.broadcast("§c§l${sender.name}§c expulsou §l$playerName§c pelo motivo \"$reason\" no servidor ${(sender as? ProxiedPlayer)?.server?.info?.name ?: "Desconhecido"}".toTextComponent())
					DreamNetwork.PANTUFA.sendMessage(
							"378318041542426634",
							"**$playerName** foi expulso!\nFazer o que né, não soube ler as regras!\n\n**Expulso pelo:** ${sender.name}\n**Motivo:** $reason\n**Servidor:** ${(sender as? ProxiedPlayer)?.server?.info?.name ?: "Desconhecido"}"
					)
				}
			}
			
			3 -> {
				val player = m.proxy.getPlayer(punishedUniqueId!!)
				
				if (player != null) {
					player.disconnect("§cVocê está chegando ao limite de avisos, cuidado!\n§cTotal de avisos: §e$count".toTextComponent())
					
					m.proxy.broadcast("§c§l${sender.name}§c expulsou §l$playerName§c pelo motivo \"$reason\" no servidor ${(sender as? ProxiedPlayer)?.server?.info?.name ?: "Desconhecido"}".toTextComponent())
					DreamNetwork.PANTUFA.sendMessage(
							"378318041542426634",
							"**$playerName** foi expulso!\nFazer o que né, não soube ler as regras!\n\n**Expulso pelo:** ${sender.name}\n**Motivo:** $reason\n**Servidor:** ${(sender as? ProxiedPlayer)?.server?.info?.name ?: "Desconhecido"}"
					)
				}
			}
			
			4 -> {
				// Ban de 4 horas
				
				val expires = System.currentTimeMillis() + 14400000 // 4 horas
				transaction(Databases.databaseNetwork) {
					Ban.new {
						this.player = punishedUniqueId!!
						this.punisherName = punisherDisplayName
						this.punishedBy = punishedUniqueId
						this.punishedAt = System.currentTimeMillis()
						this.reason = effectiveReason
						
						this.temporary = true
						this.expiresAt = expires
					}
					
					if (ip != null) {
						IpBan.new {
							this.ip = ip
							this.punisherName = punisherDisplayName
							this.punishedBy = punishedUniqueId
							this.punishedAt = System.currentTimeMillis()
							this.reason = effectiveReason
							this.temporary = true
							this.expiresAt = expires
						}
					}
				}
				
				// TODO: Hard coded, remover depois
				player?.disconnect("""
					§cVocê foi temporariamente banido!
					§cMotivo:
					
					§a$effectiveReason
					§cPor: $punisherDisplayName
					§cExpira em: §a4 horas
				""".trimIndent().toTextComponent())
				
				m.proxy.broadcast("§c§l${punisherDisplayName}§c baniu temporariamente §l$playerName§c por \"$reason\" no servidor ${player?.server?.info?.name ?: "Desconhecido"} por 4 horas".toTextComponent())
				DreamNetwork.PANTUFA.sendMessage(
						"378318041542426634",
						"**$playerName** foi banido temporariamente!\nFazer o que né, não soube ler as regras!\n\n**Banido pelo:** ${punisherDisplayName}\n**Motivo:** $reason\n**Servidor:** ${player?.server?.info?.name ?: "Desconhecido"}\nDuração: 4 horas"
				)
			}
			
			5 -> {
				// Ban de 12 horas
				
				val expires = System.currentTimeMillis() + 43200000 // 12 horas
				transaction(Databases.databaseNetwork) {
					Ban.new {
						this.player = punishedUniqueId!!
						this.punisherName = punisherDisplayName
						this.punishedBy = punishedUniqueId
						this.punishedAt = System.currentTimeMillis()
						this.reason = effectiveReason
						
						this.temporary = true
						this.expiresAt = expires
					}
					
					if (ip != null) {
						IpBan.new {
							this.ip = ip
							this.punisherName = punisherDisplayName
							this.punishedBy = punishedUniqueId
							this.punishedAt = System.currentTimeMillis()
							this.reason = effectiveReason
							this.temporary = true
							this.expiresAt = expires
						}
					}
				}
				
				// TODO: Hard coded, remover depois
				player?.disconnect("""
					§cVocê foi temporariamente banido!
					§cMotivo:
					
					§a$effectiveReason
					§cPor: $punisherDisplayName
					§cExpira em: §a12 horas
				""".trimIndent().toTextComponent())
				
				
				m.proxy.broadcast("§c§l${punisherDisplayName}§c baniu temporariamente §l$playerName§c por \"$reason\" no servidor ${player?.server?.info?.name ?: "Desconhecido"} por 12 horas".toTextComponent())
				DreamNetwork.PANTUFA.sendMessage(
						"378318041542426634",
						"**$playerName** foi banido temporariamente!\nFazer o que né, não soube ler as regras!\n\n**Banido pelo:** ${punisherDisplayName}\n**Motivo:** $reason\n**Servidor:** ${player?.server?.info?.name ?: "Desconhecido"}\nDuração: 12 horas"
				)
			}
			
			6 -> {
				// Ban de 1 dia
				
				val expires = System.currentTimeMillis() + 86400000 // 24 horas
				transaction(Databases.databaseNetwork) {
					Ban.new {
						this.player = punishedUniqueId!!
						this.punisherName = punisherDisplayName
						this.punishedBy = punishedUniqueId
						this.punishedAt = System.currentTimeMillis()
						this.reason = effectiveReason
						
						this.temporary = true
						this.expiresAt = expires
					}
					
					if (ip != null) {
						IpBan.new {
							this.ip = ip
							this.punisherName = punisherDisplayName
							this.punishedBy = punishedUniqueId
							this.punishedAt = System.currentTimeMillis()
							this.reason = effectiveReason
							this.temporary = true
							this.expiresAt = expires
						}
					}
				}
				
				// TODO: Hard coded, remover depois
				player?.disconnect("""
					§cVocê foi temporariamente banido!
					§cMotivo:
					
					§a$effectiveReason
					§cPor: $punisherDisplayName
					§cExpira em: §a1 dia
				""".trimIndent().toTextComponent())
				
				m.proxy.broadcast("§c§l${punisherDisplayName}§c baniu temporariamente §l$playerName§c por \"$reason\" no servidor ${player?.server?.info?.name ?: "Desconhecido"} por 1 dia".toTextComponent())
				DreamNetwork.PANTUFA.sendMessage(
						"378318041542426634",
						"**$playerName** foi banido temporariamente!\nFazer o que né, não soube ler as regras!\n\n**Banido pelo:** ${punisherDisplayName}\n**Motivo:** $reason\n**Servidor:** ${player?.server?.info?.name ?: "Desconhecido"}\nDuração: 1 dia"
				)
			}
			
			7 -> {
				// Ban de 3 dias
				val expires = System.currentTimeMillis() + 259200000 // 72 horas
				transaction(Databases.databaseNetwork) {
					Ban.new {
						this.player = punishedUniqueId!!
						this.punisherName = punisherDisplayName
						this.punishedBy = punishedUniqueId
						this.punishedAt = System.currentTimeMillis()
						this.reason = effectiveReason
						
						this.temporary = true
						this.expiresAt = expires
					}
					
					if (ip != null) {
						IpBan.new {
							this.ip = ip
							this.punisherName = punisherDisplayName
							this.punishedBy = punishedUniqueId
							this.punishedAt = System.currentTimeMillis()
							this.reason = effectiveReason
							this.temporary = true
							this.expiresAt = expires
						}
					}
				}
				
				// TODO: Hard coded, remover depois
				player?.disconnect("""
					§cVocê foi temporariamente banido!
					§cMotivo:
					
					§a$effectiveReason
					§cPor: $punisherDisplayName
					§cExpira em: §a3 dias
				""".trimIndent().toTextComponent())
				
				m.proxy.broadcast("§c§l${punisherDisplayName}§c baniu temporariamente §l$playerName§c por \"$reason\" no servidor ${player?.server?.info?.name ?: "Desconhecido"} por 4 horas".toTextComponent())
				DreamNetwork.PANTUFA.sendMessage(
						"378318041542426634",
						"**$playerName** foi banido temporariamente!\nFazer o que né, não soube ler as regras!\n\n**Banido pelo:** ${punisherDisplayName}\n**Motivo:** $reason\n**Servidor:** ${player?.server?.info?.name ?: "Desconhecido"}\nDuração: 3 dias"
				)
			}
			
			8 -> {
				// Ban permanente
				
				transaction(Databases.databaseNetwork) {
					Ban.new {
						this.player = punishedUniqueId!!
						this.punisherName = punisherDisplayName
						this.punishedBy = punishedUniqueId
						this.punishedAt = System.currentTimeMillis()
						this.reason = effectiveReason
						
						this.temporary = false
					}
					
					if (ip != null) {
						IpBan.new {
							this.ip = ip
							this.punisherName = punisherDisplayName
							this.punishedBy = punishedUniqueId
							this.punishedAt = System.currentTimeMillis()
							this.reason = effectiveReason
							this.temporary = true
							this.expiresAt = PunishmentManager.DEFAULT_IPBAN_EXPIRATION
						}
					}
				}
				
				player?.disconnect("""
					§cVocê foi banido!
					§cMotivo:
					
					§a$effectiveReason
					§cPor: $punisherDisplayName
				""".trimIndent().toTextComponent())
				
				m.proxy.broadcast("§c§l${punisherDisplayName}§c baniu §l$playerName§c por \"$reason\" no servidor ${player?.server?.info?.name ?: "Desconhecido"}".toTextComponent())
				DreamNetwork.PANTUFA.sendMessage(
						"378318041542426634",
						"**$playerName** foi banido permanentemente!\nFazer o que né, não soube ler as regras!\n\n**Banido pelo:** ${punisherDisplayName}\n**Motivo:** $reason\n**Servidor:** ${player?.server?.info?.name ?: "Desconhecido"}"
				)
			}
		}

		sender.sendMessage("§b${punishedDisplayName}§a foi punido com sucesso, yay!! ^-^".toTextComponent())
		if (silent) {
			DreamNetwork.PANTUFA.sendMessage(
					"506859824034611212",
					"**$playerName** foi avisado!\nFazer o que né, não soube ler as regras!\n\n**Avisado pelo:** ${punisherDisplayName}\n**Motivo:** $effectiveReason\n**Servidor:** ${player?.server?.info?.name ?: "Desconhecido"}"
			)
		} else {
			m.proxy.broadcast("§b${punisherDisplayName}§a deu um aviso em §c${punishedDisplayName}§a por §6\"§e${effectiveReason}§6\"§a!".toTextComponent())
			DreamNetwork.PANTUFA.sendMessage(
					"378318041542426634",
					"**$playerName** foi avisado!\nFazer o que né, não soube ler as regras!\n\n**Avisado pelo:** ${punisherDisplayName}\n**Motivo:** $effectiveReason\n**Servidor:** ${player?.server?.info?.name ?: "Desconhecido"}"
			)
		}
	}
}