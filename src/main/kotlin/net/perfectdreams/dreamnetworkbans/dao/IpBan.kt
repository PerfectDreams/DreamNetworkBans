package net.perfectdreams.dreamnetworkbans.dao

import net.perfectdreams.dreamnetworkbans.tables.Bans
import net.perfectdreams.dreamnetworkbans.tables.IpBans
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass

class IpBan(id: EntityID<Long>) : LongEntity(id) {
	companion object : LongEntityClass<IpBan>(IpBans)

	var ip by IpBans.ip
	var player by IpBans.player

	var punishedBy by IpBans.punishedBy
	var punishedAt by IpBans.punishedAt
	var punisherName by IpBans.punisherName
	var reason by IpBans.reason
	var temporary by IpBans.temporary
	var expiresAt by IpBans.expiresAt
}