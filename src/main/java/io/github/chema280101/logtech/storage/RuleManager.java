package io.github.chema280101.logtech.storage;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

/**
 * Gestor de reglas adaptado a ItemKey y persistencia NBT de Minecraft para el Núcleo.
 */
public class RuleManager extends RuleEngine<ItemKey> {

    public Tag saveRules(HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (int i = 0; i < MAX_RULES; i++) {
            AutomationRule<ItemKey> rule = getRule(i);
            if (rule != null) {
                CompoundTag tag = new CompoundTag();
                tag.putInt("id", rule.getId());
                tag.putBoolean("enabled", rule.isEnabled());
                tag.putString("condition", rule.getConditionType().name());
                tag.putLong("threshold", rule.getThreshold());
                tag.putString("action", rule.getAction().name());
                if (rule.getItem() != null) {
                    Tag keyTag = ItemKey.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), rule.getItem())
                            .resultOrPartial()
                            .orElse(null);
                    if (keyTag != null) {
                        tag.put("item", keyTag);
                    }
                }
                list.add(tag);
            }
        }
        return list;
    }

    public void loadRules(Tag tag, HolderLookup.Provider registries) {
        clear();
        if (tag instanceof ListTag list) {
            for (int i = 0; i < list.size(); i++) {
                CompoundTag ruleTag = list.getCompound(i);
                int id = ruleTag.getInt("id");
                if (id < 0 || id >= MAX_RULES) {
                    id = i;
                }
                boolean enabled = ruleTag.getBoolean("enabled");
                String condName = ruleTag.getString("condition");
                RuleConditionType condition = RuleConditionType.fromSerialized(condName);
                long threshold = ruleTag.getLong("threshold");
                String actName = ruleTag.getString("action");
                RuleAction action = RuleAction.fromSerialized(actName);

                ItemKey item = null;
                if (ruleTag.contains("item")) {
                    item = ItemKey.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), ruleTag.get("item"))
                            .resultOrPartial()
                            .orElse(null);
                }

                setRule(id, new AutomationRule<>(id, item, condition, threshold, action, enabled));
            }
        }
    }
}
