package cc.game.soc.core;

/**
 * Created by chriscaron on 2/21/18.
 */

public class StringsEnglish implements IStrings {
    @Override
    public String getSpecialVictoryTypeString(SpecialVictoryType type) {
        switch (type) {
            case LargestArmy:
                return "Given to player who has largest army";
            case LongestRoad:
                return "Given to player with the longest road";
            case DefenderOfCatan:
                return "Awarded when a player single-handedly defends against Barbarians.";
            case Tradesman:
                return "Given to the player who controls the Merchant.";
            case Constitution:
                return "When this prgress card is picked it is emmediately played and cannot be taken.";
            case Printer:
                return "When this progress card is picked it is emmediately played and cannot be taken.";
            case Merchant:
                return "Given to last player who has placed the Merchant";
            case HarborMaster:
                return "Player who has most harbor points gets this card";
            case OldBoot:
                return "Counts against your points so you need 1 extra point to win";
            case WealthiestSettler:
                return "Given to player with most gold coins";
            case PoorestSettler:
                return "Given to player with fewest gold coins";
            case DiscoveredIsland:
                return "One given for each discovered island";
            case DamagedRoad:
                return "One of users roads is damaged";
            case CapturePirateFortress:
                return "Given for each pirate fortress conquered";
            case Explorer:
                return "Given to player who has discovered most territories";
        }
        return null;
    }

    @Override
    public String getResourceTypeHelpText(ResourceType type) {
        switch (type) {
            case Wood:
                return ("Produced by Forest Tiles");
            case Sheep:
                return ("Produced by Pasture Tiles");
            case Ore:
                return ("Produced by Mountains Tiles");
            case Wheat:
                return ("Produced by Fields Tiles");
            case Brick:
                return ("Produced by Hills Tiles");
        }
        return null;
    }
}
