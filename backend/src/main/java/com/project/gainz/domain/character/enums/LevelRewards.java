package com.project.gainz.domain.character.enums;

import java.util.ArrayList;
import java.util.List;

// 레벨마다 받는 보상 표: 5레벨마다 상의·하의 한 벌, Lv.10·20·30 무대 배경, 나머지는 소품·헤어
// 보상은 레벨에서 정해지므로 따로 저장하지 않고 레벨로 판단한다
public final class LevelRewards {

    private LevelRewards() {
    }

    public enum Type { OUTFIT, BOTTOM, STAGE, ACCESSORY, HAIR }

    // slot은 소품일 때만
    public record Reward(int level, Type type, String key, String name, AccessorySlot slot) {
    }

    public static List<Reward> at(int level) {
        List<Reward> out = new ArrayList<>();
        for (LevelOutfit o : LevelOutfit.values()) {
            if (o.getRequiredLevel() == level) out.add(new Reward(level, Type.OUTFIT, o.name(), o.getLabel(), null));
        }
        for (Accessory a : Accessory.values()) {
            if (a.getSlot() == AccessorySlot.BOTTOM && a.getRequiredLevel() == level) {
                out.add(new Reward(level, Type.BOTTOM, a.name(), a.getLabel(), a.getSlot()));
            }
        }
        for (StageBackground s : StageBackground.values()) {
            if (s.getRequiredLevel() == level && level > 1) out.add(new Reward(level, Type.STAGE, s.name(), s.getLabel(), null));
        }
        for (Accessory a : Accessory.values()) {
            if (a.getSlot() != AccessorySlot.BOTTOM && a.getRequiredLevel() == level) {
                out.add(new Reward(level, Type.ACCESSORY, a.name(), a.getLabel(), a.getSlot()));
            }
        }
        for (HairStyle h : HairStyle.values()) {
            if (h.getRequiredLevel() == level && level > 1) out.add(new Reward(level, Type.HAIR, h.name(), h.getLabel(), null));
        }
        return out;
    }

    // from보다 높고 to 이하인 레벨에서 받는 보상
    public static List<Reward> between(int from, int to) {
        List<Reward> out = new ArrayList<>();
        for (int level = from + 1; level <= Math.min(to, LevelPolicy.MAX_LEVEL); level++) {
            out.addAll(at(level));
        }
        return out;
    }
}
