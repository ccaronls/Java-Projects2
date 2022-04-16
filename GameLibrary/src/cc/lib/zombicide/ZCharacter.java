package cc.lib.zombicide;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cc.lib.game.AGraphics;
import cc.lib.game.GColor;
import cc.lib.game.GDimension;
import cc.lib.game.GRectangle;
import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.ui.IButton;
import cc.lib.utils.Table;

public final class ZCharacter extends ZActor<ZPlayerName> implements Table.Model, IButton {

    static final Logger log = LoggerFactory.getLogger(ZCharacter.class);

    static {
        addAllFields(ZCharacter.class);
    }

    public final static int MAX_BACKPACK_SIZE = 5;
    public final static int MAX_WOUNDS = 4;
    public final static int MOVE_SPEED_MILIS = 750;

    private ZPlayerName name;
    private int woundBar;
    private int dangerBar;
    private List<ZActionType> actionsDoneThisTurn = new ArrayList<>();
    private final List<ZSkill> allSkills = new ArrayList<>(); // all skills based on the characters level and choices
    private final Set<ZSkill> availableSkills = new HashSet<>(); // skills from all skills minus ones used that are once per turn
    @Omit
    private List<ZSkill> cachedSkills = null; // cached from getAvailableSkills() - includes skills given by weapons or armor
    private final List<ZSkill>[] skillsRemaining = new List[ZSkillLevel.NUM_LEVELS];

    private final List<ZEquipment> backpack = new ArrayList<>();
    private ZEquipment leftHand, rightHand, body;
    private int [] kills = new int[ZZombieType.values().length];
    private boolean fallen = false;
    private boolean forceInvisible = false;
    private GColor color = GColor.WHITE;
    private int zonesMoved = 0;
    private boolean startingWeaponChosen = false;

    synchronized void clear() {
        Arrays.fill(kills, 0);
        leftHand = rightHand = body = null;
        backpack.clear();
        allSkills.clear();
        availableSkills.clear();
        cachedSkills = null;
        dangerBar=0;
        woundBar=0;
        startingWeaponChosen=false;
    }

    ZCharacter(ZPlayerName name) {
        super(-1);
        this.name = name;
    }

    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (!(other instanceof ZCharacter))
            return false;
        return ((ZCharacter)other).name == name;
    }

    public ZCharacter() {
        this(null);
    }

    @Override
    public int getImageId() {
        return name.imageId;
    }

    @Override
    public int getOutlineImageId() {
        return name.outlineImageId;
    }

    @Override
    public GDimension getDimension() {
        return name.imageDim;
    }

    public GColor getColor() {
        return color;
    }

    public void setColor(GColor color) {
        this.color = color;
    }

    public void addExp(int exp) {
        dangerBar += exp;
    }

    public int getExp() {
        return dangerBar;
    }

    @Override
    void onBeginRound() {
        actionsDoneThisTurn.clear();
        availableSkills.clear();
        availableSkills.addAll(allSkills);
        cachedSkills = null;
        zonesMoved = 0;
        fallen = isDead();
        super.onBeginRound();
    }

    @Override
    protected synchronized void deserialize(BufferedReader in, boolean keepInstances) throws Exception {
        super.deserialize(in, keepInstances);
        name.character = this;
        cachedSkills = null;
    }

    @Override
    public int getMaxCharsPerLine() {
        return 256;
    }

    Table getKillsTable() {
        Table tab = new Table(this).setNoBorder().setPadding(0);
        boolean added = false;
        for (ZZombieType nm : ZZombieType.values()) {
            if (kills.length > nm.ordinal() && kills[nm.ordinal()] > 0) {
                tab.addRow(nm + " x " + kills[nm.ordinal()]);
                added = true;
            }
        }
        if (!added) {
            tab.addRow("None");
        }
        return tab;
    }

    @Override
    public ZPlayerName getType() {
        return name;
    }

    @Override
    public String name() {
        return name.getLabel();
    }

    void onKilledZombie(ZZombie zombie) {
        kills[zombie.type.ordinal()]++;
    }

    @Override
    protected boolean performAction(ZActionType action, ZGame game) {
        if (action == ZActionType.MOVE) {
            zonesMoved++;
        }
        for (ZSkill skill : getAvailableSkills()) {
            switch (skill.modifyActionsRemaining(this, action, game)) {
                case 1:
                    removeAvailableSkill(skill);
                case -1:
                    game.addLogMessage(name() + " used " + skill.getLabel());
                    //game.addLogMessage(name() + " used " + skill.getLabel() + " for a free action");
                    return true;
            }
        }
        switch (action) {
            case INVENTORY:
            case CONSUME:
                if (!hasAvailableSkill(ZSkill.Inventory)) {
                    addAvailableSkill(ZSkill.Inventory);
                }
                break;
        }
        if (action.oncePerTurn()) {
            actionsDoneThisTurn.add(action);
        }
        if (isInvisible() && action.breaksInvisibility()) {
            removeAvailableSkill(ZSkill.Invisible);
        }
        return super.performAction(action, game);
    }

    boolean tryOpenDoor(ZGame game) {
        List<ZWeapon> weapons = getWeapons();
        // order the weapons so that the best choice for door is in the front
        Collections.sort(weapons, (o1, o2) -> {
            // first order by % to open
            int v1 = o1.getOpenDoorValue();
            int v2 = o2.getOpenDoorValue();
            return Integer.compare(v2, v1);
        });
        for (ZWeapon w : weapons) {
            ZWeaponStat openDoorStat = w.getOpenDoorStat();
            if (openDoorStat != null) {
                if (w.type.openDoorsIsNoisy)
                    game.addNoise(occupiedZone, 1);
                if (openDoorStat.dieRollToOpenDoor>1) {
                    Integer [] die = game.rollDice(1);
                    if (die[0] < openDoorStat.dieRollToOpenDoor) {
                        game.addLogMessage(name() + " Failed to open the door with their " + w);
                        return false;
                    }
                }
                game.addLogMessage(name() + " Used their " + w + " to break open the door");
                return true;
            }
        }
        return false;
    }

    @Override
    protected int getActionsPerTurn() {
        int actions = 3;
        for (ZSkill s : getAvailableSkills()) {
            if (s == ZSkill.Plus1_Action) // TODO: consider implementing modifyActionsRemaining for Plus1_Action
                actions++;
        }
        return actions;
    }

    public boolean canUnjamDoor() {
        for (ZWeapon w : getWeapons()) {
            if (w.isOpenDoorCapable())
                return true;
        }
        return false;
    }

    public boolean canBarricadeDoors() {
        return isInPossession(ZItemType.BARRICADE);
    }

    /**
     *
     * @return
     */
    public List<ZWeapon> getWeapons() {
        return (List)Utils.filter(Utils.toArray(leftHand, rightHand, body), object -> object instanceof ZWeapon);
    }

    /**
     * Return any armor and / or shileds player has
     * @return
     */
    public List<ZArmor> getArmor() {
        return (List)Utils.filter(Utils.toArray(leftHand, rightHand, body), object -> object instanceof ZArmor);
    }

    public List<ZSpell> getSpells() {
        List<ZSpell> spells = (List)Utils.filter(Utils.toArray(leftHand, rightHand, body), object -> object instanceof ZSpell);
        if (getAvailableSkills().contains(ZSkill.Spellbook)) {
            spells.addAll((List)Utils.filter(backpack, object -> object.isEnchantment()));
        }
        return spells;
    }

    /**
     *
     * @return
     */
    public boolean isDualWielding() {
        if (leftHand == null || rightHand == null)
            return false;
        if (!leftHand.getType().equals(rightHand.getType()))
            return false;
        if (leftHand.isDualWieldCapable())
            return true;
        if (leftHand != null && leftHand instanceof ZWeapon) {
            for (ZSkill skill : this.getAvailableSkills()) {
                if (skill.canTwoHand((ZWeapon) leftHand))
                    return true;
            }
        }
        return false;
    }

    public boolean isDualWielding(ZWeapon weapon) {
        return canDualWield(weapon)
                && leftHand != null && leftHand.getType().equals(weapon.getType())
                && rightHand != null && rightHand.getType().equals(weapon.getType());
    }

    public boolean canDualWield(ZWeapon weapon) {
        if (weapon.isDualWieldCapable())
            return true;
        for (ZSkill skill : getAvailableSkills()) {
            if (skill.canTwoHand(weapon))
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

    List<ZEquipment> getBackpack() {
        return backpack;
    }

    public ZEquipSlot getEmptyEquipSlotFor(ZEquipment e) {
        if (body == null && canEquipBody(e)) {
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
        Utils.assertTrue(!isBackpackFull());
        return ZEquipSlot.BACKPACK;
    }

    public ZEquipSlot getEmptyEquipSlotForOrNull(ZEquipment e) {
        if (body == null && canEquipBody(e)) {
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
        if (isBackpackFull())
            return null;
        return ZEquipSlot.BACKPACK;
    }

    /**
     * If user can equip the item the result slot equipped too. null otherwise
     * @param e
     * @return
     */
    public ZEquipSlot tryEquip(ZEquipment e) {
        if (e.isEquippable(this)) {
            if (body == null && canEquipBody(e)) {
                body = e;
                e.slot = ZEquipSlot.BODY;
                cachedSkills = null;
                return ZEquipSlot.BODY;
            }
            switch (e.getSlotType()) {
                case HAND:
                    if (leftHand == null) {
                        leftHand = e;
                        cachedSkills = null;
                        return e.slot = ZEquipSlot.LEFT_HAND;
                    }
                    if (rightHand == null) {
                        rightHand = e;
                        cachedSkills = null;
                        return e.slot = ZEquipSlot.RIGHT_HAND;
                    }
                    break;
                case BODY:
                    if (body == null) {
                        body = e;
                        cachedSkills = null;
                        return e.slot = ZEquipSlot.BODY;
                    }
                    break;
            }
        }
        if (backpack.size() < MAX_BACKPACK_SIZE) {
            backpack.add(e);
            return e.slot = ZEquipSlot.BACKPACK;
        }
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
                Utils.assertTrue(idx >= 0);
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
            default:
                Utils.assertTrue(false);
                return null;
        }
        cachedSkills = null;
        removed.slot = null;
        return removed;
    }

    public ZEquipment attachEquipment(ZEquipment equipment) {
        return attachEquipment(equipment, getEmptyEquipSlotFor(equipment));
    }

    public ZEquipment attachEquipment(ZEquipment equipment, ZEquipSlot slot) {
        Utils.assertTrue(slot != null);
        equipment.slot = slot;
        ZEquipment prev = null;
        assert slot != null;
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
        cachedSkills = null;
        return prev;
    }

    public ZSkillLevel getSkillLevel() {
        return ZSkillLevel.getLevel(dangerBar);
    }

    public synchronized Table getInfoTable(ZGame game) {

        /*

        Left Hand    |   Body    |    Right Hand    |    Stats    |     Skills
        -----------------------------------------------------------------------

        <left hand card? | <body card> | <right hand card> | <stats card> | <skills[0]>


         */

        Table info = new Table(this).setNoBorder().setPadding(0);

        if (isDualWielding()) {
            info.addColumn("HANDS(DW)", Collections.singletonList(getSlotInfo(ZEquipSlot.LEFT_HAND, game)));
            info.addColumn(ZEquipSlot.BODY.getLabel(), Collections.singletonList(getSlotInfo(ZEquipSlot.BODY, game)));
        } else {
            info.addColumn(ZEquipSlot.LEFT_HAND.getLabel(), Collections.singletonList(getSlotInfo(ZEquipSlot.LEFT_HAND, game)));
            info.addColumn(ZEquipSlot.BODY.getLabel(), Collections.singletonList(getSlotInfo(ZEquipSlot.BODY, game)));
            info.addColumn(ZEquipSlot.RIGHT_HAND.getLabel(), Collections.singletonList(getSlotInfo(ZEquipSlot.RIGHT_HAND, game)));
        }

        Table slotInfo = getSlotInfo(ZEquipSlot.BACKPACK, game);
        info.addColumn(ZEquipSlot.BACKPACK.getLabel() + (isBackpackFull() ? " (full)" : ""), Collections.singletonList(slotInfo));

        Table stats = new Table(this).setNoBorder().setPadding(0);
        StringBuilder armorRating = new StringBuilder("none");
        List<Integer> ratings = getArmorRatings(ZZombieType.Walker);
        if (ratings.size() > 0) {
            armorRating = new StringBuilder("" + ratings.get(0) + "+");
            for (int i=1; i<ratings.size(); i++) {
                armorRating.append("/").append(ratings.get(i)).append("+");
            }
        }
        stats.addRow("Moves", String.format("%d of %d", getActionsLeftThisTurn(), getActionsPerTurn()));
        stats.addRow("Wounds", String.format("%d of %d", woundBar, MAX_WOUNDS));
        stats.addRow("Armor Rolls", armorRating.toString());
        ZSkillLevel sl = getSkillLevel();

        int ptsToNxt = sl.getPtsToNextLevel(dangerBar);
        stats.addRow("Skill", sl);
        stats.addRow("Exp", dangerBar);
        stats.addRow("Next level", ptsToNxt);
        stats.addRow("Dual\nWielding", isDualWielding());

        info.addColumn("Stats", Collections.singletonList(stats));
        if (getAvailableSkills().size() > 0) {
            Table skills = new Table(this).setNoBorder().addColumnNoHeader(Utils.toStringArray(getAvailableSkills(), true));
            info.addColumn("Skills", skills);
        }

        Table main = new Table(this).setNoBorder();
        if (isDead()) {
                main.addRow(String.format("%s (%s) Killed in Action",
                    name.getLabel(), name.characterClass));
        } else {
            main.addRow(String.format("%s (%s) Body:%s",
                    name.getLabel(), name.characterClass, name.alternateBodySlot
            ));

        }
        main.addRow(info);
        return main;
    }

    public boolean isBackpackFull() {
        return backpack.size() == MAX_BACKPACK_SIZE;
    }

    @Override
    public GDimension drawInfo(AGraphics g, ZGame game, float width, float height) {
        return getInfoTable(game).draw(g);
    }

    public boolean canTrade() {
        return getAllEquipment().size() > 0;
    }

    public boolean canSearch(ZZone zone) {
        if (actionsDoneThisTurn.contains(ZActionType.SEARCH))
            return false;
        if (getAvailableSkills().contains(ZSkill.Scavenger))
            return true;
        return zone.isSearchable();
    }

    public List<ZEquipSlot> getEquipableSlots(ZEquipment equip) {
        List<ZEquipSlot> options = new ArrayList<>();
        boolean canEquip = !isBackpackFull();
        if (!canEquip) {
            for (ZEquipment e : backpack) {
                if (e == equip) {
                    canEquip = true;
                    break;
                }
            }
        }
        boolean canWield = equip.isEquippable(this);
        switch (equip.getSlotType()) {
            case BODY:
                if (canWield && (body == null || canEquip))
                    options.add(ZEquipSlot.BODY);
                break;
            case HAND:
                if (canWield) {
                    if (leftHand == null || canEquip)
                        options.add(ZEquipSlot.LEFT_HAND);
                    if (rightHand == null || canEquip)
                        options.add(ZEquipSlot.RIGHT_HAND);
                    if (body == null || canEquip) {
                        if (canEquipBody(equip)) {
                            options.add(ZEquipSlot.BODY);
                        }
                    }
                }
                break;
            case BACKPACK:
                if (!isBackpackFull()) {
                    options.add(ZEquipSlot.BACKPACK);
                }
                break;
        }
        return options;
    }

    public boolean canEquipBody(ZEquipment equip) {
        return name.alternateBodySlot.equals(equip.getType().getEquipmentClass());
    }

    public List<ZWeapon> getMeleeWeapons() {
        List<ZWeapon> slots = new ArrayList<>();
        if (isDualWielding() && leftHand.isMelee()) {
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
        if (isDualWielding() && leftHand.isRanged() && isLoaded(leftHand)) {
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
        List<ZWeapon> slots = new ArrayList<>();
        if (isDualWielding() && leftHand.isMagic()) {
            slots.add((ZWeapon)leftHand);
        } else {
            if (leftHand != null && leftHand.isMagic())
                slots.add((ZWeapon)leftHand);
            if (rightHand != null && rightHand.isMagic())
                slots.add((ZWeapon)rightHand);
        }
        if (body != null && body.isMagic())
            slots.add((ZWeapon)body);
        if (getAvailableSkills().contains(ZSkill.Spellbook)) {
            slots.addAll((List)Utils.filter(backpack, ZEquipment::isMagic));
        }
        return slots;
    }

    public List<ZEquipment> getThrowableEquipment() {
        List<ZEquipment> slots = new ArrayList<>();
        if (leftHand != null && leftHand.isThrowable())
            slots.add(leftHand);
        if (rightHand != null && rightHand.isThrowable())
            slots.add(rightHand);
        if (body != null && body.isThrowable())
            slots.add(body);
        return slots;
    }


    public boolean canReload() {
        return false;
    }

    public boolean isLoaded(ZEquipment weapon) {
        return true;
    }

    public ZWeaponStat getWeaponStat(ZWeapon weapon, ZActionType actionType, ZGame game, int targetZone) {
        ZWeaponStat stat = weapon.getStatForAction(actionType);
        if (stat == null) {
            return null;
        }
        stat = stat.copy();
        for (ZSkill skill : Utils.mergeLists(getAvailableSkills(), weapon.type.getSkillsWhenUsed())) {
            skill.modifyStat(stat, actionType, this, game, targetZone);
        }
        if (weapon.slot != ZEquipSlot.BODY && isDualWielding(weapon)) {
            stat.numDice*=2;
        }
        return stat;
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

    public List<Integer> getArmorRatings(ZZombieType type) {
        List<Integer> ratings = new ArrayList<>();
        int skillRating = 0;
        for (ZSkill skill : getAvailableSkills()) {
            skillRating = Math.max(skillRating, skill.getArmorRating(type));
        }
        for (ZEquipSlot slot : ZEquipSlot.wearableValues()) {
            ZEquipment e = getSlot(slot);
            if (e != null) {
                int rating = e.getType().getDieRollToBlock(type);
                if (rating > 0) {
                    if (e.getType().isShield()) {
                        ratings.add(rating);
                    } else {
                        if (skillRating > 0)
                            rating -= 1;
                        ratings.add(rating);
                        skillRating = 0;
                    }
                }
            }
        }
        if (skillRating > 0) {
            ratings.add(skillRating);
        }
        return ratings;
    }

    /**
     *
     * @param type
     * @return
     */
    public int getArmorRating(ZZombieType type) {
        int rating = 0;
        for (int i : getArmorRatings(type)) {
            rating +=i;
        }
        return rating;
    }

    /**
     *
     * @return
     */
    public final boolean isDead() {
        return woundBar >= MAX_WOUNDS;
    }

    /**
     *
     * @return
     */
    @Override
    public final boolean isAlive() {
        return woundBar < MAX_WOUNDS;
    }

    @Override
    public int getNoise() {
        return isInvisible() ? 0 : 1;
    }

    @Override
    public boolean isInvisible() {
        return forceInvisible || getAvailableSkills().contains(ZSkill.Invisible);
    }

    public void setInvisible(boolean enable) {
        forceInvisible = enable;
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
        List<ZEquipment> all = new ArrayList<>();
        all.addAll(backpack);
        all.addAll(Utils.filter(Utils.toArray(leftHand, body, rightHand), object -> true));
        return all;
    }

    public void removeEquipment(ZEquipment equip) {
        switch (equip.slot) {
            case BACKPACK:
                boolean success = backpack.remove(equip);
                Utils.assertTrue(success);
                break;
            case LEFT_HAND:
                Utils.assertTrue(leftHand == equip);
                leftHand = null;
                cachedSkills = null;
                break;
            case RIGHT_HAND:
                Utils.assertTrue(rightHand == equip);
                rightHand = null;
                cachedSkills = null;
                break;
            case BODY:
                Utils.assertTrue(body == equip);
                body = null;
                cachedSkills = null;
                break;
        }
        cachedSkills = null;
        equip.slot = null;
    }

    /**
     * Return true if a equip type is in any of the users slots or backpack
     * @param type
     * @return
     */
    boolean isInPossession(ZEquipmentType type) {
        for (ZEquipment e : getAllEquipment()) {
            if (e.getType().equals(type))
                return true;
        }
        return false;
    }

    ZEquipment getEquipmentOfType(ZEquipmentType type) {
        for (ZEquipment e : getAllEquipment()) {
            if (e.getType().equals(type))
                return e;
        }
        return null;
    }

    boolean isEquipped(ZEquipmentType type) {
        return Utils.count(Utils.toArray(leftHand, rightHand, body), e -> e.getType() == type) > 0;
    }

    /*
    boolean isRoll6Plus1Die(ZWeaponType weaponType, ZActionType actionType) {
        for (ZSkill skill : Utils.mergeLists(getAvailableSkills(), weaponType.getSkillsWhenUsed())) {
            if (skill.isRoll6Plus1(actionType))
                return true;
        }
        return false;
    }*/

    boolean canReroll(ZAttackType action) {
        if (getAvailableSkills().contains(ZSkill.Lucky))
            return true;

        switch (action) {
            case RANGED_ARROWS:
                return isInPossession(ZItemType.PLENTY_OF_ARROWS);
            case RANGED_BOLTS:
                return isInPossession(ZItemType.PLENTY_OF_BOLTS);
        }
        return false;
    }

    void onEndOfTurn(ZGame game) {
        for (ZSkill skill : getAvailableSkills()) {
            skill.onEndOfTurn(game, this);
        }
        availableSkills.clear();
        for (ZEquipment e : Arrays.asList(leftHand, rightHand, body)) {
            if (e != null) {
                e.onEndOfRound(game);
            }
        }
        availableSkills.addAll(allSkills);
        cachedSkills = null;
    }

    @Override
    long getMoveSpeed() {
        return MOVE_SPEED_MILIS;
    }

    @Override
    int getPriority() {
        return 100;
    }

    public boolean canFriendlyFire() {
        for (ZSkill s : getAvailableSkills()) {
            if (s.avoidsInflictingFriendlyFire())
                return false;
        }
        return true;
    }

    public boolean canReceiveFriendlyFire() {
        if (isDead())
            return false;
        for (ZSkill s : getAvailableSkills()) {
            if (s.avoidsReceivingFriendlyFire())
                return false;
        }
        return true;
    }

    @Override
    public void draw(AGraphics g) {
        if (fallen) {
            g.drawImage(ZIcon.GRAVESTONE.imageIds[0], getRect().fit(g.getImage(ZIcon.GRAVESTONE.imageIds[0])));
        } else {
            drawPedestal(g);
            super.draw(g);
        }
    }

    protected void drawPedestal(AGraphics g) {
        float hgt = 0.04f;
        GRectangle rect = getRect();
        if (color != null) {
            g.setColor(color.darkened(.5f));
            float x = rect.x + 0.02f;
            float w = rect.w - 0.04f;
            g.drawFilledRect(x, rect.y + rect.h - hgt / 4, w, hgt);
            g.drawFilledOval(x, rect.y + rect.h + hgt / 4, w, hgt);
            g.setColor(color);
            g.drawFilledOval(x, rect.y + rect.h - hgt / 2, w, hgt);
        }

    }

    List<ZSkill> getAvailableSkills() {
        if (cachedSkills == null) {
            cachedSkills = new ArrayList<>();
            cachedSkills.addAll(availableSkills);
            for (ZEquipment e : Utils.toArray(leftHand, body, rightHand)) {
                if (e != null) {
                    cachedSkills.addAll(e.getType().getSkillsWhileEquipped());
                }
            }
        }
        return cachedSkills;
    }

    public boolean hasAvailableSkill(ZSkill skill) {
        return getAvailableSkills().contains(skill);
    }

    public boolean useMarksmanForSorting(int zoneIdx) {
        for (ZSkill skill : getAvailableSkills()) {
            if (skill.useMarksmanForSorting(getOccupiedZone(), zoneIdx)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasSkill(ZSkill skill) {
        return allSkills.contains(skill);
    }

    public void addSkill(ZSkill skill) {
        allSkills.add(skill);
        availableSkills.add(skill);
    }

    public void addAvailableSkill(ZSkill skill) {
        availableSkills.add(skill);
        cachedSkills = null;
    }

    public void removeAvailableSkill(ZSkill skill) {
        availableSkills.remove(skill);
        cachedSkills = null;
    }

    public void initAllSkills(ZSkill [][] skills) {
        allSkills.clear();
        for (int i=0; i<ZSkillLevel.NUM_LEVELS; i++) {
            skillsRemaining[i] = new ArrayList<>();
            skillsRemaining[i].addAll(Arrays.asList(skills[i]));
        }
        List<ZSkill> blueSkills = getRemainingSkillsForLevel(0);
        allSkills.addAll(blueSkills);
        blueSkills.clear();
    }

    public List<ZSkill> getRemainingSkillsForLevel(int level) {
        return skillsRemaining[level];
    }

    public void setFallen(boolean fallen) {
        this.fallen = fallen;
    }

    public ZPlayerName getPlayerName() {
        return name;
    }


    public int getDangerBar() {
        return dangerBar;
    }

    public boolean heal(ZGame game, int amt) {
        if (woundBar > 0) {
            game.addLogMessage(String.format("%s has %d wounds healed.", name(), amt));
            woundBar = Math.max(0, woundBar - amt);
            return true;
        }
        return false;
    }

    public void wound(int amt) {
        woundBar += amt;
    }

    public int getKills(ZZombieType type) {
        return kills[type.ordinal()];
    }

    public int getWoundBar() {
        return woundBar;
    }

    public ZEquipment getLeftHand() {
        return leftHand;
    }

    public ZEquipment getRightHand() {
        return rightHand;
    }

    public ZEquipment getBody() {
        return body;
    }

    public void addExperience(int pts) {
        dangerBar += pts;
    }

    public int getZonesMoved() {
        return zonesMoved;
    }

    public boolean isStartingWeaponChosen() {
        return startingWeaponChosen;
    }

    public void setStartingEquipment(ZEquipmentType type) {
        attachEquipment(type.create());
        startingWeaponChosen = true;
    }

    @Override
    boolean isNoisy() {
        return isAlive() && !isInvisible();
    }

    @Override
    public float getScale() {
        switch (name) {
            case Nelly:
                return 1.4f;
            case Baldric:
                return 1.2f;
        }
        return super.getScale();
    }
}
