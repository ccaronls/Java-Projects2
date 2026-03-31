package cc.lib.net

import cc.lib.utils.CommandLineParser
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Created by Chris Caron on 3/30/26.
 */
class DNSTest {

	companion object {
		@JvmStatic
		fun main(args: Array<String>) {
			object : CommandLineParser(args) {
				override fun onZeroArgs() {
					printUsage()
				}

				override fun printUsage() {
					println("USAGE: DNSTest svr|client")
				}

				override fun onArg(arg: String) {
					when (arg) {
						"svr" -> startSvr()
						"client" -> startClient()
						else -> printUsage()
					}
				}
			}
		}

		fun startClient() {
			runBlocking {
				val client = NetTest.TestNetClient()
				client.startDiscovery()

				launch {
					client.discoveredHosts.onEach {
						println("HOSTS:\n${it.values.joinToString("\n")}")
					}.collect()
				}
			}
		}

		fun startSvr() {
			runBlocking {
				val stopPressed = CompletableDeferred<Int>()
				launch {
					val server = NetTest.TestNetServer()
					server.startDiscovery("DNSTest")
					server.listen()
					stopPressed.await()
					server.stop()
				}

				launch {
					BufferedReader(InputStreamReader(System.`in`)).use {
						while (true) {
							println("type 'stop' to terminate")
							if (it.readLine() == "stop") {
								stopPressed.complete(0)
								break
							}
						}
					}
				}
			}
		}
	}

}