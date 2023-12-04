package cc.lib.yahtzee

import cc.lib.reflector.Reflector

/**
 * http://grail.sourceforge.net/demo/yahtzee/rules.html
 *
 * The Rules of Yahtzee
 * Standard Play
 * Objective of the Game
 * Yahtzee can be played in solitary or by a group. The group version simply consists of a number of players playing the solitary version simultaneously, with the highest score winning. I'll explain the solitary version, since that's what the applet lets you play (although you could use the "Clone Window" option to let multiple players play).
 *
 * The game consists of 13 rounds. In each round, you roll the dice and then score the roll in one of 13 categories. You must score once in each category -- which means that towards the end of the game you may have to settle for scoring zero in some categories. The score is determined by a different rule for each category; see the section on Scoring below.
 *
 * The object of the game is to maximize your total score (of course :-). The game ends once all 13 categories have been scored.
 * Rolling the Dice
 * You have five dice which you can roll, represented by the die faces at the top of the applet window. To start with, you roll all dice by clicking on the Roll All button. After you roll all dice, you can either score the current roll, or re-roll any or all of the five dice.
 *
 * To re-roll some of the dice, click on the toggle button underneath the die face you want to re-roll, then click on the Re-roll button. This will re-roll the selected dice, leaving the unselected ones unchanged.
 *
 * You can roll the dice a total of three times -- the initial roll (in which you must roll all the dice), plus two re-rolls of any or all dice. After rolling three times, you must score the roll.
 *
 * Once you've scored the roll, you roll all the dice again and repeat the process. You continue until all 13 categories have been filled, at which time the game is over.
 *
 * Scoring
 * Once you have the dice face combination you want to score, you score the roll in one of the 13 categories. You do this by clicking on one of the radio buttons in either the Upper Scores or Lower Scores box. Once a category has been scored, it is closed out for the rest of the game; you cannot change a category's score once it's been set. Each category defines its own scoring rules, as described below.
 * Upper Scores
 * In the upper scores, you total only the specified die face. So if you roll:
 * <3> <3> <4> <3> <6>
 * and score in the Threes category, your total for that entry would be 9. This same roll would yield zero points if you scored it in the Aces (Ones), Twos, or Fives category, four points if you scored it in the Fours category, or six points if you scored it in the Sixes category.
 *
 * When the game is over, if you score 63 or more upper points (an average of 3 die faces per category), you will get an upper bonus of 35 points. Of course do don't need to score exactly three die faces in each upper category to get the bonus, as long as the upper total is at least 63.
 *
 * Lower Scores
 * In the lower scores, you score either a set amount (defined by the category), or zero if you don't satisfy the category requirements.
 * 3 and 4 of a Kind
 * For 3 of a Kind, you must have at least three of the same die faces. If so, you total all the die faces and score that total. Similarly for 4 of a Kind, except that you must have 4 of the 5 die faces the same. So for example, if you rolled:
 * <5> <5> <3> <2> <5>
 * you would receive 20 points for 3 of a Kind, but zero points for 4 of a Kind.
 *
 * Straights
 * Like in poker, a straight is a sequence of consecutive die faces; a small straight is 4 consecutive faces, and a large straight is 5 consecutive faces. Small straights score 30 points and large straights score 40 points. Thus, if you rolled:
 * <5> <4> <3> <2> <6>
 * you could score either a small straight or a large straight, since this roll satisfies both.
 *
 * Full House
 * Again as in poker, a Full House is a roll where you have both a 3 of a kind, and a pair. Full houses score 25 points.
 *
 * Yahtzee
 * A Yahtzee is a 5 of a Kind (i.e. all the die faces are the same), and it scores 50 points. If you roll more than one Yahtzee in a single game, you will earn a 100 point bonus for each additional Yahtzee roll, provided that you have already scored a 50 in the Yahtzee category. If you have not scored in the Yahtzee category, you will not receive a bonus. If you have scored a zero in the Yahtzee category, you cannot receive any bonuses during the current game.
 *
 * You can also use subsequent Yahtzee's as jokers in the lower scores section, provided the following criteria have been satisfied:
 *
 * You have scored a zero or 50 in the Yahtzee category.
 * You have filled the corresponding category in the upper scores section. For example, if you have rolled:
 * <5> <5> <5> <5> <5>
 * the Fives category must also be filled.
 *
 * If this is the case, you can use the Yahtzee as a joker to fill in any lower scores category. You score the category as normal. Thus for the Small Straight, Large Straight, and Full House categories, you would score 30, 40, and 25 points respectively. For the 3 of a Kind, 4 of a Kind, and Chance categories, you would score the total of the die face.
 * Chance
 * Chance is the catch-all roll. You can roll anything and you simply total all the die faces values.
 * @author chriscaron
 */
class YahtzeeRules : Reflector<YahtzeeRules>() {
	companion object {
		init {
			addAllFields(YahtzeeRules::class.java)
		}
	}

	var numRollsPerRound = 3
	var numDicePerRoll = 5
	var yahtzeeBonusScore = 100 // bonus for subsequent yahtzees
	var upperBonusPoints = 50
	var upperScoreForBonus = 63 // min score in upper range to achieve bonus
	var isEnableAlternateVersion = false
	var numDiceSides = 6
}