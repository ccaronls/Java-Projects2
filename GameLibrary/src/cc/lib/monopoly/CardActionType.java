package cc.lib.monopoly;

public enum CardActionType {

    CH_GO_BACK("Go Back three Space"),
    CH_LOAN_MATURES("Your building loan matures. Collect $150"),
    CH_MAKE_REPAIRS("Make general repairs on all your property. For each house pay $25, for each hotel pay $150"),
    CH_GET_OUT_OF_JAIL("GET OUT OF JAIL FREE. This card may be kept until needed or traded."),
    CH_ELECTED_CHAIRMAN("You have been elected chairman of the board. Pay eash player $50"),
    CH_ADVANCE_RAILROAD("ADVANCE TO THE NEAREST RAILROAD. If UNOWNED, you may buy from the bank. If OWNED, pay owner twice the rental to which they are otherwise entitled."),
    CH_ADVANCE_RAILROAD2("ADVANCE TO THE NEAREST RAILROAD. If UNOWNED, you may buy from the bank. If OWNED, pay owner twice the rental to which they are otherwise entitled."),
    CH_ADVANCE_ILLINOIS("ADVANCE TO ILLINOIS AVE. If you pass go collect $200."),
    CH_ADVANCE_TO_NEAREST_UTILITY("ADVANCE TO THE NEAREST UTILITY. If UNOWNED, you may buy from the bank. If OWNED, pay owner twice the rental to which they are otherwise entitled."),
    CH_ADVANCE_READING_RAILROAD("TAKE A TRIP TO READING RAILROAD. If you pass go collect $200."),
    CH_BANK_DIVIDEND("BANK PAYS YOU DIVIDEND OF $50."),
    CH_GO_TO_JAIL("GO TO JAIL. GO DIRECTLY TO JAIL. DO NOT PASS GO, DO NOT COLLECT $200."),
    CH_SPEEDING_TICKET("SPEEDING FINE $15."),
    CH_ADVANCE_BOARDWALK("ADVANCE TO BOARDWALK."),
    CH_ADVANCE_ST_CHARLES("ADVANCE TO ST. CHARLES PLACE. If you pass go collect $200."),
    CH_ADVANCE_GO("ADVANCE TO GO. (COLLECT $200.)"),

    CC_BANK_ERROR("BANK ERROR IN YOUR FAVOR. COLLECT $200."),
    CC_SALE_OF_STOCK("FROM SALE OF STOCK YOU GET $50."),
    CC_BEAUTY_CONTEST("YOU HAVE WON SECOND PRIZE IN BEAUTY CONTEST. COLLECT $10."),
    CC_ASSESSED_REPAIRS("YOU ARE ASSESSED FOR STREET REPAIRS. PAY $40 PER HOUSE AND $115 PER HOTEL YOU OWN."),
    CC_HOSPITAL_FEES("HOSPITAL FEES. PAY $100."),
    CC_CONSULTANCY_FEE("RECEIVE $25 CONSULTANCY FEE."),
    CC_HOLIDAY_FUND_MATURES("HOLIDAY FUND MATURES. RECEIVE $100."),
    CC_LIFE_INSURANCE_MATURES("LIFE INSURANCE MATURES. COLLECT $100."),
    CC_BIRTHDAY("ITS YOUR BIRTHDAY. COLLECT $10 FROM EACH PLAYER."),
    CC_SCHOOL_FEES("SCHOOL FEES. PAY $50."),
    CC_ADVANCE_TO_GO("ADVANCE TO GO. (COLLECT $200)"),
    CC_GO_TO_JAIL("GO TO JAIL. GO DIRECTLY TO JAIL. DO NOT PASS GO, DO NOT COLLECT $200."),
    CC_INHERITANCE("YOU INHERIT $100."),
    CC_INCOME_TAX_REFUND("INCOME TAX REFUND. COLLECT $20."),
    CC_DOCTORS_FEES("DOCTORS FEES. PAY $50."),
    CC_GET_OUT_OF_JAIL("GET OUT OF JAIL FREE. This card may be kept until needed or traded."),

    ;

    CardActionType(String desc) {
        this.desc = desc;
    }

    final String desc;

    public boolean isChance() {
        return name().startsWith("CH");
    }

    public String getDescription() {
        return desc;
    }
}
