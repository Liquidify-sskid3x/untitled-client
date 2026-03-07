package com.liquidify.untitled.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface MinecraftClientAccessor {
    @Mutable
    @Accessor("user")
    void untitled$setUser(User user);

    @Accessor("user")
    User untitled$getUser();
}