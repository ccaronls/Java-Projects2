package cc.lib.net

data class NetConnectionStatus(val rttMs: Int = -1, val packetLossPct: Float = 0f, val jitterMs: Int = 0) {
	val quality: NetConnectQuality
		get() = NetConnectQuality.from(rttMs)
}
