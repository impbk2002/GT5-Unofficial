package gregtech.api.interfaces;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;

public interface IBlockOnWalkOver {
    void onWalkOver(EntityLivingBase aEntity, World aWorld, int aX, int aY, int aZ);
}
