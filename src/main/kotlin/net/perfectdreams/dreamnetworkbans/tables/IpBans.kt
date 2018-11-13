package net.perfectdreams.dreamnetworkbans.tables

import org.jetbrains.exposed.dao.LongIdTable

// Esta table é um long boi
// Ao inserir na tabela, o ID irá incrementar (1 -> 2 -> 3... etc)
object IpBans : LongIdTable() {
	// IP do player
	// Já que a gente vai acessar se o ban existe várias vezes, vamos indexar!
	val ip = text("ip").index()
	// Punido por...
	// Sim, pode ser nulo, caso seja nulo, iremos colocar quem puniu como "Pantufa"
	val punishedBy = uuid("punished_by").nullable()
	val punishedAt = Bans.long("punished_at")
	val punisherName = text("punisher_name")
	// Motivo da punição
	val reason = text("reason").nullable()
	val temporary = Bans.bool("temporary")
	val expiresAt = Bans.long("expires_at").nullable()
}