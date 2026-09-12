package nano.skytest.client;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.UniformType;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.Optional;
import java.util.OptionalDouble;

public class SimpleQuadTexture {
    private static final int SIZE = 64;
    private static final int UBO_SIZE = new Std140SizeCalculator().putVec3().putVec3().get();

    public static final BindGroupLayout LIGHT_INFO = BindGroupLayout.builder().withUniform("LightInfo", UniformType.UNIFORM_BUFFER).build();
    private static final RenderPipeline PIXEL_GEOMETRY = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withBindGroupLayout(LIGHT_INFO)
                    .withVertexShader(SkytestClient.modID("core/position_color_light"))
                    .withFragmentShader(SkytestClient.modID("core/position_color_light"))
                    .withLocation(SkytestClient.modID("pipeline/pixel_geometry"))
                    .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR_NORMAL)
                    .withDepthStencilState(Optional.empty())
                    .build()
    );

    private final StagedVertexBuffer stagedBuffer;
    private final Projection projection;

    private GpuTexture texture;
    private GpuTextureView textureView;
    private GpuTexture depthTexture;
    private GpuTextureView depthTextureView;
    private ProjectionMatrixBuffer projectionMatrixBuffer;
    private MappableRingBuffer ubo;

    public SimpleQuadTexture() {
        this.stagedBuffer = new StagedVertexBuffer(() -> "Test Buffer", RenderType.SMALL_BUFFER_SIZE);
        this.projection = new Projection();
    }

    public GpuTextureView getTexture() {
        return textureView;
    }

    public void prepareTexture() {

        RenderPipeline renderPipeline = PIXEL_GEOMETRY;
        VertexFormat formatBinding = renderPipeline.getVertexFormatBinding(0);

        assert formatBinding != null;

        PrimitiveTopology primitiveTopology = renderPipeline.getPrimitiveTopology();
        StagedVertexBuffer.Draw draw = this.stagedBuffer.appendDraw(formatBinding, primitiveTopology, RenderSystem.getProjectionType().vertexSorting());

        // Start Render

        PoseStack matrices = new PoseStack();
        Vec3 camera = new Vec3(-0.5, -0.5, -1.0);
        Matrix3f rotation = new Matrix3f();

        matrices.pushPose();

        matrices.translate(-camera.x, -camera.y, -camera.z);
        long t = Minecraft.getInstance().level.getGameTime();
        matrices.rotate(Axis.ZP, t * 0.02f);
        matrices.rotate(Axis.YP, t * 0.03f);
        matrices.rotate(Axis.XP, t * 0.02f);

        VertexConsumer builder = this.stagedBuffer.getVertexBuilder(draw);
        renderFilledBox(matrices.last().pose(), builder, -0.25f, -0.25f, -0.25f, 0.25f, 0.25f, 0.25f, 1.0f, 1.0f, 1.0f, 1.0f);

        matrices.last().pose().get3x3(rotation);
        rotation.invert();
        matrices.popPose();

        // End Render

        this.stagedBuffer.upload();
        StagedVertexBuffer.ExecuteInfo info = this.stagedBuffer.getExecuteInfo(draw);

        if(info != null) {
            GpuDevice device = RenderSystem.getDevice();

            if(this.texture == null) {
                this.texture = device.createTexture("Test Texture", 13, GpuFormat.RGBA8_UNORM, SIZE, SIZE, 1, 1);
                this.textureView = device.createTextureView(this.texture);
                this.depthTexture = device.createTexture("Test Depth", 9, GpuFormat.D32_FLOAT, SIZE, SIZE, 1, 1);
                this.depthTextureView = device.createTextureView(this.depthTexture);
                this.projectionMatrixBuffer = new ProjectionMatrixBuffer("Test Projection");
                this.ubo = new MappableRingBuffer(() -> "Test UBO", 130, UBO_SIZE);
            }

            device.createCommandEncoder().clearColorAndDepthTextures(this.texture, new Vector4f(0.0f, 0.0f, 0.0f, 0.0f), this.depthTexture, 0.0);
            this.projection.setupOrtho(-100.0f, 100.0f, 1.0f, 1.0f, true);
            RenderSystem.setProjectionMatrix(this.projectionMatrixBuffer.getBuffer(this.projection), ProjectionType.PERSPECTIVE);

            try (GpuBufferSlice.MappedView view = this.ubo.currentBuffer().map(false, true)) {
                Vector3f light = new Vector3f(12.0f, -8.0f, 12.0f).mul(rotation);
                Vector3f viewPos = camera.toVector3f().mul(rotation);
                Std140Builder.intoBuffer(view.data()).putVec3(light).putVec3(viewPos);
            }

            try(RenderPass renderPass = device.createCommandEncoder().createRenderPass(() -> "Test", this.textureView, Optional.empty(), this.depthTextureView, OptionalDouble.of(0))) {
                GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms().writeTransform(RenderSystem.getModelViewMatrixCopy());
                renderPass.setPipeline(RenderSystem.getCompiledPipeline(renderPipeline));
                RenderSystem.bindDefaultUniforms(renderPass);
                renderPass.setUniform("DynamicTransforms", dynamicTransforms);
                renderPass.setUniform("LightInfo", this.ubo.currentBuffer());
                renderPass.setVertexBuffer(0, info.vertexBuffer().slice());
                renderPass.setIndexBuffer(info.indexBuffer(), info.indexType());
                renderPass.drawIndexed(info.indexCount(), 1, info.firstIndex(), info.baseVertex(), 0);
            }
        }

        this.stagedBuffer.endFrame();
    }

    private static void renderFilledBox(Matrix4fc positionMatrix, VertexConsumer buffer, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, float red, float green, float blue, float alpha) {
        // Front Face
        buffer.addVertex(positionMatrix, minX, minY, maxZ).setNormal(0.0f, 0.0f, 1.0f).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, maxX, minY, maxZ).setNormal(0.0f, 0.0f, 1.0f).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, maxX, maxY, maxZ).setNormal(0.0f, 0.0f, 1.0f).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, minX, maxY, maxZ).setNormal(0.0f, 0.0f, 1.0f).setColor(red, green, blue, alpha);

        // Back face
        buffer.addVertex(positionMatrix, maxX, minY, minZ).setNormal(0.0f, 0.0f, -1.0f).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, minX, minY, minZ).setNormal(0.0f, 0.0f, -1.0f).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, minX, maxY, minZ).setNormal(0.0f, 0.0f, -1.0f).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, maxX, maxY, minZ).setNormal(0.0f, 0.0f, -1.0f).setColor(red, green, blue, alpha);

        // Left face
        buffer.addVertex(positionMatrix, minX, minY, minZ).setNormal(-1.0f, 0.0f, 0.0f).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, minX, minY, maxZ).setNormal(-1.0f, 0.0f, 0.0f).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, minX, maxY, maxZ).setNormal(-1.0f, 0.0f, 0.0f).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, minX, maxY, minZ).setNormal(-1.0f, 0.0f, 0.0f).setColor(red, green, blue, alpha);

        // Right face
        buffer.addVertex(positionMatrix, maxX, minY, maxZ).setNormal(1.0f, 0.0f, 0.0f).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, maxX, minY, minZ).setNormal(1.0f, 0.0f, 0.0f).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, maxX, maxY, minZ).setNormal(1.0f, 0.0f, 0.0f).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, maxX, maxY, maxZ).setNormal(1.0f, 0.0f, 0.0f).setColor(red, green, blue, alpha);

        // Top face
        buffer.addVertex(positionMatrix, minX, maxY, maxZ).setNormal(0.0f, 1.0f, 0.0f).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, maxX, maxY, maxZ).setNormal(0.0f, 1.0f, 0.0f).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, maxX, maxY, minZ).setNormal(0.0f, 1.0f, 0.0f).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, minX, maxY, minZ).setNormal(0.0f, 1.0f, 0.0f).setColor(red, green, blue, alpha);

        // Bottom face
        buffer.addVertex(positionMatrix, minX, minY, minZ).setNormal(0.0f, -1.0f, 0.0f).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, maxX, minY, minZ).setNormal(0.0f, -1.0f, 0.0f).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, maxX, minY, maxZ).setNormal(0.0f, -1.0f, 0.0f).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, minX, minY, maxZ).setNormal(0.0f, -1.0f, 0.0f).setColor(red, green, blue, alpha);
    }
}