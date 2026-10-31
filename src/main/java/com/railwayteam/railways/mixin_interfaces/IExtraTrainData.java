package com.railwayteam.railways.mixin_interfaces;

import net.minecraft.nbt.CompoundTag;

/** Railways' extra saved fields on Create trains and carriages (see {@code DynamicNbtCompat}). */
public interface IExtraTrainData {
    void railways$writeExtraData(CompoundTag tag);

    void railways$readExtraData(CompoundTag tag);
}
