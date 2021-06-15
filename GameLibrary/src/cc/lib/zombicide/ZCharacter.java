package cc.lib.zombicide;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import cc.lib.game.AGraphics;
import cc.lib.game.GDimension;
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

    ZPlayerName name;
    int woundBar;
    int dangerBar;
    boolean inventoryThisTurn =false;
    private List<ZActionType> actionsDoneThisTurn = new ArrayList<>();
    private final List<ZSkill> allSkills = new ArrayList<>();
    private final List<ZSkill> availableSkills = new ArrayList<>();

    private final List<ZEquipment> backpack = new ArrayList<>();
    ZEquipment leftHand, rightHand, body;
    int [] kills = new int[ZZombieType.values().length];
    private boolean fallen = false;

    synchronized void clear() {
        Arrays.fill(kills, 0);
        leftHand = rightHand = body = null;
        backpack.clear();
        allSkills.clear();
        availableSkills.clear();
        inventoryThisTurn =false;
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
    public GDimension getDimension() {
        return name.imageDim;
    }

    @Override
    synchronized void onBeginRound() {
        actionsDoneThisTurn.clear();
        availableSkills.clear();
        availableSkills.addAll(allSkills);
        inventoryThisTurn = false;
        super.onBeginRound();
    }

    @Override
    protected synchronized void deserialize(BufferedReader _in) throws Exception {
        super.deserialize(_in);
        name.character = this;
    }

    @Override
    public int getMaxCharsPerLine() {
        return 256;
    }

    Table getKillsTable() {
        Table tab = new Table(this).setNoBorder().setPadding(0);
        boolean added = false;
        for (ZZombieType nm : ZZombieType.values()) {
            if (kills[nm.ordinal()] > 0) {
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
    protected synchronized boolean performAction(ZActionType action, ZGame game) {
        for (ZSkill skill : availableSkills) {
            if (skill.modifyActionsRemaining(this, action, game)) {
                game.getCurrentUser().showMessage(name() + " used " + skill + " for a free action");
                availableSkills.remove(skill);
                return true;
            }
        }
        switch (action) {
            case INVENTORY:
                if (inventoryThisTurn)
                    return true;
                inventoryThisTurn = true;
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
        for (ZSkill s : availableSkills) {
            if (s == ZSkill.Plus1_Action) // TODO: condider implementing modifyActionsRemaining for Plus1_Action
                actions++;
        }
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

    public synchronized List<ZSpell> getSpells() {
        List<ZSpell> spells = (List)Utils.filterItems(object -> object instanceof ZSpell, leftHand, rightHand, body);
        if (availableSkills.contains(ZSkill.Spellbook)) {
            spells.addAll((List)Utils.filter(new ArrayList<>(backpack), object -> object.isEnchantment()));
        }
        return spells;
    }

    public synchronized List<ZArmor> getArmorForDefense() {
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
    public synchronized boolean isDualWeilding() {
        if (leftHand == null || rightHand == null)
            return false;
        if (!leftHand.getType().equals(rightHand.getType()))
            return false;
        if (leftHand.canDualWield())
            return true;
        if (leftHand != null && leftHand instanceof ZWeapon) {
            for (ZSkill skill : this.availableSkills) {
                if (skill.canTwoHand((ZWeapon) leftHand))
                    return true;
            }
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
        Utils.assertTrue(!isBackpackFull());
        return ZEquipSlot.BACKPACK;
    }

    public ZEquipSlot equip(ZEquipment e) {
        if (body == null && Utils.linearSearch(name.alternateBodySlots, e.getType()) >= 0) {
            body = e;
            e.slot = ZEquipSlot.BODY;
            return ZEquipSlot.BODY;
        }
        switch (e.getSlotType()) {
            case HAND:
                if (leftHand == null) {
                    leftHand = e;
                    return e.slot = ZEquipSlot.LEFT_HAND;
                }
                if (rightHand == null) {
                    rightHand = e;
                    return e.slot = ZEquipSlot.RIGHT_HAND;
                }
                break;
            case BODY:
                if (body == null) {
                    body = e;
                    return e.slot = ZEquipSlot.BODY;
                }
                break;
        }
        if (backpack.size() < MAX_BACKPACK_SIZE) {
            backpack.add(e);
            return e.slot = ZEquipSlot.BACKPACK;
        }

        Utils.assertTrue(false);
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

    public synchronized Table getInfoTable(ZGame game) {

        /*

        Left Hand    |   Body    |    Right Hand    |    Stats    |     Skills
        -----------------------------------------------------------------------

        <left hand card? | <body card> | <right hand card> | <stats card> | <skills[0]>


         */

        Table info = new Table(this).setNoBorder().setPadding(0);

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

        Table stats = new Table(this).setNoBorder().setPadding(0);
        stats.addRow("Wounds", woundBar);
        ZSkillLevel sl = getSkillLevel();
        int ptsToNxt = sl.getPtsToNextLevel(dangerBar);
        stats.addRow("Skill", sl);
        stats.addRow("Exp", dangerBar);
        stats.addRow("Next level", ptsToNxt);
        stats.addRow("Dual\nWielding", isDualWeilding());

        info.addColumn("Stats", Arrays.asList(stats));
        if (availableSkills.size() > 0) {
            Table skills = new Table(this).setNoBorder().addColumnNoHeader(Utils.toStringArray(availableSkills, true));
            info.addColumn("Skills", skills);
        }

        Table main = new Table(this).setNoBorder();
        if (isDead()) {
                main.addRow(String.format("%s (%s) Killed in Action",
                    name.getLabel(), name.characterClass));
        } else {
            main.addRow(String.format("%s (%s) moves: %d/%d Body:%s Actions:%s",
                    name.getLabel(), name.characterClass,
                    getActionsLeftThisTurn(), getActionsPerTurn(),
                    Arrays.toString(Utils.toStringArray(name.alternateBodySlots, true)),
                    Arrays.toString(Utils.toStringArray(actionsDoneThisTurn, true))));

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

    public boolean canSearch() {
        return !isBackpackFull() && !actionsDoneThisTurn.contains(ZActionType.SEARCH);
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
        switch (equip.getSlotType()) {
            case BODY:
                if (body == null || canEquip)
                    options.add(ZEquipSlot.BODY);
                break;
            case HAND:
                if (leftHand == null || canEquip)
                    options.add(ZEquipSlot.LEFT_HAND);
                if (rightHand == null || canEquip)
                    options.add(ZEquipSlot.RIGHT_HAND);
                if (body == null || canEquip) {
                    if (Utils.linearSearch(name.alternateBodySlots, equip.getType()) >= 0) {
                        options.add(ZEquipSlot.BODY);
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
        if (isDualWeilding() && leftHand.isRanged() && isLoaded(leftHand)) {
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
        if (isDualWeilding() && leftHand.isMagic()) {
            slots.add((ZWeapon)leftHand);
        } else {
            if (leftHand != null && leftHand.isMagic())
                slots.add((ZWeapon)leftHand);
            if (rightHand != null && rightHand.isMagic())
                slots.add((ZWeapon)rightHand);
        }
        if (body != null && body.isMagic())
            slots.add((ZWeapon)body);
        if (availableSkills.contains(ZSkill.Spellbook)) {
            slots.addAll((List)Utils.filter(new ArrayList<>(backpack), object -> object.isMagic()));
        }
        return slots;
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

    public synchronized ZWeaponStat getWeaponStat(ZWeapon weapon, ZActionType attackType, ZGame game, int targetZone) {
        ZWeaponStat stat = null;
        switch (attackType) {
            case MELEE:
                if (!weapon.isMelee())
                    return null;
                stat = weapon.type.meleeStats.copy();
                break;
            case ARROWS:
                if (!weapon.isRanged())
                    return null;
                if (!weapon.type.usesArrows)
                    return null;
                stat = weapon.type.rangedStats.copy();
                break;
            case BOLTS:
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
            skill.modifyStat(stat, attackType, this, game, targetZone);
        }
        if (weapon.slot != ZEquipSlot.BODY && isDualWeilding()) {
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
    public final boolean isDead() {
        return woundBar >= MAX_WOUNDS;
    }

    /**
     *
     * @return
     */
    public final boolean isAlive() {
        return woundBar < MAX_WOUNDS;
    }

    @Override
    public int getNoise() {
        return isInvisible() ? 0 : 1;
    }

    @Override
    public synchronized boolean isInvisible() {
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
        switch (equip.slot) {
            case BACKPACK:
                boolean success = backpack.remove(equip);
                Utils.assertTrue(success);
                break;
            case LEFT_HAND:
                Utils.assertTrue(leftHand == equip);
                leftHand = null;
                break;
            case RIGHT_HAND:
                Utils.assertTrue(rightHand == equip);
                rightHand = null;
                break;
            case BODY:
                Utils.assertTrue(body == equip);
                body = null;
                break;
        }
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

    boolean isEquiped(ZEquipmentType type) {
        return Utils.filterItems((ZEquipment e) -> e.getType() == type, leftHand, rightHand, body).size() > 0;
    }

    boolean isRoll6Plus1Die(ZActionType type) {
        for (ZSkill skill : availableSkills) {
            if (skill.isRoll6Plus1(type))
                return true;
        }
        return false;
    }

    synchronized boolean canReroll(ZActionType action) {
        if (availableSkills.contains(ZSkill.Lucky))
            return true;

        switch (action) {
            case ARROWS:
                return isInPossession(ZItemType.PLENTY_OF_ARROWS);
            case BOLTS:
                return isInPossession(ZItemType.PLENTY_OF_BOLTS);
        }
        return false;
    }

    synchronized void onEndOfTurn(ZGame game) {
        for (ZSkill skill : availableSkills) {
            skill.onEndOfTurn(game, this);
        }
        availableSkills.clear();
    }

    @Override
    long getMoveSpeed() {
        return MOVE_SPEED_MILIS;
    }

    @Override
    int getPriority() {
        return 100;
    }

    public synchronized boolean canFriendlyFire() {
        for (ZSkill s : availableSkills) {
            if (s.avoidsFriendlyFire())
                return false;
        }
        return true;
    }

    @Override
    public void draw(AGraphics g) {
        if (fallen) {
            g.drawImage(ZIcon.GRAVESTONE.imageIds[0], getRect());
        } else {
            super.draw(g);
        }
    }

    public boolean isInventoryThisTurn() {
        return inventoryThisTurn;
    }

    public List<ZSkill> getAvailableSkills() {
        return Collections.unmodifiableList(availableSkills);
    }

    public boolean hasAvailableSkill(ZSkill skill) {
        return availableSkills.contains(skill);
    }

    public boolean hasSkill(ZSkill skill) {
        return allSkills.contains(skill);
    }

    public synchronized void addSkill(ZSkill skill) {
        allSkills.add(skill);
        availableSkills.add(skill);
    }

    public synchronized void addAvailableSkill(ZSkill skill) {
        availableSkills.add(skill);
    }

    public synchronized void removeAvailableSkill(ZSkill skill) {
        availableSkills.remove(skill);
    }

    public synchronized void initAllSkills(ZSkill ... skills) {
        allSkills.clear();
        allSkills.addAll(Arrays.asList(skills));
    }

    public void setFallen(boolean fallen) {
        this.fallen = fallen;
    }

    public ZPlayerName getPlayerName() {
        return name;
    }
}
