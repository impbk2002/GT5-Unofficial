package gregtech.common.tileentities.machines.multi;

import gregtech.api.GregTech_API;
import gregtech.api.enums.Textures;
import gregtech.api.enums.Textures.BlockIcons;
import gregtech.api.gui.GT_GUIContainer_MultiMachine;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_Output;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_MultiBlockBase;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GT_ModHandler;
import gregtech.api.util.GT_Multiblock_Tooltip_Builder;
import gregtech.api.util.GT_Recipe;
import gregtech.api.util.GT_Utility;
import net.minecraft.block.Block;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;

import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_DISTILLATION_TOWER;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_DISTILLATION_TOWER_ACTIVE;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_DISTILLATION_TOWER_ACTIVE_GLOW;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_DISTILLATION_TOWER_GLOW;

public class GT_MetaTileEntity_DistillationTower extends GT_MetaTileEntity_MultiBlockBase {
    private static final int CASING_INDEX = 49;
    private short controllerY;

    public GT_MetaTileEntity_DistillationTower(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public GT_MetaTileEntity_DistillationTower(String aName) {
        super(aName);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT_MetaTileEntity_DistillationTower(this.mName);
    }

    @Override
    public String[] getDescription() {
        final GT_Multiblock_Tooltip_Builder tt = new GT_Multiblock_Tooltip_Builder();
        tt.addMachineType("Distillery")
                .addInfo("Controller block for the Distillation Tower")
                .addInfo("Fluids are only put out at the correct height")
                .addInfo("The correct height equals the slot number in the NEI recipe")
                .addSeparator()
                .beginVariableStructureBlock(3, 3, 3, 12, 3, 3, true)
                .addController("Front bottom")
                .addOtherStructurePart("Clean Stainless Steel Machine Casing", "7 x h - 5 (minimum)")
                .addEnergyHatch("Any casing")
                .addMaintenanceHatch("Any casing")
                .addInputHatch("Any bottom layer casing")
                .addOutputBus("Any bottom layer casing")
                .addOutputHatch("2-11x Output Hatches (One per layer except bottom layer)")
                .toolTipFinisher("Gregtech");
        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
            return tt.getStructureInformation();
        } else {
            return tt.getInformation();
        }
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, byte aSide, byte aFacing, byte aColorIndex, boolean aActive, boolean aRedstone) {
        if (aSide == aFacing) {
            if (aActive)
                return new ITexture[]{
                        BlockIcons.getCasingTextureForId(CASING_INDEX),
                        TextureFactory.of(OVERLAY_FRONT_DISTILLATION_TOWER_ACTIVE),
                        TextureFactory.builder().addIcon(OVERLAY_FRONT_DISTILLATION_TOWER_ACTIVE_GLOW).glow().build()};
            return new ITexture[]{
                    BlockIcons.getCasingTextureForId(CASING_INDEX),
                    TextureFactory.of(OVERLAY_FRONT_DISTILLATION_TOWER),
                    TextureFactory.builder().addIcon(OVERLAY_FRONT_DISTILLATION_TOWER_GLOW).glow().build()};
        }
        return new ITexture[]{Textures.BlockIcons.getCasingTextureForId(CASING_INDEX)};
    }

    @Override
    public Object getClientGUI(int aID, InventoryPlayer aPlayerInventory, IGregTechTileEntity aBaseMetaTileEntity) {
        return new GT_GUIContainer_MultiMachine(aPlayerInventory, aBaseMetaTileEntity, getLocalName(), "DistillationTower.png");
    }

    @Override
    public GT_Recipe.GT_Recipe_Map getRecipeMap() {
        return GT_Recipe.GT_Recipe_Map.sDistillationRecipes;
    }

    @Override
    public boolean isCorrectMachinePart(ItemStack aStack) {
        return true;
    }

    @Override
    public boolean isFacingValid(byte aFacing) {
        return aFacing > 1;
    }

    @Override
    public boolean checkRecipe(ItemStack aStack) {

        ArrayList<FluidStack> tFluidList = getStoredFluids();
        for (int i = 0; i < tFluidList.size() - 1; i++) {
            for (int j = i + 1; j < tFluidList.size(); j++) {
                if (GT_Utility.areFluidsEqual(tFluidList.get(i), tFluidList.get(j))) {
                    if (tFluidList.get(i).amount >= tFluidList.get(j).amount) {
                        tFluidList.remove(j--);
                    } else {
                        tFluidList.remove(i--);
                        break;
                    }
                }
            }
        }

        long tVoltage = getMaxInputVoltage();
        byte tTier = (byte) Math.max(0, GT_Utility.getTier(tVoltage));
        FluidStack[] tFluids = tFluidList.toArray(new FluidStack[0]);
        if (tFluids.length > 0) {
            for (FluidStack tFluid : tFluids) {
                GT_Recipe tRecipe = GT_Recipe.GT_Recipe_Map.sDistillationRecipes.findRecipe(getBaseMetaTileEntity(), false, gregtech.api.enums.GT_Values.V[tTier], new FluidStack[]{tFluid});
                if (tRecipe != null) {
                    if (tRecipe.isRecipeInputEqual(true, tFluids)) {
                        this.mEfficiency = (10000 - (getIdealStatus() - getRepairStatus()) * 1000);
                        this.mEfficiencyIncrease = 10000;
                        calculateOverclockedNessMulti(tRecipe.mEUt, tRecipe.mDuration, 1, tVoltage);
                        //In case recipe is too OP for that machine
                        if (mMaxProgresstime == Integer.MAX_VALUE - 1 && mEUt == Integer.MAX_VALUE - 1)
                            return false;
                        if (this.mEUt > 0) {
                            this.mEUt = (-this.mEUt);
                        }
                        this.mMaxProgresstime = Math.max(1, this.mMaxProgresstime);
                        this.mOutputItems = new ItemStack[]{tRecipe.getOutput(0)};
                        this.mOutputFluids = tRecipe.mFluidOutputs.clone();
                        updateSlots();
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @Override
    public boolean checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack) {
        controllerY = aBaseMetaTileEntity.getYCoord();
        int xDir = ForgeDirection.getOrientation(aBaseMetaTileEntity.getBackFacing()).offsetX;
        int zDir = ForgeDirection.getOrientation(aBaseMetaTileEntity.getBackFacing()).offsetZ;
        int y = 0; //height
        int casingAmount = 0;
            boolean reachedTop = false;

               for (int x = xDir - 1; x <= xDir + 1; x++) { //x=width
           		for (int z = zDir - 1; z <= zDir + 1; z++) { //z=depth
              			if (x != 0 || z != 0) {
                				IGregTechTileEntity tileEntity = aBaseMetaTileEntity.getIGregTechTileEntityOffset(x, y, z);
                   				Block block = aBaseMetaTileEntity.getBlockOffset(x, y, z);
                   				if (!addInputToMachineList(tileEntity, CASING_INDEX)
                           						&& !addOutputToMachineList(tileEntity, CASING_INDEX)
                           						&& !addMaintenanceToMachineList(tileEntity, CASING_INDEX)
                           						&& !addEnergyInputToMachineList(tileEntity, CASING_INDEX)) {
                        					if (block == GregTech_API.sBlockCasings4 && aBaseMetaTileEntity.getMetaIDOffset(x, y, z) == 1) {
                           						casingAmount++;
                           					} else {
                           						return false;
                           					}
                        				}
                   			}
               		}
           	}
             y++;

                   while (y < 12 && !reachedTop) {
               		for (int x = xDir - 1; x <= xDir + 1; x++) { //x=width
                   			for (int z = zDir - 1; z <= zDir + 1; z++) { //z=depth
                   					IGregTechTileEntity tileEntity = aBaseMetaTileEntity.getIGregTechTileEntityOffset(x, y, z);
                   					Block block = aBaseMetaTileEntity.getBlockOffset(x, y, z);
                   					if (aBaseMetaTileEntity.getAirOffset(x, y, z)) {
                       						if (x != xDir || z != zDir) {
                           							return false;
                             						}
                       					} else {
                       						if (x == xDir && z == zDir) {
                                						reachedTop = true;
                               					}
                        						if (!addOutputToMachineList(tileEntity, CASING_INDEX)
                               								&& !addMaintenanceToMachineList(tileEntity, CASING_INDEX)
                                								&& !addEnergyInputToMachineList(tileEntity, CASING_INDEX)) {
                           							if (block == GregTech_API.sBlockCasings4 && aBaseMetaTileEntity.getMetaIDOffset(x, y, z) == 1) {
                                    							casingAmount++;
                                    						} else {
                                    							return false;
                                    						}
                           						}
                        					}
                        			}
                    		}
                  	y++;
        }
        return casingAmount >= 7 * y - 5 && y >= 3 && y <= 12 && reachedTop;
    }

    @Override
    public int getMaxEfficiency(ItemStack aStack) {
        return 10000;
    }

    @Override
    public int getPollutionPerTick(ItemStack aStack) {
        return 0;
    }

    @Override
    public int getDamageToComponent(ItemStack aStack) {
        return 0;
    }


    @Override
    public boolean explodesOnComponentBreak(ItemStack aStack) {
        return false;
    }

@Override
   public boolean addOutput(FluidStack aLiquid) {
        if (aLiquid == null) return false;
           FluidStack tLiquid = aLiquid.copy();
           for (GT_MetaTileEntity_Hatch_Output tHatch : mOutputHatches) {
                if (isValidMetaTileEntity(tHatch) && GT_ModHandler.isSteam(aLiquid) ? tHatch.outputsSteam() : tHatch.outputsLiquids()) {
               	if (tHatch.getBaseMetaTileEntity().getYCoord() == this.controllerY + 1) {
               		int tAmount = tHatch.fill(tLiquid, false);
                    	if (tAmount >= tLiquid.amount) {
                      	return tHatch.fill(tLiquid, true) >= tLiquid.amount;
                   	} else if (tAmount > 0) {
                       	tLiquid.amount = tLiquid.amount - tHatch.fill(tLiquid, true);
                   	}
              	}
                }
          }
            return false;
       }

       @Override
   protected void addFluidOutputs(FluidStack[] mOutputFluids2) {
           for (int i = 0; i < mOutputFluids2.length; i++) {
               if (mOutputHatches.size() > i && mOutputHatches.get(i) != null && mOutputFluids2[i] != null && isValidMetaTileEntity(mOutputHatches.get(i))) {
              	if (mOutputHatches.get(i).getBaseMetaTileEntity().getYCoord() == this.controllerY + 1 + i) {
                		mOutputHatches.get(i).fill(mOutputFluids2[i], true);
                	}
                }
           }

        }
}
