package net.perfectdreams.dreamnetworkbans.dao

import net.perfectdreams.dreamnetworkbans.tables.Mutes
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass

class Mute(id: EntityID<Long>) : LongEntity(id) {
	companion object : LongEntityClass<Mute>(Mutes)
	
	var player by Mutes.player
	
	var punisher by Mutes.punisher
	var punishedAt by Mutes.punishedAt
	var punisherName by Mutes.punisherName
	
	var reason by Mutes.reason
	
	var temporary by Mutes.temporary
	var expiresAt by Mutes.expiresAt
}