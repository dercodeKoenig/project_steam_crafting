package ProjectSteamCrafting.Blocks.Mechanics.HandGenerator;

import ARLib.obj.Face;
import ARLib.obj.ModelFormatException;
import ARLib.obj.WavefrontObject;
import ProjectSteam.Blocks.Mechanics.CrankShaft.EntityCrankShaftBase;
import ProjectSteam.Static;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;

import static ProjectSteam.Static.*;
import static net.minecraft.client.renderer.RenderStateShard.*;

public class RenderSieve implements BlockEntityRenderer<EntitySieve> {

    static WavefrontObject model;
    static ResourceLocation tex = ResourceLocation.fromNamespaceAndPath("projectsteam", "textures/block/planks.png");

    static {
        try {
            model = new WavefrontObject(ResourceLocation.fromNamespaceAndPath("projectsteam_crafting", "objmodels/mechanical_sieve.obj"));
        } catch (ModelFormatException ex) {
            throw new RuntimeException(ex);
        }
    }


    public RenderSieve(BlockEntityRendererProvider.Context c) {
        super();
    }


    void renderModelWithLight(EntitySieve tile, int light) {

        ByteBufferBuilder byteBuffer;
        BufferBuilder b;

        tile.vertexBuffer2.bind();
        byteBuffer = new ByteBufferBuilder(1024);
        b = new BufferBuilder(byteBuffer, VertexFormat.Mode.TRIANGLES, POSITION_COLOR_TEXTURE_NORMAL_LIGHT);
        for (Face i : model.groupObjects.get("sieve.001").faces) {
            i.addFaceForRender(new PoseStack(), b, light, 0, 0xffffffff);
        }
        tile.mesh2 = b.build();
        tile.vertexBuffer2.upload(tile.mesh2);
        byteBuffer.close();

        tile.vertexBuffer.bind();
        byteBuffer = new ByteBufferBuilder(1024);
        b = new BufferBuilder(byteBuffer, VertexFormat.Mode.TRIANGLES, POSITION_COLOR_TEXTURE_NORMAL_LIGHT);
        for (Face i : model.groupObjects.get("sieve.002").faces) {
            i.addFaceForRender(new PoseStack(), b, light, 0, 0xffffffff);
        }
        tile.mesh = b.build();
        tile.vertexBuffer.upload(tile.mesh);
        byteBuffer.close();

        tile.vertexBuffer3.bind();
        byteBuffer = new ByteBufferBuilder(1024);
        b = new BufferBuilder(byteBuffer, VertexFormat.Mode.TRIANGLES, POSITION_COLOR_TEXTURE_NORMAL_LIGHT);
        for (Face i : model.groupObjects.get("arm").faces) {
            i.addFaceForRender(new PoseStack(), b, light, 0, 0xffffffff);
        }
        tile.mesh3 = b.build();
        tile.vertexBuffer3.upload(tile.mesh3);
        byteBuffer.close();
    }

    @Override
    public void render(EntitySieve tile, float partialTick, PoseStack stack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if(tile.isRemoved())return;
        BlockState axleState = tile.getBlockState();
        if (axleState.getBlock() instanceof BlockSieve) {
            Direction facing = axleState.getValue(BlockSieve.FACING);

            if (packedLight != tile.lastLight) {
                tile.lastLight = packedLight;
                renderModelWithLight(tile, packedLight);
            }
            Matrix4f m1 = new Matrix4f(RenderSystem.getModelViewMatrix());
            m1 = m1.mul(stack.last().pose());
            m1 = m1.translate(0.5f, 0.5f, 0.5f);

            if(facing == Direction.WEST){
                m1 = m1.rotate(new Quaternionf().fromAxisAngleDeg(0f,1.0f, 0, 0f));
            }
            if(facing == Direction.EAST){
                m1 = m1.rotate(new Quaternionf().fromAxisAngleDeg(0f,1.0f, 0, 180f));

            }
            if(facing == Direction.SOUTH){
                m1 = m1.rotate(new Quaternionf().fromAxisAngleDeg(0f,1.0f, 0, 90f));
            }
            if(facing == Direction.NORTH){
                m1 = m1.rotate(new Quaternionf().fromAxisAngleDeg(0f,1.0f, 0, 270f));
            }



            LIGHTMAP.setupRenderState();
            LEQUAL_DEPTH_TEST.setupRenderState();
            NO_TRANSPARENCY.setupRenderState();

            RenderSystem.setShader(Static::getEntitySolidDynamicNormalShader);
            ShaderInstance shader = RenderSystem.getShader();
            RenderSystem.setShaderTexture(0, tex);

            Matrix4f m2 = new Matrix4f(m1);
            float crankshaftR = 0.07f;
            double targetHeight = 0.03;
            double armLength = 0.62;
            float XRotationMultiplier =
                    (facing.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : -1)
                    *(facing.getAxis() == Direction.Axis.X ? 1 : -1);

            double a = tile.myMechanicalBlock.currentRotation / 180 * Math.PI + tile.myMechanicalBlock.internalVelocity/TPS*(partialTick+1); // I have no idea why but it is always one tick behind so i do partialtick+1
            float translationX =  -1f+(float) Math.sin(a) * crankshaftR * XRotationMultiplier;
            float translationY =  (float) Math.cos(a) * crankshaftR;
            double b = Math.asin((translationY-targetHeight) / armLength);
            m2.translate(translationX,translationY,-0.04f);
            m2.rotate(new Quaternionf().fromAxisAngleDeg(0f,0f,1f,-(float)b*180f/(float)Math.PI));
            m2.rotate(new Quaternionf().fromAxisAngleDeg(0f,0f,1f,180f)           );

            BlockEntity t = tile.getLevel().getBlockEntity(tile.getBlockPos().relative(facing));
            if (t instanceof EntityCrankShaftBase cs) {
                shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, m2, RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());
                shader.getUniform("NormalMatrix").set((new Matrix3f(m2)).invert().transpose());
                shader.apply();
                tile.vertexBuffer3.bind();
                tile.vertexBuffer3.draw();
            }

            m2 = new Matrix4f(m1);
            float sieveTargetX = 0.4f+(float) (translationX+Math.cos(b)*armLength);
            m2.translate(sieveTargetX,0,0);

            shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, m2, RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());
            shader.getUniform("NormalMatrix").set((new Matrix3f(m2)).invert().transpose());
            shader.apply();
            tile.vertexBuffer.bind();
            tile.vertexBuffer.draw();


            //shader.apply();
            //tile.vertexBuffer2.bind();
            //tile.vertexBuffer2.draw();



            shader.clear();
            VertexBuffer.unbind();

            LIGHTMAP.clearRenderState();
            LEQUAL_DEPTH_TEST.clearRenderState();
            NO_TRANSPARENCY.clearRenderState();
        }
    }
}