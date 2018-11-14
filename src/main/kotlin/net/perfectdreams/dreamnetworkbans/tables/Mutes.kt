package net.perfectdreams.dreamnetworkbans.tables

import org.jetbrains.exposed.dao.LongIdTable

object Mutes : LongIdTable() {
	
	val player = uuid("player").index()
	
	val punisher = uuid("punisher").nullable()
	val punishedAt = long("punished_at")
	val punisherName = text("punisher_name")
	
	val reason = text("reason").nullable()
	
	val temporary = bool("temporary")
	val expiresAt = long("expires_at").nullable()
}