package net.perfectdreams.dreamnetworkbans.dao

import net.perfectdreams.dreamnetworkbans.tables.GeoLocalizations
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass

class GeoLocalization(id: EntityID<Long>) : LongEntity(id) {
	companion object : LongEntityClass<GeoLocalization>(GeoLocalizations)
	
	var player by GeoLocalizations.player
	var ip by GeoLocalizations.ip
	
	var region by GeoLocalizations.region
	var country by GeoLocalizations.country
}