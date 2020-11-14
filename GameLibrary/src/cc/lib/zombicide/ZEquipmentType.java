package cc.lib.zombicide;

interface ZEquipmentType<T extends ZEquipment> {

    T create();
}
