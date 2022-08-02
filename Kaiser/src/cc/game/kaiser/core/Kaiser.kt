package cc.game.kaiser.core

import cc.game.kaiser.ai.PlayerBot
import cc.lib.game.Utils
import cc.lib.utils.Reflector
import java.util.*

/**
 * NO external dependencies in the CORE!
 * @author ccaron
 */
class Kaiser : Reflector<Kaiser>() {
	companion object {
		@JvmField
        var DEBUG_ENABLED = false

		// Rules based on this page.
		// http://www.pagat.com/pointtrk/kaiser.html
		const val numDeckCards = 32
		const val numPlayers = 4
		const val numTeams = 2

		// tunables
		var winningPoints = 52
		var POINTS_5_HEARTS = 5
		var POINTS_3_CLUBS = -3
		fun computeBidOptions(currentBid: Bid?, isDealer: Boolean, hand: List<Card>): Array<Bid> {
			if (DEBUG_ENABLED) println("currentBid= " + currentBid + ", isDealer=" + isDealer + ", hand=" + hand.joinToString())
			val options: MutableList<Bid> = ArrayList()
			if (!isDealer) options.add(NO_BID)
			val suits = getBestTrumpOptions(hand)
			var start = Rules.MINIMUM_BID
			if (currentBid != null) {
				start = currentBid.numTricks
				if (!isDealer) {
					if (currentBid.trump !== Suit.NOTRUMP) {
						options.add(Bid(start, Suit.NOTRUMP))
					}
					start++
				} else if (currentBid.trump === Suit.NOTRUMP) {
					options.add(Bid(start, Suit.NOTRUMP))
					start++
				}
			}
			for (i in start..12) {
				for (s in suits) {
					options.add(Bid(i, s))
				}
			}
			return options.toTypedArray()
		}

		fun getBestTrumpOptions(hand: List<Card>): Array<Suit> {
			// sort the suits based on their relevance in the set of cards.  This is to support bidding
			val arr = Array(4) {
				val suit = Suit.values()[it]
				Pair(hand.count { it.suit == suit }, suit)
			}
			arr.sortBy { it.first }
			return arr.map { it.second }.toTypedArray()
		}

		// package access for unit testing
		fun getTrickWinnerIndex(trump: Suit, cards: Array<Card>, leadIndex: Int): Int {
			var trickWinner = leadIndex
			var lead = cards[leadIndex]
			for (i in 1 until cards.size) {
				val ii = (i + leadIndex) % cards.size
				val test = cards[ii] ?: return -1
				if (trump === Suit.NOTRUMP) {
					if (test.suit !== lead.suit) {
						// lead wins
						continue
					} else {
						// normal compare
					}
				} else {
					if (lead.suit === trump && test.suit !== trump) {
						// lead wins
						continue
					} else if (lead.suit !== trump && test.suit === trump) {
						// test wins
						lead = test
						trickWinner = ii
						continue
					} else {
						// normal compare
					}
				}

				//assert(test.rank != lead.rank);
				//assert(test.rank != lead.rank, "Expected ranks to be different");
				if (test.rank.ordinal > lead.rank.ordinal) {
					lead = test
					trickWinner = ii
				}
			}
			return trickWinner
		}

		init {
			addAllFields(Kaiser::class.java)
		}
	}
	/**
	 * Return set of all cards
	 *
	 * @return
	 */
	/**
	 * Handle to affect how loadGame instantiates a player from a player description.
	 * Default behavior is to treat description as a classname.
	 *
	 * Class in question must have constructor with signature:
	 * classname(String name)
	 *
	 * @param clazz
	 * @param name
	 * @return
	 * @throws Exception
	 *
	 * protected Player instantiatePlayer(String clazz, String name) throws Exception {
	 * return (Player) getClass().getClassLoader().loadClass(clazz).getConstructor(new Class []{ String.class }).newInstance(name);
	 * }
	 *
	 * / **
	 * Handle to affect how saveGame describes a player.  Default behavior is to use the players classname.
	 * @param player
	 * @return
	 *
	 * protected String describePlayer(Player player) {
	 * return player.getClass().getName();
	 * }
	 *
	 * / **
	 *
	 * @return
	 */
	val deck  = ArrayList<Card>()

	var curPlayer // the current player whose turn it is
		= 0
		private set

	/**
	 *
	 * @return
	 */
	var startPlayer // the first player to play in this round
		= 0
		private set

	/**
	 *
	 * @return
	 */
	var dealer // the player who is the dealer this round
		= 0
		private set

	/**
	 *
	 * @return
	 */
	var state // current game state
		: State? = null
		private set
	private var numCards // number of cards in the deck
		= 0
	private var curBidTeam // current team that is bidding
		= 0
	private var numTricks // number of tricks played this round
		= 0

	/**
	 *
	 * @return
	 */
	var numRounds // number of rounds played
		= 0
		private set
	private val players = Array<Player>(numPlayers) { PlayerBot() }
	private val teams = Array(numTeams) { Team() }
	private val trick = Array(numPlayers) { Card() }


	private fun initCards() {
		deck.clear()
		arrayOf(Suit.HEARTS, Suit.CLUBS, Suit.DIAMONDS, Suit.SPADES).forEach { s ->
			deck.add(Card(Rank.ACE, s))
			deck.add(Card(Rank.KING, s))
			deck.add(Card(Rank.QUEEN, s))
			deck.add(Card(Rank.JACK, s))
			deck.add(Card(Rank.TEN, s))
			deck.add(Card(Rank.NINE, s))
			deck.add(Card(Rank.EIGHT, s))
			when (s) {
				Suit.CLUBS -> deck.add(Card(Rank.THREE, s))
				Suit.HEARTS -> deck.add(Card(Rank.FIVE, s))
				else        -> deck.add(Card(Rank.SEVEN, s))
			}
		}
	}

	private fun computeBidOptions(dealer: Boolean, hand: Hand): Array<Bid> {
		var bid: Bid? = getCurBidTeam()?.bid
		return computeBidOptions(bid, dealer, hand.mCards)
	}

	private fun computePlayerTrickOptions(p: Player): Array<Card> {
		var hasLead = false
		if (curPlayer != startPlayer) {
			val lead = trick[startPlayer].suit
			for (i in 0 until p.numCards) {
				if (p.getCard(i).suit === lead) {
					hasLead = true
					break
				}
			}
		}
		val list = ArrayList<Card>()
		for (i in 0 until p.numCards) {
			if (curPlayer == startPlayer || !hasLead) {
				//options[num++] = i;
				list.add(p.getCard(i))
				continue
			}
			val lead = trick[startPlayer].suit
			if (p.getCard(i).suit === lead) {
				//options[num++] = i;
				list.add(p.getCard(i))
			}
		}
		return list.toTypedArray()
	}

	private fun processTrick() {
		if (trick[startPlayer] == null) {
			state = State.RESET_TRICK
			return
		}
		val best = trickWinnerIndex
		val winner = players[best]
		numTricks += 1
		message("%s wins the trick %d of %d", winner.name, numTricks, Player.MAX_PLAYER_TRICKS)
		val pTrick = Hand()
		pTrick.clear()
		pTrick.add(trick[0])
		pTrick.add(trick[1])
		pTrick.add(trick[2])
		pTrick.add(trick[3])
		winner.tricks.add(pTrick)
		winner.onWinsTrick(this, pTrick)
		var points = 1
		for (i in 0 until numPlayers) {
			if (trick[i].rank === Rank.THREE) {
				message("Team " + getTeam(winner.team).name + " loses " + Math.abs(POINTS_3_CLUBS) + " points for the 3 of clubes")
				points += POINTS_3_CLUBS
			} else if (trick[i].rank === Rank.FIVE) {
				message("Team " + getTeam(winner.team).name + " gains " + Math.abs(POINTS_5_HEARTS) + " points for the 5 of hearts")
				points += POINTS_5_HEARTS
			}
		}
		startPlayer = best
		val team = getTeam(players[best].team)
		team.roundPoints += points

		// allow all the players to process the trick
		for (i in 0 until numPlayers) players[i].onProcessTrick(this, pTrick, players[best], points)
		// message(best, "Team %s gets %d points", team.name.getStr(), points);
		if (numTricks == Player.MAX_PLAYER_TRICKS) {
			state = State.PROCESS_ROUND
		} else {
			startPlayer = best
			state = State.RESET_TRICK
		}
		clearTricks()
	}

	private fun clearTricks() {
		Arrays.fill(trick, Card())
	}

	private fun processTeam(team: Team, winner: Boolean) {
		var pts = 0
		if (team.bid === NO_BID) {
			if (winner || team.totalPoints < 45 || team.roundPoints < 0) pts = team.roundPoints
		} else {
			var multiplier = 1
			if (team.bid.trump === Suit.NOTRUMP) {
				message("\n\nDOUBLE MULTIPLIER FOR NOTRUMP\n\n")
				multiplier = 2
			}
			pts = if (team.bid.numTricks <= team.roundPoints) multiplier * team.roundPoints else -multiplier * team.bid.numTricks //roundPoints;
		}
		team.totalPoints += pts
		message("Team %s %s %d points for %d total.", team.name,
			if (pts > 0) "gains" else "loses", Math.abs(pts), team.totalPoints)
	}

	private fun shuffle() {
		message("shuffling ...")
		for (i in 0..1999) {
			val a = Utils.getRandom().nextInt(numDeckCards)
			val b = Utils.getRandom().nextInt(numDeckCards)
			val t = deck[a]
			deck[a] = deck[b]
			deck[b] = t
		}
	}

	/**
	 * Set the name of a team
	 *
	 * @param index
	 * @param name
	 */
	fun setTeam(index: Int, name: String) {
		teams[index].name = name
	}

	/**
	 *
	 * @param index
	 * @param player
	 */
	fun setPlayer(index: Int, player: Player?) {
		if (player == null) throw NullPointerException("player cannot be null")
		if (index < 0 || index >= numPlayers) throw IndexOutOfBoundsException("Invalid index " + index + " range is [0-" + numPlayers + ")")
		players[index] = player
		player.playerNum = index
		val i_mod_2 = index % 2
		val i_div_2 = index / 2
		players[index].team = i_mod_2
		teams[i_mod_2].players[i_div_2] = index
	}

	/**
	 *
	 */
	fun runGame() {
		var i: Int
		when (state) {
			State.NEW_GAME -> {
				message("Begin new game")
				numCards = 0
				curBidTeam = -1
				numRounds = 0
				i = 0
				while (i < numPlayers) {
					if (players[i] == null) throw NullPointerException("Null player at index $i")
					players[i].onNewGame(this)
					i++
				}
				state = State.NEW_ROUND
				dealer = Utils.getRandom().nextInt(numPlayers)
				curPlayer = (dealer + 1) % numPlayers
				teams[0].totalPoints = 0
				teams[1].totalPoints = 0
				message("Teams are: ")
				message("Team A: " + teams[0])
				message("Team B: " + teams[1])
			}
			State.NEW_ROUND -> {
				message("Starting round %d", numRounds++)
				message("Dealer is %s", getPlayer(dealer).name)
				i = 0
				while (i < numPlayers) {
					players[i].onNewRound(this)
					players[i].tricks.clear()
					i++
				}
				curPlayer = (dealer + 1) % numPlayers
				curBidTeam = -1
				numCards = 0
				numTricks = 0
				shuffle()
				state = State.DEAL
				clearTricks()
				teams[0].roundPoints = 0
				teams[1].roundPoints = 0
				teams[0].bid = NO_BID
				teams[1].bid = NO_BID
			}
			State.DEAL -> {
				val card = deck[numCards++]
				players[curPlayer].mHand.add(card)
				players[curPlayer].onDealtCard(this, card)
				if (numCards == numDeckCards) {
					state = State.BID
					var ii = 0
					while (ii < numPlayers) {
						players[ii].mHand.sort()
						ii++
					}
					curPlayer = (dealer + 1) % numPlayers
				} else {
					curPlayer = (curPlayer + 1) % numPlayers
				}
			}
			State.BID -> {
				val p = players[curPlayer]
				message("Player %s Place bid", p.name)
				val options = computeBidOptions(curPlayer == dealer, p.hand)
				val bid = p.makeBid(this, options)
				if (bid == null) {
					message("Player %s passes", p.name)
					return
				}
				if (bid.numTricks != 0) {
					message("%s bids %d %s\n", p.name, bid.numTricks,
						bid.trump.suitString)
					var bidTeam = getCurBidTeam()
					if (bidTeam != null) bidTeam.bid = NO_BID
					bidTeam = getTeam(p.team)
					bidTeam.bid = bid
					startPlayer = curPlayer
				}
				if (curPlayer == dealer) {
					if (curBidTeam < 0) {
						startPlayer = (dealer + 1) % numPlayers
					}
					curPlayer = startPlayer
					state = State.TRICK
				} else {
					curPlayer = (curPlayer + 1) % numPlayers
				}
				message("Player %s bids %s", p.name, bid.toString())
			}
			State.TRICK -> {
				val p = players[curPlayer]
				val options = computePlayerTrickOptions(p)
				val card = p.playTrick(this, options) ?: return
				trick[curPlayer] = card
				message("Player %s played %s", p.name, trick[curPlayer].toPrettyString())
				p.mHand.remove(card)
				p.mHand.sort()
				curPlayer = (curPlayer + 1) % numPlayers
				if (curPlayer == startPlayer) state = State.PROCESS_TRICK
			}
			State.PROCESS_TRICK -> processTrick()
			State.RESET_TRICK -> {
				curPlayer = startPlayer
				state = State.TRICK
			}
			State.PROCESS_ROUND -> {
				message("Processing Round ...")
				processTeam(teams[0],
					teams[0].roundPoints > teams[1].roundPoints)
				processTeam(teams[1],
					teams[1].roundPoints > teams[0].roundPoints)

				// check for a winner
				if (teams[0].totalPoints >= winningPoints && teams[0].totalPoints > teams[1].totalPoints) {
					message("Team %s wins!", teams[0].name)
					state = State.GAME_OVER
				}
				if (teams[1].totalPoints >= winningPoints && teams[1].totalPoints > teams[0].totalPoints) {
					message("Team %s wins!", teams[1].name)
					state = State.GAME_OVER
				}
				if (state !== State.GAME_OVER) {
					dealer = (dealer + 1) % numPlayers
					i = 0
					while (i < numPlayers) {
						players[i].onProcessRound(this)
						i++
					}
					state = State.NEW_ROUND
				}
			}
			State.GAME_OVER -> {
			}
		}
	}

	/**
	 *
	 * @param fileName
	 * @throws IOException
	 *
	 * public void saveGame(String fileName) throws IOException {
	 * File file = new File(fileName);
	 * if (DEBUG_ENABLED)
	 * System.out.println("Writing to " + file.getAbsolutePath());
	 * saveGame(new FileOutputStream(file));
	 * }
	 *
	 * / **
	 * Save a game to the stream.  the stream is always closed.
	 * @param output
	 * @throws IOException
	 *
	 * public void saveGame(OutputStream output) throws IOException {
	 *
	 * PrintWriter out = new PrintWriter(new OutputStreamWriter(output));
	 *
	 * try {
	 * int i;
	 *
	 * out.println("CUR_PLAYER:" + curPlayer);
	 * out.println("START_PLAYER:" + startPlayer);
	 * out.println("DEALER:" + dealer);
	 * out.println("STATE:" + state.ordinal());
	 * out.println("NUM_CARDS:" + numCards);
	 * out.println("CUR_BID_TEAM:" + curBidTeam);
	 * out.println("NUM_TRICKS:" + numTricks);
	 * out.println("NUM_ROUNDS:" + numRounds);
	 *
	 * int tricksFound = 0;
	 * for (i=0; i<trick.length></trick.length>; i++) {
	 * if (trick[i] != null) {
	 * out.println("TRICK:" + i + " " + trick[i]);
	 * tricksFound ++;
	 * }
	 * }
	 * //if (tricksFound != numTricks) {
	 * //    System.err.println("Expected equals");
	 * // }
	 * //            assert(tricksFound == numTricks);
	 * if (tricksFound > 0) {
	 * assert(trick[startPlayer] != null);
	 * }
	 *
	 * for (i = 0; i < NUM_PLAYERS; i++) {
	 * if (players[i] == null)
	 * continue;
	 * out.println("PLAYER:" + i + " " + describePlayer(players[i])
	 * + " \"" + players[i].getName() + "\" {");
	 * players[i].write(out);
	 * out.println("}");
	 * }
	 *
	 * for (i = 0; i < NUM_TEAMS; i++) {
	 * Team tm = teams[i];
	 * out.println("TEAM:" + i + " \"" + tm.name + "\" {");
	 * out.println("BID:" + tm.bid);
	 * out.println("ROUND_PTS:" + tm.roundPoints);
	 * out.println("TOTAL_PTS:" + tm.totalPoints);
	 * out.println("}");
	 * }
	 * } catch (IOException e) {
	 * throw e;
	 * } catch (Exception e) {
	 * e.printStackTrace();
	 * throw new IOException(e.getClass().getSimpleName() + " " + e.getMessage(), e);
	 * } finally {
	 * try {
	 * out.close();
	 * } catch (Exception e) {}
	 * }
	 * }
	 *
	 * / **
	 * Convenience
	 *
	 * @param fileName
	 * @throws IOException
	 *
	 * public void loadGame(String fileName) throws IOException {
	 * loadGame(new FileInputStream(fileName));
	 * }
	 *
	 * / **
	 * Load a game from a stream.  the stream is always closed even on exception.
	 * @param in
	 * @throws IOException
	 *
	 * public void loadGame(InputStream in) throws IOException {
	 *
	 * final int[] lineNum = new int[1];
	 * String line;
	 * BufferedReader input = new BufferedReader(new InputStreamReader(in)) {
	 * @Override
	 * public String readLine() throws IOException {
	 * String line = super.readLine();
	 * lineNum[0]++;
	 * return line;
	 * }
	 * };
	 *
	 * try {
	 * while (true) {
	 *
	 * line = input.readLine();
	 * if (line == null)
	 * break;
	 * line = line.trim();
	 * if (line.length() == 0 || line.startsWith("#"))
	 * continue; // skip past empty lines and comments
	 *
	 * int index = 0;
	 * String entry = parseNextLineElement(line, index++);
	 * if (entry.equals("CARD")) {
	 * int num = Integer.parseInt(parseNextLineElement(line, index++));
	 * Card card = Card.parseCard(parseNextLineElement(line, index++) + " " + parseNextLineElement(line, index++));
	 * if (num < 0 || num >= NUM_DECK_CARDS) {
	 * throw new IOException("Error line " + lineNum
	 * + " Invalid value for card num " + num);
	 * }
	 * cards[num] = card;
	 * }
	 *
	 * else if (entry.equals("CUR_PLAYER")) {
	 * int n = Integer.parseInt(parseNextLineElement(line, index++));
	 * if (n < 0 || n >= NUM_PLAYERS) {
	 * throw new Exception("Integer " + n
	 * + " out of bounds (0-NUM_PLAYERS]");
	 * }
	 * this.curPlayer = n;
	 * }
	 *
	 * else if (entry.equals("START_PLAYER")) {
	 * int n = Integer.parseInt(parseNextLineElement(line, index++));
	 * if (n < 0 || n >= NUM_PLAYERS) {
	 * throw new Exception("Integer " + n
	 * + " out of bounds (0-NUM_PLAYERS]");
	 * }
	 * this.startPlayer = n;
	 * }
	 *
	 * else if (entry.equals("DEALER")) {
	 * int n = Integer.parseInt(parseNextLineElement(line, index++));
	 * if (n < 0 || n >= NUM_PLAYERS) {
	 * throw new Exception("Integer " + n
	 * + " out of bounds (0-NUM_PLAYERS]");
	 * }
	 * this.dealer = n;
	 * }
	 *
	 * else if (entry.equals("STATE")) {
	 * int n = Integer.parseInt(parseNextLineElement(line, index++));
	 * if (n < 0 || n >= State.values().length) {
	 * throw new Exception("Integer " + n
	 * + " out of bounds (0-" + State.values().length
	 * + "]");
	 * }
	 * this.state = State.values()[n];
	 * }
	 *
	 * else if (entry.equals("NUM_CARDS")) {
	 * int n = Integer.parseInt(parseNextLineElement(line, index++));
	 * if (n < 0 || n > NUM_DECK_CARDS) {
	 * throw new Exception("Integer " + n
	 * + " out of bounds (0-" + NUM_DECK_CARDS + ")");
	 * }
	 * this.numCards = n;
	 * }
	 *
	 * else if (entry.equals("CUR_BID_TEAM")) {
	 * int n = Integer.parseInt(parseNextLineElement(line, index++));
	 * if (n >= NUM_TEAMS) {
	 * throw new Exception("Integer " + n
	 * + " out of bounds (-INF-NUM_TEAMS]");
	 * }
	 * this.curBidTeam = n;
	 * }
	 *
	 * else if (entry.equals("NUM_TRICKS")) {
	 * int n = Integer.parseInt(parseNextLineElement(line, index++));
	 * if (n < 0) {
	 * throw new Exception("Invalid valaue for NUM_TRICKS '"
	 * + n + "'");
	 * }
	 * this.numTricks = n;
	 * }
	 *
	 * else if (entry.equals("NUM_ROUNDS")) {
	 * int n = Integer.parseInt(parseNextLineElement(line, index++));
	 * if (n < 0) {
	 * throw new Exception("Invalid valaue for NUM_ROUNDS '"
	 * + n + "'");
	 * }
	 * this.numRounds = n;
	 * }
	 *
	 * else if (entry.equals("TRICK")) {
	 * int n = Integer.parseInt(parseNextLineElement(line, index++));
	 * trick[n] = Card.parseCard(parseNextLineElement(line, index++) + " " + parseNextLineElement(line, index++));
	 * }
	 *
	 * else if (entry.equals("PLAYER")) {
	 * int n = Integer.parseInt(parseNextLineElement(line, index++));
	 * Player p = instantiatePlayer(parseNextLineElement(line, index++), parseNextLineElement(line, index++));
	 * if (n < 0 || n >= NUM_PLAYERS) {
	 * throw new Exception("Integer value '" + n
	 * + "' out of bounds [0-" + NUM_PLAYERS + ")");
	 * }
	 * p.read(this, input);
	 * this.setPlayer(n, p);
	 * }
	 *
	 * else if (entry.equals("TEAM")) {
	 * int n = Integer.parseInt(parseNextLineElement(line, index++));
	 * if (n < 0 || n >= NUM_TEAMS) {
	 * throw new Exception("Integer value '" + n
	 * + "' out of bounds [0-" + NUM_TEAMS + ")");
	 * }
	 * teams[n].name = parseNextLineElement(line, index++);
	 * teams[n].parseTeamInfo(input);
	 * }
	 *
	 * else {
	 * throw new Exception("Cant parse line '" + line + "'");
	 * }
	 * }
	 *
	 * } catch (Exception e) {
	 * e.printStackTrace();
	 * throw new IOException("Error line:" + lineNum[0] + " "
	 * + e.getClass().getSimpleName() + " " + e.getMessage());
	 * } finally {
	 * try {
	 * input.close();
	 * } catch (Exception e) {
	 * }
	 * }
	 *
	 * }
	 *
	 * private final static Pattern pattern = Pattern.compile("(\"[A-Za-z0-9_ .]+\")|([1-9][0-9]*)|(0)|([a-zA-Z][^ :]*)");
	 *
	 * static String parseNextLineElement(String line, int index) {
	 * Matcher matcher = pattern.matcher(line);
	 * int start = 0;
	 * while (true) {
	 * if (!matcher.find(start))
	 * throw new IllegalArgumentException("Failed to find the " + index + "nth entry in line: " + line);
	 * start = matcher.end();
	 * if (--index < 0)
	 * break;
	 * }
	 * String s = matcher.group();
	 * if (s.startsWith("\"")) {
	 * s = s.substring(1, s.length()-1);
	 * }
	 * return s;
	 * }
	 *
	 *
	 *
	 * / **
	 */
	fun newGame() {
		curPlayer = 0
		startPlayer = 0
		dealer = 0
		state = State.NEW_GAME
		numCards = 0
		curBidTeam = -1
		numTricks = 0
		numRounds = 0
		teams[0].bid = NO_BID
		teams[1].bid = NO_BID
		clearTricks()
	}

	/**
	 *
	 * @return
	 */
	val isGameOver: Boolean
		get() = state === State.GAME_OVER

	/**
	 *
	 * @return
	 */
	fun getCurBidTeam(): Team? {
		return if (curBidTeam < 0) null else teams[curBidTeam]
	}

	/**
	 *
	 * @param index
	 * @return
	 */
	fun getTrick(index: Int): Card {
		return trick[index]
	}

	/**
	 *
	 * @return
	 */
	val trickLead: Card
		get() = trick[startPlayer]

	/**
	 *
	 * @return
	 */
    val trickWinnerIndex: Int
		get() = getTrickWinnerIndex(trump, trick, startPlayer)

	/**
	 *
	 * @return
	 */
	val trickWinner: Player?
		get() {
			val index = trickWinnerIndex
			return if (index < 0) null else players[index]
		}

	/**
	 * Zero based index.
	 * @param index
	 * @return
	 */
	fun getTeam(index: Int): Team {
		return teams[index]
	}

	/**
	 * Get the current trump suit based on the bid.
	 * @return
	 */
	val trump: Suit
		get() = getCurBidTeam()?.bid?.trump?:Suit.NOTRUMP

	/**
	 * Get the lead suit
	 * @return
	 */
	val lead: Suit
		get() = if (trick[startPlayer] != null) trick[startPlayer].suit else Suit.NOTRUMP

	/**
	 * Zero based index
	 * @param index
	 * @return
	 */
	fun getPlayer(index: Int): Player {
		return players[index]
	}

	/**
	 *
	 * @return
	 */
	fun getPlayers(): Iterable<Player> {
		return Arrays.asList(*players)
	}

	/**
	 * Handle to affect how game messages are handled.  Default prints to stdout.
	 *
	 * @param format
	 * @param params
	 */
	protected fun message(format: String, vararg params: Any) {
		val msg = String.format(format, *params)
		println(msg)
	}

	/**
	 * Return the winning card from 2 cards.  The current state of the game affects this method,
	 * specifically, the trick lead and trump suit is used.
	 *
	 * @param c0
	 * @param c1
	 * @return
	 */
	fun getWinningCard(c0: Card, c1: Card): Card {
		val lead = if (trickLead == null) Suit.NOTRUMP else trickLead.suit
		val trump = trump

		// trump suits win anything that is not a trump
		if (c0.suit === trump && c1.suit !== trump) return c0
		if (c0.suit !== trump && c1.suit === trump) return c1

		// otherwise lead suits win anythiing that is not a lead
		if (c0.suit === lead && c1.suit !== lead) return c0
		if (c0.suit !== lead && c1.suit === lead) return c1

		// otherwise use the rank
		return if (c0.rank.ordinal < c1.rank.ordinal) c1 else c0
	}

	/**
	 * Get the card instance associated with a rank and suit.
	 * return null for invalid cards.
	 * @param rank
	 * @param suit
	 * @return
	 */
	fun getCard(rank: Rank, suit: Suit): Card {
		for (i in 0 until numDeckCards) {
			if (deck[i].rank === rank && deck[i].suit === suit) return deck[i]
		}
		throw IllegalArgumentException("No card $rank of $suit in deck")
	}

	fun getTeammate(player: Int): Int {
		val t = teams[getPlayer(player).team]
		if (t.players[0] == player) {
			return t.players[1]
		} else if (t.players[1] == player) {
			return t.players[0]
		}
		throw RuntimeException("Cannot find team for player $player")
	}

	/**
	 *
	 */
	init {
		initCards()
		teams[0] = Team("Team0")
		teams[1] = Team("Team1")
		newGame()
	}
}