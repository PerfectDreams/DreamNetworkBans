package net.perfectdreams.dreamnetworkbans.utils

import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.connection.ProxiedPlayer

// Retorna o "fancy name" de um CommandSender
// No caso, se não for um jogador (provavelmente é alguém pelo console), o nome retorna "Pantufa"
val CommandSender.fancyName: String get() {
	return if (this !is ProxiedPlayer)
		"Pantufa"
	else
		this.name
}