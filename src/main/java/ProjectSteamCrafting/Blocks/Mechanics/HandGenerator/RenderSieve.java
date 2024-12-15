package ProjectSteamCrafting.Blocks.Mechanics.HandGenerator;

import ARLib.obj.Face;
import ARLib.obj.ModelFormatException;
import ARLib.obj.WavefrontObject;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

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
    }

    @Override
    public void render(EntitySieve tile, float partialTick, PoseStack stack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        BlockState axleState = tile.getBlockState();
        if (axleState.getBlock() instanceof BlockSieve) {
            Direction facing = axleState.getValue(BlockSieve.FACING);

            RENDERTYPE_ENTITY_SOLID_SHADER.setupRenderState();
            LIGHTMAP.setupRenderState();
            LEQUAL_DEPTH_TEST.setupRenderState();
            NO_TRANSPARENCY.setupRenderState();
            RenderSystem.setShaderTexture(0, tex);

            if (packedLight != tile.lastLight) {
                tile.lastLight = packedLight;
                renderModelWithLight(tile, packedLight);
            }

            ShaderInstance shader = RenderSystem.getShader();
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

            Matrix4f m2 = new Matrix4f(m1);

            m2.translate((float) Math.sin(tile.myMechanicalBlock.currentRotation / 180 * Math.PI)*0.07f,0,0);

            shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, m2, RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());

            shader.apply();
            tile.vertexBuffer2.bind();
            tile.vertexBuffer2.draw();


            shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, m2, RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());

            shader.apply();
            tile.vertexBuffer.bind();
            tile.vertexBuffer.draw();

            shader.clear();
            VertexBuffer.unbind();

            LIGHTMAP.clearRenderState();
            LEQUAL_DEPTH_TEST.clearRenderState();
            NO_TRANSPARENCY.clearRenderState();
            RENDERTYPE_ENTITY_SOLID_SHADER.clearRenderState();
        }
    }
}