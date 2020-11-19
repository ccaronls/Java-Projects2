package cc.lib.zombicide;

public interface ZEquipmentType<T extends ZEquipment> {

    T create();
}
