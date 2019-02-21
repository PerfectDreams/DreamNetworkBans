package net.perfectdreams.dreamnetworkbans

import java.util.*

object PunishmentManager {
	// 14 dias
	const val WARN_EXPIRATION = 1_209_600_000
	
	// 90 dias
	const val DEFAULT_IPBAN_EXPIRATION = 466_560_000_000
	
	fun getUniqueId(playerName: String): UUID {
		// UUIDs podem ser diferentes... mas já que a gente é um offline mode boi, complicar pra quê?
		return UUID.nameUUIDFromBytes("OfflinePlayer:$playerName".toByteArray(Charsets.UTF_8))
	}
}