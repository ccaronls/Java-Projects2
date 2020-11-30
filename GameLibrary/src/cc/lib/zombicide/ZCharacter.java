package cc.lib.zombicide;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import cc.lib.game.AGraphics;
import cc.lib.game.GDimension;
import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.utils.Table;

public final class ZCharacter extends ZActor<ZPlayerName> {

    static final Logger log = LoggerFactory.getLogger(ZCharacter.class);

    static {
        addAllFields(ZCharacter.class);
    }

    public final static int MAX_BACKPACK_SIZE = 5;
    public final static int MAX_WOUNDS = 4;

    ZPlayerName name;
    int woundBar;
    int dangerBar;
    boolean organizedThisTurn=false;
    private List<ZActionType> actionsDoneThisTurn = new ArrayList<>();
    final List<ZSkill> allSkills = new ArrayList<>();
    final List<ZSkill> availableSkills = new ArrayList<>();

    private final List<ZEquipment> backpack = new ArrayList<>();
    ZEquipment leftHand, rightHand, body;
    int [] kills = new int[ZZombieType.values().length];

    void clear() {
        Arrays.fill(kills, 0);
        leftHand = rightHand = body = null;
        backpack.clear();
        allSkills.clear();
        availableSkills.clear();
        organizedThisTurn=false;
        dangerBar=0;
        woundBar=0;
    }

    public ZCharacter() {
        super(-1);
    }

    @Override
    public int getImageId() {
        return name.imageId;
    }

    @Override
    void onBeginRound() {
        super.onBeginRound();
        actionsDoneThisTurn.clear();
        availableSkills.clear();
        availableSkills.addAll(allSkills);
        organizedThisTurn = false;
    }

    @Override
    protected synchronized void deserialize(BufferedReader _in) throws Exception {
        super.deserialize(_in);
        name.character = this;
    }

    Table getKillsTable() {
        Table tab = new Table().setNoBorder().setPadding(0);
        for (ZZombieType nm : ZZombieType.values()) {
            tab.addRow(nm, "x", kills[nm.ordinal()]);
        }
        return tab;
    }

    @Override
    public ZPlayerName getType() {
        return name;
    }

    @Override
    public String name() {
        return name.name();
    }

    void onKilledZombie(ZZombie zombie) {
        kills[zombie.type.ordinal()]++;
    }

    @Override
    protected boolean performAction(ZActionType action, ZGame game) {
        for (ZSkill skill : availableSkills) {
            if (skill.modifyActionsRemaining(this, action, game)) {
                game.getCurrentUser().showMessage(name() + " used " + skill + " for a free action");
                availableSkills.remove(skill);
                return true;
            }
        }
        switch (action) {
            case ORGANIZE:
                if (organizedThisTurn)
                    return true;
                organizedThisTurn = true;
                break;
        }
        if (action.oncePerTurn()) {
            actionsDoneThisTurn.add(action);
        }
        if (isInvisible() && action.breaksInvisibility()) {
            availableSkills.remove(ZSkill.Invisible);
        }
        return super.performAction(action, game);
    }

    boolean tryOpenDoor(ZGame game) {
        List<ZWeapon> weapons = getWeapons();
        // order the weapons so that the best choice for door is in the front
        Collections.sort(weapons, new Comparator<ZWeapon>() {
            @Override
            public int compare(ZWeapon o1, ZWeapon o2) {
                // first order by % to open
                int v1 = o1.getOpenDoorValue();
                int v2 = o2.getOpenDoorValue();
                return Integer.compare(v2, v1);
            }
        });
        for (ZWeapon w : weapons) {
            if (w.canOpenDoor()) {
                if (w.type.openDoorsIsNoisy)
                    game.addNoise(occupiedZone, 1);
                if (w.type.meleeStats.dieRollToOpenDoor>1) {
                    Integer [] die = game.rollDice(1);
                    if (die[0] < w.type.meleeStats.dieRollToOpenDoor) {
                        game.getCurrentUser().showMessage(name() + " Failed to open the door with their " + w);
                        return false;
                    }
                }
                game.getCurrentUser().showMessage(name() + " Used their " + w + " to break open the door");
                return true;
            }
        }
        return false;
    }

    @Override
    protected int getActionsPerTurn() {
        int actions = 3;
        if (availableSkills.contains(ZSkill.Plus1_Action))
            actions++;
        return actions;
    }

    public boolean canUnjamDoor() {
        for (ZWeapon w : getWeapons()) {
            if (w.canOpenDoor())
                return true;
        }
        return false;
    }

    /**
     *
     * @return
     */
    public List<ZWeapon> getWeapons() {
        return (List)Utils.filterItems(object -> object instanceof ZWeapon, leftHand, rightHand, body);
    }

    /**
     * Return any armor and / or shileds player has
     * @return
     */
    public List<ZArmor> getArmor() {
        return (List)Utils.filterItems(object -> object instanceof ZArmor, leftHand, rightHand, body);
    }

    public List<ZSpell> getSpells() {
        List<ZSpell> spells = (List)Utils.filterItems(object -> object instanceof ZSpell, leftHand, rightHand, body);
        if (availableSkills.contains(ZSkill.Spellbook)) {
            spells.addAll((List)Utils.filter(new ArrayList<>(backpack), object -> object.isEnchantment()));
        }
        return spells;
    }

    public List<ZArmor> getArmorForDefense() {
        List<ZArmor> armor = getArmor();
        if (availableSkills.contains(ZSkill.Iron_hide)) {
            if (armor.size() == 0) {
                armor.add(new ZArmor(ZArmorType.IRON_HIDE));
            } else {
                List<ZArmor> adjustedArmor = new ArrayList<>();
                for (ZArmor ar : armor) {
                    adjustedArmor.add(new ZArmor(ar.getType(), 1));
                }
                return adjustedArmor;
            }
        } else if (availableSkills.contains(ZSkill.Steel_hide)) {
            if (armor.size() == 0) {
                armor.add(new ZArmor(ZArmorType.STEEL_HIDE));
            } else {
                List<ZArmor> adjustedArmor = new ArrayList<>();
                for (ZArmor ar : armor) {
                    adjustedArmor.add(new ZArmor(ar.getType(), 1));
                }
                return adjustedArmor;
            }
        }

        return armor;
    }

    /**
     *
     * @return
     */
    public boolean isDualWeilding() {
        if (leftHand == null || rightHand == null)
            return false;
        if (!leftHand.getType().equals(rightHand.getType()))
            return false;
        if (leftHand.canDualWield())
            return true;
        for (ZSkill skill : this.availableSkills) {
            if (skill.canTwoHand((ZWeapon)leftHand))
                return true;
        }
        return false;
    }

    public int getNumBackpackItems() {
        return backpack.size();
    }

    public ZEquipment getBackpackItem(int index) {
        return backpack.get(index);
    }

    public List<ZEquipment> getBackpack() {
        return Collections.unmodifiableList(backpack);
    }

    public ZEquipSlot getEmptyEquipSlotFor(ZEquipment e) {
        if (body == null && Utils.linearSearch(name.alternateBodySlots, e.getType()) >= 0) {
            return ZEquipSlot.BODY;
        }
        switch (e.getSlotType()) {
            case HAND:
                if (leftHand == null) {
                    return ZEquipSlot.LEFT_HAND;
                }
                if (rightHand == null) {
                    return ZEquipSlot.RIGHT_HAND;
                }
                break;
            case BODY:
                if (body == null) {
                    return ZEquipSlot.BODY;
                }
                break;
        }
        assert(!isBackpackFull());
        return ZEquipSlot.BACKPACK;
    }

    public ZEquipSlot equip(ZEquipment e) {
        if (body == null && Utils.linearSearch(name.alternateBodySlots, e.getType()) >= 0) {
            body = e;
            return ZEquipSlot.BODY;
        }
        switch (e.getSlotType()) {
            case HAND:
                if (leftHand == null) {
                    leftHand = e;
                    return ZEquipSlot.LEFT_HAND;
                }
                if (rightHand == null) {
                    rightHand = e;
                    return ZEquipSlot.RIGHT_HAND;
                }
                break;
            case BODY:
                if (body == null) {
                    body = e;
                    return ZEquipSlot.BODY;
                }
                break;
        }
        if (backpack.size() < MAX_BACKPACK_SIZE) {
            backpack.add(e);
            return ZEquipSlot.BACKPACK;
        }

        assert(false);
        return null;
    }

    public ZEquipment removeEquipment(Object type, ZEquipSlot slot) {
        ZEquipment removed = null;
        switch (slot) {
            case BODY:
                removed = body;
                body = null;
                break;
            case BACKPACK: {
                int idx = backpack.indexOf(type);
                assert(idx >= 0);
                removed = backpack.remove(idx);
                break;
            }
            case LEFT_HAND:
                removed = leftHand;
                leftHand = null;
                break;
            case RIGHT_HAND:
                removed = rightHand;
                rightHand = null;
                break;
        }
        return removed;
    }
    public ZEquipment attachEquipment(ZEquipment equipment) {
        return attachEquipment(equipment, getEmptyEquipSlotFor(equipment));
    }

    public ZEquipment attachEquipment(ZEquipment equipment, ZEquipSlot slot) {
        assert(slot != null);
        ZEquipment prev = null;
        switch (slot) {
            case RIGHT_HAND:
                prev = rightHand;
                rightHand = equipment;
                break;
            case LEFT_HAND:
                prev = leftHand;
                leftHand = equipment;
                break;
            case BODY:
                prev = body;
                body = equipment;
                break;
            case BACKPACK:
                backpack.add(equipment);
                break;
        }
        return prev;
    }

    public ZSkillLevel getSkillLevel() {
        return ZSkillLevel.getLevel(dangerBar);
    }

    public Table getInfoTable(ZGame game) {

        /*

        Left Hand    |   Body    |    Right Hand    |    Stats    |     Skills
        -----------------------------------------------------------------------

        <left hand card? | <body card> | <right hand card> | <stats card> | <skills[0]>


         */

        Table info = new Table().setNoBorder().setPadding(0);

        for (ZEquipSlot slot : ZEquipSlot.values()) {
            switch (slot) {
                case LEFT_HAND:
                    if (isDualWeilding())
                        info.addColumn("DUAL WIELDING",Arrays.asList(getSlotInfo(slot, game)));
                case RIGHT_HAND:
                    if (isDualWeilding())
                        continue;
            }
            Table slotInfo = getSlotInfo(slot, game);
            if (slotInfo != null)
                info.addColumn(slot.getLabel(), Arrays.asList(slotInfo));
        }

        Table stats = new Table().setNoBorder().setPadding(0);
        stats.addRow("Wounds", woundBar);
        ZSkillLevel sl = getSkillLevel();
        int ptsToNxt = sl.getPtsToNextLevel(dangerBar);
        stats.addRow("Skill", sl);
        stats.addRow("Exp", dangerBar);
        stats.addRow("Next level", ptsToNxt);
        stats.addRow("Dual\nWielding", isDualWeilding());

        info.addColumn("STATS", Arrays.asList(stats));
        Table skills = new Table().setNoBorder().addColumnNoHeader(availableSkills);
        info.addColumn("Skills", skills);

        Table main = new Table().setNoBorder()
                .addRow(String.format("%s (%s) moves: %d/%d Body:%s Actions:%s",
                name.name(), name.characterClass,
                getActionsLeftThisTurn(), getActionsPerTurn(),
                Arrays.toString(name.alternateBodySlots),
                actionsDoneThisTurn))
                .addRow(info);
        return main;
    }

    public boolean isBackpackFull() {
        return backpack.size() == MAX_BACKPACK_SIZE;
    }

    @Override
    public GDimension drawInfo(AGraphics g, ZGame game, int width, int height) {
        return getInfoTable(game).draw(g);
    }

    public boolean canTrade() {
        return getAllEquipment().size() > 0;
    }

    public boolean canSearch() {
        return !isBackpackFull() && !actionsDoneThisTurn.contains(ZActionType.SEARCH);
    }

    public List<ZEquipSlot> getEquipableSlots(ZEquipment equip) {
        List<ZEquipSlot> options = new ArrayList<>();
        switch (equip.getSlotType()) {
            case BODY:
                return Arrays.asList(ZEquipSlot.BODY);
            case HAND:
                options.add(ZEquipSlot.LEFT_HAND);
                options.add(ZEquipSlot.RIGHT_HAND);
                if (Utils.linearSearch(name.alternateBodySlots, equip.getType()) >= 0) {
                    options.add(ZEquipSlot.BODY);
                }
                break;
        }
        return options;
    }

    public List<ZWeapon> getMeleeWeapons() {
        List<ZWeapon> slots = new ArrayList<>();
        if (isDualWeilding() && leftHand.isMelee()) {
            slots.add((ZWeapon)leftHand);
        } else {
            if (leftHand != null && leftHand.isMelee())
                slots.add((ZWeapon)leftHand);
            if (rightHand != null && rightHand.isMelee())
                slots.add((ZWeapon)rightHand);
        }
        if (body != null && body.isMelee())
            slots.add((ZWeapon)body);
        return slots;
    }

    public List<ZWeapon> getRangedWeapons() {
        List<ZWeapon> slots = new ArrayList<>();
        if (isDualWeilding() && leftHand.isRanged()) {
            slots.add((ZWeapon)leftHand);
        } else {
            if (leftHand != null && leftHand.isRanged() && isLoaded(leftHand))
                slots.add((ZWeapon)leftHand);
            if (rightHand != null && rightHand.isRanged() && isLoaded(rightHand))
                slots.add((ZWeapon)rightHand);
        }
        if (body != null && body.isRanged() && isLoaded(body))
            slots.add((ZWeapon)body);
        return slots;
    }

    public List<ZWeapon> getMagicWeapons() {
        List<ZWeapon> magic = Utils.filter(getWeapons(), object -> object.isMagic());
        if (availableSkills.contains(ZSkill.Spellbook)) {
            magic.addAll((List)Utils.filter(new ArrayList<>(backpack), object -> object.isMagic()));
        }
        return magic;
    }

    public List<ZItem> getThrowableItems() {
        List<ZItem> slots = new ArrayList<>();
        if (leftHand != null && leftHand.isThrowable())
            slots.add((ZItem)leftHand);
        if (rightHand != null && rightHand.isThrowable())
            slots.add((ZItem)rightHand);
        if (body != null && body.isThrowable())
            slots.add((ZItem)body);
        return slots;
    }


    public boolean canReload() {
        return false;
    }

    public boolean isLoaded(ZEquipment weapon) {
        return true;
    }

    public ZWeaponStat getWeaponStat(ZEquipSlot slot, ZActionType attackType, ZGame game) {
        return getWeaponStat(getSlot(slot), attackType, game);
    }

    public <T extends ZEquipment> T getSlot(ZEquipSlot slot) {
        switch (slot) {
            case BODY:
                return (T)body;
            case RIGHT_HAND:
                return (T)rightHand;
            case LEFT_HAND:
                return (T)leftHand;
        }
        return null;
    }

    public ZWeaponStat getWeaponStat(ZWeapon weapon, ZActionType attackType, ZGame game) {
        ZWeaponStat stat = null;
        switch (attackType) {
            case MELEE:
                if (!weapon.isMelee())
                    return null;
                stat = weapon.type.meleeStats.copy();
                break;
            case RANGED_ARROWS:
                if (!weapon.isRanged())
                    return null;
                if (!weapon.type.usesArrows)
                    return null;
                stat = weapon.type.rangedStats.copy();
                break;
            case RANGED_BOLTS:
                if (!weapon.isRanged())
                    return null;
                if (!weapon.type.usesBolts)
                    return null;
                stat = weapon.type.rangedStats.copy();
                break;
            case MAGIC:
                if (!weapon.isMagic())
                    return null;
                stat = weapon.type.magicStats.copy();
                break;
            default:
                return null;
        }
        for (ZSkill skill : availableSkills) {
            skill.modifyStat(stat, attackType, this, game);
        }
        if (isDualWeilding()) {
            stat.numDice*=2;
        }
        return stat;
    }

    /**
     *
     * @param type
     * @return
     */
    public int getArmorRating(ZZombieType type) {
        int rating = 0;
        for (ZArmor armor : getArmor()) {
            rating += armor.getRating(type);
        }
        return rating;
    }

    /**
     *
     * @return
     */
    public boolean isDead() {
        return woundBar >= MAX_WOUNDS;
    }

    @Override
    public int getNoise() {
        return isInvisible() ? 0 : 1;
    }

    @Override
    public boolean isInvisible() {
        return availableSkills.contains(ZSkill.Invisible);
    }

    Table getSlotInfo(ZEquipSlot slot, ZGame game) {
        switch (slot) {
            case RIGHT_HAND:
                if (rightHand == null)
                    return null;
                return rightHand.getCardInfo(this, game);
            case LEFT_HAND:
                if (leftHand == null)
                    return null;
                return leftHand.getCardInfo(this, game);
            case BODY:
                if (body == null) {
                    return null;
                }
                return body.getCardInfo(this, game);
            case BACKPACK: {
                if (backpack.size() == 0)
                    return null;
                Table table = new Table().setNoBorder();
                for (ZEquipment e : backpack) {
                    table.addRow(e);
                }
                return table;
            }
        }
        return null;
    }

    public List<ZEquipment> getAllEquipment() {
        List<ZEquipment> all = Utils.filterItems(object -> true, leftHand, body, rightHand);
        all.addAll(backpack);
        return all;
    }

    public void removeEquipment(ZEquipment equip) {
        if (backpack.remove(equip))
            return;
        else if (leftHand == equip) {
            leftHand = null;
        } else if (rightHand == equip) {
            rightHand = null;
        } else if (body == equip) {
            body = null;
        } else {
            log.error("Cannot remove item %s from equipment:", equip, getAllEquipment());
        }
    }

    boolean isHolding(ZEquipmentType type) {
        for (ZEquipment e : getAllEquipment()) {
            if (e.getType().equals(type))
                return true;
        }
        return false;
    }

    boolean canReroll(ZActionType action) {
        if (availableSkills.contains(ZSkill.Lucky))
            return true;

        switch (action) {
            case RANGED_ARROWS:
                return isHolding(ZItemType.PLENTY_OF_ARROWS);
            case RANGED_BOLTS:
                return isHolding(ZItemType.PLENTY_OF_BOLTS);
        }
        return false;
    }

    boolean isRoll6Plus1Die(ZActionType type) {
        for (ZSkill skill : availableSkills) {
            if (skill.isRoll6Plus1(type))
                return true;
        }
        return false;
    }

    void onEndOfTurn(ZGame game) {
        for (Iterator<ZSkill> it = availableSkills.iterator(); it.hasNext(); ) {
            if (it.next().onEndOfTurn(game, this)) {
                it.remove();
            }
        }
    }

    @Override
    long getMoveSpeed() {
        return 750;
    }

    @Override
    int getPriority() {
        return 100;
    }
}
