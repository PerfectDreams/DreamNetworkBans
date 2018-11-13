package net.perfectdreams.dreamnetworkbans.tables

import org.jetbrains.exposed.dao.LongIdTable

object GeoLocalizations : LongIdTable() {

	val player = uuid("user").index()
	val ip = text("ip")
	
	val country = text("country")
	val region = text("region")

}